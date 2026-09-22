package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.logging.Logger;

/**
 * Exporta una labor de fertilizacion como shapefile de prescripcion.
 * Misma estructura que {@link ExportarPrescripcionSiembraTask}: flatten →
 * reabsorver si hay demasiadas zonas → limitar partes → escribir SHP.
 */
public class ExportarPrescripcionFertilizacionTask extends ProgresibleTask<File> {
	private static final Logger logger = Logger.getLogger(ExportarPrescripcionFertilizacionTask.class.getName());

	private static final int MAX_ITEMS = 100;
	private FertilizacionLabor laborToExport = null;
	private File shapeFile = null;
	public boolean guardarConfig = true;

	public ExportarPrescripcionFertilizacionTask(FertilizacionLabor laborToExport, File shapeFile) {
		super();
		this.laborToExport = laborToExport;
		this.shapeFile = shapeFile;
		super.updateTitle(taskName);
		this.taskName = laborToExport.getNombre();
	}

	public File call() {
		try {
			SimpleFeatureType type = constructType();

			List<LaborItem> items = new ArrayList<>();
			super.updateProgress(0, 100);

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			int initialItemsSize = 0;
			while (it.hasNext()) {
				initialItemsSize++;
				SimpleFeature next = it.next();
				FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(next, true);
				fertilizacionToFlat(items, fi);
			}
			it.close();

			int zonas = items.size();
			logger.fine("pase de " + initialItemsSize + " a " + zonas + " items por flatPolygons");

			if (zonas >= MAX_ITEMS) {
				reabsorverZonasChicas(items);
			}

			for (LaborItem item : items) {
				checkCancelled();
				Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(item.getGeometry());
				if (limited != item.getGeometry()) {
					logger.fine("reduciendo item " + item + " de "
							+ item.getGeometry().getNumGeometries() + " a "
							+ limited.getNumGeometries() + " partes");
					item.setGeometry(limited);
				}
			}

			DefaultFeatureCollection exportFeatureCollection = new DefaultFeatureCollection("PrescType", type);
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
			super.updateTitle("exportando");
			updateProgress(0, items.size());
			int processed = 0;
			for (LaborItem i : items) {
				checkCancelled();
				FertilizacionItem fi = (FertilizacionItem) i;
				Geometry itemGeometry = fi.getGeometry();
				List<Polygon> flatPolygons = GeometryHelper.limitPrescriptionFlatPolygons(
						PolygonValidator.geometryToFlatPolygons(itemGeometry));
				if (flatPolygons.size() > 1) {
					logger.fine("el item " + i.getId() + " tiene " + flatPolygons.size() + " poligonos");
				}

				Double dosisHa = Math.rint(fi.getDosistHa());
				for (Polygon p : flatPolygons) {
					// Schema compatible con monitores: Rate/linea = dosis, costado=0, semilla=0
					SimpleFeature exportFeature = fb.buildFeature(null, new Object[] { p, dosisHa, 0L, 0L });
					exportFeatureCollection.add(exportFeature);
				}
				processed++;
				updateProgress(processed, items.size());
			}

			ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile, type);
			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {
				e.printStackTrace();
			}

			super.updateTitle("escribiendo el archivo");
			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);

