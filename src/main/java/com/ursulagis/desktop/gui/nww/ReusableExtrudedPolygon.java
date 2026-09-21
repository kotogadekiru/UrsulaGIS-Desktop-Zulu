package com.ursulagis.desktop.gui.nww;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.ExtrudedPolygon;

/**
 * ExtrudedPolygon that can be pooled across viewport rebuilds.
 * Call {@link #clearBoundarys()} before filling a new outline, then
 * {@link #onBoundariesFilled()} after outer (and any inner) rings are set —
 * otherwise WorldWind keeps stale {@code totalFaceCount}/vertex buffers and
 * throws {@code newLimit > capacity} when the next feature has more vertices.
 */
public class ReusableExtrudedPolygon extends ExtrudedPolygon {

	/**
	 * Drop previous outer/inner rings so reuse does not keep empty hole lists
	 * or stale boundary references from the last feature.
	 */
	public void clearBoundarys() {
		if (boundaries == null) {
			return;
		}
		boundaries.clear();
		boundaries.add(new ArrayList<LatLon>()); // placeholder for outer boundary
	}

	@SuppressWarnings("unchecked")
	public List<? extends LatLon> getBoundary() {
		if (boundaries.isEmpty()) {
			this.boundaries.add(new ArrayList<LatLon>());
		}
		return boundaries.get(0);
	}

	/**
	 * Recompute face counts and discard cached ShapeData/vertex buffers.
	 * Must run after the outer boundary list has been filled (and after any
	 * {@link #addInnerBoundary} calls).
	 */
	public void onBoundariesFilled() {
		reset();
	}
}
