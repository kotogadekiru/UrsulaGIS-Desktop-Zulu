package com.ursulagis.desktop.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwindx.examples.kml.KMZDocumentBuilder;

/**
 * Exports one or more {@link Poligono} instances to a KMZ file for Google Earth.
 */
public class ExportPoligonoToKMZ {
	private static final Logger logger = Logger.getLogger(ExportPoligonoToKMZ.class.getName());

	private ExportPoligonoToKMZ() {
	}

	public static File export(Poligono poligono) {
		if (poligono == null) {
			return null;
		}
		return export(Collections.singletonList(poligono));
	}

	/**
	 * Prompts for a destination file and writes all polygons into a single KMZ.
	 *
	 * @return the written file, or {@code null} if the user cancelled or export failed
	 */
	public static File export(List<Poligono> poligonos) {
		if (poligonos == null || poligonos.isEmpty()) {
			return null;
		}

		String defaultName = poligonos.size() == 1
				? safeFileName(poligonos.get(0).getNombre()) + ".kmz"
				: "poligonos.kmz";
		File selected = FileHelper.getNewFile(defaultName, "*.kmz");
		if (selected == null) {
			return null;
		}
		if (!selected.getName().toLowerCase().endsWith(".kmz")) {
			selected = new File(selected.getParentFile(), selected.getName() + ".kmz");
		}

		try (OutputStream outStream = new FileOutputStream(selected)) {
			KMZDocumentBuilder kmzBuilder = new KMZDocumentBuilder(outStream);
			ShapeAttributes attrs = createDefaultAttributes();
			int written = 0;
			for (Poligono poli : poligonos) {
				if (poli == null) {
					continue;
				}
				PoligonLayerFactory.syncPoligonoFromMeasureTool(poli);
				List<SurfacePolygon> shapes = GeometryHelper.createSurfacePolygonsFromPoligono(poli);
				if (shapes == null || shapes.isEmpty()) {
					continue;
				}
				String name = poli.getNombre() != null && !poli.getNombre().isBlank()
						? poli.getNombre()
						: "Poligono";
				for (SurfacePolygon shape : shapes) {
					if (shape == null || shape.getLocations() == null) {
						continue;
					}
					shape.setValue(AVKey.DISPLAY_NAME, name);
					shape.setAttributes(attrs);
					kmzBuilder.writeObject(shape);
					written++;
				}
			}
			kmzBuilder.close();
			if (written == 0) {
				logger.warning("No se pudo exportar ningún polígono a KMZ (sin geometría válida)");
				if (selected.exists() && !selected.delete()) {
					logger.fine("No se pudo borrar el KMZ vacío: " + selected);
				}
				return null;
			}
			logger.info("Exportados " + written + " polígonos a " + selected.getAbsolutePath());
			return selected;
		} catch (IOException | XMLStreamException e) {
			logger.log(Level.WARNING, "Error al exportar polígonos a KMZ", e);
			return null;
		}
	}

	private static ShapeAttributes createDefaultAttributes() {
		BasicShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setInteriorMaterial(new Material(new Color(0, 150, 255, 120)));
		attrs.setOutlineMaterial(new Material(new Color(0, 90, 180)));
		attrs.setInteriorOpacity(0.45);
		attrs.setOutlineOpacity(1.0);
		attrs.setOutlineWidth(2.0);
		attrs.setDrawInterior(true);
		attrs.setDrawOutline(true);
		return attrs;
	}

	private static String safeFileName(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			return "poligono";
		}
		return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}
}
