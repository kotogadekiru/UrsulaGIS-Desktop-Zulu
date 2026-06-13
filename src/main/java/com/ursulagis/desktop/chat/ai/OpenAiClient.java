package com.ursulagis.desktop.chat.ai;

import java.util.logging.Logger;

/**
 * Mocked OpenAI ChatGPT client. Logs the request and delegates parsing to {@link MockAiClient}.
 * Replace {@link #complete(String, String)} body with a real HTTP call to api.openai.com when ready.
 */
public class OpenAiClient extends MockAiClient {

	private static final Logger LOG = Logger.getLogger(OpenAiClient.class.getName());
	private static final String MODEL = "gpt-4o (mocked)";

	@Override
	public AiProvider getProvider() {
		return AiProvider.OPENAI;
	}

	@Override
	public AiResponse complete(String systemPrompt, String userPrompt) {
		long start = System.currentTimeMillis();
		LOG.info(() -> String.format("[OpenAI mock] POST /v1/chat/completions model=%s user=%d chars",
				MODEL, userPrompt == null ? 0 : userPrompt.length()));
		simulateLatency();
		String content = parseIntent(userPrompt, systemPrompt);
		long elapsed = System.currentTimeMillis() - start;
		LOG.fine(() -> "[OpenAI mock] response: " + content);
		return new AiResponse(content, MODEL, getProvider(), elapsed, true);
	}
}
