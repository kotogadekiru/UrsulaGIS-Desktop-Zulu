package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.tasks.ProcessMapTask;

/**
 * Full redraw after edit/delete. One LaborItem can map to many ExtrudedPolygons
 * (e.g. MultiPolygon parts), and the spatial cache / SurfaceImage still reference
 * the old geometry — so we {@link Labor#clearCache()} and run a full
 * {@link #runLater} (new extruded shell + SurfaceImage).
 * <p>
 * {@code runLater} installs the extruded layer before rasterizing SurfaceImage so
 * {@code rebuildForVisibleSector} can drop every polygon for the deleted item
 * without waiting on the slow raster.
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
