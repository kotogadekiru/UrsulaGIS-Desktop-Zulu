package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.tasks.ProcessMapTask;

/**
 * Full redraw after edit/delete. Clears the spatial cache and runs
 * {@link #runLater} (new extruded shell for all items + SurfaceImage).
 * Extruded is installed before SurfaceImage so polygons can rebuild while the
 * raster catches up.
 */
public class RedrawMapTask extends ProcessMapTask<LaborItem, Labor<LaborItem>> {

	public RedrawMapTask(Labor<LaborItem> cosechaLabor) {
		super(cosechaLabor);
		labor.clearCache();
	}

	@Override
	protected void doProcess() throws IOException {
		runLater(this.getItemsList());
	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}

	public static void redraw(Labor<LaborItem> l) {
		if (l == null) {
			return;
		}
		JFXMain.executorPool.execute(new RedrawMapTask(l));
	}
}
