package com.ursulagis.desktop.utils.tim;

import java.util.Arrays;

/**
 * One section of a TIM {@code .srm} file: a 512 byte header followed by a raster
 * body. All integers are little endian.
 *
 * <p>
 * Header layout, as reverse engineered from {@code docs/pavin.srm}:
 *
 * <pre>
 * 0x00  uint16  unknown; not any standard checksum of the section
 * 0x02  uint24  column of the west edge, on the {@link SrmGrid} global grid
 * 0x05  uint24  row of the north edge
 * 0x08  uint8   flag, 1 in every known section
 * 0x09  uint16  tile size, in cells per side
 * 0x0B  uint16  tiles across
 * 0x0D  uint16  tiles down
 * 0x0F  uint24  column of the east edge
 * 0x12  uint24  row of the south edge
 * 0x15  uint16  width in cells, equal to eastColumn - westColumn
 * 0x17  uint16  height in cells, equal to northRow - southRow
 * 0x20  8 bytes unknown, non zero only in the prescription section
 * 0x30  uint32  byte offset of the next section, 0 in the last one
 * 0x34  uint32  0xFFFFFFFF sentinel
 * </pre>
 *
 * <p>
 * The body is <b>tiled, not row major</b>: tiles run in row major order and the
 * cells inside each tile run in row major order too. Reading the body as
 * scanlines yields noise. See {@link #cellOffset(int, int)}.
 */
public class SrmSection {

	public static final int HEADER_BYTES = 512;

	/** Cell rate meaning "outside the prescribed area". */
	public static final int NODATA_RATE = 0xFFFF;

	/** A prescription cell holds four little endian uint16 product slots. */
	public static final int PRESCRIPTION_CELL_BYTES = 8;

	/** A preview cell holds a single palette index; 0 is outside the field. */
	public static final int PREVIEW_CELL_BYTES = 1;

	/** Product slots per prescription cell. */
	public static final int PRODUCT_SLOTS = 4;

	/**
	 * Highest dose that fits in the uint16 rate field. Values in
	 * {@code docs/pavin.srm} (264..424) line up with thousands of seeds per
	 * hectare for soybean; the stored integer is used as-is, with no scaling.
	 */
	public static final int MAX_RATE = NODATA_RATE - 1;

	private static final int OFF_UNKNOWN_WORD = 0x00;
	private static final int OFF_WEST_COLUMN = 0x02;
	private static final int OFF_NORTH_ROW = 0x05;
	private static final int OFF_FLAG = 0x08;
	private static final int OFF_TILE_SIZE = 0x09;
	private static final int OFF_TILES_X = 0x0B;
	private static final int OFF_TILES_Y = 0x0D;
	private static final int OFF_EAST_COLUMN = 0x0F;
	private static final int OFF_SOUTH_ROW = 0x12;
	private static final int OFF_WIDTH = 0x15;
	private static final int OFF_HEIGHT = 0x17;
	private static final int OFF_EXTRA = 0x20;
	private static final int EXTRA_BYTES = 8;
	private static final int OFF_NEXT = 0x30;
	private static final int OFF_SENTINEL = 0x34;
	private static final long SENTINEL = 0xFFFFFFFFL;

	private int unknownWord;
	private int westColumn;
	private int northRow;
	private int eastColumn;
	private int southRow;
	private int flag;
	private int tileSize;
	private int tilesX;
	private int tilesY;
	private int width;
	private int height;
	private byte[] extra = new byte[EXTRA_BYTES];
	private int bytesPerCell;
	private byte[] body;

