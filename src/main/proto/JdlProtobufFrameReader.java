package com.ursulagis.desktop.dao.utils;

import com.google.protobuf.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

public class JdlProtobufFrameReader {

    public static void main(String[] args) throws IOException {
        Path machineFolder = Paths.get("D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026");

        // Find all .jdl files
        List<Path> allJdl = Files.walk(machineFolder)
                .filter(p -> p.toString().endsWith(".jdl"))
                .sorted((a, b) -> Long.compare(b.toFile().length(), a.toFile().length()))
                .collect(Collectors.toList());

        if (allJdl.size() < 2) {
            System.out.println("Need at least 2 .jdl files (metadata + data)");
            return;
        }

        // The largest file is the data, the second largest is the metadata
        Path dataFile = allJdl.get(0);
        Path metaFile = allJdl.get(1);   // 0.6 MB metadata file

        System.out.println("Metadata: " + metaFile);
        Map<Integer, ChannelDef> channelDefs = loadChannelDefinitions(metaFile);
        System.out.println("Loaded " + channelDefs.size() + " channel definitions");
        if (channelDefs.isEmpty()) {
            System.out.println("Falling back to hardcoded channels. Please add them manually.");
            // If needed, hardcode from the dump, or exit.
            return;
        }

        System.out.println("Data file: " + dataFile);
        Path csvPath = dataFile.resolveSibling(dataFile.getFileName() + ".harvest.csv");
        exportHarvestCsv(dataFile, channelDefs, csvPath);
        System.out.println("CSV written: " + csvPath);
    }

