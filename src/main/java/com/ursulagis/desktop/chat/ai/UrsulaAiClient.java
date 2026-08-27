package com.ursulagis.desktop.chat.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Default Ursula GIS chat client. POSTs a DeepSeek-compatible payload to
 * {@code https://www.ursulagis.com/chat/completions}, which proxies to DeepSeek
 * with a server-side API key. Client auth uses the device {@code USER} from
 * {@link Configuracion} as the Bearer token. {@link #complete} requests JSON mode
 * and extracts intent JSON; {@link #completePlain} returns guidance text as-is.
 */
public class UrsulaAiClient implements AiClient {

	private static final Logger LOG = Logger.getLogger(UrsulaAiClient.class.getName());
	private static final String API_URL = "https://www.ursulagis.com/chat/completions";
	private static final String DEFAULT_MODEL = "deepseek-chat";
	private static final String USER_CONFIG_KEY = "USER";

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(30))
			.build();

	/** Identifies this client as the {@link AiProvider#URSULA} cloud proxy backend. */
	@Override
	public AiProvider getProvider() {
		return AiProvider.URSULA;
	}

	/**
	 * Requests free-form assistant text for step-by-step guidance (no JSON mode).
	 *
	 * @throws IllegalStateException if device {@code USER} is not configured
	 * @throws RuntimeException      if the HTTP call fails or is interrupted
	 */
	@Override
	public AiResponse completePlain(String systemPrompt, String userPrompt) {
		String userToken = resolveUserToken();
		long start = System.currentTimeMillis();
		try {
			String jsonBody = buildRequestBody(systemPrompt, userPrompt, userToken, false);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + userToken)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.timeout(Duration.ofSeconds(90))
					.build();

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			long elapsed = System.currentTimeMillis() - start;

			if (response.statusCode() >= 400) {
				throw new IOException("Ursula API error " + response.statusCode() + ": " + response.body());
			}

			String content = extractPlainAssistantContent(response.body());
			String model = extractModel(response.body());
			return new AiResponse(content, model, getProvider(), elapsed, false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Ursula request interrupted.", e);
		} catch (IOException e) {
			throw new RuntimeException("Ursula request failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Requests intent JSON ({@code response_format: json_object}) and returns the
	 * extracted object string for the chat intent parser.
	 *
	 * @throws IllegalStateException if device {@code USER} is not configured
	 * @throws RuntimeException      if the HTTP call fails or is interrupted
	 */
	@Override
	public AiResponse complete(String systemPrompt, String userPrompt) {
		String userToken = resolveUserToken();
		long start = System.currentTimeMillis();
		try {
			String jsonBody = buildRequestBody(systemPrompt, userPrompt, userToken, true);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + userToken)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.timeout(Duration.ofSeconds(60))
					.build();

			LOG.info(() -> String.format("[Ursula] POST /chat/completions model=%s user=%d chars",
					DEFAULT_MODEL, userPrompt == null ? 0 : userPrompt.length()));

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			long elapsed = System.currentTimeMillis() - start;

			if (response.statusCode() >= 400) {
				throw new IOException("Ursula API error " + response.statusCode() + ": " + response.body());
			}

			String content = extractAssistantContent(response.body());
			String model = extractModel(response.body());
			LOG.fine(() -> "[Ursula] response: " + content);
			return new AiResponse(content, model, getProvider(), elapsed, false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Ursula request interrupted.", e);
		} catch (IOException e) {
			throw new RuntimeException("Ursula request failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Resolves the device USER token used as Bearer auth for ursulagis.com.
	 *
	 * @throws IllegalStateException when USER is missing or a placeholder value
	 */
	private static String resolveUserToken() {
		Configuracion config = activeConfig();
		String user = config == null ? "" : config.getPropertyOrDefault(USER_CONFIG_KEY, "").trim();
		if (user.isBlank() || "nonefound".equalsIgnoreCase(user) || "number not set".equalsIgnoreCase(user)) {
			throw new IllegalStateException(
					"USER not set in configuration. Ursula GIS chat requires a configured device USER.");
		}
		return user;
	}

	/**
	 * Prefers {@code JFXMain.config} when the UI is up; otherwise
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

	/**
	 * Builds a DeepSeek-compatible body for the ursulagis.com proxy.
	 *
	 * @param jsonMode when {@code true}, request {@code response_format: json_object} (intent parsing)
	 */
	private static String buildRequestBody(String systemPrompt, String userPrompt, String userToken,
			boolean jsonMode) {
		JsonObject body = new JsonObject();
		body.addProperty("model", DEFAULT_MODEL);
		body.addProperty("stream", false);
		body.addProperty("temperature", 0.1);
		body.addProperty("user", userToken);

		JsonObject thinking = new JsonObject();
		thinking.addProperty("type", "disabled");
		body.add("thinking", thinking);

		if (jsonMode) {
			JsonObject responseFormat = new JsonObject();
			responseFormat.addProperty("type", "json_object");
			body.add("response_format", responseFormat);
		}

		JsonArray messages = new JsonArray();

		JsonObject system = new JsonObject();
		system.addProperty("role", "system");
		system.addProperty("content", systemPrompt == null ? "" : systemPrompt);
		messages.add(system);

		JsonObject user = new JsonObject();
		user.addProperty("role", "user");
		user.addProperty("content", userPrompt == null ? "" : userPrompt);
		messages.add(user);

		body.add("messages", messages);
		return body.toString();
	}

	/** Reads {@code choices[0].message.content} without post-processing. */
	private static String extractPlainAssistantContent(String responseBody) throws IOException {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			throw new IOException("Ursula response missing choices.");
		}
		return choices.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content")
				.getAsString();
	}

	/** Reads assistant content and normalizes it via {@link DeepSeekAiClient#extractJsonPayload}. */
	private static String extractAssistantContent(String responseBody) throws IOException {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			throw new IOException("Ursula response missing choices.");
		}
		String content = choices.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content")
				.getAsString();
		return DeepSeekAiClient.extractJsonPayload(content);
	}

	/** Model id from the API response, or {@link #DEFAULT_MODEL} if absent. */
	private static String extractModel(String responseBody) {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		if (root.has("model") && !root.get("model").isJsonNull()) {
			return root.get("model").getAsString();
		}
		return DEFAULT_MODEL;
	}
}
