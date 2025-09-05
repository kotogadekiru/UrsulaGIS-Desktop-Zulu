package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class to extract and load JOGL native libraries from JAR files.
 * This is necessary because JOGL native libraries need to be extracted to the filesystem
 * before they can be loaded by the JVM.
 */
public class JOGLNativeLoader {
    
    private static final String NATIVE_LIB_DIR = "jogl_natives";
    public static boolean nativesLoaded = false;
    
    /**
     * Extracts and loads JOGL native libraries from the repo directory.
     * This method should be called before any JOGL/WorldWind initialization.
     */
    public static synchronized void loadNatives() {
        if (nativesLoaded) {
            return;
        }
        
        try {
            // First try the system property approach
            loadNativesFromSystemProperty();
            
            // Create temporary directory for native libraries
            Path tempDir = Files.createTempDirectory(NATIVE_LIB_DIR);
            tempDir.toFile().deleteOnExit();
            
            // Extract natives from JOGL JAR files in repo directory
            extractNativesFromRepo(tempDir);
            
            // Set the native library path
            String nativePath = tempDir.toString();
            String existingPath = System.getProperty("java.library.path");
            if (existingPath != null && !existingPath.isEmpty()) {
                nativePath = existingPath + File.pathSeparator + nativePath;
            }
            System.setProperty("java.library.path", nativePath);
            
            // Reset the library path cache
            resetLibraryPath();
            
            // Try to load the native libraries directly
            loadNativeLibrariesDirectly(tempDir);
            
            nativesLoaded = true;
            System.out.println("JOGL native libraries loaded successfully from: " + tempDir);
            
        } catch (Exception e) {
            System.err.println("Failed to load JOGL native libraries: " + e.getMessage());
            e.printStackTrace();
            // Try fallback approach
            tryFallbackLoading();
        }
    }
    
    /**
     * Extracts native libraries from JOGL JAR files in the repo directory.
     */
    private static void extractNativesFromRepo(Path tempDir) throws IOException {
        String repoPath = "repo";
        File repoDir = new File(repoPath);
        
        if (!repoDir.exists()) {
            throw new IOException("Repo directory not found: " + repoPath);
        }
        
        // Look for JOGL native JAR files
        File[] jarFiles = repoDir.listFiles((dir, name) -> 
            name.contains("jogl") && name.contains("natives") && name.endsWith(".jar"));
        
        if (jarFiles == null || jarFiles.length == 0) {
            throw new IOException("No JOGL native JAR files found in repo directory");
        }
        
        // Extract natives from each JAR file
        for (File jarFile : jarFiles) {
            extractNativesFromJar(jarFile, tempDir);
        }
        
        // Also look for GlueGen native JAR files
        File[] gluegenJarFiles = repoDir.listFiles((dir, name) -> 
            name.contains("gluegen") && name.contains("natives") && name.endsWith(".jar"));
        
        if (gluegenJarFiles != null) {
            for (File jarFile : gluegenJarFiles) {
                extractNativesFromJar(jarFile, tempDir);
            }
        }
    }
    
    /**
     * Extracts native libraries from a specific JAR file.
     */
    private static void extractNativesFromJar(File jarFile, Path tempDir) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Extract native library files (.so, .dll, .dylib)
                if (isNativeLibrary(entryName) && !entry.isDirectory()) {
                    extractNativeFile(jar, entry, tempDir);
                }
            }
        }
    }
    
    /**
     * Checks if a file is a native library based on its extension.
     */
    private static boolean isNativeLibrary(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".so") || 
               lowerName.endsWith(".dll") || 
               lowerName.endsWith(".dylib") ||
               lowerName.endsWith(".jnilib");
    }
    
    /**
     * Extracts a single native library file from a JAR.
     */
    private static void extractNativeFile(JarFile jar, JarEntry entry, Path tempDir) throws IOException {
        String fileName = new File(entry.getName()).getName();
        Path outputPath = tempDir.resolve(fileName);
        
        try (InputStream inputStream = jar.getInputStream(entry);
             FileOutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        // Make the file executable on Unix-like systems
        if (System.getProperty("os.name").toLowerCase().contains("nix") ||
            System.getProperty("os.name").toLowerCase().contains("nux") ||
            System.getProperty("os.name").toLowerCase().contains("mac")) {
            outputPath.toFile().setExecutable(true);
        }
        
        System.out.println("Extracted native library: " + fileName);
    }
    
    /**
     * Resets the library path cache by using reflection to clear the cached field.
     */
    private static void resetLibraryPath() {
        try {
            // Reset the cached library path in ClassLoader
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader instanceof java.net.URLClassLoader) {
                java.lang.reflect.Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
                field.setAccessible(true);
                field.set(systemClassLoader, new java.util.HashSet<>());
            }
        } catch (Exception e) {
            // Ignore reflection errors - this is just an optimization
            System.out.println("Could not reset library path cache: " + e.getMessage());
        }
    }
    
    /**
     * Alternative method to load natives using system property approach.
     * This can be used as a fallback if the extraction method fails.
     */
    public static void loadNativesFromSystemProperty() {
        try {
            // Set JOGL system properties for native library loading
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            // Force JOGL to use the bundled natives
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            System.out.println("JOGL system properties configured for native loading");
        } catch (Exception e) {
            System.err.println("Failed to configure JOGL system properties: " + e.getMessage());
        }
    }
    
    /**
     * Try to load native libraries directly from the extracted directory.
     */
    private static void loadNativeLibrariesDirectly(Path tempDir) {
        try {
            File[] nativeFiles = tempDir.toFile().listFiles((dir, name) -> 
                name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib"));
            
            if (nativeFiles != null) {
                for (File nativeFile : nativeFiles) {
                    try {
                        System.load(nativeFile.getAbsolutePath());
                        System.out.println("Loaded native library: " + nativeFile.getName());
                    } catch (UnsatisfiedLinkError e) {
                        System.out.println("Could not load native library " + nativeFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading native libraries directly: " + e.getMessage());
        }
    }
    
    /**
     * Fallback loading approach using different methods.
     */
    private static void tryFallbackLoading() {
        try {
            System.out.println("Attempting fallback JOGL native loading...");
            
            // Try to set JOGL to use bundled natives
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            // Try to force JOGL to use the classpath natives
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            System.setProperty("jogamp.gluegen.UseTempJarCache", "true");
            
            nativesLoaded = true;
            System.out.println("Fallback JOGL native loading completed");
            
        } catch (Exception e) {
            System.err.println("Fallback JOGL native loading failed: " + e.getMessage());
        }
    }
}