    private static void exportHarvestCsv(Path dataFile, Map<Integer, ChannelDef> channelDefs,
                                         Path csvPath) throws IOException {
        byte[] rawBytes = readJdlBytes(dataFile);
        int offset = protobufPayloadStartOffset(rawBytes);
        CodedInputStream cis = CodedInputStream.newInstance(rawBytes, offset, rawBytes.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet root = UnknownFieldSet.parseFrom(cis);

        // Get all frames (field 4)
        UnknownFieldSet.Field f4 = root.getField(4);
        List<ByteString> frameList = f4.getLengthDelimitedList();
        System.out.println("Total frames: " + frameList.size());

        // Identify GPS channel IDs (those with unit "arcdeg")
        List<Integer> latChIds = new ArrayList<>();
        List<Integer> lonChIds = new ArrayList<>();
        for (ChannelDef def : channelDefs.values()) {
            if ("arcdeg".equals(def.unit)) {
                if (def.spn == 12008 || def.spn == 12010) { // common John Deere lat/lon SPNs
                    if (def.spn == 12008) latChIds.add(def.id);
                    else lonChIds.add(def.id);
                } else {
                    // Heuristic: first two arcdeg channels become lat/lon if not known
                    if (latChIds.isEmpty()) latChIds.add(def.id);
                    else if (lonChIds.isEmpty()) lonChIds.add(def.id);
                }
            }
        }
        System.out.println("GPS channels – lat IDs: " + latChIds + ", lon IDs: " + lonChIds);

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
            // Write header
            pw.print("frame_index,point_index,latitude,longitude,timestamp_ms");
            // Optionally add columns for all channels (or just selected ones)
            Set<String> importantUnits = new HashSet<>(Arrays.asList("prcnt", "kg1ha-18", "kg1bu-1", "RPM", "km1hr-18"));
            List<Integer> extraChannels = new ArrayList<>();
            for (ChannelDef def : channelDefs.values()) {
                if (importantUnits.contains(def.unit) && !def.unit.equals("arcdeg")) {
                    extraChannels.add(def.id);
                }
            }
            for (int chId : extraChannels) {
                ChannelDef def = channelDefs.get(chId);
                pw.printf(",ch%d_%s", chId, def.unit);
            }
            pw.println();

            int frameIdx = 0;
            for (ByteString frameBs : frameList) {
                UnknownFieldSet frame = UnknownFieldSet.parseFrom(frameBs);
                int timestamp = 0;
                if (frame.hasField(4)) timestamp = getVarint(frame, 4);   // time offset
                // Get the point records (field 3 is repeated inside frame)
                UnknownFieldSet.Field f3 = frame.getField(3);
                List<UnknownFieldSet> points = new ArrayList<>();
                points.addAll(f3.getGroupList());
                for (ByteString bs : f3.getLengthDelimitedList()) {
                    try {
                        points.add(UnknownFieldSet.parseFrom(bs));
                    } catch (IOException ignore) {}
                }

                int pointIdx = 0;
                for (UnknownFieldSet point : points) {
                    if (!point.hasField(2) || !point.hasField(3)) continue;   // packed data
                    byte[] ids = point.getField(2).getLengthDelimitedList().get(0).toByteArray();
                    byte[] vals = point.getField(3).getLengthDelimitedList().get(0).toByteArray();
                    int n = Math.min(ids.length / 2, vals.length / 2);
                    double lat = Double.NaN, lon = Double.NaN;
                    Map<Integer, Double> scaledValues = new HashMap<>();

                    for (int i = 0; i < n; i++) {
                        int chId = ((ids[i*2+1] & 0xFF) << 8) | (ids[i*2] & 0xFF);
                        int raw   = ((vals[i*2+1] & 0xFF) << 8) | (vals[i*2] & 0xFF);
                        ChannelDef def = channelDefs.get(chId);
                        double scaled = raw;
                        if (def != null) {
                            scaled = raw * def.scale;
                            if ("arcdeg".equals(def.unit)) {
                                // GPS sometimes stored as signed 32‑bit × 1e-7
                                // However we only have 16-bit here. John Deere often
                                // sends lat/lon as two 16-bit values: low word then high word.
                                // We'll handle that by looking ahead.
                                // For now, treat as unsigned 16-bit divided by 10^7.
                                scaled = raw / 10_000_000.0;
                            }
                        }
                        if (latChIds.contains(chId)) lat = scaled;
                        else if (lonChIds.contains(chId)) lon = scaled;
                        scaledValues.put(chId, scaled);
                    }

                    // Handle 32-bit GPS (two consecutive channels)
                    // If lat/lon are still NaN, try combining with next channel of same ID? 
                    // Usually the IDs for lat_low, lat_high, lon_low, lon_high.
                    // We'll skip for now.

                    if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                        pw.printf("%d,%d,%.8f,%.8f,%d", frameIdx, pointIdx, lat, lon, timestamp);
                        for (int chId : extraChannels) {
                            double val = scaledValues.getOrDefault(chId, 0.0);
                            pw.printf(",%.4f", val);
                        }
                        pw.println();
                    }
                    pointIdx++;
                }
                frameIdx++;
            }
        }
    }

    // ------------------ Channel definition loading (works with groups) ------------------
    private static Map<Integer, ChannelDef> loadChannelDefinitions(Path metaFile) throws IOException {
        byte[] raw = readJdlBytes(metaFile);
        int offset = protobufPayloadStartOffset(raw);
        CodedInputStream cis = CodedInputStream.newInstance(raw, offset, raw.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet root = UnknownFieldSet.parseFrom(cis);

        UnknownFieldSet.Field f1 = root.getField(1);
        if (f1 == null || f1.getGroupList().isEmpty()) return Collections.emptyMap();
        UnknownFieldSet header = f1.getGroupList().get(0);

        Map<Integer, ChannelDef> map = new HashMap<>();
        UnknownFieldSet.Field chField = header.getField(5);
        if (chField != null) {
            for (UnknownFieldSet ch : chField.getGroupList()) {
                int id = getVarint(ch, 1);
                int spn = getVarint(ch, 2);
                String unit = getString(ch, 5);
                double scale = getDouble(ch, 4);
                map.put(id, new ChannelDef(id, spn, unit, scale));
            }
        }
        return map;
    }

    // ------------------ Helper methods (same as before) ------------------
    private static byte[] readJdlBytes(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        if (data.length >= 2 && data[0] == 0x50 && data[1] == 0x4B) {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) return zis.readAllBytes();
                }
            }
        }
        return data;
    }

    private static int protobufPayloadStartOffset(byte[] data) {
        if (data.length > 5 && data[0] == 0x0D && (data[1] & 0xFF) == 0x13
                && data[2] == 0x00 && data[3] == 0x00 && data[4] == 0x00) {
            return 5;
        }
        return 0;
    }

    private static String getString(UnknownFieldSet msg, int fieldNum) {
        if (msg == null || !msg.hasField(fieldNum)) return "";
        UnknownFieldSet.Field f = msg.getField(fieldNum);
        if (!f.getLengthDelimitedList().isEmpty()) {
            return f.getLengthDelimitedList().get(0).toStringUtf8();
        }
        return "";
    }

    private static int getVarint(UnknownFieldSet msg, int fieldNum) {
        if (msg == null || !msg.hasField(fieldNum)) return 0;
        UnknownFieldSet.Field f = msg.getField(fieldNum);
        if (!f.getVarintList().isEmpty()) {
            return f.getVarintList().get(0).intValue();
        }
        return 0;
    }

    private static double getDouble(UnknownFieldSet msg, int fieldNum) {
        if (msg == null || !msg.hasField(fieldNum)) return 0.0;
        UnknownFieldSet.Field f = msg.getField(fieldNum);
        if (!f.getFixed64List().isEmpty()) {
            return Double.longBitsToDouble(f.getFixed64List().get(0));
        }
        return 0.0;
    }

    // ------------------ Data classes ------------------
    static class ChannelDef {
        final int id, spn;
        final String unit;
        final double scale;

        ChannelDef(int id, int spn, String unit, double scale) {
            this.id = id; this.spn = spn; this.unit = unit; this.scale = scale;
        }

        @Override
        public String toString() {
            return String.format("ID %d (SPN %d) unit='%s' scale=%.2f", id, spn, unit, scale);
        }
    }
}

