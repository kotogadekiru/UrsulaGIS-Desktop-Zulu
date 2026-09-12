package com.ursulagis.desktop.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Paths for the legacy CNH Voyager 2 Java wrapper.
 * <p>
 * The CN1SDK project is obsolete; binaries are bundled under {@code libs/voyager2/} and
 * shipped inside the Windows installer at {@code app/voyager2/} for compatibility with old machines.
 * Native DLLs are Windows x64 only.
 * <p>
 * Layout:
 * <pre>
 *   voyager2/
 *     sdk/      CNHVoyager2.dll and .NET dependencies
 *     native/   CNHVoyager2JNI.dll, CNHVoyager2Bridge.dll, nethost.dll
 * </pre>
 * Config overrides may point at either the {@code sdk}/{@code native} folder or the
 * {@code voyager2} parent; broken overrides are ignored in favour of the bundled layout.
 */
public class Voyager2Settings {

    public static final String SDK_PATH_KEY = "VOYAGER2_SDK_PATH";
    public static final String LICENSE_KEY_KEY = "VOYAGER2_LICENSE_KEY";
    public static final String NATIVE_LIB_PATH_KEY = "VOYAGER2_NATIVE_LIB_PATH";

    private static final String EMBEDDED_LICENSE_KEY = "1C675C9A-93C4-469A-8248-91E27587733A";
    private static final String BUNDLED_ROOT = "voyager2";
    private static final String SDK_SUBDIR = "sdk";
    private static final String NATIVE_SUBDIR = "native";
    private static final String SDK_DLL = "CNHVoyager2.dll";
    private static final String NATIVE_DLL = "CNHVoyager2JNI.dll";

    private final String sdkBasePath;
    private final String licenseKey;
    private final String nativeLibPath;

    public Voyager2Settings(String sdkBasePath, String licenseKey, String nativeLibPath) {
        this.sdkBasePath = sdkBasePath;
        this.licenseKey = licenseKey;
        this.nativeLibPath = nativeLibPath;
    }

    public static Voyager2Settings fromConfig(Configuracion config) {
        config.loadProperties();
        String sdk = firstUsable(
                normalizeSdkPath(config.getPropertyOrDefault(SDK_PATH_KEY, "")),
                resolveBundledPath(SDK_SUBDIR),
                resolveDevPath(SDK_SUBDIR));
        String natives = firstUsable(
                normalizeNativePath(config.getPropertyOrDefault(NATIVE_LIB_PATH_KEY, "")),
                resolveBundledPath(NATIVE_SUBDIR),
                resolveDevPath(NATIVE_SUBDIR));
        String license = firstNonBlank(
                config.getPropertyOrDefault(LICENSE_KEY_KEY, ""),
                EMBEDDED_LICENSE_KEY);
        return new Voyager2Settings(sdk, license, natives);
    }

    /**
     * True when this JVM can run Voyager 2 import (Windows x64 with SDK + native DLLs available).
     */
    public static boolean isImportSupported(Configuracion config) {
        return unsupportedReason(config) == null;
    }

    /**
     * Human-readable reason import is unavailable, or {@code null} when supported.
     */
    public static String unsupportedReason(Configuracion config) {
        if (!isWindowsOs()) {
            return "Voyager 2 import is only available on Windows.";
        }
        if (!isWindowsX64()) {
            return "Voyager 2 import requires 64-bit Windows.";
        }
        try {
            fromConfig(config).validateForImport();
            return null;
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    public static boolean isWindowsOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("windows");
    }

    public static boolean isWindowsX64() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return "amd64".equals(arch) || "x86_64".equals(arch);
    }

    public String getSdkBasePath() {
        return sdkBasePath;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getNativeLibPath() {
        return nativeLibPath;
    }

    public void validateForImport() throws IllegalStateException {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new IllegalStateException(
                    "Voyager 2 license key is not configured. Set " + LICENSE_KEY_KEY + " in config.properties.");
        }
        if (sdkBasePath == null || sdkBasePath.isBlank()) {
            throw new IllegalStateException(
                    "Voyager 2 SDK path is not configured. Reinstall the Windows build or set "
                            + SDK_PATH_KEY + " in config.properties.");
        }
        File sdk = new File(sdkBasePath);
        if (!sdk.isDirectory()) {
            throw new IllegalStateException("Voyager 2 SDK path does not exist: " + sdkBasePath);
        }
        File dll = new File(sdk, SDK_DLL);
        if (!dll.isFile()) {
            throw new IllegalStateException(SDK_DLL + " not found under: " + sdkBasePath
                    + " (expected .../voyager2/sdk/" + SDK_DLL + ")");
        }
        if (nativeLibPath == null || nativeLibPath.isBlank()) {
            throw new IllegalStateException(
                    "Voyager 2 native library path is not configured. Reinstall the Windows build or set "
                            + NATIVE_LIB_PATH_KEY + " in config.properties.");
        }
        Path nativeDir = Path.of(nativeLibPath);
        if (!Files.isDirectory(nativeDir)) {
            throw new IllegalStateException("Voyager 2 native library folder not found: " + nativeLibPath);
        }
        if (!Files.isRegularFile(nativeDir.resolve(NATIVE_DLL))) {
            throw new IllegalStateException(
                    NATIVE_DLL + " not found in " + nativeLibPath
                            + " (expected .../voyager2/native/" + NATIVE_DLL + ")");
        }
    }

