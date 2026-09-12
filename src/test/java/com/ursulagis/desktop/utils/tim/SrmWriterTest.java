package com.ursulagis.desktop.utils.tim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Writer tests; these need no sample file. */
class SrmWriterTest {

	@TempDir
	Path tmp;

	private static SrmFile buildTwoSectionFile() {
		SrmSection presc = SrmSection.createPrescription(4184850, 2024584, 4, 3);
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				presc.setRate(column, row, 8 * (33 + (column + row) % 4));
			}
		}
		SrmSection preview = SrmSection.createPreview(4184850, 2024584, 1, 1);
		preview.setPreviewIndex(5, 7, 126);

		SrmFile file = new SrmFile();
		file.addSection(presc);
		file.addSection(preview);
		return file;
	}

	@Test
	@DisplayName("a freshly created prescription is empty everywhere")
	void newPrescriptionStartsOutsideTheField() {
		SrmSection presc = SrmSection.createPrescription(4184850, 2024584, 2, 2);
		assertEquals(16, presc.getWidth());
		assertEquals(16, presc.getHeight());
		assertEquals(4184850 + 16, presc.getEastColumn());
		assertEquals(2024584 - 16, presc.getSouthRow());
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				assertEquals(SrmSection.NODATA_RATE, presc.getRate(column, row));
				assertTrue(!presc.isPrescribed(column, row));
			}
		}
	}

	@Test
	@DisplayName("rates survive a write and read cycle")
	void ratesSurviveWriteAndRead() throws IOException {
		Path out = tmp.resolve("out.srm");
		SrmFile written = buildTwoSectionFile();
		written.write(out);

		SrmFile reread = SrmFile.read(out);
		assertEquals(2, reread.getSections().size());
		SrmSection presc = reread.getPrescription();
		assertEquals(32, presc.getWidth());
		assertEquals(24, presc.getHeight());
		for (int row = 0; row < presc.getHeight(); row++) {
			for (int column = 0; column < presc.getWidth(); column++) {
				assertEquals(8 * (33 + (column + row) % 4), presc.getRate(column, row));
				assertTrue(presc.isPrescribed(column, row));
			}
		}
		assertEquals(126, reread.getSections().get(1).getPreviewIndex(5, 7));
		assertEquals(0, reread.getSections().get(1).getPreviewIndex(0, 0));
	}

	@Test
	@DisplayName("a written file re-encodes byte for byte")
	void writtenFileReEncodesExactly() throws IOException {
		Path out = tmp.resolve("out.srm");
		buildTwoSectionFile().write(out);
		byte[] raw = Files.readAllBytes(out);
		assertArrayEquals(raw, SrmFile.read(raw).toByteArray());
	}

	@Test
	@DisplayName("the section chain records the offset of the next section")
	void sectionChainIsLinked() throws IOException {
		byte[] raw = buildTwoSectionFile().toByteArray();
		SrmFile reread = SrmFile.read(raw);
		SrmSection first = reread.getSections().get(0);
		assertEquals(first.byteLength(), SrmSection.u32(raw, 0x30));
		assertEquals(0, SrmSection.u32(raw, first.byteLength() + 0x30));
	}

	@Test
	@DisplayName("a rate that will not fit in the uint16 field is rejected")
	void oversizedRateIsRejected() {
		SrmSection presc = SrmSection.createPrescription(0, 100, 1, 1);
		assertThrows(IllegalArgumentException.class, () -> presc.setRate(0, 0, SrmSection.NODATA_RATE));
		assertThrows(IllegalArgumentException.class, () -> presc.setRate(0, 0, 70000));
	}

	@Test
	@DisplayName("a dose in thousands of seeds per hectare is stored as-is")
	void thousandsOfSeedsPerHaRoundTrip() {
		SrmSection presc = SrmSection.createPrescription(0, 100, 1, 1);
		for (int dose : new int[] { 264, 288, 320, 336, 368, 424 }) {
			presc.setRate(0, 0, dose);
			assertEquals(dose, presc.getRate(0, 0));
		}
	}

	@Test
	@DisplayName("cell indices and degrees convert back and forth")
	void gridConversionsAreInverses() {
		int column = 4184850;
		int row = 2024584;
		assertEquals(column, Math.round(SrmGrid.column(SrmGrid.lon(column))));
		assertEquals(row, Math.round(SrmGrid.row(SrmGrid.lat(row))));
	}
}
