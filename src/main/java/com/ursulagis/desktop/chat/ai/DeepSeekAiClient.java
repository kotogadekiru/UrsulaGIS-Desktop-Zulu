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

/**
 * DeepSeek chat completions client (OpenAI-compatible API).
 */
public class DeepSeekAiClient implements AiClient {

	private static final Logger LOG = Logger.getLogger(DeepSeekAiClient.class.getName());
	private static final String API_URL = "https://api.deepseek.com/chat/completions";
	private static final String DEFAULT_MODEL = "deepseek-chat";

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(30))
			.build();

	@Override
	public AiProvider getProvider() {
		return AiProvider.DEEPSEEK;
	}

	@Override
	public AiResponse completePlain(String systemPrompt, String userPrompt) {
		String apiKey = AiApiKeys.deepSeek();
		if (apiKey.isBlank()) {
			throw new IllegalStateException(
					"DeepSeek API key not set. Set DEEPSEEK_API_KEY or the deepseek.api.key system property.");
		}

		long start = System.currentTimeMillis();
		try {
			String jsonBody = buildRequestBody(systemPrompt, userPrompt);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.timeout(Duration.ofSeconds(90))
					.build();

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			long elapsed = System.currentTimeMillis() - start;

			if (response.statusCode() >= 400) {
				throw new IOException("DeepSeek API error " + response.statusCode() + ": " + response.body());
			}

			String content = extractPlainAssistantContent(response.body());
			String model = extractModel(response.body());
			return new AiResponse(content, model, getProvider(), elapsed, false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("DeepSeek request interrupted.", e);
		} catch (IOException e) {
			throw new RuntimeException("DeepSeek request failed: " + e.getMessage(), e);
		}
	}

	@Override
	public AiResponse complete(String systemPrompt, String userPrompt) {
		String apiKey = AiApiKeys.deepSeek();
		if (apiKey.isBlank()) {
			throw new IllegalStateException(
					"DeepSeek API key not set. Set DEEPSEEK_API_KEY or the deepseek.api.key system property.");
		}

		long start = System.currentTimeMillis();
		try {
			String jsonBody = buildRequestBody(systemPrompt, userPrompt);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.timeout(Duration.ofSeconds(60))
					.build();

			LOG.info(() -> String.format("[DeepSeek] POST /chat/completions model=%s user=%d chars",
					DEFAULT_MODEL, userPrompt == null ? 0 : userPrompt.length()));

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			long elapsed = System.currentTimeMillis() - start;

			if (response.statusCode() >= 400) {
				throw new IOException("DeepSeek API error " + response.statusCode() + ": " + response.body());
			}

			String content = extractAssistantContent(response.body());
			String model = extractModel(response.body());
			LOG.fine(() -> "[DeepSeek] response: " + content);
			return new AiResponse(content, model, getProvider(), elapsed, false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("DeepSeek request interrupted.", e);
		} catch (IOException e) {
			throw new RuntimeException("DeepSeek request failed: " + e.getMessage(), e);
		}
	}

	private static String buildRequestBody(String systemPrompt, String userPrompt) {
		JsonObject body = new JsonObject();
		body.addProperty("model", DEFAULT_MODEL);
		body.addProperty("temperature", 0.1);

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

	private static String extractPlainAssistantContent(String responseBody) throws IOException {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			throw new IOException("DeepSeek response missing choices.");
		}
		return choices.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content")
				.getAsString();
	}

	private static String extractAssistantContent(String responseBody) throws IOException {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			throw new IOException("DeepSeek response missing choices.");
		}
		String content = choices.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content")
				.getAsString();
		return extractJsonPayload(content);
	}

	private static String extractModel(String responseBody) {
		JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
		if (root.has("model") && !root.get("model").isJsonNull()) {
			return root.get("model").getAsString();
		}
		return DEFAULT_MODEL;
	}

	public static String extractJsonPayload(String content) {
		if (content == null) {
			return "";
		}
		String trimmed = content.trim();
		if (trimmed.startsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			int endFence = trimmed.lastIndexOf("```");
			if (firstNewline > 0 && endFence > firstNewline) {
				trimmed = trimmed.substring(firstNewline + 1, endFence).trim();
			}
		}
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return trimmed.substring(start, end + 1);
		}
		return trimmed;
	}
}
