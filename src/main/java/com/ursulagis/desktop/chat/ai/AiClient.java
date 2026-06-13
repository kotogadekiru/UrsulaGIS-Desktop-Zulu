package com.ursulagis.desktop.chat.ai;

/**
 * Abstraction for sending prompts to an LLM and receiving structured text back.
 */
public interface AiClient {

	AiProvider getProvider();

	/**
	 * @param systemPrompt instructions and action catalog for the model
	 * @param userPrompt   the user's natural-language request
	 */
	AiResponse complete(String systemPrompt, String userPrompt);
}
