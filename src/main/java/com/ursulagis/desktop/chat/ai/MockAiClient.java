package com.ursulagis.desktop.chat.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.AchievementIntentCatalog;
import com.ursulagis.desktop.chat.AchievementIntentMatch;
import com.ursulagis.desktop.chat.ChatGuidanceService;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.UrsulaPersonality;

/**
 * Local rule-based client that simulates LLM intent parsing without network calls.
 * Delegates action matching to {@link AchievementIntentCatalog} (onboarding logros).
 */
public class MockAiClient implements AiClient {

	private static final Pattern TARGET_PATTERN = Pattern.compile(
			"(?:capa|labor|mapa|cosecha|recorrida|ndvi)\\s+[\"']?([^\"'\\n]+)[\"']?",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	@Override
	public AiProvider getProvider() {
		return AiProvider.MOCK;
	}

	@Override
	public AiResponse completePlain(String systemPrompt, String userPrompt) {
		long start = System.currentTimeMillis();
		simulateLatency();
		String text = ChatGuidanceService.guidanceWithoutAi(userPrompt, "", MapLayerContext.empty());
		long elapsed = System.currentTimeMillis() - start;
		return new AiResponse(text, "mock-local", getProvider(), elapsed, true);
	}

	@Override
	public AiResponse complete(String systemPrompt, String userPrompt) {
		long start = System.currentTimeMillis();
		simulateLatency();
		String json = parseIntent(userPrompt, systemPrompt);
		long elapsed = System.currentTimeMillis() - start;
		return new AiResponse(json, "mock-local", getProvider(), elapsed, true);
	}

	protected void simulateLatency() {
		try {
			Thread.sleep(120 + (long) (Math.random() * 180));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected String parseIntent(String userPrompt, String systemPrompt) {
		String text = userPrompt == null ? "" : userPrompt.toLowerCase(Locale.ROOT);
		String target = extractTarget(userPrompt);
		if ((target == null || target.isBlank()) && refersToActiveLayer(text)) {
			target = resolveActiveLayerTarget(systemPrompt);
		}

		if (containsAny(text, "hola", "buenos", "buenas", "hello", "hi", "hey")) {
			return intentJson("HELP", target, 1.0, UrsulaPersonality.greeting());
		}
		if (containsAny(text, "capas cargadas", "loaded layers", "qué capas", "que capas", "listar capas")) {
			return intentJson("LIST_LAYERS", target, 1.0,
					"Te muestro las capas que tenés cargadas en el mapa.");
		}
		if (containsAny(text, "ayuda", "help", "qué puedes", "que puedes", "acciones")) {
			return intentJson("HELP", target, 1.0,
					"Con gusto te cuento todo lo que puedo hacer por vos hoy.");
		}

		Optional<AchievementIntentMatch> achievementMatch = AchievementIntentCatalog.match(userPrompt);
		if (achievementMatch.isPresent()) {
			AchievementIntentMatch match = achievementMatch.get();
			double confidence = Math.min(0.98, 0.75 + (match.score() / 40.0));
			return intentJson(match.action().name(), target, confidence, match.suggestedReply());
		}

		if (containsAny(text, "voyager")) {
			return intentJson("IMPORT_COSECHA_VOYAGER", target, 0.95,
					"Abriendo importación desde Voyager.");
		}
		if (containsAny(text, "importar ndvi", "abrir ndvi", "import ndvi")) {
			return intentJson("IMPORT_NDVI", target, 0.9,
					"Abriendo diálogo para importar NDVI.");
		}
		if (containsAny(text, "ndvi asign", "ndvi campa", "ndvi de soja camp", "descargar ndvi campa",
				"obtener ndvi campa", "ndvi contornos", "download ndvi campaign",
				"lotes asignad", "imagenes ndvi", "últimas imagenes ndvi", "ultimas imagenes ndvi",
				"ndvi de los lotes", "asignados a")) {
			return intentJsonAsignacionNdvi(userPrompt, target);
		}
		if (containsAny(text, "descargar ndvi", "bulk ndvi", "ndvi masivo")) {
			return intentJson("BULK_NDVI_DOWNLOAD", target, 0.9,
					"Iniciando descarga masiva de NDVI.");
		}
		if (containsAny(text, "tabla de labores", "ver labores", "list labors", "show labors")) {
			return intentJson("SHOW_LABORES_TABLE", target, 0.9,
					"Mostrando tabla de labores.");
		}
		if (containsAny(text, "ir a", "zoom", "go to", "centrar")) {
			return intentJson("GO_TO_LAYER", target, 0.85,
					"Centrando vista en la capa.");
		}

		return intentJson("UNKNOWN", target, 0.3, UrsulaPersonality.unknownReply());
	}

	private static boolean refersToActiveLayer(String text) {
		return containsAny(text, "capa activa", "active layer", "la activa", "capa seleccionada", "selected layer");
	}

	private static String resolveActiveLayerTarget(String systemPrompt) {
		if (systemPrompt == null) {
			return null;
		}
		for (String line : systemPrompt.split("\n")) {
			if (line.startsWith("Active layers: ")) {
				String names = line.substring("Active layers: ".length()).trim();
				if (names.isEmpty()) {
					return null;
				}
				String[] parts = names.split(",\\s*");
				if (parts.length == 1) {
					return parts[0].trim();
				}
			}
			if (line.startsWith("Selected in layer tree: ")) {
				return line.substring("Selected in layer tree: ".length()).trim();
			}
		}
		return null;
	}

	private static String extractTarget(String userPrompt) {
		if (userPrompt == null) {
			return null;
		}
		Matcher m = TARGET_PATTERN.matcher(userPrompt);
		if (m.find()) {
			return m.group(1).trim();
		}
		return null;
	}

	private static boolean containsAny(String text, String... keywords) {
		for (String kw : keywords) {
			if (text.contains(kw)) {
				return true;
			}
		}
		return false;
	}

	static String intentJson(String action, String targetName, double confidence, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"action\":\"").append(escape(action)).append("\"");
		if (targetName != null && !targetName.isBlank()) {
			sb.append(",\"targetName\":\"").append(escape(targetName)).append("\"");
		}
		sb.append(",\"confidence\":").append(confidence);
		sb.append(",\"message\":\"").append(escape(message)).append("\"}");
		return sb.toString();
	}

	private static String intentJsonAsignacionNdvi(String userPrompt, String target) {
		com.ursulagis.desktop.chat.AsignacionNdviRequest req =
				com.ursulagis.desktop.chat.AsignacionNdviRequest.parse(userPrompt);
		StringBuilder sb = new StringBuilder();
		sb.append("{\"action\":\"DOWNLOAD_NDVI_ASIGNACIONES\"");
		if (target != null && !target.isBlank()) {
			sb.append(",\"targetName\":\"").append(escape(target)).append("\"");
		}
		if (req.campaniaName() != null) {
			sb.append(",\"campaniaName\":\"").append(escape(req.campaniaName())).append("\"");
		}
		if (req.cultivoName() != null) {
			sb.append(",\"cultivoName\":\"").append(escape(req.cultivoName())).append("\"");
		}
		if (req.begin() != null) {
			sb.append(",\"beginDate\":\"").append(req.begin()).append("\"");
		}
		if (req.end() != null) {
			sb.append(",\"endDate\":\"").append(req.end()).append("\"");
		}
		sb.append(",\"confidence\":0.95");
		sb.append(",\"message\":\"").append(escape(
				"Voy a buscar los contornos de las asignaciones y descargar el NDVI del período indicado.")).append("\"}");
		return sb.toString();
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
