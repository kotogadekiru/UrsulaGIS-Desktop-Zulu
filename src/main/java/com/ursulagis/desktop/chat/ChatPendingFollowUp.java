package com.ursulagis.desktop.chat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers the last chat action that asked the user for a clarification
 * (e.g. which campaign) so a short follow-up like "26/27" can resume it.
 */
public final class ChatPendingFollowUp {

	private static final Pattern CAMPAIGN_YY = Pattern.compile("\\b(\\d{2})\\s*[/-]\\s*(\\d{2})\\b");
	private static final Pattern CAMPAIGN_COMPACT = Pattern.compile("\\b(\\d{2})(\\d{2})\\b");
	private static final Pattern CAMPANIA_PHRASE = Pattern.compile(
			"campa[nñ]a\\s+[\"']?([^\"'\\n]+?)[\"']?\\s*$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	public static final String AWAIT_CAMPANIA = "campania";

	public record Pending(
			UrsulaAction action,
			String originalUserText,
			String campaniaName,
			String cultivoName,
			LocalDate beginDate,
			LocalDate endDate,
			String awaiting) {
	}

	private static volatile Pending pending;

	private ChatPendingFollowUp() {
	}

	public static void set(Pending value) {
		pending = value;
	}

	public static void clear() {
		pending = null;
	}

	public static Optional<Pending> get() {
		return Optional.ofNullable(pending);
	}

	public static boolean isAwaitingCampania() {
		return pending != null && AWAIT_CAMPANIA.equals(pending.awaiting());
	}

	/**
	 * If there is a pending clarification and {@code userText} answers it,
	 * returns a resumed {@link ParsedIntent}; otherwise empty.
	 */
	public static Optional<ParsedIntent> tryResume(String userText) {
		Pending current = pending;
		if (current == null || userText == null || userText.isBlank()) {
			return Optional.empty();
		}

		if (looksLikeNewIndependentRequest(userText)) {
			clear();
			return Optional.empty();
		}

		if (AWAIT_CAMPANIA.equals(current.awaiting())) {
			String campania = extractCampaniaReply(userText);
			if (campania == null || campania.isBlank()) {
				return Optional.empty();
			}
			clear();
			String mergedText = mergeOriginalWithCampania(current.originalUserText(), campania);
			ParsedIntent intent = new ParsedIntent(
					current.action(),
					null,
					1.0,
					"Continúo con la campaña " + campania + ".",
					campania,
					current.cultivoName(),
					current.beginDate(),
					current.endDate(),
					mergedText);
			return Optional.of(intent.enrichFromUserText(mergedText));
		}

		return Optional.empty();
	}

	public static void rememberNdviAsignacionNeedsCampania(AsignacionNdviRequest req, String originalUserText) {
		set(new Pending(
				UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES,
				originalUserText,
				req != null ? req.campaniaName() : null,
				req != null ? req.cultivoName() : null,
				req != null ? req.begin() : null,
				req != null ? req.end() : null,
				AWAIT_CAMPANIA));
	}

	static String extractCampaniaReply(String userText) {
		String text = userText.trim();
		Matcher phrase = CAMPANIA_PHRASE.matcher(text);
		if (phrase.find()) {
			return phrase.group(1).trim();
		}
		Matcher yy = CAMPAIGN_YY.matcher(text);
		if (yy.find()) {
			return yy.group(1) + "/" + yy.group(2);
		}
		Matcher compact = CAMPAIGN_COMPACT.matcher(text);
		if (compact.find()) {
			int a = Integer.parseInt(compact.group(1));
			int b = Integer.parseInt(compact.group(2));
			if (!(a == 20 && b >= 20) && (b == a + 1 || b == (a + 1) % 100)) {
				return compact.group(1) + "/" + compact.group(2);
			}
		}
		// Short replies that are only the campaign name already known in DB.
		if (text.length() <= 24 && !text.contains(" ")) {
			return text;
		}
		return null;
	}

	private static String mergeOriginalWithCampania(String original, String campania) {
		String base = original == null || original.isBlank()
				? "descargar ndvi de asignaciones"
				: original.trim();
		return base + " campaña " + campania;
	}

	private static boolean looksLikeNewIndependentRequest(String userText) {
		String n = AchievementIntentCatalog.normalize(userText);
		if (n.length() < 12) {
			return false;
		}
		return n.contains("importar") || n.contains("exportar") || n.contains("generar margen")
				|| n.contains("crear poligono") || n.contains("compartir") || n.contains("ayuda")
				|| (n.contains("descargar") && n.contains("ndvi") && n.length() > 40);
	}
}
