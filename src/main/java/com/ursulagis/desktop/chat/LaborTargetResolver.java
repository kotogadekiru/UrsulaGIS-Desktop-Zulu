package com.ursulagis.desktop.chat;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;

/**
 * Picks which loaded labor layer a chat intent refers to: by explicit name,
 * selected/active layer, or type keywords (cosecha / siembra / fertilización).
 * Also builds Spanish messages when the choice is ambiguous.
 */
public final class LaborTargetResolver {

	private static final Set<String> GENERIC_TARGETS = Set.of(
			"cosecha", "la cosecha", "cosecha activa", "la cosecha activa",
			"siembra", "la siembra", "siembra activa",
			"fertilizacion", "fertilización", "la fertilizacion", "la fertilización",
			"labor", "la labor", "labor activa", "capa", "la capa", "capa activa",
			"activa", "activo", "active layer", "capa activa del mapa");

	/** Prevents instantiation. */
	private LaborTargetResolver() {
	}

	/**
	 * Treats generic phrases like “capa activa” as no name so resolution falls
	 * back to selected/active layers; returns a trimmed concrete name otherwise.
	 */
	public static String sanitizeTargetName(String targetName) {
		if (targetName == null || targetName.isBlank()) {
			return null;
		}
		String normalized = AchievementIntentCatalog.normalize(targetName);
		if (GENERIC_TARGETS.contains(normalized)) {
			return null;
		}
		if (normalized.equals("cosecha activa del mapa") || normalized.endsWith(" activa")) {
			String withoutActiva = normalized.replaceAll("\\s+activa$", "").trim();
			if (GENERIC_TARGETS.contains(withoutActiva) || withoutActiva.equals("cosecha")
					|| withoutActiva.equals("siembra") || withoutActiva.equals("fertilizacion")) {
				return null;
			}
		}
		return targetName.trim();
	}

	/** Whether the text mentions harvest / cosecha. */
	public static boolean mentionsCosecha(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String n = AchievementIntentCatalog.normalize(text);
		return n.contains("cosecha") || n.contains("harvest");
	}

	/** Whether the text mentions seeding / siembra. */
	public static boolean mentionsSiembra(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String n = AchievementIntentCatalog.normalize(text);
		return n.contains("siembra") || n.contains("seeding");
	}

	/** Whether the text mentions fertilization. */
	public static boolean mentionsFertilizacion(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String n = AchievementIntentCatalog.normalize(text);
		return n.contains("fertiliz") || n.contains("fert ") || n.endsWith(" fert");
	}

	/**
	 * Resolves a single active (or only) seeding layer when unambiguous.
	 */
	public static Optional<SiembraLabor> resolveActiveSiembra(MapLayerContext mapCtx) {
		return resolveSingleOfType(mapCtx, SiembraLabor.class)
				.filter(SiembraLabor.class::isInstance)
				.map(SiembraLabor.class::cast);
	}

	/**
	 * Resolves a labor for the intent: named match → selected → single active →
	 * type keywords → sole loaded labor of the requested kind.
	 *
	 * @param mapCtx       current layers
	 * @param targetName   optional name or generic phrase from the intent
	 * @param cosechaOnly  when {@code true}, only {@link CosechaLabor} is accepted
	 */
	public static Optional<Labor<?>> resolve(MapLayerContext mapCtx, String targetName, boolean cosechaOnly) {
		String effectiveTarget = sanitizeTargetName(targetName);
		boolean preferCosecha = cosechaOnly || mentionsCosecha(targetName);

		if (effectiveTarget != null && !effectiveTarget.isBlank()) {
			Optional<LoadedLayerInfo> byName = mapCtx.findByName(effectiveTarget, true);
			if (byName.isPresent()) {
				Optional<Labor<?>> labor = toLabor(byName.get(), preferCosecha);
				if (labor.isPresent()) {
					return labor;
				}
			}
		}

		Optional<LoadedLayerInfo> selected = mapCtx.getSelectedLayer();
		if (selected.isPresent()) {
			Optional<Labor<?>> fromSelected = toLabor(selected.get(), preferCosecha);
			if (fromSelected.isPresent()) {
				return fromSelected;
			}
		}

		Optional<Labor<?>> fromActive = resolveSingleActive(mapCtx, preferCosecha);
		if (fromActive.isPresent()) {
			return fromActive;
		}

		if (!preferCosecha && mentionsCosecha(targetName)) {
			return resolveSingleActive(mapCtx, true);
		}
		if (mentionsSiembra(targetName)) {
			return resolveSingleOfType(mapCtx, SiembraLabor.class);
		}
		if (mentionsFertilizacion(targetName)) {
			return resolveSingleOfType(mapCtx, FertilizacionLabor.class);
		}

		List<LoadedLayerInfo> loaded = mapCtx.getLabors(preferCosecha);
		if (loaded.size() == 1) {
			return toLabor(loaded.get(0), preferCosecha);
		}
		return Optional.empty();
	}

