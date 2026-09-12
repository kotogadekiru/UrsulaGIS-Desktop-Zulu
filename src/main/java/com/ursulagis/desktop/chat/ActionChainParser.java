package com.ursulagis.desktop.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Splits a user utterance into ordered chat actions so requests like
 * {@code "importar cosecha y compartirla"} or {@code "crear polígono luego importar suelo"}
 * can run as a chain.
 */
public final class ActionChainParser {

	private static final Pattern SPLIT = Pattern.compile(
			"\\s+(?:y\\s+luego|luego|despues|después|and\\s+then|then|y\\s+ademas|y\\s+además|y\\s+tambien|y\\s+también)\\s+",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPLIT_Y = Pattern.compile(
			"\\s+y\\s+",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private ActionChainParser() {
	}

	/**
	 * Parses {@code userText} into one or more {@link UrsulaAction}s using
	 * {@link AchievementIntentCatalog} per segment. Pronouns like {@code compartirla}
	 * inherit the domain of the previous action when possible.
	 *
	 * @return ordered actions (never empty — {@link UrsulaAction#UNKNOWN} if nothing matched)
	 */
	public static List<UrsulaAction> parse(String userText) {
		if (userText == null || userText.isBlank()) {
			return List.of(UrsulaAction.UNKNOWN);
		}

		List<String> segments = splitSegments(userText.trim());
		List<UrsulaAction> actions = new ArrayList<>();
		UrsulaAction previous = null;

		for (String segment : segments) {
			String enriched = enrichPronounSegment(segment, previous);
			var match = AchievementIntentCatalog.match(enriched);
			if (match.isEmpty() && !enriched.equals(segment)) {
				match = AchievementIntentCatalog.match(segment);
			}
			if (match.isPresent()) {
				UrsulaAction action = match.get().action();
				if (actions.isEmpty() || actions.get(actions.size() - 1) != action) {
					actions.add(action);
				}
				previous = action;
			}
		}

		if (actions.isEmpty()) {
			var whole = AchievementIntentCatalog.match(userText);
			if (whole.isPresent()) {
				return List.of(whole.get().action());
			}
			return List.of(UrsulaAction.UNKNOWN);
		}
		return List.copyOf(actions);
	}

	/** Splits on explicit sequence words; falls back to {@code y} when both sides match actions. */
	static List<String> splitSegments(String text) {
		String[] primary = SPLIT.split(text);
		if (primary.length > 1) {
			return nonBlank(primary);
		}

		String[] byY = SPLIT_Y.split(text);
		if (byY.length < 2) {
			return List.of(text);
		}

		List<String> usable = new ArrayList<>();
		for (String part : byY) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (AchievementIntentCatalog.match(trimmed).isPresent()
					|| looksLikePronounContinuation(trimmed)) {
				usable.add(trimmed);
			} else {
				// "mapa de cosecha vy1" style — keep as one phrase when mid parts don't match alone
				if (usable.isEmpty()) {
					usable.add(trimmed);
				} else {
					usable.set(usable.size() - 1, usable.get(usable.size() - 1) + " y " + trimmed);
				}
			}
		}
		if (usable.size() >= 2) {
			return usable;
		}
		return List.of(text);
	}

	private static List<String> nonBlank(String[] parts) {
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			if (p != null && !p.isBlank()) {
				out.add(p.trim());
			}
		}
		return out.isEmpty() ? List.of() : out;
	}

	private static boolean looksLikePronounContinuation(String segment) {
		String n = AchievementIntentCatalog.normalize(segment);
		return n.matches(".*(compartir|exportar|resumir|clonar|guardar|grillar|sumar|editar).*la")
				|| n.equals("compartirla")
				|| n.equals("exportarla")
				|| n.equals("resumirla")
				|| n.equals("clonarla")
				|| n.equals("guardarla")
				|| n.startsWith("compartir ")
				|| n.startsWith("exportar ")
				|| n.equals("compartir");
	}

	/**
	 * Turns {@code compartirla} into {@code compartir cosecha} when the previous
	 * action was a harvest import, and similarly for siembra/fert/pulv/recorrida.
	 */
	static String enrichPronounSegment(String segment, UrsulaAction previous) {
		if (previous == null) {
			return segment;
		}
		String n = AchievementIntentCatalog.normalize(segment);
		String verb = null;
		if (n.contains("compartir")) {
			verb = "compartir";
		} else if (n.contains("exportar")) {
			verb = "exportar";
		} else if (n.contains("resumir")) {
			verb = "resumir";
		} else if (n.contains("clonar")) {
			verb = "clonar";
		} else if (n.contains("guardar")) {
			verb = "guardar";
		} else if (n.contains("grillar")) {
			verb = "grillar";
		} else if (n.contains("sumar")) {
			verb = "sumar";
		} else if (n.contains("editar")) {
			verb = "editar";
		}
		if (verb == null) {
			return segment;
		}
		boolean pronoun = n.equals(verb)
				|| n.equals(verb + "la")
				|| n.equals(verb + "lo")
				|| n.endsWith(" " + verb + "la")
				|| n.equals(verb + " la")
				|| (n.startsWith(verb) && n.length() <= verb.length() + 4);
		if (!pronoun && !n.equals(verb)) {
			return segment;
		}
		String domain = domainNoun(previous);
		if (domain == null) {
			return segment;
		}
		return verb + " " + domain;
	}

	private static String domainNoun(UrsulaAction action) {
		String name = action.name().toLowerCase(Locale.ROOT);
		if (name.contains("cosecha") || name.contains("harvest")) {
			return "cosecha";
		}
		if (name.contains("siembra") || name.contains("seed")) {
			return "siembra";
		}
		if (name.contains("fertiliz")) {
			return "fertilizacion";
		}
		if (name.contains("pulver")) {
			return "pulverizacion";
		}
		if (name.contains("recorrida")) {
			return "recorrida";
		}
		if (name.contains("suelo") || name.contains("soil")) {
			return "suelo";
		}
		if (name.contains("margen")) {
			return "margen";
		}
		if (name.contains("ndvi")) {
			return "ndvi";
		}
		if (name.contains("poligono") || name.contains("polygon")) {
			return "poligono";
		}
		if (name.contains("labor")) {
			return "labor";
		}
		return null;
	}
}
