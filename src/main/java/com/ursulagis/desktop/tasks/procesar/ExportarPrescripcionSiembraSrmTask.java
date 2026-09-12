package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.ProyectionConstants;
import com.ursulagis.desktop.utils.tim.SrmFile;
import com.ursulagis.desktop.utils.tim.SrmGrid;
import com.ursulagis.desktop.utils.tim.SrmSection;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Exports a seeding prescription as a TIM {@code .srm} file for CS-ORO / TS-ORO
 * monitors.
 *
 * <p>
 * Unlike {@link ExportarPrescripcionSiembraTask}, which writes polygons to a
 * shapefile for ProMAP to convert, this writes the monitor's own raster format
 * directly, so the zone count limit that shapefile prescriptions face does not
 * apply.
 *
 * <p>
 * There is no choice of unit here, unlike in the shapefile export: the monitor
 * stores thousands of seeds per hectare as a plain uint16, so that is what gets
 * written (converted from {@link SiembraItem#getDosisML()} with the labor's row
 * spacing, same formula as {@link ExportarPrescripcionSiembraTask}).
 *
 * <p>
 * Georeferencing is settled: {@link SrmGrid#CELLS_PER_DEGREE} was confirmed
 * against imagery. What remains unverified is the header word that
 * {@link SrmSection#setUnknownWord(int)} controls, documented there.
 */
public class ExportarPrescripcionSiembraSrmTask extends ProgresibleTask<File> {
	private static final Logger logger = Logger.getLogger(ExportarPrescripcionSiembraSrmTask.class.getName());

	private static final int PRESCRIPTION_TILE = 8;
	private static final int PREVIEW_TILE = 32;

	/** Palette indices for the preview raster are spread over this range. */
	private static final int PREVIEW_INDEX_MIN = 3;
	private static final int PREVIEW_INDEX_MAX = 250;

	private final SiembraLabor laborToExport;
	private final File srmFile;
	public boolean guardarConfig = true;

	public ExportarPrescripcionSiembraSrmTask(SiembraLabor _laborToExport, File _srmFile) {
		laborToExport = _laborToExport;
		srmFile = _srmFile;
	}

	/** One prescribed polygon and its three product rates, already scaled for storage. */
	private static class Zona {
		final PreparedGeometry geometry;
		final Envelope envelope;
		final int seed;
		final int fertLinea;
		final int fertCostado;

		Zona(Geometry g, int seed, int fertLinea, int fertCostado) {
			this.geometry = PreparedGeometryFactory.prepare(g);
			this.envelope = g.getEnvelopeInternal();
			this.seed = seed;
			this.fertLinea = fertLinea;
			this.fertCostado = fertCostado;
		}
	}

	@Override
	public File call() {
		try {
			updateProgress(0, 100);
			List<Zona> zonas = leerZonas();
			if (zonas.isEmpty()) {
				logger.warning("la labor no tiene zonas para exportar");
				return null;
			}

			Envelope env = new Envelope();
			for (Zona z : zonas) {
				env.expandToInclude(z.envelope);
			}

			int west = (int) Math.floor(SrmGrid.column(env.getMinX()));
			int east = (int) Math.ceil(SrmGrid.column(env.getMaxX()));
			int south = (int) Math.floor(SrmGrid.row(env.getMinY()));
			int north = (int) Math.ceil(SrmGrid.row(env.getMaxY()));

			SrmSection prescripcion = SrmSection.createPrescription(west, north,
					tiles(east - west, PRESCRIPTION_TILE), tiles(north - south, PRESCRIPTION_TILE));
			rasterizar(zonas, prescripcion.getWidth(), prescripcion.getHeight(), west, north,
					(column, row, z) -> prescripcion.setProducts(column, row, z.seed, z.fertLinea, z.fertCostado),
					0, 80);

			SrmSection preview = SrmSection.createPreview(west, north,
					tiles(east - west, PREVIEW_TILE), tiles(north - south, PREVIEW_TILE));
			PreviewPalette palette = new PreviewPalette(zonas);
			rasterizar(zonas, preview.getWidth(), preview.getHeight(), west, north,
					(column, row, z) -> preview.setPreviewIndex(column, row, palette.indexFor(z.seed)), 80, 100);

			SrmFile file = new SrmFile();
			file.addSection(prescripcion);
			file.addSection(preview);
			file.write(srmFile.toPath());

			logger.fine("escribi " + srmFile + " " + prescripcion + " y " + preview);

			if (guardarConfig) {
				Configuracion config = Configuracion.getInstance();
				config.loadProperties();
				config.setProperty(Configuracion.LAST_FILE, srmFile.getAbsolutePath());
				config.save();
			}
			updateProgress(100, 100);
			return srmFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static int tiles(int cells, int tileSize) {
		return Math.max(1, (int) Math.ceil(cells / (double) tileSize));
	}

	private interface CellSink {
		void accept(int column, int row, Zona zona);
	}

	/**
	 * Assigns each distinct seed dose a preview colour index. The mapping in
	 * {@code docs/pavin.srm} is not monotonic in the dose, which is what shows
	 * these bytes to be palette indices rather than values, so any distinct
	 * non-zero index will do.
	 */
	private static class PreviewPalette {
		private final List<Integer> rates = new ArrayList<>();

		PreviewPalette(List<Zona> zonas) {
			SortedSet<Integer> distinct = new TreeSet<>();
			for (Zona z : zonas) {
				distinct.add(z.seed);
			}
			rates.addAll(distinct);
		}

		int indexFor(int rate) {
			int i = rates.indexOf(rate);
			if (rates.size() < 2) {
				return PREVIEW_INDEX_MIN;
			}
			return PREVIEW_INDEX_MIN + i * (PREVIEW_INDEX_MAX - PREVIEW_INDEX_MIN) / (rates.size() - 1);
		}
	}

	/**
	 * Paints every zone into the raster. Zones are visited in order, so where
	 * they overlap the last one wins, and cells no zone covers keep the value the
	 * section was created with.
	 */
	private void rasterizar(List<Zona> zonas, int width, int height, int west, int north, CellSink sink,
			int progressFrom, int progressTo) throws InterruptedException {
		GeometryFactory factory = ProyectionConstants.getGeometryFactory();
		int done = 0;
		for (Zona z : zonas) {
			checkCancelled();
			int firstColumn = clamp((int) Math.floor(SrmGrid.column(z.envelope.getMinX())) - west, 0, width - 1);
			int lastColumn = clamp((int) Math.ceil(SrmGrid.column(z.envelope.getMaxX())) - west, 0, width - 1);
			int firstRow = clamp(north - (int) Math.ceil(SrmGrid.row(z.envelope.getMaxY())), 0, height - 1);
			int lastRow = clamp(north - (int) Math.floor(SrmGrid.row(z.envelope.getMinY())), 0, height - 1);

			for (int row = firstRow; row <= lastRow; row++) {
				double lat = SrmGrid.lat(north - row - 0.5);
				for (int column = firstColumn; column <= lastColumn; column++) {
					double lon = SrmGrid.lon(west + column + 0.5);
					Point centre = factory.createPoint(new Coordinate(lon, lat));
					if (z.geometry.intersects(centre)) {
						sink.accept(column, row, z);
					}
				}
			}
			done++;
			updateProgress(progressFrom + (progressTo - progressFrom) * (long) done / zonas.size(), 100);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private List<Zona> leerZonas() {
		List<Zona> zonas = new ArrayList<>();
		int maxRate = 0;
		double entreSurco = laborToExport.getEntreSurco();
		SimpleFeatureIterator it = laborToExport.outCollection.features();
		try {
			while (it.hasNext()) {
				SimpleFeature next = it.next();
				SiembraItem item = laborToExport.constructFeatureContainerStandar(next, true);
				int seed = (int) Math.rint(item.getDosisML() * (10.0 / entreSurco));
				int fertL = clampRate((int) Math.rint(item.getDosisFertLinea()));
				int fertC = clampRate((int) Math.rint(item.getDosisFertCostado()));
				maxRate = Math.max(maxRate, seed);
				if (seed <= 0 || seed > SrmSection.MAX_RATE) {
					continue;
				}
				zonas.add(new Zona(item.getGeometry(), seed, fertL, fertC));
			}
		} finally {
			it.close();
		}
		if (maxRate > SrmSection.MAX_RATE) {
			avisarDosisFueraDeRango(maxRate);
			return new ArrayList<>();
		}
		return zonas;
	}

	private static int clampRate(int rate) {
		if (rate < 0) {
			return 0;
		}
		return Math.min(rate, SrmSection.MAX_RATE);
	}

	private void avisarDosisFueraDeRango(int maxRate) {
		String mensaje = String.format(
				"La dosis maxima de esta labor es %,d miles de semillas por hectarea y el formato .srm admite hasta"
						+ " %,d. Revise la configuracion de la siembra.",
				maxRate, SrmSection.MAX_RATE);
		logger.warning(mensaje);
		Platform.runLater(() -> {
			Alert a = new Alert(Alert.AlertType.ERROR);
			a.setContentText(mensaje);
			a.showAndWait();
		});
	}
}
