package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsignacionNdviRequestTest {

	@Test
	@DisplayName("parses campaña, cultivo and ISO date range")
	void parsesCampaniaCultivoAndIsoDates() {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				"descargar NDVI de soja campaña 25/26 desde 2025-11-01 hasta 2026-03-31");

		assertEquals("25/26", req.campaniaName());
		assertEquals("soja", req.cultivoName());
		assertEquals(LocalDate.of(2025, 11, 1), req.begin());
		assertEquals(LocalDate.of(2026, 3, 31), req.end());
		assertTrue(req.hasPeriod());
	}

	@Test
	@DisplayName("parses month range for a campaign season")
	void parsesMonthRange() {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				"ndvi campaña 25/26 de noviembre a marzo");

		assertEquals("25/26", req.campaniaName());
		assertEquals(LocalDate.of(2025, 11, 1), req.begin());
		assertEquals(LocalDate.of(2026, 3, 31), req.end());
	}

	@Test
	@DisplayName("campaign token alone yields seasonal defaults when entity has no meaningful dates")
	void campaignTokenSeasonDefaults() {
		AsignacionNdviRequest req = AsignacionNdviRequest.parse("ndvi asignaciones campaña 25/26");

		assertTrue(AsignacionNdviRequest.campaignNamesMatch(req.campaniaName(), "25/26")
				|| "25/26".equals(req.campaniaName()));
		assertTrue(req.hasPeriod());
		// Prefer entity inicio/fin when configured; otherwise seasonal Nov–Apr.
		DateRangeLike expectedSeason = new DateRangeLike(
				LocalDate.of(2025, 11, 1), LocalDate.of(2026, 4, 30));
		boolean season = req.begin().equals(expectedSeason.begin) && req.end().equals(expectedSeason.end);
		boolean fromEntity = AsignacionNdviRequest.isMeaningfulPeriod(req.begin(), req.end())
				&& !req.begin().equals(LocalDate.now().minusMonths(1));
		assertTrue(season || fromEntity);
	}

	private record DateRangeLike(LocalDate begin, LocalDate end) {
	}

	@Test
	@DisplayName("2627 matches 26/27 campaign naming")
	void compactCampaignNamesMatch() {
		assertTrue(AsignacionNdviRequest.campaignNamesMatch("2627", "26/27"));
		assertTrue(AsignacionNdviRequest.campaignNamesMatch("2627", "26-27"));
		assertEquals(2627, AsignacionNdviRequest.campaignYearRank("2627"));
		assertEquals(2627, AsignacionNdviRequest.campaignYearRank("26/27"));
		assertTrue(AsignacionNdviRequest.campaignYearRank("2627") > AsignacionNdviRequest.campaignYearRank("2526"));
		assertTrue(AsignacionNdviRequest.campaignYearRank("2627") > AsignacionNdviRequest.campaignYearRank("18/19"));
	}

	@Test
	@DisplayName("últimas imágenes usa inicio/fin de campaña cuando existen")
	void latestImagesAssignedWheat() {
		String query = "descargar las ultimas imagenes ndvi de los lotes asignados a trigo";
		assertTrue(AchievementIntentCatalog.isAsignacionNdviQuery(query));

		AsignacionNdviRequest req = AsignacionNdviRequest.parse(query);
		assertEquals("trigo", req.cultivoName());
		assertTrue(req.hasPeriod());
		assertTrue(req.campaniaName() != null && !req.campaniaName().isBlank());
		assertTrue(req.end().compareTo(LocalDate.now()) <= 0);
		assertTrue(req.begin().isBefore(req.end()));
	}

	@Test
	@DisplayName("isMeaningfulPeriod rejects same-day defaults")
	void meaningfulPeriodRejectsDefaults() {
		LocalDate today = LocalDate.now();
		assertFalse(AsignacionNdviRequest.isMeaningfulPeriod(today, today));
		assertFalse(AsignacionNdviRequest.isMeaningfulPeriod(today, today.plusDays(3)));
		assertTrue(AsignacionNdviRequest.isMeaningfulPeriod(today.minusMonths(6), today));
	}
}