				try {
					featureStore.setFeatures(exportFeatureCollection.reader());
					try {
						transaction.commit();
					} catch (Exception e1) {
						e1.printStackTrace();
					} finally {
						try {
							transaction.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			try {
				long bytes = Files.size(shapeFile.toPath());
				long kilobytes = bytes / 1024;
				if (kilobytes > 500) {
					Platform.runLater(() -> {
						Alert a = new Alert(Alert.AlertType.ERROR);
						a.setContentText(String.format(
								"El archivo generado pesa %,dKB,  en algunos monitores 512KB es lo maximo",
								kilobytes));
						a.showAndWait();
					});
				}
				logger.fine(String.format("%,d kilobytes", bytes / 1024));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (guardarConfig) {
				Configuracion config = Configuracion.getInstance();
				config.loadProperties();
				config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
				config.save();
			}

			updateProgress(100, 100);
			return shapeFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Compatibilidad con callers que invocaban {@code run(labor, file)}. */
	public void run(FertilizacionLabor laborToExport, File shapeFile) {
		this.laborToExport = laborToExport;
		this.shapeFile = shapeFile;
		call();
	}

	public SimpleFeatureType constructType() {
		SimpleFeatureType type = null;
		String typeDescriptor = "*the_geom:" + Polygon.class.getCanonicalName() + ":4326,"
				+ SiembraLabor.COLUMNA_DOSIS_LINEA + ":java.lang.Long,"
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO + ":java.lang.Long,"
				+ SiembraLabor.COLUMNA_SEM_10METROS + ":java.lang.Long";

		logger.fine("creando type con: " + typeDescriptor);
		try {
			type = DataUtilities.createType("PrescType", typeDescriptor);
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		logger.fine("PrescType: " + DataUtilities.encodeType(type));
		return type;
	}

	public void fertilizacionToFlat(List<LaborItem> items, FertilizacionItem fi) {
		List<Polygon> simples = PolygonValidator.geometryToFlatPolygons(fi.getGeometry());
		for (Polygon sp : simples) {
			FertilizacionItem clone = new FertilizacionItem(fi);
			clone.setGeometry(sp);
			items.add(clone);
		}
	}

	public void reabsorverZonasChicas(List<LaborItem> items) throws InterruptedException {
		logger.fine("tiene mas de 100 zonas, reabsorviendo...");
		Double areaPromedio = null;
		Double desvioPromedio = null;
		OptionalDouble areaPromedioOP = items.parallelStream()
				.mapToDouble(item -> item.getGeometry().getArea())
				.average();
		if (areaPromedioOP.isPresent()) {
			areaPromedio = areaPromedioOP.getAsDouble();
			OptionalDouble desvioPromedioOP = items.parallelStream()
					.mapToDouble(item -> Math.abs(item.getGeometry().getArea() - areaPromedioOP.getAsDouble()))
					.average();
			if (desvioPromedioOP.isPresent()) {
				desvioPromedio = desvioPromedioOP.getAsDouble();
			}
		}

		if (desvioPromedio != null) {
			Double porcDesvio = desvioPromedio / areaPromedio;
			if (porcDesvio < 0.1) {
				logger.fine("debo resumir labor por categorias");

				List<List<LaborItem>> itemsByCat = new ArrayList<>();
				for (int i = 0; i < this.laborToExport.clasificador.getNumClasses(); i++) {
					itemsByCat.add(i, new ArrayList<>());
				}

				for (LaborItem fi : items) {
					int cat = this.laborToExport.clasificador.getCategoryFor(fi.getAmount());
					itemsByCat.get(cat).add(fi);
				}
				List<LaborItem> itemsCategoria = new ArrayList<>();
				for (List<LaborItem> catItems : itemsByCat) {
					logger.fine("resumiendo " + catItems.size());
					if (catItems.size() > 0) {
						FertilizacionItem fertCat = (FertilizacionItem) ResumirLaborMapTask.resumirFertItems(catItems);
						fertilizacionToFlat(itemsCategoria, fertCat);
					}
				}
				items.clear();
				items.addAll(itemsCategoria);
				logger.fine("flat items resumidos por categoria " + items.size());
			}
		}

		if (items.size() >= MAX_ITEMS) {
			items.sort((i1, i2) -> -1 * Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea()));
			List<LaborItem> itemsAgrandar = items.subList(0, MAX_ITEMS - 1);
			Quadtree tree = new Quadtree();
			for (LaborItem ar : itemsAgrandar) {
				Geometry gAr = ar.getGeometry();
				tree.insert(gAr.getEnvelopeInternal(), ar);
			}

			List<LaborItem> itemsAReducir = new ArrayList<>(items.subList(MAX_ITEMS - 1, items.size()));
			int aReducirCount = itemsAReducir.size();
			logger.fine("reduciendo " + aReducirCount);
			int n = 0;
			super.updateTitle("reabsorver zonas chicas");
			while (itemsAReducir.size() > 0 && n < 10) {
				checkCancelled();
				List<LaborItem> done = new ArrayList<>();
				for (LaborItem ar : itemsAReducir) {
					Geometry gAr = ar.getGeometry();
					@SuppressWarnings("unchecked")
					List<LaborItem> vecinos = (List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

					int indexAReducir = itemsAReducir.indexOf(ar);
					logger.fine((indexAReducir * 100 / itemsAReducir.size()) + "% vecinos " + vecinos.size());
					updateProgress(indexAReducir, itemsAReducir.size());
					if (vecinos.size() > 0) {
						Optional<LaborItem> opV = vecinos.stream()
								.filter(v -> gAr.intersects(v.getGeometry()))
								.max((v1, v2) -> -1 * Double.compare(
										v1.getGeometry().getArea(), v2.getGeometry().getArea()));

						if (opV.isPresent()) {
							LaborItem v = opV.get();
							Geometry g = v.getGeometry();
							try {
								tree.remove(g.getEnvelopeInternal(), v);
								Geometry union = GeometryHelper.unirGeometrias(Arrays.asList(gAr, g));
								v.setGeometry(union);
								tree.insert(union.getEnvelopeInternal(), v);
								done.add(ar);
							} catch (Exception e) {
								e.printStackTrace();
								tree.insert(g.getEnvelopeInternal(), v);
							}
						} else {
							logger.fine("no se encontraron vecinos para " + gAr);
						}
					}
				}
				logger.fine(n + " ya di una vuelta a todos los items, reduci " + done.size());
				n++;
				itemsAReducir.removeAll(done);
				done.clear();
				updateProgress(aReducirCount - itemsAReducir.size(), aReducirCount);
			}
			items.clear();
			@SuppressWarnings("unchecked")
			List<LaborItem> kept = (List<LaborItem>) tree.queryAll();
			items.addAll(kept);
		} else {
			logger.fine("vuelvo sin absorver geometrias chicas porque no hay mas de 100 flat geoms");
		}
	}
}
