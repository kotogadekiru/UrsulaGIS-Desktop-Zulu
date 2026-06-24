package com.ursulagis.desktop.chat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ursulagis.desktop.dao.config.Configuracion;

class AiApiKeysTest {

	@Test
	@DisplayName("DeepSeek config key matches Chat settings namespace")
	void deepSeekConfigKey() {
		assertEquals("Chat.DEEPSEEK_API_KEY", AiApiKeys.DEEPSEEK_CONFIG_KEY);
	}

	@Test
	@DisplayName("saved config key is detected without prompting again")
	void readsSavedKeyFromConfig(@TempDir Path tempDir) throws Exception {
		Path configFile = tempDir.resolve("config.properties");
		Files.writeString(configFile, "");

		Configuracion config = newConfigAt(configFile.toString());
		AiApiKeys.saveDeepSeekToConfig(config, "sk-test-from-config");

		assertEquals("sk-test-from-config", AiApiKeys.readConfigValue(config, AiApiKeys.DEEPSEEK_CONFIG_KEY));
	}

	@Test
	@DisplayName("blank config value is not treated as configured")
	void blankConfigValueIsNotConfigured(@TempDir Path tempDir) throws Exception {
		Path configFile = tempDir.resolve("config.properties");
		Files.writeString(configFile, AiApiKeys.DEEPSEEK_CONFIG_KEY + "=\n");

		Configuracion config = newConfigAt(configFile.toString());

		assertEquals("", AiApiKeys.readConfigValue(config, AiApiKeys.DEEPSEEK_CONFIG_KEY));
	}

	private static Configuracion newConfigAt(String propertiesPath) throws Exception {
		Constructor<Configuracion> ctor = Configuracion.class.getDeclaredConstructor(String.class);
		ctor.setAccessible(true);
		return ctor.newInstance(propertiesPath);
	}
}
