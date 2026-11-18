package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import com.ursulagis.desktop.dao.Poligono;

import gov.nasa.worldwind.geom.Position.PositionList;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLAbstractGeometry;
import gov.nasa.worldwind.ogc.kml.KMLDocument;
import gov.nasa.worldwind.ogc.kml.KMLExtendedData;
import gov.nasa.worldwind.ogc.kml.KMLFolder;
import gov.nasa.worldwind.ogc.kml.KMLLinearRing;
import gov.nasa.worldwind.ogc.kml.KMLMultiGeometry;
import gov.nasa.worldwind.ogc.kml.KMLPlacemark;
import gov.nasa.worldwind.ogc.kml.KMLPolygon;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.KMLSchemaData;
import gov.nasa.worldwind.ogc.kml.KMLSimpleData;
import gov.nasa.worldwind.util.Logging;

/**
 * Lee polígonos desde KML, incluyendo los que vienen dentro de MultiGeometry.
 */
public final class KMLHelper {
	private static final Logger logger = Logger.getLogger(KMLHelper.class.getName());

	private KMLHelper() {
	}

	public static final class Lectura {
		private final String archivo;
		private final Poligono poligono;
		private final String motivo;

		private Lectura(String archivo, Poligono poligono, String motivo) {
			this.archivo = archivo == null || archivo.isBlank() ? "sin archivo" : archivo;
			this.poligono = poligono;
			this.motivo = motivo;
		}

		public String getArchivo() {
			return archivo;
		}

		public Poligono getPoligono() {
			return poligono;
		}

		public String getMotivo() {
			return motivo;
		}
	}

	public static List<Lectura> leer(File source) {
		List<Lectura> lecturas = new ArrayList<>();
		String archivo = source == null ? "sin archivo" : source.getName();
		try {
			KMLRoot kmlRoot = KMLRoot.create(source);
			if (kmlRoot == null) {
				String message = Logging.getMessage("generic.UnrecognizedSourceType", source.toString(), source.toString());
				throw new IllegalArgumentException(message);
			}
			try {
				kmlRoot.parse();
			} catch (XMLStreamException e) {
				kmlRoot = KMLRoot.create(source, false);
				kmlRoot.parse();
			}
			Object documentField = kmlRoot.getField("Document");
			if (!(documentField instanceof KMLDocument)) {
				lecturas.add(omitido(archivo, "el archivo no tiene un Document KML"));
				return lecturas;
			}
			KMLDocument doc = (KMLDocument) documentField;
			Queue<KMLAbstractFeature> featuresQueue = new ConcurrentLinkedQueue<>();
			featuresQueue.addAll(doc.getFeatures());
			int placemarkIndex = 1;
			while (!featuresQueue.isEmpty()) {
				KMLAbstractFeature feature = featuresQueue.poll();
				if (feature instanceof KMLPlacemark) {
					lecturas.addAll(leer((KMLPlacemark) feature, archivo, placemarkIndex));
					placemarkIndex++;
				} else if (feature instanceof KMLFolder) {
					featuresQueue.addAll(((KMLFolder) feature).getFeatures());
				}
			}
		} catch (IOException | XMLStreamException | RuntimeException e) {
			logger.log(Level.WARNING, "no se pudo leer el KML " + archivo, e);
			lecturas.add(omitido(archivo, "no se pudo leer el KML: " + mensaje(e)));
		}
		return lecturas;
	}

	public static List<Lectura> leer(KMLPlacemark placemark) {
		return leer(placemark, "", 1);
	}

	public static List<Lectura> leer(KMLPlacemark placemark, String archivo, int indice) {
		List<Lectura> lecturas = new ArrayList<>();
		if (placemark == null) {
			lecturas.add(omitido(archivo, "placemark nulo"));
			return lecturas;
		}
		agregarGeometria(placemark.getGeometry(), archivo, nombrePlacemark(placemark, indice), lecturas);
		return lecturas;
	}

	private static void agregarGeometria(KMLAbstractGeometry geometry, String archivo, String name, List<Lectura> lecturas) {
		if (geometry instanceof KMLMultiGeometry) {
			List<KMLAbstractGeometry> partes = ((KMLMultiGeometry) geometry).getGeometries();
			if (partes == null || partes.isEmpty()) {
				lecturas.add(omitido(archivo, name + ": MultiGeometry vacía"));
				return;
			}
			int parte = 1;
			for (KMLAbstractGeometry geometria : partes) {
				String nombreParte = partes.size() == 1 ? name : name + " [" + parte + "]";
				agregarGeometria(geometria, archivo, nombreParte, lecturas);
				parte++;
			}
			return;
		}
		if (!(geometry instanceof KMLPolygon)) {
			String tipo = geometry == null ? "sin geometría" : geometry.getClass().getSimpleName();
			lecturas.add(omitido(archivo, name + ": no es un polígono (" + tipo + ")"));
			return;
		}
		lecturas.add(leerPoligono(archivo, name, (KMLPolygon) geometry));
	}

	private static Lectura leerPoligono(String archivo, String name, KMLPolygon kmlPolygon) {
		try {
			List<PositionList> holes = new ArrayList<>();
			Iterable<? extends KMLLinearRing> innerBoundaries = kmlPolygon.getInnerBoundaries();
			if (innerBoundaries != null) {
				for (KMLLinearRing innerRing : innerBoundaries) {
					if (innerRing != null && innerRing.getCoordinates() != null) {
						holes.add(innerRing.getCoordinates());
					}
				}
			}
			KMLLinearRing outer = kmlPolygon.getOuterBoundary();
			if (outer == null || outer.getCoordinates() == null) {
				return omitido(archivo, name + ": sin contorno exterior");
			}
			Poligono poli = GeometryHelper.constructPolygon(outer.getCoordinates(), holes);
			poli.setNombre(name);
			poli.setArea(ProyectionConstants.A_HAS(poli.toGeometry().getArea()));
			return new Lectura(archivo, poli, null);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "no se pudo convertir el polígono " + name, e);
			return omitido(archivo, name + ": " + mensaje(e));
		}
	}

	private static Lectura omitido(String archivo, String motivo) {
		return new Lectura(archivo, null, motivo);
	}

	private static String mensaje(Exception e) {
		if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
			return e == null ? "error desconocido" : e.getClass().getSimpleName();
		}
		return e.getMessage();
	}

	private static String nombrePlacemark(KMLPlacemark placemark, int indice) {
		String name = placemark.getName();
		if (name != null && !name.isBlank()) {
			return name.trim();
		}
		String desdeDatos = nombreDesdeSimpleData(placemark.getSimpleData());
		if (desdeDatos != null) {
			return desdeDatos;
		}
		KMLExtendedData extended = placemark.getExtendedData();
		if (extended != null && extended.getSchemaData() != null) {
			for (KMLSchemaData schema : extended.getSchemaData()) {
				if (schema.getSimpleData() == null) {
					continue;
				}
				for (KMLSimpleData data : schema.getSimpleData()) {
					String valor = nombreDesdeSimpleData(data);
					if (valor != null) {
						return valor;
					}
				}
			}
		}
		return "Polígono " + indice;
	}

	private static String nombreDesdeSimpleData(KMLSimpleData data) {
		if (data == null || data.getCharacters() == null || data.getCharacters().isBlank()) {
			return null;
		}
		String valor = data.getCharacters().trim();
		String campo = data.getName();
		if (campo != null && !campo.isBlank()) {
			return campo.trim() + " " + valor;
		}
		return valor;
	}
}
