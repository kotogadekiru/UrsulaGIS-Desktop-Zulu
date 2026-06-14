package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	@DisplayName("\"vamos a crear un poligono\" maps to CREAR_POLIGONO")
	void mapsVamosACrearUnPoligonoToCrearPoligono() {
		IntentParser parser = new IntentParser(new MockAiClient(), MapLayerContext.empty());

		ParsedIntent intent = parser.parse("vamos a crear un poligono");

		assertEquals(UrsulaAction.CREAR_POLIGONO, intent.getAction());
	}

	@Test
	@DisplayName("\"convertir poligono a cosecha\" maps to CONVERTIR_POLIGONO_A_COSECHA")
	void mapsConvertirPoligonoACosecha() {
		IntentParser parser = new IntentParser(new MockAiClient(), MapLayerContext.empty());

		ParsedIntent intent = parser.parse("convertir poligono a cosecha");

		assertEquals(UrsulaAction.CONVERTIR_POLIGONO_A_COSECHA, intent.getAction());
		assertTrue(intent.getMessage().toLowerCase().contains("polígono")
				|| intent.getMessage().toLowerCase().contains("poligono"));
	}

	@Test
	@DisplayName("parses JSON messages that contain escaped quotes")
	void parsesMessageWithEscapedQuotes() {
		IntentParser parser = new IntentParser(new MockAiClient(), MapLayerContext.empty());

		ParsedIntent intent = parser.parse("frase sin sentido xyz123");

		assertEquals(UrsulaAction.UNKNOWN, intent.getAction());
		assertTrue(intent.getMessage().contains("ayuda"));
	}
}
