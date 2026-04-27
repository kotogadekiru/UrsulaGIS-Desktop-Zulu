package com.ursulagis.desktop.dao.utils;

import com.google.protobuf.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

public class ProtobufJdlInspector {
    private static final Map<Integer, ChannelDef> BUILTIN_CHANNELS = new HashMap<>();
static {
    // ID, SPN, Unit, Scale (from earlier 5fd8b3ef dump)
    BUILTIN_CHANNELS.put(507, new ChannelDef(507, 12871, "unitless", 1.0));
    BUILTIN_CHANNELS.put(508, new ChannelDef(508, 12872, "unitless", 1.0));
    BUILTIN_CHANNELS.put(509, new ChannelDef(509, 12873, "unitless", 1.0));
    BUILTIN_CHANNELS.put(510, new ChannelDef(510, 807, "unitless", 1.0));
    BUILTIN_CHANNELS.put(1433, new ChannelDef(1433, 2559, "unitless", 1.0));
    BUILTIN_CHANNELS.put(1434, new ChannelDef(1434, 2559, "unitless", 1.0));
    BUILTIN_CHANNELS.put(1435, new ChannelDef(1435, 2045, "prcnt", 1.0));
    BUILTIN_CHANNELS.put(1436, new ChannelDef(1436, 2047, "bar", 0.5));  // scale 0x3fe0 = 0.5
    BUILTIN_CHANNELS.put(1437, new ChannelDef(1437, 2046, "bar", 0.5));
    BUILTIN_CHANNELS.put(1438, new ChannelDef(1438, 2133, "prcnt", 1.0));
    BUILTIN_CHANNELS.put(1439, new ChannelDef(1439, 12002, "m", 0.5));  // scale 0x408f40 = 1000? Actually 0x408f400000000000 = 1000 (double)
    // Add more as needed, especially arcdeg channels:
    BUILTIN_CHANNELS.put(1501, new ChannelDef(1501, 12010, "arcdeg", 1.0));
    BUILTIN_CHANNELS.put(1504, new ChannelDef(1504, 12008, "arcdeg", 1.0));
    BUILTIN_CHANNELS.put(1506, new ChannelDef(1506, 12012, "arcdeg", 1.0));
    // ... fill in the rest from your dump
}


