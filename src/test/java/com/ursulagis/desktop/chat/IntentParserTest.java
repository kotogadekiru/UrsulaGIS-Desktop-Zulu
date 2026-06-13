package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.chat.ai.MockAiClient;

class IntentParserTest {

	@Test
	@DisplayName("\"lets create a new polygon\" maps to CREAR_POLIGONO")
	void mapsCreateNewPolygonToCrearPoligono() {
		IntentParser parser = new IntentParser(new MockAiClient(), MapLayerContext.empty());

		ParsedIntent intent = parser.parse("lets create a new polygon");

		assertEquals(UrsulaAction.CREAR_POLIGONO, intent.getAction());
	}
}
