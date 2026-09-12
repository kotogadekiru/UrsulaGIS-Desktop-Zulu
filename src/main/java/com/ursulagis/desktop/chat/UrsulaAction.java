package com.ursulagis.desktop.chat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalog of desktop actions the Ursula chat assistant can trigger.
 * Each constant declares whether a labor, harvest, or scouting route must be
 * resolved before {@link ChatActionExecutor} runs the corresponding UI path.
 */
public enum UrsulaAction {

	HELP(false, false, false, "List available commands"),
	LIST_LAYERS(false, false, false, "List loaded map layers and their active state"),
	UNKNOWN(false, false, false, "Unrecognized request"),

	// --- Import ---
	IMPORT_COSECHA(false, false, false, "Import harvest map from shapefile"),
	IMPORT_COSECHA_VOYAGER(false, false, false, "Import harvest from Voyager 2 card (.vy1)"),
	IMPORT_SIEMBRA(false, false, false, "Import seeding map from shapefile"),
	IMPORT_SIEMBRA_SRM(false, false, false, "Import seeding from TIM SRM"),
	IMPORT_FERTILIZACION(false, false, false, "Import fertilization map from shapefile"),
	IMPORT_PULVERIZACION(false, false, false, "Import spray map from shapefile"),
	IMPORT_RECORRIDA(false, false, false, "Import scouting route map"),
	IMPORT_NDVI(false, false, false, "Import NDVI raster"),
	IMPORT_IMAGERY(false, false, false, "Import imagery / raster image"),
	IMPORT_SUELO(false, false, false, "Import soil map"),
	IMPORT_MARGEN(false, false, false, "Import margin map from shapefile"),
	IMPORT_POLIGONO(false, false, false, "Import polygon layers from KML or SHP"),

	LOAD_RECORRIDAS(false, false, false, "Load saved scouting routes from the DB"),
	BULK_NDVI_DOWNLOAD(false, false, false, "Bulk download NDVI imagery"),
	DOWNLOAD_NDVI_ASIGNACIONES(false, false, false, "Download NDVI for assignment contours"),
	DOWNLOAD_NDVI(true, false, false, "Download NDVI for a labor"),

	// --- Polygon ---
	CREAR_POLIGONO(false, false, false, "Draw polygon / measure area"),
	MEDIR_DISTANCIA(false, false, false, "Measure distance on map"),
	ACTIVAR_POLIGONOS_SUPERFICIE(false, false, false, "Enable polygons with area > 0"),
	UNIR_POLIGONOS(false, false, false, "Union enabled polygons"),
	INTERSECTAR_POLIGONOS(false, false, false, "Intersect enabled polygons"),
	EXTRAER_POLIGONOS(true, false, false, "Extract polygons from a labor"),
	EXTRAER_CONTORNO(true, false, false, "Extract contour polygon from a labor"),
	CONVERTIR_POLIGONO_A_COSECHA(false, false, false, "Convert polygons to harvest"),
	CONVERTIR_POLIGONO_A_SIEMBRA(false, false, false, "Convert polygons to seeding"),
	CONVERTIR_POLIGONO_A_FERTILIZACION(false, false, false, "Convert polygons to fertilization"),
	CONVERTIR_POLIGONO_A_PULVERIZACION(false, false, false, "Convert polygons to spray"),
	CONVERTIR_POLIGONO_A_SUELO(false, false, false, "Convert polygon to soil map"),
	GUARDAR_POLIGONO(false, false, false, "Save selected polygons locally"),
	EDITAR_POLIGONO(false, false, false, "Edit selected polygon"),
	CLONAR_POLIGONO(false, false, false, "Clone selected polygon"),
	SIMPLIFICAR_POLIGONO(false, false, false, "Simplify selected polygon"),
	EXPLOTAR_POLIGONO(false, false, false, "Explode selected polygon"),
	GO_TO_POLIGONO(false, false, false, "Zoom to selected polygon"),
	POLIGONO_A_RECORRIDA(false, false, false, "Convert path/polygon to scouting route"),
	POLIGONO_A_CIRCULO(false, false, false, "Convert polygon to circle"),
	EDITAR_CAMINO(false, false, false, "Edit path (camino)"),
	ACORTAR_CAMINO(false, false, false, "Shorten path (camino)"),

