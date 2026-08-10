package com.ursulagis.desktop.gui.onboarding;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Gamified onboarding: tracks first-time user actions and shows reward dialogs.
 * Achievements are persisted per user via java.util.prefs.Preferences.
 */
public final class OnboardingAchievements {

    private static final String PREFS_NODE = "com/ursulagis/desktop/onboarding";
    private static final String PREFIX = "achievement.";
    private static final String SHOW_AT_START_KEY = "showAtStart";

    /** First time the user draws and confirms a polygon. */
    public static final String FIRST_POLYGON_DRAWN = "FIRST_POLYGON_DRAWN";
    /** First time the user downloads NDVI for a polygon. */
    public static final String FIRST_NDVI_DOWNLOADED = "FIRST_NDVI_DOWNLOADED";
    /** First time the user downloads NDVI for assignment contours of a campaign. */
    public static final String FIRST_NDVI_ASIGNACIONES_DOWNLOADED = "FIRST_NDVI_ASIGNACIONES_DOWNLOADED";
    /** First time the user converts NDVI to harvest (cosecha). */
    public static final String FIRST_CONVERT_NDVI_TO_HARVEST = "FIRST_CONVERT_NDVI_TO_HARVEST";
    /** First time the user recommends Phosphorous fertilization from harvest. */
    public static final String FIRST_P_FERTILIZATION_RECOMMENDED = "FIRST_P_FERTILIZATION_RECOMMENDED";
    /** First time the user converts NDVI to fertilization. */
    public static final String FIRST_NDVI_TO_FERTILIZATION = "FIRST_NDVI_TO_FERTILIZATION";
    /** First time the user exports NDVI (Excel or KMZ). */
    public static final String FIRST_NDVI_EXPORTED = "FIRST_NDVI_EXPORTED";
    /** First time the user opens the NDVI chart. */
    public static final String FIRST_NDVI_CHART_VIEWED = "FIRST_NDVI_CHART_VIEWED";
    /** First time the user opens the NDVI accumulated chart. */
    public static final String FIRST_NDVI_ACUM_CHART_VIEWED = "FIRST_NDVI_ACUM_CHART_VIEWED";
    /** First time the user shows NDVI evolution. */
    public static final String FIRST_NDVI_EVOLUTION_VIEWED = "FIRST_NDVI_EVOLUTION_VIEWED";
    /** First time the user converts accumulated NDVI to harvest. */
    public static final String FIRST_NDVI_ACUM_TO_HARVEST = "FIRST_NDVI_ACUM_TO_HARVEST";
    /** First time the user filters NDVI by date range. */
    public static final String FIRST_NDVI_DATE_FILTERED = "FIRST_NDVI_DATE_FILTERED";
    /** First time the user saves NDVI. */
    public static final String FIRST_NDVI_SAVED = "FIRST_NDVI_SAVED";
    /** First time the user exports a single NDVI layer to TIFF. */
    public static final String FIRST_NDVI_EXPORTED_TIFF = "FIRST_NDVI_EXPORTED_TIFF";
    /** First time the user opens the histogram for an NDVI layer. */
    public static final String FIRST_NDVI_HISTOGRAM_VIEWED = "FIRST_NDVI_HISTOGRAM_VIEWED";

