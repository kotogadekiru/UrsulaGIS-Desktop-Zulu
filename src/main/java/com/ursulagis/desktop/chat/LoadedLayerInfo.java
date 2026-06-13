package com.ursulagis.desktop.chat;

/**
 * Snapshot of one Ursula entity currently loaded on the map.
 */
public class LoadedLayerInfo {

	private final String name;
	private final String type;
	private final boolean active;
	private final Object entity;

	public LoadedLayerInfo(String name, String type, boolean active, Object entity) {
		this.name = name;
		this.type = type;
		this.active = active;
		this.entity = entity;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public boolean isActive() {
		return active;
	}

	public Object getEntity() {
		return entity;
	}

	public String describe() {
		return name + " (" + type + ", " + (active ? "activa" : "inactiva") + ")";
	}
}
