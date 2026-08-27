package com.ursulagis.desktop.chat;

/**
 * Snapshot of one Ursula entity currently loaded on the WorldWind map:
 * display name, Java type name, whether the layer checkbox is on, and the DAO entity.
 * Built by {@link MapLayerContextBuilder} and queried by intent resolution.
 */
public class LoadedLayerInfo {

	private final String name;
	private final String type;
	private final boolean active;
	private final Object entity;

	/**
	 * @param name   user-visible layer name
	 * @param type   simple class name of the entity (e.g. {@code CosechaLabor})
	 * @param active {@code true} when the WorldWind layer is enabled
	 * @param entity underlying DAO object attached to the layer
	 */
	public LoadedLayerInfo(String name, String type, boolean active, Object entity) {
		this.name = name;
		this.type = type;
		this.active = active;
		this.entity = entity;
	}

	/** Display name shown in the layer tree and chat replies. */
	public String getName() {
		return name;
	}

	/** Simple type label used when listing layers to the user or the LLM. */
	public String getType() {
		return type;
	}

	/** Whether the layer is currently visible/enabled on the map. */
	public boolean isActive() {
		return active;
	}

	/** DAO entity (labor, polygon, NDVI, recorrida, …) behind this layer. */
	public Object getEntity() {
		return entity;
	}

	/** Compact Spanish description for chat lists, e.g. {@code Lote A (CosechaLabor, activa)}. */
	public String describe() {
		return name + " (" + type + ", " + (active ? "activa" : "inactiva") + ")";
	}
}
