package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;


import java.util.logging.Logger;
/**
 * task que genera una siembra con dosis fija a partir de un poligono
 * @author quero
 *
 */
public class ConvertirASiembraTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	private static final Logger logger = Logger.getLogger(ConvertirASiembraTask.class.getName());

	Map<String,Double[]> plantasM2ObjetivoMap = null;//0.0;
	CosechaLabor cosecha=null;

	public ConvertirASiembraTask(CosechaLabor _cosecha,SiembraLabor labor,Map<String,Double[]> valores){
		super(labor);
		plantasM2ObjetivoMap=valores;
		cosecha=_cosecha;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(cosecha.getContorno());
		Semilla semilla = labor.getSemilla();
		logger.fine("semilla es "+semilla);
		double entresurco = labor.getEntreSurco();
		double pmil = semilla.getPesoDeMil();
		double pg = semilla.getPG();
		double metrosLinealesHa = ProyectionConstants.METROS2_POR_HA/entresurco;//23809 a 0.42
		//System.out.println("metrosLinealesHa "+metrosLinealesHa);//metrosLinealesHa 52631.57894736842 ok!
		//double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas


		//System.out.println("semillasMetroLineal "+semillasMetroLineal);//semillasMetroLineal 38.0 ok!
		//List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		Clasificador cl = cosecha.getClasificador();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);			
			String nombre = cl.getLetraCat(cl.getCategoryFor(ci.getRindeTnHa()));
			double plantasM2Objetivo = plantasM2ObjetivoMap.get(nombre)[0];
			double fertL = plantasM2ObjetivoMap.get(nombre)[1];
			double fertC = plantasM2ObjetivoMap.get(nombre)[2];
			double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas
			double semillasMetroLineal = semillasHa/metrosLinealesHa;//si es trigo va en plantas /m2 si es maiz o soja va en miles de plantas por ha

			SiembraItem si = new SiembraItem();

			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
			si.setDosisFertLinea(fertL);
			si.setDosisFertCostado(fertC);
			si.setDosisML(semillasMetroLineal);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg

			labor.setPropiedadesLabor(si);

			si.setGeometry(ci.getGeometry());			
			si.setId(labor.getNextID());
			si.setElevacion(10.0);
			labor.insertFeature(si);
		}
		reader.close();

		//		for(Poligono pol : this.polis) {
		//			SiembraItem si = new SiembraItem();
		//					
		//			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
		//
		//			si.setDosisML(semillasMetroLineal);
		//			//dosis sembradora va en semillas cada 10mts
		//			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg
		//			
		//			labor.setPropiedadesLabor(si);
		//
		//			si.setGeometry(pol.toGeometry());
		//			si.setId(labor.getNextID());
		//
		//			labor.insertFeature(si);
		//		}
		labor.constructClasificador();
		labor.markInternalDosisAsKgHa();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
	}
	
	public static String buildTooltipText(SiembraItem siembraFeature, double area) {
		return buildTooltipText(siembraFeature, area, (SiembraLabor) siembraFeature.getLabor());
	}

	public static String buildTooltipText(SiembraItem siembraFeature, double area, SiembraLabor labor) {
		NumberFormat df = Messages.getNumberFormat();

		//densidad seeds/metro lineal
		String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.density")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.seedmL")); //$NON-NLS-1$ //$NON-NLS-2$

		Double seedsM2 = null;
		Double entreSurco = labor != null ? labor.getEntreSurco() : null;
		if (entreSurco != null && entreSurco > 0) {
			seedsM2 = siembraFeature.getDosisML() / entreSurco;
		} else if (labor != null && labor.getSemilla() != null
				&& labor.getSemilla().getPesoDeMil() != null
				&& labor.getSemilla().getPesoDeMil() > 0
				&& siembraFeature.getDosisHa() != null) {
			double kgM2 = siembraFeature.getDosisHa() / ProyectionConstants.METROS2_POR_HA;
			seedsM2 = (1000.0 * 1000.0 * kgM2) / labor.getSemilla().getPesoDeMil();
		} else {
			logger.fine("No se pudo calcular Sem/m²; entreSurco=" + entreSurco);
		}
		if (seedsM2 != null) {
			tooltipText = tooltipText.concat(
					df.format(seedsM2) + " "
							+ Messages.getString("SiembraConfigDialogController.plaMetroCuadrado") + "\n");
		}
		//kg semillas por ha
		tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.kg") + df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.kgHa")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert l y c		
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertL") +": "+ df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.fertKgHa")		); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertC") +": "+ df.format(siembraFeature.getDosisFertCostado()) + Messages.getString("ProcessSiembraMapTask.fertKgHa")		); //$NON-NLS-1$ //$NON-NLS-2$
		//fert costo
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.cost") + df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.costHa")		); //$NON-NLS-1$ //$NON-NLS-2$

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.sfc")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.m2")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.sfc")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.has")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return tooltipText;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task