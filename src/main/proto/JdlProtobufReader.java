package com.ursulagis.desktop.dao.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Best-effort reader for John Deere .jdl log files that store a root protobuf
 * message (optionally after a 5-byte magic or inside a ZIP wrapper).
 */
public class JdlProtobufReader {

    private static final int FIELD_HEADER = 1;
    private static final int FIELD_DEVICES = 7;
    private static final int FIELD_CLIENT = 11;
    private static final int FIELD_FARM = 12;
    private static final int FIELD_FIELD = 13;
    private static final int FIELD_YEAR = 17;
    private static final int FIELD_CROP = 21;
    private static final int FIELD_VERSION = 24;

    private static final int HF_CHANNELS = 5;
    private static final int HF_SAMPLES = 10;

    /**
     * Optional default paths for local debugging ({@link JdlHarvestLogUtil}, inspectors).
     * Prefer passing file paths as {@code main} arguments; replace or clear for your machine.
     */
    public static final String[] SAMPLE_JDL_FILE_PATHS = {
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/5fd8b3ef-af8f-4b74-9119-321514a92513.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/7d969a27-357c-4d15-8ed9-b11745f42c1d.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/9fa8c8a7-6cfc-4389-9886-a15f6472b07a.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/43f14fca-c802-4794-bb23-a7411696a93b.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/66b832ce-77f1-434e-bbaf-30f764822cfd.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/83dacb24-ea56-4731-b360-6b213ea63e2d.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/89d74d1d-dbf9-4bb6-9e98-de4fd44240c7.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/521d04a6-2f55-483b-80bd-21f45b4ad3fe.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/679ea2bf-06ae-4a77-b5ec-0ea18f502969.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/882c1e6c-e9e8-4317-ae07-f00a4083d111.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/66232aba-947f-4982-83a3-81ddbdb047ba.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/68365f93-f907-48f0-8809-defdf70e9a8f.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/83950766-64e7-499c-9dc3-eeeceaf20dc7.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/c623ad6a-2c8a-48c2-b644-8d69406f6117.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/cef4a387-59bb-4837-9303-a4ce96c9ff66.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/d8e62adf-162f-4d97-b4b4-e5101df09a5e.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/e2353a6c-fe95-4e46-b34b-2c59162ccc9d.jdl",
        "D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/f9fe66c7-8cb2-4c90-a6e8-d5038612c3ca.jdl"
    };

