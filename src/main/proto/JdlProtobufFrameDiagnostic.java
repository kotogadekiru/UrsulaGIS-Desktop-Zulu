package com.ursulagis.desktop.dao.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Standalone diagnostic reader for John Deere .jdl files whose top-level field 4
 * contains embedded protobuf frames.
 */
public class JdlProtobufFrameDiagnostic {

    private static final int ROOT_FRAME_FIELD = 4;
    private static final int FRAME_RECORD_FIELD = 3;
    private static final int RECORD_IDS_FIELD = 2;
    private static final int RECORD_VALUES_FIELD = 3;
    private static final int HEX_PREVIEW_BYTES = 64;
    private static final int TEXT_PREVIEW_CHARS = 120;
    private static final int VARINT_PREVIEW_VALUES = 80;
    private static final int RECORD_CONTAINER_SCAN_DEPTH = 5;

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: JdlProtobufFrameDiagnostic <file-or-folder.jdl> [output-folder]");
            return;
        }

        Path input = Path.of(args[0]);
        Path outputFolder = args.length == 2 ? Path.of(args[1]) : null;
        List<Path> inputs = collectInputs(input);
        if (inputs.isEmpty()) {
            System.err.println("No .jdl files found in " + input);
            return;
        }

        for (Path jdl : inputs) {
            Path outDir = outputFolder != null ? outputFolder : jdl.getParent();
            if (outDir != null) {
                Files.createDirectories(outDir);
            }
            diagnose(jdl, outDir);
        }
    }

    public static void diagnose(Path jdlFile, Path outputFolder) throws IOException {
        byte[] rawBytes = JdlProtobufReader.readJdlBytes(jdlFile);
        UnknownFieldSet root = JdlProtobufReader.parseRootMessage(rawBytes);

        Path outDir = outputFolder != null ? outputFolder : jdlFile.getParent();
        if (outDir == null) {
            outDir = Path.of(".");
        }
        String baseName = jdlFile.getFileName().toString();
        Path framesPath = outDir.resolve(baseName + ".frames.txt");
        Path summaryPath = outDir.resolve(baseName + ".frame-summary.csv");
        Path recordsPath = outDir.resolve(baseName + ".frame-records.csv");
        Path dataPath = outDir.resolve(baseName + ".data.txt");

        try (PrintWriter frames = new PrintWriter(Files.newBufferedWriter(framesPath, StandardCharsets.UTF_8));
                PrintWriter summary = new PrintWriter(Files.newBufferedWriter(summaryPath, StandardCharsets.UTF_8));
                PrintWriter records = new PrintWriter(Files.newBufferedWriter(recordsPath, StandardCharsets.UTF_8));
                PrintWriter data = new PrintWriter(Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8))) {
            writeHeaders(summary, records);

            frames.println("Input: " + jdlFile);
            frames.println("Unwrapped payload bytes: " + rawBytes.length);
            frames.println("Root fields: " + fieldNumbers(root));
            frames.println();

            Field frameField = root.getField(ROOT_FRAME_FIELD);
            if (frameField == null || frameField.getLengthDelimitedList().isEmpty()) {
                frames.println("No top-level field " + ROOT_FRAME_FIELD + " length-delimited frames found.");
                return;
            }

            List<ByteString> frameBytes = frameField.getLengthDelimitedList();
            frames.println("Top-level field " + ROOT_FRAME_FIELD + " embedded frames: " + frameBytes.size());
            frames.println();
            data.println("Input: " + jdlFile);
            data.println();

            for (int frameIndex = 0; frameIndex < frameBytes.size(); frameIndex++) {
                ByteString frameByteString = frameBytes.get(frameIndex);
                UnknownFieldSet frame = parseLengthDelimited(frameByteString);
                writeFrameDump(frames, frameIndex, frameByteString, frame);
                writeFrameSummary(summary, frameIndex, frameByteString, frame);
                writeFrameRecords(records, frameIndex, frame);
                writeDataOnly(data, frame);
            }
        }

        System.out.println("Wrote " + framesPath);
        System.out.println("Wrote " + summaryPath);
        System.out.println("Wrote " + recordsPath);
        System.out.println("Wrote " + dataPath);
        System.out.println();
        System.out.println("Decoded data:");
        try (PrintWriter console = new PrintWriter(System.out, true)) {
            for (ByteString frameByteString : root.getField(ROOT_FRAME_FIELD).getLengthDelimitedList()) {
                writeDataOnly(console, parseLengthDelimited(frameByteString));
            }
        }
    }

    private static List<Path> collectInputs(Path input) throws IOException {
        if (Files.isRegularFile(input)) {
            return List.of(input);
        }
        if (!Files.isDirectory(input)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(input)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jdl"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
    }

    private static void writeHeaders(PrintWriter summary, PrintWriter records) {
        summary.println("frame_index,frame_bytes,field_numbers,field_summaries,record_count");
        records.println(
                "frame_index,record_index,path,blob_field,blob_bytes,varint_count,id_count,value_count,"
                        + "paired_count,varints_preview,paired_preview,hex_preview,text_preview,"
                        + "parseable_message_fields,parseable_scalar_preview");
    }

    private static void writeFrameDump(PrintWriter out, int frameIndex, ByteString frameBytes, UnknownFieldSet frame) {
        out.println("Frame " + frameIndex);
        out.println("Bytes: " + frameBytes.size());
        out.println("Fields: " + fieldNumbers(frame));
        out.println("Field summaries:");
        for (Map.Entry<Integer, Field> entry : frame.asMap().entrySet()) {
            out.println("  " + entry.getKey() + ": " + describeField(entry.getValue()));
        }
        out.println("UnknownFieldSet:");
        out.println(frame);
        out.println("Length-delimited diagnostics:");
        writeLengthDelimitedDiagnostics(out, frame, "frame");
        out.println();
    }

    private static void writeFrameSummary(
            PrintWriter out, int frameIndex, ByteString frameBytes, UnknownFieldSet frame) {
        out.printf("%d,%d,%s,%s,%d%n",
                frameIndex,
                frameBytes.size(),
                csv(fieldNumbers(frame)),
                csv(describeFields(frame)),
                countCandidateRecords(frame));
    }

    private static void writeFrameRecords(PrintWriter out, int frameIndex, UnknownFieldSet frame) {
        List<MessageAtPath> containers = new ArrayList<>();
        collectRecordContainers(containers, frame, "frame", 0);

        int recordIndex = 0;
        for (MessageAtPath container : containers) {
            List<UnknownFieldSet> records = repeatedMessages(container.message, FRAME_RECORD_FIELD);
            for (int localIndex = 0; localIndex < records.size(); localIndex++) {
                UnknownFieldSet record = records.get(localIndex);
                if (!isCandidateRecord(record)) {
                    continue;
                }

                String path = container.path + ".3[" + localIndex + "]";
                List<Long> ids = firstLengthDelimitedVarints(record, RECORD_IDS_FIELD);
                List<Long> values = firstLengthDelimitedVarints(record, RECORD_VALUES_FIELD);
                int pairedCount = Math.min(ids.size(), values.size());

                writeRecordBlob(out, frameIndex, recordIndex, path, RECORD_IDS_FIELD, record,
                        ids, ids, values, pairedCount);
                writeRecordBlob(out, frameIndex, recordIndex, path, RECORD_VALUES_FIELD, record,
                        values, ids, values, pairedCount);

                writeNestedRecordBlobs(out, frameIndex, recordIndex, record, path);
                recordIndex++;
            }
        }
    }

    private static void writeDataOnly(PrintWriter out, UnknownFieldSet frame) {
        writeCoordinateData(out, "coordinate", frame, 6);
        writeCoordinateData(out, "coordinate", frame, 7);

        List<MessageAtPath> containers = new ArrayList<>();
        collectRecordContainers(containers, frame, "frame", 0);
        for (MessageAtPath container : containers) {
            for (UnknownFieldSet record : repeatedMessages(container.message, FRAME_RECORD_FIELD)) {
                if (!isCandidateRecord(record)) {
                    continue;
                }
                List<Long> ids = firstLengthDelimitedVarints(record, RECORD_IDS_FIELD);
                List<Long> values = firstLengthDelimitedVarints(record, RECORD_VALUES_FIELD);
                if (!ids.isEmpty() && !values.isEmpty()) {
                    out.println("channel_values: " + pairPreview(ids, values, Integer.MAX_VALUE));
                }
                writeDecodedScalarData(out, record);
            }
        }
        out.flush();
    }

    private static void writeCoordinateData(PrintWriter out, String label, UnknownFieldSet frame, int fieldNum) {
        ByteString blob = firstLengthDelimited(frame, fieldNum);
        if (blob == null) {
            return;
        }
        UnknownFieldSet coord = tryParse(blob);
        if (coord == null || !coord.hasField(1) || !coord.hasField(2)) {
            return;
        }
        Field latField = coord.getField(1);
        Field lonField = coord.getField(2);
        if (latField.getFixed64List().isEmpty() || lonField.getFixed64List().isEmpty()) {
            return;
        }
        double latitude = Double.longBitsToDouble(latField.getFixed64List().get(0));
        double longitude = Double.longBitsToDouble(lonField.getFixed64List().get(0));
        out.printf("%s: latitude=%.12f longitude=%.12f%n", label, latitude, longitude);
    }

    private static void writeDecodedScalarData(PrintWriter out, UnknownFieldSet message) {
        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            Field field = entry.getValue();
            for (ByteString blob : field.getLengthDelimitedList()) {
                UnknownFieldSet parsed = tryParse(blob);
                if (parsed != null) {
                    String scalars = scalarPreview(parsed);
                    if (!scalars.isEmpty()) {
                        out.println("decoded: " + scalars);
                    }
                    writeDecodedScalarData(out, parsed);
                }
            }
            for (UnknownFieldSet nested : field.getGroupList()) {
                String scalars = scalarPreview(nested);
                if (!scalars.isEmpty()) {
                    out.println("decoded: " + scalars);
                }
                writeDecodedScalarData(out, nested);
            }
        }
    }

    private static void writeRecordBlob(
            PrintWriter out,
            int frameIndex,
            int recordIndex,
            String path,
            int fieldNum,
            UnknownFieldSet record,
            List<Long> varints,
            List<Long> ids,
            List<Long> values,
            int pairedCount) {
        ByteString blob = firstLengthDelimited(record, fieldNum);
        if (blob == null) {
            return;
        }
        UnknownFieldSet parseable = tryParse(blob);
        out.printf("%d,%d,%s,%d,%d,%d,%d,%d,%d,%s,%s,%s,%s,%s,%s%n",
                frameIndex,
                recordIndex,
                csv(path),
                fieldNum,
                blob.size(),
                varints.size(),
                ids.size(),
                values.size(),
                pairedCount,
                csv(preview(varints, VARINT_PREVIEW_VALUES)),
                csv(pairPreview(ids, values, VARINT_PREVIEW_VALUES)),
                csv(hexPreview(blob.toByteArray())),
                csv(textPreview(blob.toByteArray())),
                csv(parseable == null ? "" : fieldNumbers(parseable)),
                csv(parseable == null ? "" : scalarPreview(parseable)));
    }

    private static void writeNestedRecordBlobs(
            PrintWriter out, int frameIndex, int recordIndex, UnknownFieldSet message, String path) {
        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            int fieldNum = entry.getKey();
            Field field = entry.getValue();
            for (int i = 0; i < field.getLengthDelimitedList().size(); i++) {
                if (fieldNum == RECORD_IDS_FIELD || fieldNum == RECORD_VALUES_FIELD) {
                    continue;
                }
                ByteString blob = field.getLengthDelimitedList().get(i);
                List<Long> varints = decodePackedVarints(blob.toByteArray());
                UnknownFieldSet parseable = tryParse(blob);
                out.printf("%d,%d,%s,%d,%d,%d,%d,%d,%d,%s,%s,%s,%s,%s,%s%n",
                        frameIndex,
                        recordIndex,
                        csv(path + "." + fieldNum + "[" + i + "]"),
                        fieldNum,
                        blob.size(),
                        varints.size(),
                        0,
                        0,
                        0,
                        csv(preview(varints, VARINT_PREVIEW_VALUES)),
                        csv(""),
                        csv(hexPreview(blob.toByteArray())),
                        csv(textPreview(blob.toByteArray())),
                        csv(parseable == null ? "" : fieldNumbers(parseable)),
                        csv(parseable == null ? "" : scalarPreview(parseable)));
            }
            for (int i = 0; i < field.getGroupList().size(); i++) {
                writeNestedRecordBlobs(out, frameIndex, recordIndex, field.getGroupList().get(i),
                        path + "." + fieldNum + "{" + i + "}");
            }
        }
    }

    private static void writeLengthDelimitedDiagnostics(PrintWriter out, UnknownFieldSet message, String path) {
        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            int fieldNum = entry.getKey();
            Field field = entry.getValue();
            for (int i = 0; i < field.getLengthDelimitedList().size(); i++) {
                ByteString blob = field.getLengthDelimitedList().get(i);
                List<Long> varints = decodePackedVarints(blob.toByteArray());
                UnknownFieldSet parseable = tryParse(blob);
                out.println("  " + path + "." + fieldNum + "[" + i + "]"
                        + " bytes=" + blob.size()
                        + " hex=" + hexPreview(blob.toByteArray())
                        + " text=" + textPreview(blob.toByteArray())
                        + " varints=" + preview(varints, VARINT_PREVIEW_VALUES)
                        + " parseableFields=" + (parseable == null ? "" : fieldNumbers(parseable))
                        + " scalars=" + (parseable == null ? "" : scalarPreview(parseable)));
                if (parseable != null && blob.size() < 4096) {
                    writeLengthDelimitedDiagnostics(out, parseable, path + "." + fieldNum + "[" + i + "]");
                }
            }
            for (int i = 0; i < field.getGroupList().size(); i++) {
                writeLengthDelimitedDiagnostics(out, field.getGroupList().get(i), path + "." + fieldNum + "{" + i + "}");
            }
        }
    }

    private static UnknownFieldSet parseLengthDelimited(ByteString bytes) throws IOException {
        CodedInputStream cis = bytes.newCodedInput();
        cis.setSizeLimit(Integer.MAX_VALUE);
        cis.setRecursionLimit(100);
        return UnknownFieldSet.parseFrom(cis);
    }

    private static UnknownFieldSet tryParse(ByteString bytes) {
        try {
            return parseLengthDelimited(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    private static List<UnknownFieldSet> repeatedMessages(UnknownFieldSet parent, int fieldNum) {
        if (parent == null || !parent.hasField(fieldNum)) {
            return List.of();
        }
        List<UnknownFieldSet> messages = new ArrayList<>();
        Field field = parent.getField(fieldNum);
        messages.addAll(field.getGroupList());
        for (ByteString blob : field.getLengthDelimitedList()) {
            UnknownFieldSet parsed = tryParse(blob);
            if (parsed != null) {
                messages.add(parsed);
            }
        }
        return messages;
    }

    private static int countRepeatedMessages(UnknownFieldSet parent, int fieldNum) {
        return repeatedMessages(parent, fieldNum).size();
    }

    private static int countCandidateRecords(UnknownFieldSet frame) {
        List<MessageAtPath> containers = new ArrayList<>();
        collectRecordContainers(containers, frame, "frame", 0);
        int count = 0;
        for (MessageAtPath container : containers) {
            for (UnknownFieldSet record : repeatedMessages(container.message, FRAME_RECORD_FIELD)) {
                if (isCandidateRecord(record)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void collectRecordContainers(
            List<MessageAtPath> containers, UnknownFieldSet message, String path, int depth) {
        if (message == null || depth > RECORD_CONTAINER_SCAN_DEPTH) {
            return;
        }
        if (hasCandidateRecords(message)) {
            containers.add(new MessageAtPath(path, message));
        }

        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            int fieldNum = entry.getKey();
            Field field = entry.getValue();
            for (int i = 0; i < field.getGroupList().size(); i++) {
                collectRecordContainers(containers, field.getGroupList().get(i),
                        path + "." + fieldNum + "{" + i + "}", depth + 1);
            }
            for (int i = 0; i < field.getLengthDelimitedList().size(); i++) {
                ByteString blob = field.getLengthDelimitedList().get(i);
                UnknownFieldSet parsed = tryParse(blob);
                if (parsed != null) {
                    collectRecordContainers(containers, parsed,
                            path + "." + fieldNum + "[" + i + "]", depth + 1);
                }
            }
        }
    }

    private static boolean hasCandidateRecords(UnknownFieldSet message) {
        for (UnknownFieldSet record : repeatedMessages(message, FRAME_RECORD_FIELD)) {
            if (isCandidateRecord(record)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCandidateRecord(UnknownFieldSet message) {
        return hasLengthDelimited(message, RECORD_IDS_FIELD) || hasLengthDelimited(message, RECORD_VALUES_FIELD);
    }

    private static boolean hasLengthDelimited(UnknownFieldSet message, int fieldNum) {
        return message != null
                && message.hasField(fieldNum)
                && !message.getField(fieldNum).getLengthDelimitedList().isEmpty();
    }

    private static ByteString firstLengthDelimited(UnknownFieldSet message, int fieldNum) {
        if (message == null || !message.hasField(fieldNum)) {
            return null;
        }
        List<ByteString> blobs = message.getField(fieldNum).getLengthDelimitedList();
        return blobs.isEmpty() ? null : blobs.get(0);
    }

    private static List<Long> firstLengthDelimitedVarints(UnknownFieldSet message, int fieldNum) {
        ByteString blob = firstLengthDelimited(message, fieldNum);
        if (blob == null) {
            return List.of();
        }
        return decodePackedVarints(blob.toByteArray());
    }

    private static List<Long> decodePackedVarints(byte[] bytes) {
        List<Long> values = new ArrayList<>();
        int offset = 0;
        while (offset < bytes.length) {
            long value = 0;
            int shift = 0;
            int start = offset;
            while (offset < bytes.length && shift < 64) {
                int b = bytes[offset++] & 0xFF;
                value |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    values.add(value);
                    break;
                }
                shift += 7;
            }
            if (offset == bytes.length && (bytes[offset - 1] & 0x80) != 0) {
                return List.of();
            }
            if (offset == start || shift >= 64) {
                return List.of();
            }
        }
        return values;
    }

    private static String describeFields(UnknownFieldSet message) {
        List<String> summaries = new ArrayList<>();
        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            summaries.add(entry.getKey() + ":" + describeField(entry.getValue()));
        }
        return String.join(" ", summaries);
    }

    private static String describeField(Field field) {
        return "varint=" + field.getVarintList().size()
                + "|fixed32=" + field.getFixed32List().size()
                + "|fixed64=" + field.getFixed64List().size()
                + "|len=" + field.getLengthDelimitedList().size()
                + "|group=" + field.getGroupList().size();
    }

    private static String fieldNumbers(UnknownFieldSet message) {
        return message.asMap().keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
    }

    private static String scalarPreview(UnknownFieldSet message) {
        List<String> scalars = new ArrayList<>();
        collectScalarPreview(scalars, message, "", 0);
        return String.join(" ", scalars);
    }

    private static void collectScalarPreview(List<String> out, UnknownFieldSet message, String path, int depth) {
        if (message == null || depth > 4 || out.size() >= 80) {
            return;
        }
        for (Map.Entry<Integer, Field> entry : message.asMap().entrySet()) {
            int fieldNum = entry.getKey();
            String fieldPath = path.isEmpty() ? String.valueOf(fieldNum) : path + "." + fieldNum;
            Field field = entry.getValue();
            for (long value : field.getVarintList()) {
                addScalar(out, fieldPath + "=" + value);
            }
            for (int value : field.getFixed32List()) {
                addScalar(out, fieldPath + "=fixed32:" + Integer.toUnsignedLong(value));
            }
            for (long bits : field.getFixed64List()) {
                addScalar(out, fieldPath + "=fixed64:" + Double.longBitsToDouble(bits));
            }
            for (ByteString blob : field.getLengthDelimitedList()) {
                String text = printableText(blob);
                if (!text.isEmpty()) {
                    addScalar(out, fieldPath + "=\"" + text + "\"");
                }
                UnknownFieldSet nested = tryParse(blob);
                if (nested != null) {
                    collectScalarPreview(out, nested, fieldPath, depth + 1);
                }
            }
            for (UnknownFieldSet nested : field.getGroupList()) {
                collectScalarPreview(out, nested, fieldPath, depth + 1);
            }
        }
    }

    private static void addScalar(List<String> out, String value) {
        if (out.size() < 80) {
            out.add(value);
        }
    }

    private static String hexPreview(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(bytes.length, HEX_PREVIEW_BYTES);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        if (bytes.length > count) {
            sb.append(" ...");
        }
        return sb.toString();
    }

    private static String textPreview(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int c = b & 0xFF;
            if (c >= 32 && c < 127) {
                sb.append((char) c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else {
                sb.append('.');
            }
            if (sb.length() >= TEXT_PREVIEW_CHARS) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }

    private static String printableText(ByteString blob) {
        byte[] bytes = blob.toByteArray();
        if (bytes.length == 0 || bytes.length > TEXT_PREVIEW_CHARS) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int c = b & 0xFF;
            if (c < 32 || c >= 127) {
                return "";
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    private static String preview(List<Long> values, int limit) {
        if (values.isEmpty()) {
            return "";
        }
        String body = values.stream()
                .limit(limit)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        if (values.size() > limit) {
            return body + " ...";
        }
        return body;
    }

    private static String pairPreview(List<Long> ids, List<Long> values, int limit) {
        int count = Math.min(Math.min(ids.size(), values.size()), limit);
        List<String> pairs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pairs.add(ids.get(i) + "=" + values.get(i));
        }
        String body = String.join(" ", pairs);
        if (Math.min(ids.size(), values.size()) > limit) {
            return body + " ...";
        }
        return body;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static class MessageAtPath {
        final String path;
        final UnknownFieldSet message;

        MessageAtPath(String path, UnknownFieldSet message) {
            this.path = path;
            this.message = message;
        }
    }
}
