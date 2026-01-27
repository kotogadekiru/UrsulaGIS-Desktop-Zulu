package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.geometry.Position;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.siembra.SiembraConfig;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class GrillarSiembrasMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	/**
	 * la lista de las siembras a unir
	 */
	private List<SiembraLabor> siembras;
	private boolean rellenarHuecos = false;
	private double ancho=10;

			
	public GrillarSiembrasMapTask(List<SiembraLabor> siembras){
		super(new SiembraLabor());
		this.taskName="grillar siembras";
		this.siembras=siembras;
		
		SiembraConfig sConfig= labor.getConfiguracion();
		sConfig.valorMetrosPorUnidadDistanciaProperty().set(1.0);
		sConfig.correccionDistanciaProperty().set(false);
		sConfig.correccionAnchoProperty().set(false);
		sConfig.correccionSuperposicionProperty().set(false);
		sConfig.resumirGeometriasProperty().setValue(false);
		sConfig.supMinimaProperty().set(0);
		
		labor.setNombre(Messages.getString("GrillarSiembrasMapTask.0"));//este es el nombre que se muestra en el progressbar
	}
	
	public void setAncho(double _ancho) {
		this.ancho=_ancho;
	}

	/**
	 * proceso que toma una lista de siembras y las une 
	 * con una grilla promediando los valores de acuerdo a su promedio ponderado por la superficie
	 * superpuesta de cada item sobre la superficie superpuesta total de cada "pixel de la grilla"
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las siembras
		ReferencedEnvelope unionEnvelope = null;
		double ancho = this.ancho;
		String nombre =null;
		for(SiembraLabor s:siembras){
			labor.setFecha(s.getFecha());
			labor.precioInsumo=s.precioInsumo;
			labor.precioLabor=s.precioLabor;
			labor.setSemilla(s.getSemilla());
			labor.setEntreSurco(s.getEntreSurco());
			labor.setFertLinea(s.getFertLinea());
			labor.setFertCostado(s.getFertCostado());
			
			if(nombre == null){
				nombre=labor.getNombre()+Messages.getString("GrillarSiembrasMapTask.1")+s.getNombre();
			}else {
				nombre+=Messages.getString("GrillarSiembrasMapTask.2")+s.getNombre();
			}

			ReferencedEnvelope b = s.outCollection.getBounds();
			if(unionEnvelope==null){
				unionEnvelope=b;
			}else{
				unionEnvelope.expandToInclude(b);
			}
		}
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());
		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = construirGrilla(unionEnvelope, ancho);
		System.out.println("creando una grilla con"+grilla.size()+" elementos");

		featureCount = grilla.size();

		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		
		ConcurrentMap<Polygon,SiembraItem > byPolygon =
				grilla.parallelStream().collect(
						() -> new  ConcurrentHashMap< Polygon,SiembraItem>(),
						(map, poly) -> {
							
						try{
							List<SiembraItem>  siembrasPoly = siembras.parallelStream().collect(
									()->new  ArrayList<SiembraItem>(),
									(list, siembra) ->{			
										list.addAll(siembra.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2)
									);

							SiembraItem item = construirFeature(siembrasPoly,poly);                    			

							if(item!=null){
								map.put(poly,item);
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
										labor.getType());
								SimpleFeature f = item.getFeature(fBuilder);
								if(f!=null){
									boolean res = features.add(f);
									if(!res){
										System.out.println("no se pudo agregar la feature"+f);
									}
								}
							}
							this.featureNumber++;
							updateProgress( this.featureNumber, featureCount);

						}catch(Exception e){
							System.err.println("error al construir un elemento de la grilla");
							e.printStackTrace();
						}
						},
						(map1, map2) -> map1.putAll(map2)
						);
		//Limpio la cache de las labores despues de hacer las querys
		for(SiembraLabor s:siembras){
			s.clearCache();
		}

		System.out.println("cree una union de"+byPolygon.size()+" elementos");

		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection(Messages.getString("GrillarSiembrasMapTask.9"),labor.getType());
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){
			System.out.println("no se pudieron agregar las features al outCollection");
		}

		labor.constructClasificador();

		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde"+time+" milisegundos en unir las siembras. es"+time/featureCount+" milisegundos por polígono");
	}

	/**
	 * 
	 * @param siembrasPoly lista de siembraItems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el siembra Item promedio
	 * @return SimpleFeature de tipo SiembraItem que represente a siembrasPoly 
	 */
	private SiembraItem construirFeature(List<SiembraItem> siembrasPoly, Polygon poly) {
		if(siembrasPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();
		double areaItersectadaTotal = 0;
		Map<SiembraItem,Double> areasIntersecciones = new HashMap<SiembraItem,Double>();
		for(SiembraItem sPoly : siembrasPoly){
			Geometry g = sPoly.getGeometry();
			try{				
				Geometry interseccion= GeometryHelper.getIntersection(poly, g);
				Double areaInterseccion = interseccion.getArea();
				areaItersectadaTotal+=areaInterseccion;
				areasIntersecciones.put(sPoly,areaInterseccion);
				intersections.add(interseccion);			
			}catch(Exception e){
				System.err.println("fallo la interseccion entre "+poly+" y "+g);
			}		
		}

		SiembraItem s = null;

		if(areaItersectadaTotal>getAreaMinimaLongLat()){
			double dosisHaProm=0, dosisMLProm=0, dosisFertLineaProm=0, dosisFertCostadoProm=0;
			double elev=0;
			double ancho=labor.getConfiguracion().getAnchoGrilla();
			double distancia=ancho;
			
			for(SiembraItem sPoly : areasIntersecciones.keySet()){
				Double gArea = areasIntersecciones.get(sPoly);
				if(gArea==null){
					continue;
				}
				double peso = gArea/areaItersectadaTotal;
				dosisHaProm+=sPoly.getDosisHa()*peso;
				dosisMLProm+=sPoly.getDosisML()*peso;
				dosisFertLineaProm+=sPoly.getDosisFertLinea()*peso;
				dosisFertCostadoProm+=sPoly.getDosisFertCostado()*peso;
				elev+=sPoly.getElevacion()*peso;
			}

			synchronized(labor){
				s = new SiembraItem();
				s.setId(labor.getNextID());
				labor.setPropiedadesLabor(s);
			}

			Geometry union2 = null;
			if(!rellenarHuecos) {				
				try{
					GeometryCollection colectionCat = GeometryHelper.toGeometryCollection(intersections);
					union2 = colectionCat.convexHull();
				}catch(Exception e){
					// Si falla, usar el polígono original
					union2 = poly;
				}
			} else { 
				union2 = poly;
			}
			
			s.setGeometry(union2);
			s.setDosisHa(dosisHaProm);
			s.setDosisML(dosisMLProm);
			s.setDosisFertLinea(dosisFertLineaProm);
			s.setDosisFertCostado(dosisFertCostadoProm);
			s.setAncho(ancho);
			s.setDistancia(distancia);
			s.setElevacion(elev);
		}
		return s;
	}

	private double getAreaMinimaLongLat() {
		return labor.getConfiguracion().supMinimaProperty().doubleValue()
				*ProyectionConstants.metersToLong()
				*ProyectionConstants.metersToLat();
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	public static List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		List<Polygon> polygons = new ArrayList<Polygon>();
		Position esq = bounds.getUpperCorner();
		ProyectionConstants.setLatitudCalculo(esq.getOrdinate(1));
		
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong() + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat() + ancho/2;
		Double x0=minX;
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;

				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());

				Coordinate[] coordinates = { A, B, C, D, A };
				GeometryFactory fact = new GeometryFactory();
				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);			
				polygons.add(poly);
			}
		}
		return polygons;
	}
	
	public void setRellenarHuecos(boolean rellenar) {
		this.rellenarHuecos=rellenar;
	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}

}
