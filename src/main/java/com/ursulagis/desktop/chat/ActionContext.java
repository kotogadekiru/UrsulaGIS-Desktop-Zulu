package com.ursulagis.desktop.chat;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.gui.JFXMain;

/**
 * Runtime context for executing a parsed intent.
 */
public class ActionContext {

	private final JFXMain main;
	private final String targetName;
	private final MapLayerContext layerContext;
	private Labor<?> labor;
	private CosechaLabor cosecha;
	private Recorrida recorrida;

	public ActionContext(JFXMain main, String targetName, MapLayerContext layerContext) {
		this.main = main;
		this.targetName = targetName;
		this.layerContext = layerContext != null ? layerContext : MapLayerContext.empty();
	}

	public JFXMain getMain() {
		return main;
	}

	public String getTargetName() {
		return targetName;
	}

	public MapLayerContext getLayerContext() {
		return layerContext;
	}

	public Labor<?> getLabor() {
		return labor;
	}

	public void setLabor(Labor<?> labor) {
		this.labor = labor;
	}

	public CosechaLabor getCosecha() {
		return cosecha;
	}

	public void setCosecha(CosechaLabor cosecha) {
		this.cosecha = cosecha;
	}

	public Recorrida getRecorrida() {
		return recorrida;
	}

	public void setRecorrida(Recorrida recorrida) {
		this.recorrida = recorrida;
	}
}
