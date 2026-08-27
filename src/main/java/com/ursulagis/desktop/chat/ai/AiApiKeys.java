package com.ursulagis.desktop.chat.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Resolves API keys and tokens used by the chat AI stack.
 * Looks up values in order from environment variables, JVM system properties,
 * {@code %APPDATA%/UrsulaGIS/ai-keys.properties}, and (for DeepSeek) {@link Configuracion}.
 */
public final class AiApiKeys {

	private static final String DEEPSEEK_ENV = "DEEPSEEK_API_KEY";
	private static final String DEEPSEEK_PROPERTY = "deepseek.api.key";
	private static final String GITHUB_ENV = "GITHUB_TOKEN";
	private static final String GITHUB_PROPERTY = "github.token";
	private static final String AI_KEYS_FILE = "ai-keys.properties";

	/** Configuracion property key for the DeepSeek API key. */
	public static final String DEEPSEEK_CONFIG_KEY = "Chat.DEEPSEEK_API_KEY";

	/** Prevents instantiation. */
	private AiApiKeys() {
	}

	/**
	 * Returns the DeepSeek API key from the first non-blank source found.
	 *
	 * @return trimmed key, or an empty string if none is configured
	 */
	public static String deepSeek() {
		return firstNonBlank(
				System.getenv(DEEPSEEK_ENV),
				System.getProperty(DEEPSEEK_PROPERTY),
				readFromAiKeysFile(DEEPSEEK_PROPERTY),
				readFromConfig(DEEPSEEK_CONFIG_KEY));
	}

	/**
	 * Returns the GitHub token used for optional code-context features.
	 *
	 * @return trimmed token, or an empty string if none is configured
	 */
	public static String github() {
		return firstNonBlank(
				System.getenv(GITHUB_ENV),
				System.getProperty(GITHUB_PROPERTY),
				readFromAiKeysFile(GITHUB_PROPERTY));
	}

	/**
	 * @return {@code true} if {@code ai-keys.properties} exists under the UrsulaGIS folder
	 */
	public static boolean hasAiKeysFile() {
		return aiKeysFile().isFile();
	}

	/**
	 * @return {@code true} if a DeepSeek key is available from any lookup source
	 */
	public static boolean hasDeepSeekKey() {
		return !deepSeek().isBlank();
	}

	/**
	 * @return {@code true} if the active {@link Configuracion} already stores a DeepSeek key
	 */
	public static boolean hasDeepSeekKeyInConfig() {
		Configuracion config = activeConfig();
		return config != null && !readConfigValue(config, DEEPSEEK_CONFIG_KEY).isBlank();
	}

	/**
	 * Persists a DeepSeek API key into the given configuration and saves it.
	 * No-op when {@code apiKey} or {@code config} is null/blank.
	 */
	public static void saveDeepSeekToConfig(Configuracion config, String apiKey) {
		if (apiKey == null || apiKey.isBlank() || config == null) {
			return;
		}
		config.setProperty(DEEPSEEK_CONFIG_KEY, apiKey.trim());
		config.save();
	}

	/** File path for optional local key storage: {@code UrsulaGIS/ai-keys.properties}. */
	private static File aiKeysFile() {
		return new File(Configuracion.ursulaGISFolder, AI_KEYS_FILE);
	}

	/** Reads a single property from the local AI keys file, or {@code ""} if missing. */
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

	/** Reads a config property via the active {@link Configuracion} instance. */
	private static String readFromConfig(String key) {
		Configuracion config = activeConfig();
		if (config == null) {
			return "";
		}
		return readConfigValue(config, key);
	}

	/**
	 * Returns a non-blank config value, reloading properties once if the first read is empty.
	 */
	static String readConfigValue(Configuracion config, String key) {
		String value = nonBlankConfigValue(config, key);
		if (!value.isEmpty()) {
			return value;
		}
		config.loadProperties();
		return nonBlankConfigValue(config, key);
	}

	/** Trimmed config property, or {@code ""} when blank/missing. */
	private static String nonBlankConfigValue(Configuracion config, String key) {
		String value = config.getProperty(key);
		return value.isBlank() ? "" : value.trim();
	}

	/**
	 * Prefers {@code JFXMain.config} when the UI is up; otherwise falls back to
	 * {@link Configuracion#getInstance()}.
	 */
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

	/** First trimmed non-blank argument, or {@code ""} if all are blank/null. */
	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return "";
	}
}
