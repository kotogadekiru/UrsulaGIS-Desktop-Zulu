package com.ursulagis.desktop.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

/**
 * One-off analysis for monitor geometry limits. Run with:
 * mvn -q test -Dtest=MonitorGeometryAnalysisTest#analyzeProblemGeometry
 */
class MonitorGeometryAnalysisTest {

	@Test
	void analyzeProblemGeometry() throws Exception {
		Path wktPath = Path.of("src/test/resources/monitor-problem.wkt");
		if (!Files.exists(wktPath)) {
			System.out.println("Skip: create " + wktPath + " with the problem WKT");
			return;
		}
		String wkt = Files.readString(wktPath);
		Geometry geom = new WKTReader(ProyectionConstants.getGeometryFactory()).read(wkt);

		int parts = geom.getNumGeometries();
		int totalInterior = 0;
		int maxInterior = 0;
		int polygonsWithHoles = 0;
		for (int i = 0; i < parts; i++) {
			if (geom.getGeometryN(i) instanceof Polygon p) {
				int holes = p.getNumInteriorRing();
				totalInterior += holes;
				maxInterior = Math.max(maxInterior, holes);
				if (holes > 0) {
					polygonsWithHoles++;
				}
			}
		}
		int totalRings = parts + totalInterior;

		System.out.println("=== Raw geometry ===");
		System.out.println("MultiPolygon parts (getNumGeometries): " + parts);
		System.out.println("Total interior rings: " + totalInterior);
		System.out.println("Max interior rings in one polygon: " + maxInterior);
		System.out.println("Polygons with holes: " + polygonsWithHoles);
		System.out.println("Total rings (parts + interior): " + totalRings);

		Geometry afterParts = GeometryHelper.limitPrescriptionGeometryParts(geom);
		printStats("After limitPrescriptionGeometryParts", afterParts);
		org.junit.jupiter.api.Assertions.assertTrue(
				((Polygon) afterParts.getGeometryN(0)).getNumInteriorRing()
						<= GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS);

		var flat = PolygonValidator.geometryToFlatPolygons(afterParts);
		var limitedFlat = GeometryHelper.limitPrescriptionFlatPolygons(flat);
		System.out.println("=== Flat export path ===");
		System.out.println("Flat polygons before limit: " + flat.size());
		System.out.println("Flat polygons after limit: " + limitedFlat.size());
		for (int i = 0; i < limitedFlat.size(); i++) {
			Polygon p = limitedFlat.get(i);
			System.out.println("  export polygon " + i + ": interiorRings=" + p.getNumInteriorRing()
					+ " area=" + p.getArea());
		}

		assertTrue(parts > 0);
	}

	private static void printStats(String label, Geometry geom) {
		int parts = geom.getNumGeometries();
		int totalInterior = 0;
		int maxInterior = 0;
		for (int i = 0; i < parts; i++) {
			if (geom.getGeometryN(i) instanceof Polygon p) {
				int holes = p.getNumInteriorRing();
				totalInterior += holes;
				maxInterior = Math.max(maxInterior, holes);
			}
		}
		System.out.println("=== " + label + " ===");
		System.out.println("  parts=" + parts + " totalInterior=" + totalInterior
				+ " maxInterior=" + maxInterior + " totalRings=" + (parts + totalInterior));
	}
}
