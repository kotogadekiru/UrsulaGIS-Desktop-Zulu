package com.ursulagis.desktop.chat;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.gui.JFXMain;

/**
 * Mutable runtime bag passed while executing a parsed intent: main window,
 * optional layer name from the user, current map layers, and resolved entities
 * (labor / harvest / scouting route) when the action needs them.
 */
public class ActionContext {

	private final JFXMain main;
	private final String targetName;
	private final MapLayerContext layerContext;
	private Labor<?> labor;
	private CosechaLabor cosecha;
	private Recorrida recorrida;

	/**
	 * @param main         application main window used to open controllers
	 * @param targetName   layer name hinted by the intent, or {@code null}
	 * @param layerContext snapshot of loaded map layers (empty if {@code null})
	 */
	public ActionContext(JFXMain main, String targetName, MapLayerContext layerContext) {
		this.main = main;
		this.targetName = targetName;
		this.layerContext = layerContext != null ? layerContext : MapLayerContext.empty();
	}

	/** Host app used to call GUI controllers and map helpers. */
	public JFXMain getMain() {
		return main;
	}

	/** Optional layer name from the parsed intent (may be a generic phrase). */
	public String getTargetName() {
		return targetName;
	}

	/** Layers visible on the map when the user sent the message. */
	public MapLayerContext getLayerContext() {
		return layerContext;
	}

	/** Labor resolved for actions that need a map labor layer. */
	public Labor<?> getLabor() {
		return labor;
	}

	/** Sets the labor entity used by labor-targeting actions. */
	public void setLabor(Labor<?> labor) {
		this.labor = labor;
	}

	/** Harvest specifically required by share-harvest and similar actions. */
	public CosechaLabor getCosecha() {
		return cosecha;
	}

	/** Sets the harvest entity when the action needs a {@link CosechaLabor}. */
	public void setCosecha(CosechaLabor cosecha) {
		this.cosecha = cosecha;
	}

	/** Scouting route resolved for sync/export recorrida actions. */
	public Recorrida getRecorrida() {
		return recorrida;
	}

	/** Sets the scouting route for recorrida sync/export actions. */
	public void setRecorrida(Recorrida recorrida) {
		this.recorrida = recorrida;
	}
}
