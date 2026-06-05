package com.ursulagis.desktop.utils;

import java.nio.file.Files;
import java.nio.file.Path;

import com.cnh.voyager2.jni.CNHVoyager2Native;

/**
 * Loads CNHVoyager2 JNI DLLs before the Voyager SDK is used.
 */
public final class Voyager2NativeLoader {

    private static volatile boolean loaded;

    private Voyager2NativeLoader() {
    }

    public static void ensureLoaded(Voyager2Settings settings) {
        if (loaded) {
            return;
        }
        synchronized (Voyager2NativeLoader.class) {
            if (loaded) {
                return;
            }
            String nativeLibPath = settings.getNativeLibPath();
            if (nativeLibPath == null || nativeLibPath.isBlank()) {
                throw new IllegalStateException(
                        "Voyager 2 native library path is not configured. Set "
                                + Voyager2Settings.NATIVE_LIB_PATH_KEY + " in config.properties.");
            }
            Path dir = Path.of(nativeLibPath).toAbsolutePath().normalize();
            if (!Files.isDirectory(dir)) {
                throw new IllegalStateException("Voyager 2 native library folder not found: " + dir);
            }
            if (!Files.isRegularFile(dir.resolve("CNHVoyager2JNI.dll"))) {
                throw new IllegalStateException(
                        "CNHVoyager2JNI.dll not found in " + dir + ". Run JavaWrapper\\build.bat.");
            }

            String dirPath = dir.toString();
            System.setProperty(CNHVoyager2Native.NATIVE_DIR_PROPERTY, dirPath);
            CNHVoyager2Native.ensureNativeLibrariesLoaded(dirPath);
            loaded = true;
        }
    }
}
