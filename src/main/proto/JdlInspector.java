package com.ursulagis.desktop.dao.utils;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JdlInspector {

    public static void main(String[] args) {
        // Replace with your file path
		String [] filePaths = {"D:/Drive-Kotogadekiru/LOS JAGUELES/2526/soja/jag 21/cosecha/1J0S790BLP0145110_04232026/JD-Data/log/2026/5fd8b3ef-af8f-4b74-9119-321514a92513.jdl",
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

		for (String filePath : filePaths) {
			System.out.println("Processing file: " + filePath);
			try {
				byte[] header = readFileHeader(filePath, 16);
				System.out.println("First 16 bytes (hex): " + bytesToHex(header));
				System.out.println("First 16 bytes (ASCII): " + bytesToAscii(header));

				String fileType = detectType(header);
				System.out.println("Detected type: " + fileType);

				if ("ZIP".equals(fileType)) {
					System.out.println("/n--- Attempting to extract ZIP contents ---");
					extractZip(filePath, "extracted_here/");
				} else {
					System.out.println("/nFile does not appear to be ZIP or SQLite.");
					System.out.println("Look for readable strings in the file for clues.");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }

    /** Reads the first 'maxBytes' bytes of a file. */
    private static byte[] readFileHeader(String path, int maxBytes) throws IOException {
        byte[] header = new byte[maxBytes];
        try (InputStream is = new FileInputStream(path)) {
            int bytesRead = is.read(header);
            if (bytesRead < maxBytes) {
                byte[] actual = new byte[bytesRead];
                System.arraycopy(header, 0, actual, 0, bytesRead);
                return actual;
            }
        }
        return header;
    }

    /** Converts bytes to a hex string for inspection. */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /** Converts bytes to a readable ASCII string (dots for non-printable). */
    private static String bytesToAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    /** Simple magic byte detection. */
    private static String detectType(byte[] header) {
        if (header.length < 4) return "Too small";

        // ZIP signature: PK..
        if (header[0] == 0x50 && header[1] == 0x4B) {
            return "ZIP";
        }
        // SQLite signature
        if (new String(header, 0, Math.min(header.length, 16)).startsWith("SQLite format 3")) {
            return "SQLite";
        }
        // GZIP signature: 1F 8B
        if (header[0] == (byte)0x1F && header[1] == (byte)0x8B) {
            return "GZIP";
        }
        return "Unknown";
    }

    /** Extracts all entries from a ZIP file to a target directory. */
    private static void extractZip(String zipPath, String destDir) throws IOException {
        Path dest = Paths.get(destDir);
        Files.createDirectories(dest);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = dest.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        zis.transferTo(os);
                    }
                    System.out.println("Extracted: " + entry.getName());
                }
                zis.closeEntry();
            }
        }
    }
}
