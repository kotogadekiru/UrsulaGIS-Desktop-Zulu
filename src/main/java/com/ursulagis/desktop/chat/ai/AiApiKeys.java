package com.ursulagis.desktop.chat.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Resolves API keys from environment variables, JVM system properties,
 * or {@code %APPDATA%/UrsulaGIS/ai-keys.properties}.
 */
public final class AiApiKeys {

	private static final String DEEPSEEK_ENV = "DEEPSEEK_API_KEY";
	private static final String DEEPSEEK_PROPERTY = "deepseek.api.key";
	private static final String GITHUB_ENV = "GITHUB_TOKEN";
	private static final String GITHUB_PROPERTY = "github.token";
	private static final String AI_KEYS_FILE = "ai-keys.properties";

	private AiApiKeys() {
	}

	public static String deepSeek() {
		return firstNonBlank(
				System.getenv(DEEPSEEK_ENV),
				System.getProperty(DEEPSEEK_PROPERTY),
				readFromAiKeysFile(DEEPSEEK_PROPERTY));
	}

	public static String github() {
		return firstNonBlank(
				System.getenv(GITHUB_ENV),
				System.getProperty(GITHUB_PROPERTY),
				readFromAiKeysFile(GITHUB_PROPERTY));
	}

	private static String readFromAiKeysFile(String key) {
		File file = new File(Configuracion.ursulaGISFolder, AI_KEYS_FILE);
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

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return "";
	}
}
