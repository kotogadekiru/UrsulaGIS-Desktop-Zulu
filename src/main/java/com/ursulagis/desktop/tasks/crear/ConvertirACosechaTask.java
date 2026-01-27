package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.util.Map;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.tasks.ProcessMapTask;

/**
 * task que convierte una cosecha a otra cosecha asignando valores por clase
 * @author quero
 *
 */
public class ConvertirACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Map<String,Double[]> rindeMap = null;
	CosechaLabor cosechaOrigen=null;

	public ConvertirACosechaTask(CosechaLabor _cosechaOrigen,CosechaLabor laborDestino,Map<String,Double[]> valores){
		super(laborDestino);
		rindeMap=valores;
		cosechaOrigen=_cosechaOrigen;
	}

	public void doProcess() throws IOException {
		this.featureCount=cosechaOrigen.outCollection.size();
		this.featureNumber=0;
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosechaOrigen.outCollection.reader();
		Clasificador cl = cosechaOrigen.getClasificador();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ciOrigen = cosechaOrigen.constructFeatureContainerStandar(simpleFeature,false);			
			String nombre = cl.getLetraCat(cl.getCategoryFor(ciOrigen.getRindeTnHa()));
			Double rindeNuevo = rindeMap.get(nombre)[0];
		
			CosechaItem ciNuevo = new CosechaItem();
			ciNuevo.setRindeTnHa(rindeNuevo);
			ciNuevo.setDesvioRinde(ciOrigen.getDesvioRinde()); // Mantener el desvío original
			labor.setPropiedadesLabor(ciNuevo);
			ciNuevo.setGeometry(ciOrigen.getGeometry());
			ciNuevo.setId(labor.getNextID());
			ciNuevo.setElevacion(ciOrigen.getElevacion());
			ciNuevo.setAncho(ciOrigen.getAncho());
			ciNuevo.setDistancia(ciOrigen.getDistancia());
			ciNuevo.setRumbo(ciOrigen.getRumbo());
			labor.insertFeature(ciNuevo);
			this.updateProgress(featureNumber++, featureCount);
		}
		reader.close();		

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task
