package com.ursulagis.desktop.chat.ai;

/**
 * Supported AI backends for intent parsing.
 * Production implementations would call real APIs; current clients are mocked.
 */
public enum AiProvider {
	MOCK("Mock (local rules)"),
	OPENAI("ChatGPT (mocked)"),
	CLAUDE("Claude (mocked)");

	private final String displayName;

	AiProvider(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
