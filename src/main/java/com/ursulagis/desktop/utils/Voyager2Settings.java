package com.ursulagis.desktop.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ursulagis.desktop.dao.config.Configuracion;

/**
 * Paths and license for the CNH Voyager 2 Java wrapper (see JavaWrapper in CN1SDK).
 */
public class Voyager2Settings {

    public static final String SDK_PATH_KEY = "VOYAGER2_SDK_PATH";
    public static final String LICENSE_KEY_KEY = "VOYAGER2_LICENSE_KEY";
    public static final String NATIVE_LIB_PATH_KEY = "VOYAGER2_NATIVE_LIB_PATH";

    private static final String DEFAULT_SDK_PATH =
            "D:\\worskpaces\\CN1SDK_4.1.2\\Voyager2SampleApp\\Voyager2SampleApp\\bin\\Release\\net8.0-windows7.0";
    private static final String DEFAULT_NATIVE_LIB_PATH =
            "D:\\worskpaces\\CN1SDK_4.1.2\\Voyager2SampleApp\\Voyager2SampleApp\\JavaWrapper\\build\\Release";
            //VOYAGER2_NATIVE_LIB_PATH=D:\worskpaces\CN1SDK_4.1.2\Voyager2SampleApp\Voyager2SampleApp\JavaWrapper\build\Release            

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
        String sdk = config.getPropertyOrDefault(SDK_PATH_KEY, DEFAULT_SDK_PATH);
        String license = config.getPropertyOrDefault(LICENSE_KEY_KEY, "1C675C9A-93C4-469A-8248-91E27587733A");
        String natives = config.getPropertyOrDefault(NATIVE_LIB_PATH_KEY, DEFAULT_NATIVE_LIB_PATH);
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
        File sdk = new File(sdkBasePath);
        if (!sdk.isDirectory()) {
            throw new IllegalStateException("Voyager 2 SDK path does not exist: " + sdkBasePath);
        }
        File dll = new File(sdk, "CNHVoyager2.dll");
        if (!dll.isFile()) {
            throw new IllegalStateException("CNHVoyager2.dll not found under: " + sdkBasePath);
        }
        Path nativeDir = Path.of(nativeLibPath);
        if (!Files.isDirectory(nativeDir)) {
            throw new IllegalStateException("Voyager 2 native library folder not found: " + nativeLibPath);
        }
        if (!Files.isRegularFile(nativeDir.resolve("CNHVoyager2JNI.dll"))) {
            throw new IllegalStateException(
                    "CNHVoyager2JNI.dll not found in " + nativeLibPath + ". Run JavaWrapper\\build.bat.");
        }
    }
}
