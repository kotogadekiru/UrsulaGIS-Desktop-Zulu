package com.ursulagis.desktop.chat.workflow;

import java.util.function.Consumer;

import com.ursulagis.desktop.chat.ActionExecutionResult;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.MapLayerContextBuilder;
import com.ursulagis.desktop.chat.SiembraFertilizadaWorkflowGuide;
import com.ursulagis.desktop.gui.JFXMain;

import javafx.application.Platform;

/**
 * Holds the active workflow orchestrator between chat messages.
 */
public final class ChatWorkflowSession {

	private static final int MAX_AUTO_STEPS = 15;

	private static SiembraFertilizadaOrchestrator active;
	private static JFXMain boundMain;
	private static Consumer<String> messageConsumer;

	private ChatWorkflowSession() {
	}

	public static void bind(JFXMain main, Consumer<String> onWorkflowMessage) {
		boundMain = main;
		messageConsumer = onWorkflowMessage;
	}

	public static boolean hasActiveSession() {
		return active != null && active.getCurrentStep() != SiembraFertilizadaWorkflowStep.DONE;
	}

	/** Called by the orchestrator when an async step finishes to continue the flow. */
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

	public static void clear() {
		active = null;
	}

	private static void publishMessage(String message) {
		if (messageConsumer != null && message != null && !message.isBlank()) {
			messageConsumer.accept(message);
		}
	}

	/** Runs sync steps in sequence until async work, user dialog, error, or completion. */
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

	private static void appendMessage(StringBuilder messages, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		if (messages.length() > 0) {
			messages.append("\n");
		}
		messages.append(text);
	}

	private static boolean isContinueCommand(String text) {
		if (text == null) {
			return false;
		}
		String n = text.trim().toLowerCase();
		return n.equals("continuar") || n.equals("siguiente") || n.equals("siguiente paso")
				|| n.equals("continue") || n.equals("next");
	}

	private static ActionExecutionResult toActionResult(String message, boolean launched) {
		return launched
				? ActionExecutionResult.launched(message)
				: ActionExecutionResult.notLaunched(message);
	}
}
