package com.ursulagis.desktop.gui;

/**
 * Optional values to pre-fill {@link SiembraConfigDialogController} (chat workflow).
 */
public record SiembraConfigPrefill(String seedNameHint, double rowSpacingM) {

	public SiembraConfigPrefill {
		if (rowSpacingM <= 0) {
			rowSpacingM = 0.19;
		}
	}
}