	// --- Harvest ---
	COMPARTIR_COSECHA(true, true, false, "Share harvest map"),
	GRILLAR_COSECHA(true, true, false, "Grid a harvest map"),
	SUMAR_COSECHAS(false, false, false, "Sum selected harvest maps"),
	UNIR_COSECHAS(true, true, false, "Join harvest maps"),
	EDITAR_COSECHA(true, true, false, "Edit and reprocess harvest"),
	SHOW_COSECHA_ELEVATION_CHART(true, true, false, "Show harvest amount vs elevation chart"),
	GENERAR_RECORRIDA_DIRIGIDA(true, false, false, "Generate directed scouting from labor"),
	COSECHA_A_SUELO(true, true, false, "Create soil map from harvest"),
	EXPORT_COSECHA_PUNTOS(true, true, false, "Export harvest to points"),
	RECOMENDAR_FERT_N(true, true, false, "Recommend N fertilization from harvest"),
	RECOMENDAR_FERT_P(true, true, false, "Recommend P fertilization from harvest"),
	RECOMENDAR_FERT_P_BALANCE(true, true, false, "Recommend P balance fertilization from harvest"),
	RECOMENDAR_FERT_K(true, true, false, "Recommend K fertilization from harvest"),
	RECOMENDAR_FERT_S(true, true, false, "Recommend S fertilization from harvest"),
	COSECHA_A_COSECHA(true, true, false, "Create harvest from harvest"),
	COSECHA_A_FERTILIZACION(true, true, false, "Create fertilization from harvest"),
	COSECHA_A_PULVERIZACION(true, true, false, "Create spray from harvest"),
	SIEMBRA_DESDE_COSECHA(true, true, false, "Create seeding from harvest"),

	// --- Fertilization ---
	COMPARTIR_FERTILIZACION(true, false, false, "Share fertilization prescription"),
	EXPORT_FERTILIZACION(true, false, false, "Export fertilization prescription"),
	UNIR_FERTILIZACIONES(false, false, false, "Join/grid fertilization maps"),
	PARTIR_FERTILIZACION(true, false, false, "Split a fertilization map"),
	EDITAR_FERTILIZACION(true, false, false, "Edit and reprocess fertilization"),
	SIEMBRA_DESDE_FERTILIZACION(true, false, false, "Create seeding from fertilization"),

	// --- Seeding ---
	COMPARTIR_SIEMBRA(true, false, false, "Share seeding prescription (QR)"),
	EXPORT_SIEMBRA(true, false, false, "Export seeding prescription"),
	EXPORT_SIEMBRA_SRM(true, false, false, "Export seeding prescription as TIM SRM"),
	UNIR_SIEMBRAS(false, false, false, "Join seeding maps"),
	GRILLAR_SIEMBRA(true, false, false, "Grid a seeding map"),
	EDITAR_SIEMBRA(true, false, false, "Edit and reprocess seeding"),
	GENERAR_SIEMBRA_FERTILIZADA(false, false, false, "Generate fertilized seeding"),

	// --- Spray ---
	COMPARTIR_PULVERIZACION(true, false, false, "Share spray prescription"),
	EXPORT_PULVERIZACION(true, false, false, "Export spray prescription SHP"),
	EXPORT_PULVERIZACION_JSON(true, false, false, "Export spray prescription JSON"),
	UNIR_PULVERIZACIONES(false, false, false, "Join/grid spray maps"),
	EDITAR_PULVERIZACION(true, false, false, "Edit and reprocess spray map"),

	// --- Soil ---
	BALANCE_NUTRIENTES(false, false, false, "Run nutrient balance"),
	EDITAR_SUELO(true, false, false, "Edit and reprocess soil map"),
	ESTIMAR_RENDIMIENTO_SUELO(true, false, false, "Estimate yield potential from soil"),

	// --- Recorrida ---
	UPDATE_RECORRIDA(false, false, true, "Sync scouting route from cloud"),
	EXPORT_RECORRIDA(false, false, true, "Export scouting route"),
	COMPARTIR_RECORRIDA(false, false, true, "Share scouting route"),
	GUARDAR_RECORRIDA(false, false, true, "Save scouting route locally"),
	INTERPOLAR_RECORRIDA(false, false, true, "Interpolate scouting route to soil"),
	ASIGNAR_VALORES_RECORRIDA(false, false, true, "Assign lab values to survey samples"),
	GO_TO_RECORRIDA(false, false, true, "Zoom to scouting route"),
	EDITAR_RECORRIDA(false, false, true, "Edit scouting route table"),

	// --- Margin ---
	GENERAR_MARGEN(false, false, false, "Generate margin map from active labors"),
	EDITAR_MARGEN(true, false, false, "Edit margin settings and recompute"),
	SUMAR_MARGENES(false, false, false, "Sum selected margin maps"),

	// --- NDVI ---
	CONVERTIR_NDVI_A_COSECHA(false, false, false, "Convert NDVI to harvest"),
	CONVERTIR_NDVI_A_FERTILIZACION(false, false, false, "Convert NDVI to fertilization"),
	CONVERTIR_NDVI_ACUM_A_COSECHA(false, false, false, "Convert accumulated NDVI to harvest"),
	EXPORT_NDVI(false, false, false, "Export NDVI to Excel/KMZ"),
	EXPORT_NDVI_TIFF(false, false, false, "Export NDVI layer to TIFF"),
	SHOW_NDVI_CHART(false, false, false, "Show NDVI chart"),
	SHOW_NDVI_ACUM_CHART(false, false, false, "Show accumulated NDVI chart"),
	SHOW_NDVI_EVOLUTION(false, false, false, "Show NDVI evolution"),
	SHOW_NDVI_HISTOGRAM(false, false, false, "Show NDVI histogram"),
	FILTRAR_NDVI_FECHA(false, false, false, "Filter NDVI by date range"),
	FILTRAR_NDVI_NUBLADO(false, false, false, "Filter cloudy NDVI"),
	GUARDAR_NDVI(false, false, false, "Save NDVI"),
	GO_TO_NDVI(false, false, false, "Zoom to selected NDVI"),
	EDITAR_NDVI(false, false, false, "Rename/edit selected NDVI"),

