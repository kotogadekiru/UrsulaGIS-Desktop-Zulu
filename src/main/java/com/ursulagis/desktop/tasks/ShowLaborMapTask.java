package com.ursulagis.desktop.tasks;

import java.io.IOException;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.nww.LaborLayer;

/**
 * Renders a labor whose {@link Labor#outCollection} is already populated
 * (e.g. restored from the content blob on load).
 */
public class ShowLaborMapTask extends ProcessMapTask<LaborItem, Labor<LaborItem>> {

	@SuppressWarnings("unchecked")
	public ShowLaborMapTask(Labor<?> labor) {
		super((Labor<LaborItem>) labor);
		if (this.labor.getLayer() == null) {
			this.labor.setLayer(new LaborLayer());
		}
	}

	@Override
	protected void doProcess() throws IOException {
		if (labor.outCollection == null || labor.outCollection.isEmpty()) {
			return;
		}
		labor.constructClasificador();
		runLater(getItemsList());
	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}
}
