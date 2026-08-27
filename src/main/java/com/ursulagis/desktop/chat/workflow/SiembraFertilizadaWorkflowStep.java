package com.ursulagis.desktop.chat.workflow;

/**
 * Ordered stages of the fertilized-seeding (siembra fertilizada) chat workflow.
 * {@link SiembraFertilizadaOrchestrator} advances through these until {@link #DONE}.
 */
public enum SiembraFertilizadaWorkflowStep {
	/** Pick and enable the field contour polygon (and discover lomas / good-zone environments). */
	SELECT_FIELD_POLYGON,
	/** Download NDVI for the selected field over the request date window, or reuse an existing layer. */
	DOWNLOAD_NDVI,
	/** Convert the best NDVI layer into a synthetic harvest (cosecha) map. */
	CONVERT_NDVI_TO_HARVEST,
	/** Run phosphorus reposición recommendation on that harvest to produce a fertilization layer. */
	RECOMMEND_FERT_P,
	/** Convert the "lomas" environment polygon into a seeding labor (user confirms the dialog). */
	CREATE_SIEMBRA_LOMAS,
	/** Convert each good-zone environment polygon into a seeding labor, one at a time. */
	CREATE_SIEMBRA_GOOD_ZONES,
	/** Pair seeding + fertilization layers and generate siembra fertilizada (in-line fert) per environment. */
	GENERATE_SIEMBRA_FERTILIZADA,
	/** Workflow finished; no further automatic steps. */
	DONE
}
