package com.ursulagis.desktop.chat;

/**
 * Structured intent returned by the AI layer.
 */
public class ParsedIntent {

	private final UrsulaAction action;
	private final String targetName;
	private final double confidence;
	private final String message;

	public ParsedIntent(UrsulaAction action, String targetName, double confidence, String message) {
		this.action = action;
		this.targetName = targetName;
		this.confidence = confidence;
		this.message = message;
	}

	public UrsulaAction getAction() {
		return action;
	}

	public String getTargetName() {
		return targetName;
	}

	public double getConfidence() {
		return confidence;
	}

	public String getMessage() {
		return message;
	}
}