	// --- Config / tables ---
	SHOW_LABORES_TABLE(false, false, false, "Show labores table"),
	COMPARE_ACTIVE_LAYERS(false, false, false, "Compare active layers (multi-layer histogram)"),
	CONFIG_ASIGNACION(false, false, false, "Open lot activity allocation"),
	EXPORT_PANTALLA(false, false, false, "Export screen snapshot"),
	GENERAR_ORDEN_COMPRA(false, false, false, "Generate purchase order"),
	COTIZAR_ORDEN_COMPRA(false, false, false, "Quote purchase order online"),
	GO_TO_ADDRESS(false, false, false, "Go to address on map"),
	CAMBIAR_PROYECTO(false, false, false, "Change project database"),
	ACTUALIZAR_APP(false, false, false, "Check for app updates"),
	CORRELACIONAR_CAPAS(false, false, false, "Correlate two layers"),
	CAMBIAR_IDIOMA(false, false, false, "Change application language"),
	CONFIG_CULTIVOS(false, false, false, "Open crops configuration"),
	CONFIG_FERTILIZANTES(false, false, false, "Open fertilizers configuration"),
	CONFIG_AGROQUIMICOS(false, false, false, "Open agrochemicals configuration"),
	CONFIG_SEMILLAS(false, false, false, "Open seeds configuration"),
	CONFIG_PLAGA(false, false, false, "Open pests configuration"),
	CONFIG_EMPRESA(false, false, false, "Open company configuration"),
	CONFIG_ESTABLECIMIENTO(false, false, false, "Open farm configuration"),
	CONFIG_LOTE(false, false, false, "Open lot configuration"),
	CONFIG_CAMPANIA(false, false, false, "Open campaign configuration"),
	CONFIG_POLIGONOS_TABLE(false, false, false, "Open polygons table"),
	SHOW_NDVI_TABLE(false, false, false, "Open NDVI table"),
	SHOW_RECORRIDAS_TABLE(false, false, false, "Open scouting routes table"),
	SHOW_ORDENES_COMPRA(false, false, false, "Open purchase orders table"),
	SHOW_ORDENES_PULVERIZACION(false, false, false, "Open spray orders table"),
	SHOW_ORDENES_FERTILIZACION(false, false, false, "Open fertilization orders table"),
	SHOW_ORDENES_SIEMBRA(false, false, false, "Open seeding orders table"),
	SHOW_CONFIG_TABLE(false, false, false, "Open configuration properties table"),
	SHOW_ACHIEVEMENTS(false, false, false, "Show onboarding achievements"),
	SHOW_ACERCA_DE(false, false, false, "Show about dialog"),
	SHOW_LOG(false, false, false, "Show application log"),

	// --- Generic labor ---
	GO_TO_LAYER(true, false, false, "Zoom to a labor layer"),
	RESUMIR_LABOR(true, false, false, "Summarize / simplify a labor map"),
	EXPORT_LABOR(true, false, false, "Export labor to shapefile"),
	CLONAR_LABOR(true, false, false, "Clone a labor map"),
	GUARDAR_LABOR(true, false, false, "Save labor locally"),
	FILTRAR_OUTLIERS(true, false, false, "Filter outliers from a labor"),
	ACENTUAR_MEDIA_LABOR(true, false, false, "Accentuate mean of a labor map"),
	SHOW_HISTOGRAMA_LABOR(true, false, false, "Show labor histogram"),
	SHOW_LABOR_TABLE(true, false, false, "Show labor data table"),
	CORTAR_LABOR_POR_POLIGONO(true, false, false, "Cut labor by enabled polygons"),
	REMOVE_LAYER(true, false, false, "Remove labor layer from map"),
	LAYER_TRANSPARENCIA(true, false, false, "Adjust layer transparency"),
	REPORTE_PDF_LABOR(true, false, false, "Generate PDF report for a labor"),
	JUNTAR_SHAPES(false, false, false, "Merge shapefiles");

	private final boolean requiresLabor;
	private final boolean requiresCosecha;
	private final boolean requiresRecorrida;
	private final String description;

	UrsulaAction(boolean requiresLabor, boolean requiresCosecha, boolean requiresRecorrida, String description) {
		this.requiresLabor = requiresLabor;
		this.requiresCosecha = requiresCosecha;
		this.requiresRecorrida = requiresRecorrida;
		this.description = description;
	}

	public boolean requiresLabor() {
		return requiresLabor;
	}

	public boolean requiresCosecha() {
		return requiresCosecha;
	}

	public boolean requiresRecorrida() {
		return requiresRecorrida;
	}

	public String getDescription() {
		return description;
	}

	public static Optional<UrsulaAction> fromId(String id) {
		if (id == null || id.isBlank()) {
			return Optional.empty();
		}
		String normalized = id.trim().toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
				.filter(a -> a.name().equals(normalized))
				.findFirst();
	}
}
