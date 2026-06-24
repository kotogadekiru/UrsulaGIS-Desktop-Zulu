package com.ursulagis.desktop.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class GeometryHelperTest {

	@Test
	@DisplayName("mergeNearbyPrescriptionGeometryParts reduces part count for close fragments")
	void mergeNearbyPartsJoinsClosePolygons() {
		var fact = ProyectionConstants.getGeometryFactory();
		Polygon part1 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0, 0),
				new org.locationtech.jts.geom.Coordinate(0.001, 0),
				new org.locationtech.jts.geom.Coordinate(0.001, 0.001),
				new org.locationtech.jts.geom.Coordinate(0, 0.001),
				new org.locationtech.jts.geom.Coordinate(0, 0)
		});
		// Solape leve: queda dentro del buffer de 0.25 m.
		Polygon part2 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0.0005, 0),
				new org.locationtech.jts.geom.Coordinate(0.0015, 0),
				new org.locationtech.jts.geom.Coordinate(0.0015, 0.001),
				new org.locationtech.jts.geom.Coordinate(0.0005, 0.001),
				new org.locationtech.jts.geom.Coordinate(0.0005, 0)
		});
		Geometry input = fact.createMultiPolygon(new Polygon[] { part1, part2 });

		Geometry merged = GeometryHelper.mergeNearbyPrescriptionGeometryParts(input);

		assertEquals(1, merged.getNumGeometries());
	}

	@Test
	@DisplayName("limitGeometryParts keeps the largest parts up to the maximum")
	void limitGeometryPartsDiscardsSmallestParts() {
		var fact = ProyectionConstants.getGeometryFactory();
		Polygon[] parts = new Polygon[55];
		for (int i = 0; i < parts.length; i++) {
			double offset = i * 0.01;
			double size = 0.001;
			parts[i] = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(offset, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, size),
					new org.locationtech.jts.geom.Coordinate(offset, size),
					new org.locationtech.jts.geom.Coordinate(offset, 0)
			});
		}
		Geometry input = fact.createMultiPolygon(parts);

		Geometry limited = GeometryHelper.limitGeometryParts(input, GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS);

		assertEquals(GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS, limited.getNumGeometries());
		assertEquals(parts[54].getArea(), limited.getGeometryN(0).getArea(), 1e-12);
	}

	@Test
	@DisplayName("limitPrescriptionGeometryParts merges first and only then trims")
	void limitPrescriptionGeometryPartsAppliesMergeBeforeTrim() {
		var fact = ProyectionConstants.getGeometryFactory();
		Polygon[] parts = new Polygon[55];
		for (int i = 0; i < parts.length; i++) {
			double offset = i * 0.01;
			double size = 0.001;
			parts[i] = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(offset, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, size),
					new org.locationtech.jts.geom.Coordinate(offset, size),
					new org.locationtech.jts.geom.Coordinate(offset, 0)
			});
		}

		Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(fact.createMultiPolygon(parts));

		assertEquals(GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS, limited.getNumGeometries());
	}

	@Test
	@DisplayName("limitFlatPolygons keeps at most 50 polygons")
	void limitFlatPolygonsDiscardsSmallestPolygons() {
		var fact = ProyectionConstants.getGeometryFactory();
		List<Polygon> polygons = new java.util.ArrayList<>();
		for (int i = 0; i < 60; i++) {
			double offset = i * 0.01;
			double size = 0.001;
			polygons.add(fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(offset, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, size),
					new org.locationtech.jts.geom.Coordinate(offset, size),
					new org.locationtech.jts.geom.Coordinate(offset, 0)
			}));
		}

		List<Polygon> limited = GeometryHelper.limitFlatPolygons(polygons, GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS);

		assertEquals(GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS, limited.size());
	}

	@Test
	@DisplayName("limitPrescriptionFlatPolygons merges close polygons before trimming")
	void limitPrescriptionFlatPolygonsAppliesMergeBeforeTrim() {
		var fact = ProyectionConstants.getGeometryFactory();
		List<Polygon> polygons = new java.util.ArrayList<>();
		for (int i = 0; i < 60; i++) {
			double offset = i * 0.00001;
			double size = 0.001;
			polygons.add(fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(offset, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, 0),
					new org.locationtech.jts.geom.Coordinate(offset + size, size),
					new org.locationtech.jts.geom.Coordinate(offset, size),
					new org.locationtech.jts.geom.Coordinate(offset, 0)
			}));
		}

		List<Polygon> limited = GeometryHelper.limitPrescriptionFlatPolygons(polygons);

		assertTrue(limited.size() <= GeometryHelper.MAX_PRESCRIPTION_GEOMETRY_PARTS);
	}

	@Test
	@DisplayName("limitPolygonInteriorRings keeps at most 50 holes by area")
	void limitPolygonInteriorRingsDiscardsSmallestHoles() {
		var fact = ProyectionConstants.getGeometryFactory();
		LinearRing shell = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0, 0),
				new org.locationtech.jts.geom.Coordinate(10, 0),
				new org.locationtech.jts.geom.Coordinate(10, 10),
				new org.locationtech.jts.geom.Coordinate(0, 10),
				new org.locationtech.jts.geom.Coordinate(0, 0)
		});
		LinearRing[] holes = new LinearRing[55];
		for (int i = 0; i < holes.length; i++) {
			double x = 0.2 + i * 0.17;
			double size = 0.05 + (i * 0.001);
			holes[i] = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(x, 1),
					new org.locationtech.jts.geom.Coordinate(x + size, 1),
					new org.locationtech.jts.geom.Coordinate(x + size, 1 + size),
					new org.locationtech.jts.geom.Coordinate(x, 1 + size),
					new org.locationtech.jts.geom.Coordinate(x, 1)
			});
		}
		Polygon input = fact.createPolygon(shell, holes);

		Polygon limited = GeometryHelper.limitPolygonInteriorRings(input, GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS);

		assertEquals(GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS, limited.getNumInteriorRing());
	}

	@Test
	@DisplayName("limitPrescriptionGeometryParts trims holes on single-part MultiPolygon")
	void limitPrescriptionGeometryPartsTrimsInteriorRingsOnMultiPolygon() {
		var fact = ProyectionConstants.getGeometryFactory();
		LinearRing shell = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0, 0),
				new org.locationtech.jts.geom.Coordinate(10, 0),
				new org.locationtech.jts.geom.Coordinate(10, 10),
				new org.locationtech.jts.geom.Coordinate(0, 10),
				new org.locationtech.jts.geom.Coordinate(0, 0)
		});
		LinearRing[] holes = new LinearRing[60];
		for (int i = 0; i < holes.length; i++) {
			double x = 0.2 + i * 0.15;
			holes[i] = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(x, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1)
			});
		}
		Geometry input = fact.createMultiPolygon(new Polygon[] { fact.createPolygon(shell, holes) });

		Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(input);

		assertEquals(1, limited.getNumGeometries());
		assertEquals(GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS,
				((Polygon) limited.getGeometryN(0)).getNumInteriorRing());
	}

	@Test
	@DisplayName("limitPrescriptionGeometryParts trims holes on every MultiPolygon part")
	void limitPrescriptionGeometryPartsTrimsInteriorRingsOnEveryPart() {
		var fact = ProyectionConstants.getGeometryFactory();
		Polygon[] members = new Polygon[3];
		for (int p = 0; p < members.length; p++) {
			double offset = p * 20.0;
			LinearRing shell = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(offset, 0),
					new org.locationtech.jts.geom.Coordinate(offset + 10, 0),
					new org.locationtech.jts.geom.Coordinate(offset + 10, 10),
					new org.locationtech.jts.geom.Coordinate(offset, 10),
					new org.locationtech.jts.geom.Coordinate(offset, 0)
			});
			LinearRing[] holes = new LinearRing[60];
			for (int i = 0; i < holes.length; i++) {
				double x = offset + 0.2 + i * 0.15;
				holes[i] = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
						new org.locationtech.jts.geom.Coordinate(x, 1),
						new org.locationtech.jts.geom.Coordinate(x + 0.05, 1),
						new org.locationtech.jts.geom.Coordinate(x + 0.05, 1.5),
						new org.locationtech.jts.geom.Coordinate(x, 1.5),
						new org.locationtech.jts.geom.Coordinate(x, 1)
				});
			}
			members[p] = fact.createPolygon(shell, holes);
		}
		Geometry input = fact.createMultiPolygon(members);

		Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(input);

		assertEquals(3, limited.getNumGeometries());
		for (int i = 0; i < limited.getNumGeometries(); i++) {
			assertEquals(GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS,
					((Polygon) limited.getGeometryN(i)).getNumInteriorRing());
		}
	}

	@Test
	@DisplayName("limitPrescriptionGeometryParts also trims interior rings")
	void limitPrescriptionGeometryPartsTrimsInteriorRings() {
		var fact = ProyectionConstants.getGeometryFactory();
		LinearRing shell = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0, 0),
				new org.locationtech.jts.geom.Coordinate(10, 0),
				new org.locationtech.jts.geom.Coordinate(10, 10),
				new org.locationtech.jts.geom.Coordinate(0, 10),
				new org.locationtech.jts.geom.Coordinate(0, 0)
		});
		LinearRing[] holes = new LinearRing[60];
		for (int i = 0; i < holes.length; i++) {
			double x = 0.2 + i * 0.15;
			holes[i] = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(x, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1)
			});
		}
		Geometry input = fact.createPolygon(shell, holes);

		Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(input);

		assertEquals(GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS, ((Polygon) limited).getNumInteriorRing());
	}

	@Test
	@DisplayName("limitPrescriptionFlatPolygons trims interior rings on each polygon")
	void limitPrescriptionFlatPolygonsTrimsInteriorRings() {
		var fact = ProyectionConstants.getGeometryFactory();
		LinearRing shell = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(0, 0),
				new org.locationtech.jts.geom.Coordinate(10, 0),
				new org.locationtech.jts.geom.Coordinate(10, 10),
				new org.locationtech.jts.geom.Coordinate(0, 10),
				new org.locationtech.jts.geom.Coordinate(0, 0)
		});
		LinearRing[] holes = new LinearRing[60];
		for (int i = 0; i < holes.length; i++) {
			double x = 0.2 + i * 0.15;
			holes[i] = fact.createLinearRing(new org.locationtech.jts.geom.Coordinate[] {
					new org.locationtech.jts.geom.Coordinate(x, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1),
					new org.locationtech.jts.geom.Coordinate(x + 0.05, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1.5),
					new org.locationtech.jts.geom.Coordinate(x, 1)
			});
		}
		List<Polygon> polygons = List.of(fact.createPolygon(shell, holes));

		List<Polygon> limited = GeometryHelper.limitPrescriptionFlatPolygons(polygons);

		assertEquals(1, limited.size());
		assertEquals(GeometryHelper.MAX_PRESCRIPTION_INTERIOR_RINGS, limited.get(0).getNumInteriorRing());
	}
}
