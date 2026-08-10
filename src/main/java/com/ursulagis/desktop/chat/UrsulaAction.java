package com.ursulagis.desktop.chat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalog of actions the Ursula assistant can trigger.
 */
public enum UrsulaAction {

	HELP(false, false, false, "List available commands"),
	LIST_LAYERS(false, false, false, "List loaded map layers and their active state"),
	UNKNOWN(false, false, false, "Unrecognized request"),

	IMPORT_COSECHA(false, false, false, "Import harvest map from shapefile"),
	IMPORT_SIEMBRA(false, false, false, "Import seeding map from shapefile"),
	IMPORT_COSECHA_VOYAGER(false, false, false, "Import harvest from Voyager"),
	IMPORT_RECORRIDA(false, false, false, "Import scouting route map"),
	IMPORT_NDVI(false, false, false, "Import NDVI raster"),
	IMPORT_SUELO(false, false, false, "Import soil map"),
	IMPORT_MARGEN(false, false, false, "Import margin map from shapefile"),
	GENERAR_MARGEN(false, false, false, "Generate margin map from active harvest, seeding, fertilization and spray layers"),
	BULK_NDVI_DOWNLOAD(false, false, false, "Bulk download NDVI imagery"),
	DOWNLOAD_NDVI_ASIGNACIONES(false, false, false, "Download NDVI for assignment contours of a campaign/crop and period"),
	BALANCE_NUTRIENTES(false, false, false, "Run nutrient balance"),
	JUNTAR_SHAPES(false, false, false, "Merge shapefiles"),
	MEDIR_DISTANCIA(false, false, false, "Measure distance on map"),
	CREAR_POLIGONO(false, false, false, "Draw polygon / measure area"),
	IMPORT_POLIGONO(false, false, false, "Import polygon layers from KML or SHP"),
	ACTIVAR_POLIGONOS_SUPERFICIE(false, false, false, "Enable polygon layers with area greater than zero"),
	CONVERTIR_POLIGONO_A_COSECHA(false, false, false, "Convert enabled polygon layers on the map into a harvest map"),
	CONVERTIR_POLIGONO_A_SIEMBRA(false, false, false, "Convert enabled polygon layers into a seeding map"),
	CONVERTIR_POLIGONO_A_FERTILIZACION(false, false, false, "Convert enabled polygon layers into a fertilization map"),
	CONVERTIR_POLIGONO_A_PULVERIZACION(false, false, false, "Convert enabled polygon layers into a spray map"),
	SHOW_LABORES_TABLE(false, false, false, "Show labores table"),
	COMPARE_ACTIVE_LAYERS(false, false, false, "Compare active layers (multi-layer histogram)"),
	CONFIG_ASIGNACION(false, false, false, "Open lot activity allocation (Asignación)"),

	GO_TO_LAYER(true, false, false, "Zoom to a labor layer"),
	RESUMIR_LABOR(true, false, false, "Summarize / simplify a labor map"),
	EXPORT_LABOR(true, false, false, "Export labor to shapefile"),
	CLONAR_LABOR(true, false, false, "Clone a labor map"),
	DOWNLOAD_NDVI(true, false, false, "Download NDVI for a labor"),
	COMPARTIR_COSECHA(true, true, false, "Share harvest map"),
	COMPARTIR_SIEMBRA(true, false, false, "Share seeding prescription (QR)"),

	UPDATE_RECORRIDA(false, false, true, "Sync scouting route from cloud"),
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
