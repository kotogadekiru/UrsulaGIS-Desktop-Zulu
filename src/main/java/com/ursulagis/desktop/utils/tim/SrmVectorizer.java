package com.ursulagis.desktop.utils.tim;

import java.io.File;
import java.io.IOException;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * Turns the raster of a {@link SrmSection} into polygons, so a {@code .srm} can
 * be loaded through the shapefile import path.
 *
 * <p>
 * Each prescribed cell becomes one polygon. The first three product slots are
 * exposed as seed / fert línea / fert costado; the operator chooses which slot
 * is seed and the other two are assigned to
 * {@link SiembraLabor#COLUMNA_DOSIS_LINEA} and
 * {@link SiembraLabor#COLUMNA_DOSIS_COSTADO} in that order.
 */
public final class SrmVectorizer {

	/** Seed dose: thousands of seeds per hectare (same integer the file stores). */
	public static final String COLUMNA_DOSIS = SiembraLabor.COLUMNA_MILES_SEM_HA;

	/** Product slots that can be mapped to seed / fert (slot 3 is a constant flag). */
	public static final int MAPPABLE_SLOTS = 3;

	private static final String TYPE_NAME = "SrmType";

	private SrmVectorizer() {
	}

	public static SimpleFeatureType featureType() {
		try {
			return DataUtilities.createType(TYPE_NAME,
					"*the_geom:" + Polygon.class.getCanonicalName() + ":4326,"
							+ COLUMNA_DOSIS + ":java.lang.Double,"
							+ SiembraLabor.COLUMNA_DOSIS_LINEA + ":java.lang.Double,"
							+ SiembraLabor.COLUMNA_DOSIS_COSTADO + ":java.lang.Double");
		} catch (SchemaException e) {
			throw new IllegalStateException("no pude crear el type de " + TYPE_NAME, e);
		}
	}

	/** Same as {@link #vectorize(SrmSection, int)} with product slot 0 as seed. */
	public static DefaultFeatureCollection vectorize(SrmSection presc) {
		return vectorize(presc, 0);
	}

	/**
	 * @param seedSlot which of the first three product slots is seed (0, 1 or 2);
	 *                 the remaining two become fert línea then fert costado
	 */
	public static DefaultFeatureCollection vectorize(SrmSection presc, int seedSlot) {
		if (seedSlot < 0 || seedSlot >= MAPPABLE_SLOTS) {
			throw new IllegalArgumentException("seedSlot must be 0.." + (MAPPABLE_SLOTS - 1));
		}
		int[] fertSlots = fertSlotsFor(seedSlot);

		SimpleFeatureType type = featureType();
		DefaultFeatureCollection collection = new DefaultFeatureCollection(TYPE_NAME, type);
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
		GeometryFactory factory = ProyectionConstants.getGeometryFactory();

		int width = presc.getWidth();
		int height = presc.getHeight();
		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				int seed = presc.getProductRate(column, row, seedSlot);
				if (seed == SrmSection.NODATA_RATE) {
					continue;
				}
				double west = SrmGrid.lon(presc.getWestColumn() + column);
				double east = SrmGrid.lon(presc.getWestColumn() + column + 1);
				double north = SrmGrid.lat(presc.getNorthRow() - row);
				double south = SrmGrid.lat(presc.getNorthRow() - row - 1);
				Polygon polygon = factory.createPolygon(new Coordinate[] {
						new Coordinate(west, north),
						new Coordinate(east, north),
						new Coordinate(east, south),
						new Coordinate(west, south),
						new Coordinate(west, north) });
				collection.add(fb.buildFeature(null, new Object[] {
						polygon,
						asDose(seed),
						asDose(presc.getProductRate(column, row, fertSlots[0])),
						asDose(presc.getProductRate(column, row, fertSlots[1])) }));
			}
		}
		return collection;
	}

	/** Remaining mappable slots after {@code seedSlot}, in ascending order → Fert L, Fert C. */
	public static int[] fertSlotsFor(int seedSlot) {
		int[] fert = new int[MAPPABLE_SLOTS - 1];
		int i = 0;
		for (int slot = 0; slot < MAPPABLE_SLOTS; slot++) {
			if (slot != seedSlot) {
				fert[i++] = slot;
			}
		}
		return fert;
	}

	private static double asDose(int rate) {
		return rate == SrmSection.NODATA_RATE ? 0.0 : (double) rate;
	}

	public static void writeShapefile(DefaultFeatureCollection collection, File shapeFile) throws IOException {
		ShapefileDataStore store = FileHelper.createShapefileDataStore(shapeFile, collection.getSchema());
		if (store == null) {
			throw new IOException("no pude crear el shapefile " + shapeFile);
		}
		SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		if (!(featureSource instanceof SimpleFeatureStore)) {
			throw new IOException("el shapefile " + shapeFile + " no es escribible");
		}
		SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
		Transaction transaction = new DefaultTransaction("create");
		featureStore.setTransaction(transaction);
		try {
			featureStore.setFeatures(collection.reader());
			transaction.commit();
		} catch (Exception e) {
			transaction.rollback();
			throw new IOException("no pude escribir " + shapeFile, e);
		} finally {
			transaction.close();
			store.dispose();
		}
	}
}
