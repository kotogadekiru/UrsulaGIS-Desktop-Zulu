package com.ursulagis.desktop.chat.workflow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.ursulagis.desktop.chat.LoadedLayerInfo;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.JFXMain;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * Map-layer utilities for the fertilized-seeding chat workflow.
 * Finds polygons, NDVI, and labor layers, toggles visibility, and zooms the globe
 * so {@link SiembraFertilizadaOrchestrator} can drive each step without duplicating GIS lookups.
 */
public final class WorkflowLayerHelper {

	/** Prevents instantiation. */
	private WorkflowLayerHelper() {
	}

	/**
	 * Finds polygons whose names match {@code nameHint}, with fuzzy fallback to the closest names
	 * when there is no substring match.
	 *
	 * @param main     application main window
	 * @param nameHint field or environment name fragment (blank returns all polygons)
	 * @return matching polygons, possibly a short fuzzy shortlist
	 */
	public static List<Poligono> findPolygons(JFXMain main, String nameHint) {
		return findPolygons(main, nameHint, false);
	}

	/**
	 * Only returns polygons whose name contains the hint (no fuzzy fallback).
	 *
	 * @param main     application main window
	 * @param nameHint required name substring
	 * @return strict matches, or empty if none
	 */
	public static List<Poligono> findPolygonsStrict(JFXMain main, String nameHint) {
		return findPolygons(main, nameHint, true);
	}

	/**
	 * Shared polygon lookup: substring match first; if none and not strict, returns up to three
	 * fuzzy candidates sorted by name distance.
	 */
	private static List<Poligono> findPolygons(JFXMain main, String nameHint, boolean strict) {
		List<Poligono> all = castPolygons(main.getObjectFromLayersOfClass(Poligono.class));
		if (nameHint == null || nameHint.isBlank()) {
			return all;
		}
		String needle = nameHint.toLowerCase(Locale.ROOT);
		List<Poligono> matches = all.stream()
				.filter(p -> p.getNombre() != null && p.getNombre().toLowerCase(Locale.ROOT).contains(needle))
				.toList();
		if (!matches.isEmpty()) {
			return new ArrayList<>(matches);
		}
		if (strict) {
			return List.of();
		}
		return all.stream()
				.sorted(Comparator.comparingInt(p -> nameDistance(needle, p.getNombre())))
				.limit(3)
				.toList();
	}

	/**
	 * Prefer the main field contour when several polygons match the field name
	 * (skips ambient names like loma/pehuen and picks the largest remaining area).
	 *
	 * @param candidates polygons that matched a field hint
	 * @return best contour guess, or {@code null} if the list is empty
	 */
	public static Poligono preferFieldContour(List<Poligono> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		List<Poligono> nonAmbient = candidates.stream()
				.filter(p -> !isAmbientName(p.getNombre()))
				.toList();
		List<Poligono> pool = nonAmbient.isEmpty() ? candidates : nonAmbient;
		return pool.stream()
				.max(Comparator.comparingDouble(WorkflowLayerHelper::polygonAreaSafe))
				.orElse(pool.get(0));
	}

