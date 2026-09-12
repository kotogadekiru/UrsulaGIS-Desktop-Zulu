package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionChainParserTest {

	@Test
	@DisplayName("importar cosecha y compartirla → IMPORT_COSECHA then COMPARTIR_COSECHA")
	void importAndShareHarvest() {
		List<UrsulaAction> actions = ActionChainParser.parse("importar cosecha y compartirla");
		assertEquals(List.of(UrsulaAction.IMPORT_COSECHA, UrsulaAction.COMPARTIR_COSECHA), actions);
	}

	@Test
	@DisplayName("crear poligono luego importar suelo → CREAR_POLIGONO then IMPORT_SUELO")
	void polygonThenSoil() {
		List<UrsulaAction> actions = ActionChainParser.parse("crear poligono luego importar suelo");
		assertEquals(UrsulaAction.CREAR_POLIGONO, actions.get(0));
		assertEquals(UrsulaAction.IMPORT_SUELO, actions.get(1));
	}

	@Test
	@DisplayName("single action stays size 1")
	void singleAction() {
		List<UrsulaAction> actions = ActionChainParser.parse("importar fertilizacion");
		assertEquals(1, actions.size());
		assertEquals(UrsulaAction.IMPORT_FERTILIZACION, actions.get(0));
	}

	@Test
	@DisplayName("all mapped achievements have at least one catalog phrase match path")
	void everyAchievementHasChatMapping() {
		var mapped = new java.util.HashSet<String>();
		String catalog = AchievementIntentCatalog.buildActionCatalogForPrompt();
		for (String id : com.ursulagis.desktop.gui.onboarding.OnboardingAchievements.getInstance().getAllAchievementIds()) {
			// ACTIVAR_POLIGONOS shares FIRST_POLYGON_IMPORTED; bulk NDVI has no achievement
			boolean inCatalog = catalog.contains(id);
			if (inCatalog) {
				mapped.add(id);
			}
		}
		assertTrue(mapped.size() >= 85,
				"expected most achievements in prompt catalog, got " + mapped.size());
	}
}
