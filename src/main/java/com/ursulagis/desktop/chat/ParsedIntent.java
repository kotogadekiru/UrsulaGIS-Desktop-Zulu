package com.ursulagis.desktop.chat;

import java.time.LocalDate;

/**
 * Structured intent returned by the AI layer.
 */
public class ParsedIntent {

	private final UrsulaAction action;
	private final String targetName;
	private final double confidence;
	private final String message;
	private final String campaniaName;
	private final String cultivoName;
	private final LocalDate beginDate;
	private final LocalDate endDate;
	private final String sourceUserText;

	public ParsedIntent(UrsulaAction action, String targetName, double confidence, String message) {
		this(action, targetName, confidence, message, null, null, null, null, null);
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
		this.action = action;
		this.targetName = targetName;
		this.confidence = confidence;
		this.message = message;
		this.campaniaName = campaniaName;
		this.cultivoName = cultivoName;
		this.beginDate = beginDate;
		this.endDate = endDate;
		this.sourceUserText = sourceUserText;
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

	/** Enriches asignacion/NDVI fields from the original user text when missing. */
	public ParsedIntent enrichFromUserText(String userText) {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				userText, campaniaName, cultivoName, beginDate, endDate);
		return new ParsedIntent(
				action,
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
