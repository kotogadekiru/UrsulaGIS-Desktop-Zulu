package com.ursulagis.desktop.chat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Loads Ursula GIS PDF manuals and video-tutorial transcript .txt files from docs/,
 * then selects query-relevant excerpts for chat prompts. Transcript filenames describe the flow.
 */
public final class ManualContextBuilder {

	private static final Logger LOG = Logger.getLogger(ManualContextBuilder.class.getName());
	private static final String CLASSPATH_PREFIX = "com/ursulagis/desktop/docs/";
	private static final String TUTORIAL_INDEX = "tutorial-index.txt";
	private static final int MAX_SOURCES = 3;
	private static final int MAX_CHARS_PER_SOURCE = 3200;
	private static final int MAX_TOTAL_CHARS = 7500;
	private static final int TUTORIAL_FILENAME_BOOST = 16;
	private static final int FULL_TRANSCRIPT_LIMIT = 4000;
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern CAMEL_CASE = Pattern.compile("(?<=[a-záéíóúñ])(?=[A-ZÁÉÍÓÚÑ])");

	private static final Map<String, String> TEXT_CACHE = new ConcurrentHashMap<>();
	private static volatile List<ManualSource> TUTORIAL_SOURCES;

	private enum SourceKind {
		PDF, VIDEO_TRANSCRIPT
	}

	private record ManualSource(String filename, String title, List<String> keywords, int topicBoost, SourceKind kind) {
	}

	private record ScoredParagraph(String text, double score) {
	}

	private record ScoredManual(ManualSource manual, double score) {
	}

	private static final List<ManualSource> PDF_MANUALS = List.of(
			new ManualSource("ModoDeUso_0.2.18.pdf", "Modo de uso Ursula GIS",
					List.of("uso", "menu", "importar", "exportar", "capa", "poligono", "cosecha", "siembra",
							"herramienta", "configur", "interfaz", "ayuda", "trabajar"),
					1, SourceKind.PDF),
			new ManualSource("Instructivo_0.2.18.pdf", "Instructivo Ursula GIS",
					List.of("instructivo", "tutorial", "como", "paso", "empezar", "instal", "primer", "guia"),
					1, SourceKind.PDF),
			new ManualSource("MapaDeMargenesUrsulaGIS.pdf", "Mapa de márgenes",
					List.of("margen", "margenes", "rentabilidad", "rentabilidades", "costo", "ganancia", "utilidad"),
					12, SourceKind.PDF),
			new ManualSource("ObtenerNDVI.pdf", "Obtener NDVI",
					List.of("ndvi", "sentinel", "satelite", "indice", "vegetacion", "imagen", "descargar"),
					12, SourceKind.PDF),
			new ManualSource("AmbRefert.pdf", "Ambientes y fertilización",
					List.of("ambiente", "fertiliz", "refert", "fosforo", "nitrogeno", "recomend", "reposicion"),
					12, SourceKind.PDF));

	private ManualContextBuilder() {
	}

