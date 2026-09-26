package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.Messages;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;


import java.util.logging.Logger;
/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */

public class ExportarPrescripcionSiembraTask extends ProgresibleTask<File>{
	private static final Logger logger = Logger.getLogger(ExportarPrescripcionSiembraTask.class.getName());

	private SiembraLabor laborToExport=null;
	private File shapeFile=null;
	private String unidad=null;
	public boolean guardarConfig=true;

	public  ExportarPrescripcionSiembraTask(SiembraLabor _laborToExport,File _shapeFile,String _unidad) {	
		laborToExport=_laborToExport;
		shapeFile=_shapeFile;
		unidad=_unidad;		
	}

	public File call() {
		try {
			SimpleFeatureType type = null;
			//*the_geom:    												:4326,
			//	String typeDescriptor = Messages.getString("ExportarPrescripcionSiembraTask.theGeom")+Polygon.class.getCanonicalName()+Messages.getString("ExportarPrescripcionSiembraTask.srid4326") //$NON-NLS-1$ //$NON-NLS-2$
			//
			//		+ SiembraLabor.COLUMNA_DOSIS_LINEA + Messages.getString("ExportarPrescripcionSiembraTask.javaLangLong") //$NON-NLS-1$ :java.lang.Long,
			//		+ SiembraLabor.COLUMNA_DOSIS_COSTADO + Messages.getString("ExportarPrescripcionSiembraTask.javaLangLong") //$NON-NLS-1$ :java.lang.Long,
			//		//seeding
			//		+ Messages.getString("ExportarPrescripcionSiembraTask.siembra") + Messages.getString("ExportarPrescripcionSiembraTask.javaLangLong"); //$NON-NLS-1$ :java.lang.Long


			//		Map<String,String> availableColums = new LinkedHashMap<String,String>();
			//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_10METROS"),SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
			//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_DOSIS_SEMILLA"),SiembraLabor.COLUMNA_KG_SEMILLA);//("kgSemHa");
			//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_MILES_SEM_HA"),SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
			//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_ML"),SiembraLabor.COLUMNA_SEM_ML);//("semML");

			String dosisClass = "java.lang.Long";
			if(SiembraLabor.COLUMNA_SEM_ML.equals(unidad)) {
				dosisClass = "java.lang.Float";
			}

			String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":4326,"//$NON-NLS-1$
					+ SiembraLabor.COLUMNA_DOSIS_LINEA +":java.lang.Long,"//java.lang.Long,"//$NON-NLS-1$
					+ SiembraLabor.COLUMNA_DOSIS_COSTADO +":java.lang.Long,"//$NON-NLS-1$
					+unidad+":"+dosisClass;//$NON-NLS-1$ semilla siempre tiene que ser la 3ra columna

			logger.fine("creando type con: "+typeDescriptor); //$NON-NLS-1$ the_geom:Polygon:4326,Fert L:java.lang.Long,Fert C:java.lang.Long,seeding:java.lang.Long
			//System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
			try {
				//type = DataUtilities.createType(Messages.getString("ExportarPrescripcionSiembraTask.prescType"), typeDescriptor); //$NON-NLS-1$
				type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
			} catch (SchemaException e) {
				e.printStackTrace();
			}
			logger.fine("PrescType: "+DataUtilities.encodeType(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$


			List<LaborItem> items = new ArrayList<LaborItem>();

			super.updateProgress(0, 100);

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			int initialItemsSize =0;
			while(it.hasNext()){
				initialItemsSize++;
				SimpleFeature next = it.next();			
				SiembraItem si = laborToExport.constructFeatureContainerStandar(next,true);
				//System.out.println("leyendo un item con dosisML "+si.getDosisML());
				//Poligono p =GeometryHelper.constructPoligono(fi.getGeometry());
				//XXX porque no usar fi.getGeometry() directamente?
				//R: porque construct poligono une las partes
				//fi.setGeometry(p.toGeometry());
				siembrasToFlat(items, si);			
			}
			it.close();



			int zonas = items.size();
			logger.fine("pase de "+initialItemsSize+" a "+zonas+" items por flatPolygons");

			int maxItems = GeometryHelper.getMaxPrescriptionItems();
			if(zonas>=maxItems) {
				reabsorverZonasChicas(items, maxItems);
			}

			for(LaborItem item:items) {
				checkCancelled();
				Geometry limited = GeometryHelper.limitPrescriptionGeometryParts(item.getGeometry());
				if (limited != item.getGeometry()) {
					logger.fine("reduciendo item " + item + " de "
							+ item.getGeometry().getNumGeometries() + " a "
							+ limited.getNumGeometries() + " partes");
					item.setGeometry(limited);
				}
			}

			long maxKb = GeometryHelper.getMaxPrescriptionExportKb();
			long kilobytes = writePrescriptionShapefile(type, items, 0);
			for (int attempt = 1;
					kilobytes > maxKb
							&& attempt <= GeometryHelper.MAX_PRESCRIPTION_SIMPLIFY_ATTEMPTS;
					attempt++) {
				double toleranceM = GeometryHelper.prescriptionSimplifyToleranceMeters(attempt);
				logger.info(String.format(
						"prescripcion siembra %,dKB > %dKB; simplificando vertices (tolerancia %.2fm, intento %d)",
						kilobytes, maxKb, toleranceM, attempt));
				super.updateTitle("simplificando geometrias");
				GeometryHelper.simplifyLaborItemsForPrescriptionExport(items, toleranceM);
				kilobytes = writePrescriptionShapefile(type, items, attempt);
			}

			if (kilobytes > maxKb) {
				final long kb = kilobytes;
				final long limitKb = maxKb;
				Platform.runLater(() -> {
					Alert a = new Alert(Alert.AlertType.ERROR);
					a.setContentText(String.format(
							"El archivo generado pesa %,dKB,  en algunos monitores %,dKB es lo maximo",
							kb, limitKb));
					a.showAndWait();
				});
			}
			logger.fine(String.format("%,d kilobytes", kilobytes));

			if(guardarConfig) {
				//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
				Configuracion config = Configuracion.getInstance();
				config.loadProperties();
				config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
				config.save();
			}
			//		System.out.println(Messages.getString("ExportarPrescripcionSiembraTask.emptyString")+ shapeFile); //$NON-NLS-1$
			//		Configuracion config = Configuracion.getInstance();
			//		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			//		config.save();

			return shapeFile;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Construye features, escribe el shapefile y devuelve el tamaño en KB.
	 * Si {@code attempt} &gt; 0, elimina el shapefile previo para reescribirlo
	 * con geometrias simplificadas.
	 */
	private long writePrescriptionShapefile(SimpleFeatureType type, List<LaborItem> items, int attempt)
			throws InterruptedException {
		if (attempt > 0) {
			deleteShapefileSidecars(shapeFile);
		}

		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
		Integer id = 0;
		for(LaborItem i:items) {//(it.hasNext()){
			checkCancelled();
			SiembraItem fi=(SiembraItem) i;
			Geometry itemGeometry=fi.getGeometry();
			List<Polygon> flatPolygons = GeometryHelper.limitPrescriptionFlatPolygons(
					PolygonValidator.geometryToFlatPolygons(itemGeometry));
			if(flatPolygons.size()>1) {
				logger.fine("el item "+i.getId()+" tiene "+flatPolygons.size()+" poligonos");
			}

			for(Polygon p : flatPolygons){
				logger.fine("exportando un item con dosis "+fi.getDosisML());
				Double semilla = Math.rint(fi.getDosisML()*10);///XXX aca hago magia para convertir de plantas por metro a plantas cada 10 metros
				if(SiembraLabor.COLUMNA_KG_SEMILLA.equals(unidad)) {
					semilla = Math.rint(fi.getDosisHa());
				} else if(SiembraLabor.COLUMNA_MILES_SEM_HA.equals(unidad)) {
					semilla = Math.rint(fi.getDosisML()*(10/laborToExport.getEntreSurco()));
				} else if(SiembraLabor.COLUMNA_SEM_HA.equals(unidad)) {
					// semillas/ha = sem/m lineal * (m2/ha / entreSurco)
					semilla = Math.rint(fi.getDosisML()*(ProyectionConstants.METROS2_POR_HA/laborToExport.getEntreSurco()));
				} else if(SiembraLabor.COLUMNA_SEM_ML.equals(unidad)) {
					semilla = fi.getDosisML();
					logger.fine("Exportando prescripcion con dosis ML "+semilla);
				}
				Double linea = fi.getDosisFertLinea();
				Double costado = fi.getDosisFertCostado();

				id++;
				SimpleFeature exportFeature = fb.buildFeature(null, new Object[]{p,linea,costado,semilla});
				exportFeatureCollection.add(exportFeature);
			}
		}

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1
		try {
			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
				Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
				featureStore.setTransaction(transaction);

				try {
					featureStore.setFeatures(exportFeatureCollection.reader());
					try {
						transaction.commit();
					} catch (Exception e1) {
						e1.printStackTrace();
					}finally {
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
		} finally {
			if (newDataStore != null) {
				newDataStore.dispose();
			}
		}

		try {
			return Files.size(shapeFile.toPath()) / 1024;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static void deleteShapefileSidecars(File shp) {
		if (shp == null) {
			return;
		}
		String absolute = shp.getAbsolutePath();
		String base = absolute.toLowerCase().endsWith(".shp")
				? absolute.substring(0, absolute.length() - 4)
				: absolute;
		for (String ext : new String[] { ".shp", ".shx", ".dbf", ".prj", ".fixx", ".qix", ".cpg", ".sbn", ".sbx" }) {
			File sidecar = new File(base + ext);
			if (sidecar.exists() && !sidecar.delete()) {
				logger.fine("no se pudo borrar " + sidecar.getAbsolutePath());
			}
		}
	}

	public void siembrasToFlat(List<LaborItem> items, SiembraItem si) {
		List<Polygon> simples = PolygonValidator.geometryToFlatPolygons(si.getGeometry());
		for(Polygon sp:simples) {
			SiembraItem fi = new SiembraItem(si);
			logger.fine("siembras to flat con dosisML "+fi.getDosisML()+" de "+si.getDosisML());
			fi.setGeometry(sp);
			items.add(fi);
		}
	}




	public void reabsorverZonasChicas( List<LaborItem> items, int maxItems) throws InterruptedException {
		if (maxItems < 2) {
			return;
		}
		// reabsorver zonas mas chicas a las mas grandes vecinas
		logger.fine("tiene mas de "+maxItems+" zonas, reabsorviendo..."); //$NON-NLS-1$
		// tomar las N zonas mas grandes y reabsorver las otras en estas
		Double areaPromedio=null;
		Double desvioPromedio=null;
		OptionalDouble areaPromedioOP = items.parallelStream().mapToDouble(item->item.getGeometry().getArea()).average();
		if(areaPromedioOP.isPresent()) {
			areaPromedio = areaPromedioOP.getAsDouble();
			OptionalDouble desvioPromedioOP = items.parallelStream().mapToDouble(
					item->Math.abs(item.getGeometry().getArea()-areaPromedioOP.getAsDouble())
					).average();	
			if(desvioPromedioOP.isPresent()) {
				desvioPromedio=desvioPromedioOP.getAsDouble();
			}
		}

		if(desvioPromedio!=null) {
			double has =ProyectionConstants.A_HAS(areaPromedio);
			Double porcDesvio=desvioPromedio/areaPromedio;
			//System.out.println("has "+has+" desvio promedio es "+porcDesvio*100+"%");
			if(porcDesvio<0.1) {
				logger.fine("debo resumir labor por categorias");				

				List<List<LaborItem>> itemsByCat = new ArrayList<List<LaborItem>>();
				for(int i=0;i<this.laborToExport.clasificador.getNumClasses();i++){
					itemsByCat.add(i, new ArrayList<LaborItem>());
				}

				for(LaborItem si : items) {
					int cat = this.laborToExport.clasificador.getCategoryFor(si.getAmount());
					//si.getCategoria()
					itemsByCat.get(cat).add(si);
				}
				List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();
				for(List<LaborItem> catItems : itemsByCat) {
					logger.fine("resumiendo "+catItems.size());
					if(catItems.size()>0) {
						List<SiembraItem> siembraItems =catItems.stream().map(item->(SiembraItem)item).collect(Collectors.toList());

						SiembraItem siembraCat =(SiembraItem)ResumirLaborMapTask.resumirSiembraItems(siembraItems);
						siembrasToFlat(itemsCategoria,siembraCat);

					}				
				}
				// pasar a flat items
				items.clear();
				items.addAll(itemsCategoria);
				//items = itemsCategoria;//aca perdi la referencia a la lista original
				logger.fine("flat items resumidos por categoria "+items.size());
			}//las geometrias son todas iguales		
		}//desvio promedio no es null
		if(items.size()>=maxItems) {

			items.sort((i1,i2)->-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea()));					
			List<LaborItem> itemsAgrandar =items.subList(0,maxItems-1);//de 0 al N-2
			Quadtree tree=new Quadtree();
			for(LaborItem ar : itemsAgrandar) {
				Geometry gAr =ar.getGeometry();
				tree.insert(gAr.getEnvelopeInternal(), ar);
				//System.out.println("insertando al tree item con dosis "+ar.getAmount());//ok
			}

			List<LaborItem> itemsAReducir =new ArrayList<LaborItem>(items.subList(maxItems-1, items.size()-1));
			int aReducirCount = itemsAReducir.size();
			logger.fine("reduciendo "+aReducirCount);
			int n=0;
			while(itemsAReducir.size()>0 && n<10) {//corro mientras haya items a reducir o hasta 10 veces
				checkCancelled();
				List<LaborItem> done = new ArrayList<LaborItem>();		
				for(LaborItem ar : itemsAReducir) {
					Geometry gAr =ar.getGeometry();
					@SuppressWarnings("unchecked")
					List<LaborItem> vecinos =(List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

					int indexAReducir = itemsAReducir.indexOf(ar);
					logger.fine((indexAReducir*100/itemsAReducir.size())+"% vecinos "+vecinos.size());
					updateProgress(indexAReducir,itemsAReducir.size());
					if(vecinos.size()>0) {
						Optional<LaborItem> opV = 
								vecinos.stream().filter(v->{
									return gAr.intersects(v.getGeometry());//filtro solo los que intersectan
								}).max((v1,v2)->-1*Double.compare(v1.getGeometry().getArea(), v2.getGeometry().getArea()));	


						if(opV.isPresent()) {

							LaborItem v = opV.get();
							Geometry g = v.getGeometry();
							try {
								tree.remove(g.getEnvelopeInternal(), v);//hace falta sacar el feature? o alcanza con modificar la geom?

								Geometry union = GeometryHelper.unirGeometrias(Arrays.asList(gAr,g));
								//Geometry union = g.union(gAr);
								v.setGeometry(union);
								logger.fine("insertando al tree item con dosisML "+((SiembraItem)v).getDosisML());
								tree.insert(union.getEnvelopeInternal(), v);
								done.add(ar);
							}catch(Exception e) {
								e.printStackTrace();
								tree.insert(g.getEnvelopeInternal(), v);
							}
						}else {
							logger.fine("no se encontraron vecinos para "+gAr);
						}
					}
				}//termino de recorrer todos los items a reducir
				logger.fine(n+" ya di una vuelta a todos los items, reduci "+done.size());
				n++;
				itemsAReducir.removeAll(done);
				done.clear();
				updateProgress(aReducirCount-itemsAReducir.size(), aReducirCount);
			}
			items.clear();
			items.addAll((List<LaborItem>)tree.queryAll());
//			for(LaborItem i : items) {
//				System.out.println("termine de procesar item del tree con area "+ProyectionConstants.A_HAS(i.getGeometry().getArea()));
//			}
		}else {
			logger.fine("vuelvo sin absorver geometrias chicas porque no hay mas de "+maxItems+" flat geoms");
		}
	}
}
