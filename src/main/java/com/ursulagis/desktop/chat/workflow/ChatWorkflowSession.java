package com.ursulagis.desktop.chat.workflow;

import java.util.function.Consumer;

import com.ursulagis.desktop.chat.ActionExecutionResult;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.MapLayerContextBuilder;
import com.ursulagis.desktop.chat.SiembraFertilizadaWorkflowGuide;
import com.ursulagis.desktop.gui.JFXMain;

import javafx.application.Platform;

/**
 * Session holder for the active siembra fertilizada (fertilized seeding) chat workflow.
 * Bridges chat messages to {@link SiembraFertilizadaOrchestrator}, auto-advances sync steps,
 * and resumes after async GUI work finishes.
 */
public final class ChatWorkflowSession {

	private static final int MAX_AUTO_STEPS = 15;

	private static SiembraFertilizadaOrchestrator active;
	private static JFXMain boundMain;
	private static Consumer<String> messageConsumer;

	/** Prevents instantiation. */
	private ChatWorkflowSession() {
	}

	/**
	 * Registers the main window and a callback for workflow status messages shown in chat.
	 *
	 * @param main              application main window used by the orchestrator
	 * @param onWorkflowMessage consumer that posts intermediate workflow text to the chat UI
	 */
	public static void bind(JFXMain main, Consumer<String> onWorkflowMessage) {
		boundMain = main;
		messageConsumer = onWorkflowMessage;
	}

	/**
	 * Whether a fertilized-seeding workflow is in progress (not finished or cleared).
	 *
	 * @return {@code true} if an orchestrator exists and has not reached {@link SiembraFertilizadaWorkflowStep#DONE}
	 */
	public static boolean hasActiveSession() {
		return active != null && active.getCurrentStep() != SiembraFertilizadaWorkflowStep.DONE;
	}

	/**
	 * Continues the workflow on the FX thread after an async step (NDVI download, conversions, etc.) completes.
	 * Called by the orchestrator via its async completion callbacks.
	 */
	public static void resumeAfterAsync() {
		if (active == null || boundMain == null) {
			return;
		}
		Platform.runLater(() -> {
			if (active == null || active.isWaitingAsync()) {
				return;
			}
			MapLayerContext ctx = MapLayerContextBuilder.from(boundMain);
			ActionExecutionResult result = runUntilPause(boundMain, ctx);
			publishMessage(result.message());
		});
	}

	/**
	 * Entry point from chat: starts a new siembra fertilizada flow when the text matches the guide,
	 * or advances an existing one on continue commands ("continuar", "siguiente", …).
	 *
	 * @param main         application main window
	 * @param userText     latest user chat message
	 * @param layerContext snapshot of loaded map layers for polygon/NDVI lookup
	 * @return execution result for the chat UI, or {@code null} if the text is not a workflow request
	 */
	public static ActionExecutionResult handle(JFXMain main, String userText, MapLayerContext layerContext) {
		boundMain = main;

		if (isContinueCommand(userText)) {
			if (active == null) {
				return ActionExecutionResult.notLaunched(
						"No hay un flujo en curso. Describí la siembra fertilizada que querés armar.");
			}
			if (active.isWaitingAsync()) {
				return ActionExecutionResult.notLaunched(
						"Todavía está corriendo el proceso anterior. Esperá un momento…");
			}
			return runUntilPause(main, layerContext);
		}

		if (!SiembraFertilizadaWorkflowGuide.matches(userText)) {
			return null;
		}

		active = new SiembraFertilizadaOrchestrator(SiembraFertilizadaWorkflowRequest.parse(userText));
		return runUntilPause(main, layerContext);
	}

	/** Clears the active orchestrator so a new fertilized-seeding request can start fresh. */
	public static void clear() {
		active = null;
	}

	/** Posts a workflow status line to the bound chat message consumer, if any. */
	private static void publishMessage(String message) {
		if (messageConsumer != null && message != null && !message.isBlank()) {
			messageConsumer.accept(message);
		}
	}

	/**
	 * Runs orchestrator steps in a tight loop until async work, a user dialog, an error,
	 * completion, or the auto-step safety limit is hit.
	 */
	private static ActionExecutionResult runUntilPause(JFXMain main, MapLayerContext layerContext) {
		StringBuilder messages = new StringBuilder();
		boolean launched = false;

		for (int i = 0; i < MAX_AUTO_STEPS; i++) {
			WorkflowStepResult result = active.executeStep(main, layerContext);
			appendMessage(messages, result.message());
			launched = launched || result.launched();

			if (result.workflowComplete()) {
				active = null;
				return toActionResult(messages.toString(), launched);
			}
			if (result.waitingAsync() || result.needsUser() || active.isWaitingAsync()) {
				return toActionResult(messages.toString(), launched);
			}
			if (!result.launched() && result.nextStep() == result.currentStep()) {
				return toActionResult(messages.toString(), launched);
			}
		}

		appendMessage(messages, "El flujo sigue en curso…");
		return toActionResult(messages.toString(), launched);
	}

	/** Appends a non-blank step message, separating lines with newlines. */
	private static void appendMessage(StringBuilder messages, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		if (messages.length() > 0) {
			messages.append("\n");
		}
		messages.append(text);
	}

	/** Detects user phrases that mean "advance the current workflow" in Spanish or English. */
	private static boolean isContinueCommand(String text) {
		if (text == null) {
			return false;
		}
		String n = text.trim().toLowerCase();
		return n.equals("continuar") || n.equals("siguiente") || n.equals("siguiente paso")
				|| n.equals("continue") || n.equals("next");
	}

	/** Wraps accumulated step text as launched or not-launched for the chat UI. */
	private static ActionExecutionResult toActionResult(String message, boolean launched) {
		return launched
				? ActionExecutionResult.launched(message)
				: ActionExecutionResult.notLaunched(message);
	}
}