	/**
	 * Cell contents outside the prescribed area. Product slots 1 and 2 are
	 * 0xFFFF; slots 3 and 4 keep the same constant they carry everywhere.
	 */
	private static final byte[] CELL_OUTSIDE = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0, 0, 1, 1 };

	/**
	 * Bytes 0x20..0x27 of the prescription section in {@code docs/pavin.srm}.
	 * Their meaning is unknown, so files we write reuse the only value a monitor
	 * is known to have accepted. See {@link #setExtra(byte[])}.
	 */
	private static final byte[] OBSERVED_EXTRA = { 0x1a, 0x2d, 0x64, 0x00, 0x02, 0x01, 0x01, 0x00 };

	/**
	 * An empty prescription section anchored at the given global cell, sized to
	 * whole 8 cell tiles, with every cell outside the prescribed area.
	 */
	public static SrmSection createPrescription(int westColumn, int northRow, int tilesX, int tilesY) {
		SrmSection s = blank(westColumn, northRow, tilesX, tilesY, 8, PRESCRIPTION_CELL_BYTES);
		for (int off = 0; off < s.body.length; off += PRESCRIPTION_CELL_BYTES) {
			System.arraycopy(CELL_OUTSIDE, 0, s.body, off, PRESCRIPTION_CELL_BYTES);
		}
		s.extra = OBSERVED_EXTRA.clone();
		return s;
	}

	/**
	 * An empty preview section anchored at the given global cell, sized to whole
	 * 32 cell tiles. A zero cell is outside the field.
	 */
	public static SrmSection createPreview(int westColumn, int northRow, int tilesX, int tilesY) {
		return blank(westColumn, northRow, tilesX, tilesY, 32, PREVIEW_CELL_BYTES);
	}

	private static SrmSection blank(int westColumn, int northRow, int tilesX, int tilesY, int tileSize,
			int bytesPerCell) {
		if (tilesX <= 0 || tilesY <= 0) {
			throw new IllegalArgumentException("a section needs at least one tile");
		}
		SrmSection s = new SrmSection();
		s.tileSize = tileSize;
		s.tilesX = tilesX;
		s.tilesY = tilesY;
		s.width = tilesX * tileSize;
		s.height = tilesY * tileSize;
		s.westColumn = westColumn;
		s.northRow = northRow;
		s.eastColumn = westColumn + s.width;
		s.southRow = northRow - s.height;
		s.flag = 1;
		s.bytesPerCell = bytesPerCell;
		s.body = new byte[s.width * s.height * bytesPerCell];
		return s;
	}

	/**
	 * Parses the section starting at {@code offset}, whose body ends at
	 * {@code end} (exclusive).
	 */
	public static SrmSection parse(byte[] raw, int offset, int end) {
		SrmSection s = new SrmSection();
		s.unknownWord = u16(raw, offset + OFF_UNKNOWN_WORD);
		s.westColumn = u24(raw, offset + OFF_WEST_COLUMN);
		s.northRow = u24(raw, offset + OFF_NORTH_ROW);
		s.flag = raw[offset + OFF_FLAG] & 0xFF;
		s.tileSize = u16(raw, offset + OFF_TILE_SIZE);
		s.tilesX = u16(raw, offset + OFF_TILES_X);
		s.tilesY = u16(raw, offset + OFF_TILES_Y);
		s.eastColumn = u24(raw, offset + OFF_EAST_COLUMN);
		s.southRow = u24(raw, offset + OFF_SOUTH_ROW);
		s.width = u16(raw, offset + OFF_WIDTH);
		s.height = u16(raw, offset + OFF_HEIGHT);
		s.extra = Arrays.copyOfRange(raw, offset + OFF_EXTRA, offset + OFF_EXTRA + EXTRA_BYTES);

		if (s.tileSize <= 0 || s.tilesX <= 0 || s.tilesY <= 0) {
			throw new IllegalArgumentException("srm section at " + offset + " has no tiles");
		}
		if (s.tilesX * s.tileSize != s.width || s.tilesY * s.tileSize != s.height) {
			throw new IllegalArgumentException(String.format(
					"srm section at %d: %dx%d tiles of %d do not match the declared %dx%d cells",
					offset, s.tilesX, s.tilesY, s.tileSize, s.width, s.height));
		}
		if (s.eastColumn - s.westColumn != s.width || s.northRow - s.southRow != s.height) {
			throw new IllegalArgumentException(String.format(
					"srm section at %d: bounding box %dx%d does not match the declared %dx%d cells",
					offset, s.eastColumn - s.westColumn, s.northRow - s.southRow, s.width, s.height));
		}

		int bodyStart = offset + HEADER_BYTES;
		int bodyLength = end - bodyStart;
		int cells = s.width * s.height;
		if (bodyLength <= 0 || bodyLength % cells != 0) {
			throw new IllegalArgumentException(String.format(
					"srm section at %d: %d body bytes do not divide into %d cells", offset, bodyLength, cells));
		}
		s.bytesPerCell = bodyLength / cells;
		s.body = Arrays.copyOfRange(raw, bodyStart, end);
		return s;
	}

	/**
	 * Byte offset of a cell inside {@link #getBody()}. Tiles run in row major
	 * order, then cells within the tile.
	 */
	public int cellOffset(int column, int row) {
		if (column < 0 || column >= width || row < 0 || row >= height) {
			throw new IndexOutOfBoundsException("cell " + column + "," + row + " outside " + width + "x" + height);
		}
		int tileRow = row / tileSize;
		int tileColumn = column / tileSize;
		int index = (tileRow * tilesX + tileColumn) * tileSize * tileSize
				+ (row % tileSize) * tileSize + (column % tileSize);
		return index * bytesPerCell;
	}

	public boolean isPrescription() {
		return bytesPerCell == PRESCRIPTION_CELL_BYTES;
	}

	/** Raw rate of one product slot (thousands of seeds per hectare for slot 0). */
	public int getProductRate(int column, int row, int slot) {
		if (slot < 0 || slot >= PRODUCT_SLOTS) {
			throw new IllegalArgumentException("product slot " + slot);
		}
		if (!isPrescription()) {
			throw new IllegalStateException("section holds " + bytesPerCell + " bytes per cell, not a prescription");
		}
		return u16(body, cellOffset(column, row) + slot * 2);
	}

	/**
	 * Seed rate as stored: thousands of seeds per hectare. {@link #NODATA_RATE}
	 * means outside the prescribed area.
	 */
	public int getRate(int column, int row) {
		return getProductRate(column, row, 0);
	}

	public boolean isPrescribed(int column, int row) {
		return isPrescription()
				? getRate(column, row) != NODATA_RATE
				: getPreviewIndex(column, row) != 0;
	}

	/** Palette index of a preview cell; 0 is outside the field. */
	public int getPreviewIndex(int column, int row) {
		if (bytesPerCell != PREVIEW_CELL_BYTES) {
			throw new IllegalStateException("section holds " + bytesPerCell + " bytes per cell, not a preview");
		}
		return body[cellOffset(column, row)] & 0xFF;
	}

	/**
	 * Marks a cell as prescribed with seed only; fert slots are left at 0 and the
	 * trailing flag at {@code 0x0101}, as in files without fertilizer.
	 */
	public void setRate(int column, int row, int rate) {
		setProducts(column, row, rate, 0, 0);
	}

	/**
	 * Marks a cell as prescribed. Rates are thousands of seeds (or fertilizer
	 * units) per hectare, written as-is into product slots 0..2. Slot 3 keeps the
	 * constant {@code 0x0101} observed in {@code docs/pavin.srm}.
	 */
	public void setProducts(int column, int row, int seed, int fertLinea, int fertCostado) {
		if (!isPrescription()) {
			throw new IllegalStateException("section holds " + bytesPerCell + " bytes per cell, not a prescription");
		}
		requireRate(seed);
		requireRate(fertLinea);
		requireRate(fertCostado);
		int off = cellOffset(column, row);
		putU16(body, off, seed);
		putU16(body, off + 2, fertLinea);
		putU16(body, off + 4, fertCostado);
		putU16(body, off + 6, 0x0101);
	}

	private static void requireRate(int rate) {
		if (rate < 0 || rate > MAX_RATE) {
			throw new IllegalArgumentException("rate " + rate + " does not fit in 0.." + MAX_RATE);
		}
	}

	public void setPreviewIndex(int column, int row, int index) {
		if (bytesPerCell != PREVIEW_CELL_BYTES) {
			throw new IllegalStateException("section holds " + bytesPerCell + " bytes per cell, not a preview");
		}
		body[cellOffset(column, row)] = (byte) index;
	}

	public byte[] getCell(int column, int row) {
		int off = cellOffset(column, row);
		return Arrays.copyOfRange(body, off, off + bytesPerCell);
	}

	public void setCell(int column, int row, byte[] cell) {
		if (cell.length != bytesPerCell) {
			throw new IllegalArgumentException("cell must be " + bytesPerCell + " bytes, got " + cell.length);
		}
		System.arraycopy(cell, 0, body, cellOffset(column, row), bytesPerCell);
	}

	public double getWestLon() {
		return SrmGrid.lon(westColumn);
	}

	public double getEastLon() {
		return SrmGrid.lon(eastColumn);
	}

	public double getNorthLat() {
		return SrmGrid.lat(northRow);
	}

	public double getSouthLat() {
		return SrmGrid.lat(southRow);
	}

	/** Header plus body, as this section occupies the file. */
	public int byteLength() {
		return HEADER_BYTES + body.length;
	}

	/**
	 * Writes this section at {@code offset}, recording {@code nextOffset} as the
	 * link to the following section (0 when it is the last one). Every byte the
	 * format is not known to use is written as zero, so a section that survives
	 * a read and write round trip carries no undiscovered fields.
	 */
	public void writeTo(byte[] out, int offset, int nextOffset) {
		putU16(out, offset + OFF_UNKNOWN_WORD, unknownWord);
		putU24(out, offset + OFF_WEST_COLUMN, westColumn);
		putU24(out, offset + OFF_NORTH_ROW, northRow);
		out[offset + OFF_FLAG] = (byte) flag;
		putU16(out, offset + OFF_TILE_SIZE, tileSize);
		putU16(out, offset + OFF_TILES_X, tilesX);
		putU16(out, offset + OFF_TILES_Y, tilesY);
		putU24(out, offset + OFF_EAST_COLUMN, eastColumn);
		putU24(out, offset + OFF_SOUTH_ROW, southRow);
		putU16(out, offset + OFF_WIDTH, width);
		putU16(out, offset + OFF_HEIGHT, height);
		System.arraycopy(extra, 0, out, offset + OFF_EXTRA, EXTRA_BYTES);
		putU32(out, offset + OFF_NEXT, nextOffset);
		putU32(out, offset + OFF_SENTINEL, SENTINEL);
		System.arraycopy(body, 0, out, offset + HEADER_BYTES, body.length);
	}

	static int u16(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
	}

	static int u24(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16);
	}

	static long u32(byte[] b, int off) {
		return u16(b, off) | ((long) u16(b, off + 2) << 16);
	}

	static void putU16(byte[] b, int off, int value) {
		b[off] = (byte) value;
		b[off + 1] = (byte) (value >>> 8);
	}

	static void putU24(byte[] b, int off, int value) {
		b[off] = (byte) value;
		b[off + 1] = (byte) (value >>> 8);
		b[off + 2] = (byte) (value >>> 16);
	}

	static void putU32(byte[] b, int off, long value) {
		b[off] = (byte) value;
		b[off + 1] = (byte) (value >>> 8);
		b[off + 2] = (byte) (value >>> 16);
		b[off + 3] = (byte) (value >>> 24);
	}

	public int getUnknownWord() {
		return unknownWord;
	}

	/**
	 * Sets the 16 bit word at offset 0x00, whose meaning is still unknown: it
	 * matches no standard checksum of the section, so files we write leave it at
	 * 0 unless a monitor turns out to validate it.
	 */
	public void setUnknownWord(int unknownWord) {
		this.unknownWord = unknownWord;
	}

	/** Sets bytes 0x20..0x27, whose meaning is still unknown. */
	public void setExtra(byte[] extra) {
		if (extra.length != EXTRA_BYTES) {
			throw new IllegalArgumentException("extra must be " + EXTRA_BYTES + " bytes, got " + extra.length);
		}
		this.extra = extra.clone();
	}

	public int getWestColumn() {
		return westColumn;
	}

	public int getNorthRow() {
		return northRow;
	}

	public int getEastColumn() {
		return eastColumn;
	}

	public int getSouthRow() {
		return southRow;
	}

	public int getFlag() {
		return flag;
	}

	public int getTileSize() {
		return tileSize;
	}

	public int getTilesX() {
		return tilesX;
	}

	public int getTilesY() {
		return tilesY;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getBytesPerCell() {
		return bytesPerCell;
	}

	public byte[] getExtra() {
		return extra;
	}

	public byte[] getBody() {
		return body;
	}

	@Override
	public String toString() {
		return String.format("SrmSection[%dx%d cells, %dx%d tiles of %d, %d bytes/cell, lon %.5f..%.5f lat %.5f..%.5f]",
				width, height, tilesX, tilesY, tileSize, bytesPerCell,
				getWestLon(), getEastLon(), getSouthLat(), getNorthLat());
	}
}
