package com.ursulagis.desktop.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;

/**
 * Point-in-time view of Ursula layers loaded on the map and their visibility state.
 */
public class MapLayerContext {

	private final List<LoadedLayerInfo> layers;
	private final String selectedLayerName;

	public MapLayerContext(List<LoadedLayerInfo> layers, String selectedLayerName) {
		this.layers = layers != null ? List.copyOf(layers) : List.of();
		this.selectedLayerName = selectedLayerName;
	}

	public List<LoadedLayerInfo> getLayers() {
		return layers;
	}

	public String getSelectedLayerName() {
		return selectedLayerName;
	}

	public List<LoadedLayerInfo> getActiveLayers() {
		return layers.stream().filter(LoadedLayerInfo::isActive).collect(Collectors.toList());
	}

	public List<LoadedLayerInfo> getLabors(boolean cosechaOnly) {
		List<LoadedLayerInfo> result = new ArrayList<>();
		for (LoadedLayerInfo info : layers) {
			Object entity = info.getEntity();
			if (!(entity instanceof Labor<?>)) {
				continue;
			}
			if (cosechaOnly && !(entity instanceof CosechaLabor)) {
				continue;
			}
			result.add(info);
		}
		return result;
	}

	public List<LoadedLayerInfo> getRecorridas() {
		return layers.stream()
				.filter(info -> info.getEntity() instanceof Recorrida)
				.collect(Collectors.toList());
	}

	public Optional<LoadedLayerInfo> findByName(String targetName, boolean preferActive) {
		if (targetName == null || targetName.isBlank()) {
			return Optional.empty();
		}
		String needle = targetName.toLowerCase(Locale.ROOT);

		Optional<LoadedLayerInfo> exact = layers.stream()
				.filter(info -> info.getName() != null && info.getName().equalsIgnoreCase(targetName))
				.findFirst();
		if (exact.isPresent()) {
			return exact;
		}

		List<LoadedLayerInfo> partial = layers.stream()
				.filter(info -> info.getName() != null
						&& info.getName().toLowerCase(Locale.ROOT).contains(needle))
				.collect(Collectors.toList());
		if (partial.size() == 1) {
			return Optional.of(partial.get(0));
		}
		if (preferActive && partial.size() > 1) {
			List<LoadedLayerInfo> activePartial = partial.stream()
					.filter(LoadedLayerInfo::isActive)
					.collect(Collectors.toList());
			if (activePartial.size() == 1) {
				return Optional.of(activePartial.get(0));
			}
		}
		return Optional.empty();
	}

	public Optional<LoadedLayerInfo> getSelectedLayer() {
		if (selectedLayerName == null || selectedLayerName.isBlank()) {
			return Optional.empty();
		}
		return findByName(selectedLayerName, false);
	}

	public Optional<LoadedLayerInfo> getSingleActiveLabor(boolean cosechaOnly) {
		List<LoadedLayerInfo> active = getLabors(cosechaOnly).stream()
				.filter(LoadedLayerInfo::isActive)
				.collect(Collectors.toList());
		if (active.size() == 1) {
			return Optional.of(active.get(0));
		}
		return Optional.empty();
	}

	public Optional<LoadedLayerInfo> getSingleActiveRecorrida() {
		List<LoadedLayerInfo> active = getRecorridas().stream()
				.filter(LoadedLayerInfo::isActive)
				.collect(Collectors.toList());
		if (active.size() == 1) {
			return Optional.of(active.get(0));
		}
		return Optional.empty();
	}

	public String toPromptSection() {
		if (layers.isEmpty()) {
			return "Loaded map layers: none.";
		}
		StringBuilder sb = new StringBuilder("Loaded map layers:\n");
		for (LoadedLayerInfo info : layers) {
			sb.append("- ").append(info.describe());
			if (info.getName() != null && info.getName().equals(selectedLayerName)) {
				sb.append(" [selected in tree]");
			}
			sb.append('\n');
		}
		List<LoadedLayerInfo> active = getActiveLayers();
		if (!active.isEmpty()) {
			sb.append("Active layers: ")
					.append(active.stream().map(LoadedLayerInfo::getName).collect(Collectors.joining(", ")))
					.append('\n');
		}
		if (selectedLayerName != null && !selectedLayerName.isBlank()) {
			sb.append("Selected in layer tree: ").append(selectedLayerName).append('\n');
		}
		sb.append("When the user refers to \"active layer\" or \"capa activa\", use an active layer name as targetName.");
		sb.append(" Prefer active layers over inactive ones when resolving targets.");
		return sb.toString();
	}

	public String formatLayerList() {
		if (layers.isEmpty()) {
			return "No hay capas cargadas en el mapa.";
		}
		return layers.stream()
				.map(LoadedLayerInfo::describe)
				.collect(Collectors.joining("\n• ", "Capas cargadas:\n• ", ""));
	}

	public static MapLayerContext empty() {
		return new MapLayerContext(Collections.emptyList(), null);
	}
}
