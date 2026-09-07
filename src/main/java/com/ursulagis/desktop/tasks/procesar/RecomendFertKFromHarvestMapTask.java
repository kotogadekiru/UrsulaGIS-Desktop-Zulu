package com.ursulagis.desktop.tasks.procesar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.DoubleStream;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;

import java.util.logging.Logger;

/**
 * Recomendación de fertilización potásica (K) a partir de cosecha estimada,
 * descontando K disponible en suelo y fertilizaciones previas (misma lógica que N).
 */
public class RecomendFertKFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private static final Logger logger = Logger.getLogger(RecomendFertKFromHarvestMapTask.class.getName());

	private CosechaLabor cosecha;
	private List<Suelo> suelos;
	private List<FertilizacionLabor> fertilizaciones;
	private Double minFert=null;
	private Double maxFert=null;

	public RecomendFertKFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor cosechaEstimada, List<Suelo> _suelos, List<FertilizacionLabor> _fert) {
		super(labor);
		this.cosecha =cosechaEstimada;
		this.suelos=_suelos;
		this.fertilizaciones=_fert;
	}

	public void doProcess() {	
		try {
			featureCount = cosecha.outCollection.size();
			Cultivo cultivo = cosecha.getCultivo();
			Fertilizante fert = this.labor.fertilizante;

			List<CosechaItem> cItems = new ArrayList<CosechaItem>();
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
				cItems.add(ci);
			}
			reader.close();
			ForkJoinPool myPool = new ForkJoinPool(4);
			myPool.submit(() ->{

				cItems.parallelStream().forEach(cItem->{
					try {
						FertilizacionItem fi = new FertilizacionItem();			
						fi.setId(labor.getNextID());
						labor.setPropiedadesLabor(fi);
						Geometry geom = PolygonValidator.validate(cItem.getGeometry());
						if(geom==null) {
							logger.fine("item geom es null");
							return;
						}
						Double areaGeom =  ProyectionConstants.A_HAS(geom.getArea());
						if(areaGeom==0) {
							logger.fine("item areaGeom es 0");
							return;
						}
						fi.setGeometry(geom);
						double absK = cItem.getRindeTnHa()*cultivo.getAbsK();
						double dispKSuelo = getKDisponibleSuelo(geom) / areaGeom;
						double dispKFert = getKDisponibleFert(geom) / areaGeom;
						double kAAplicar= absK-dispKSuelo-dispKFert;
						if(Double.isNaN(kAAplicar)) {
							logger.fine("item kAAplicar es NaN");
							kAAplicar=0;
						}

						kAAplicar = Math.max(0, kAAplicar);
						double reposicionK = fert.getPorcK()>0 ? kAAplicar / (fert.getPorcK()/100) : 0;

						if(this.minFert != null && this.minFert>0 &&this.minFert > reposicionK) {
							reposicionK = minFert;
						}
						if(this.maxFert != null && this.maxFert>0 && this.maxFert < reposicionK) {
							reposicionK = maxFert;
						}
						fi.setDosistHa(reposicionK);
						fi.setElevacion(10d);
						labor.setPropiedadesLabor(fi);

						labor.insertFeature(fi);
						featureNumber++;
						updateProgress(featureNumber, featureCount);
					}catch(Exception e) {
						logger.warning("error al procesar el item "+cItem);
						e.printStackTrace();
					}
				});
			}
					).get();
			logger.fine("construyendo clasificador");
			labor.constructClasificador();
			runLater(getItemsList());
			updateProgress(0, featureCount);	
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	private Double getKDisponibleFert(Geometry geometry) {
		Double kgKFert = 0.0;
		kgKFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizante;
			return items.parallelStream().flatMapToDouble(item->{
				Double kgKHa = (Double) item.getDosistHa() * fertilizante.getPorcK()/100;
				Geometry fertGeom = item.getGeometry();				
				fertGeom = PolygonValidator.validate(fertGeom);
				if(fertGeom == null || !fertGeom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					Geometry inteseccionGeom = GeometryHelper.getIntersection(fertGeom, geometry);
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgKHa * area);				
			});
		}).sum();

		return kgKFert;
	}

	private Double getKDisponibleSuelo(Geometry geometry) {
		Double kgKSuelo = 0.0;
		kgKSuelo=	suelos.stream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				// K disponible 0-60 cm
				Double kgKHa= Suelo.ppmToKg(item.getDensAp(), item.getPpmK(), 0.6);

				Geometry geom = item.getGeometry();				
				geom = PolygonValidator.validate(geom);
				if(!geom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geom, geometry);
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgKHa * area);				
			});
		}).sum();

		return kgKSuelo;
	}

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 1000;
	}

	public void setMinFert(Double _minFert) {
		this.minFert=_minFert;

	}
	public void setMaxFert(Double _maxFert) {
		this.maxFert=_maxFert;

	}
}
