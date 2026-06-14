package com.ursulagis.desktop.chat;

/**
 * Outcome of executing a parsed chat intent.
 */
public record ActionExecutionResult(String message, boolean launched) {

	public static ActionExecutionResult launched(String message) {
		return new ActionExecutionResult(message, true);
	}

	public static ActionExecutionResult notLaunched(String message) {
		return new ActionExecutionResult(message, false);
	}
}
