package com.ursulagis.desktop.utils.tim;

/**
 * Global cell grid that TIM {@code .srm} files use to place their rasters.
 *
 * <p>
 * Sections do not store coordinates. They store integer cell indices on a
 * single equirectangular grid whose origin is 180W / 90S, so
 * {@link #CELLS_PER_DEGREE} is the only constant needed to move between cell
 * indices and degrees.
 */
public final class SrmGrid {

	/**
	 * Cells per degree of latitude and of longitude, which is one tenth of an
	 * arcsecond per cell.
	 *
	 * <p>
	 * Confirmed against satellite imagery: {@code docs/pavin.srm} lands on a real
	 * field in southwest Cordoba, Argentina, with its outer boundary following the
	 * field edges and its unprescribed holes falling on the patches of scrub
	 * inside it. The bounding boxes alone are self consistent for any value
	 * between roughly 32700 and 37000, so the file could not settle this on its
	 * own; 32768, 35000 and 35500 were each ruled out by importing the sample and
	 * finding it hundreds of kilometres from the field.
	 *
	 * <p>
	 * If another {@code .srm} ever disagrees, both axes of a known field give the
	 * constant independently and have to agree:
	 *
	 * <pre>
	 * CELLS_PER_DEGREE = westColumn / (westLon + 180)
	 * CELLS_PER_DEGREE = northRow / (northLat + 90)
	 * </pre>
	 */
	public static final int CELLS_PER_DEGREE = 36000;

	private SrmGrid() {
	}

	public static double lon(double column) {
		return column / CELLS_PER_DEGREE - 180.0;
	}

	public static double lat(double row) {
		return row / CELLS_PER_DEGREE - 90.0;
	}

	public static double column(double lon) {
		return (lon + 180.0) * CELLS_PER_DEGREE;
	}

	public static double row(double lat) {
		return (lat + 90.0) * CELLS_PER_DEGREE;
	}
}
