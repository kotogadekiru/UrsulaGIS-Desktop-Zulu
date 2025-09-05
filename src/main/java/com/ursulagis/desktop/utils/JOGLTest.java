package com.ursulagis.desktop.utils;

/**
 * Simple test to verify JOGL can be loaded.
 */
public class JOGLTest {
    
    public static void testJOGLLoading() {
        try {
            System.out.println("Testing JOGL loading...");
            
            // Set JOGL system properties
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            // Try to load JOGL classes
            Class.forName("com.jogamp.common.os.Platform");
            System.out.println("✓ Platform class loaded successfully");
            
            Class.forName("com.jogamp.opengl.GLProfile");
            System.out.println("✓ GLProfile class loaded successfully");
            
            Class.forName("gov.nasa.worldwind.Configuration");
            System.out.println("✓ WorldWind Configuration class loaded successfully");
            
            System.out.println("JOGL test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("JOGL test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
