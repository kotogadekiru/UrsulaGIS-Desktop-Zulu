package com.ursulagis.desktop.gui.nww;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import javafx.stage.Screen;

/**
 * Helper class to detect high-DPI (Retina) displays.
 * Detects DPI scaling on both JavaFX and AWT/Swing components.
 */
public class HiDPIHelper {
	private static Boolean isHiDPI = null;
	
	/**
	 * Detects if the system is running on a high-DPI display.
	 * Checks both JavaFX Screen DPI and AWT GraphicsConfiguration scale factors.
	 * 
	 * @return true if running on a high-DPI display (scale factor > 1.0), false otherwise
	 */
	public static boolean isHiDPI() {
		if (isHiDPI == null) {
			boolean detected = false;
			
			// Method 1: Check JavaFX Screen DPI
			try {
				Screen primaryScreen = Screen.getPrimary();
				if (primaryScreen != null) {
					double dpi = primaryScreen.getDpi();
					// High-DPI displays typically have DPI > 96 (standard) or > 120
					if (dpi > 120.0) {
						detected = true;
					}
				}
			} catch (Exception e) {
				// JavaFX Screen API might not be available, continue with AWT check
			}
			
			// Method 2: Check AWT GraphicsConfiguration scale transform
			// This is the most reliable method for Swing/AWT components
			if (!detected) {
				try {
					GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
					GraphicsDevice gd = ge.getDefaultScreenDevice();
					GraphicsConfiguration gc = gd.getDefaultConfiguration();
					
					if (gc != null) {
						// Get the default transform which contains the scale factor
						double scaleX = gc.getDefaultTransform().getScaleX();
						double scaleY = gc.getDefaultTransform().getScaleY();
						
						// High-DPI displays have scale factors > 1.0 (typically 1.5, 2.0, etc.)
						if (scaleX > 1.0 || scaleY > 1.0) {
							detected = true;
						}
					}
				} catch (Exception e) {
					// Fallback: check system property
					try {
						String dpiScale = System.getProperty("sun.java2d.uiScale");
						if (dpiScale != null && !dpiScale.equals("1.0")) {
							detected = true;
						}
					} catch (Exception ex) {
						// If all methods fail, assume not high-DPI
					}
				}
			}
			
			isHiDPI = detected;
		}
		
		return isHiDPI;
	}
}
