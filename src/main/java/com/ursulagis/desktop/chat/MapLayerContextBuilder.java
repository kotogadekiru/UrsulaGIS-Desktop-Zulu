package com.ursulagis.desktop.chat;

import java.util.ArrayList;
import java.util.List;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.nww.LayerPanel;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * Builds a {@link MapLayerContext} from the current WorldWind layer stack.
 */
public final class MapLayerContextBuilder {

	private MapLayerContextBuilder() {
	}

	public static MapLayerContext from(JFXMain main) {
		List<LoadedLayerInfo> layers = new ArrayList<>();
		if (main.getWwd() != null) {
			LayerList layerList = main.getWwd().getModel().getLayers();
			for (Layer layer : layerList) {
				Object entity = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (entity == null || isWorldWindBaseLayer(layer.getName())) {
					continue;
				}
				layers.add(new LoadedLayerInfo(
						resolveName(entity, layer),
						entity.getClass().getSimpleName(),
						layer.isEnabled(),
						entity));
			}
		}
		String selectedName = resolveSelectedLayerName(main);
		return new MapLayerContext(layers, selectedName);
	}

	private static String resolveSelectedLayerName(JFXMain main) {
		LayerPanel panel = main.getLayerPanel();
		if (panel == null) {
			return null;
		}
		Layer selected = panel.getSelectedLayer();
		if (selected == null) {
			return null;
		}
		Object entity = selected.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		if (entity != null) {
			return resolveName(entity, selected);
		}
		return selected.getName();
	}

	private static String resolveName(Object entity, Layer layer) {
		if (entity instanceof Labor<?> labor && labor.getNombre() != null && !labor.getNombre().isBlank()) {
			return labor.getNombre();
		}
		if (entity instanceof Recorrida recorrida && recorrida.getNombre() != null && !recorrida.getNombre().isBlank()) {
			return recorrida.getNombre();
		}
		if (entity instanceof Ndvi ndvi && ndvi.getNombre() != null && !ndvi.getNombre().isBlank()) {
			return ndvi.getNombre();
		}
		if (entity instanceof Poligono poligono && poligono.getNombre() != null && !poligono.getNombre().isBlank()) {
			return poligono.getNombre();
		}
		String layerName = layer.getName();
		return layerName != null ? layerName : "sin nombre";
	}

	private static boolean isWorldWindBaseLayer(String name) {
		if (name == null) {
			return true;
		}
		String lower = name.toLowerCase();
		return lower.equals("stars")
				|| lower.equals("atmosphere")
				|| lower.contains("blue marble")
				|| lower.contains("landsat")
				|| lower.contains("bing imagery")
				|| lower.equals("place names")
				|| lower.equals("world map")
				|| lower.equals("scale bar")
				|| lower.equals("view controls")
				|| lower.equals("annotations")
				|| lower.equals("compass")
				|| lower.equals("capas");
	}
}
