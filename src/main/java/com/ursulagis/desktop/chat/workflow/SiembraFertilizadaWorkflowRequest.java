package com.ursulagis.desktop.chat.workflow;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.chat.SiembraFertilizadaWorkflowGuide;

/**
 * Parsed parameters for the fertilized seeding workflow.
 */
public record SiembraFertilizadaWorkflowRequest(
		String fieldName,
		String harvestCrop,
		double yieldTn,
		String fertSourceKey,
		String lomasSeed,
		String goodZoneSeed,
		double rowSpacingM,
		String seedingCrop,
		LocalDate ndviBegin,
		LocalDate ndviEnd) {

	private static final Pattern YIELD_KG_PATTERN = Pattern.compile("(\\d{3,5})\\s*kg\\s*/?\\s*ha", Pattern.CASE_INSENSITIVE);
	private static final Pattern YIELD_TN_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*t(?:ns?)?\\s*/?\\s*ha", Pattern.CASE_INSENSITIVE);

	public static SiembraFertilizadaWorkflowRequest parse(String query) {
		if (query == null || query.isBlank()) {
			return defaults();
		}
		String lower = query.toLowerCase(Locale.ROOT);
		return new SiembraFertilizadaWorkflowRequest(
				extractFieldName(query),
				extractHarvestCrop(lower),
				SiembraFertilizadaWorkflowGuide.extractYieldTn(query),
				extractFertSourceKey(lower),
				extractLomasSeed(lower),
				extractGoodZoneSeed(lower),
				lower.contains("0.19") || lower.contains("0,19") ? 0.19 : 0.19,
				lower.contains("trigo") ? "trigo" : "trigo",
				parseCampaignBegin(lower),
				parseCampaignEnd(lower));
	}

	private static SiembraFertilizadaWorkflowRequest defaults() {
		return new SiembraFertilizadaWorkflowRequest(
				"", "soja", 4.6, "Fosfato monoamonico",
				"Baguette 620 2627", "Pehuen", 0.19, "trigo",
				LocalDate.of(2025, 11, 1), LocalDate.of(2026, 4, 30));
	}

	private static String extractFieldName(String query) {
		String n = SiembraFertilizadaWorkflowGuide.normalize(query);
		if (n.contains("regalada")) {
			return "regalada";
		}
		Matcher m = Pattern.compile("(?:para|del|de la)\\s+([a-z0-9]+)").matcher(n);
		return m.find() ? m.group(1) : "";
	}

	private static String extractHarvestCrop(String lower) {
		if (lower.contains("soja")) {
			return "soja";
		}
		if (lower.contains("maiz") || lower.contains("maíz")) {
			return "maiz";
		}
		return "soja";
	}

	private static String extractFertSourceKey(String lower) {
		if (lower.contains("fosfato monoamonico") || lower.contains("fosfato monoamónico") || lower.contains("map")) {
			return "Fosfato monoamonico";
		}
		return "Fosfato monoamonico";
	}

	private static String extractLomasSeed(String lower) {
		if (lower.contains("baguette") || lower.contains("baguete")) {
			return "Baguette 620 2627";
		}
		return "Baguette 620 2627";
	}

	private static String extractGoodZoneSeed(String lower) {
		return lower.contains("pehuen") ? "Pehuen" : "Pehuen";
	}

	private static LocalDate parseCampaignBegin(String lower) {
		if (lower.contains("25/26") || lower.contains("25-26")) {
			return LocalDate.of(2025, 11, 1);
		}
		return LocalDate.now().minusMonths(6);
	}

	private static LocalDate parseCampaignEnd(String lower) {
		if (lower.contains("25/26") || lower.contains("25-26")) {
			return LocalDate.of(2026, 4, 30);
		}
		return LocalDate.now();
	}
}
