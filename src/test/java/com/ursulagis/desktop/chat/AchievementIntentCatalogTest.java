package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;

class AchievementIntentCatalogTest {

	@Test
	@DisplayName("\"convertir poligono a cosecha\" maps to CONVERTIR_POLIGONO_A_COSECHA via logro")
	void mapsConvertPolygonToHarvest() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("convertir poligono a cosecha")
				.orElseThrow();

		assertEquals(UrsulaAction.CONVERTIR_POLIGONO_A_COSECHA, match.action());
		assertEquals(OnboardingAchievements.FIRST_POLYGON_TO_HARVEST, match.achievementId());
	}

	@Test
	@DisplayName("\"importar cosecha\" maps to IMPORT_COSECHA via logro")
	void mapsImportHarvest() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("importar cosecha")
				.orElseThrow();

		assertEquals(UrsulaAction.IMPORT_COSECHA, match.action());
		assertEquals(OnboardingAchievements.FIRST_HARVEST_IMPORTED, match.achievementId());
	}

	@Test
	@DisplayName("\"como genero un mapa de margenes\" maps to GENERAR_MARGEN via logro")
	void mapsGenerateMarginMap() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("como genero un mapa de margenes")
				.orElseThrow();

		assertEquals(UrsulaAction.GENERAR_MARGEN, match.action());
		assertEquals(OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS, match.achievementId());
	}

	@Test
	@DisplayName("\"importar margen\" maps to IMPORT_MARGEN via logro")
	void mapsImportMargin() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("importar margen")
				.orElseThrow();

		assertEquals(UrsulaAction.IMPORT_MARGEN, match.action());
		assertEquals(OnboardingAchievements.FIRST_MARGEN_IMPORTED, match.achievementId());
	}

	@Test
	@DisplayName("\"asignar actividades a lotes\" maps to CONFIG_ASIGNACION via logro")
	void mapsAsignacionToLotes() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("asignar actividades a lotes")
				.orElseThrow();

		assertEquals(UrsulaAction.CONFIG_ASIGNACION, match.action());
		assertEquals(OnboardingAchievements.FIRST_CONFIG_ASIGNACION_CREATED, match.achievementId());
	}

	@Test
	@DisplayName("\"descargar ndvi de soja campaña 25/26\" maps to DOWNLOAD_NDVI_ASIGNACIONES")
	void mapsNdviAsignaciones() {
		String query = "descargar ndvi de soja campaña 25/26 desde 2025-11-01 hasta 2026-03-31";
		AchievementIntentMatch match = AchievementIntentCatalog.match(query).orElseThrow();

		assertEquals(UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES, match.action());
		assertEquals(OnboardingAchievements.FIRST_NDVI_ASIGNACIONES_DOWNLOADED, match.achievementId());
		assertTrue(AchievementIntentCatalog.isAsignacionNdviQuery(query));
	}

	@Test
	@DisplayName("\"cargar las ultimas recorridas para los lotes que van a maiz\" maps to LOAD_RECORRIDAS not NDVI")
	void mapsLoadRecorridasNotNdvi() {
		String query = "cargar las ultimas recorridas para los lotes que van a maiz";
		assertTrue(AchievementIntentCatalog.isRecorridaLoadQuery(query));
		assertTrue(!AchievementIntentCatalog.isAsignacionNdviQuery(query));
		AchievementIntentMatch match = AchievementIntentCatalog.match(query).orElseThrow();
		assertEquals(UrsulaAction.LOAD_RECORRIDAS, match.action());
		assertEquals(OnboardingAchievements.FIRST_RECORRIDA_GUIDED_SHOWN, match.achievementId());
	}

	@Test
	@DisplayName("\"descargar las ultimas imagenes ndvi de los lotes asignados a trigo\" still maps to NDVI")
	void mapsLatestNdviStillWorks() {
		String query = "descargar las ultimas imagenes ndvi de los lotes asignados a trigo";
		assertTrue(AchievementIntentCatalog.isAsignacionNdviQuery(query));
		assertTrue(!AchievementIntentCatalog.isRecorridaLoadQuery(query));
		AchievementIntentMatch match = AchievementIntentCatalog.match(query).orElseThrow();
		assertEquals(UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES, match.action());
	}

	@Test
	@DisplayName("margin generation hints rank calculated-from-labors above import")
	void marginHintsPreferGeneration() {
		String hints = AchievementIntentCatalog.buildRelevantHintsForQuery("como genero un mapa de margenes");

		int calculated = hints.indexOf("FIRST_MARGEN_CALCULATED_FROM_LABORS");
		int imported = hints.indexOf("FIRST_MARGEN_IMPORTED");
		assertTrue(calculated >= 0);
		if (imported >= 0) {
			assertTrue(calculated < imported);
		}
	}

	@Test
	@DisplayName("\"activa los poligonos con superficie mayor a cero\" maps to ACTIVAR_POLIGONOS_SUPERFICIE not CREAR_POLIGONO")
	void mapsActivatePolygonsByArea() {
		String query = "activa los poligonos con superficie mayor a cero";
		AchievementIntentMatch match = AchievementIntentCatalog.match(query).orElseThrow();

		assertEquals(UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE, match.action());
		Optional<AchievementIntentMatch> crear = AchievementIntentCatalog.match("crear poligono");
		assertTrue(crear.isPresent());
		assertEquals(UrsulaAction.CREAR_POLIGONO, crear.get().action());
	}

	@Test
	@DisplayName("\"cargar una siembra y compartirla\" maps to siembra actions not GENERAR_MARGEN")
	void mapsLoadAndShareSiembra() {
		String query = "cargar una siembra y compartirla";
		AchievementIntentMatch match = AchievementIntentCatalog.match(query).orElseThrow();

		assertEquals(UrsulaAction.COMPARTIR_SIEMBRA, match.action());
		assertTrue(AchievementIntentCatalog.isSiembraShareOrImportQuery(query));
	}

	@Test
	@DisplayName("\"comparar capas activas\" maps to COMPARE_ACTIVE_LAYERS")
	void mapsCompareActiveLayers() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("comparar capas activas")
				.orElseThrow();

		assertEquals(UrsulaAction.COMPARE_ACTIVE_LAYERS, match.action());
		assertEquals(OnboardingAchievements.FIRST_CONFIG_MULTI_LAYER_HISTOGRAM, match.achievementId());
	}

	@Test
	@DisplayName("\"exportar pantalla\" maps to EXPORT_PANTALLA not EXPORT_LABOR")
	void mapsExportScreen() {
		AchievementIntentMatch match = AchievementIntentCatalog.match("exportar pantalla")
				.orElseThrow();

		assertEquals(UrsulaAction.EXPORT_PANTALLA, match.action());
		assertEquals(OnboardingAchievements.FIRST_CONFIG_SCREEN_EXPORTED, match.achievementId());
		assertTrue(AchievementIntentCatalog.isExportScreenQuery("exportar pantalla"));
	}

	@Test
	@DisplayName("\"exportar\" alone does not map to EXPORT_RECORRIDA")
	void bareExportDoesNotMapToRecorrida() {
		Optional<AchievementIntentMatch> match = AchievementIntentCatalog.match("exportar");
		if (match.isPresent()) {
			assertTrue(match.get().action() != UrsulaAction.EXPORT_RECORRIDA,
					() -> "unexpected action " + match.get().action() + " score=" + match.get().score());
		}
	}

	@Test
	@DisplayName("prompt catalog includes achievement hints")
	void promptIncludesAchievementHints() {
		String prompt = AchievementIntentCatalog.buildActionCatalogForPrompt();

		assertTrue(prompt.contains("FIRST_POLYGON_TO_HARVEST"));
		assertTrue(prompt.contains("CONVERTIR_POLIGONO_A_COSECHA"));
		assertTrue(prompt.contains("FIRST_HARVEST_IMPORTED"));
		assertTrue(prompt.contains("EXPORT_PANTALLA"));
	}
}