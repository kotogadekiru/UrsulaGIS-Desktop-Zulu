package com.ursulagis.desktop.chat.workflow;

/**
 * Outcome of executing one orchestrator step.
 */
public record WorkflowStepResult(
		String message,
		boolean launched,
		boolean waitingAsync,
		boolean needsUser,
		boolean workflowComplete,
		SiembraFertilizadaWorkflowStep currentStep,
		SiembraFertilizadaWorkflowStep nextStep) {

	public static WorkflowStepResult done(String message) {
		return new WorkflowStepResult(message, false, false, false, true, SiembraFertilizadaWorkflowStep.DONE, null);
	}

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