	/** Sole active labor of the requested kind, when exactly one is enabled. */
	private static Optional<Labor<?>> resolveSingleActive(MapLayerContext mapCtx, boolean cosechaOnly) {
		Optional<LoadedLayerInfo> singleActive = mapCtx.getSingleActiveLabor(cosechaOnly);
		if (singleActive.isPresent()) {
			return toLabor(singleActive.get(), cosechaOnly);
		}
		return Optional.empty();
	}

	/**
	 * Sole labor of {@code type}: prefers the single active match, else the sole loaded one.
	 */
	private static Optional<Labor<?>> resolveSingleOfType(MapLayerContext mapCtx, Class<?> type) {
		List<LoadedLayerInfo> matches = mapCtx.getLabors(false).stream()
				.filter(info -> type.isInstance(info.getEntity()))
				.toList();
		List<LoadedLayerInfo> active = matches.stream().filter(LoadedLayerInfo::isActive).toList();
		if (active.size() == 1) {
			return Optional.of((Labor<?>) active.get(0).getEntity());
		}
		if (matches.size() == 1) {
			return Optional.of((Labor<?>) matches.get(0).getEntity());
		}
		return Optional.empty();
	}

	/** Casts the layer entity to {@link Labor}, optionally requiring {@link CosechaLabor}. */
	private static Optional<Labor<?>> toLabor(LoadedLayerInfo info, boolean cosechaOnly) {
		Object entity = info.getEntity();
		if (!(entity instanceof Labor<?> labor)) {
			return Optional.empty();
		}
		if (cosechaOnly && !(entity instanceof CosechaLabor)) {
			return Optional.empty();
		}
		return Optional.of(labor);
	}

	/**
	 * Spanish clarification when more than one labor could be the target
	 * (or none are loaded/active).
	 */
	public static String ambiguousLaborMessage(MapLayerContext mapCtx, String targetName, boolean cosechaOnly) {
		boolean preferCosecha = cosechaOnly || mentionsCosecha(targetName);
		List<LoadedLayerInfo> active = mapCtx.getLabors(preferCosecha).stream()
				.filter(LoadedLayerInfo::isActive)
				.toList();
		if (active.size() == 1) {
			return "Encontré una sola capa activa: **" + active.get(0).getName()
					+ "**. Activá solo esa capa o indicá el nombre exacto.";
		}
		List<LoadedLayerInfo> loaded = mapCtx.getLabors(preferCosecha);
		if (loaded.isEmpty()) {
			return preferCosecha
					? "No hay capas de cosecha cargadas en el mapa."
					: "No hay capas de labor cargadas en el mapa.";
		}
		String options = loaded.stream().map(LoadedLayerInfo::describe)
				.collect(java.util.stream.Collectors.joining(", "));
		if (active.isEmpty()) {
			return "No hay capas activas. Activá una en el árbol de capas o indicá el nombre: " + options;
		}
		return "Hay varias capas cargadas. Activá solo una o indicá el nombre: " + options;
	}

	/** Spanish clarification when sharing/importing siembra needs a unique seeding layer. */
	public static String ambiguousSiembraMessage(MapLayerContext mapCtx) {
		List<LoadedLayerInfo> siembras = mapCtx.getLabors(false).stream()
				.filter(info -> info.getEntity() instanceof SiembraLabor)
				.toList();
		if (siembras.isEmpty()) {
			return "No hay siembras cargadas. Importá un SHP desde el nodo **Siembras** → **Importar**.";
		}
		String options = siembras.stream().map(LoadedLayerInfo::describe)
				.collect(java.util.stream.Collectors.joining(", "));
		return "Activá una siembra en el árbol de capas o indicá el nombre: " + options;
	}
}
