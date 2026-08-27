package com.ursulagis.desktop.chat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatAiSettingsTest {

	@Test
	@DisplayName("defaults to URSULA when Chat.AI_PROVIDER is not configured")
	void defaultsToUrsula() {
		assertEquals(AiProvider.URSULA, ChatAiSettings.resolveProvider());
	}
}
