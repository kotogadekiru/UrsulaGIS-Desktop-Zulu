package com.ursulagis.desktop.chat.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Supported AI backends for intent parsing.
 * Production implementations would call real APIs; current clients are mocked.
 */
public enum AiProvider {
	MOCK("Mock (local rules)"),
	OPENAI("ChatGPT (mocked)"),
	CLAUDE("Claude (mocked)"),
	DEEPSEEK("DeepSeek");

	private final String displayName;

	AiProvider(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

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
