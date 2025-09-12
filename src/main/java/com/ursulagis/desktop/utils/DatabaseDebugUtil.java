package com.ursulagis.desktop.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Utility class for showing database debug information in GUI dialogs
 */
public class DatabaseDebugUtil {
    private static boolean isDebugMode = false;
    private static StringBuilder debugLog = new StringBuilder();
    private static Alert alert = null;
    
    /**
     * Shows a debug alert dialog for database troubleshooting
     */
    public static void showDebugAlert(String title, String message) {
        
        try {
            // Check if we're on the JavaFX application thread
            if (Platform.isFxApplicationThread()) {
                showAlert(title, message);
            } else {
                // If not on FX thread, run on FX thread
                Platform.runLater(() -> showAlert(title, message));
            }
        } catch (Exception e) {
            // Fallback to console if GUI fails
            System.out.println("DEBUG ALERT [" + title + "]: " + message);
            e.printStackTrace();
        }
    }
    
    private static void showAlert(String title, String message) {
        System.out.println("DEBUG ALERT [" + title + "]: " + message);
        debugLog.append(title).append(": ").append(message).append("\n");
        if (alert == null) {
            alert = new Alert(AlertType.INFORMATION);
        }
        alert.setTitle("DB Debug: " + title);
        alert.setHeaderText(null);
        alert.setContentText(debugLog.toString());
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(600, 400);
        if(!alert.isShowing()) {
            alert.show();
        }
        
    }
}
