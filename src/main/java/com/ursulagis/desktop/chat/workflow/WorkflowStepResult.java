package com.ursulagis.desktop.chat.workflow;

/**
 * Outcome of one {@link SiembraFertilizadaOrchestrator} step for the chat UI and session loop.
 * Tells {@link ChatWorkflowSession} whether to keep auto-advancing, wait for async work,
 * prompt the user, or treat the fertilized-seeding flow as complete.
 *
 * @param message           human-readable status or instruction for the chat
 * @param launched          {@code true} if this step started a real GUI action
 * @param waitingAsync      {@code true} if background work must finish before the next step
 * @param needsUser         {@code true} if the user must confirm a dialog or supply missing data
 * @param workflowComplete  {@code true} when the fertilized-seeding flow has ended
 * @param currentStep       step that just ran
 * @param nextStep          step the orchestrator will run next (may be {@code null} when done)
 */
public record WorkflowStepResult(
		String message,
		boolean launched,
		boolean waitingAsync,
		boolean needsUser,
		boolean workflowComplete,
		SiembraFertilizadaWorkflowStep currentStep,
		SiembraFertilizadaWorkflowStep nextStep) {

	/**
	 * Builds a terminal result: flow complete, no further steps.
	 *
	 * @param message final status shown in chat
	 * @return a result with {@code workflowComplete} set and step {@link SiembraFertilizadaWorkflowStep#DONE}
	 */
	public static WorkflowStepResult done(String message) {
		return new WorkflowStepResult(message, false, false, false, true, SiembraFertilizadaWorkflowStep.DONE, null);
	}

	/**
	 * Builds an in-progress step result for the session auto-advance loop.
	 *
	 * @param current      step that was just executed
	 * @param next         step to run afterward
	 * @param message      chat-facing explanation of what happened or what the user should do
	 * @param launched     whether a GUI action was started
	 * @param waitingAsync whether the session should pause for async completion
	 * @param needsUser    whether the session should pause for user input / dialog confirmation
	 * @return a non-complete {@link WorkflowStepResult}
	 */
	public static WorkflowStepResult step(
			SiembraFertilizadaWorkflowStep current,
			SiembraFertilizadaWorkflowStep next,
			String message,
			boolean launched,
			boolean waitingAsync,
			boolean needsUser) {
		return new WorkflowStepResult(message, launched, waitingAsync, needsUser, false, current, next);
	}
}