    public static void main(String[] args) throws IOException {
        for (String filePath : SAMPLE_JDL_FILE_PATHS) {
            System.out.println("Processing file: " + filePath);
            String outPath = filePath + ".protobuf-inspect.txt";
			PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath), StandardCharsets.UTF_8));
            readJdlFile(filePath, out);
            out.close();
        }

    }
    public static void readJdlFile(String filePath, PrintWriter out) throws IOException {
    //public static void readJdl(String[] args) throws IOException {
        //String filePath =filePaths[0];
        // if (args != null && args.length > 0) {
        //     filePath = args[0];
        // } else {
        //     System.err.println("Usage: JdlProtobufReader <path-to-file.jdl>");
        //     return;
        // }

        byte[] data = readJdlBytes(filePath);
        int offset = protobufPayloadStartOffset(data);
        CodedInputStream cis = CodedInputStream.newInstance(data, offset, data.length - offset);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        UnknownFieldSet msg;
        try {
            msg = UnknownFieldSet.parseFrom(cis);
        } catch (InvalidProtocolBufferException e) {
            if (offset == 5) {
                cis = CodedInputStream.newInstance(data);
                cis.setSizeLimit(Integer.MAX_VALUE);
                cis.setRecursionLimit(100);
                msg = UnknownFieldSet.parseFrom(cis);
            } else {
                throw e;
            }
        }

        Map<Integer, ChannelDef> channelById = new HashMap<>();
        Map<Integer, String> unitByChannel = new HashMap<>();

        UnknownFieldSet header = getMessageField(msg, FIELD_HEADER);
        if (header != null) {
            for (UnknownFieldSet ch : repeatedMessageFields(header, HF_CHANNELS)) {
                int id = getVarint(ch, 1);
                int spn = getVarint(ch, 2);
                String unit = getString(ch, 5);
                double scale = getDouble(ch, 4);
                int maxRaw = getVarint(ch, 7);

                ChannelDef def = new ChannelDef();
                def.id = id;
                def.spn = spn;
                def.unit = unit;
                def.scale = scale;
                def.maxRaw = maxRaw;
                channelById.put(id, def);
                unitByChannel.put(id, unit);
            }

            out.println("First few data samples:");
            int count = 0;
            for (UnknownFieldSet sample : repeatedMessageFields(header, HF_SAMPLES)) {
                if (count++ > 10) {
                    break;
                }
                int idx = getVarint(sample, 1);
                int refSpn = getVarint(sample, 2);
                double rawValue = getDouble(sample, 8);
               out.printf("  Sample #%d, SPN %d: raw = %f%n", idx, refSpn, rawValue);
            }
        }

        UnknownFieldSet client = getMessageField(msg, FIELD_CLIENT);
        if (client != null) {
            UnknownFieldSet clientInner = getMessageField(client, 1);
            if (clientInner != null) {
                out.println("Client: " + getString(clientInner, 2));
            }
        }

        UnknownFieldSet farm = getMessageField(msg, FIELD_FARM);
        if (farm != null) {
            UnknownFieldSet farmInner = getMessageField(farm, 1);
            if (farmInner != null) {
                out.println("Farm: " + getString(farmInner, 2));
            }
        }

        UnknownFieldSet field = getMessageField(msg, FIELD_FIELD);
        if (field != null) {
            UnknownFieldSet fieldInner = getMessageField(field, 1);
            if (fieldInner != null) {
                out.println("Field: " + getString(fieldInner, 2));
            }
        }

        out.println("Year: " + getVarint(msg, FIELD_YEAR));
        out.println("Version: " + getString(msg, FIELD_VERSION));

        UnknownFieldSet crop = getMessageField(msg, FIELD_CROP);
        if (crop != null) {
            UnknownFieldSet cropInner = getMessageField(crop, 1);
            if (cropInner != null) {
                out.println("Crop: " + getString(cropInner, 2));
                UnknownFieldSet yieldTgt = getMessageField(cropInner, 5);
                if (yieldTgt != null) {
                    double val = getDouble(yieldTgt, 1);
                    String u = getString(yieldTgt, 3);
                    out.printf("  Yield target: %.2f %s%n", val, u);
                }
                UnknownFieldSet moistTgt = getMessageField(cropInner, 6);
                if (moistTgt != null) {
                    double val = getDouble(moistTgt, 1);
                    String u = getString(moistTgt, 3);
                    out.printf("  Moisture target: %.2f %s%n", val, u);
                }
            }
        }

        out.println("Devices/modules:");
        for (UnknownFieldSet dev : repeatedMessageFields(msg, FIELD_DEVICES)) {
            out.println("  - " + getString(dev, 3));
        }

        out.println();
        out.println("Defined channels:");
        channelById.forEach(
                (id, def) ->
                        out.printf(
                                "  ID %3d \u2192 SPN %5d, unit='%s', scale=%.6f, maxRaw=%d%n",
                                id, def.spn, def.unit, def.scale, def.maxRaw));
    }

    /**
     * Read raw bytes from a .jdl path: if the file is a ZIP archive, the payload of
     * the first non-directory entry is returned; otherwise the full file.
     */
    public static byte[] readJdlBytes(String path) throws IOException {
        return readJdlBytes(Path.of(path));
    }

    public static byte[] readJdlBytes(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        if (data.length >= 2 && data[0] == 0x50 && data[1] == 0x4B) {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    return zis.readAllBytes();
                }
            }
        }
        return data;
    }

    /**
     * Skip the 5-byte JD log prefix (0D 13 00 00 00) when present; otherwise 0.
     */
    public static int protobufPayloadStartOffset(byte[] data) {
        if (data == null || data.length <= 5) {
            return 0;
        }
        if (data[0] == 0x0D
                && (data[1] & 0xFF) == 0x13
                && data[2] == 0x00
                && data[3] == 0x00
                && data[4] == 0x00) {
            return 5;
        }
        return 0;
    }

    public static UnknownFieldSet parseRootMessage(byte[] data) throws IOException {
        int offset = protobufPayloadStartOffset(data);
        int len = data.length - offset;
        CodedInputStream cis = CodedInputStream.newInstance(data, offset, len);
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        try {
            return UnknownFieldSet.parseFrom(cis);
        } catch (InvalidProtocolBufferException e) {
            if (offset == 5) {
                CodedInputStream fallback = CodedInputStream.newInstance(data);
                fallback.setSizeLimit(Integer.MAX_VALUE);
                fallback.setRecursionLimit(100);
                return UnknownFieldSet.parseFrom(fallback);
            }
            throw e;
        }
    }

    private static UnknownFieldSet getMessageField(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) return null;
        Field f = parent.getField(fieldNum);
        // 1. Try group (wire type 3/4) – most common in JD files
        // if (f.getGroup() != null) {
        //     return f.getGroup();
        // }
        // 2. Try length‑delimited (wire type 2)
        if (!f.getLengthDelimitedList().isEmpty()) {
            try {
                return UnknownFieldSet.parseFrom(f.getLengthDelimitedList().get(0));
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    // private static UnknownFieldSet getMessageFieldOld(UnknownFieldSet parent, int fieldNum) {
    //     if (parent == null || !parent.hasField(fieldNum)) {
    //         return null;
    //     }
    //     Field f = parent.getField(fieldNum);
    //     if (!f.getLengthDelimitedList().isEmpty()) {
    //         try {
    //             return UnknownFieldSet.parseFrom(f.getLengthDelimitedList().get(0));
    //         } catch (IOException e) {
    //             return null;
    //         }
    //     }
    //     return null;
    // }

    private static List<UnknownFieldSet> repeatedMessageFields(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) return Collections.emptyList();
        Field f = parent.getField(fieldNum);
        List<UnknownFieldSet> out = new ArrayList<>();
        // 1. Repeated groups
        if (!f.getGroupList().isEmpty()) {
            out.addAll(f.getGroupList());
        }
        // 2. Repeated length‑delimited messages
        for (ByteString bs : f.getLengthDelimitedList()) {
            try {
                out.add(UnknownFieldSet.parseFrom(bs));
            } catch (IOException ignored) {}
        }
        return out;
    }

    public static void dumpRepeatedMessages(UnknownFieldSet msg, int fieldNum, PrintWriter out) {
        for (UnknownFieldSet sub : repeatedMessageFields(msg, fieldNum)) {
            out.println("  Record:");
            sub.asMap().forEach((num, field) -> {
                if (!field.getVarintList().isEmpty()) out.printf("    %d (varint): %d%n", num, field.getVarintList().get(0).longValue());
                if (!field.getFixed64List().isEmpty()) {
                    long bits = field.getFixed64List().get(0);
                    out.printf("    %d (fixed64): 0x%016X = %f%n", num, bits, Double.longBitsToDouble(bits));
                }
            });
        }
    }

    // private static List<UnknownFieldSet> repeatedMessageFieldsOld(UnknownFieldSet parent, int fieldNum) {
    //     if (parent == null || !parent.hasField(fieldNum)) {
    //         return Collections.emptyList();
    //     }
    //     Field f = parent.getField(fieldNum);
    //     List<ByteString> chunks = f.getLengthDelimitedList();
    //     if (chunks.isEmpty()) {
    //         return Collections.emptyList();
    //     }
    //     List<UnknownFieldSet> out = new ArrayList<>(chunks.size());
    //     for (ByteString bs : chunks) {
    //         try {
    //             out.add(UnknownFieldSet.parseFrom(bs));
    //         } catch (IOException ignored) {
    //             // skip malformed pieces
    //         }
    //     }
    //     return out;
    // }

    private static String getString(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) {
            return "";
        }
        Field f = parent.getField(fieldNum);
        if (!f.getLengthDelimitedList().isEmpty()) {
            return f.getLengthDelimitedList().get(0).toStringUtf8();
        }
        return "";
    }

    private static int getVarint(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) {
            return 0;
        }
        Field f = parent.getField(fieldNum);
        if (!f.getVarintList().isEmpty()) {
            return f.getVarintList().get(0).intValue();
        }
        return 0;
    }

    private static double getDouble(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) {
            return 0.0;
        }
        Field f = parent.getField(fieldNum);
        if (!f.getFixed64List().isEmpty()) {
            return Double.longBitsToDouble(f.getFixed64List().get(0));
        }
        return 0.0;
    }

    static class ChannelDef {
        int id;
        int spn;
        String unit;
        double scale;
        int maxRaw;
    }
}
