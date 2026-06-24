package com.ursulagis.desktop.gui.chat;

import java.util.Optional;

import com.ursulagis.desktop.chat.ai.AiApiKeys;
import com.ursulagis.desktop.chat.ai.AiProvider;
import com.ursulagis.desktop.chat.ai.ChatAiSettings;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.JFXMain;

import javafx.stage.Window;

/**
 * Ensures a DeepSeek API key is available, prompting the user when needed.
 */
public final class DeepSeekApiKeyHelper {

	private DeepSeekApiKeyHelper() {
	}

	public static boolean isDeepSeekProvider() {
		return ChatAiSettings.resolveProvider() == AiProvider.DEEPSEEK;
	}

	public static boolean ensureConfigured(Window owner) {
		if (!isDeepSeekProvider() || AiApiKeys.hasDeepSeekKey()) {
			return true;
		}
		Optional<String> entered = DeepSeekApiKeyDialog.prompt(owner);
		if (entered.isEmpty() || entered.get().isBlank()) {
			return false;
		}
		Configuracion config = JFXMain.config != null ? JFXMain.config : Configuracion.getInstance();
		AiApiKeys.saveDeepSeekToConfig(config, entered.get());
		return true;
	}
}
