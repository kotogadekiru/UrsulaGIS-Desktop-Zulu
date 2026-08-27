package com.ursulagis.desktop.chat.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Supported AI backends for Ursula IA chat (intent parsing and guidance).
 * {@link #DEEPSEEK} and {@link #URSULA} call live HTTP APIs;
 * {@link #OPENAI} and {@link #CLAUDE} currently use the local mock parser;
 * {@link #MOCK} is offline rule-based matching.
 */
public enum AiProvider {
	/** Local keyword/rules client; no network calls. */
	MOCK("Mock (local rules)"),
	/** OpenAI-branded option; currently mocked via {@link MockAiClient}. */
	OPENAI("ChatGPT (mocked)"),
	/** Anthropic Claude option; currently mocked via {@link MockAiClient}. */
	CLAUDE("Claude (mocked)"),
	/** Live DeepSeek chat completions API ({@code api.deepseek.com}). */
	DEEPSEEK("DeepSeek"),
	/** Ursula GIS hosted proxy at {@code ursulagis.com} (server-side DeepSeek key). */
	URSULA("Ursula GIS");

	private final String displayName;

	AiProvider(String displayName) {
		this.displayName = displayName;
	}

	/** Human-readable label for settings UI and diagnostics. */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Parses a stored provider id (enum name, case-insensitive).
	 *
	 * @param id value from configuration, e.g. {@code "URSULA"}
	 * @return matching provider, or empty if blank/unknown
	 */
	public static Optional<AiProvider> fromId(String id) {
		if (id == null || id.isBlank()) {
			return Optional.empty();
		}
		String normalized = id.trim().toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
				.filter(p -> p.name().equals(normalized))
				.findFirst();
	}
}
