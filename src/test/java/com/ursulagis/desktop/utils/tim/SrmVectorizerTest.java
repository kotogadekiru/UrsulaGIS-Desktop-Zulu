package com.ursulagis.desktop.utils.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.siembra.SiembraLabor;

/**
 * Vectorizer tests against a real TIM prescription. {@code docs/pavin.srm} is not
 * committed, so every test here is skipped when it is missing.
 */
class SrmVectorizerTest {

	private static final Path SAMPLE = Path.of("docs/pavin.srm");

	@TempDir
	Path tmp;

	@BeforeAll
	static void requireSample() {
		assumeTrue(Files.exists(SAMPLE), "place a .srm at " + SAMPLE + " to run these tests");
	}

	private static SrmSection prescription() throws IOException {
		return SrmFile.read(SAMPLE).getPrescription();
	}

	private static int prescribedCells(SrmSection presc) {
		int cells = 0;
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				if (presc.isPrescribed(column, row)) {
					cells++;
				}
			}
		}
		return cells;
	}

	@Test
	@DisplayName("each prescribed cell becomes one polygon, covering the field exactly")
	void onePolygonPerPrescribedCell() throws IOException {
		SrmSection presc = prescription();
		int cells = prescribedCells(presc);
		assertEquals(156720, cells, "prescribed cells in pavin.srm");

		DefaultFeatureCollection collection = SrmVectorizer.vectorize(presc);
		assertEquals(cells, collection.size());

		double cellArea = 1.0 / SrmGrid.CELLS_PER_DEGREE / SrmGrid.CELLS_PER_DEGREE;
		double area = 0;
		try (SimpleFeatureIterator it = collection.features()) {
			while (it.hasNext()) {
				area += ((Geometry) it.next().getDefaultGeometry()).getArea();
			}
		}
		assertEquals(cells * cellArea, area, cells * cellArea * 1e-9);
	}

	@Test
	@DisplayName("every rate zone comes back as thousands of seeds per hectare")
	void dosesAreThousandsOfSeedsPerHa() throws IOException {
		DefaultFeatureCollection collection = SrmVectorizer.vectorize(prescription());
		SortedSet<Double> doses = new TreeSet<>();
		try (SimpleFeatureIterator it = collection.features()) {
			while (it.hasNext()) {
				doses.add((Double) it.next().getAttribute(SrmVectorizer.COLUMNA_DOSIS));
			}
		}
		assertEquals(List.of(264.0, 288.0, 320.0, 336.0, 368.0, 424.0), List.copyOf(doses));
	}

	@Test
	@DisplayName("fert columns come from the other product slots")
	void fertColumnsComeFromOtherSlots() throws IOException {
		DefaultFeatureCollection collection = SrmVectorizer.vectorize(prescription(), 0);
		try (SimpleFeatureIterator it = collection.features()) {
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				// pavin.srm: slot1=1, slot2=0 on every prescribed cell
				assertEquals(1.0, (Double) f.getAttribute(SiembraLabor.COLUMNA_DOSIS_LINEA), 1e-9);
				assertEquals(0.0, (Double) f.getAttribute(SiembraLabor.COLUMNA_DOSIS_COSTADO), 1e-9);
			}
		}

		DefaultFeatureCollection seedOnSlot1 = SrmVectorizer.vectorize(prescription(), 1);
		SortedSet<Double> seedDoses = new TreeSet<>();
		try (SimpleFeatureIterator it = seedOnSlot1.features()) {
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				seedDoses.add((Double) f.getAttribute(SrmVectorizer.COLUMNA_DOSIS));
				assertEquals(0.0, (Double) f.getAttribute(SiembraLabor.COLUMNA_DOSIS_COSTADO), 1e-9);
			}
		}
		// slot1 is constantly 1 in pavin, so treating it as seed collapses to one zone value
		assertEquals(List.of(1.0), List.copyOf(seedDoses));
	}

	@Test
	@DisplayName("fertSlotsFor leaves seed out and keeps ascending order")
	void fertSlotsForOrdering() {
		assertEquals(List.of(1, 2), Arrays.stream(SrmVectorizer.fertSlotsFor(0)).boxed().toList());
		assertEquals(List.of(0, 2), Arrays.stream(SrmVectorizer.fertSlotsFor(1)).boxed().toList());
		assertEquals(List.of(0, 1), Arrays.stream(SrmVectorizer.fertSlotsFor(2)).boxed().toList());
	}

	@Test
	@DisplayName("the polygons stay inside the section bounding box")
	void polygonsStayInsideTheBoundingBox() throws IOException {
		SrmSection presc = prescription();
		DefaultFeatureCollection collection = SrmVectorizer.vectorize(presc);
		var bounds = collection.getBounds();
		assertTrue(bounds.getMinX() >= presc.getWestLon(), "west edge");
		assertTrue(bounds.getMaxX() <= presc.getEastLon(), "east edge");
		assertTrue(bounds.getMinY() >= presc.getSouthLat(), "south edge");
		assertTrue(bounds.getMaxY() <= presc.getNorthLat(), "north edge");
	}

	@Test
	@DisplayName("the shapefile it writes reads back with every polygon")
	void writtenShapefileReadsBack() throws IOException {
		DefaultFeatureCollection collection = SrmVectorizer.vectorize(prescription());
		File shapeFile = tmp.resolve("pavin.shp").toFile();
		SrmVectorizer.writeShapefile(collection, shapeFile);
		assertTrue(shapeFile.exists());

		FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
		try {
			assertEquals(collection.size(), store.getFeatureSource().getFeatures().size());
		} finally {
			store.dispose();
		}
	}
}