    public static void main(String[] args) throws IOException {
        Path machineFolder = Paths.get("D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026");
    
        List<Path> allJdl = Files.walk(machineFolder)
                .filter(p -> p.toString().endsWith(".jdl"))
                .sorted((a, b) -> Long.compare(b.toFile().length(), a.toFile().length()))
                .collect(Collectors.toList());
    
        if (allJdl.isEmpty()) return;
    
        allJdl.forEach(p -> {
            try {
                String outPath = p.toString() + ".channel-definitions.txt";
                PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath), StandardCharsets.UTF_8));
                System.out.println("Processing file: " + p);
                fullDump(p, out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // return;
        // // 1) Metadata file – the 0.6 MB one (second largest)
        // Path metaFile = allJdl.get(1);  // index 1 = 0.6 MB
        // System.out.println("Metadata file: " + metaFile);
        // //Map<Integer, ChannelDef> channelDefs = loadChannelDefinitions(metaFile);
        // Map<Integer, ChannelDef> channelDefs = BUILTIN_CHANNELS;
        // System.out.println("Channel definitions loaded: " + channelDefs.size());
    
        // // 2) Data file – the 9.4 MB one (largest)
        // Path dataFile = allJdl.get(0);
        // System.out.println("Data file: " + dataFile);
        // List<Measurement> measurements = parseDataFile(dataFile, channelDefs);
        // System.out.println("Measurements extracted: " + measurements.size());
    
        // // 3) Write CSV
        // Path csvPath = dataFile.resolveSibling(dataFile.getFileName() + ".decoded.csv");
        // try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
        //     pw.println("index,channel_id,spn,unit,raw_value,scaled_value");
        //     for (int i = 0; i < measurements.size(); i++) {
        //         Measurement m = measurements.get(i);
        //         ChannelDef def = channelDefs.get(m.channelId);
        //         String unit = def != null ? def.unit : "?";
        //         int spn = def != null ? def.spn : 0;
        //         double scaled = m.rawValue;
        //         if (def != null) {
        //             scaled = m.rawValue * def.scale;
        //             if ("arcdeg".equals(unit)) {
        //                 // GPS usually stored as integer * 1e‑7 (signed 32‑bit)
        //                 scaled = (int) m.rawValue / 10_000_000.0;
        //             }
        //         }
        //         pw.printf("%d,%d,%d,%s,%d,%f%n", i, m.channelId, spn, unit, m.rawValue, scaled);
        //     }
        // }
        // System.out.println("CSV written: " + csvPath);
    }

    private static void dumpChannelDefinitions(Path metadataFile, PrintWriter out) throws IOException {
        byte[] raw = readJdlBytes(metadataFile);
        int offset = protobufPayloadStartOffset(raw);
        CodedInputStream cis = CodedInputStream.newInstance(raw, offset, raw.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet root = UnknownFieldSet.parseFrom(cis);
    
        UnknownFieldSet header = getSingleMessageField(root, 5);
        if (header == null) {
            out.println("No header (field 1) found.");
            return;
        }
    
        List<UnknownFieldSet> channels = repeatedMessageFields(header, 5);
        if (channels.isEmpty()) {
            out.println("No channel definitions in header.");
            return;
        }
    
        out.println("// Auto-generated channel definitions from " + metadataFile.getFileName());
        for (UnknownFieldSet ch : channels) {
            int id = getVarint(ch, 1);
            int spn = getVarint(ch, 2);
            String unit = getString(ch, 5);
            double scale = getDouble(ch, 4);            // fixed64 scale
            if (scale == 0.0) scale = 1.0;              // default scale = 1
            out.printf("BUILTIN_CHANNELS.put(%d, new ChannelDef(%d, %d, \"%s\", %.6f));%n",
                    id, id, spn, unit, scale);
        }
      
    }

    private static void fullDump(Path path, PrintWriter out) throws IOException {
        byte[] raw = readJdlBytes(path);
        int offset = protobufPayloadStartOffset(raw);
        CodedInputStream cis = CodedInputStream.newInstance(raw, offset, raw.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet msg = UnknownFieldSet.parseFrom(cis);
        com.google.protobuf.UnknownFieldSet.Field f4 = msg.getField(4);
        List<ByteString> field4List = f4.getLengthDelimitedList();
        for (int i = 0; i < field4List.size(); i++) {
            UnknownFieldSet frame = UnknownFieldSet.parseFrom(field4List.get(i));
            out.println("Frame " + i + ": \n" + frame.toString());
            // frame has field 5 (packed blob)
            // if (frame.hasField(5)) {
            //      List<ByteString> list = frame.getField(5).getLengthDelimitedList();
            //      if (!list.isEmpty()) {
            //          byte[] blob =list.get(0).toByteArray();
            //          dumpPackedBlob(blob, out);
            //      }
            // }
        }
        // out.println(msg.toString());
    }

    private static void dumpPackedBlob(byte[] blob, PrintWriter out) {
        try {
            UnknownFieldSet inner = UnknownFieldSet.parseFrom(blob);
            out.println(inner.toString());
        } catch (IOException e) {
            System.out.println("Failed to parse blob: " + e.getMessage());
            // fallback: show hex of first 64 bytes
            for (int i = 0; i < Math.min(64, blob.length); i++) {
                out.printf("%02X ", blob[i]);
            }
            out.println();
        }
    }
    
    private static List<Measurement> parseDataFile(Path path, Map<Integer, ChannelDef> channelDefs) throws IOException {
        byte[] raw = readJdlBytes(path);
        int offset = protobufPayloadStartOffset(raw);
        
        // We'll scan the whole file manually
        CodedInputStream cis = CodedInputStream.newInstance(raw, offset, raw.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(1);
    
        List<Measurement> measurements = new ArrayList<>();
        int currentChannel = 0;
    
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            if (tag == 0) break; // end of stream
            int fieldNum = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (fieldNum == 1) {
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    currentChannel = cis.readInt32();   // channel index
                } else if (wireType == WireFormat.WIRETYPE_FIXED32) {
                    int rawValue = cis.readFixed32();
                    // Convert unsigned 32‑bit to long to keep full range
                    long val = rawValue & 0xFFFFFFFFL;
                    measurements.add(new Measurement(currentChannel, val));
                } else {
                    // skip unknown wire type
                    cis.skipField(tag);
                }
            } else {
                // skip non‑field‑1 field (e.g., field 2, 3 … which may appear later)
                cis.skipField(tag);
            }
        }
        return measurements;
    }

    private static Map<Integer, ChannelDef> loadChannelDefinitions(Path path) throws IOException {
        byte[] raw = readJdlBytes(path);
        int offset = protobufPayloadStartOffset(raw);
        CodedInputStream cis = CodedInputStream.newInstance(raw, offset, raw.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet root = UnknownFieldSet.parseFrom(cis);
    
        // Field 1 is the header (group or length‑delimited)
        UnknownFieldSet.Field f1 = root.getField(1);
        UnknownFieldSet header = null;
        if (f1 != null) {
            if (!f1.getGroupList().isEmpty()) {
                header = f1.getGroupList().get(0);
            } else if (!f1.getLengthDelimitedList().isEmpty()) {
                header = UnknownFieldSet.parseFrom(f1.getLengthDelimitedList().get(0));
            }
        }
        if (header == null) return Collections.emptyMap();
    
        Map<Integer, ChannelDef> map = new HashMap<>();
        // Channel definitions are in repeated field 5 inside the header
        for (UnknownFieldSet ch : repeatedMessageFields(header, 5)) {
            int id = getVarint(ch, 1);
            int spn = getVarint(ch, 2);
            String unit = getString(ch, 5);
            double scale = getDouble(ch, 4);
            map.put(id, new ChannelDef(id, spn, unit, scale));
        }
        return map;
    }

    private static void exportCsv(Path path) throws IOException {
        byte[] rawBytes = readJdlBytes(path);
        // Print the first 32 bytes of what we're about to parse
        System.out.print("First bytes after ZIP/magic handling (" + rawBytes.length + " total): ");
        for (int i = 0; i < Math.min(32, rawBytes.length); i++) {
            System.out.printf("%02X ", rawBytes[i]);
        }
        System.out.println();

        int offset = protobufPayloadStartOffset(rawBytes);
        CodedInputStream cis = CodedInputStream.newInstance(rawBytes, offset, rawBytes.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet msg = UnknownFieldSet.parseFrom(cis);

        System.out.println("=== Root message dump (first 5000 chars) ===");
        String dump = msg.toString();
        if (dump.length() > 5000) dump = dump.substring(0, 5000) + "...";
        System.out.println(dump);

        // 1. Build channel definitions
        Map<Integer, ChannelDef> channelDefs = new HashMap<>();
        UnknownFieldSet header = getSingleMessageField(msg, 1);
        if (header != null) {
            for (UnknownFieldSet ch : repeatedMessageFields(header, 5)) {
                int id = getVarint(ch, 1);
                int spn = getVarint(ch, 2);
                String unit = getString(ch, 5);
                double scale = getDouble(ch, 4);
                channelDefs.put(id, new ChannelDef(id, spn, unit, scale));
            }
        }
        System.out.println("Channel definitions: " + channelDefs.size());

        // 2. Get the concatenated point records from field 3
        List<UnknownFieldSet> points = new ArrayList<>();
        if (msg.hasField(3)) {
            UnknownFieldSet.Field f3 = msg.getField(3);
            // Try groups first
            points.addAll(f3.getGroupList());
            // Try length‑delimited (single blob that might itself be a message with repeated points)
            for (ByteString bs : f3.getLengthDelimitedList()) {
                try {
                    UnknownFieldSet inner = UnknownFieldSet.parseFrom(bs);
                    // The inner message might have a repeated field (e.g., field 1 = repeated points)
                    // We need to find the field that has many entries. Let's just look for the largest repeated field.
                    List<UnknownFieldSet> candidates = new ArrayList<>();
                    for (int fn = 1; fn <= 10; fn++) {
                        if (inner.hasField(fn)) {
                            UnknownFieldSet.Field fi = inner.getField(fn);
                            candidates.addAll(fi.getGroupList());
                            for (ByteString innerBs : fi.getLengthDelimitedList()) {
                                try {
                                    candidates.add(UnknownFieldSet.parseFrom(innerBs));
                                } catch (IOException ex) {}
                            }
                        }
                    }
                    if (!candidates.isEmpty()) {
                        points.addAll(candidates);
                    } else {
                        // fallback: use the inner message itself as the point container
                        // but something must be added
                    }
                } catch (IOException ex) {
                    // blob couldn't be parsed as a single message – may be raw bytes
                }
            }
        }
        System.out.println("Points extracted: " + points.size());

        // 3. Find channel IDs that represent lat/lon
        int latCh = -1, lonCh = -1;
        for (ChannelDef def : channelDefs.values()) {
            if (def.unit.equals("arcdeg")) {
                if (latCh == -1) latCh = def.id;
                else if (lonCh == -1) lonCh = def.id;
            }
        }
        if (latCh == -1 || lonCh == -1) {
            // fallback: hard‑coded SPNs from your earlier data (12008=lat, 12010=lon?)
            for (ChannelDef def : channelDefs.values()) {
                if (def.spn == 12008) latCh = def.id;
                if (def.spn == 12010) lonCh = def.id;
            }
        }
        System.out.printf("GPS channels – latitude ID=%d, longitude ID=%d%n", latCh, lonCh);

        // 4. Write CSV
        Path csvPath = Paths.get(path.toString() + ".points.csv");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
            pw.println("index,latitude,longitude,channel_id,channel_unit,raw_value,scaled_value");
            int count = 0;
            for (UnknownFieldSet pt : points) {
                if (!pt.hasField(2) || !pt.hasField(3)) continue;
                byte[] ids = pt.getField(2).getLengthDelimitedList().get(0).toByteArray();
                byte[] vals = pt.getField(3).getLengthDelimitedList().get(0).toByteArray();
                int n = Math.min(ids.length / 2, vals.length / 2);
                double lat = Double.NaN, lon = Double.NaN;
                Map<Integer, Double> measurements = new HashMap<>();
                for (int i = 0; i < n; i++) {
                    int chId = ((ids[i*2+1] & 0xFF) << 8) | (ids[i*2] & 0xFF);
                    int raw  = ((vals[i*2+1] & 0xFF) << 8) | (vals[i*2] & 0xFF);
                    ChannelDef def = channelDefs.get(chId);
                    double scaled = raw;
                    if (def != null) {
                        scaled = raw * def.scale;
                        if (def.unit.equals("arcdeg")) {
                            // GPS usually stored as integer * 1e-7
                            if (Math.abs(def.scale - 1.0) < 0.001) {
                                scaled = raw / 10_000_000.0;   // typical JD scaling
                            }
                        }
                    }
                    if (chId == latCh) lat = scaled;
                    else if (chId == lonCh) lon = scaled;
                    measurements.put(chId, scaled);
                }
                // Write only if we have GPS (optional: write all points)
                if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                    for (Map.Entry<Integer, Double> m : measurements.entrySet()) {
                        int ch = m.getKey();
                        double val = m.getValue();
                        ChannelDef def = channelDefs.get(ch);
                        String unit = (def != null) ? def.unit : "?";
                        pw.printf("%d,%.8f,%.8f,%d,%s,%f,%f%n",
                                count, lat, lon, ch, unit, val, val);
                    }
                }
                count++;
            }
        }
        System.out.println("CSV written: " + csvPath);
    }

    // ---------- reused helper methods (same as before, plus splitting) ----------

    private static List<UnknownFieldSet> splitConcatenatedProtobuf(byte[] data) throws IOException {
        List<UnknownFieldSet> list = new ArrayList<>();
        CodedInputStream cis = CodedInputStream.newInstance(data);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            if (tag == 0) break;
            int size = cis.readRawVarint32();
            byte[] msgBytes = cis.readRawBytes(size);
            list.add(UnknownFieldSet.parseFrom(msgBytes));
        }
        return list;
    }

//new 
private static UnknownFieldSet getSingleMessageField(UnknownFieldSet parent, int fieldNum) {
    if (parent == null || !parent.hasField(fieldNum)) return null;
    UnknownFieldSet.Field f = parent.getField(fieldNum);
    if (!f.getGroupList().isEmpty()) return f.getGroupList().get(0);
    if (!f.getLengthDelimitedList().isEmpty()) {
        try {
            return UnknownFieldSet.parseFrom(f.getLengthDelimitedList().get(0));
        } catch (IOException e) { return null; }
    }
    return null;
}

private static List<UnknownFieldSet> repeatedMessageFields(UnknownFieldSet parent, int fieldNum) {
    if (parent == null || !parent.hasField(fieldNum)) return Collections.emptyList();
    UnknownFieldSet.Field f = parent.getField(fieldNum);
    List<UnknownFieldSet> out = new ArrayList<>();
    out.addAll(f.getGroupList());
    for (ByteString bs : f.getLengthDelimitedList()) {
        try {
            byte[] bytes = bs.toByteArray();
            if (bytes.length > 1000) {
                out.addAll(splitConcatenatedProtobuf(bytes));
            } else {
                out.add(UnknownFieldSet.parseFrom(bs));
            }
        } catch (IOException ignore) {}
    }
    return out;
}

private static String getString(UnknownFieldSet msg, int fieldNum) {
    if (msg == null || !msg.hasField(fieldNum)) return "";
    UnknownFieldSet.Field f = msg.getField(fieldNum);
    if (!f.getLengthDelimitedList().isEmpty()) return f.getLengthDelimitedList().get(0).toStringUtf8();
    return "";
}

private static int getVarint(UnknownFieldSet msg, int fieldNum) {
    if (msg == null || !msg.hasField(fieldNum)) return 0;
    UnknownFieldSet.Field f = msg.getField(fieldNum);
    if (!f.getVarintList().isEmpty()) return f.getVarintList().get(0).intValue();
    return 0;
}

private static double getDouble(UnknownFieldSet msg, int fieldNum) {
    if (msg == null || !msg.hasField(fieldNum)) return 0.0;
    UnknownFieldSet.Field f = msg.getField(fieldNum);
    if (!f.getFixed64List().isEmpty()) return Double.longBitsToDouble(f.getFixed64List().get(0));
    return 0.0;
}
    //old
    

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

    static class ChannelDef {
        final int id, spn;
        final String unit;
        final double scale;
        ChannelDef(int id, int spn, String unit, double scale) {
            this.id = id; this.spn = spn; this.unit = unit; this.scale = scale;
        }
    }

    private static class Measurement {
        final int channelId;
        final long rawValue;   // or double, depending on encoding
    
        Measurement(int channelId, long rawValue) {
            this.channelId = channelId;
            this.rawValue = rawValue;
        }
    }
}