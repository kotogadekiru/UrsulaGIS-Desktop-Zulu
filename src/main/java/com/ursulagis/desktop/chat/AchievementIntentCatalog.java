package com.ursulagis.desktop.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;

/**
 * Bridges onboarding achievements (logros) to chat actions: phrase/hint scoring,
 * specialized query detectors (margin, NDVI asignaciones, recorridas, …), and
 * prompt/help text built from i18n achievement titles and hints.
 */
public final class AchievementIntentCatalog {

	private static final String ACHIEVEMENT_KEY_PREFIX = "Onboarding.achievement.";
	private static final String HINT_KEY_PREFIX = "Onboarding.hint.";
	private static final double MIN_MATCH_SCORE = 2.5;

	/** One achievement → action mapping with extra natural-language phrases. */
	private record ChatMapping(String achievementId, UrsulaAction action, String... phrases) {
	}

	private static final List<ChatMapping> CHAT_MAPPINGS = List.of(
			// Polígono
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_DRAWN, UrsulaAction.CREAR_POLIGONO,
					"crear poligono", "crear polígono", "dibujar poligono", "dibujar polígono", "draw polygon", "new polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_IMPORTED, UrsulaAction.IMPORT_POLIGONO,
					"importar poligono", "importar polígonos", "importar poligonos", "import polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGONS_AREA_ACTIVATED, UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE,
					"activa poligonos superficie", "activar poligonos superficie",
					"activa los poligonos con superficie", "poligonos superficie mayor cero",
					"enable polygons area"),
			new ChatMapping(OnboardingAchievements.FIRST_DISTANCE_MEASURED, UrsulaAction.MEDIR_DISTANCIA,
					"medir distancia", "measure distance"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_UNION, UrsulaAction.UNIR_POLIGONOS,
					"unir poligonos", "unir polígonos", "union poligonos", "merge polygons"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_INTERSECTED, UrsulaAction.INTERSECTAR_POLIGONOS,
					"intersectar poligonos", "intersectar polígonos", "interseccion poligonos", "intersect polygons"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_EXTRACTED, UrsulaAction.EXTRAER_POLIGONOS,
					"extraer poligonos", "extraer polígonos", "extract polygons"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_CONTOUR_EXTRACTED, UrsulaAction.EXTRAER_CONTORNO,
					"extraer contorno", "extract contour"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_HARVEST, UrsulaAction.CONVERTIR_POLIGONO_A_COSECHA,
					"convertir poligono cosecha", "convertir polígono cosecha", "convertir a cosecha",
					"convert polygon harvest", "convertir poligonos cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_SIEMBRA, UrsulaAction.CONVERTIR_POLIGONO_A_SIEMBRA,
					"convertir poligono siembra", "convertir polígono siembra", "convertir a siembra"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_FERTILIZATION, UrsulaAction.CONVERTIR_POLIGONO_A_FERTILIZACION,
					"convertir poligono fertilizacion", "convertir polígono fertilización", "convertir a fertilizacion",
					"convertir a fertilización"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_PULVERIZATION, UrsulaAction.CONVERTIR_POLIGONO_A_PULVERIZACION,
					"convertir poligono pulverizacion", "convertir polígono pulverización", "convertir a pulverizacion",
					"convertir a pulverización"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_SOIL, UrsulaAction.CONVERTIR_POLIGONO_A_SUELO,
					"convertir poligono suelo", "convertir polígono a suelo", "poligono a suelo"),

