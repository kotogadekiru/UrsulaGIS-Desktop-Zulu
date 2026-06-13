package com.ursulagis.desktop.chat.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.UrsulaPersonality;

/**
 * Local rule-based client that simulates LLM intent parsing without network calls.
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
		if (containsAny(text, "importar cosecha", "abrir cosecha", "open harvest", "import harvest")) {
			return intentJson("IMPORT_COSECHA", target, 0.95,
					"¡Perfecto! Abrimos el diálogo para importar tu mapa de cosecha.");
		}
		if (containsAny(text, "voyager")) {
			return intentJson("IMPORT_COSECHA_VOYAGER", target, 0.95,
					"Abriendo importación desde Voyager.");
		}
		if (containsAny(text, "importar recorrida", "abrir recorrida", "import recorrida")) {
			return intentJson("IMPORT_RECORRIDA", target, 0.95,
					"Abriendo diálogo para importar recorrida.");
		}
		if (containsAny(text, "importar ndvi", "abrir ndvi", "import ndvi")) {
			return intentJson("IMPORT_NDVI", target, 0.9,
					"Abriendo diálogo para importar NDVI.");
		}
		if (containsAny(text, "descargar ndvi", "bulk ndvi", "ndvi masivo")) {
			return intentJson("BULK_NDVI_DOWNLOAD", target, 0.9,
					"Iniciando descarga masiva de NDVI.");
		}
		if (containsAny(text, "importar suelo", "abrir suelo", "import soil")) {
			return intentJson("IMPORT_SUELO", target, 0.9,
					"Abriendo diálogo para importar mapa de suelo.");
		}
		if (containsAny(text, "balance", "nutrientes")) {
			return intentJson("BALANCE_NUTRIENTES", target, 0.85,
					"Procesando balance de nutrientes.");
		}
		if (containsAny(text, "unir shape", "juntar shape", "merge shape")) {
			return intentJson("JUNTAR_SHAPES", target, 0.9,
					"Abriendo herramienta para unir shapefiles.");
		}
		if (containsAny(text, "medir distancia", "measure distance")) {
			return intentJson("MEDIR_DISTANCIA", target, 0.9,
					"Activando herramienta de medición de distancia.");
		}
		if (containsAny(text, "crear polígono", "crear poligono", "draw polygon",
				"create a new polygon", "create a polygon", "create polygon")) {
			return intentJson("CREAR_POLIGONO", target, 0.85,
					"Activando herramienta para crear polígono.");
		}
		if (containsAny(text, "tabla de labores", "ver labores", "list labors", "show labors")) {
			return intentJson("SHOW_LABORES_TABLE", target, 0.9,
					"Mostrando tabla de labores.");
		}
		if (containsAny(text, "sincronizar recorrida", "actualizar recorrida", "update recorrida", "sync recorrida")) {
			return intentJson("UPDATE_RECORRIDA", target, 0.9,
					"Sincronizando recorrida desde la nube.");
		}
		if (containsAny(text, "exportar recorrida", "export recorrida")) {
			return intentJson("EXPORT_RECORRIDA", target, 0.9,
					"Exportando recorrida.");
		}
		if (containsAny(text, "compartir cosecha", "share harvest")) {
			return intentJson("COMPARTIR_COSECHA", target, 0.9,
					"Compartiendo mapa de cosecha.");
		}
		if (containsAny(text, "resumir", "simplify", "simplificar")) {
			return intentJson("RESUMIR_LABOR", target, 0.88,
					"Resumiendo labor seleccionada.");
		}
		if (containsAny(text, "exportar capa", "exportar labor", "export layer", "export shape")) {
			return intentJson("EXPORT_LABOR", target, 0.88,
					"Exportando capa a shapefile.");
		}
		if (containsAny(text, "clonar", "clone")) {
			return intentJson("CLONAR_LABOR", target, 0.85,
					"Clonando labor.");
		}
		if (containsAny(text, "ir a", "zoom", "go to", "centrar")) {
			return intentJson("GO_TO_LAYER", target, 0.85,
					"Centrando vista en la capa.");
		}
		if (containsAny(text, "descargar ndvi para", "ndvi para")) {
			return intentJson("DOWNLOAD_NDVI", target, 0.85,
					"Descargando NDVI para la capa indicada.");
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

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
