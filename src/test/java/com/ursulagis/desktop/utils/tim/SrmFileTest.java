package com.ursulagis.desktop.utils.tim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reader tests against a real TIM prescription. {@code docs/pavin.srm} is not
 * committed, so every test here is skipped when it is missing.
 */
class SrmFileTest {

	private static final Path SAMPLE = Path.of("docs/pavin.srm");

	@BeforeAll
	static void requireSample() {
		assumeTrue(Files.exists(SAMPLE), "place a .srm at " + SAMPLE + " to run these tests");
	}

	@Test
	@DisplayName("pavin.srm parses into a prescription plus a preview raster")
	void parsesBothSections() throws IOException {
		SrmFile file = SrmFile.read(SAMPLE);
		List<SrmSection> sections = file.getSections();
		assertEquals(2, sections.size());

		SrmSection presc = sections.get(0);
		assertEquals(472, presc.getWidth());
		assertEquals(536, presc.getHeight());
		assertEquals(8, presc.getTileSize());
		assertEquals(59, presc.getTilesX());
		assertEquals(67, presc.getTilesY());
		assertEquals(SrmSection.PRESCRIPTION_CELL_BYTES, presc.getBytesPerCell());
		assertTrue(presc.isPrescription());
		assertEquals(presc, file.getPrescription());

		SrmSection preview = sections.get(1);
		assertEquals(384, preview.getWidth());
		assertEquals(416, preview.getHeight());
		assertEquals(32, preview.getTileSize());
		assertEquals(SrmSection.PREVIEW_CELL_BYTES, preview.getBytesPerCell());
	}

	@Test
	@DisplayName("bounding box width and height agree with the cell dimensions")
	void boundingBoxMatchesDimensions() throws IOException {
		for (SrmSection s : SrmFile.read(SAMPLE).getSections()) {
			assertEquals(s.getWidth(), s.getEastColumn() - s.getWestColumn());
			assertEquals(s.getHeight(), s.getNorthRow() - s.getSouthRow());
		}
	}

	@Test
	@DisplayName("the prescription lands on its field in southwest Cordoba")
	void georeferencesOntoTheKnownField() throws IOException {
		SrmSection presc = SrmFile.read(SAMPLE).getPrescription();
		assertEquals(-63.754167, presc.getWestLon(), 1e-6);
		assertEquals(-63.741056, presc.getEastLon(), 1e-6);
		assertEquals(-33.776444, presc.getSouthLat(), 1e-6);
		assertEquals(-33.761556, presc.getNorthLat(), 1e-6);
	}

	@Test
	@DisplayName("the preview sits inside the prescription on the shared global grid")
	void previewIsContainedInPrescription() throws IOException {
		List<SrmSection> sections = SrmFile.read(SAMPLE).getSections();
		SrmSection presc = sections.get(0);
		SrmSection preview = sections.get(1);
		assertTrue(preview.getWestColumn() >= presc.getWestColumn());
		assertTrue(preview.getEastColumn() <= presc.getEastColumn());
		assertTrue(preview.getSouthRow() >= presc.getSouthRow());
		assertTrue(preview.getNorthRow() <= presc.getNorthRow());
	}

	@Test
	@DisplayName("the tiling formula visits every body byte exactly once")
	void tilingCoversTheBodyExactlyOnce() throws IOException {
		for (SrmSection s : SrmFile.read(SAMPLE).getSections()) {
			boolean[] seen = new boolean[s.getBody().length];
			for (int row = 0; row < s.getHeight(); row++) {
				for (int column = 0; column < s.getWidth(); column++) {
					int off = s.cellOffset(column, row);
					for (int i = 0; i < s.getBytesPerCell(); i++) {
						assertTrue(!seen[off + i], "byte " + (off + i) + " reached twice");
						seen[off + i] = true;
					}
				}
			}
			for (int i = 0; i < seen.length; i++) {
				assertTrue(seen[i], "byte " + i + " never reached");
			}
		}
	}

	@Test
	@DisplayName("six rate zones as thousands of seeds per hectare")
	void ratesAreThousandsOfSeedsPerHa() throws IOException {
		SrmSection presc = SrmFile.read(SAMPLE).getPrescription();
		assertNotNull(presc);
		Map<Integer, Integer> counts = new TreeMap<>();
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				counts.merge(presc.getRate(column, row), 1, Integer::sum);
			}
		}
		assertTrue(counts.containsKey(SrmSection.NODATA_RATE), "expected cells outside the field");
		counts.remove(SrmSection.NODATA_RATE);

		assertEquals(6, counts.size(), "rate zones found: " + counts);
		assertEquals(List.of(264, 288, 320, 336, 368, 424), List.copyOf(counts.keySet()));
		assertEquals(424, presc.getRate(378, 340));
	}

	@Test
	@DisplayName("the trailing product slots are unused in every cell")
	void trailingProductSlotsAreConstant() throws IOException {
		SrmSection presc = SrmFile.read(SAMPLE).getPrescription();
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				assertEquals(0x0000, presc.getProductRate(column, row, 2));
				assertEquals(0x0101, presc.getProductRate(column, row, 3));
			}
		}
	}

	@Test
	@DisplayName("re-encoding the parsed model reproduces the file byte for byte")
	void roundTripIsByteExact() throws IOException {
		byte[] original = Files.readAllBytes(SAMPLE);
		assertArrayEquals(original, SrmFile.read(original).toByteArray());
	}
}
