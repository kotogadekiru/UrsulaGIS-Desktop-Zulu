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
 * Layer selection helpers for chat workflow orchestration.
 */
public final class WorkflowLayerHelper {

	private WorkflowLayerHelper() {
	}

	public static List<Poligono> findPolygons(JFXMain main, String nameHint) {
		return findPolygons(main, nameHint, false);
	}

	/** Only returns polygons whose name contains the hint (no fuzzy fallback). */
	public static List<Poligono> findPolygonsStrict(JFXMain main, String nameHint) {
		return findPolygons(main, nameHint, true);
	}

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

	/** Prefer the main field contour when several polygons match the field name. */
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

	public static boolean isAmbientName(String name) {
		if (name == null) {
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		return n.contains("loma") || n.contains("baja") || n.contains("alta")
				|| n.contains("pehuen") || n.contains("baguette");
	}

	private static double polygonAreaSafe(Poligono p) {
		try {
			var g = p.toGeometry();
			return g != null ? g.getArea() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	public static List<Poligono> findPolygons(MapLayerContext ctx, String nameHint) {
		if (ctx == null) {
			return List.of();
		}
		return ctx.findPolygonsMatching(nameHint).stream()
				.map(info -> info.getEntity() instanceof Poligono p ? p : null)
				.filter(p -> p != null)
				.toList();
	}

	public static void setOnlyPolygonsEnabled(JFXMain main, List<Poligono> enabled) {
		for (Poligono poly : castPolygons(main.getObjectFromLayersOfClass(Poligono.class))) {
			if (poly.getLayer() != null) {
				poly.getLayer().setEnabled(enabled.contains(poly));
			}
		}
		refreshLayerPanel(main);
	}

	public static void setLaborEnabled(Labor<?> labor, boolean enabled) {
		if (labor != null && labor.getLayer() != null) {
			labor.getLayer().setEnabled(enabled);
		}
	}

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

	public static Optional<Ndvi> findBestNdvi(JFXMain main) {
		return castNdvis(main.getObjectFromLayersOfClass(Ndvi.class)).stream()
				.filter(n -> n.getMeanNDVI() != null)
				.max(Comparator.comparingDouble(Ndvi::getMeanNDVI));
	}

	public static Optional<Ndvi> findBestNdvi(MapLayerContext ctx) {
		if (ctx == null) {
			return Optional.empty();
		}
		return ctx.findNdviWithHighestMean()
				.map(LoadedLayerInfo::getEntity)
				.filter(Ndvi.class::isInstance)
				.map(Ndvi.class::cast);
	}

	public static Optional<CosechaLabor> findLatestCosecha(JFXMain main) {
		List<CosechaLabor> list = castCosechas(main.getObjectFromLayersOfClass(CosechaLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

	public static Optional<FertilizacionLabor> findLatestFertilizacion(JFXMain main) {
		List<FertilizacionLabor> list = castFerts(main.getObjectFromLayersOfClass(FertilizacionLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

	public static Optional<SiembraLabor> findLatestSiembra(JFXMain main) {
		List<SiembraLabor> list = castSiembras(main.getObjectFromLayersOfClass(SiembraLabor.class));
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
	}

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

	public static void refreshLayerPanel(JFXMain main) {
		if (main.getLayerPanel() != null && main.getWwd() != null) {
			main.getLayerPanel().update(main.getWwd());
		}
	}

	private static int nameDistance(String needle, String name) {
		if (name == null) {
			return Integer.MAX_VALUE;
		}
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.contains(needle) ? 0 : Math.abs(lower.length() - needle.length());
	}

	@SuppressWarnings("unchecked")
	private static List<Poligono> castPolygons(List<?> list) {
		return (List<Poligono>) (List<?>) list;
	}

	@SuppressWarnings("unchecked")
	private static List<Ndvi> castNdvis(List<?> list) {
		return (List<Ndvi>) (List<?>) list;
	}

	@SuppressWarnings("unchecked")
	private static List<CosechaLabor> castCosechas(List<?> list) {
		return (List<CosechaLabor>) (List<?>) list;
	}

	@SuppressWarnings("unchecked")
	private static List<FertilizacionLabor> castFerts(List<?> list) {
		return (List<FertilizacionLabor>) (List<?>) list;
	}

	@SuppressWarnings("unchecked")
	private static List<SiembraLabor> castSiembras(List<?> list) {
		return (List<SiembraLabor>) (List<?>) list;
	}
}
