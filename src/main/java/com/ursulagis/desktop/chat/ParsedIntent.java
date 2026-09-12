package com.ursulagis.desktop.chat;

import java.time.LocalDate;
import java.util.List;

/**
 * Structured chat intent: one or more {@link UrsulaAction}s to run in order,
 * optional layer target, confidence, reply text, and NDVI filter fields.
 */
public class ParsedIntent {

	private final List<UrsulaAction> actions;
	private final String targetName;
	private final double confidence;
	private final String message;
	private final String campaniaName;
	private final String cultivoName;
	private final LocalDate beginDate;
	private final LocalDate endDate;
	private final String sourceUserText;

	public ParsedIntent(UrsulaAction action, String targetName, double confidence, String message) {
		this(List.of(action != null ? action : UrsulaAction.UNKNOWN),
				targetName, confidence, message, null, null, null, null, null);
	}

	public ParsedIntent(
			UrsulaAction action,
			String targetName,
			double confidence,
			String message,
			String campaniaName,
			String cultivoName,
			LocalDate beginDate,
			LocalDate endDate,
			String sourceUserText) {
		this(List.of(action != null ? action : UrsulaAction.UNKNOWN),
				targetName, confidence, message, campaniaName, cultivoName, beginDate, endDate, sourceUserText);
	}

	public ParsedIntent(
			List<UrsulaAction> actions,
			String targetName,
			double confidence,
			String message,
			String campaniaName,
			String cultivoName,
			LocalDate beginDate,
			LocalDate endDate,
			String sourceUserText) {
		if (actions == null || actions.isEmpty()) {
			this.actions = List.of(UrsulaAction.UNKNOWN);
		} else {
			this.actions = List.copyOf(actions);
		}
		this.targetName = targetName;
		this.confidence = confidence;
		this.message = message;
		this.campaniaName = campaniaName;
		this.cultivoName = cultivoName;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.sourceUserText = sourceUserText;
	}

	/** First (or only) action — kept for callers that expect a single action. */
	public UrsulaAction getAction() {
		return actions.get(0);
	}

	/** Ordered chain of actions to execute (size ≥ 1). */
	public List<UrsulaAction> getActions() {
		return actions;
	}

	/** Whether more than one action should run in sequence. */
	public boolean isChain() {
		return actions.size() > 1;
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

	public String getCampaniaName() {
		return campaniaName;
	}

	public String getCultivoName() {
		return cultivoName;
	}

	public LocalDate getBeginDate() {
		return beginDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public String getSourceUserText() {
		return sourceUserText;
	}

	/** Copy with a single action replaced (used when executing one step of a chain). */
	public ParsedIntent withAction(UrsulaAction action) {
		return new ParsedIntent(
				action,
				targetName,
				confidence,
				message,
				campaniaName,
				cultivoName,
				beginDate,
				endDate,
				sourceUserText);
	}

	/** Copy with a full action chain. */
	public ParsedIntent withActions(List<UrsulaAction> newActions) {
		return new ParsedIntent(
				newActions,
				targetName,
				confidence,
				message,
				campaniaName,
				cultivoName,
				beginDate,
				endDate,
				sourceUserText);
	}

	public ParsedIntent enrichFromUserText(String userText) {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				userText, campaniaName, cultivoName, beginDate, endDate);
		return new ParsedIntent(
				actions,
				targetName,
				confidence,
				message,
				req.campaniaName(),
				req.cultivoName(),
				req.begin(),
				req.end(),
				userText);
	}
}