    // Polygon (poligono) related achievements
    /** First time the user imports polygons from KML/SHP. */
    public static final String FIRST_POLYGON_IMPORTED = "FIRST_POLYGON_IMPORTED";
    /** First time the user unions multiple polygons. */
    public static final String FIRST_POLYGON_UNION = "FIRST_POLYGON_UNION";
    /** First time the user intersects polygons. */
    public static final String FIRST_POLYGON_INTERSECTED = "FIRST_POLYGON_INTERSECTED";
    /** First time the user extracts polygons from a labor. */
    public static final String FIRST_POLYGON_EXTRACTED = "FIRST_POLYGON_EXTRACTED";
    /** First time the user extracts a contour polygon from a labor. */
    public static final String FIRST_POLYGON_CONTOUR_EXTRACTED = "FIRST_POLYGON_CONTOUR_EXTRACTED";
    /** First time the user measures a distance path. */
    public static final String FIRST_DISTANCE_MEASURED = "FIRST_DISTANCE_MEASURED";
    /** First time the user converts polygons to a seeding map. */
    public static final String FIRST_POLYGON_TO_SIEMBRA = "FIRST_POLYGON_TO_SIEMBRA";
    /** First time the user converts polygons to a fertilization map. */
    public static final String FIRST_POLYGON_TO_FERTILIZATION = "FIRST_POLYGON_TO_FERTILIZATION";
    /** First time the user converts polygons to a pulverization map. */
    public static final String FIRST_POLYGON_TO_PULVERIZATION = "FIRST_POLYGON_TO_PULVERIZATION";
    /** First time the user converts polygons to a harvest map. */
    public static final String FIRST_POLYGON_TO_HARVEST = "FIRST_POLYGON_TO_HARVEST";
    /** First time the user converts a polygon to a soil map. */
    public static final String FIRST_POLYGON_TO_SOIL = "FIRST_POLYGON_TO_SOIL";

    // Harvest (cosecha) related achievements
    /** First time the user imports a harvest map. */
    public static final String FIRST_HARVEST_IMPORTED = "FIRST_HARVEST_IMPORTED";
    /** First time the user grids a harvest map. */
    public static final String FIRST_HARVEST_GRIDDED = "FIRST_HARVEST_GRIDDED";
    /** First time the user sums multiple harvest maps. */
    public static final String FIRST_HARVEST_SUMMED = "FIRST_HARVEST_SUMMED";
    /** First time the user shares a harvest online (QR). */
    public static final String FIRST_HARVEST_SHARED = "FIRST_HARVEST_SHARED";
    /** First time the user creates a soil map from harvest. */
    public static final String FIRST_HARVEST_SOIL_CREATED = "FIRST_HARVEST_SOIL_CREATED";
    /** First time the user exports harvest to points. */
    public static final String FIRST_HARVEST_POINTS_EXPORTED = "FIRST_HARVEST_POINTS_EXPORTED";

    /** First time the user recommends Nitrogen fertilization from harvest. */
    public static final String FIRST_N_FERTILIZATION_RECOMMENDED = "FIRST_N_FERTILIZATION_RECOMMENDED";
    /** First time the user recommends Phosphorous balance fertilization from harvest. */
    public static final String FIRST_P_BALANCE_FERTILIZATION_RECOMMENDED = "FIRST_P_BALANCE_FERTILIZATION_RECOMMENDED";

    /** First time the user converts a harvest to a new harvest. */
    public static final String FIRST_HARVEST_FROM_HARVEST = "FIRST_HARVEST_FROM_HARVEST";
    /** First time the user converts a harvest to fertilization. */
    public static final String FIRST_FERTILIZATION_FROM_HARVEST = "FIRST_FERTILIZATION_FROM_HARVEST";
    /** First time the user converts a harvest to pulverization. */
    public static final String FIRST_PULVERIZATION_FROM_HARVEST = "FIRST_PULVERIZATION_FROM_HARVEST";

    // Fertilization (fertilizacion) related achievements
    /** First time the user imports a fertilization map (fertilizacion shapefile). */
    public static final String FIRST_FERTILIZATION_IMPORTED = "FIRST_FERTILIZATION_IMPORTED";
    /** First time the user joins multiple fertilizations. */
    public static final String FIRST_FERTILIZATION_JOINED = "FIRST_FERTILIZATION_JOINED";
    /** First time the user grids a fertilization map. */
    public static final String FIRST_FERTILIZATION_GRIDDED = "FIRST_FERTILIZATION_GRIDDED";
    /** First time the user splits a fertilization labor into two parts. */
    public static final String FIRST_FERTILIZATION_SPLIT = "FIRST_FERTILIZATION_SPLIT";
    /** First time the user exports a fertilization prescription (SHP). */
    public static final String FIRST_FERTILIZATION_EXPORTED = "FIRST_FERTILIZATION_EXPORTED";
    /** First time the user shares a fertilization prescription online (QR). */
    public static final String FIRST_FERTILIZATION_SHARED = "FIRST_FERTILIZATION_SHARED";
    /** First time the user creates a seeding map starting from a fertilization map. */
    public static final String FIRST_SEEDING_FROM_FERTILIZATION = "FIRST_SEEDING_FROM_FERTILIZATION";

