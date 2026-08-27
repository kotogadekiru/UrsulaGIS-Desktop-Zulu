package com.ursulagis.desktop.chat.ai;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Resolves which {@link AiProvider} Ursula IA chat should use.
 * Reads {@link #AI_PROVIDER_KEY} from {@link Configuracion} and defaults to
 * {@link AiProvider#URSULA} when unset or unrecognized.
 */
public final class ChatAiSettings {

	/** {@link Configuracion} property holding the selected provider enum name. */
	public static final String AI_PROVIDER_KEY = "Chat.AI_PROVIDER";

	/** Prevents instantiation. */
	private ChatAiSettings() {
	}

	/**
	 * Returns the configured chat AI provider, or Ursula GIS when missing/invalid.
	 *
	 * @return non-null provider used by {@link AiClientFactory#createConfigured()}
	 */
	public static AiProvider resolveProvider() {
		Configuracion config = Configuracion.getInstance();
		if (config == null || !config.containsKey(AI_PROVIDER_KEY)) {
			return AiProvider.URSULA;
		}
		String value = config.getPropertyOrDefault(AI_PROVIDER_KEY, AiProvider.URSULA.getDisplayName()).trim();
		if (value.isBlank()) {
			return AiProvider.URSULA;
		}
		return AiProvider.fromId(value).orElse(AiProvider.URSULA);
	}
}
