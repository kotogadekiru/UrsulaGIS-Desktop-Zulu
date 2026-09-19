package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatPendingFollowUpTest {

	@AfterEach
	void clearPending() {
		ChatPendingFollowUp.clear();
	}

	@Test
	@DisplayName("short campaign reply resumes pending NDVI asignaciones")
	void resumesWithCampaignReply() {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				"descargar las ultimas imagenes ndvi de los lotes asignados a trigo",
				"18/19",
				"trigo",
				null,
				null);
		ChatPendingFollowUp.rememberNdviAsignacionNeedsCampania(
				req, "descargar las ultimas imagenes ndvi de los lotes asignados a trigo");

		assertTrue(ChatPendingFollowUp.isAwaitingCampania());

		ParsedIntent resumed = ChatPendingFollowUp.tryResume("26/27").orElseThrow();
		assertEquals(UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES, resumed.getAction());
		assertTrue(AsignacionNdviRequest.campaignNamesMatch(resumed.getCampaniaName(), "26/27"));
		assertEquals("trigo", resumed.getCultivoName());
		assertFalse(ChatPendingFollowUp.isAwaitingCampania());
	}

	@Test
	@DisplayName("extractCampaniaReply understands 2627 and 26/27")
	void extractsCampaignTokens() {
		assertEquals("26/27", ChatPendingFollowUp.extractCampaniaReply("26/27"));
		assertEquals("26/27", ChatPendingFollowUp.extractCampaniaReply("2627"));
		assertEquals("26/27", ChatPendingFollowUp.extractCampaniaReply("campaña 26/27"));
	}

	@Test
	@DisplayName("labor-name clarification resumes pending share fertilizacion")
	void resumesWithLaborNameReply() {
		ChatPendingFollowUp.rememberNeedsLaborTarget(
				UrsulaAction.COMPARTIR_FERTILIZACION,
				"share jag 21 fertilizacion",
				"jag 21");

		assertTrue(ChatPendingFollowUp.isAwaitingLabor());

		ParsedIntent resumed = ChatPendingFollowUp.tryResume("the one with jag 21 in the name").orElseThrow();
		assertEquals(UrsulaAction.COMPARTIR_FERTILIZACION, resumed.getAction());
		assertEquals("jag 21", resumed.getTargetName());
		assertFalse(ChatPendingFollowUp.isAwaitingLabor());
	}

	@Test
	@DisplayName("extractLaborNameReply understands clarification phrases")
	void extractsLaborNameFromClarification() {
		assertEquals("jag 21", ChatPendingFollowUp.extractLaborNameReply("the one with jag 21 in the name"));
		assertEquals("jag 21", ChatPendingFollowUp.extractLaborNameReply("la que tiene jag 21"));
	}
}
