package com.ursulagis.desktop.utils;

/**
 * Simple JOGL native loader that uses system properties to enable JOGL
 * to find and load native libraries from JAR files in the classpath.
 */
public class SimpleJOGLNativeLoader {
    
    private static boolean initialized = false;
    
    /**
     * Initialize JOGL native libraries using system properties.
     * This is a simpler approach that relies on JOGL's built-in native loading.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            System.out.println("Initializing JOGL native libraries...");
            
            // Set JOGL system properties to enable native library loading from JARs
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            // Try using the more complex loader first
            JOGLNativeLoader.loadNatives();
            
            // Test JOGL loading
            JOGLTest.testJOGLLoading();
            
            initialized = true;
            System.out.println("JOGL native libraries initialized successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize JOGL native libraries: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if JOGL natives have been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}