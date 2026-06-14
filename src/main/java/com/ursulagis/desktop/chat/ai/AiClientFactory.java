package com.ursulagis.desktop.chat.ai;

/**
 * Creates {@link AiClient} instances for the configured provider.
 */
public final class AiClientFactory {

	private AiClientFactory() {
	}

	public static AiClient create(AiProvider provider) {
		return switch (provider) {
			case OPENAI -> new OpenAiClient();
			case CLAUDE -> new ClaudeAiClient();
			case DEEPSEEK -> new DeepSeekAiClient();
			case MOCK -> new MockAiClient();
		};
	}

	public static AiClient createConfigured() {
		return create(ChatAiSettings.resolveProvider());
	}
}
