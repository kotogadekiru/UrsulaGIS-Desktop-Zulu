package com.ursulagis.desktop.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Voyager2SettingsTest {

    @TempDir
    Path temp;

    @Test
    void normalizeSdkPath_acceptsSdkFolderDirectly() throws Exception {
        Path sdk = temp.resolve("voyager2").resolve("sdk");
        Files.createDirectories(sdk);
        Files.writeString(sdk.resolve("CNHVoyager2.dll"), "x");

        String resolved = Voyager2Settings.normalizeSdkPath(sdk.toString());
        assertEquals(sdk.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    void normalizeSdkPath_acceptsVoyager2ParentFolder() throws Exception {
        Path root = temp.resolve("voyager2");
        Path sdk = root.resolve("sdk");
        Files.createDirectories(sdk);
        Files.writeString(sdk.resolve("CNHVoyager2.dll"), "x");

        String resolved = Voyager2Settings.normalizeSdkPath(root.toString());
        assertEquals(sdk.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    void normalizeSdkPath_rejectsMissingDll() throws Exception {
        Path root = temp.resolve("voyager2");
        Files.createDirectories(root.resolve("sdk"));

        assertNull(Voyager2Settings.normalizeSdkPath(root.toString()));
        assertNull(Voyager2Settings.normalizeSdkPath(""));
        assertNull(Voyager2Settings.normalizeSdkPath(null));
    }

    @Test
    void normalizeNativePath_acceptsNativeFolderAndParent() throws Exception {
        Path root = temp.resolve("voyager2");
        Path nativeDir = root.resolve("native");
        Files.createDirectories(nativeDir);
        Files.writeString(nativeDir.resolve("CNHVoyager2JNI.dll"), "x");

        assertEquals(
                nativeDir.toAbsolutePath().normalize().toString(),
                Voyager2Settings.normalizeNativePath(nativeDir.toString()));
        assertEquals(
                nativeDir.toAbsolutePath().normalize().toString(),
                Voyager2Settings.normalizeNativePath(root.toString()));
    }

    @Test
    void validateForImport_succeedsWithNormalizedLayout() throws Exception {
        Path root = temp.resolve("voyager2");
        Path sdk = root.resolve("sdk");
        Path nativeDir = root.resolve("native");
        Files.createDirectories(sdk);
        Files.createDirectories(nativeDir);
        Files.writeString(sdk.resolve("CNHVoyager2.dll"), "x");
        Files.writeString(nativeDir.resolve("CNHVoyager2JNI.dll"), "x");

        String sdkPath = Voyager2Settings.normalizeSdkPath(root.toString());
        String nativePath = Voyager2Settings.normalizeNativePath(root.toString());
        Voyager2Settings settings = new Voyager2Settings(sdkPath, "license", nativePath);
        settings.validateForImport();
        assertTrue(sdkPath.endsWith("sdk") || sdkPath.endsWith("sdk" + java.io.File.separator));
    }
}
