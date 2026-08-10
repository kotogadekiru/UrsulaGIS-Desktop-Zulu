package com.ursulagis.desktop.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;

/**
 * Maps onboarding achievements (logros) to chat actions and intent matching.
 * Achievement titles and hints from i18n are the single source for natural-language mapping.
 */
public final class AchievementIntentCatalog {

	private static final String ACHIEVEMENT_KEY_PREFIX = "Onboarding.achievement.";
	private static final String HINT_KEY_PREFIX = "Onboarding.hint.";
	private static final double MIN_MATCH_SCORE = 2.5;

	private record ChatMapping(String achievementId, UrsulaAction action, String... phrases) {
	}

	private static final List<ChatMapping> CHAT_MAPPINGS = List.of(
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_DRAWN, UrsulaAction.CREAR_POLIGONO,
					"crear poligono", "crear polígono", "dibujar poligono", "dibujar polígono", "draw polygon", "new polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_IMPORTED, UrsulaAction.IMPORT_POLIGONO,
					"importar poligono", "importar polígonos", "importar poligonos", "import polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_IMPORTED, UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE,
					"activa poligonos superficie", "activar poligonos superficie",
					"activa los poligonos con superficie", "poligonos superficie mayor cero",
					"enable polygons area"),
			new ChatMapping(OnboardingAchievements.FIRST_DISTANCE_MEASURED, UrsulaAction.MEDIR_DISTANCIA,
					"medir distancia", "measure distance"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_HARVEST, UrsulaAction.CONVERTIR_POLIGONO_A_COSECHA,
					"convertir poligono cosecha", "convertir polígono cosecha", "convertir a cosecha",
					"convert polygon harvest", "convertir poligonos cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_SIEMBRA, UrsulaAction.CONVERTIR_POLIGONO_A_SIEMBRA,
					"convertir poligono siembra", "convertir polígono siembra", "convertir a siembra"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_FERTILIZATION, UrsulaAction.CONVERTIR_POLIGONO_A_FERTILIZACION,
					"convertir poligono fertilizacion", "convertir polígono fertilización", "convertir a fertilizacion",
					"convertir a fertilización"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_PULVERIZATION, UrsulaAction.CONVERTIR_POLIGONO_A_PULVERIZACION,
					"convertir poligono pulverizacion", "convertir polígono pulverización", "convertir a pulverizacion",
					"convertir a pulverización"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_IMPORTED, UrsulaAction.IMPORT_COSECHA,
					"importar cosecha", "abrir cosecha", "import harvest", "open harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_IMPORTED, UrsulaAction.IMPORT_SIEMBRA,
					"importar siembra", "cargar siembra", "abrir siembra", "import seeding", "open seeding"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_SHARED, UrsulaAction.COMPARTIR_SIEMBRA,
					"compartir siembra", "share seeding", "cargar siembra compartir", "cargar una siembra y compartirla"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_IMPORTED, UrsulaAction.IMPORT_RECORRIDA,
					"importar recorrida", "abrir recorrida", "import recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_IMPORTED, UrsulaAction.IMPORT_SUELO,
					"importar suelo", "abrir suelo", "import soil"),
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_NUTRIENT_BALANCE, UrsulaAction.BALANCE_NUTRIENTES,
					"balance nutrientes", "balance de nutrientes", "nutrient balance"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_SHAPEFILES_JOINED, UrsulaAction.JUNTAR_SHAPES,
					"unir shape", "juntar shape", "merge shape", "unir shapefiles"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_MULTI_LAYER_HISTOGRAM, UrsulaAction.COMPARE_ACTIVE_LAYERS,
					"comparar capas activas", "comparacion capas activas", "comparación capas activas",
					"histograma multilayer", "comparar capas", "compare active layers"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ASIGNACION_CREATED, UrsulaAction.CONFIG_ASIGNACION,
					"asignar actividades", "asignar actividad", "asignar a lote", "asignar a lotes",
					"asignacion lotes", "asignación lotes", "asignar cultivo lote", "asignar campania lote",
					"asignar campaña lote", "abrir asignacion", "abrir asignación"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.RESUMIR_LABOR,
					"resumir labor", "resumir capa", "resumir cosecha", "resumir la cosecha",
					"resumir cosecha activa", "simplificar", "simplify"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_EXPORTED, UrsulaAction.EXPORT_LABOR,
					"exportar labor", "exportar capa", "export layer", "export shape"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_CLONED, UrsulaAction.CLONAR_LABOR,
					"clonar labor", "clonar capa", "clone labor"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_ASIGNACIONES_DOWNLOADED, UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES,
					"ndvi asignacion", "ndvi asignación", "ndvi campania", "ndvi campaña",
					"descargar ndvi campania", "descargar ndvi campaña", "descargar ndvi asignacion",
					"ndvi contornos asignacion", "ndvi de soja campania", "ndvi de soja campaña",
					"obtener ndvi campania", "obtener ndvi campaña", "ndvi por asignacion",
					"download ndvi campaign", "ndvi assignments",
					"lotes asignados", "imagenes ndvi", "últimas imagenes ndvi", "ultimas imagenes ndvi",
					"ndvi de los lotes", "ndvi trigo", "asignados a trigo"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_DOWNLOADED, UrsulaAction.DOWNLOAD_NDVI,
					"descargar ndvi", "obtener ndvi", "download ndvi", "ndvi para"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_SHARED, UrsulaAction.COMPARTIR_COSECHA,
					"compartir cosecha", "share harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_SYNCED_FROM_CLOUD, UrsulaAction.UPDATE_RECORRIDA,
					"sincronizar recorrida", "actualizar recorrida", "update recorrida", "sync recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_EXPORTED, UrsulaAction.EXPORT_RECORRIDA,
					"exportar recorrida", "export recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS, UrsulaAction.GENERAR_MARGEN,
					"generar mapa margen", "generar mapa de margen", "generar margen", "generar margenes",
					"mapa de margenes", "mapa de margen", "calcular margen", "calcular rentabilidad",
					"rentabilidades", "crear mapa margen", "como genero margen"),
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_IMPORTED, UrsulaAction.IMPORT_MARGEN,
					"importar margen", "importar margenes", "importar mapa margen", "abrir margen"));

	private AchievementIntentCatalog() {
	}

	public static Optional<AchievementIntentMatch> match(String userPrompt) {
		if (userPrompt == null || userPrompt.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(userPrompt);

		AchievementIntentMatch best = null;
		double bestScore = 0;

		for (ChatMapping mapping : CHAT_MAPPINGS) {
			double score = scoreMapping(normalized, mapping);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						mapping.action(),
						mapping.achievementId(),
						score,
						suggestReply(mapping.achievementId(), mapping.action()));
			}
		}

		if (best == null || bestScore < MIN_MATCH_SCORE) {
			return Optional.empty();
		}
		return Optional.of(best);
	}

	public static Optional<AchievementIntentMatch> findNearest(String userPrompt) {
		if (userPrompt == null || userPrompt.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(userPrompt);
		AchievementIntentMatch best = null;
		double bestScore = 0;

		for (ChatMapping mapping : CHAT_MAPPINGS) {
			double score = scoreMapping(normalized, mapping);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						mapping.action(),
						mapping.achievementId(),
						score,
						suggestReply(mapping.achievementId(), mapping.action()));
			}
		}
		for (String achievementId : OnboardingAchievements.getInstance().getAllAchievementIds()) {
			double score = scoreHintForQuery(normalized, achievementId);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						UrsulaAction.UNKNOWN,
						achievementId,
						score,
						achievementHint(achievementId));
			}
		}
		return bestScore > 0 ? Optional.ofNullable(best) : Optional.empty();
	}

	public static String buildRelevantHintsForQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return "";
		}
		String normalized = normalize(userQuery);
		return Arrays.stream(OnboardingAchievements.getInstance().getAllAchievementIds())
				.map(id -> new ScoredHint(id, scoreHintForQuery(normalized, id)))
				.filter(s -> s.score() > 0)
				.sorted((a, b) -> Double.compare(b.score(), a.score()))
				.limit(5)
				.map(s -> "- [" + s.achievementId() + "] " + achievementHint(s.achievementId()))
				.reduce((a, b) -> a + "\n" + b)
				.orElse("- (no close achievement match)");
	}

	public static boolean isMarginGenerationQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String normalized = normalize(userQuery);
		return mentionsMargin(normalized)
				&& containsAnyToken(normalized, GENERATION_VERBS)
				&& !containsAnyToken(normalized, IMPORT_VERBS);
	}

	public static boolean isActivatePolygonsWithAreaQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean activate = n.contains("activa") || n.contains("activar");
		boolean polygon = n.contains("poligono");
		boolean area = n.contains("superficie") || n.contains("area") || n.contains("mayor") || n.contains("cero");
		return activate && polygon && area;
	}

	public static boolean isSiembraShareOrImportQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean siembra = n.contains("siembra");
		boolean share = n.contains("compartir");
		boolean load = n.contains("cargar") || n.contains("importar") || n.contains("abrir");
		return siembra && (share || load);
	}

	public static boolean isAsignacionNdviQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean ndvi = n.contains("ndvi");
		boolean asignacion = n.contains("asignacion") || n.contains("asignad")
				|| n.contains("contorno") || (n.contains("lote") && n.contains("asign"));
		boolean campania = n.contains("campania") || n.contains("campana") || CAMPAIGN_TOKEN.matcher(n).find();
		boolean crop = n.contains("soja") || n.contains("maiz") || n.contains("trigo")
				|| n.contains("girasol") || n.contains("cultivo");
		boolean latestLots = (n.contains("ultima") || n.contains("ultimo") || n.contains("imagen"))
				&& (n.contains("lote") || asignacion);
		return ndvi && (asignacion || latestLots || (campania && crop) || (campania && n.contains("descargar"))
				|| (crop && n.contains("lote")));
	}

	private static final java.util.regex.Pattern CAMPAIGN_TOKEN =
			java.util.regex.Pattern.compile("\\b\\d{2}\\s*[/-]\\s*\\d{2}\\b");

	private record ScoredHint(String achievementId, double score) {
	}

	public static String buildActionCatalogForPrompt() {
		StringBuilder sb = new StringBuilder();
		sb.append("Available actions (mapped from onboarding achievements):\n");
		for (ChatMapping mapping : CHAT_MAPPINGS) {
			sb.append("- ").append(mapping.action().name())
					.append(" [").append(mapping.achievementId()).append("]: ")
					.append(achievementLabel(mapping.achievementId())).append(" — ")
					.append(achievementHint(mapping.achievementId())).append('\n');
		}
		for (UrsulaAction action : UrsulaAction.values()) {
			if (action == UrsulaAction.UNKNOWN || action == UrsulaAction.HELP || action == UrsulaAction.LIST_LAYERS) {
				continue;
			}
			if (isMapped(action)) {
				continue;
			}
			sb.append("- ").append(action.name()).append(": ").append(action.getDescription()).append('\n');
		}
		return sb.toString();
	}

	public static String buildHelpBullets() {
		List<String> bullets = new ArrayList<>();
		for (ChatMapping mapping : CHAT_MAPPINGS) {
			String hint = achievementHint(mapping.achievementId());
			if (!hint.isBlank()) {
				bullets.add("• " + phraseFromHint(hint));
			}
		}
		bullets.add("• capas cargadas / ayuda");
		bullets.add("• activar o desactivar una rama completa: clic en el checkbox del nodo de la rama en el árbol de capas");
		return String.join("\n", bullets);
	}

	public static String achievementLabel(String achievementId) {
		return msg(ACHIEVEMENT_KEY_PREFIX + achievementId, achievementId);
	}

	public static String achievementHint(String achievementId) {
		return msg(HINT_KEY_PREFIX + achievementId, "");
	}

	private static boolean isMapped(UrsulaAction action) {
		return CHAT_MAPPINGS.stream().anyMatch(m -> m.action() == action);
	}

	private static double scoreMapping(String normalizedUser, ChatMapping mapping) {
		double score = 0;
		for (String phrase : mapping.phrases()) {
			if (matchesPhrase(normalizedUser, normalize(phrase))) {
				score = Math.max(score, 10.0);
			}
		}
		score = Math.max(score, tokenOverlapScore(normalizedUser, achievementLabel(mapping.achievementId())));
		score = Math.max(score, tokenOverlapScore(normalizedUser, achievementHint(mapping.achievementId())));
		score = adjustMarginScore(normalizedUser, mapping.achievementId(), score);
		score = adjustPolygonActivateScore(normalizedUser, mapping.action(), score);
		score = adjustSiembraQueryScore(normalizedUser, mapping.action(), score);
		return score;
	}

	private static double scoreHintForQuery(String normalizedUser, String achievementId) {
		double score = tokenOverlapScore(normalizedUser, achievementLabel(achievementId))
				+ tokenOverlapScore(normalizedUser, achievementHint(achievementId));
		return adjustMarginScore(normalizedUser, achievementId, score);
	}

	private static double adjustMarginScore(String normalizedUser, String achievementId, double score) {
		if (!mentionsMargin(normalizedUser)) {
			return score;
		}
		if (OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS.equals(achievementId)) {
			if (containsAnyToken(normalizedUser, GENERATION_VERBS)) {
				score = Math.max(score, 12.0);
			}
		}
		if (OnboardingAchievements.FIRST_MARGEN_IMPORTED.equals(achievementId)) {
			if (containsAnyToken(normalizedUser, GENERATION_VERBS) && !containsAnyToken(normalizedUser, IMPORT_VERBS)) {
				score *= 0.15;
			} else if (containsAnyToken(normalizedUser, IMPORT_VERBS)) {
				score = Math.max(score, 12.0);
			}
		}
		return score;
	}

	private static double adjustPolygonActivateScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isActivatePolygonsWithAreaQuery(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.CREAR_POLIGONO) {
			return score * 0.05;
		}
		return score;
	}

	private static double adjustSiembraQueryScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isSiembraShareOrImportQuery(normalizedUser) || mentionsMargin(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.GENERAR_MARGEN) {
			return score * 0.05;
		}
		if (action == UrsulaAction.COMPARTIR_SIEMBRA) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.IMPORT_SIEMBRA) {
			return Math.max(score, 13.0);
		}
		return score;
	}

	private static boolean mentionsMargin(String text) {
		return text.contains("margen") || text.contains("margenes") || text.contains("rentabilidad");
	}

	private static boolean containsAnyToken(String text, Set<String> tokens) {
		for (String token : tokens) {
			if (text.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesPhrase(String user, String phrase) {
		if (phrase.isBlank()) {
			return false;
		}
		for (String word : phrase.split("\\s+")) {
			if (word.length() < 3) {
				continue;
			}
			if (!user.contains(word)) {
				return false;
			}
		}
		return true;
	}

	private static double tokenOverlapScore(String user, String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		double score = 0;
		for (String token : significantTokens(normalize(text))) {
			if (user.contains(token)) {
				score += token.length() >= 6 ? 2.0 : 1.0;
			}
		}
		return score;
	}

	private static List<String> significantTokens(String text) {
		return Arrays.stream(text.split("\\s+"))
				.filter(t -> t.length() >= 4)
				.filter(t -> !STOPWORDS.contains(t))
				.distinct()
				.toList();
	}

	static String suggestReply(String achievementId, UrsulaAction action) {
		if (action == UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
			return "¡Dale! Activo los polígonos con superficie mayor a cero.";
		}
		if (action == UrsulaAction.IMPORT_SIEMBRA) {
			return "¡Dale! Abrí el importador de siembra (SHP).";
		}
		if (action == UrsulaAction.COMPARTIR_SIEMBRA) {
			return "¡Dale! Comparto la siembra activa (prescripción en línea con QR).";
		}
		String hint = achievementHint(achievementId);
		if (!hint.isBlank()) {
			return "¡Dale! " + phraseFromHint(hint);
		}
		return "¡Dale! " + action.getDescription();
	}

	private static String phraseFromHint(String hint) {
		String trimmed = hint.trim();
		int end = trimmed.indexOf('.');
		if (end > 0) {
			return trimmed.substring(0, end) + ".";
		}
		return trimmed;
	}

	public static String normalize(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		return stripped.replaceAll("[^a-z0-9\\s]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}

	private static final Set<String> STOPWORDS = Set.of(
			"para", "desde", "sobre", "como", "usa", "usá", "usar", "puedes", "podes", "podés",
			"primera", "primero", "vez", "with", "from", "your", "the", "and", "una", "uno",
			"varios", "varias", "nuevo", "nueva", "menu", "menú", "herramientas", "tools");

	private static final Set<String> GENERATION_VERBS = Set.of(
			"generar", "genero", "genera", "generado", "crear", "creo", "calcular", "calculo", "armar");

	private static final Set<String> IMPORT_VERBS = Set.of(
			"importar", "importo", "abrir", "cargar", "shapefile");
}
