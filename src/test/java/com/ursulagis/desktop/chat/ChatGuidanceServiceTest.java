package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.chat.ai.MockAiClient;

class ChatGuidanceServiceTest {

	@Test
	@DisplayName("UNKNOWN intent triggers step-by-step guidance for grillar cosecha")
	void guidanceForUnknownQuery() {
		String guidance = ChatGuidanceService.generate(
				new MockAiClient(),
				"como grillar una cosecha",
				"",
				MapLayerContext.empty());

		assertTrue(guidance.contains("paso a paso") || guidance.contains("1."));
	}

	@Test
	@DisplayName("margin generation query returns Rentabilidades guidance")
	void marginGenerationGuidance() {
		String guidance = ChatGuidanceService.guidanceWithoutAi(
				"como genero un mapa de margenes",
				"",
				MapLayerContext.empty());

		assertTrue(guidance.contains("Rentabilidades"));
		assertTrue(guidance.contains("Herramientas"));
		assertFalse(guidance.contains("Importar") && guidance.indexOf("Importar") < guidance.indexOf("Rentabilidades"));
	}

	@Test
	@DisplayName("GitHub context includes repo and local controller paths")
	void githubContextIncludesRepo() {
		String context = GitHubCodeContextBuilder.buildForQuery("convertir poligono cosecha");

		assertTrue(context.contains("kotogadekiru/UrsulaGIS-Desktop-Zulu"));
		assertTrue(context.contains("PoligonoGUIController.java")
				|| context.contains("AchievementIntentCatalog.java"));
	}
}
