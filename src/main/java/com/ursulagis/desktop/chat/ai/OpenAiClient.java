package com.ursulagis.desktop.chat.ai;

import java.util.logging.Logger;

/**
 * Mocked OpenAI ChatGPT client for the chat AI stack.
 * Logs a stand-in {@code POST /v1/chat/completions} and delegates intent parsing to
 * {@link MockAiClient}; replace {@link #complete(String, String)} with a real
 * call to {@code api.openai.com} when wiring the live API.
 */
public class OpenAiClient extends MockAiClient {

	private static final Logger LOG = Logger.getLogger(OpenAiClient.class.getName());
	private static final String MODEL = "gpt-4o (mocked)";

	/** Identifies this client as the mocked {@link AiProvider#OPENAI} backend. */
	@Override
	public AiProvider getProvider() {
		return AiProvider.OPENAI;
	}

	/**
	 * Simulates an OpenAI completion: brief delay, then local rule-based intent JSON.
	 * Response content is the same structured payload used by {@link MockAiClient}.
	 */
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
