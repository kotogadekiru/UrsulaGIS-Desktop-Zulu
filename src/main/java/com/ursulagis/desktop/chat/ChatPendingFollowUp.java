package com.ursulagis.desktop.chat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session-scoped memory for the last chat action that asked the user for a
 * clarification (e.g. which campaign). A short follow-up like {@code "26/27"}
 * can then resume the pending {@link UrsulaAction} without re-asking the whole request.
 */
public final class ChatPendingFollowUp {

	private static final Pattern CAMPAIGN_YY = Pattern.compile("\\b(\\d{2})\\s*[/-]\\s*(\\d{2})\\b");
	private static final Pattern CAMPAIGN_COMPACT = Pattern.compile("\\b(\\d{2})(\\d{2})\\b");
	private static final Pattern CAMPANIA_PHRASE = Pattern.compile(
			"campa[nñ]a\\s+[\"']?([^\"'\\n]+?)[\"']?\\s*$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	/** {@link Pending#awaiting()} value when Ursula is waiting for a campaign name. */
	public static final String AWAIT_CAMPANIA = "campania";

	/**
	 * Snapshot of a paused action waiting for user clarification.
	 *
	 * @param action           action to resume
	 * @param originalUserText full original request text
	 * @param campaniaName     campaign already known, if any
	 * @param cultivoName      crop already known, if any
	 * @param beginDate        NDVI begin already known, if any
	 * @param endDate          NDVI end already known, if any
	 * @param awaiting         what is missing ({@link #AWAIT_CAMPANIA}, …)
	 */
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

	/** Prevents instantiation. */
	private ChatPendingFollowUp() {
	}

	/** Stores or replaces the current pending clarification. */
	public static void set(Pending value) {
		pending = value;
	}

	/** Clears any pending follow-up (after resume or a new independent request). */
	public static void clear() {
		pending = null;
	}

	/** Current pending clarification, if any. */
	public static Optional<Pending> get() {
		return Optional.ofNullable(pending);
	}

	/** Whether Ursula is waiting specifically for a campaign reply. */
	public static boolean isAwaitingCampania() {
		return pending != null && AWAIT_CAMPANIA.equals(pending.awaiting());
	}

	/**
	 * If there is a pending clarification and {@code userText} answers it,
	 * returns a resumed {@link ParsedIntent}; otherwise empty.
	 * Clears the pending state on successful resume or when the text looks like a new request.
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

	/**
	 * Remembers an NDVI-asignaciones download that still needs the user to pick a campaign.
	 */
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

	/**
	 * Extracts a campaign token from a short follow-up reply ({@code 26/27},
	 * {@code campaña …}, or a single short name).
	 */
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

	/** Appends the campaign token onto the original request text for re-parsing. */
	private static String mergeOriginalWithCampania(String original, String campania) {
		String base = original == null || original.isBlank()
				? "descargar ndvi de asignaciones"
				: original.trim();
		return base + " campaña " + campania;
	}

	/** Detects a full new command so we abandon the pending clarification. */
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
