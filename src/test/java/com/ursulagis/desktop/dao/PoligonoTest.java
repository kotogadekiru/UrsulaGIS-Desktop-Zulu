package com.ursulagis.desktop.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

class PoligonoTest {

	@Test
	@DisplayName("setGeometry keeps every part available for NDVI download")
	void setGeometryMultipolygonRemainsComplete() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part1 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});
		Polygon part2 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2)
		});

		Poligono poligono = new Poligono();
		poligono.setGeometry(fact.createMultiPolygon(new Polygon[] { part1, part2 }));

		assertEquals(2, poligono.getGeometry().getNumGeometries());
		assertTrue(poligono.getText().contains("MULTIPOLYGON"));
	}

	@Test
	@DisplayName("getPoligonoStringForSharing unites exterior rings for multipolygon contours")
	void sharingStringIncludesAllMultipolygonParts() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part1 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});
		Polygon part2 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2)
		});

		Poligono poligono = new Poligono();
		poligono.setGeometry(fact.createMultiPolygon(new Polygon[] { part1, part2 }));

		String sharing = poligono.getPoligonoStringForSharing();

		assertTrue(sharing.startsWith("{{"), "merged contour must use legacy positionsString format");
		assertFalse(sharing.contains("MULTIPOLYGON"), "must not send WKT to server");
		assertTrue(sharing.contains("-33.00000000,-60.00000000"), "first polygon part must be included");
		assertTrue(sharing.contains("-33.20000000,-60.20000000") || sharing.contains("-33.30000000,-60.30000000"),
				"second polygon part must be included after merging rings");
	}

	@Test
	@DisplayName("getPoligonoStringForSharing keeps legacy format for single-part contours")
	void sharingStringUsesLegacyFormatForSinglePolygon() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});

		Poligono poligono = new Poligono();
		poligono.setGeometry(part);

		String sharing = poligono.getPoligonoStringForSharing();

		assertTrue(sharing.startsWith("{{"), "single polygon must keep legacy positionsString format");
		assertTrue(sharing.contains("-33.00000000,-60.00000000"));
	}

	@Test
	@DisplayName("getPoligonoToString includes every part of a multipolygon")
	void multipolygonIncludesAllParts() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part1 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});
		Polygon part2 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2)
		});

		Poligono poligono = new Poligono();
		poligono.setGeometry(fact.createMultiPolygon(new Polygon[] { part1, part2 }));

		String polygons = poligono.getPoligonoToString();

		assertTrue(polygons.contains("-60.0,-33.0"), "first polygon part must be sent to NDVI API");
		assertTrue(polygons.contains("-60.2,-33.2"), "second polygon part must be sent to NDVI API");
		assertTrue(polygons.indexOf("-60.2,-33.2") > polygons.indexOf("-60.0,-33.0"),
				"both polygon parts must appear in the same payload");
	}

	@Test
	@DisplayName("explotarPoligono creates one polygon per disconnected part")
	void explotarPoligonoCreatesOnePolygonPerPart() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part1 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});
		Polygon part2 = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.2),
				new org.locationtech.jts.geom.Coordinate(-60.3, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.3),
				new org.locationtech.jts.geom.Coordinate(-60.2, -33.2)
		});

		Poligono poligono = new Poligono();
		poligono.setNombre("Lote A");
		poligono.setGeometry(fact.createMultiPolygon(new Polygon[] { part1, part2 }));

		List<Poligono> partes = GeometryHelper.explotarPoligono(poligono);

		assertEquals(2, partes.size());
		assertEquals("Lote A (1)", partes.get(0).getNombre());
		assertEquals("Lote A (2)", partes.get(1).getNombre());
		assertEquals(1, partes.get(0).getGeometry().getNumGeometries());
		assertEquals(1, partes.get(1).getGeometry().getNumGeometries());
	}

	@Test
	@DisplayName("explotarPoligono returns empty list for single-part polygons")
	void explotarPoligonoSkipsSinglePart() {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon part = fact.createPolygon(new org.locationtech.jts.geom.Coordinate[] {
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.0),
				new org.locationtech.jts.geom.Coordinate(-60.1, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.1),
				new org.locationtech.jts.geom.Coordinate(-60.0, -33.0)
		});

		Poligono poligono = new Poligono();
		poligono.setGeometry(part);

		assertTrue(GeometryHelper.explotarPoligono(poligono).isEmpty());
	}
}