    // Seeding (siembra) related achievements
    /** First time the user imports a seeding map (siembra shapefile). */
    public static final String FIRST_SEEDING_IMPORTED = "FIRST_SEEDING_IMPORTED";
    /** First time the user joins multiple seeding maps. */
    public static final String FIRST_SEEDING_JOINED = "FIRST_SEEDING_JOINED";
    /** First time the user grids a seeding map. */
    public static final String FIRST_SEEDING_GRIDDED = "FIRST_SEEDING_GRIDDED";
    /** First time the user edits and reprocesses a seeding map. */
    public static final String FIRST_SEEDING_EDITED = "FIRST_SEEDING_EDITED";
    /** First time the user exports a seeding prescription (SHP). */
    public static final String FIRST_SEEDING_EXPORTED = "FIRST_SEEDING_EXPORTED";
    /** First time the user shares a seeding prescription online (QR). */
    public static final String FIRST_SEEDING_SHARED = "FIRST_SEEDING_SHARED";
    /** First time the user generates fertilized seeding from seeding + fertilization. */
    public static final String FIRST_FERTILIZED_SEEDING_GENERATED = "FIRST_FERTILIZED_SEEDING_GENERATED";
    /** First time the user creates a seeding map from a harvest map. */
    public static final String FIRST_SEEDING_FROM_HARVEST = "FIRST_SEEDING_FROM_HARVEST";

    // Pulverization (pulverizacion) related achievements
    /** First time the user imports a pulverization map (pulverizacion shapefile). */
    public static final String FIRST_PULVERIZATION_IMPORTED = "FIRST_PULVERIZATION_IMPORTED";
    /** First time the user joins multiple pulverizations into one map. */
    public static final String FIRST_PULVERIZATION_JOINED = "FIRST_PULVERIZATION_JOINED";
    /** First time the user grids a pulverization map. */
    public static final String FIRST_PULVERIZATION_GRIDDED = "FIRST_PULVERIZATION_GRIDDED";
    /** First time the user edits and reprocesses a pulverization map. */
    public static final String FIRST_PULVERIZATION_EDITED = "FIRST_PULVERIZATION_EDITED";
    /** First time the user exports a pulverization prescription (SHP). */
    public static final String FIRST_PULVERIZATION_EXPORTED = "FIRST_PULVERIZATION_EXPORTED";
    /** First time the user exports a pulverization prescription in JSON format. */
    public static final String FIRST_PULVERIZATION_EXPORTED_JSON = "FIRST_PULVERIZATION_EXPORTED_JSON";
    /** First time the user shares a pulverization prescription online (QR). */
    public static final String FIRST_PULVERIZATION_SHARED = "FIRST_PULVERIZATION_SHARED";

    // Soil (suelo) related achievements
    /** First time the user imports a soil map (suelo shapefile). */
    public static final String FIRST_SOIL_IMPORTED = "FIRST_SOIL_IMPORTED";
    /** First time the user edits and reprocesses a soil map. */
    public static final String FIRST_SOIL_EDITED = "FIRST_SOIL_EDITED";
    /** First time the user estimates harvest potential from a soil map. */
    public static final String FIRST_SOIL_YIELD_ESTIMATED = "FIRST_SOIL_YIELD_ESTIMATED";
    /** First time the user runs nutrient balance from selected layers. */
    public static final String FIRST_SOIL_NUTRIENT_BALANCE = "FIRST_SOIL_NUTRIENT_BALANCE";

