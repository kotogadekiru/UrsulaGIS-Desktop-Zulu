package com.ursulagis.desktop.chat.ai;

/**
 * Abstraction for sending prompts to an LLM and receiving text back.
 * Chat intent parsing and guidance services call this interface so the UI
 * stays provider-agnostic.
 */
public interface AiClient {

	/** Which backend this client talks to (used for settings and fallbacks). */
	AiProvider getProvider();

	/**
	 * Completes a chat turn intended for structured intent JSON.
	 * Callers parse {@link AiResponse#getContent()} as an action payload.
	 *
	 * @param systemPrompt instructions and action catalog for the model
	 * @param userPrompt   the user's natural-language request
	 * @return model output wrapped with provider/model/latency metadata
	 */
	AiResponse complete(String systemPrompt, String userPrompt);

	/**
	 * Plain-text completion for step-by-step guidance (no JSON extraction).
	 * Default implementation delegates to {@link #complete(String, String)}.
	 *
	 * @param systemPrompt guidance instructions and context
	 * @param userPrompt   the user's natural-language request
	 * @return free-form assistant text for display in chat
	 */
	default AiResponse completePlain(String systemPrompt, String userPrompt) {
		return complete(systemPrompt, userPrompt);
	}
}
