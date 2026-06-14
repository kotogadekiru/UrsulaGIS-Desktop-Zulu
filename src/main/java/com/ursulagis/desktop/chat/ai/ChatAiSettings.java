package com.ursulagis.desktop.chat.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Resolves the Ursula IA chat provider from {@link Configuracion}.
 */
public final class ChatAiSettings {

	public static final String AI_PROVIDER_KEY = "Chat.AI_PROVIDER";

	private ChatAiSettings() {
	}

	public static AiProvider resolveProvider() {
		Configuracion config = Configuracion.getInstance();
		if (config == null || !config.containsKey(AI_PROVIDER_KEY)) {
			return AiProvider.DEEPSEEK;
		}
		String value = config.getPropertyOrDefault(AI_PROVIDER_KEY, "").trim();
		if (value.isBlank()) {
			return AiProvider.DEEPSEEK;
		}
		return AiProvider.fromId(value).orElse(AiProvider.DEEPSEEK);
	}
}
