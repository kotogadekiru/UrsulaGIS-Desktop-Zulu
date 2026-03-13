package com.ursulagis.desktop.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.ursulagis.desktop.gui.JFXMain;
import javafx.application.Application;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/*
 * <dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.4</version>
</dependency>
 */
/**
 * 
 * @author quero
 *
 */
public class PDFHelper {
	public static void main(String[] args) {
		//testInsertText();
		
		//testInsertImage();
		
		Application.launch(PDFHelperAPP.class, args);

	}
	
	public static void testInsertText() {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);

		PDPageContentStream contentStream;
		try {
			contentStream = new PDPageContentStream(document, page);

			//contentStream.setFont(PDType1Font.COURIER, 12);
			contentStream.beginText();
			contentStream.showText("Hola Ursula");
			contentStream.endText();
			contentStream.close();

			document.save("pdfBoxHelloWorld.pdf");
			document.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	public static void testInsertImage() {
		PDDocument document2 = new PDDocument();
		PDPage page2 = new PDPage();
		document2.addPage(page2);

		Path path2;
		try {
			path2 = Paths.get(ClassLoader.getSystemResource("./gui/ursula_logo_2020.png").toURI());

			PDPageContentStream contentStream2 = new PDPageContentStream(document2, page2);
			PDImageXObject image = PDImageXObject.createFromFile(
					path2.toAbsolutePath().toString(),
					document2);
			contentStream2.drawImage(image, 0, 0);
			contentStream2.close();

			document2.save("pdfBoxImage.pdf");
			document2.close();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a PDF report with the map image (centered on labor), the labor histogram, and optional histogram table.
	 * @param outputFile destination PDF file
	 * @param mapImage screenshot of the map view
	 * @param histogramImage screenshot of the labor histogram chart
	 * @param laborName title for the report
	 * @param histogramTableData optional table data (first row = header, e.g. "Rango","Superficie","Cantidad"); null or empty = no table
	 */
	public static void createLaborReportPDF(java.io.File outputFile,
			BufferedImage mapImage, BufferedImage histogramImage, String laborName,
			List<Object[]> histogramTableData) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			final float margin = 40f;
			final float pageWidth = PDRectangle.A4.getWidth();
			final float pageHeight = PDRectangle.A4.getHeight();
			final float contentWidth = pageWidth - 2 * margin;

			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				float yPos = pageHeight - margin;
				final float headerHeight = 28f;
				final float titleFontSize = 14f;
				PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

				// Header: band with UrsulaGIS icon (top-right in header)
				float headerYBottom = yPos - headerHeight;
				float iconHeight = 22f;
				float iconY = headerYBottom + (headerHeight - iconHeight) / 2f;
				InputStream logoStream = null;
				logoStream = JFXMain.class.getResourceAsStream("U_nueva_3_256x256_verde.png");
				if (logoStream == null) {
					logoStream = JFXMain.class.getResourceAsStream("ursula_logo_2020.png");
				}
				if (logoStream == null) {
					logoStream = PDFHelper.class.getClassLoader().getResourceAsStream("com/ursulagis/desktop/gui/U_nueva_3_256x256_verde.png");
				}
				if (logoStream == null) {
					logoStream = PDFHelper.class.getClassLoader().getResourceAsStream("gui/ursula_logo_2020.png");
				}
				if (logoStream != null) {
					try {
						BufferedImage logoImg = ImageIO.read(logoStream);
						if (logoImg != null && logoImg.getWidth() > 0 && logoImg.getHeight() > 0) {
							float iconWidth = iconHeight * logoImg.getWidth() / logoImg.getHeight();
							// place icon flush to the right inside content area
							float iconX = margin + contentWidth - iconWidth;
							PDImageXObject logoPdf = LosslessFactory.createFromImage(document, logoImg);
							contentStream.drawImage(logoPdf, iconX, iconY, iconWidth, iconHeight);
						}
					} catch (IOException ignored) {
						// skip icon
					} finally {
						try {
							logoStream.close();
						} catch (IOException ignored) {
						}
					}
				}
				// Line under header
				contentStream.moveTo(margin, headerYBottom);
				contentStream.lineTo(margin + contentWidth, headerYBottom);
				contentStream.stroke();
				yPos = headerYBottom - 14f;

				// Title (labor name) below header
				String title = laborName != null ? laborName : "Reporte de labor";
				if (title.length() > 80) {
					title = title.substring(0, 80);
				}
				contentStream.beginText();
				contentStream.setFont(titleFont, titleFontSize);
				contentStream.newLineAtOffset(margin, yPos);
				contentStream.showText(title);
				contentStream.endText();
				yPos -= 16f;

				// Map image (scale to fit width, center)
				if (mapImage != null && mapImage.getWidth() > 0 && mapImage.getHeight() > 0) {
					float mapHeight = contentWidth * mapImage.getHeight() / mapImage.getWidth();
					float maxMapHeight = (pageHeight - 2 * margin - 24 - 120) / 2f;
					if (mapHeight > maxMapHeight) {
						mapHeight = maxMapHeight;
					}
					PDImageXObject mapPdfImage = LosslessFactory.createFromImage(document, mapImage);
					contentStream.drawImage(mapPdfImage, margin, yPos - mapHeight, contentWidth, mapHeight);
					yPos -= mapHeight + 16;
				}

				// Reserve space for table below histogram when present
				boolean hasTable = histogramTableData != null && !histogramTableData.isEmpty();
				final float tableRowHeight = 11f;
				final float tableFontSize = 7f;
				float tableHeight = hasTable ? (histogramTableData.size() * tableRowHeight) + 8f : 0f;

				// Histogram image (scale to fit width)
				if (histogramImage != null && histogramImage.getWidth() > 0 && histogramImage.getHeight() > 0) {
					float histHeight = contentWidth * histogramImage.getHeight() / histogramImage.getWidth();
					float maxHistHeight = yPos - margin - tableHeight;
					if (histHeight > maxHistHeight) {
						histHeight = maxHistHeight;
					}
					PDImageXObject histPdfImage = LosslessFactory.createFromImage(document, histogramImage);
					contentStream.drawImage(histPdfImage, margin, yPos - histHeight, contentWidth, histHeight);
					yPos -= histHeight + 8f;
				}

				// Histogram table (same data as Excel export)
				if (hasTable) {
					int cols = 3;
					float col0 = margin;
					float col1 = margin + contentWidth * 0.45f;
					float col2 = margin + contentWidth * 0.70f;
					float colEnd = margin + contentWidth;
					PDType1Font tableFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
					PDType1Font tableHeaderFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
					float cellPadding = 2f;
					// Top border of table
					contentStream.moveTo(col0, yPos);
					contentStream.lineTo(colEnd, yPos);
					contentStream.stroke();
					for (int r = 0; r < histogramTableData.size(); r++) {
						Object[] row = histogramTableData.get(r);
						float rowY = yPos - (r * tableRowHeight);
						float rowYBottom = rowY - tableRowHeight;
						// Grid lines
						contentStream.moveTo(col0, rowYBottom);
						contentStream.lineTo(colEnd, rowYBottom);
						contentStream.moveTo(col1, rowY);
						contentStream.lineTo(col1, rowYBottom);
						contentStream.moveTo(col2, rowY);
						contentStream.lineTo(col2, rowYBottom);
						contentStream.stroke();
						// Cell text
						boolean isHeader = (r == 0);
						contentStream.setFont(isHeader ? tableHeaderFont : tableFont, tableFontSize);
						for (int c = 0; c < Math.min(cols, row.length); c++) {
							String cell = row[c] != null ? row[c].toString() : "";
							if (cell.length() > 35) {
								cell = cell.substring(0, 32) + "...";
							}
							float x = c == 0 ? col0 + cellPadding : (c == 1 ? col1 + cellPadding : col2 + cellPadding);
							contentStream.beginText();
							contentStream.newLineAtOffset(x, rowYBottom + cellPadding);
							contentStream.showText(cell);
							contentStream.endText();
						}
					}
					contentStream.moveTo(col0, yPos - histogramTableData.size() * tableRowHeight);
					contentStream.lineTo(colEnd, yPos - histogramTableData.size() * tableRowHeight);
					contentStream.stroke();
				}
			}
			document.save(outputFile);
		}
	}

	public static void printToPDF(Node yourNode,Stage stage) {
		PrinterJob job = PrinterJob.createPrinterJob();
		 if(job != null){
		
		
		   job.showPrintDialog(stage); // Window must be your main Stage
		   job.printPage(yourNode);
		   job.endJob();
		 }
	}
	class PDFHelperAPP extends Application{
		public PDFHelperAPP() {
			super();
		}
		@Override
		public void start(Stage primaryStage) throws Exception {
			 Node node = new Circle(100, 200, 200);
			 PDFHelper.printToPDF(node,primaryStage);
		}
		
	}
}
