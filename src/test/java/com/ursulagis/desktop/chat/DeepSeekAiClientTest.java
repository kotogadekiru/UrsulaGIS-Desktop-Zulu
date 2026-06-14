package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.chat.ai.DeepSeekAiClient;

class DeepSeekAiClientTest {

	@Test
	@DisplayName("extractJsonPayload keeps raw JSON")
	void rawJson() {
		String json = "{\"action\":\"HELP\",\"confidence\":1.0,\"message\":\"Hola\"}";
		assertEquals(json, DeepSeekAiClient.extractJsonPayload(json));
	}

	@Test
	@DisplayName("extractJsonPayload unwraps fenced JSON blocks")
	void fencedJson() {
		String content = "```json\n{\"action\":\"CREAR_POLIGONO\"}\n```";
		assertEquals("{\"action\":\"CREAR_POLIGONO\"}", DeepSeekAiClient.extractJsonPayload(content));
	}

	@Test
	@DisplayName("extractJsonPayload extracts JSON from surrounding text")
	void embeddedJson() {
		String content = "Sure! {\"action\":\"HELP\",\"message\":\"ok\"} Thanks.";
		String extracted = DeepSeekAiClient.extractJsonPayload(content);
		assertTrue(extracted.contains("\"action\":\"HELP\""));
	}
}