	public static String buildForQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return "";
		}
		String normalized = AchievementIntentCatalog.normalize(userQuery);
		List<ManualSource> ranked = rankSources(normalized);
		if (ranked.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Official Ursula GIS documentation (PDF manuals and video tutorial transcripts):\n\n");
		int total = sb.length();
		boolean addedExcerpt = false;

		for (ManualSource source : ranked) {
			if (total >= MAX_TOTAL_CHARS) {
				break;
			}
			String fullText = loadDocumentText(source.filename());
			if (fullText.isBlank()) {
				continue;
			}
			int budget = Math.min(MAX_CHARS_PER_SOURCE, MAX_TOTAL_CHARS - total);
			String excerpt = selectContent(source, fullText, normalized, budget);
			if (excerpt.isBlank()) {
				continue;
			}
			sb.append("--- ").append(source.title()).append(" (").append(source.filename()).append(") ---\n");
			sb.append(excerpt).append("\n\n");
			total = sb.length();
			addedExcerpt = true;
		}

		return addedExcerpt ? sb.toString().trim() : "";
	}

	static String titleFromFilename(String filename) {
		String base = stripExtension(filename);
		String spaced = CAMEL_CASE.matcher(base).replaceAll(" ");
		spaced = spaced.replace('_', ' ').replace('-', ' ').trim();
		if (spaced.isBlank()) {
			return "Videotutorial";
		}
		return capitalizeWords(spaced) + " (videotutorial)";
	}

	static List<String> keywordsFromFilename(String filename) {
		String base = AchievementIntentCatalog.normalize(stripExtension(filename));
		Set<String> keywords = new LinkedHashSet<>();
		if (!base.isBlank()) {
			keywords.add(base);
		}
		for (String part : base.split("[_\\-\\s]+")) {
			if (part.length() >= 3) {
				keywords.add(part);
			}
		}
		String spaced = CAMEL_CASE.matcher(stripExtension(filename)).replaceAll(" ");
		for (String part : AchievementIntentCatalog.normalize(spaced).split("\\s+")) {
			if (part.length() >= 3) {
				keywords.add(part);
			}
		}
		return List.copyOf(keywords);
	}

	private static List<ManualSource> rankSources(String normalizedQuery) {
		boolean genericHelp = normalizedQuery.contains("ayuda")
				|| normalizedQuery.contains("como usar")
				|| normalizedQuery.contains("manual")
				|| normalizedQuery.contains("instructivo")
				|| normalizedQuery.contains("videotutorial")
				|| normalizedQuery.contains("tutorial")
				|| normalizedQuery.length() < 12;

		List<ManualSource> all = new ArrayList<>(PDF_MANUALS);
		all.addAll(discoverTutorialSources());

		List<ScoredManual> scored = new ArrayList<>();
		for (ManualSource source : all) {
			double score = scoreSource(normalizedQuery, source);
			if (genericHelp && source.kind() == SourceKind.PDF
					&& (source.filename().startsWith("ModoDeUso") || source.filename().startsWith("Instructivo"))) {
				score = Math.max(score, 4.0);
			}
			if (score > 0) {
				scored.add(new ScoredManual(source, score));
			}
		}
		scored.sort(Comparator.comparingDouble(ScoredManual::score).reversed());
		return scored.stream()
				.limit(MAX_SOURCES)
				.map(ScoredManual::manual)
				.toList();
	}

	private static double scoreSource(String normalizedQuery, ManualSource source) {
		double score = 0;
		for (String keyword : source.keywords()) {
			if (normalizedQuery.contains(keyword)) {
				score += source.topicBoost();
			}
		}
		if (source.kind() == SourceKind.VIDEO_TRANSCRIPT) {
			for (String keyword : keywordsFromFilename(source.filename())) {
				if (normalizedQuery.contains(keyword)) {
					score += TUTORIAL_FILENAME_BOOST;
				}
			}
		}
		return score;
	}

	private static List<ManualSource> discoverTutorialSources() {
		List<ManualSource> cached = TUTORIAL_SOURCES;
		if (cached != null) {
			return cached;
		}
		synchronized (ManualContextBuilder.class) {
			if (TUTORIAL_SOURCES != null) {
				return TUTORIAL_SOURCES;
			}
			Set<String> filenames = new LinkedHashSet<>();
			collectTutorialFilenamesFromDisk(filenames);
			collectTutorialFilenamesFromIndex(filenames);
			List<ManualSource> sources = new ArrayList<>();
			for (String filename : filenames) {
				if (!filename.toLowerCase(Locale.ROOT).endsWith(".txt")
						|| TUTORIAL_INDEX.equals(filename)) {
					continue;
				}
				List<String> keywords = new ArrayList<>(keywordsFromFilename(filename));
				sources.add(new ManualSource(
						filename,
						titleFromFilename(filename),
						keywords,
						10,
						SourceKind.VIDEO_TRANSCRIPT));
			}
			TUTORIAL_SOURCES = List.copyOf(sources);
			return TUTORIAL_SOURCES;
		}
	}

	private static void collectTutorialFilenamesFromDisk(Set<String> filenames) {
		Path docsDir = Path.of(System.getProperty("user.dir", "."), "docs");
		if (!Files.isDirectory(docsDir)) {
			return;
		}
		try (Stream<Path> stream = Files.list(docsDir)) {
			stream.map(path -> path.getFileName().toString())
					.filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".txt"))
					.filter(name -> !TUTORIAL_INDEX.equals(name))
					.forEach(filenames::add);
		} catch (IOException e) {
			LOG.fine(() -> "Failed to list tutorial transcripts in docs/: " + e.getMessage());
		}
	}

	private static void collectTutorialFilenamesFromIndex(Set<String> filenames) {
		try (InputStream in = ManualContextBuilder.class.getClassLoader()
				.getResourceAsStream(CLASSPATH_PREFIX + TUTORIAL_INDEX)) {
			if (in == null) {
				return;
			}
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
				String name = line.trim();
				if (!name.isBlank() && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
					filenames.add(name);
				}
			}
		} catch (IOException e) {
			LOG.fine(() -> "Failed to read tutorial index: " + e.getMessage());
		}
	}

	static String loadDocumentText(String filename) {
		return TEXT_CACHE.computeIfAbsent(filename, ManualContextBuilder::readDocumentText);
	}

	/** @deprecated use {@link #loadDocumentText(String)} */
	@Deprecated
	static String loadManualText(String filename) {
		return loadDocumentText(filename);
	}

	private static String readDocumentText(String filename) {
		if (filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			return readPlainText(filename);
		}
		return readPdfText(filename);
	}

	private static String readPlainText(String filename) {
		try (InputStream in = openDocumentStream(filename)) {
			if (in == null) {
				return "";
			}
			return cleanText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOG.fine(() -> "Failed to read transcript " + filename + ": " + e.getMessage());
			return "";
		}
	}

	private static String readPdfText(String filename) {
		try (InputStream in = openDocumentStream(filename)) {
			if (in == null) {
				return "";
			}
			try (PDDocument document = Loader.loadPDF(in.readAllBytes())) {
				PDFTextStripper stripper = new PDFTextStripper();
				stripper.setSortByPosition(true);
				return cleanText(stripper.getText(document));
			}
		} catch (IOException e) {
			LOG.fine(() -> "Failed to read manual " + filename + ": " + e.getMessage());
			return "";
		}
	}

	private static InputStream openDocumentStream(String filename) throws IOException {
		Path local = Path.of(System.getProperty("user.dir", "."), "docs", filename);
		if (Files.isRegularFile(local)) {
			return Files.newInputStream(local);
		}
		return ManualContextBuilder.class.getClassLoader().getResourceAsStream(CLASSPATH_PREFIX + filename);
	}

	private static String selectContent(ManualSource source, String fullText, String normalizedQuery, int maxChars) {
		if (source.kind() == SourceKind.VIDEO_TRANSCRIPT && fullText.length() <= FULL_TRANSCRIPT_LIMIT) {
			return truncate(fullText, maxChars);
		}
		return selectExcerpts(fullText, normalizedQuery, maxChars, allKeywordHints(source));
	}

	static String selectExcerpts(String fullText, String normalizedQuery, int maxChars) {
		return selectExcerpts(fullText, normalizedQuery, maxChars, List.of());
	}

	private static String selectExcerpts(
			String fullText, String normalizedQuery, int maxChars, List<String> extraKeywords) {
		if (fullText == null || fullText.isBlank()) {
			return "";
		}
		String[] paragraphs = fullText.split("\\n{2,}");
		List<ScoredParagraph> scored = new ArrayList<>();
		for (String paragraph : paragraphs) {
			String cleaned = cleanText(paragraph);
			if (cleaned.length() < 40) {
				continue;
			}
			double score = paragraphScore(normalizedQuery, cleaned, extraKeywords);
			if (score > 0) {
				scored.add(new ScoredParagraph(cleaned, score));
			}
		}
		scored.sort(Comparator.comparingDouble(ScoredParagraph::score).reversed());

		StringBuilder sb = new StringBuilder();
		for (ScoredParagraph paragraph : scored) {
			if (sb.length() + paragraph.text().length() + 2 > maxChars) {
				break;
			}
			sb.append(paragraph.text()).append("\n\n");
		}
		if (!sb.isEmpty()) {
			return sb.toString().trim();
		}
		return truncate(fullText, maxChars);
	}

	private static List<String> allKeywordHints(ManualSource source) {
		List<String> hints = new ArrayList<>(source.keywords());
		if (source.kind() == SourceKind.VIDEO_TRANSCRIPT) {
			hints.addAll(keywordsFromFilename(source.filename()));
		}
		return hints;
	}

	private static double paragraphScore(String normalizedQuery, String paragraph, List<String> extraKeywords) {
		String normalizedParagraph = AchievementIntentCatalog.normalize(paragraph);
		double score = 0;
		for (String token : normalizedQuery.split("\\s+")) {
			if (token.length() < 4) {
				continue;
			}
			if (normalizedParagraph.contains(token)) {
				score += token.length() >= 6 ? 2.0 : 1.0;
			}
		}
		for (String keyword : extraKeywords) {
			if (normalizedQuery.contains(keyword) && normalizedParagraph.contains(keyword)) {
				score += 8.0;
			}
		}
		for (ManualSource manual : PDF_MANUALS) {
			for (String keyword : manual.keywords()) {
				if (normalizedQuery.contains(keyword) && normalizedParagraph.contains(keyword)) {
					score += manual.topicBoost();
				}
			}
		}
		return score;
	}

	private static String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot > 0 ? filename.substring(0, dot) : filename;
	}

	private static String capitalizeWords(String text) {
		String[] words = text.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				sb.append(word.substring(1));
			}
		}
		return sb.toString();
	}

	private static String cleanText(String text) {
		if (text == null) {
			return "";
		}
		return WHITESPACE.matcher(text.replace('\r', '\n').trim()).replaceAll(" ");
	}

	private static String truncate(String content, int maxChars) {
		if (content.length() <= maxChars) {
			return content;
		}
		int cut = content.lastIndexOf(' ', maxChars);
		if (cut < maxChars / 2) {
			cut = maxChars;
		}
		return content.substring(0, cut).trim() + "\n... [truncated]";
	}
}