    /**
     * Accepts either the folder that contains {@code CNHVoyager2.dll}, or the parent
     * {@code voyager2} folder (appends {@code sdk/}). Returns {@code null} if unusable.
     */
    static String normalizeSdkPath(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (isUsablePath(path, SDK_SUBDIR)) {
            return path.toString();
        }
        Path asSdkChild = path.resolve(SDK_SUBDIR);
        if (isUsablePath(asSdkChild, SDK_SUBDIR)) {
            return asSdkChild.toString();
        }
        return null;
    }

    /**
     * Accepts either the folder that contains {@code CNHVoyager2JNI.dll}, or the parent
     * {@code voyager2} folder (appends {@code native/}). Returns {@code null} if unusable.
     */
    static String normalizeNativePath(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (isUsablePath(path, NATIVE_SUBDIR)) {
            return path.toString();
        }
        Path asNativeChild = path.resolve(NATIVE_SUBDIR);
        if (isUsablePath(asNativeChild, NATIVE_SUBDIR)) {
            return asNativeChild.toString();
        }
        return null;
    }

    private static String resolveBundledPath(String subdir) {
        for (Path installRoot : candidateInstallRoots()) {
            if (installRoot == null) {
                continue;
            }
            // Standard jpackage layout: <install>/app/voyager2/{sdk|native}
            Path underApp = installRoot.resolve("app").resolve(BUNDLED_ROOT).resolve(subdir)
                    .toAbsolutePath()
                    .normalize();
            if (isUsablePath(underApp, subdir)) {
                return underApp.toString();
            }
            // If the root is already the app/ folder
            Path direct = installRoot.resolve(BUNDLED_ROOT).resolve(subdir)
                    .toAbsolutePath()
                    .normalize();
            if (isUsablePath(direct, subdir)) {
                return direct.toString();
            }
            // Misconfigured install root pointing at voyager2 itself
            Path asChild = installRoot.resolve(subdir).toAbsolutePath().normalize();
            if (isUsablePath(asChild, subdir)) {
                return asChild.toString();
            }
        }
        return null;
    }

    /**
     * Resolves the jpackage install directory. {@code jpackage.app-path} is the launcher
     * executable (e.g. {@code ...\UrsulaGIS.exe}), not the {@code app/} folder.
     */
    private static Path[] candidateInstallRoots() {
        ArrayList<Path> roots = new ArrayList<>(2);
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path p = Path.of(appPath);
            if (Files.isRegularFile(p) || looksLikeExecutable(p)) {
                if (p.getParent() != null) {
                    roots.add(p.getParent());
                }
            } else if (Files.isDirectory(p)) {
                roots.add(p);
            } else if (p.getParent() != null) {
                roots.add(p.getParent());
            }
        }
        // Windows/Linux jpackage: java.home is <install>/runtime → parent is install root
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            Path runtime = Path.of(javaHome);
            if (runtime.getParent() != null) {
                roots.add(runtime.getParent());
            }
        }
        return roots.toArray(Path[]::new);
    }

    private static boolean looksLikeExecutable(Path p) {
        String name = p.getFileName() != null ? p.getFileName().toString().toLowerCase() : "";
        return name.endsWith(".exe") || name.endsWith(".bat") || !name.contains(".");
    }

    private static String resolveDevPath(String subdir) {
        Path candidate = Path.of(System.getProperty("user.dir"), "libs", BUNDLED_ROOT, subdir)
                .toAbsolutePath()
                .normalize();
        return isUsablePath(candidate, subdir) ? candidate.toString() : null;
    }

    private static boolean isUsablePath(Path dir, String subdir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        if (SDK_SUBDIR.equals(subdir)) {
            return Files.isRegularFile(dir.resolve(SDK_DLL));
        }
        if (NATIVE_SUBDIR.equals(subdir)) {
            return Files.isRegularFile(dir.resolve(NATIVE_DLL));
        }
        return false;
    }

    private static String firstUsable(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        return firstUsable(values);
    }
}
