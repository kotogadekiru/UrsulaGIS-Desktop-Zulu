package com.ursulagis.desktop.chat.workflow;

/**
 * Steps of the fertilized seeding workflow orchestrator.
 */
public enum SiembraFertilizadaWorkflowStep {
	SELECT_FIELD_POLYGON,
	DOWNLOAD_NDVI,
	CONVERT_NDVI_TO_HARVEST,
	RECOMMEND_FERT_P,
	CREATE_SIEMBRA_LOMAS,
	CREATE_SIEMBRA_GOOD_ZONES,
	GENERATE_SIEMBRA_FERTILIZADA,
	DONE
}
