package com.ursulagis.desktop.chat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiResponse;

/**
 * Asks an {@link AiClient} to map free-form user text to a JSON intent, then
 * parses that JSON into a {@link ParsedIntent} and enriches campaign/crop/dates
 * from the original utterance. Holds the last raw AI response for debugging/UI.
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

	/** Parser with layer context only (no code/manual grounding). */
	public IntentParser(AiClient aiClient, MapLayerContext layerContext) {
		this(aiClient, layerContext, "", "");
	}

	/** Parser with optional GitHub/local code context. */
	public IntentParser(AiClient aiClient, MapLayerContext layerContext, String codeContext) {
		this(aiClient, layerContext, codeContext, "");
	}

	/**
	 * @param aiClient      client that returns JSON intent responses
	 * @param layerContext  current map layers for the system prompt
	 * @param codeContext   optional source snippets
	 * @param manualContext optional PDF/transcript excerpts
	 */
	public IntentParser(AiClient aiClient, MapLayerContext layerContext, String codeContext, String manualContext) {
		this.aiClient = aiClient;
		this.layerContext = layerContext != null ? layerContext : MapLayerContext.empty();
		this.codeContext = codeContext != null ? codeContext : "";
		this.manualContext = manualContext != null ? manualContext : "";
	}

	/** AI client used for this parse session. */
	public AiClient getAiClient() {
		return aiClient;
	}

	/** Raw response from the most recent {@link #parse(String)} call. */
	public AiResponse getLastResponse() {
		return lastResponse;
	}

	/**
	 * Sends {@code userMessage} to the AI with {@link #buildSystemPrompt()},
	 * parses the JSON body, and enriches NDVI filters from the user text.
	 */
	public ParsedIntent parse(String userMessage) {
		lastResponse = aiClient.complete(buildSystemPrompt(), userMessage);
		ParsedIntent intent = parseJson(lastResponse.getContent());
		return intent.enrichFromUserText(userMessage);
	}

	/**
	 * System prompt listing available actions, UI knowledge, layers, and grounding
	 * context; instructs the model to reply with intent JSON only.
	 */
	public String buildSystemPrompt() {
		StringBuilder sb = new StringBuilder();
		sb.append(UrsulaPersonality.systemPromptPreamble()).append('\n');
		sb.append("Map user requests to one or more action ids.\n");
		sb.append("Respond ONLY with JSON: {\"action\":\"ACTION_ID\",\"actions\":[\"ACTION_ID\"],\"targetName\":\"optional\",");
		sb.append("\"campaniaName\":\"optional\",\"cultivoName\":\"optional\",");
		sb.append("\"beginDate\":\"yyyy-MM-dd optional\",\"endDate\":\"yyyy-MM-dd optional\",");
		sb.append("\"confidence\":0.0-1.0,\"message\":\"short reply in Ursula's voice\"}\n");
		sb.append("When the user asks for several steps in one message (e.g. import and then share), ");
		sb.append("fill \"actions\" with the ordered list of action ids; also set \"action\" to the first one.\n");
		sb.append("For DOWNLOAD_NDVI_ASIGNACIONES fill campaniaName/cultivoName/beginDate/endDate when the user provides them.\n");
		sb.append("CRITICAL: recorrida/recorridas means scouting routes, NOT satellite NDVI. ");
		sb.append("If the user says recorrida(s), choose LOAD_RECORRIDAS (saved routes) or IMPORT_RECORRIDA (shapefile) — ");
		sb.append("never DOWNLOAD_NDVI_ASIGNACIONES / DOWNLOAD_NDVI unless they also say NDVI or imágenes satelitales.\n");
		sb.append("CRITICAL: if the user mentions Voyager, .vy1 or vy1 for harvest/cosecha, choose IMPORT_COSECHA_VOYAGER ");
		sb.append("(not IMPORT_COSECHA, which is shapefile only).\n");
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

	private static final Pattern ACTIONS_ARRAY_PATTERN = Pattern.compile(
			"\"actions\"\\s*:\\s*\\[([^\\]]*)]");

	/**
	 * Best-effort parse of the model's JSON (regex field extraction).
	 * Invalid/empty input becomes {@link UrsulaAction#UNKNOWN}.
	 */
	static ParsedIntent parseJson(String json) {
		if (json == null || json.isBlank()) {
			return new ParsedIntent(UrsulaAction.UNKNOWN, null, 0, "Empty AI response.");
		}
		String actionId = extract(ACTION_PATTERN, json);
		List<UrsulaAction> actions = extractActionsArray(json);
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
		if (actions.isEmpty()) {
			actions = List.of(action);
		} else if (action != UrsulaAction.UNKNOWN && actions.get(0) != action) {
			List<UrsulaAction> merged = new ArrayList<>();
			merged.add(action);
			for (UrsulaAction a : actions) {
				if (a != action) {
					merged.add(a);
				}
			}
			actions = merged;
		}
		if (message == null || message.isBlank()) {
			message = actions.get(0).getDescription();
		}
		return new ParsedIntent(actions, target, confidence, message, campania, cultivo, begin, end, null);
	}

	/** Parses optional {@code "actions":["A","B"]} from the model JSON. */
	private static List<UrsulaAction> extractActionsArray(String json) {
		Matcher m = ACTIONS_ARRAY_PATTERN.matcher(json);
		if (!m.find()) {
			return List.of();
		}
		List<UrsulaAction> out = new ArrayList<>();
		Matcher ids = Pattern.compile("\"([A-Z0-9_]+)\"").matcher(m.group(1));
		while (ids.find()) {
			UrsulaAction.fromId(ids.group(1)).ifPresent(out::add);
		}
		return out;
	}

	/** Parses an ISO date string; returns null on blank or invalid input. */
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

	/** First capturing group of {@code pattern} in {@code json}, with JSON escapes undone. */
	private static String extract(Pattern pattern, String json) {
		Matcher m = pattern.matcher(json);
		if (m.find()) {
			return unescape(m.group(1));
		}
		return null;
	}

	/** Undoes {@code \"} and {@code \\} escapes from a JSON string capture. */
	private static String unescape(String s) {
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
