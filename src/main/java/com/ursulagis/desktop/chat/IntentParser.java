package com.ursulagis.desktop.chat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiResponse;

/**
 * Sends user text to an AI client and parses the JSON intent response.
 */
public class IntentParser {

	private static final Pattern JSON_STRING = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

	private static final Pattern ACTION_PATTERN = Pattern.compile("\"action\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern TARGET_PATTERN = Pattern.compile("\"targetName\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern CAMPANIA_PATTERN = Pattern.compile("\"campaniaName\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern CULTIVO_PATTERN = Pattern.compile("\"cultivoName\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern BEGIN_PATTERN = Pattern.compile("\"beginDate\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern END_PATTERN = Pattern.compile("\"endDate\"\\s*:\\s*" + JSON_STRING.pattern());
	private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*([0-9.]+)");
	private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*" + JSON_STRING.pattern());

	private final AiClient aiClient;
	private final MapLayerContext layerContext;
	private final String codeContext;
	private final String manualContext;
	private AiResponse lastResponse;

	public IntentParser(AiClient aiClient, MapLayerContext layerContext) {
		this(aiClient, layerContext, "", "");
	}

	public IntentParser(AiClient aiClient, MapLayerContext layerContext, String codeContext) {
		this(aiClient, layerContext, codeContext, "");
	}

	public IntentParser(AiClient aiClient, MapLayerContext layerContext, String codeContext, String manualContext) {
		this.aiClient = aiClient;
		this.layerContext = layerContext != null ? layerContext : MapLayerContext.empty();
		this.codeContext = codeContext != null ? codeContext : "";
		this.manualContext = manualContext != null ? manualContext : "";
	}

	public AiClient getAiClient() {
		return aiClient;
	}

	public AiResponse getLastResponse() {
		return lastResponse;
	}

	public ParsedIntent parse(String userMessage) {
		lastResponse = aiClient.complete(buildSystemPrompt(), userMessage);
		ParsedIntent intent = parseJson(lastResponse.getContent());
		return intent.enrichFromUserText(userMessage);
	}

	public String buildSystemPrompt() {
		StringBuilder sb = new StringBuilder();
		sb.append(UrsulaPersonality.systemPromptPreamble()).append('\n');
		sb.append("Map user requests to one action id.\n");
		sb.append("Respond ONLY with JSON: {\"action\":\"ACTION_ID\",\"targetName\":\"optional\",");
		sb.append("\"campaniaName\":\"optional\",\"cultivoName\":\"optional\",");
		sb.append("\"beginDate\":\"yyyy-MM-dd optional\",\"endDate\":\"yyyy-MM-dd optional\",");
		sb.append("\"confidence\":0.0-1.0,\"message\":\"short reply in Ursula's voice\"}\n");
		sb.append("For DOWNLOAD_NDVI_ASIGNACIONES fill campaniaName/cultivoName/beginDate/endDate when the user provides them.\n");
		sb.append("Use the achievement hints below to choose the correct action. ");
		sb.append("Converting polygons on the map is not the same as importing a shapefile.\n");
		sb.append("If no action applies, respond with action UNKNOWN.\n");
		sb.append(ChatUiKnowledge.marginMapSection()).append('\n');
		sb.append(ChatUiKnowledge.siembraFertilizadaWorkflowSection()).append('\n');
		if (!manualContext.isBlank()) {
			sb.append("\n").append(manualContext).append('\n');
		}
		if (!codeContext.isBlank()) {
			sb.append("\nGitHub source context (").append(GitHubRepoConfig.OWNER).append('/')
					.append(GitHubRepoConfig.REPO).append("):\n").append(codeContext).append('\n');
		}
		sb.append(layerContext.toPromptSection()).append('\n');
		sb.append(AchievementIntentCatalog.buildActionCatalogForPrompt());
		return sb.toString();
	}

	static ParsedIntent parseJson(String json) {
		if (json == null || json.isBlank()) {
			return new ParsedIntent(UrsulaAction.UNKNOWN, null, 0, "Empty AI response.");
		}
		String actionId = extract(ACTION_PATTERN, json);
		String target = extract(TARGET_PATTERN, json);
		String campania = extract(CAMPANIA_PATTERN, json);
		String cultivo = extract(CULTIVO_PATTERN, json);
		LocalDate begin = parseDate(extract(BEGIN_PATTERN, json));
		LocalDate end = parseDate(extract(END_PATTERN, json));
		String confidenceStr = extract(CONFIDENCE_PATTERN, json);
		String message = extract(MESSAGE_PATTERN, json);

		double confidence = 0.5;
		if (confidenceStr != null) {
			try {
				confidence = Double.parseDouble(confidenceStr);
			} catch (NumberFormatException ignored) {
				// keep default
			}
		}

		UrsulaAction action = UrsulaAction.fromId(actionId).orElse(UrsulaAction.UNKNOWN);
		if (message == null || message.isBlank()) {
			message = action.getDescription();
		}
		return new ParsedIntent(action, target, confidence, message, campania, cultivo, begin, end, null);
	}

	private static LocalDate parseDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(raw.trim());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String extract(Pattern pattern, String json) {
		Matcher m = pattern.matcher(json);
		if (m.find()) {
			return unescape(m.group(1));
		}
		return null;
	}

	private static String unescape(String s) {
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
