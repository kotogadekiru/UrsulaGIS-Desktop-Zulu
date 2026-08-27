package com.ursulagis.desktop.chat.ai;

/**
 * Result of an AI completion request in the chat stack.
 * Carries the model text plus metadata used for logging and UI feedback.
 */
public class AiResponse {

	private final String content;
	private final String model;
	private final AiProvider provider;
	private final long latencyMs;
	private final boolean mocked;

	/**
	 * @param content   assistant text (intent JSON or plain guidance)
	 * @param model     model id reported by the provider or a local label
	 * @param provider  backend that produced this response
	 * @param latencyMs round-trip time in milliseconds
	 * @param mocked    {@code true} when generated without a live LLM call
	 */
	public AiResponse(String content, String model, AiProvider provider, long latencyMs, boolean mocked) {
		this.content = content;
		this.model = model;
		this.provider = provider;
		this.latencyMs = latencyMs;
		this.mocked = mocked;
	}

	/** Assistant output: structured intent JSON or free-form guidance text. */
	public String getContent() {
		return content;
	}

	/** Model identifier (API-reported or a mock label such as {@code mock-local}). */
	public String getModel() {
		return model;
	}

	/** Provider that served this completion. */
	public AiProvider getProvider() {
		return provider;
	}

	/** Elapsed time for the completion call, in milliseconds. */
	public long getLatencyMs() {
		return latencyMs;
	}

	/**
	 * @return {@code true} if the response came from a local mock, not a live API
	 */
	public boolean isMocked() {
		return mocked;
	}
}
