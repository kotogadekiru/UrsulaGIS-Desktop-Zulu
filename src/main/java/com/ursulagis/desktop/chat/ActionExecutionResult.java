package com.ursulagis.desktop.chat;

/**
 * Outcome of running a parsed chat intent: the message shown in the chat UI
 * and whether a desktop dialog/task was actually started.
 *
 * @param message  user-facing reply (success, clarification, or error)
 * @param launched {@code true} when a controller/task was invoked; {@code false} when only text was returned
 */
public record ActionExecutionResult(String message, boolean launched) {

	/** Result for a successfully started UI action or background task. */
	public static ActionExecutionResult launched(String message) {
		return new ActionExecutionResult(message, true);
	}

	/** Result that only replies in chat (help, ambiguity, or missing prerequisites). */
	public static ActionExecutionResult notLaunched(String message) {
		return new ActionExecutionResult(message, false);
	}
}
