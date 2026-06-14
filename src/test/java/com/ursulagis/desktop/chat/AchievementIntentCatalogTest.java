package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;

class AchievementIntentCatalogTest {

	@Test
	@DisplayName("\"convertir poligono a cosecha\" maps to CONVERTIR_POLIGONO_A_COSECHA via logro")
	void mapsConvertPolygonToHarvest() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("convertir poligono a cosecha")
				.orElseThrow();

		assertEquals(UrsulaAction.CONVERTIR_POLIGONO_A_COSECHA, match.action());
		assertEquals(OnboardingAchievements.FIRST_POLYGON_TO_HARVEST, match.achievementId());
	}

	@Test
	@DisplayName("\"importar cosecha\" maps to IMPORT_COSECHA via logro")
	void mapsImportHarvest() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("importar cosecha")
				.orElseThrow();

		assertEquals(UrsulaAction.IMPORT_COSECHA, match.action());
		assertEquals(OnboardingAchievements.FIRST_HARVEST_IMPORTED, match.achievementId());
	}

	@Test
	@DisplayName("\"como genero un mapa de margenes\" maps to GENERAR_MARGEN via logro")
	void mapsGenerateMarginMap() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("como genero un mapa de margenes")
				.orElseThrow();

		assertEquals(UrsulaAction.GENERAR_MARGEN, match.action());
		assertEquals(OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS, match.achievementId());
	}

	@Test
	@DisplayName("\"importar margen\" maps to IMPORT_MARGEN via logro")
	void mapsImportMargin() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("importar margen")
				.orElseThrow();

		assertEquals(UrsulaAction.IMPORT_MARGEN, match.action());
		assertEquals(OnboardingAchievements.FIRST_MARGEN_IMPORTED, match.achievementId());
	}

	@Test
	@DisplayName("margin generation hints rank calculated-from-labors above import")
	void marginHintsPreferGeneration() {
		String hints = AchievementIntentCatalog.buildRelevantHintsForQuery("como genero un mapa de margenes");

		int calculated = hints.indexOf("FIRST_MARGEN_CALCULATED_FROM_LABORS");
		int imported = hints.indexOf("FIRST_MARGEN_IMPORTED");
		assertTrue(calculated >= 0);
		if (imported >= 0) {
			assertTrue(calculated < imported);
		}
	}

	@Test
	@DisplayName("prompt catalog includes achievement hints")
	void promptIncludesAchievementHints() {
		String prompt = AchievementIntentCatalog.buildActionCatalogForPrompt();

		assertTrue(prompt.contains("FIRST_POLYGON_TO_HARVEST"));
		assertTrue(prompt.contains("CONVERTIR_POLIGONO_A_COSECHA"));
		assertTrue(prompt.contains("FIRST_HARVEST_IMPORTED"));
	}
}
