package com.ursulagis.desktop.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiResponse;

/**
 * Sends user text to an AI client and parses the JSON intent response.
 */
public class IntentParser {

	private static final Pattern ACTION_PATTERN = Pattern.compile("\"action\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern TARGET_PATTERN = Pattern.compile("\"targetName\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*([0-9.]+)");
	private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

	private final AiClient aiClient;
	private final MapLayerContext layerContext;
	private AiResponse lastResponse;

	public IntentParser(AiClient aiClient, MapLayerContext layerContext) {
		this.aiClient = aiClient;
		this.layerContext = layerContext != null ? layerContext : MapLayerContext.empty();
	}

	public AiClient getAiClient() {
		return aiClient;
	}

	public AiResponse getLastResponse() {
		return lastResponse;
	}

	public ParsedIntent parse(String userMessage) {
		lastResponse = aiClient.complete(buildSystemPrompt(), userMessage);
		return parseJson(lastResponse.getContent());
	}

	public String buildSystemPrompt() {
		StringBuilder sb = new StringBuilder();
		sb.append(UrsulaPersonality.systemPromptPreamble()).append('\n');
		sb.append("Map user requests to one action id.\n");
		sb.append("Respond ONLY with JSON: {\"action\":\"ACTION_ID\",\"targetName\":\"optional\",\"confidence\":0.0-1.0,\"message\":\"short reply in Ursula's voice\"}\n");
		sb.append(layerContext.toPromptSection()).append('\n');
		sb.append("Available actions:\n");
		for (UrsulaAction action : UrsulaAction.values()) {
			if (action == UrsulaAction.UNKNOWN) {
				continue;
			}
			sb.append("- ").append(action.name()).append(": ").append(action.getDescription()).append('\n');
		}
		return sb.toString();
	}

	static ParsedIntent parseJson(String json) {
		if (json == null || json.isBlank()) {
			return new ParsedIntent(UrsulaAction.UNKNOWN, null, 0, "Empty AI response.");
		}
		String actionId = extract(ACTION_PATTERN, json);
		String target = extract(TARGET_PATTERN, json);
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
		return new ParsedIntent(action, target, confidence, message);
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
