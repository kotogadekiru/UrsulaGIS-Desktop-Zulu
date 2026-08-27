package com.ursulagis.desktop.chat;

import java.time.LocalDate;

/**
 * Structured chat intent produced by the AI (or by follow-up resume): which
 * {@link UrsulaAction} to run, optional layer target, confidence, reply text,
 * and campaign/crop/date fields used by NDVI-asignación downloads.
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

	/** Minimal constructor used when campaign/date fields are not needed. */
	public ParsedIntent(UrsulaAction action, String targetName, double confidence, String message) {
		this(action, targetName, confidence, message, null, null, null, null, null);
	}

	/**
	 * Full intent including filters for NDVI-by-assignment and the original user text.
	 *
	 * @param action         action id to execute
	 * @param targetName     optional layer name from the model
	 * @param confidence     0–1 confidence from the model (heuristics may ignore it)
	 * @param message        short reply already in Ursula's voice
	 * @param campaniaName   campaign name if provided in JSON
	 * @param cultivoName    crop name if provided in JSON
	 * @param beginDate      NDVI period start if provided
	 * @param endDate        NDVI period end if provided
	 * @param sourceUserText original user utterance (used to re-parse filters)
	 */
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

	/** Action the executor should attempt. */
	public UrsulaAction getAction() {
		return action;
	}

	/** Layer or entity name the user/model referred to, if any. */
	public String getTargetName() {
		return targetName;
	}

	/** Model confidence in the chosen action (0–1). */
	public double getConfidence() {
		return confidence;
	}

	/** Chat reply to show alongside or instead of launching UI. */
	public String getMessage() {
		return message;
	}

	/** Campaign filter for {@link UrsulaAction#DOWNLOAD_NDVI_ASIGNACIONES}. */
	public String getCampaniaName() {
		return campaniaName;
	}

	/** Crop filter for assignment-based NDVI downloads. */
	public String getCultivoName() {
		return cultivoName;
	}

	/** Inclusive start of the NDVI imagery window when known. */
	public LocalDate getBeginDate() {
		return beginDate;
	}

	/** Inclusive end of the NDVI imagery window when known. */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Original user text that produced this intent; preferred when re-parsing
	 * campaign/crop/dates for NDVI or recorrida loads.
	 */
	public String getSourceUserText() {
		return sourceUserText;
	}

	/**
	 * Fills missing campaña/cultivo/date fields by re-parsing {@code userText}
	 * (and any existing fields) through {@link AsignacionNdviRequest}.
	 */
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
