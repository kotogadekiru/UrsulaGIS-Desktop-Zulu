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

	/** Show help bullets of supported phrases. */
	HELP(false, false, false, "List available commands"),
	/** List loaded map layers and active/inactive state. */
	LIST_LAYERS(false, false, false, "List loaded map layers and their active state"),
	/** Fallback when the request cannot be mapped to a concrete action. */
	UNKNOWN(false, false, false, "Unrecognized request"),

	/** Open harvest shapefile import. */
	IMPORT_COSECHA(false, false, false, "Import harvest map from shapefile"),
	/** Open seeding shapefile import. */
	IMPORT_SIEMBRA(false, false, false, "Import seeding map from shapefile"),
	/** Import harvest from Voyager. */
	IMPORT_COSECHA_VOYAGER(false, false, false, "Import harvest from Voyager"),
	/** Import a scouting route shapefile. */
	IMPORT_RECORRIDA(false, false, false, "Import scouting route map"),
	/** Load saved recorridas from the DB (by crop/lot keywords). */
	LOAD_RECORRIDAS(false, false, false, "Load saved scouting routes from the DB matching crop/lot filters"),
	/** Import an NDVI raster/TIFF. */
	IMPORT_NDVI(false, false, false, "Import NDVI raster"),
	/** Import a soil map. */
	IMPORT_SUELO(false, false, false, "Import soil map"),
	/** Import a margin map shapefile. */
	IMPORT_MARGEN(false, false, false, "Import margin map from shapefile"),
	/** Generate a margin map from active labor layers (Rentabilidades). */
	GENERAR_MARGEN(false, false, false, "Generate margin map from active harvest, seeding, fertilization and spray layers"),
	/** Start bulk NDVI download tooling. */
	BULK_NDVI_DOWNLOAD(false, false, false, "Bulk download NDVI imagery"),
	/** Download NDVI for lot assignment contours of a campaign/crop/period. */
	DOWNLOAD_NDVI_ASIGNACIONES(false, false, false, "Download NDVI for assignment contours of a campaign/crop and period"),
	/** Run soil nutrient balance. */
	BALANCE_NUTRIENTES(false, false, false, "Run nutrient balance"),
	/** Merge multiple shapefiles. */
	JUNTAR_SHAPES(false, false, false, "Merge shapefiles"),
	/** Activate the map distance-measure tool. */
	MEDIR_DISTANCIA(false, false, false, "Measure distance on map"),
	/** Activate the draw-polygon / measure-area tool. */
	CREAR_POLIGONO(false, false, false, "Draw polygon / measure area"),
	/** Import polygon layers from KML or SHP. */
	IMPORT_POLIGONO(false, false, false, "Import polygon layers from KML or SHP"),
	/** Enable polygon layers whose area is greater than zero. */
	ACTIVAR_POLIGONOS_SUPERFICIE(false, false, false, "Enable polygon layers with area greater than zero"),
	/** Convert enabled polygons into a harvest map. */
	CONVERTIR_POLIGONO_A_COSECHA(false, false, false, "Convert enabled polygon layers on the map into a harvest map"),
	/** Convert enabled polygons into a seeding map. */
	CONVERTIR_POLIGONO_A_SIEMBRA(false, false, false, "Convert enabled polygon layers into a seeding map"),
	/** Convert enabled polygons into a fertilization map. */
	CONVERTIR_POLIGONO_A_FERTILIZACION(false, false, false, "Convert enabled polygon layers into a fertilization map"),
	/** Convert enabled polygons into a spray map. */
	CONVERTIR_POLIGONO_A_PULVERIZACION(false, false, false, "Convert enabled polygon layers into a spray map"),
	/** Open the labores table. */
	SHOW_LABORES_TABLE(false, false, false, "Show labores table"),
	/** Compare active layers via multi-layer histogram. */
	COMPARE_ACTIVE_LAYERS(false, false, false, "Compare active layers (multi-layer histogram)"),
	/** Open lot activity allocation (Asignación). */
	CONFIG_ASIGNACION(false, false, false, "Open lot activity allocation (Asignación)"),
	/** Export a screen snapshot (Exportar → Pantalla). */
	EXPORT_PANTALLA(false, false, false, "Export screen snapshot (Exportar → Pantalla)"),

	/** Zoom the map to a resolved labor layer. */
	GO_TO_LAYER(true, false, false, "Zoom to a labor layer"),
	/** Summarize / simplify a labor map. */
	RESUMIR_LABOR(true, false, false, "Summarize / simplify a labor map"),
	/** Export a labor to shapefile. */
	EXPORT_LABOR(true, false, false, "Export labor to shapefile"),
	/** Clone a labor map. */
	CLONAR_LABOR(true, false, false, "Clone a labor map"),
	/** Download NDVI for a specific labor footprint. */
	DOWNLOAD_NDVI(true, false, false, "Download NDVI for a labor"),
	/** Share a harvest map online. */
	COMPARTIR_COSECHA(true, true, false, "Share harvest map"),
	/** Share a seeding prescription (QR). */
	COMPARTIR_SIEMBRA(true, false, false, "Share seeding prescription (QR)"),

	/** Sync a scouting route from the cloud. */
	UPDATE_RECORRIDA(false, false, true, "Sync scouting route from cloud"),
	/** Export a scouting route. */
	EXPORT_RECORRIDA(false, false, true, "Export scouting route");

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

	/** Whether the executor must resolve a loaded labor before running. */
	public boolean requiresLabor() {
		return requiresLabor;
	}

	/** Whether a {@link com.ursulagis.desktop.dao.cosecha.CosechaLabor} specifically is required. */
	public boolean requiresCosecha() {
		return requiresCosecha;
	}

	/** Whether a loaded {@link com.ursulagis.desktop.dao.recorrida.Recorrida} is required. */
	public boolean requiresRecorrida() {
		return requiresRecorrida;
	}

	/** Short English description used in prompts and fallback replies. */
	public String getDescription() {
		return description;
	}

	/**
	 * Looks up an action by its enum name (case-insensitive).
	 *
	 * @param id action id from the AI JSON {@code action} field
	 * @return matching action, or empty if unknown/blank
	 */
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
