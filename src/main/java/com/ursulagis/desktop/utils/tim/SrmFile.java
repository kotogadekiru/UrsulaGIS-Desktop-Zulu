package com.ursulagis.desktop.utils.tim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A TIM {@code .srm} prescription file: a singly linked chain of
 * {@link SrmSection}s, with no magic number, no string table and no
 * compression.
 *
 * <p>
 * {@code docs/pavin.srm} holds two sections, a full resolution prescription
 * followed by a coarser preview raster of the same zones.
 */
public class SrmFile {

	private final List<SrmSection> sections = new ArrayList<>();

	public static SrmFile read(Path path) throws IOException {
		return read(Files.readAllBytes(path));
	}

	public static SrmFile read(byte[] raw) {
		SrmFile file = new SrmFile();
		int offset = 0;
		while (true) {
			if (offset + SrmSection.HEADER_BYTES > raw.length) {
				throw new IllegalArgumentException("srm truncated: no header at " + offset);
			}
			long next = SrmSection.u32(raw, offset + 0x30);
			if (next != 0 && (next <= offset || next > raw.length)) {
				throw new IllegalArgumentException("srm section at " + offset + " links to " + next);
			}
			int end = next == 0 ? raw.length : (int) next;
			file.sections.add(SrmSection.parse(raw, offset, end));
			if (next == 0) {
				return file;
			}
			offset = (int) next;
		}
	}

	public byte[] toByteArray() {
		int total = 0;
		for (SrmSection s : sections) {
			total += s.byteLength();
		}
		byte[] out = new byte[total];
		int offset = 0;
		for (int i = 0; i < sections.size(); i++) {
			SrmSection s = sections.get(i);
			int next = offset + s.byteLength();
			s.writeTo(out, offset, i == sections.size() - 1 ? 0 : next);
			offset = next;
		}
		return out;
	}

	public void write(Path path) throws IOException {
		Files.write(path, toByteArray());
	}

	public List<SrmSection> getSections() {
		return Collections.unmodifiableList(sections);
	}

	public void addSection(SrmSection section) {
		sections.add(section);
	}

	/** The full resolution prescription, or null when the file has none. */
	public SrmSection getPrescription() {
		for (SrmSection s : sections) {
			if (s.isPrescription()) {
				return s;
			}
		}
		return null;
	}
}
