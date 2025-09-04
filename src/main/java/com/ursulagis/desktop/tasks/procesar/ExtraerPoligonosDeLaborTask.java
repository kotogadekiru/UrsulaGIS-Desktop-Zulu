package com.ursulagis.desktop.tasks.procesar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.precision.EnhancedPrecisionOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.Messages;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ExtraerPoligonosDeLaborTask extends Task<List<Poligono>> {
	private static final String TASK_CLOSE_ICON = Messages.getString("ExtraerPoligonosDeLaborTask.0"); //$NON-NLS-1$

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private Labor<?> labor;
	private double supMin = 0;
	public ExtraerPoligonosDeLaborTask(Labor<?> l) {
		this.labor = l;
		this.supMin = labor.getConfig().supMinimaProperty().get()/ProyectionConstants.METROS2_POR_HA;//5
		// PropertyHelper.initDoubleProperty(Cosecha.SUP_MINIMA_M2_KEY, "10", labor.getConfig());
		//Configuracion.getInstance().getPropertyOrDefault(CosechaLabor., def)
	}

	@Override
	protected List<Poligono> call() {
		try{
			List<Poligono> poligonos = new ArrayList<Poligono>();
			SimpleFeatureIterator it = labor.outCollection.features();
			List<LaborItem> items = new ArrayList<LaborItem>();
			while(it.hasNext()){
				LaborItem fi = labor.constructFeatureContainerStandar(it.next(),false);
				items.add(fi);
			}
			it.close();
			
			int featureCount=items.size();//labor.outCollection.size();
			
			if(featureCount>=100) {
				items = resumirGeometrias(labor);
				reabsorverZonasChicas(items);
			}
			
			int index =0;
			for(LaborItem next : items) {
			//while(it.hasNext()){
				updateProgress(index, featureCount);
				//SimpleFeature next = it.next();
				double has = ProyectionConstants.A_HAS(next.getGeometry().getArea());
				
				if(has>supMin) {//0.2){//cada poli mayor a 10m2
				Poligono poli = itemToPoligono(next);
				int cat = labor.getClasificador().getCategoryFor(next.getAmount());
				String catName = labor.getClasificador().getLetraCat(cat);
				poli.setNombre(labor.getNombre()+" "+catName); //$NON-NLS-1$
				GeometryFactory fact = ((Geometry)next.getGeometry()).getFactory();
				List<Position> positions = poli.getPositions();

				Coordinate[] coordinates = new Coordinate[positions.size()];
				for(int i =0;i<positions.size();i++){
					Position pos = positions.get(i);
					coordinates[i]=new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees);
				}
				Polygon p =fact.createPolygon(coordinates);
				//		p = (Polygon) JTS.smooth( p,1 );
				Geometry bp = p.getBoundary();
				poli.setArea(has);
					Coordinate[] finalCoords = bp.getCoordinates();
					poli.getPositions().clear();
					for(Coordinate c :finalCoords){//las coordenadas no estan ordenadas o tienen huecos
						poli.getPositions().add(Position.fromDegrees(c.y,c.x));
					}
					poligonos.add(poli);
				} else{
					System.out.println(Messages.getString("ExtraerPoligonosDeLaborTask.2")+has); //$NON-NLS-1$
				}
				//}
				index++;
			}
			return poligonos;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * metodo usado por las capas de siembra fertilizacion, pulverizacion y suelo para obtener los poligonos
	 * @param dao
	 * @return
	 */
	protected List<Polygon> getPolygons(Object geometry){
		List<Polygon> polygons = new ArrayList<Polygon>();

		if (geometry instanceof Geometry) {		

			Geometry mp = (Geometry) geometry;
			if( mp.getNumGeometries()>1 || geometry instanceof MultiPolygon){
				for (int i = 0; i < mp.getNumGeometries() ; i++) {
					Geometry g = mp.getGeometryN(i);
					polygons.addAll(getPolygons(g));//recursion			
				}

			} else {
				polygons.add((Polygon) mp);//org.locationtech.jts.geom.MultiPolygon cannot be cast to org.locationtech.jts.geom.Polygon
			}
		}
		return polygons;
	}
	
	
//	public static Poligono geometryToPoligonoNOt(Geometry g) {
//		if(g instanceof Geometry){						
//			Geometry mainBoundary = ((Geometry) g).getBoundary();
//			if(mainBoundary.getNumGeometries() == 0)return null;
//			Geometry seed = mainBoundary.getGeometryN(0);
//			List<Position> iterable = geometryToPositions(seed);
//			//iterable tiene las posiciones de la geometria 0 o contorno
//			for(int n = 1; n < mainBoundary.getNumGeometries(); n++){//recorro las otras geometrias uniendolas a iterable en el punto mas cercano
//				//Geometry toAdd =mainBoundary.getGeometryN(n);// mp.getGeometryN(0);
//				//Coordinate[] cToAdd= toAdd.getCoordinates();
//				List<Position> posNToAdd = geometryToPositions(mainBoundary.getGeometryN(n));
//				//1 buscar los puntos de cada una de las geometrias que esten mas cerca
//				int minIt=0;
//				int minCoord=0;
//				double minDistance=-1;
//				for(int i=0;i < iterable.size();i++){
//					Position itPos = iterable.get(i);					
//					for(int j=0;j<posNToAdd.size();j++){
//						
//						Position pToAdd = posNToAdd.get(j);
//						double dist = Position.linearDistance(pToAdd,itPos ).degrees;
//						if(minDistance<0 || dist<minDistance){
//							minDistance=dist;
//							minIt=i;
//							minCoord=j;												
//						}
//					}
//				}			
//				Position p0 = iterable.get(minIt);
//				Position p1 = posNToAdd.get(minCoord);
//				LineSegment ls=new LineSegment(p0.longitude.degrees,p0.latitude.degrees,p1.longitude.degrees,p1.latitude.degrees);
//				GeometryFactory fact = ProyectionConstants.getGeometryFactory();
//				Geometry conector = ls.toGeometry(fact).buffer(ProyectionConstants.metersToLongLat(0.25),3,BufferParameters.CAP_FLAT);
//				
//				GeometryCollection collection = fact.createGeometryCollection(new Geometry[] {seed,conector,mainBoundary.getGeometryN(n)});
//				seed = collection.buffer(ProyectionConstants.metersToLongLat(0.25));
//				iterable = geometryToPositions(seed);
//			}
//			Poligono poli = new Poligono();
//			poli.setPositions(iterable);
//			g=poli.toGeometry();
////			try{	
////				g = TopologyPreservingSimplifier.simplify(g, ProyectionConstants.metersToLongLat(10));
////				g =g.buffer(0);
////				for(Coordinate c : g.getBoundary().getCoordinates()){
////					iterable.add(Position.fromDegrees(c.y, c.x));							
////				}
////				poli.setPositions(iterable);
////			}catch(Exception e){
////				e.printStackTrace();
////			}
//			
//			double has = ProyectionConstants.A_HAS(g.getArea());
//			poli.setArea(has);
//			return poli;
//		} else {return null;}
//	}
	
	public static Poligono geometryToPoligono(Geometry g){			
		//Object g=feature.getDefaultGeometry();
		
		if(g instanceof Geometry){						
			Geometry mainBoundary = ((Geometry) g).getBoundary();
			if(mainBoundary.getNumGeometries() == 0)return null;
			Geometry seed = mainBoundary.getGeometryN(0);
			List<Position> iterable = geometryToPositions(seed);
			//iterable tiene las posiciones de la geometrya 0 o contorno
			for(int n = 1; n < mainBoundary.getNumGeometries(); n++){//recooro las otras geometrias uniendolas a iterable en el punto mas cercano
				//Geometry toAdd =mainBoundary.getGeometryN(n);// mp.getGeometryN(0);
				//Coordinate[] cToAdd= toAdd.getCoordinates();
				List<Position> posNToAdd = geometryToPositions(mainBoundary.getGeometryN(n));
				//1 buscar los puntos de cada una de las geometrias que esten mas cerca
				int minIt=0;
				int minCoord=0;
				double minDistance=-1;
				for(int i=0;i < iterable.size();i++){
					Position itPos = iterable.get(i);
					
					for(int j=0;j<posNToAdd.size();j++){
						
						Position pToAdd = posNToAdd.get(j);
						double dist = Position.linearDistance(pToAdd,itPos ).degrees;
						if(minDistance<0 || dist<minDistance){
							minDistance=dist;
							minIt=i;
							minCoord=j;												
						}
					}
				}			
				List<Position> arrayToInsert=new ArrayList<Position>();
				//insertar en iterable position minIt las coordenadas de ToAdd empezando por minCoord
				for(int j=0; j<posNToAdd.size(); j++){//empiezo en j=0
					int index = (minCoord+j)%posNToAdd.size();//recorro circularmente a partir del indice minCoord

					//Coordinate c = //cToAdd[index];//para recorrer hasta cero al final
					Position pToAdd = posNToAdd.get(index);// Position.fromDegrees(c.y, c.x);
					//int finalIndex = minIt+1+j;
					//iterable.add(finalIndex, pToAdd);
					if(!arrayToInsert.contains(pToAdd)) {
						arrayToInsert.add(pToAdd);//evito duplicar el principio y el fin del loop
					} else {
						//System.out.println("quitando el inicio");
					}
				}

				double halfWidth = ProyectionConstants.metersToLongLat(0.25);
				
				Position firstInsert = arrayToInsert.get(0);
				Position antesfirstInsert = arrayToInsert.get(arrayToInsert.size()-1);//busco el ultimo
				Angle dist = Position.linearDistance(firstInsert, antesfirstInsert);
				
				firstInsert = Position.interpolate(halfWidth/dist.degrees, firstInsert, antesfirstInsert);//retrocedo 1%				
				arrayToInsert.add(firstInsert);//cierro el loop				
				
				firstInsert = arrayToInsert.get(0);
				Position despuesfirstInsert = arrayToInsert.get(1);//busco el ultimo
				dist = Position.linearDistance(firstInsert, despuesfirstInsert);
				firstInsert = Position.interpolate(halfWidth/dist.degrees, firstInsert, despuesfirstInsert);//retrocedo 1%				
				arrayToInsert.set(0,firstInsert);//cierro el loop
				
				Position lastInsert = iterable.get(minIt);
				Position despueslastInsert = iterable.get(minIt+1);
				dist = Position.linearDistance(lastInsert, despueslastInsert);				
				lastInsert = Position.interpolate(halfWidth/dist.degrees, lastInsert, despueslastInsert);//retrocedo 1%		
				arrayToInsert.add(lastInsert);
								
				iterable.addAll(minIt+1, arrayToInsert);								
			}
			//TODO simplificar iterable si los puntos estan muy cerca

			Poligono poli = new Poligono();
			poli.setPositions(iterable);

			try{	
				g = TopologyPreservingSimplifier.simplify(poli.toGeometry(), ProyectionConstants.metersToLongLat(0.25));
				//g =g.buffer(0);		
				poli.setPositions(geometryToPositions(g));
			}catch(Exception e){
				e.printStackTrace();
			}
			
			double has = ProyectionConstants.A_HAS(g.getArea());
			poli.setArea(has);
			return poli;
		} else {return null;}
	}

	private static List<Position>  geometryToPositions( Geometry seed) {
		List<Position> iterable=new ArrayList<Position>();
		Coordinate[] coordinates = seed.getCoordinates();
		for(Coordinate c : coordinates){
			iterable.add(Position.fromDegrees(c.y, c.x));							
		}
		return iterable;
	}

	public static Poligono itemToPoligono(LaborItem feature){			
		Object g=feature.getGeometry();
		if(g instanceof Geometry){
			return ExtraerPoligonosDeLaborTask.geometryToPoligono((Geometry)g);
		} else {return null;}
	}
	
	public static Poligono featureToPoligono(SimpleFeature feature){			
		Object g=feature.getDefaultGeometry();
		if(g instanceof Geometry){
			return ExtraerPoligonosDeLaborTask.geometryToPoligono((Geometry)g);
		} else {return null;}
	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label(labor.getNombre());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println(Messages.getString("ExtraerPoligonosDeLaborTask.3")); //$NON-NLS-1$
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}

	public void uninstallProgressBar() {
		progressPane.getChildren().remove(progressContainer);
	}
	
	private  List<LaborItem> resumirGeometrias(Labor labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<SimpleFeature>> colections = new ArrayList<List<SimpleFeature>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			colections.add(i, new ArrayList<SimpleFeature>());
		}
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			LaborItem ci = labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			colections.get(cat).add(f);
		}
		it.close();
		updateProgress(1, 100);
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
		//TODO pasar esto a parallel streams
		//XXX por cada categoria 
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();
			updateProgress(i+1, labor.clasificador.getNumClasses());
			//	Geometry slowUnion = null;
			Double sumRinde=0.0;
			Double sumatoriaAltura=0.0;
			int n=0;
			for(SimpleFeature f : colections.get(i)){//por cada item de la categoria i
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(labor.getColAmount().get()));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;
			
			double sumaDesvio2 = 0.0;
			for(SimpleFeature f:colections.get(i)){
				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(labor.getColAmount().get()));	
				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
			}
			
			double desvioPromedio = sumaDesvio2/n;
			if(n>0){//si no hay ningun feature en esa categoria esto da out of bounds
				GeometryFactory fact = geometriesCat.get(0).getFactory();
				Geometry[] geomArray = new Geometry[geometriesCat.size()];
				GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));

				Geometry buffered = null;
				double bufer= ProyectionConstants.metersToLongLat(0.25);
				try{
					buffered = colectionCat.union();
					buffered =buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
					buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}

				SimpleFeature fIn = colections.get(i).get(0);
				//TODO recorrer buffered y crear una feature por cada geometria de la geometry collection
				for(int igeom=0;buffered!=null && igeom<buffered.getNumGeometries();igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
					Geometry g = buffered.getGeometryN(igeom);
					fIn.setAttribute(labor.getColAmount().get(), rindeProm);
					LaborItem ci=labor.constructFeatureContainerStandar(fIn,true);
					//ci.set(rindeProm);
					//ci.setDesvioRinde(desvioPromedio);
					ci.setElevacion(elevProm);

					ci.setGeometry(g);

					itemsCategoria.add(ci);
					//SimpleFeature f = ci.getFeature(labor.featureBuilder);
					//boolean res = newOutcollection.add(f);
				}
			}	

		}//termino de recorrer las categorias
		//labor.setOutCollection(newOutcollection);
		//FIXME esto las resume pero no garantiza que sean menos de 100
		return itemsCategoria;
	}
	
	public void reabsorverZonasChicas( List<LaborItem> items) {
		if(items.size()<100)return;
		//TODO reabsorver zonas mas chicas a las mas grandes vecinas
		System.out.println("tiene mas de 100 zonas, reabsorviendo..."); //$NON-NLS-1$
		//TODO tomar las 100 zonas mas grandes y reabsorver las otras en estas

	

		items.sort((i1,i2)
				->	(-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea())));	
		int limit = Math.min(100,items.size());
		List<LaborItem> itemsAgrandar =items.subList(0,limit);
		Quadtree tree=new Quadtree();
		for(LaborItem ar : itemsAgrandar) {
			Geometry gAr =ar.getGeometry();
			tree.insert(gAr.getEnvelopeInternal(), ar);
		}
		List<LaborItem> itemsAReducir =items.subList(limit, items.size());
		int n=0;
		int i=itemsAReducir.size();
		super.updateTitle("reabsorver zonas chicas");
		updateProgress(0, i);
		while(itemsAReducir.size() > 0 || n > 100) {//trato de reducirlos 10 veces
			List<LaborItem> done = new ArrayList<LaborItem>();		
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr = ar.getGeometry();
				List<LaborItem> vecinos = (List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

				if(vecinos.size()>0) {
					Optional<LaborItem> opV = vecinos.stream().reduce((v1,v2)->{
						boolean v1i = gAr.intersects(v1.getGeometry());
						boolean v2i = gAr.intersects(v2.getGeometry());
						return (v1i && v2i) 
								? (v1.getGeometry().getArea() > v2.getGeometry().getArea() ? v1 : v2) 
								: (v1i ? v1 : v2);
					});
					if(opV.isPresent()) {
						LaborItem v = opV.get();
						Geometry g = v.getGeometry();
						tree.remove(g.getEnvelopeInternal(), v);
						Geometry union = g.union(gAr);
						v.setGeometry(union);
						tree.insert(union.getEnvelopeInternal(), v);
						done.add(ar);
					}
				}
				updateProgress(done.size(),itemsAReducir.size());
			}
			updateProgress(i-itemsAReducir.size(),i);
			n++;
			itemsAReducir.removeAll(done);
		}
		
		items.clear();
		items.addAll((List<LaborItem>)tree.queryAll());
	}


}
