package com.ursulagis.desktop.chat.ai;

import java.util.logging.Logger;

/**
 * Mocked Anthropic Claude client for the chat AI stack.
 * Logs a stand-in {@code POST /v1/messages} and delegates intent parsing to
 * {@link MockAiClient}; replace {@link #complete(String, String)} with a real
 * call to {@code api.anthropic.com} when wiring the live API.
 */
public class ClaudeAiClient extends MockAiClient {

	private static final Logger LOG = Logger.getLogger(ClaudeAiClient.class.getName());
	private static final String MODEL = "claude-sonnet-4 (mocked)";

	/** Identifies this client as the mocked {@link AiProvider#CLAUDE} backend. */
	@Override
	public AiProvider getProvider() {
		return AiProvider.CLAUDE;
	}

	/**
	 * Simulates a Claude completion: brief delay, then local rule-based intent JSON.
	 * Response content is the same structured payload used by {@link MockAiClient}.
	 */
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
