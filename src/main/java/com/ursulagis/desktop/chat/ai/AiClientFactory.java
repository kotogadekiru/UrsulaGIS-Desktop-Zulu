package com.ursulagis.desktop.chat.ai;

/**
 * Creates {@link AiClient} instances for the configured provider.
 * Central entry point used when opening Ursula IA chat so the rest of the
 * stack depends only on {@link AiClient}.
 */
public final class AiClientFactory {

	/** Prevents instantiation. */
	private AiClientFactory() {
	}

	/**
	 * Builds a client for the given provider enum value.
	 *
	 * @param provider backend to instantiate
	 * @return a ready-to-use client (mocked or HTTP-backed)
	 */
	public static AiClient create(AiProvider provider) {
		return switch (provider) {
			case OPENAI -> new OpenAiClient();
			case CLAUDE -> new ClaudeAiClient();
			case DEEPSEEK -> new DeepSeekAiClient();
			case URSULA -> new UrsulaAiClient();
			case MOCK -> new MockAiClient();
		};
	}

	/**
	 * Builds a client for the provider currently stored in {@link ChatAiSettings}.
	 *
	 * @return client for the configured provider (defaults to Ursula GIS)
	 */
	public static AiClient createConfigured() {
		return create(ChatAiSettings.resolveProvider());
	}
}
