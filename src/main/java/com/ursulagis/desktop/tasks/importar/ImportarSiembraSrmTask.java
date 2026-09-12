package com.ursulagis.desktop.tasks.importar;

import java.io.File;
import java.util.logging.Logger;

import org.geotools.feature.DefaultFeatureCollection;

import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.tim.SrmFile;
import com.ursulagis.desktop.utils.tim.SrmGrid;
import com.ursulagis.desktop.utils.tim.SrmSection;
import com.ursulagis.desktop.utils.tim.SrmVectorizer;

/**
 * Reads a TIM {@code .srm} prescription and writes it out as a shapefile next to
 * it, so the regular siembra import path can turn it into a layer.
 *
 * <p>
 * {@code seedSlot} selects which of the first three product rates is seed; the
 * other two become fert línea and fert costado.
 */
public class ImportarSiembraSrmTask extends ProgresibleTask<File> {
	private static final Logger logger = Logger.getLogger(ImportarSiembraSrmTask.class.getName());

	private final File srmFile;
	private final int seedSlot;

	public ImportarSiembraSrmTask(File _srmFile, int _seedSlot) {
		srmFile = _srmFile;
		seedSlot = _seedSlot;
	}

	@Override
	public File call() {
		try {
			updateProgress(0, 100);
			SrmSection presc = SrmFile.read(srmFile.toPath()).getPrescription();
			if (presc == null) {
				logger.warning(srmFile + " no tiene una seccion de prescripcion");
				return null;
			}
			logger.info(String.format(
					"%s: %dx%d celdas, lon %.6f..%.6f lat %.6f..%.6f, semilla=slot %d",
					srmFile.getName(), presc.getWidth(), presc.getHeight(),
					presc.getWestLon(), presc.getEastLon(), presc.getSouthLat(), presc.getNorthLat(),
					seedSlot));

			DefaultFeatureCollection collection = SrmVectorizer.vectorize(presc, seedSlot);
			if (collection.isEmpty()) {
				logger.warning(srmFile + " no tiene celdas prescriptas");
				return null;
			}
			logger.fine("la prescripcion quedo en " + collection.size() + " poligonos");
			updateProgress(50, 100);

			File shapeFile = shapeFileFor(srmFile);
			SrmVectorizer.writeShapefile(collection, shapeFile);
			updateProgress(100, 100);
			return shapeFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static File shapeFileFor(File srmFile) {
		String name = srmFile.getName();
		int dot = name.lastIndexOf('.');
		return new File(srmFile.getParentFile(), (dot > 0 ? name.substring(0, dot) : name) + ".shp");
	}
}
