package com.ursulagis.desktop.chat.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Resolves API keys from environment variables, JVM system properties,
 * {@code %APPDATA%/UrsulaGIS/ai-keys.properties}, or {@link Configuracion}.
 */
public final class AiApiKeys {

	private static final String DEEPSEEK_ENV = "DEEPSEEK_API_KEY";
	private static final String DEEPSEEK_PROPERTY = "deepseek.api.key";
	private static final String GITHUB_ENV = "GITHUB_TOKEN";
	private static final String GITHUB_PROPERTY = "github.token";
	private static final String AI_KEYS_FILE = "ai-keys.properties";

	public static final String DEEPSEEK_CONFIG_KEY = "Chat.DEEPSEEK_API_KEY";

	private AiApiKeys() {
	}

	public static String deepSeek() {
		return firstNonBlank(
				System.getenv(DEEPSEEK_ENV),
				System.getProperty(DEEPSEEK_PROPERTY),
				readFromAiKeysFile(DEEPSEEK_PROPERTY),
				readFromConfig(DEEPSEEK_CONFIG_KEY));
	}

	public static String github() {
		return firstNonBlank(
				System.getenv(GITHUB_ENV),
				System.getProperty(GITHUB_PROPERTY),
				readFromAiKeysFile(GITHUB_PROPERTY));
	}

	public static boolean hasAiKeysFile() {
		return aiKeysFile().isFile();
	}

	public static boolean hasDeepSeekKey() {
		return !deepSeek().isBlank();
	}

	public static boolean hasDeepSeekKeyInConfig() {
		Configuracion config = activeConfig();
		return config != null && !readConfigValue(config, DEEPSEEK_CONFIG_KEY).isBlank();
	}

	public static void saveDeepSeekToConfig(Configuracion config, String apiKey) {
		if (apiKey == null || apiKey.isBlank() || config == null) {
			return;
		}
		config.setProperty(DEEPSEEK_CONFIG_KEY, apiKey.trim());
		config.save();
	}

	private static File aiKeysFile() {
		return new File(Configuracion.ursulaGISFolder, AI_KEYS_FILE);
	}

	private static String readFromAiKeysFile(String key) {
		File file = aiKeysFile();
		if (!file.isFile()) {
			return "";
		}
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(file)) {
			props.load(in);
		} catch (IOException e) {
			return "";
		}
		String value = props.getProperty(key);
		return value == null ? "" : value.trim();
	}

	private static String readFromConfig(String key) {
		Configuracion config = activeConfig();
		if (config == null) {
			return "";
		}
		return readConfigValue(config, key);
	}

	static String readConfigValue(Configuracion config, String key) {
		String value = nonBlankConfigValue(config, key);
		if (!value.isEmpty()) {
			return value;
		}
		config.loadProperties();
		return nonBlankConfigValue(config, key);
	}

	private static String nonBlankConfigValue(Configuracion config, String key) {
		String value = config.getProperty(key);
		return value.isBlank() ? "" : value.trim();
	}

	private static Configuracion activeConfig() {
		try {
			if (com.ursulagis.desktop.gui.JFXMain.config != null) {
				return com.ursulagis.desktop.gui.JFXMain.config;
			}
		} catch (Exception ignored) {
			// JFXMain not initialized (tests, headless)
		}
		return Configuracion.getInstance();
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return "";
	}
}
