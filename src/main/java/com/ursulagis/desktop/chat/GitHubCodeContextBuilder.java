package com.ursulagis.desktop.chat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ursulagis.desktop.chat.ai.AiApiKeys;

/**
 * Fetches relevant source snippets from the public GitHub repository (and local workspace fallback)
 * to ground LLM answers in real application code.
 */
public final class GitHubCodeContextBuilder {

	private static final Logger LOG = Logger.getLogger(GitHubCodeContextBuilder.class.getName());
	private static final int MAX_FILES = 4;
	private static final int MAX_CHARS_PER_FILE = 3500;
	private static final int MAX_TOTAL_CHARS = 14000;

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	private static final Map<String, List<String>> KEYWORD_PATHS = Map.ofEntries(
			Map.entry("poligono", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/PoligonoGUIController.java")),
			Map.entry("polígono", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/PoligonoGUIController.java")),
			Map.entry("polygon", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/PoligonoGUIController.java")),
			Map.entry("cosecha", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/CosechaGUIController.java")),
			Map.entry("harvest", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/CosechaGUIController.java")),
			Map.entry("ndvi", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/NdviGUIController.java")),
			Map.entry("recorrida", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/RecorridaGUIController.java")),
			Map.entry("suelo", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/SueloGUIController.java")),
			Map.entry("soil", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/SueloGUIController.java")),
			Map.entry("siembra", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/SiembraGUIController.java",
					"src/main/java/com/ursulagis/desktop/chat/SiembraFertilizadaWorkflowGuide.java")),
			Map.entry("fertiliz", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/FertilizacionGUIController.java",
					"src/main/java/com/ursulagis/desktop/gui/controller/CosechaGUIController.java")),
			Map.entry("pulveriz", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/PulverizacionGUIController.java")),
			Map.entry("margen", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/ConfigGUI.java",
					"src/main/java/com/ursulagis/desktop/gui/controller/MargenGUIController.java")),
			Map.entry("rentabilidad", List.of(
					"src/main/java/com/ursulagis/desktop/gui/controller/ConfigGUI.java")),
			Map.entry("chat", List.of(
					"src/main/java/com/ursulagis/desktop/chat/ChatActionExecutor.java",
					"src/main/java/com/ursulagis/desktop/chat/AchievementIntentCatalog.java")),
			Map.entry("capa", List.of(
					"src/main/java/com/ursulagis/desktop/gui/nww/LayerPanel.java")),
			Map.entry("rama", List.of(
					"src/main/java/com/ursulagis/desktop/gui/nww/LayerPanel.java")),
			Map.entry("activar", List.of(
					"src/main/java/com/ursulagis/desktop/gui/nww/LayerPanel.java")),
			Map.entry("logro", List.of(
					"src/main/java/com/ursulagis/desktop/gui/onboarding/OnboardingAchievements.java",
					"src/main/java/com/ursulagis/desktop/chat/AchievementIntentCatalog.java")));

	private static final List<String> DEFAULT_PATHS = List.of(
			"chatbotReadme.md",
			"src/main/java/com/ursulagis/desktop/gui/controller/ConfigGUI.java",
			"src/main/java/com/ursulagis/desktop/gui/nww/LayerPanel.java",
			"src/main/java/com/ursulagis/desktop/chat/ChatActionExecutor.java",
			"src/main/java/com/ursulagis/desktop/chat/AchievementIntentCatalog.java",
			"src/main/java/com/ursulagis/desktop/gui/onboarding/OnboardingAchievements.java");

	private GitHubCodeContextBuilder() {
	}

	public static String buildForQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return "";
		}
		Set<String> paths = new LinkedHashSet<>();
		paths.addAll(pathsFromKeywords(userQuery));
		paths.addAll(searchGitHubCode(userQuery));
		if (paths.isEmpty()) {
			paths.addAll(DEFAULT_PATHS);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Repository: https://github.com/")
				.append(GitHubRepoConfig.OWNER).append('/').append(GitHubRepoConfig.REPO)
				.append(" (branch ").append(GitHubRepoConfig.BRANCH).append(")\n\n");

		int total = 0;
		int filesAdded = 0;
		for (String path : paths) {
			if (filesAdded >= MAX_FILES || total >= MAX_TOTAL_CHARS) {
				break;
			}
			String content = fetchFile(path);
			if (content.isBlank()) {
				continue;
			}
			String snippet = truncate(content, MAX_CHARS_PER_FILE);
			sb.append("--- ").append(path).append(" ---\n").append(snippet).append("\n\n");
			total += snippet.length();
			filesAdded++;
		}
		return sb.toString().trim();
	}

	private static List<String> pathsFromKeywords(String userQuery) {
		String normalized = AchievementIntentCatalog.normalize(userQuery);
		List<String> paths = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : KEYWORD_PATHS.entrySet()) {
			if (normalized.contains(entry.getKey())) {
				paths.addAll(entry.getValue());
			}
		}
		return paths;
	}

	private static List<String> searchGitHubCode(String userQuery) {
		List<String> paths = new ArrayList<>();
		String token = AiApiKeys.github();
		String q = extractSearchTerms(userQuery);
		if (q.isBlank()) {
			return paths;
		}
		String encoded = URLEncoder.encode(
				q + " repo:" + GitHubRepoConfig.OWNER + "/" + GitHubRepoConfig.REPO,
				StandardCharsets.UTF_8);
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create("https://api.github.com/search/code?q=" + encoded + "&per_page=5"))
					.timeout(Duration.ofSeconds(20))
					.header("Accept", "application/vnd.github+json")
					.GET();
			if (!token.isBlank()) {
				builder.header("Authorization", "Bearer " + token);
			}
			HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 400) {
				LOG.fine(() -> "GitHub code search HTTP " + response.statusCode());
				return paths;
			}
			JsonArray items = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("items");
			if (items == null) {
				return paths;
			}
			for (int i = 0; i < items.size(); i++) {
				JsonObject item = items.get(i).getAsJsonObject();
				if (item.has("path")) {
					paths.add(item.get("path").getAsString());
				}
			}
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOG.fine(() -> "GitHub code search failed: " + e.getMessage());
		}
		return paths;
	}

	private static String extractSearchTerms(String userQuery) {
		return Pattern.compile("\\s+").splitAsStream(AchievementIntentCatalog.normalize(userQuery))
				.filter(w -> w.length() >= 4)
				.limit(4)
				.reduce((a, b) -> a + " " + b)
				.orElse("");
	}

	private static String fetchFile(String repoPath) {
		String local = readLocalWorkspace(repoPath);
		if (!local.isBlank()) {
			return local;
		}
		return fetchFromGitHubRaw(repoPath);
	}

	private static String readLocalWorkspace(String repoPath) {
		Path path = Path.of(System.getProperty("user.dir", "."), repoPath);
		if (!Files.isRegularFile(path)) {
			return "";
		}
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	private static String fetchFromGitHubRaw(String repoPath) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(GitHubRepoConfig.RAW_BASE + repoPath))
					.timeout(Duration.ofSeconds(20))
					.GET()
					.build();
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				return response.body();
			}
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOG.fine(() -> "GitHub raw fetch failed for " + repoPath + ": " + e.getMessage());
		}
		return "";
	}

	private static String truncate(String content, int maxChars) {
		if (content.length() <= maxChars) {
			return content;
		}
		return content.substring(0, maxChars) + "\n... [truncated]";
	}
}