    // Field survey / recorrida related achievements
    /** First time the user imports a field survey (recorrida) shapefile. */
    public static final String FIRST_RECORRIDA_IMPORTED = "FIRST_RECORRIDA_IMPORTED";
    /** First time the user displays a guided field survey on the map. */
    public static final String FIRST_RECORRIDA_GUIDED_SHOWN = "FIRST_RECORRIDA_GUIDED_SHOWN";
    /** First time the user saves a field survey locally. */
    public static final String FIRST_RECORRIDA_SAVED_LOCAL = "FIRST_RECORRIDA_SAVED_LOCAL";
    /** First time the user interpolates a field survey to a soil map. */
    public static final String FIRST_RECORRIDA_INTERPOLATED_TO_SOIL = "FIRST_RECORRIDA_INTERPOLATED_TO_SOIL";
    /** First time the user shares a field survey (QR / URL). */
    public static final String FIRST_RECORRIDA_SHARED = "FIRST_RECORRIDA_SHARED";
    /** First time the user assigns lab/analysis values to survey samples. */
    public static final String FIRST_RECORRIDA_SAMPLES_VALUES_ASSIGNED = "FIRST_RECORRIDA_SAMPLES_VALUES_ASSIGNED";
    /** First time the user exports a field survey (SHP). */
    public static final String FIRST_RECORRIDA_EXPORTED = "FIRST_RECORRIDA_EXPORTED";
    /** First time the user syncs a field survey from the cloud. */
    public static final String FIRST_RECORRIDA_SYNCED_FROM_CLOUD = "FIRST_RECORRIDA_SYNCED_FROM_CLOUD";

    // Margin / profitability map (margen) related achievements
    /** First time the user imports a margin map from a shapefile. */
    public static final String FIRST_MARGEN_IMPORTED = "FIRST_MARGEN_IMPORTED";
    /** First time the user builds a margin map from selected labors (harvest, inputs, etc.). */
    public static final String FIRST_MARGEN_CALCULATED_FROM_LABORS = "FIRST_MARGEN_CALCULATED_FROM_LABORS";
    /** First time the user edits margin settings and recomputes the map. */
    public static final String FIRST_MARGEN_EDITED = "FIRST_MARGEN_EDITED";
    /** First time the user sums multiple margin maps into one. */
    public static final String FIRST_MARGEN_SUMMED = "FIRST_MARGEN_SUMMED";

    // App menu / ConfigGUI related achievements
    /** First time the user generates a purchase order from selected labors. */
    public static final String FIRST_CONFIG_PURCHASE_ORDER_GENERATED = "FIRST_CONFIG_PURCHASE_ORDER_GENERATED";
    /** First time the user sends a purchase order for online quoting (cloud / QR). */
    public static final String FIRST_CONFIG_PURCHASE_ORDER_QUOTED_ONLINE = "FIRST_CONFIG_PURCHASE_ORDER_QUOTED_ONLINE";
    /** First time the user opens the multi-layer histogram for active layers. */
    public static final String FIRST_CONFIG_MULTI_LAYER_HISTOGRAM = "FIRST_CONFIG_MULTI_LAYER_HISTOGRAM";
    /** First time the user uses Go to with an address that geocodes successfully. */
    public static final String FIRST_CONFIG_GO_TO_ADDRESS = "FIRST_CONFIG_GO_TO_ADDRESS";
    /** First time the user switches the project database file (.h2). */
    public static final String FIRST_CONFIG_PROJECT_CHANGED = "FIRST_CONFIG_PROJECT_CHANGED";
    /** First time the user downloads a new app version via the update action. */
    public static final String FIRST_CONFIG_APP_UPDATED = "FIRST_CONFIG_APP_UPDATED";
    /** First time the user correlates two layers and views the scatter chart. */
    public static final String FIRST_CONFIG_LAYERS_CORRELATED = "FIRST_CONFIG_LAYERS_CORRELATED";
    /** First time the user changes the application language / locale. */
    public static final String FIRST_CONFIG_LANGUAGE_CHANGED = "FIRST_CONFIG_LANGUAGE_CHANGED";
    /** First time the user assigns crop/campaign activities to a lot (Asignación). */
    public static final String FIRST_CONFIG_ASIGNACION_CREATED = "FIRST_CONFIG_ASIGNACION_CREATED";

