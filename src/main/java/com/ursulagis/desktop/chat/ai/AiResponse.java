package com.ursulagis.desktop.chat.ai;

/**
 * Result of an AI completion request.
 */
public class AiResponse {

	private final String content;
	private final String model;
	private final AiProvider provider;
	private final long latencyMs;
	private final boolean mocked;

	public AiResponse(String content, String model, AiProvider provider, long latencyMs, boolean mocked) {
		this.content = content;
		this.model = model;
		this.provider = provider;
		this.latencyMs = latencyMs;
		this.mocked = mocked;
	}

	public String getContent() {
		return content;
	}

	public String getModel() {
		return model;
	}

	public AiProvider getProvider() {
		return provider;
	}

	public long getLatencyMs() {
		return latencyMs;
	}

	public boolean isMocked() {
		return mocked;
	}
}
