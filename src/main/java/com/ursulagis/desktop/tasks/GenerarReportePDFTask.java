package com.ursulagis.desktop.tasks;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.utils.PDFHelper;

/**
 * Task that writes the labor report PDF (map + histogram images + optional histogram table) and returns the output file.
 */
public class GenerarReportePDFTask extends ProgresibleTask<File> {

	private final File outputFile;
	private final BufferedImage mapImage;
	private final BufferedImage histogramImage;
	private final String laborName;
	private final List<Object[]> histogramTableData;

	public GenerarReportePDFTask(File outputFile, BufferedImage mapImage,
			BufferedImage histogramImage, String laborName, List<Object[]> histogramTableData) {
		this.outputFile = outputFile;
		this.mapImage = mapImage;
		this.histogramImage = histogramImage;
		this.laborName = laborName != null ? laborName : "";
		this.histogramTableData = histogramTableData;
		this.taskName = Messages.getString("GenericLaborGUIController.reportePDFAction");
	}

	@Override
	protected File call() throws Exception {
		updateProgress(0, 2);
		checkCancelled();
		PDFHelper.createLaborReportPDF(outputFile, mapImage, histogramImage, laborName, histogramTableData);
		updateProgress(2, 2);
		return outputFile;
	}
}
