package com.ursulagis.desktop.utils;



import java.io.File;

import java.nio.file.Files;

import java.nio.file.Path;



import com.ursulagis.desktop.dao.config.Configuracion;



/**

 * Paths for the legacy CNH Voyager 2 Java wrapper.

 * <p>

 * The CN1SDK project is obsolete; binaries are bundled under {@code libs/voyager2/} and

 * shipped inside the Windows installer at {@code app/voyager2/} for compatibility with old machines.

 */

public class Voyager2Settings {



    public static final String SDK_PATH_KEY = "VOYAGER2_SDK_PATH";

    public static final String LICENSE_KEY_KEY = "VOYAGER2_LICENSE_KEY";

    public static final String NATIVE_LIB_PATH_KEY = "VOYAGER2_NATIVE_LIB_PATH";



    private static final String EMBEDDED_LICENSE_KEY = "1C675C9A-93C4-469A-8248-91E27587733A";

    private static final String BUNDLED_ROOT = "voyager2";

    private static final String SDK_SUBDIR = "sdk";

    private static final String NATIVE_SUBDIR = "native";



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

        String sdk = firstNonBlank(

                config.getPropertyOrDefault(SDK_PATH_KEY, ""),

                resolveBundledPath(SDK_SUBDIR),

                resolveDevPath(SDK_SUBDIR));

        String natives = firstNonBlank(

                config.getPropertyOrDefault(NATIVE_LIB_PATH_KEY, ""),

                resolveBundledPath(NATIVE_SUBDIR),

                resolveDevPath(NATIVE_SUBDIR));

        String license = firstNonBlank(

                config.getPropertyOrDefault(LICENSE_KEY_KEY, ""),

                EMBEDDED_LICENSE_KEY);

        return new Voyager2Settings(sdk, license, natives);

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

        File dll = new File(sdk, "CNHVoyager2.dll");

        if (!dll.isFile()) {

            throw new IllegalStateException("CNHVoyager2.dll not found under: " + sdkBasePath);

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

        if (!Files.isRegularFile(nativeDir.resolve("CNHVoyager2JNI.dll"))) {

            throw new IllegalStateException(

                    "CNHVoyager2JNI.dll not found in " + nativeLibPath);

        }

    }



    private static String resolveBundledPath(String subdir) {

        String appPath = System.getProperty("jpackage.app-path");

        if (appPath == null || appPath.isBlank()) {

            return null;

        }

        Path candidate = Path.of(appPath, "app", BUNDLED_ROOT, subdir).toAbsolutePath().normalize();

        return isUsablePath(candidate, subdir) ? candidate.toString() : null;

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

            return Files.isRegularFile(dir.resolve("CNHVoyager2.dll"));

        }

        if (NATIVE_SUBDIR.equals(subdir)) {

            return Files.isRegularFile(dir.resolve("CNHVoyager2JNI.dll"));

        }

        return false;

    }



    private static String firstNonBlank(String... values) {

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

}