			// Cosecha
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_IMPORTED, UrsulaAction.IMPORT_COSECHA,
					"importar cosecha", "abrir cosecha", "import harvest", "open harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_IMPORTED, UrsulaAction.IMPORT_COSECHA_VOYAGER,
					"importar cosecha voyager", "importar voyager", "cosecha voyager", "importar vy1",
					"cosecha vy1", "mapa de cosecha vy1", "import voyager", "voyager harvest", "abrir voyager"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_SHARED, UrsulaAction.COMPARTIR_COSECHA,
					"compartir cosecha", "share harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_GRIDDED, UrsulaAction.GRILLAR_COSECHA,
					"grillar cosecha", "grillar la cosecha", "grid harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_SUMMED, UrsulaAction.SUMAR_COSECHAS,
					"sumar cosechas", "sumar cosecha", "unir cosechas"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_SOIL_CREATED, UrsulaAction.COSECHA_A_SUELO,
					"crear suelo desde cosecha", "cosecha a suelo", "suelo desde cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_POINTS_EXPORTED, UrsulaAction.EXPORT_COSECHA_PUNTOS,
					"exportar cosecha puntos", "exportar puntos cosecha", "cosecha a puntos"),
			new ChatMapping(OnboardingAchievements.FIRST_N_FERTILIZATION_RECOMMENDED, UrsulaAction.RECOMENDAR_FERT_N,
					"recomendar nitrogeno", "recomendar n", "fertilizacion nitrogeno", "recomendar fert n"),
			new ChatMapping(OnboardingAchievements.FIRST_P_FERTILIZATION_RECOMMENDED, UrsulaAction.RECOMENDAR_FERT_P,
					"recomendar fosforo", "recomendar p", "fertilizacion fosforo", "recomendar fert p"),
			new ChatMapping(OnboardingAchievements.FIRST_P_BALANCE_FERTILIZATION_RECOMMENDED, UrsulaAction.RECOMENDAR_FERT_P_BALANCE,
					"balance fosforo", "recomendar p balance", "fosforo balance"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_FROM_HARVEST, UrsulaAction.COSECHA_A_COSECHA,
					"crear cosecha desde cosecha", "cosecha desde cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_FROM_HARVEST, UrsulaAction.COSECHA_A_FERTILIZACION,
					"crear fertilizacion desde cosecha", "cosecha a fertilizacion"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_FROM_HARVEST, UrsulaAction.COSECHA_A_PULVERIZACION,
					"crear pulverizacion desde cosecha", "cosecha a pulverizacion"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_FROM_HARVEST, UrsulaAction.SIEMBRA_DESDE_COSECHA,
					"crear siembra desde cosecha", "siembra desde cosecha", "cosecha a siembra"),

			// Fertilización
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_IMPORTED, UrsulaAction.IMPORT_FERTILIZACION,
					"importar fertilizacion", "importar fertilización", "abrir fertilizacion", "import fertilization"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_SHARED, UrsulaAction.COMPARTIR_FERTILIZACION,
					"compartir fertilizacion", "compartir fertilización", "share fertilization"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_EXPORTED, UrsulaAction.EXPORT_FERTILIZACION,
					"exportar fertilizacion", "exportar fertilización", "export fertilization"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_JOINED, UrsulaAction.UNIR_FERTILIZACIONES,
					"unir fertilizaciones", "juntar fertilizaciones"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_GRIDDED, UrsulaAction.UNIR_FERTILIZACIONES,
					"grillar fertilizacion", "grillar fertilización", "grid fertilization"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_SPLIT, UrsulaAction.PARTIR_FERTILIZACION,
					"partir fertilizacion", "partir fertilización", "split fertilization"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_FROM_FERTILIZATION, UrsulaAction.SIEMBRA_DESDE_FERTILIZACION,
					"siembra desde fertilizacion", "siembra desde fertilización", "crear siembra desde fertilizacion"),

			// Siembra
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_IMPORTED, UrsulaAction.IMPORT_SIEMBRA,
					"importar siembra", "cargar siembra", "abrir siembra", "import seeding", "open seeding"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_SHARED, UrsulaAction.COMPARTIR_SIEMBRA,
					"compartir siembra", "share seeding", "cargar siembra compartir", "cargar una siembra y compartirla"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_EXPORTED, UrsulaAction.EXPORT_SIEMBRA,
					"exportar siembra", "exportar prescripcion siembra", "export seeding"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_JOINED, UrsulaAction.UNIR_SIEMBRAS,
					"unir siembras", "juntar siembras"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_GRIDDED, UrsulaAction.GRILLAR_SIEMBRA,
					"grillar siembra", "grid seeding"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_EDITED, UrsulaAction.EDITAR_SIEMBRA,
					"editar siembra", "edit seeding", "reprocesar siembra"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZED_SEEDING_GENERATED, UrsulaAction.GENERAR_SIEMBRA_FERTILIZADA,
					"siembra fertilizada", "generar siembra fertilizada", "fertilized seeding"),

			// Pulverización
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_IMPORTED, UrsulaAction.IMPORT_PULVERIZACION,
					"importar pulverizacion", "importar pulverización", "abrir pulverizacion", "import spray"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_SHARED, UrsulaAction.COMPARTIR_PULVERIZACION,
					"compartir pulverizacion", "compartir pulverización", "share spray"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_EXPORTED, UrsulaAction.EXPORT_PULVERIZACION,
					"exportar pulverizacion", "exportar pulverización", "export spray"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_EXPORTED_JSON, UrsulaAction.EXPORT_PULVERIZACION_JSON,
					"exportar pulverizacion json", "exportar pulverización json", "pulverizacion json"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_JOINED, UrsulaAction.UNIR_PULVERIZACIONES,
					"unir pulverizaciones", "juntar pulverizaciones"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_GRIDDED, UrsulaAction.UNIR_PULVERIZACIONES,
					"grillar pulverizacion", "grillar pulverización"),
			new ChatMapping(OnboardingAchievements.FIRST_PULVERIZATION_EDITED, UrsulaAction.EDITAR_PULVERIZACION,
					"editar pulverizacion", "editar pulverización", "reprocesar pulverizacion"),

			// Suelo
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_IMPORTED, UrsulaAction.IMPORT_SUELO,
					"importar suelo", "abrir suelo", "import soil"),
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_NUTRIENT_BALANCE, UrsulaAction.BALANCE_NUTRIENTES,
					"balance nutrientes", "balance de nutrientes", "nutrient balance"),
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_EDITED, UrsulaAction.EDITAR_SUELO,
					"editar suelo", "reprocesar suelo", "edit soil"),
			new ChatMapping(OnboardingAchievements.FIRST_SOIL_YIELD_ESTIMATED, UrsulaAction.ESTIMAR_RENDIMIENTO_SUELO,
					"estimar rendimiento", "potencial rendimiento", "estimar potencial suelo"),

			// Recorrida
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_IMPORTED, UrsulaAction.IMPORT_RECORRIDA,
					"importar recorrida", "import recorrida", "shapefile recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_GUIDED_SHOWN, UrsulaAction.LOAD_RECORRIDAS,
					"cargar recorrida", "cargar recorridas", "ultimas recorridas", "última recorrida",
					"ultima recorrida", "mostrar recorridas", "abrir recorridas", "tabla recorridas",
					"recorridas de los lotes", "recorridas maiz", "recorridas maíz", "recorridas soja",
					"recorridas trigo", "load recorrida", "load scouting"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_SYNCED_FROM_CLOUD, UrsulaAction.UPDATE_RECORRIDA,
					"sincronizar recorrida", "actualizar recorrida", "update recorrida", "sync recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_EXPORTED, UrsulaAction.EXPORT_RECORRIDA,
					"exportar recorrida", "export recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_SHARED, UrsulaAction.COMPARTIR_RECORRIDA,
					"compartir recorrida", "share recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_SAVED_LOCAL, UrsulaAction.GUARDAR_RECORRIDA,
					"guardar recorrida", "save recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_INTERPOLATED_TO_SOIL, UrsulaAction.INTERPOLAR_RECORRIDA,
					"interpolar recorrida", "recorrida a suelo", "interpolar a suelo"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_SAMPLES_VALUES_ASSIGNED, UrsulaAction.ASIGNAR_VALORES_RECORRIDA,
					"asignar valores recorrida", "asignar muestras", "valores muestras"),

			// Margen
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS, UrsulaAction.GENERAR_MARGEN,
					"generar mapa margen", "generar mapa de margen", "generar margen", "generar margenes",
					"mapa de margenes", "mapa de margen", "calcular margen", "calcular rentabilidad",
					"rentabilidades", "crear mapa margen", "como genero margen"),
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_IMPORTED, UrsulaAction.IMPORT_MARGEN,
					"importar margen", "importar margenes", "importar mapa margen", "abrir margen"),
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_EDITED, UrsulaAction.EDITAR_MARGEN,
					"editar margen", "editar mapa margen", "reprocesar margen"),
			new ChatMapping(OnboardingAchievements.FIRST_MARGEN_SUMMED, UrsulaAction.SUMAR_MARGENES,
					"sumar margenes", "sumar márgenes", "unir margenes"),

			// NDVI
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_ASIGNACIONES_DOWNLOADED, UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES,
					"ndvi asignacion", "ndvi asignación", "ndvi campania", "ndvi campaña",
					"descargar ndvi campania", "descargar ndvi campaña", "descargar ndvi asignacion",
					"ndvi contornos asignacion", "ndvi de soja campania", "ndvi de soja campaña",
					"obtener ndvi campania", "obtener ndvi campaña", "ndvi por asignacion",
					"download ndvi campaign", "ndvi assignments",
					"lotes asignados", "imagenes ndvi", "últimas imagenes ndvi", "ultimas imagenes ndvi",
					"ndvi de los lotes", "ndvi trigo", "asignados a trigo"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_DOWNLOADED, UrsulaAction.DOWNLOAD_NDVI,
					"descargar ndvi", "obtener ndvi", "download ndvi", "ndvi para"),
			new ChatMapping(OnboardingAchievements.FIRST_CONVERT_NDVI_TO_HARVEST, UrsulaAction.CONVERTIR_NDVI_A_COSECHA,
					"convertir ndvi cosecha", "ndvi a cosecha", "convert ndvi harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_TO_FERTILIZATION, UrsulaAction.CONVERTIR_NDVI_A_FERTILIZACION,
					"convertir ndvi fertilizacion", "ndvi a fertilizacion", "ndvi a fertilización"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_ACUM_TO_HARVEST, UrsulaAction.CONVERTIR_NDVI_ACUM_A_COSECHA,
					"ndvi acumulado cosecha", "convertir ndvi acumulado", "ndvi acum a cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_EXPORTED, UrsulaAction.EXPORT_NDVI,
					"exportar ndvi", "exportar ndvi excel", "export ndvi"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_EXPORTED_TIFF, UrsulaAction.EXPORT_NDVI_TIFF,
					"exportar ndvi tiff", "exportar tiff ndvi", "ndvi a tiff"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_CHART_VIEWED, UrsulaAction.SHOW_NDVI_CHART,
					"grafico ndvi", "gráfico ndvi", "mostrar grafico ndvi", "chart ndvi"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_ACUM_CHART_VIEWED, UrsulaAction.SHOW_NDVI_ACUM_CHART,
					"grafico ndvi acumulado", "gráfico ndvi acumulado", "ndvi acum chart"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_EVOLUTION_VIEWED, UrsulaAction.SHOW_NDVI_EVOLUTION,
					"evolucion ndvi", "evolución ndvi", "ndvi evolution"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_HISTOGRAM_VIEWED, UrsulaAction.SHOW_NDVI_HISTOGRAM,
					"histograma ndvi", "ndvi histogram"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_DATE_FILTERED, UrsulaAction.FILTRAR_NDVI_FECHA,
					"filtrar ndvi", "filtrar ndvi fecha", "filtro fecha ndvi"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_SAVED, UrsulaAction.GUARDAR_NDVI,
					"guardar ndvi", "save ndvi"),

			// Config
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_SHAPEFILES_JOINED, UrsulaAction.JUNTAR_SHAPES,
					"unir shape", "juntar shape", "merge shape", "unir shapefiles"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_MULTI_LAYER_HISTOGRAM, UrsulaAction.COMPARE_ACTIVE_LAYERS,
					"comparar capas activas", "comparacion capas activas", "comparación capas activas",
					"histograma multilayer", "comparar capas", "compare active layers"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ASIGNACION_CREATED, UrsulaAction.CONFIG_ASIGNACION,
					"asignar actividades", "asignar actividad", "asignar a lote", "asignar a lotes",
					"asignacion lotes", "asignación lotes", "asignar cultivo lote", "asignar campania lote",
					"asignar campaña lote", "abrir asignacion", "abrir asignación"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_SCREEN_EXPORTED, UrsulaAction.EXPORT_PANTALLA,
					"exportar pantalla", "exportar la pantalla", "guardar pantalla", "captura pantalla",
					"capturar pantalla", "screenshot", "export screen", "save screen", "salvar tela",
					"exportar tela", "guardar tela"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_PURCHASE_ORDER_GENERATED, UrsulaAction.GENERAR_ORDEN_COMPRA,
					"generar orden compra", "orden de compra", "purchase order"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_PURCHASE_ORDER_QUOTED_ONLINE, UrsulaAction.COTIZAR_ORDEN_COMPRA,
					"cotizar orden", "cotizar online", "quote purchase order"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_GO_TO_ADDRESS, UrsulaAction.GO_TO_ADDRESS,
					"ir a direccion", "ir a dirección", "go to address", "buscar direccion"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_PROJECT_CHANGED, UrsulaAction.CAMBIAR_PROYECTO,
					"cambiar proyecto", "cambiar base datos", "cambiar db", "change project"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_APP_UPDATED, UrsulaAction.ACTUALIZAR_APP,
					"actualizar app", "buscar actualizacion", "update app", "check updates"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_LAYERS_CORRELATED, UrsulaAction.CORRELACIONAR_CAPAS,
					"correlacionar capas", "correlacion capas", "correlate layers"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_LANGUAGE_CHANGED, UrsulaAction.CAMBIAR_IDIOMA,
					"cambiar idioma", "change language", "change locale"),

			// Genérico labor
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.RESUMIR_LABOR,
					"resumir labor", "resumir capa", "resumir cosecha", "resumir la cosecha",
					"resumir cosecha activa", "simplificar", "simplify"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_EXPORTED, UrsulaAction.EXPORT_LABOR,
					"exportar labor", "exportar capa", "export layer", "export shape"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_CLONED, UrsulaAction.CLONAR_LABOR,
					"clonar labor", "clonar capa", "clone labor"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SAVED, UrsulaAction.GUARDAR_LABOR,
					"guardar labor", "guardar capa", "save labor"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_OUTLIERS_FILTERED, UrsulaAction.FILTRAR_OUTLIERS,
					"filtrar outliers", "quitar outliers", "filter outliers"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_PDF_REPORTED, UrsulaAction.REPORTE_PDF_LABOR,
					"reporte pdf", "generar pdf", "pdf labor", "informe pdf"),

			// Menu coverage achievements
			new ChatMapping(OnboardingAchievements.FIRST_IMAGERY_IMPORTED, UrsulaAction.IMPORT_IMAGERY,
					"importar imagen", "importar imagery", "abrir imagen", "import imagery", "cargar imagen"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_EMPRESA_OPENED, UrsulaAction.CONFIG_EMPRESA,
					"tabla empresas", "ver empresas", "empresas", "mostrar empresas", "configurar empresas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ESTABLECIMIENTO_OPENED, UrsulaAction.CONFIG_ESTABLECIMIENTO,
					"tabla establecimientos", "ver establecimientos", "establecimientos"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_LOTE_OPENED, UrsulaAction.CONFIG_LOTE,
					"tabla lotes", "ver lotes", "lotes", "mostrar lotes", "configurar lotes"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_CAMPANIA_OPENED, UrsulaAction.CONFIG_CAMPANIA,
					"tabla campanias", "tabla campañas", "ver campanias", "ver campañas", "campañas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_CULTIVOS_OPENED, UrsulaAction.CONFIG_CULTIVOS,
					"tabla cultivos", "ver cultivos", "cultivos", "configurar cultivos"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_FERTILIZANTES_OPENED, UrsulaAction.CONFIG_FERTILIZANTES,
					"tabla fertilizantes", "ver fertilizantes", "fertilizantes"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_AGROQUIMICOS_OPENED, UrsulaAction.CONFIG_AGROQUIMICOS,
					"tabla agroquimicos", "tabla agroquímicos", "ver agroquimicos", "agroquímicos"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_SEMILLAS_OPENED, UrsulaAction.CONFIG_SEMILLAS,
					"tabla semillas", "ver semillas", "semillas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_LABORES_TABLE_OPENED, UrsulaAction.SHOW_LABORES_TABLE,
					"tabla labores", "ver labores", "labores guardadas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_POLIGONOS_TABLE_OPENED, UrsulaAction.CONFIG_POLIGONOS_TABLE,
					"tabla poligonos", "tabla polígonos", "ver poligonos", "polígonos guardados"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_PROPERTIES_TABLE_OPENED, UrsulaAction.SHOW_CONFIG_TABLE,
					"tabla configuracion", "tabla configuración", "ver configuracion", "config keys"),
			new ChatMapping("", UrsulaAction.SHOW_LOG,
					"mostrar log", "ver log", "abrir log", "show log", "consola"),
			new ChatMapping("", UrsulaAction.SHOW_ACHIEVEMENTS,
					"mostrar logros", "ver logros", "logros", "achievements", "onboarding"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_JOINED, UrsulaAction.UNIR_COSECHAS,
					"unir cosechas", "unir mapas de cosecha", "merge harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_EDITED, UrsulaAction.EDITAR_COSECHA,
					"editar cosecha", "editar mapa de cosecha", "edit harvest"),
			new ChatMapping(OnboardingAchievements.FIRST_K_FERTILIZATION_RECOMMENDED, UrsulaAction.RECOMENDAR_FERT_K,
					"recomendar potasio", "recomendar k", "fertilizacion k", "fertilización k"),
			new ChatMapping(OnboardingAchievements.FIRST_S_FERTILIZATION_RECOMMENDED, UrsulaAction.RECOMENDAR_FERT_S,
					"recomendar azufre", "recomendar s", "fertilizacion s", "fertilización s"),
			new ChatMapping(OnboardingAchievements.FIRST_HARVEST_ELEVATION_CHART_VIEWED, UrsulaAction.SHOW_COSECHA_ELEVATION_CHART,
					"grafico cosecha", "gráfico cosecha", "chart cosecha", "elevacion cosecha", "elevación cosecha"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_DIRIGIDA_GENERATED, UrsulaAction.GENERAR_RECORRIDA_DIRIGIDA,
					"recorrida dirigida", "crear recorrida dirigida", "muestreo dirigido"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_SRM_IMPORTED, UrsulaAction.IMPORT_SIEMBRA_SRM,
					"importar srm", "importar siembra srm", "abrir srm"),
			new ChatMapping(OnboardingAchievements.FIRST_SEEDING_SRM_EXPORTED, UrsulaAction.EXPORT_SIEMBRA_SRM,
					"exportar srm", "exportar siembra srm", "guardar srm"),
			new ChatMapping(OnboardingAchievements.FIRST_FERTILIZATION_EDITED, UrsulaAction.EDITAR_FERTILIZACION,
					"editar fertilizacion", "editar fertilización", "edit fertilizer"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_EDITED, UrsulaAction.EDITAR_POLIGONO,
					"editar poligono", "editar polígono", "edit polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_CLONED, UrsulaAction.CLONAR_POLIGONO,
					"clonar poligono", "clonar polígono", "clone polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_SIMPLIFIED, UrsulaAction.SIMPLIFICAR_POLIGONO,
					"simplificar poligono", "simplificar polígono", "simplify polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_EXPLODED, UrsulaAction.EXPLOTAR_POLIGONO,
					"explotar poligono", "explotar polígono", "explode polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_SAVED, UrsulaAction.GUARDAR_POLIGONO,
					"guardar poligono", "guardar polígono", "save polygon"),
			new ChatMapping(OnboardingAchievements.FIRST_PATH_SHORTENED, UrsulaAction.ACORTAR_CAMINO,
					"acortar camino", "simplificar camino", "shorten path"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_IMPORTED, UrsulaAction.GO_TO_POLIGONO,
					"ir al poligono", "ir al polígono", "go to polygon", "centrar poligono"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_CLOUD_FILTERED, UrsulaAction.FILTRAR_NDVI_NUBLADO,
					"filtrar nublado", "quitar nubes", "filter cloudy"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_DOWNLOADED, UrsulaAction.GO_TO_NDVI,
					"ir al ndvi", "go to ndvi", "centrar ndvi"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_DOWNLOADED, UrsulaAction.EDITAR_NDVI,
					"editar ndvi", "renombrar ndvi", "edit ndvi"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_IMPORTED, UrsulaAction.GO_TO_RECORRIDA,
					"ir a recorrida", "go to recorrida", "centrar recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_RECORRIDA_IMPORTED, UrsulaAction.EDITAR_RECORRIDA,
					"editar recorrida", "tabla recorrida", "edit recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_MEAN_ACCENTUATED, UrsulaAction.ACENTUAR_MEDIA_LABOR,
					"acentuar media", "acentuar promedio", "accentuate mean"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_HISTOGRAM_VIEWED, UrsulaAction.SHOW_HISTOGRAMA_LABOR,
					"histograma", "ver histograma", "histogram"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_CUT_BY_POLYGON, UrsulaAction.CORTAR_LABOR_POR_POLIGONO,
					"cortar lote", "recortar labor", "cut labor", "cortar labor"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.SHOW_LABOR_TABLE,
					"tabla labor", "ver tabla labor", "mostrar tabla"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.LAYER_TRANSPARENCIA,
					"transparencia", "alternar transparencia", "toggle transparency"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.REMOVE_LAYER,
					"quitar labor", "remover labor", "eliminar capa", "remove labor"),

			new ChatMapping(OnboardingAchievements.FIRST_NDVI_IMPORTED, UrsulaAction.IMPORT_NDVI,
					"importar ndvi", "abrir ndvi", "import ndvi", "cargar ndvi tiff"),
			new ChatMapping(OnboardingAchievements.FIRST_NDVI_BULK_DOWNLOADED, UrsulaAction.BULK_NDVI_DOWNLOAD,
					"descarga masiva ndvi", "bulk ndvi", "descargar ndvi masivo"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_PLAGA_OPENED, UrsulaAction.CONFIG_PLAGA,
					"tabla plagas", "ver plagas", "configurar plagas", "plagas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_NDVI_TABLE_OPENED, UrsulaAction.SHOW_NDVI_TABLE,
					"tabla ndvi", "ver tabla ndvi", "mostrar ndvi guardados"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_RECORRIDAS_TABLE_OPENED, UrsulaAction.SHOW_RECORRIDAS_TABLE,
					"tabla recorridas", "ver recorridas guardadas", "mostrar recorridas"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ORDENES_COMPRA_OPENED, UrsulaAction.SHOW_ORDENES_COMPRA,
					"ordenes de compra", "órdenes de compra", "tabla ordenes compra"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ORDENES_PULVERIZACION_OPENED, UrsulaAction.SHOW_ORDENES_PULVERIZACION,
					"ordenes pulverizacion", "órdenes pulverización", "tabla ordenes pulverizacion"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ORDENES_FERTILIZACION_OPENED, UrsulaAction.SHOW_ORDENES_FERTILIZACION,
					"ordenes fertilizacion", "órdenes fertilización", "tabla ordenes fertilizacion"),
			new ChatMapping(OnboardingAchievements.FIRST_CONFIG_ORDENES_SIEMBRA_OPENED, UrsulaAction.SHOW_ORDENES_SIEMBRA,
					"ordenes siembra", "órdenes siembra", "tabla ordenes siembra"),
			new ChatMapping("", UrsulaAction.SHOW_ACERCA_DE,
					"acerca de", "about", "ayuda acerca", "version", "versión"),
			new ChatMapping(OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED, UrsulaAction.GO_TO_LAYER,
					"ir a capa", "ir a la labor", "go to layer", "centrar capa", "zoom labor"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_RECORRIDA, UrsulaAction.POLIGONO_A_RECORRIDA,
					"poligono a recorrida", "polígono a recorrida", "camino a recorrida", "convertir a recorrida"),
			new ChatMapping(OnboardingAchievements.FIRST_POLYGON_TO_CIRCLE, UrsulaAction.POLIGONO_A_CIRCULO,
					"poligono a circulo", "polígono a círculo", "convertir a circulo", "convertir a círculo"),
			new ChatMapping(OnboardingAchievements.FIRST_PATH_EDITED, UrsulaAction.EDITAR_CAMINO,
					"editar camino", "edit path", "modificar camino"));

	/** Prevents instantiation. */
	private AchievementIntentCatalog() {
	}

	/**
	 * Best chat-action match from {@link #CHAT_MAPPINGS} when the score clears
	 * {@link #MIN_MATCH_SCORE}; empty when the prompt is blank or too weak.
	 */
	public static Optional<AchievementIntentMatch> match(String userPrompt) {
		if (userPrompt == null || userPrompt.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(userPrompt);

		AchievementIntentMatch best = null;
		double bestScore = 0;

		for (ChatMapping mapping : CHAT_MAPPINGS) {
			double score = scoreMapping(normalized, mapping);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						mapping.action(),
						mapping.achievementId(),
						score,
						suggestReply(mapping.achievementId(), mapping.action()));
			}
		}

		if (best == null || bestScore < MIN_MATCH_SCORE) {
			return Optional.empty();
		}
		return Optional.of(best);
	}

	/**
	 * Softest nearest achievement for guidance: mapped actions plus any logro
	 * hint overlap (may return {@link UrsulaAction#UNKNOWN} with a hint only).
	 */
	public static Optional<AchievementIntentMatch> findNearest(String userPrompt) {
		if (userPrompt == null || userPrompt.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(userPrompt);
		AchievementIntentMatch best = null;
		double bestScore = 0;

		for (ChatMapping mapping : CHAT_MAPPINGS) {
			double score = scoreMapping(normalized, mapping);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						mapping.action(),
						mapping.achievementId(),
						score,
						suggestReply(mapping.achievementId(), mapping.action()));
			}
		}
		for (String achievementId : OnboardingAchievements.getInstance().getAllAchievementIds()) {
			double score = scoreHintForQuery(normalized, achievementId);
			if (score > bestScore) {
				bestScore = score;
				best = new AchievementIntentMatch(
						UrsulaAction.UNKNOWN,
						achievementId,
						score,
						achievementHint(achievementId));
			}
		}
		return bestScore > 0 ? Optional.ofNullable(best) : Optional.empty();
	}

	/**
	 * Top achievement hints (by token overlap) formatted for AI system prompts.
	 */
	public static String buildRelevantHintsForQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return "";
		}
		String normalized = normalize(userQuery);
		return Arrays.stream(OnboardingAchievements.getInstance().getAllAchievementIds())
				.map(id -> new ScoredHint(id, scoreHintForQuery(normalized, id)))
				.filter(s -> s.score() > 0)
				.sorted((a, b) -> Double.compare(b.score(), a.score()))
				.limit(5)
				.map(s -> "- [" + s.achievementId() + "] " + achievementHint(s.achievementId()))
				.reduce((a, b) -> a + "\n" + b)
				.orElse("- (no close achievement match)");
	}

	/** True when the user wants to generate/calculate a margin map (not import). */
	public static boolean isMarginGenerationQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String normalized = normalize(userQuery);
		return mentionsMargin(normalized)
				&& containsAnyToken(normalized, GENERATION_VERBS)
				&& !containsAnyToken(normalized, IMPORT_VERBS);
	}

	/** True for “activar polígonos con superficie/área > 0” style requests. */
	public static boolean isActivatePolygonsWithAreaQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean activate = n.contains("activa") || n.contains("activar");
		boolean polygon = n.contains("poligono");
		boolean area = n.contains("superficie") || n.contains("area") || n.contains("mayor") || n.contains("cero");
		return activate && polygon && area;
	}

	/** True when the user asks to import/load and/or share a seeding map. */
	public static boolean isSiembraShareOrImportQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean siembra = n.contains("siembra");
		boolean share = n.contains("compartir");
		boolean load = n.contains("cargar") || n.contains("importar") || n.contains("abrir");
		return siembra && (share || load);
	}

	/**
	 * True for NDVI download by campaign/assignment/lots (excludes bare “recorrida”).
	 */
	public static boolean isAsignacionNdviQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		// Recorridas (scouting) must never be treated as NDVI downloads.
		if (n.contains("recorrida") && !n.contains("ndvi")) {
			return false;
		}
		boolean ndvi = n.contains("ndvi");
		boolean asignacion = n.contains("asignacion") || n.contains("asignad")
				|| n.contains("contorno") || (n.contains("lote") && n.contains("asign"));
		boolean campania = n.contains("campania") || n.contains("campana") || CAMPAIGN_TOKEN.matcher(n).find();
		boolean crop = n.contains("soja") || n.contains("maiz") || n.contains("trigo")
				|| n.contains("girasol") || n.contains("cultivo");
		boolean latestLots = (n.contains("ultima") || n.contains("ultimo") || n.contains("imagen"))
				&& (n.contains("lote") || asignacion);
		return ndvi && (asignacion || latestLots || (campania && crop) || (campania && n.contains("descargar"))
				|| (crop && n.contains("lote")));
	}

	/**
	 * Load/show saved recorridas from the DB (not shapefile import, not NDVI).
	 */
	public static boolean isRecorridaLoadQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		if (!n.contains("recorrida") || n.contains("ndvi")) {
			return false;
		}
		if (n.contains("export") || n.contains("sincroniz") || n.contains("actualizar")
				|| n.contains("sync") || n.contains("update") || n.contains("shapefile")
				|| n.contains("importar")) {
			return false;
		}
		boolean loadVerb = n.contains("cargar") || n.contains("mostrar") || n.contains("abrir")
				|| n.contains("traer") || n.contains("ver") || n.contains("listar");
		boolean latest = n.contains("ultima") || n.contains("ultimo");
		boolean filterCtx = n.contains("lote") || n.contains("maiz") || n.contains("soja")
				|| n.contains("trigo") || n.contains("girasol") || n.contains("cultivo")
				|| n.contains("campania") || n.contains("campana") || n.contains("tabla");
		return loadVerb || latest || filterCtx;
	}

	private static final java.util.regex.Pattern CAMPAIGN_TOKEN =
			java.util.regex.Pattern.compile("\\b\\d{2}\\s*[/-]\\s*\\d{2}\\b");

	/** Achievement id paired with a hint-overlap score for ranking. */
	private record ScoredHint(String achievementId, double score) {
	}

	/** Lists mapped (and unmapped) actions for the intent-parser system prompt. */
	public static String buildActionCatalogForPrompt() {
		StringBuilder sb = new StringBuilder();
		sb.append("Available actions (mapped from onboarding achievements):\n");
		for (ChatMapping mapping : CHAT_MAPPINGS) {
			sb.append("- ").append(mapping.action().name())
					.append(" [").append(mapping.achievementId()).append("]: ")
					.append(achievementLabel(mapping.achievementId())).append(" — ")
					.append(achievementHint(mapping.achievementId())).append('\n');
		}
		for (UrsulaAction action : UrsulaAction.values()) {
			if (action == UrsulaAction.UNKNOWN || action == UrsulaAction.HELP || action == UrsulaAction.LIST_LAYERS) {
				continue;
			}
			if (isMapped(action)) {
				continue;
			}
			sb.append("- ").append(action.name()).append(": ").append(action.getDescription()).append('\n');
		}
		return sb.toString();
	}

	/** Spanish help bullets derived from achievement hints plus a few extras. */
	public static String buildHelpBullets() {
		List<String> bullets = new ArrayList<>();
		for (ChatMapping mapping : CHAT_MAPPINGS) {
			String hint = achievementHint(mapping.achievementId());
			if (!hint.isBlank()) {
				bullets.add("• " + phraseFromHint(hint));
			}
		}
		bullets.add("• capas cargadas / ayuda");
		bullets.add("• activar o desactivar una rama completa: clic en el checkbox del nodo de la rama en el árbol de capas");
		return String.join("\n", bullets);
	}

	/** Localized achievement title from i18n, or the raw id as fallback. */
	public static String achievementLabel(String achievementId) {
		return msg(ACHIEVEMENT_KEY_PREFIX + achievementId, achievementId);
	}

	/** Localized onboarding hint for the logro, or empty if missing. */
	public static String achievementHint(String achievementId) {
		return msg(HINT_KEY_PREFIX + achievementId, "");
	}

	/** Whether {@code action} already appears in {@link #CHAT_MAPPINGS}. */
	private static boolean isMapped(UrsulaAction action) {
		return CHAT_MAPPINGS.stream().anyMatch(m -> m.action() == action);
	}

	/**
	 * Scores one mapping via phrase match, label/hint token overlap, and
	 * domain-specific boosts/penalties (margin, NDVI, recorrida, …).
	 */
	private static double scoreMapping(String normalizedUser, ChatMapping mapping) {
		double score = 0;
		for (String phrase : mapping.phrases()) {
			if (matchesPhrase(normalizedUser, normalize(phrase))) {
				score = Math.max(score, 10.0);
			}
		}
		score = Math.max(score, tokenOverlapScore(normalizedUser, achievementLabel(mapping.achievementId())));
		score = Math.max(score, tokenOverlapScore(normalizedUser, achievementHint(mapping.achievementId())));
		score = adjustMarginScore(normalizedUser, mapping.achievementId(), score);
		score = adjustPolygonActivateScore(normalizedUser, mapping.action(), score);
		score = adjustSiembraQueryScore(normalizedUser, mapping.action(), score);
		score = adjustVoyagerHarvestScore(normalizedUser, mapping.action(), score);
		score = adjustExportScreenScore(normalizedUser, mapping.action(), score);
		score = adjustExportRecorridaScore(normalizedUser, mapping.action(), score);
		score = adjustNdviAsignacionScore(normalizedUser, mapping.action(), score);
		score = adjustRecorridaLoadScore(normalizedUser, mapping.action(), score);
		return score;
	}

	/** Label/hint token overlap for an achievement, with margin generate-vs-import bias. */
	private static double scoreHintForQuery(String normalizedUser, String achievementId) {
		double score = tokenOverlapScore(normalizedUser, achievementLabel(achievementId))
				+ tokenOverlapScore(normalizedUser, achievementHint(achievementId));
		return adjustMarginScore(normalizedUser, achievementId, score);
	}

	/** Boosts generate-margin and dampens import-margin when the query is about calculating. */
	private static double adjustMarginScore(String normalizedUser, String achievementId, double score) {
		if (!mentionsMargin(normalizedUser)) {
			return score;
		}
		if (OnboardingAchievements.FIRST_MARGEN_CALCULATED_FROM_LABORS.equals(achievementId)) {
			if (containsAnyToken(normalizedUser, GENERATION_VERBS)) {
				score = Math.max(score, 12.0);
			}
		}
		if (OnboardingAchievements.FIRST_MARGEN_IMPORTED.equals(achievementId)) {
			if (containsAnyToken(normalizedUser, GENERATION_VERBS) && !containsAnyToken(normalizedUser, IMPORT_VERBS)) {
				score *= 0.15;
			} else if (containsAnyToken(normalizedUser, IMPORT_VERBS)) {
				score = Math.max(score, 12.0);
			}
		}
		return score;
	}

	/** Prefers {@link UrsulaAction#ACTIVAR_POLIGONOS_SUPERFICIE} over create-polygon for area queries. */
	private static double adjustPolygonActivateScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isActivatePolygonsWithAreaQuery(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.CREAR_POLIGONO) {
			return score * 0.05;
		}
		return score;
	}

	/** Routes share/import-siembra away from margin generation when both keywords overlap. */
	private static double adjustSiembraQueryScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isSiembraShareOrImportQuery(normalizedUser) || mentionsMargin(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.GENERAR_MARGEN) {
			return score * 0.05;
		}
		if (action == UrsulaAction.COMPARTIR_SIEMBRA) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.IMPORT_SIEMBRA) {
			return Math.max(score, 13.0);
		}
		return score;
	}

	/**
	 * Prefers Voyager/.vy1 harvest import over the generic shapefile importer when
	 * the user mentions those formats.
	 */
	private static double adjustVoyagerHarvestScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isVoyagerHarvestQuery(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.IMPORT_COSECHA_VOYAGER) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.IMPORT_COSECHA) {
			return score * 0.05;
		}
		return score;
	}

	/** True when the user asks for Voyager / .vy1 harvest import (not shapefile). */
	public static boolean isVoyagerHarvestQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		return n.contains("voyager") || n.contains("vy1");
	}

	/** Prefers screen capture over labor/recorrida export when the user says pantalla/tela. */
	private static double adjustExportScreenScore(String normalizedUser, UrsulaAction action, double score) {
		if (!mentionsScreenExport(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.EXPORT_PANTALLA) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.EXPORT_LABOR || action == UrsulaAction.EXPORT_RECORRIDA) {
			return score * 0.05;
		}
		return score;
	}

	/** Dampens bare “exportar” matches that only hit recorrida via hint token overlap. */
	private static double adjustExportRecorridaScore(String normalizedUser, UrsulaAction action, double score) {
		if (action != UrsulaAction.EXPORT_RECORRIDA) {
			return score;
		}
		// Avoid matching bare "exportar …" to recorrida via hint token overlap.
		if (!normalizedUser.contains("recorrida")) {
			return score * 0.05;
		}
		return score;
	}

	/** Penalizes NDVI actions when the query is really about recorridas or lacks imagery words. */
	private static double adjustNdviAsignacionScore(String normalizedUser, UrsulaAction action, double score) {
		boolean ndviAction = action == UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES
				|| action == UrsulaAction.DOWNLOAD_NDVI
				|| action == UrsulaAction.BULK_NDVI_DOWNLOAD;
		if (!ndviAction) {
			return score;
		}
		// "últimas … lotes" overlaps the NDVI achievement hint; require explicit NDVI.
		if (normalizedUser.contains("recorrida") && !normalizedUser.contains("ndvi")) {
			return score * 0.02;
		}
		if (!normalizedUser.contains("ndvi") && !normalizedUser.contains("imagen")) {
			return score * 0.15;
		}
		return score;
	}

	/** Boosts load-recorridas and suppresses import/NDVI when the query is a DB load request. */
	private static double adjustRecorridaLoadScore(String normalizedUser, UrsulaAction action, double score) {
		if (!isRecorridaLoadQuery(normalizedUser)) {
			return score;
		}
		if (action == UrsulaAction.LOAD_RECORRIDAS) {
			return Math.max(score, 15.0);
		}
		if (action == UrsulaAction.IMPORT_RECORRIDA) {
			return score * 0.2;
		}
		if (action == UrsulaAction.DOWNLOAD_NDVI_ASIGNACIONES
				|| action == UrsulaAction.DOWNLOAD_NDVI
				|| action == UrsulaAction.BULK_NDVI_DOWNLOAD) {
			return score * 0.02;
		}
		return score;
	}

	/** True when the user asks to export/save/capture the screen (not a labor SHP). */
	public static boolean isExportScreenQuery(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		return mentionsScreenExport(normalize(userQuery));
	}

	/** True when both a screen word and an export/save/capture verb appear. */
	private static boolean mentionsScreenExport(String text) {
		boolean screen = text.contains("pantalla") || text.contains("tela") || text.contains("screen")
				|| text.contains("screenshot");
		boolean exportish = text.contains("export") || text.contains("guardar") || text.contains("salvar")
				|| text.contains("save") || text.contains("captur") || text.contains("screenshot");
		return screen && exportish;
	}

	/** True when the normalized text mentions margin or profitability. */
	private static boolean mentionsMargin(String text) {
		return text.contains("margen") || text.contains("margenes") || text.contains("rentabilidad");
	}

	/** Substring check against a fixed vocabulary set (verbs, stop lists, …). */
	private static boolean containsAnyToken(String text, Set<String> tokens) {
		for (String token : tokens) {
			if (text.contains(token)) {
				return true;
			}
		}
		return false;
	}

	/** All significant words of {@code phrase} must appear as substrings of {@code user}. */
	private static boolean matchesPhrase(String user, String phrase) {
		if (phrase.isBlank()) {
			return false;
		}
		for (String word : phrase.split("\\s+")) {
			if (word.length() < 3) {
				continue;
			}
			if (!user.contains(word)) {
				return false;
			}
		}
		return true;
	}

	/** Weighted sum of significant tokens from {@code text} found inside {@code user}. */
	private static double tokenOverlapScore(String user, String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		double score = 0;
		for (String token : significantTokens(normalize(text))) {
			if (user.contains(token)) {
				score += token.length() >= 6 ? 2.0 : 1.0;
			}
		}
		return score;
	}

	/** Distinct tokens of length ≥ 4 after stopword filtering. */
	private static List<String> significantTokens(String text) {
		return Arrays.stream(text.split("\\s+"))
				.filter(t -> t.length() >= 4)
				.filter(t -> !STOPWORDS.contains(t))
				.distinct()
				.toList();
	}

	/** Short Spanish acknowledgement used when a mapping is chosen. */
	static String suggestReply(String achievementId, UrsulaAction action) {
		if (action == UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
			return "¡Dale! Activo los polígonos con superficie mayor a cero.";
		}
		if (action == UrsulaAction.IMPORT_SIEMBRA) {
			return "¡Dale! Abrí el importador de siembra (SHP).";
		}
		if (action == UrsulaAction.IMPORT_COSECHA_VOYAGER) {
			return "¡Dale! Abro la importación de cosecha desde Voyager (.vy1).";
		}
		if (action == UrsulaAction.COMPARTIR_SIEMBRA) {
			return "¡Dale! Comparto la siembra activa (prescripción en línea con QR).";
		}
		if (action == UrsulaAction.EXPORT_PANTALLA) {
			return "¡Dale! Abro Exportar → Pantalla para guardar la captura.";
		}
		if (action == UrsulaAction.LOAD_RECORRIDAS) {
			return "¡Dale! Busco las recorridas guardadas que coincidan y las cargo en el mapa.";
		}
		String hint = achievementHint(achievementId);
		if (!hint.isBlank()) {
			return "¡Dale! " + phraseFromHint(hint);
		}
		return "¡Dale! " + action.getDescription();
	}

	/** First sentence of an onboarding hint, kept for short help bullets / replies. */
	private static String phraseFromHint(String hint) {
		String trimmed = hint.trim();
		int end = trimmed.indexOf('.');
		if (end > 0) {
			return trimmed.substring(0, end) + ".";
		}
		return trimmed;
	}

	/**
	 * Lowercases, strips accents/punctuation, and collapses whitespace so phrase
	 * matching is language-tolerant across Spanish/English chat.
	 */
	public static String normalize(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		return stripped.replaceAll("[^a-z0-9\\s]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	/** Localized string for {@code key}, or {@code fallback} when the bundle has no entry. */
	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}

	private static final Set<String> STOPWORDS = Set.of(
			"para", "desde", "sobre", "como", "usa", "usá", "usar", "puedes", "podes", "podés",
			"primera", "primero", "vez", "with", "from", "your", "the", "and", "una", "uno",
			"varios", "varias", "nuevo", "nueva", "menu", "menú", "herramientas", "tools");

	private static final Set<String> GENERATION_VERBS = Set.of(
			"generar", "genero", "genera", "generado", "crear", "creo", "calcular", "calculo", "armar");

	private static final Set<String> IMPORT_VERBS = Set.of(
			"importar", "importo", "abrir", "cargar", "shapefile");
}