	/**
	 * Whether a polygon name looks like an environment zone rather than the field contour.
	 *
	 * @param name layer display name
	 * @return {@code true} for names containing loma, baja, alta, pehuen, baguette, etc.
	 */
	public static boolean isAmbientName(String name) {
		if (name == null) {
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		return n.contains("loma") || n.contains("baja") || n.contains("alta")
				|| n.contains("pehuen") || n.contains("baguette");
	}

	/** Safe geometry area for sorting; returns 0 if geometry is missing or fails. */
	private static double polygonAreaSafe(Poligono p) {
		try {
			var g = p.toGeometry();
			return g != null ? g.getArea() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Finds polygons matching {@code nameHint} from a chat {@link MapLayerContext} snapshot.
	 *
	 * @param ctx      layer context (may be {@code null})
	 * @param nameHint name filter passed to the context
	 * @return matching {@link Poligono} entities, or empty
	 */
	public static List<Poligono> findPolygons(MapLayerContext ctx, String nameHint) {
		if (ctx == null) {
			return List.of();
		}
		return ctx.findPolygonsMatching(nameHint).stream()
				.map(info -> info.getEntity() instanceof Poligono p ? p : null)
				.filter(p -> p != null)
				.toList();
	}

	/**
	 * Enables only the given polygons on the map and refreshes the layer panel.
	 *
	 * @param main    application main window
	 * @param enabled polygons that should remain visible/enabled
	 */
	public static void setOnlyPolygonsEnabled(JFXMain main, List<Poligono> enabled) {
		for (Poligono poly : castPolygons(main.getObjectFromLayersOfClass(Poligono.class))) {
			if (poly.getLayer() != null) {
				poly.getLayer().setEnabled(enabled.contains(poly));
			}
		}
		refreshLayerPanel(main);
	}

	/**
	 * Enables or disables a single labor layer on the map.
	 *
	 * @param labor   harvest, seeding, or fertilization labor
	 * @param enabled desired layer enabled state
	 */
	public static void setLaborEnabled(Labor<?> labor, boolean enabled) {
		if (labor != null && labor.getLayer() != null) {
			labor.getLayer().setEnabled(enabled);
		}
	}

	/**
	 * Enables only the listed labor layers among all labor layers in the WorldWind model.
	 *
	 * @param main    application main window
	 * @param enabled labores that should stay enabled
	 */
	public static void setLaborsEnabled(JFXMain main, List<? extends Labor<?>> enabled) {
		LayerList layers = main.getWwd().getModel().getLayers();
		for (Layer layer : layers) {
			Object entity = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (entity instanceof Labor<?> labor) {
				layer.setEnabled(enabled.contains(labor));
			}
		}
		refreshLayerPanel(main);
	}

	/**
	 * Picks the NDVI layer with the highest mean NDVI from the live map.
	 *
	 * @param main application main window
	 * @return best NDVI if any has a mean value
	 */
	public static Optional<Ndvi> findBestNdvi(JFXMain main) {
		return castNdvis(main.getObjectFromLayersOfClass(Ndvi.class)).stream()
				.filter(n -> n.getMeanNDVI() != null)
				.max(Comparator.comparingDouble(Ndvi::getMeanNDVI));
	}

	/**
	 * Picks the NDVI with the highest mean from a chat layer-context snapshot.
	 *
	 * @param ctx layer context (may be {@code null})
	 * @return best NDVI if present
	 */
	public static Optional<Ndvi> findBestNdvi(MapLayerContext ctx) {
		if (ctx == null) {
			return Optional.empty();
		}
		return ctx.findNdviWithHighestMean()
				.map(LoadedLayerInfo::getEntity)
				.filter(Ndvi.class::isInstance)
				.map(Ndvi.class::cast);
	}

	/**
	 * Returns the last harvest labor currently loaded (typically the one just generated).
	 *
	 * @param main application main window
	 * @return latest {@link CosechaLabor}, if any
	 */
	public static Optional<CosechaLabor> findLatestCosecha(JFXMain main) {
		List<CosechaLabor> list = castCosechas(main.getObjectFromLayersOfClass(CosechaLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

	/**
	 * Returns the last fertilization labor currently loaded.
	 *
	 * @param main application main window
	 * @return latest {@link FertilizacionLabor}, if any
	 */
	public static Optional<FertilizacionLabor> findLatestFertilizacion(JFXMain main) {
		List<FertilizacionLabor> list = castFerts(main.getObjectFromLayersOfClass(FertilizacionLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

	/**
	 * Returns the last seeding labor currently loaded.
	 *
	 * @param main application main window
	 * @return latest {@link SiembraLabor}, if any
	 */
	public static Optional<SiembraLabor> findLatestSiembra(JFXMain main) {
		List<SiembraLabor> list = castSiembras(main.getObjectFromLayersOfClass(SiembraLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

	/**
	 * Zooms the globe to the centroid of the given polygon when geometry is available.
	 *
	 * @param main     application main window
	 * @param poligono target polygon (ignored if null)
	 */
	public static void zoomToPoligono(JFXMain main, Poligono poligono) {
		if (poligono == null || main.getWwd() == null) {
			return;
		}
		try {
			var geom = poligono.toGeometry();
			if (geom != null) {
				var c = geom.getCentroid();
				main.viewGoTo(Position.fromDegrees(c.getY(), c.getX()));
			}
		} catch (Exception ignored) {
			// optional zoom
		}
	}

	/**
	 * Refreshes the layer tree UI after enable/disable changes.
	 *
	 * @param main application main window
	 */
	public static void refreshLayerPanel(JFXMain main) {
		if (main.getLayerPanel() != null && main.getWwd() != null) {
			main.getLayerPanel().update(main.getWwd());
		}
	}

	/** Crude name-distance heuristic for fuzzy polygon ranking when strict match fails. */
	private static int nameDistance(String needle, String name) {
		if (name == null) {
			return Integer.MAX_VALUE;
		}
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.contains(needle) ? 0 : Math.abs(lower.length() - needle.length());
	}

	/** Unchecked cast of {@code getObjectFromLayersOfClass} results to {@link Poligono}. */
	@SuppressWarnings("unchecked")
	private static List<Poligono> castPolygons(List<?> list) {
		return (List<Poligono>) (List<?>) list;
	}

	/** Unchecked cast of layer-list results to {@link Ndvi}. */
	@SuppressWarnings("unchecked")
	private static List<Ndvi> castNdvis(List<?> list) {
		return (List<Ndvi>) (List<?>) list;
	}

	/** Unchecked cast of layer-list results to {@link CosechaLabor}. */
	@SuppressWarnings("unchecked")
	private static List<CosechaLabor> castCosechas(List<?> list) {
		return (List<CosechaLabor>) (List<?>) list;
	}

	/** Unchecked cast of layer-list results to {@link FertilizacionLabor}. */
	@SuppressWarnings("unchecked")
	private static List<FertilizacionLabor> castFerts(List<?> list) {
		return (List<FertilizacionLabor>) (List<?>) list;
	}

	/** Unchecked cast of layer-list results to {@link SiembraLabor}. */
	@SuppressWarnings("unchecked")
	private static List<SiembraLabor> castSiembras(List<?> list) {
		return (List<SiembraLabor>) (List<?>) list;
	}
}