    // Generic labor tools related achievements
    /** First time the user saves a labor to local storage/database. */
    public static final String FIRST_GENERIC_LABOR_SAVED = "FIRST_GENERIC_LABOR_SAVED";
    /** First time the user clones a labor map. */
    public static final String FIRST_GENERIC_LABOR_CLONED = "FIRST_GENERIC_LABOR_CLONED";
    /** First time the user summarizes a labor map. */
    public static final String FIRST_GENERIC_LABOR_SUMMARIZED = "FIRST_GENERIC_LABOR_SUMMARIZED";
    /** First time the user filters outliers from a labor map. */
    public static final String FIRST_GENERIC_LABOR_OUTLIERS_FILTERED = "FIRST_GENERIC_LABOR_OUTLIERS_FILTERED";
    /** First time the user exports a labor map to SHP. */
    public static final String FIRST_GENERIC_LABOR_EXPORTED = "FIRST_GENERIC_LABOR_EXPORTED";
    /** First time the user generates a PDF report for a labor. */
    public static final String FIRST_GENERIC_LABOR_PDF_REPORTED = "FIRST_GENERIC_LABOR_PDF_REPORTED";
    /** First time the user joins shapefiles into a single output shapefile. */
    public static final String FIRST_GENERIC_SHAPEFILES_JOINED = "FIRST_GENERIC_SHAPEFILES_JOINED";

    /** Controller/feature group for grouping achievements in the overview. */
    public static final String CONTROLLER_POLIGONO = "Poligono";
    public static final String CONTROLLER_NDVI = "Ndvi";
    public static final String CONTROLLER_COSECHA = "Cosecha";
    public static final String CONTROLLER_FERTILIZACION = "Fertilizacion";
    public static final String CONTROLLER_PULVERIZACION = "Pulverizacion";
    public static final String CONTROLLER_SIEMBRA = "Siembra";
    public static final String CONTROLLER_SUELO = "Suelo";
    public static final String CONTROLLER_RECORRIDA = "Recorrida";
    public static final String CONTROLLER_MARGEN = "Margen";
    public static final String CONTROLLER_CONFIG = "Config";
    public static final String CONTROLLER_GENERIC_LABOR = "GenericLabor";

    /** Controller display order for the overview dialog. */
    private static final String[] CONTROLLER_ORDER = { CONTROLLER_POLIGONO, CONTROLLER_NDVI, CONTROLLER_COSECHA, CONTROLLER_FERTILIZACION, CONTROLLER_PULVERIZACION, CONTROLLER_SIEMBRA, CONTROLLER_SUELO, CONTROLLER_RECORRIDA, CONTROLLER_MARGEN, CONTROLLER_CONFIG, CONTROLLER_GENERIC_LABOR };

