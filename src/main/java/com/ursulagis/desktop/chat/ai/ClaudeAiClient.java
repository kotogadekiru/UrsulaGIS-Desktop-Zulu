package com.ursulagis.desktop.chat.ai;

import java.util.logging.Logger;

/**
 * Mocked Anthropic Claude client. Logs the request and delegates parsing to {@link MockAiClient}.
 * Replace {@link #complete(String, String)} body with a real HTTP call to api.anthropic.com when ready.
 */
public class ClaudeAiClient extends MockAiClient {

	private static final Logger LOG = Logger.getLogger(ClaudeAiClient.class.getName());
	private static final String MODEL = "claude-sonnet-4 (mocked)";

	@Override
	public AiProvider getProvider() {
		return AiProvider.CLAUDE;
	}

	@Override
	public AiResponse complete(String systemPrompt, String userPrompt) {
		long start = System.currentTimeMillis();
		LOG.info(() -> String.format("[Claude mock] POST /v1/messages model=%s user=%d chars",
				MODEL, userPrompt == null ? 0 : userPrompt.length()));
		simulateLatency();
		String content = parseIntent(userPrompt, systemPrompt);
		long elapsed = System.currentTimeMillis() - start;
		LOG.fine(() -> "[Claude mock] response: " + content);
		return new AiResponse(content, MODEL, getProvider(), elapsed, true);
	}
}