    /** Maps each achievement ID to its controller. */
    private static final Map<String, String> ACHIEVEMENT_TO_CONTROLLER = new LinkedHashMap<>();
    static {
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_DRAWN, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_DOWNLOADED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_ASIGNACIONES_DOWNLOADED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_IMPORTED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_UNION, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_INTERSECTED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_EXTRACTED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_CONTOUR_EXTRACTED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_DISTANCE_MEASURED, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_TO_SIEMBRA, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_TO_FERTILIZATION, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_TO_PULVERIZATION, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_TO_HARVEST, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_POLYGON_TO_SOIL, CONTROLLER_POLIGONO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONVERT_NDVI_TO_HARVEST, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_TO_FERTILIZATION, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_EXPORTED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_CHART_VIEWED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_ACUM_CHART_VIEWED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_EVOLUTION_VIEWED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_ACUM_TO_HARVEST, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_DATE_FILTERED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_SAVED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_EXPORTED_TIFF, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_NDVI_HISTOGRAM_VIEWED, CONTROLLER_NDVI);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_P_FERTILIZATION_RECOMMENDED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_IMPORTED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_GRIDDED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_SUMMED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_SHARED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_SOIL_CREATED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_POINTS_EXPORTED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_N_FERTILIZATION_RECOMMENDED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_P_BALANCE_FERTILIZATION_RECOMMENDED, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_HARVEST_FROM_HARVEST, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_FROM_HARVEST, CONTROLLER_COSECHA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_FROM_HARVEST, CONTROLLER_COSECHA);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_IMPORTED, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_JOINED, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_GRIDDED, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_SPLIT, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_EXPORTED, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZATION_SHARED, CONTROLLER_FERTILIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_FROM_FERTILIZATION, CONTROLLER_SIEMBRA);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_IMPORTED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_JOINED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_GRIDDED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_EDITED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_EXPORTED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_SHARED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_FERTILIZED_SEEDING_GENERATED, CONTROLLER_SIEMBRA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SEEDING_FROM_HARVEST, CONTROLLER_SIEMBRA);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_IMPORTED, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_JOINED, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_GRIDDED, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_EDITED, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_EXPORTED, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_EXPORTED_JSON, CONTROLLER_PULVERIZACION);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_PULVERIZATION_SHARED, CONTROLLER_PULVERIZACION);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SOIL_IMPORTED, CONTROLLER_SUELO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SOIL_EDITED, CONTROLLER_SUELO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SOIL_YIELD_ESTIMATED, CONTROLLER_SUELO);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_SOIL_NUTRIENT_BALANCE, CONTROLLER_SUELO);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_IMPORTED, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_GUIDED_SHOWN, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_SAVED_LOCAL, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_INTERPOLATED_TO_SOIL, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_SHARED, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_SAMPLES_VALUES_ASSIGNED, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_EXPORTED, CONTROLLER_RECORRIDA);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_RECORRIDA_SYNCED_FROM_CLOUD, CONTROLLER_RECORRIDA);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_MARGEN_IMPORTED, CONTROLLER_MARGEN);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_MARGEN_CALCULATED_FROM_LABORS, CONTROLLER_MARGEN);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_MARGEN_EDITED, CONTROLLER_MARGEN);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_MARGEN_SUMMED, CONTROLLER_MARGEN);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_PURCHASE_ORDER_GENERATED, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_PURCHASE_ORDER_QUOTED_ONLINE, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_MULTI_LAYER_HISTOGRAM, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_GO_TO_ADDRESS, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_PROJECT_CHANGED, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_APP_UPDATED, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_LAYERS_CORRELATED, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_LANGUAGE_CHANGED, CONTROLLER_CONFIG);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_CONFIG_ASIGNACION_CREATED, CONTROLLER_CONFIG);

        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_SAVED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_CLONED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_SUMMARIZED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_OUTLIERS_FILTERED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_EXPORTED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_LABOR_PDF_REPORTED, CONTROLLER_GENERIC_LABOR);
        ACHIEVEMENT_TO_CONTROLLER.put(FIRST_GENERIC_SHAPEFILES_JOINED, CONTROLLER_GENERIC_LABOR);
    }

    /** All achievement IDs in display order (obtained first, then missing, or fixed order). */
    private static final String[] ALL_ACHIEVEMENT_IDS = {
        FIRST_POLYGON_DRAWN,
        FIRST_NDVI_DOWNLOADED,
        FIRST_NDVI_ASIGNACIONES_DOWNLOADED,
        FIRST_CONVERT_NDVI_TO_HARVEST,
        FIRST_P_FERTILIZATION_RECOMMENDED,
        FIRST_NDVI_TO_FERTILIZATION,
        FIRST_NDVI_EXPORTED,
        FIRST_NDVI_CHART_VIEWED,
        FIRST_NDVI_ACUM_CHART_VIEWED,
        FIRST_NDVI_EVOLUTION_VIEWED,
        FIRST_NDVI_ACUM_TO_HARVEST,
        FIRST_NDVI_DATE_FILTERED,
        FIRST_NDVI_SAVED,
        FIRST_NDVI_EXPORTED_TIFF,
        FIRST_NDVI_HISTOGRAM_VIEWED,
        FIRST_HARVEST_IMPORTED,
        FIRST_HARVEST_GRIDDED,
        FIRST_HARVEST_SUMMED,
        FIRST_HARVEST_SHARED,
        FIRST_HARVEST_SOIL_CREATED,
        FIRST_HARVEST_POINTS_EXPORTED,
        FIRST_N_FERTILIZATION_RECOMMENDED,
        FIRST_P_BALANCE_FERTILIZATION_RECOMMENDED,
        FIRST_HARVEST_FROM_HARVEST,
        FIRST_FERTILIZATION_FROM_HARVEST,
        FIRST_PULVERIZATION_FROM_HARVEST,
        FIRST_FERTILIZATION_IMPORTED,
        FIRST_FERTILIZATION_JOINED,
        FIRST_FERTILIZATION_GRIDDED,
        FIRST_FERTILIZATION_SPLIT,
        FIRST_FERTILIZATION_EXPORTED,
        FIRST_FERTILIZATION_SHARED,
        FIRST_PULVERIZATION_IMPORTED,
        FIRST_PULVERIZATION_JOINED,
        FIRST_PULVERIZATION_GRIDDED,
        FIRST_PULVERIZATION_EDITED,
        FIRST_PULVERIZATION_EXPORTED,
        FIRST_PULVERIZATION_EXPORTED_JSON,
        FIRST_PULVERIZATION_SHARED,
        FIRST_SOIL_IMPORTED,
        FIRST_SOIL_EDITED,
        FIRST_SOIL_YIELD_ESTIMATED,
        FIRST_SOIL_NUTRIENT_BALANCE,
        FIRST_RECORRIDA_IMPORTED,
        FIRST_RECORRIDA_GUIDED_SHOWN,
        FIRST_RECORRIDA_SAVED_LOCAL,
        FIRST_RECORRIDA_INTERPOLATED_TO_SOIL,
        FIRST_RECORRIDA_SHARED,
        FIRST_RECORRIDA_SAMPLES_VALUES_ASSIGNED,
        FIRST_RECORRIDA_EXPORTED,
        FIRST_RECORRIDA_SYNCED_FROM_CLOUD,
        FIRST_MARGEN_IMPORTED,
        FIRST_MARGEN_CALCULATED_FROM_LABORS,
        FIRST_MARGEN_EDITED,
        FIRST_MARGEN_SUMMED,
        FIRST_CONFIG_PURCHASE_ORDER_GENERATED,
        FIRST_CONFIG_PURCHASE_ORDER_QUOTED_ONLINE,
        FIRST_CONFIG_MULTI_LAYER_HISTOGRAM,
        FIRST_CONFIG_GO_TO_ADDRESS,
        FIRST_CONFIG_PROJECT_CHANGED,
        FIRST_CONFIG_APP_UPDATED,
        FIRST_CONFIG_LAYERS_CORRELATED,
        FIRST_CONFIG_LANGUAGE_CHANGED,
        FIRST_CONFIG_ASIGNACION_CREATED,
        FIRST_GENERIC_LABOR_SAVED,
        FIRST_GENERIC_LABOR_CLONED,
        FIRST_GENERIC_LABOR_SUMMARIZED,
        FIRST_GENERIC_LABOR_OUTLIERS_FILTERED,
        FIRST_GENERIC_LABOR_EXPORTED,
        FIRST_GENERIC_LABOR_PDF_REPORTED,
        FIRST_GENERIC_SHAPEFILES_JOINED,
        FIRST_SEEDING_FROM_FERTILIZATION,
        FIRST_SEEDING_IMPORTED,
        FIRST_SEEDING_JOINED,
        FIRST_SEEDING_GRIDDED,
        FIRST_SEEDING_EDITED,
        FIRST_SEEDING_EXPORTED,
        FIRST_SEEDING_SHARED,
        FIRST_FERTILIZED_SEEDING_GENERATED,
        FIRST_SEEDING_FROM_HARVEST,
        FIRST_POLYGON_IMPORTED,
        FIRST_POLYGON_UNION,
        FIRST_POLYGON_INTERSECTED,
        FIRST_POLYGON_EXTRACTED,
        FIRST_POLYGON_CONTOUR_EXTRACTED,
        FIRST_DISTANCE_MEASURED,
        FIRST_POLYGON_TO_SIEMBRA,
        FIRST_POLYGON_TO_FERTILIZATION,
        FIRST_POLYGON_TO_PULVERIZATION,
        FIRST_POLYGON_TO_HARVEST,
        FIRST_POLYGON_TO_SOIL
    };

    private static volatile OnboardingAchievements instance;
    private final Preferences prefs;
    private final CopyOnWriteArrayList<Runnable> onUnlockListeners = new CopyOnWriteArrayList<>();

    private OnboardingAchievements() {
        this.prefs = Preferences.userRoot().node(PREFS_NODE);
    }

    public static OnboardingAchievements getInstance() {
        if (instance == null) {
            synchronized (OnboardingAchievements.class) {
                if (instance == null) {
                    instance = new OnboardingAchievements();
                }
            }
        }
        return instance;
    }

    /** Returns true if the achievement was already unlocked. */
    public boolean isUnlocked(String achievementId) {
        return prefs.getBoolean(PREFIX + achievementId, false);
    }

    /**
     * Unlocks the achievement if first time and shows the reward dialog on the FX thread.
     * Safe to call from any thread; UI is shown via Platform.runLater.
     *
     * @param owner         stage to own the dialog (can be null)
     * @param achievementId one of the FIRST_* constants
     * @return true if this was the first time and the achievement was unlocked
     */
    public boolean unlock(Stage owner, String achievementId) {
        if (achievementId == null || achievementId.isEmpty()) return false;
        String key = PREFIX + achievementId;
        if (prefs.getBoolean(key, false)) return false;
        prefs.putBoolean(key, true);
        try {
            prefs.flush();
        } catch (Exception e) {
            // ignore
        }
        Stage stage = owner;
        Platform.runLater(() -> {
            AchievementUnlockedDialog.show(stage, achievementId);
            for (Runnable r : onUnlockListeners) {
                try {
                    r.run();
                } catch (Exception e) {
                    // ignore so one listener does not break others
                }
            }
        });
        return true;
    }

    /** Add a listener run on the FX thread when any achievement is unlocked. */
    public void addOnUnlockListener(Runnable listener) {
        if (listener != null) onUnlockListeners.add(listener);
    }

    /** Remove a listener previously added with {@link #addOnUnlockListener}. */
    public void removeOnUnlockListener(Runnable listener) {
        onUnlockListeners.remove(listener);
    }

    /** Returns all achievement IDs in display order. */
    public String[] getAllAchievementIds() {
        return ALL_ACHIEVEMENT_IDS.clone();
    }

    /** Returns the controller/group id for an achievement, or null if unknown. */
    public static String getControllerId(String achievementId) {
        return ACHIEVEMENT_TO_CONTROLLER.get(achievementId);
    }

    /** Returns controller IDs in the order they should appear in the overview. */
    public static String[] getControllerIdsInOrder() {
        return CONTROLLER_ORDER.clone();
    }

    /** Whether to show the achievements overview automatically at startup. */
    public boolean isShowAtStart() {
        return prefs.getBoolean(SHOW_AT_START_KEY, true);
    }

    /** Persist the 'show at start' preference. */
    public void setShowAtStart(boolean show) {
        prefs.putBoolean(SHOW_AT_START_KEY, show);
        try {
            prefs.flush();
        } catch (Exception e) {
            // ignore
        }
    }

    /** Resets all achievements (e.g. for testing). */
    public void resetAll() {
        try {
            prefs.clear();
            prefs.flush();
        } catch (Exception e) {
            // ignore
        }
    }
}