package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.ReferencedEnvelope;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.EnhancedPrecisionOp;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.util.GeometricShapeFactory;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Position.PositionList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.util.measure.MeasureTool;
import com.ursulagis.desktop.gui.PoligonLayerFactory;
import com.ursulagis.desktop.tasks.procesar.ExtraerPoligonosDeLaborTask;

import java.util.logging.Logger;
public class GeometryHelper {
	private static final Logger logger = Logger.getLogger(GeometryHelper.class.getName());

	
	/**
	 * metodo que une los poligonos mostrados como medicion de area
	 * @param pActivos
	 * @return
	 */
	public static Poligono unirPoligonos(List<Poligono> pActivos) {

		StringJoiner joiner = new StringJoiner("-");
		//joiner.add(Messages.getString("JFXMain.poligonUnionNamePrefixText"));

		List<Geometry> gActivas = pActivos.stream()
			.flatMap(p -> {
				joiner.add(p.getNombre());
				return geometriesFromPoligono(p).stream();
			}).collect(Collectors.toList());


		Geometry union = GeometryHelper.unirGeometrias(gActivas);

		double has = ProyectionConstants.A_HAS(union.getArea());

		Poligono poli = constructPoligono(union);//ExtraerPoligonosDeLaborTask.geometryToPoligono(union);
		poli.setArea(has);
		poli.setNombre(joiner.toString()); //-NLS-1$
		return poli;
	}

	public static Geometry splineInterpolation(Geometry g) {
		List<Float> X = new ArrayList<Float>();
		List<Float> Y = new ArrayList<Float>();
		for(Coordinate c: g.getCoordinates()) {
			X.add(new Float(c.x));
			Y.add(new Float(c.y));
		}
		SplineInterpolator spline = SplineInterpolator.createMonotoneCubicSpline(X, Y);
		List<Coordinate> lerps = new ArrayList<Coordinate>();
		for(Float x: X) {
			Float y = spline.interpolate(x);
			Coordinate c = new Coordinate(x,y);
			X.add(new Float(c.x));
			Y.add(new Float(c.y));
			lerps.add(c);
		}
		Geometry ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}

	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry removeClosePoints(Geometry g, Double minDistance) {
		Geometry ret=null;

		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);
		boolean changed = false;
		//	do {
		changed = false;
		for(int i=1;i<boundCoords.length;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			Coordinate c1 = boundCoords[i];			

			if(c0.distance(c1)>minDistance) {
				vertices.add(c1);
			} else {

				changed=true;
			}
		}
		//	boundCoords=vertices.toArray(new Coordinate[vertices.size()]);
		//	}while(changed);
		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry removeSinglePoints(Geometry g, Double minDistance) {
		Geometry ret=null;

		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);	
		logger.fine("bounds size ="+boundCoords.length);
		for(int i=1;i<boundCoords.length-1;i++) {	//busco i+1 asi que esta bien cortar en length-1
			Coordinate c0 = vertices.get(vertices.size()-1);//last

			Coordinate c1 = boundCoords[i];
			logger.fine("i: "+i+" "+c1);
			Coordinate c2 = boundCoords[i+1];		
			if(c0.distance(c2)>minDistance) {
				logger.fine(" agregando "+ c1);
				vertices.add(c1);
			}
		}
		//vertices.add(boundCoords[boundCoords.length-1]);//el ultimo vertice siempre va. 
		//System.out.println("n-1="+vertices.get(vertices.size()-1));
		//aunque deberia ser el mismo que el primero
		vertices.add(vertices.get(0));//Cerrar el ciclo
		logger.fine("n="+vertices.get(vertices.size()-1));
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry reduceAlignedPoints(Geometry g, Double maxError) {
		Geometry ret=null;
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);

		List<Coordinate> segmentCandidates = new ArrayList<Coordinate>();

		for(int i=1;i<boundCoords.length;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			Coordinate c1 = boundCoords[i];			

			//TODO add c1 to segmentCandidates and check the condition else pop c1			

			Coordinate[] candidatesArr =segmentCandidates.toArray(new Coordinate[segmentCandidates.size()]);

			Coordinate[] refCoords =new Coordinate[]{c0,c1}; //segmento de referencia

			LineString ls= fact.createLineString(refCoords);
			Double distances =0.0;

			for(Coordinate c:candidatesArr) {				
				distances+=	ls.distance(fact.createPoint(c));
			}
			if(maxError>(distances/ls.getLength())) {
				segmentCandidates.add(c1);				
			} else { 
				vertices.add(segmentCandidates.get(segmentCandidates.size()-1));
				segmentCandidates.clear();
				segmentCandidates.add(c1);			
			}
		}
		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * 
	 * @param g geometria a simplificar
	 * @param ratio el error tiene que ser menor a r
	 * @return la geometria simplificada
	 */
	public static Geometry lerpIf(Geometry g, Double ratio) {
		Geometry ret=null;

		List<Coordinate> lerps = new ArrayList<Coordinate>();
		//TODO smooth edges in geometry
		Coordinate[] coords = g.getCoordinates();

		for(int i=0;i<coords.length;i++) {		//FIXME for mal formado	
			int c0Index =i-1>=0?i-1:coords.length-1; 
			Coordinate c0p = coords[c0Index];//i-1/3
			if(lerps.size()>0) {
				c0p=lerps.get(lerps.size()-1);
			}

			Coordinate c1 = coords[i];

			//c2 coordenada intermedia creada para que la derivada sea continua
			Coordinate c2 = new Coordinate();
			c2.x=c1.x+(c1.x-c0p.x)/3;
			c2.y=c1.y+(c1.y-c0p.y)/3;

			int c4Index =i+1<coords.length?i+1:0; 
			Coordinate c4 = coords[c4Index];

			int c5Index =i+2<coords.length?i+2:i+2-(coords.length); 
			Coordinate c5p = coords[c5Index];

			logger.fine("i:"+i+" ["+c0Index+","+i+","+c4Index+","+c5Index+"]");
			//c3 coordenada intemedia para que la derivada sea continua
			Coordinate c3 = new Coordinate();
			c3.x=c4.x-(c5p.x-c4.x)/3;
			c3.y=c4.y-(c5p.y-c4.y)/3;
			//TODO si los puntos ya estan alineados no interpolar
			Coordinate[] candidatesArr =new Coordinate[]{c0p,c1,c2,c3,c4,c5p};
			Coordinate[] refCoords =new Coordinate[]{c1,c4}; //segmento de referencia
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();
			LineString ls= fact.createLineString(refCoords);
			Double distances =0.0;

			for(Coordinate c:candidatesArr) {				
				distances+=	ls.distance(fact.createPoint(c));
			}
			if(ratio>(distances/ls.getLength())) {
				lerps.add(c1);
			} else { 
				List<Coordinate> candidates = new ArrayList<Coordinate>();
				//t es la coordenada del punto intermedio
				for(double t=0 ;t<=5;t++) {                      
					candidates.add(cubicLerp(c1, c2, c3, c4, t/5));
				}
				//TODO check candidates for ratio of error or input the original values

				distances =0.0;
				for(Coordinate c:candidates) {
					distances+=	ls.distance(fact.createPoint(c));
				}
				if(ratio>(distances/ls.getLength())) {
					logger.fine("agregando lerps");
					lerps.addAll(candidates);
				} else { 
					logger.fine("el error es muy grande no interpolando. r = "+distances/ls.getLength());
					lerps.add(c1);
				}
			}
			//c1=lerps.get(lerps.size()-1);
			//lerps.add(c3);

		}
		lerps.add(lerps.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}

	public static Geometry simplificarContorno(Geometry g) {
		//double toleranciaLongLat = (0.005)/ProyectionConstants.A_HAS();
	//	g=GeometryHelper.removeSmallTriangles(g, toleranciaLongLat);

		g=GeometryHelper.douglassPeuckerSimplify(g,ProyectionConstants.metersToLongLat(5));
		//g=g.buffer(ProyectionConstants.metersToLongLat(10));
		//		g=GeometryHelper.removeClosePoints(g, ProyectionConstants.metersToLongLat(2));
		//		g=GeometryHelper.removeSinglePoints(g, ProyectionConstants.metersToLongLat(2));
		//		g=GeometryHelper.reduceAlignedPoints(g, 0.2);
		return g;
	}
	public static Geometry smooth(Geometry g) {
		Geometry ret=null;
		//TODO remove duplicate vertices
		//TODO usar las coordenadas de la geometria como puntos de control para popular los puntos intermedios
		//de una spline 
		//TODO populate with vertices every X distance


		List<Coordinate> lerps = new ArrayList<Coordinate>();
		//TODO smooth edges in geometry
		Coordinate[] coords = g.getCoordinates();

		for(int i=0;i<coords.length;i++) {//FIXME for mal formado
			Coordinate c1 = coords[i];
			int c0Index =i-1>=0?i-1:coords.length-1; 
			Coordinate c0p = coords[c0Index];//i-1/3
			if(lerps.size()>0) {
				c0p=lerps.get(lerps.size()-1);
			}
			Coordinate c2 = new Coordinate();
			c2.x=c1.x+(c1.x-c0p.x)/3;
			c2.y=c1.y+(c1.y-c0p.y)/3;

			int c4Index =i+1<coords.length?i+1:0; 
			Coordinate c4 = coords[c4Index];

			int c5Index =i+2<coords.length?i+2:i+2-(coords.length); 
			Coordinate c5p = coords[c5Index];

			logger.fine("i:"+i+" ["+c0Index+","+i+","+c4Index+","+c5Index+"]");
			Coordinate c3 = new Coordinate();
			c3.x=c4.x-(c5p.x-c4.x)/3;
			c3.y=c4.y-(c5p.y-c4.y)/3;

			//ProyectionConstants.setLatitudCalculo(c1.y);
			//double d10 = ProyectionConstants.metersToLongLat(10);
			//double dist = c1.distance(c2)+c2.distance(c3);
			//lerps.add(c1);
			for(double t=0 ;t<=5;t++) {
				//t=Math.min(t, 1);
				//lerps.add(lerp(c1,c2,c3,t/5));
				lerps.add(cubicLerp(c1, c2, c3, c4, t/5));
			}
			c1=lerps.get(lerps.size()-1);
			//lerps.add(c3);

		}
		lerps.add(lerps.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}


	public static Coordinate cubicLerp(Coordinate c1,Coordinate c2,Coordinate c3,Coordinate c4,double t) {
		double P0x=c1.x,P1x=c2.x,P2x=c3.x,P3x=c4.x;
		double P0y=c1.y,P1y=c2.y,P2y=c3.y,P3y=c4.y;
		Coordinate ret = new Coordinate();
		//		ret.x=P0x
		//				+t*(-3*P0x+3*P1x)
		//				+t*t*(3*P0x-6*P1x+3*P2x)
		//				+t*t*t*(-P0x+3*P1x-3*P2x+P3x);

		ret.x=P0x
				+t*3*(-P0x+P1x)
				+t*t*(3*P0x-6*P1x+3*P2x)
				+t*t*t*(-P0x+3*P1x-3*P2x+P3x);
		ret.y=P0y
				+t*(-3*P0y+3*P1y)
				+t*t*(3*P0y-6*P1y+3*P2y)
				+t*t*t*(-P0y+3*P1y-3*P2y+P3y);
		return ret;
	}

	public static Coordinate lerp(Coordinate c1,Coordinate c2,Coordinate c3,double t) {
		Coordinate c12 =lerp(c1,c2,t);
		Coordinate c23 =lerp(c2,c3,t);
		return lerp(c12,c23,t);
	}
	public static Coordinate lerp(Coordinate c1,Coordinate c2,double t) {
		//TODO sumarle a c1 un porcentaje t de su diferencia con c2
		double deltaX = c2.x-c1.x;
		double deltaY = c2.y-c1.y;
		return new Coordinate(c1.x+deltaX*t,c1.y+deltaY*t);
	}

	public static Geometry createCircle(Point c,Point c2) {	
		Double radius = ProyectionConstants.getDistancia(c, c2);
		return createCircle (c,radius);
	}

	public static Geometry createCircle(Point c,double radius) {
		logger.fine("creando un circulo con radio "+radius);
		double latRadius = ProyectionConstants.metersToLat()*radius;

		double fact = ProyectionConstants.metersToLat()/ProyectionConstants.metersToLong();

		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(64); // adjustable
		shapeFactory.setCentre(c.getCoordinate());
		// Length in meters of 1� of latitude = always 111.32 km
		shapeFactory.setHeight(2*latRadius);//diameterInMeters/111320d);

		double longRadius = latRadius/fact;
		// Length in meters of 1� of longitude = 40075 km * cos( latitude ) / 360
		shapeFactory.setWidth(2*longRadius);//diameterInMeters / (40075000 * Math.cos(Math.toRadians(latitude)) / 360));

		Polygon circle = shapeFactory.createEllipse();

		return circle;//c.buffer(radius);//esto me genera una elipse
	}

	public static Polygon constructPolygon(ReferencedEnvelope e) {
		Coordinate D = new Coordinate(e.getMaxX(), e.getMaxY()); // x-l-d
		Coordinate C = new Coordinate(e.getMinX(), e.getMaxY());// X+l-d
		Coordinate B = new Coordinate(e.getMaxX(), e.getMinY());// X+l+d
		Coordinate A = new Coordinate(e.getMinX(), e.getMinY());// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, C, D, B, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon poly = fact.createPolygon(coordinates);
		return poly;
	}

	/**
	 * 
	 * @param l
	 * @param d
	 * @param X
	 * @return devuelve un poligono con centro en X y expandido en l y d
	 */
	public static Polygon constructPolygon(Coordinate l, Coordinate d, Point X) {
		double x = X.getX();
		double y = X.getY();

		Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y); // x-l-d
		Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y);// X+l-d
		Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y);// X+l+d
		Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y);// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = X.getFactory();

		//		LinearRing shell = fact.createLinearRing(coordinates);
		//		LinearRing[] holes = null;
		//		Polygon poly = new Polygon(shell, holes, fact);
		Polygon poly = fact.createPolygon(coordinates);

		return poly;
	}

	/**
	 * 
	 * @param deltaX cateto X
	 * @param deltaY cateto Y
	 * @return devuelve el angulo en grados; null si no se puede calcular
	 */
	public static Double getAzimuth(double deltaX,double deltaY) {
		Double rumbo = null;
		if(deltaX==0) {
			if(deltaY>0) {
				return 0.0;
			} else if (deltaY==0) {
				return null;//no hay angulo para 0,0
			}else {return 180.0;}
		}
		double tan = deltaY/deltaX;//+Math.PI/2;
		rumbo = Math.atan(tan);
		rumbo = Math.toDegrees(rumbo);//como esto me da entre -90 y 90 le sumo 90 para que me de entre 0 180

		rumbo=90-rumbo;
		return rumbo<0?rumbo+360:rumbo;
		//return rumbo;
	}

	public static List<Position>  geometryToPositions( Geometry seed) {
		List<Position> iterable=new ArrayList<Position>();
		Coordinate[] coordinates = seed.getCoordinates();
		for(Coordinate c : coordinates){
			iterable.add(Position.fromDegrees(c.y, c.x));							
		}
		return iterable;
	}

	public static Poligono constructPoligono(Geometry g) {
		return ExtraerPoligonosDeLaborTask.geometryToPoligono((Geometry)g);
	}

	public static Poligono geometryToPoligono(Geometry g,Poligono poli){					
			Geometry mainBoundary = ((Geometry) g).getBoundary();
			if(mainBoundary.getNumGeometries() == 0)return null;
			Geometry seed = mainBoundary.getGeometryN(0);
			List<Position> shell = GeometryHelper.geometryToPositions(seed);
			List<List<Position>> holes = new ArrayList<List<Position>>();
			//iterable tiene las posiciones de la geometrya 0 o contorno
			for(int n = 1; n < mainBoundary.getNumGeometries(); n++){//recooro las otras geometrias uniendolas a iterable en el punto mas cercano

				List<Position> posNToAdd = GeometryHelper.geometryToPositions(mainBoundary.getGeometryN(n));
				holes.add(posNToAdd);
			}			
			if(poli==null) {poli=new Poligono();}
			//poli = new Poligono();
			poli.setPositions(shell);
			poli.setHuecos(holes);
			
			double has = ProyectionConstants.A_HAS(g.getArea());
			poli.setArea(has);
			return poli;		
	}
	
	@Deprecated //no maneja el caso de multipoligon
	public static Poligono constructPoligonoOld(Geometry g) {
		//ExtraerPoligonosDeLaborTask.geometryToPoligono((Geometry)g);
		logger.fine("convirtiendo geometria a poligono "+g);		
		List<Position> positions = new ArrayList<Position>();		

		if(g instanceof Polygon) {
			Polygon pol =(Polygon)g;
			logger.fine("es polygon");

			Coordinate[] coords = pol.getExteriorRing().getCoordinates();
			for(int i=0;i<coords.length;i++) {
				Coordinate c = coords[i];
				positions.add(Position.fromDegrees(c.y, c.x));
			}
			positions.add(positions.get(0));


			for(int r=0;r<pol.getNumInteriorRing();r++) {
				List<Position> hole =new ArrayList<Position>();	
				LineString ring = pol.getInteriorRingN(r);
				Coordinate[] ringCoords = ring.reverse().getCoordinates();
				for(int i=0;i<ringCoords.length;i++) {
					Coordinate c = ringCoords[i];
					hole.add(Position.fromDegrees(c.y, c.x));
				}
				//hole.add(hole.get(0));
				insertHole(positions,hole);
			}			
		} else if(g instanceof MultiPolygon) {
//			MultiPolygon mp =(MultiPolygon)g;
//			
//			for(int i=0;i<mp.getNumGeometries();i++) {
//				Polygon pol = mp.getGeometryN(i);
//			}
//			Coordinate[] coords = pol.getExteriorRing().getCoordinates();
//			for(int i=0;i<coords.length;i++) {
//				Coordinate c = coords[i];
//				positions.add(Position.fromDegrees(c.y, c.x));
//			}
//			positions.add(positions.get(0));
//
//
//			for(int r=0;r<pol.getNumInteriorRing();r++) {
//				List<Position> hole =new ArrayList<Position>();	
//				LineString ring = pol.getInteriorRingN(r);
//				Coordinate[] ringCoords = ring.reverse().getCoordinates();
//				for(int i=0;i<ringCoords.length;i++) {
//					Coordinate c = ringCoords[i];
//					hole.add(Position.fromDegrees(c.y, c.x));
//				}
//				//hole.add(hole.get(0));
//				insertHole(positions,hole);
//			}	
		}


		Poligono p = new Poligono();
		p.setPositions(positions);		
		p.setArea(GeometryHelper.getHas(g));
		return p;
	}

	public static Double distance(Position p1,Position p2) {		
		return Position.linearDistance(p1, p2).degrees;
	}
	public static void insertHole(List<Position> ring,List<Position> hole) {
		//TODO encontrar los puntos pas cercanos e insertar hole en outerRing
		Position minR=null,minH=null;
		int minI=-1,minJ=-1;
		Double minDist =null; 
		for(int i=0;i<ring.size();i++) {
			Position r=ring.get(i);
			for(int j=0;j<hole.size();j++) {
				Position h =hole.get(j);			
				Double dist = distance(r,h);
				if(minDist==null || minDist>dist) {
					minDist = dist;
					minI=i;
					minJ=j;
					minR=r;
					minH=h;
				}
			}
		}		
		//TODO insertar en minI hole empezando por minJ
		List<Position> sortedHole = new ArrayList<Position>();
		for(int h = 0; h<hole.size();h++) {
			Position hPos = hole.get((h+minJ)%(hole.size()));
			sortedHole.add(h,hPos);
			//ring.add(minI+h, hPos);
		}
		sortedHole.add(sortedHole.get(0));
		sortedHole.add(0,minR);
		ring.addAll(minI,sortedHole);
		ring.add(minI+sortedHole.size(),minR);
	}

	public static Polygon constructPolygon(Envelope e) {
		Coordinate D = new Coordinate(e.getMaxX(), e.getMaxY()); // x-l-d
		Coordinate C = new Coordinate(e.getMinX(), e.getMaxY());// X+l-d
		Coordinate B = new Coordinate(e.getMaxX(), e.getMinY());// X+l+d
		Coordinate A = new Coordinate(e.getMinX(), e.getMinY());// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, C, D, B, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon poly = fact.createPolygon(coordinates);
		return poly;
	}

	public static Point constructPoint(Position e) {
		Coordinate A = new Coordinate(e.getLongitude().getDegrees(), e.getLatitude().getDegrees());// X-l+d
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Point point = fact.createPoint(A);		
		return point;
	}

	public static LineString constructLineString(Position p1, Position p2) {
		Coordinate[] coords ={constructPoint(p1).getCoordinate(),constructPoint(p2).getCoordinate()};
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		return fact.createLineString(coords);
	}

	/**
	 * Representative point for inverse-distance weighting relative to a filter area.
	 * When the geometry extends outside the filter envelope, uses the centroid of
	 * the intersection with that envelope.
	 */
	public static Point centroidForDistanceWithinFilter(Geometry geometry, Geometry filterArea) {
		if (geometry == null || geometry.isEmpty()) {
			return null;
		}
		if (filterArea == null || filterArea.isEmpty()) {
			return geometry.getCentroid();
		}
		Envelope filterEnv = filterArea.getEnvelopeInternal();
		if (filterEnv.covers(geometry.getEnvelopeInternal())) {
			return geometry.getCentroid();
		}
		Geometry envelopeGeom = geometry.getFactory().toGeometry(filterEnv);
		Geometry clipped = getIntersection(geometry, envelopeGeom);
		if (clipped == null || clipped.isEmpty()) {
			return null;// geometry.getCentroid();
		}
		return clipped.getCentroid();
	}

	/**
	 * 
	 * @param g1
	 * @param g2
	 * @return computes validated intersection. returns null if geometrys dont intersect
	 * aumenta el tamanio de la geometria inicial
	 */
	/** Prepare once when intersecting the same geometry repeatedly (e.g. grid cell). */
	public static Geometry prepareForIntersection(Geometry g) {
		if (g == null) {
			return null;
		}
		return quickPrepare(PolygonValidator.force2D(g));
	}

	//FIXME check thread safety
	public static Geometry getIntersection(Geometry g1, Geometry g2){
		return getIntersection(g1, g2, false);
	}

	/** Use when g1 was already prepared with {@link #prepareForIntersection}. */
	public static Geometry getIntersection(Geometry g1, Geometry g2, boolean g1Prepared){
		if(g1==null || g2 ==null) {
			logger.warning("antes de validar geometrias devolviendo null porque una de las geometrias a intersectar es null. g1= "+g1+",g2= "+g2);
			return null;
		}
		if (!g1Prepared) {
			g1 = PolygonValidator.force2D(g1);
		}
		g2 = PolygonValidator.force2D(g2);
		if(!g1.getEnvelopeInternal().intersects(g2.getEnvelopeInternal())) {
			return null;
		}
		int parts1 = polygonPartCount(g1);
		int parts2 = polygonPartCount(g2);
		Geometry result;
		if (parts1 == 1 && parts2 == 1) {
			Geometry v1 = g1Prepared ? g1 : quickPrepare(g1);
			Geometry v2 = quickPrepare(g2);
			result = intersectPair(v1, v2);
			if (result == null) {
				result = intersectPair(
						v1 != null ? v1 : PolygonValidator.validate(g1),
						v2 != null ? v2 : PolygonValidator.validate(g2));
			}
		} else if (parts1 == 1) {
			result = intersectWithParts(g1Prepared ? g1 : quickPrepare(g1), g2);
			if (result == null && !g1Prepared) {
				result = intersectWithParts(PolygonValidator.validate(g1), g2);
			}
		} else if (parts2 == 1) {
			result = intersectWithParts(quickPrepare(g2), g1);
			if (result == null) {
				result = intersectWithParts(PolygonValidator.validate(g2), g1);
			}
		} else {
			result = intersectManyParts(g1, g2);
		}
		return finalizeIntersection(result);
	}

	private static int polygonPartCount(Geometry g) {
		if (g instanceof Polygon) {
			return 1;
		}
		return g.getNumGeometries();
	}

	/** Use geometry as-is when already valid; full repair only when needed. */
	private static Geometry quickPrepare(Geometry g) {
		if (g == null || g.isEmpty()) {
			return null;
		}
		try {
			if (g.isValid()) {
				return g;
			}
		} catch (RuntimeException ignored) {
			// fall through to repair
		}
		return PolygonValidator.validate(g);
	}

	private static Geometry intersectWithParts(Geometry simple, Geometry multi) {
		Geometry prepared = quickPrepare(simple);
		if (prepared == null) {
			return null;
		}
		Envelope env = prepared.getEnvelopeInternal();
		List<Geometry> results = new ArrayList<>();
		for (int i = 0; i < multi.getNumGeometries(); i++) {
			Geometry part = multi.getGeometryN(i);
			if (part == null || part.isEmpty() || !env.intersects(part.getEnvelopeInternal())) {
				continue;
			}
			Geometry preparedPart = quickPrepare(part);
			if (preparedPart == null) {
				continue;
			}
			Geometry inter = intersectPair(prepared, preparedPart);
			if (inter == null) {
				inter = intersectPair(prepared, PolygonValidator.validate(part));
			}
			if (inter != null && !inter.isEmpty()) {
				results.add(inter);
			}
		}
		return mergeGeometries(results);
	}

	private static Geometry intersectManyParts(Geometry g1, Geometry g2) {
		List<Geometry> results = new ArrayList<>();
		for (int i = 0; i < g1.getNumGeometries(); i++) {
			Geometry part1 = g1.getGeometryN(i);
			if (part1 == null || part1.isEmpty()) {
				continue;
			}
			Geometry prepared1 = quickPrepare(part1);
			if (prepared1 == null) {
				continue;
			}
			Envelope env1 = prepared1.getEnvelopeInternal();
			for (int j = 0; j < g2.getNumGeometries(); j++) {
				Geometry part2 = g2.getGeometryN(j);
				if (part2 == null || part2.isEmpty() || !env1.intersects(part2.getEnvelopeInternal())) {
					continue;
				}
				Geometry prepared2 = quickPrepare(part2);
				if (prepared2 == null) {
					continue;
				}
				Geometry inter = intersectPair(prepared1, prepared2);
				if (inter == null) {
					inter = intersectPair(
							prepared1,
							PolygonValidator.validate(part2));
				}
				if (inter != null && !inter.isEmpty()) {
					results.add(inter);
				}
			}
		}
		return mergeGeometries(results);
	}

	private static Geometry intersectPair(Geometry g1, Geometry g2) {
		if (g1 == null || g2 == null) {
			return null;
		}
		try {
			Geometry inter = g1.intersection(g2);
			if (inter != null && !inter.isEmpty()) {
				return inter;
			}
		} catch (RuntimeException ignored) {
			// fall through to enhanced precision
		}
		try {
			Geometry inter = EnhancedPrecisionOp.intersection(g1, g2);
			if (inter != null && !inter.isEmpty()) {
				return inter;
			}
		} catch (RuntimeException ignored) {
			return null;
		}
		return null;
	}

	private static Geometry finalizeIntersection(Geometry intersection) {
		if (intersection == null || intersection.isEmpty()) {
			return null;
		}
		try {
			if (intersection.isValid()) {
				return intersection;
			}
		} catch (RuntimeException ignored) {
			// fall through to repair
		}
		intersection = PolygonValidator.validate(intersection);
		if (intersection == null || intersection.isEmpty()) {
			return null;
		}
		return intersection;
	}

	private static Geometry mergeGeometries(List<Geometry> parts) {
		if (parts.isEmpty()) {
			return null;
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		GeometryFactory fact = parts.get(0).getFactory();
		GeometryCollection collection = fact.createGeometryCollection(parts.toArray(new Geometry[0]));
		try {
			return collection.union();
		} catch (RuntimeException e) {
			try {
				return EnhancedPrecisionOp.buffer(collection, 0);
			} catch (RuntimeException e2) {
				return collection;
			}
		}
	}

	private static Geometry validateNonEmptyIntersection(Geometry intersection) {
		return finalizeIntersection(intersection);
	}

	public static Geometry getIntersectionSlow(Geometry g1, Geometry g2){
		List<Geometry> toIntersect = Arrays.asList(g1,g2);

		Set<Geometry> parts = obtenerIntersecciones(toIntersect);
		List<Geometry> intersections = new ArrayList<Geometry>();
		Geometry intersection =null;
		for(Geometry candidate:parts) {
			boolean isContained = true;
			for(Geometry check:toIntersect) {
				isContained = isContained && check.contains(candidate);
			}
			if(isContained) {
				intersections.add(candidate);
				//intersection = intersection==null?candidate:intersection.union(candidate);
			}

		}
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();

		intersection= fact.createGeometryCollection(intersections.toArray(new Geometry[intersections.size()])).buffer(0);
		if(intersection==null) {
			//System.out.println("no se pudo calcular la interseccion entre "+g1+" y "+g2);
		}
		return intersection;
	}
	
	/**
	 * Metodo que toma un grupo de geometrias y las parte en sus partes basicas
	 * @param aIntersectar
	 * @return un grupo de geometrias que no se intersectan y cubren la superficie de la union de las geometrias de entrada
	 */
	public static Set<Geometry> obtenerIntersecciones(List<Geometry> aIntersectar){
		Polygonizer polygonizer = new Polygonizer();

		List<Geometry> boundaries = aIntersectar.stream().map(g->g.getBoundary()).collect(Collectors.toList());
		Geometry unitedBoundary = toGeometryCollection(boundaries).union();
		List<?> lines = LineStringExtracter.getLines(unitedBoundary);
		logger.fine("agregando lines "+lines.size()+" para poligonizer");
		polygonizer.add(lines);

		Collection polys = polygonizer.getPolygons();
		logger.fine("cree "+polys.size()+" poligonos");
		Set<Geometry> geometriasOutput = new HashSet<Geometry>();
		geometriasOutput.addAll(polys);
		return geometriasOutput;
	}
	
	/**
	 * metodo que recorre todas las geometrias haciendo las intersecciones de todos con todos.
	 * @param aIntersectar: Lista de geometrias a intersectar 
	 * @return el Set de las partes de las geometrias intersectadas
	 */
	@Deprecated //perdia superficie entre los poligonos
	public static Set<Geometry> obtenerIntersecciones2(List<Geometry> aIntersectar){		
		//obtengo una collection con los boundarys
		List<Geometry> boundaryList =  aIntersectar.stream().map(g->g.getBoundary()).collect(Collectors.toList());		
		GeometryCollection boundarysCol = toGeometryCollection(boundaryList);
		//Obtengo un buffer de las boundarys
		Double buffer25=ProyectionConstants.metersToLongLat(0.25);//0.25 es lo mas chico que me permite mi precision model
		Geometry boundary_buffer = boundarysCol.buffer(buffer25,1,BufferParameters.CAP_SQUARE);

		//obtengo un buffer que una a todas las geometrias a intersectar
		GeometryCollection colectionCat = toGeometryCollection(aIntersectar);

		//(buffer0,1,BufferParameters.CAP_SQUARE);
		Double buffer0=ProyectionConstants.metersToLongLat(0);
		Geometry unionBuffer = colectionCat.buffer(buffer0,1,BufferParameters.CAP_FLAT);

		//obtengo la diferencia entre el todo y los bordes de las geometrias
		Geometry diff = unionBuffer.difference(boundary_buffer);
		Set<Geometry> geometriasOutput = new HashSet<Geometry>();
		//double tolerance = ProyectionConstants.metersToLongLat(1);
		double bufferWidth = 2*buffer25;
		Double buffer30=ProyectionConstants.metersToLongLat(0.28);
		for(int n = 0; n < diff.getNumGeometries(); n++){
			Geometry g = diff.getGeometryN(n);
			//XXX al hacer el buffer se crean puntitos en las esquinas. las descarto
			if(g.getArea()<bufferWidth*bufferWidth) {
				continue;
			}

			g=g.buffer(buffer30,1,BufferParameters.CAP_FLAT);//mitad del buffer esta en esta y mitad en la otra
			g = PolygonValidator.validate(g);
			geometriasOutput.add(g);
		}

		return geometriasOutput;
	}

	/**
	 * 
	 * @param complex
	 * @return la geometria reemplazando 2 vertices por su promedio 
	 */
	public static Geometry simplify(Geometry complex) {		
		complex = ExtraerPoligonosDeLaborTask.geometryToPoligono(complex).toGeometry();
		if(complex instanceof Polygon) {
			Densifier densifier = new Densifier(complex);
			densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(10));
			complex=densifier.getResultGeometry();
			//buffered = TopologyPreservingSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(2));
			//	buffered = DouglasPeuckerSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(5));

			Geometry simple = null;
			Coordinate[] complexCoordinates = complex.getCoordinates();

			List<Coordinate> middleCoords = new ArrayList<Coordinate>();
			middleCoords.add(complexCoordinates[0]);
			for(int i=1;i<complexCoordinates.length;i++) {
				Coordinate last = complexCoordinates[i-1];
				Coordinate next = complexCoordinates[i];
				Point p1 = ProyectionConstants.getGeometryFactory().createPoint(last);
				Point p2 = ProyectionConstants.getGeometryFactory().createPoint(next);
				Double dist = p1.distance(p2);
				if(dist>ProyectionConstants.metersToLongLat(0.25)) {
					middleCoords.add(next);
				}
			}

			List<Coordinate> simpleCoords = new ArrayList<Coordinate>();
			//simpleCoords.add(middleCoords.get(0));
			for(int i=1;i<middleCoords.size();i++) {
				Coordinate last = middleCoords.get(i-1);
				Coordinate next = middleCoords.get(i);
				Coordinate newCoord = new Coordinate();
				newCoord.x=(last.x+next.x)/2;
				newCoord.y=(last.y+next.y)/2;
				simpleCoords.add(newCoord);
			}
			//		Coordinate last = middleCoords.get(middleCoords.size()-1);
			//		Coordinate next = middleCoords.get(0);
			//		Coordinate newCoord = new Coordinate();
			//		newCoord.x=(last.x+next.x)/2;
			//		newCoord.y=(last.y+next.y)/2;
			//		simpleCoords.add(0,newCoord);
			//		simpleCoords.add(newCoord);
			simpleCoords.add(simpleCoords.get(0));
			simple =complex.getFactory().createPolygon(simpleCoords.toArray(new Coordinate[simpleCoords.size()]));
			return simple;
		} else if(complex instanceof MultiPolygon) {

			List<Geometry> simples = new ArrayList<Geometry>();
			for(int i=0;i<complex.getNumGeometries();i++) {
				Geometry g = complex.getGeometryN(i);
				simples.add(simplify(g));
			}
			GeometryCollection col = new GeometryCollection(simples.toArray(new Geometry[simples.size()]),complex.getFactory());
			return col.buffer(ProyectionConstants.metersToLongLat(0.25));
		}
		return complex;
	}

	public static Double getHas(Geometry g) {
		Double area =0.0;
		try {
			if(g!=null) {
				area = ProyectionConstants.A_HAS(g.getArea());
			}else {
				//System.out.println("No se pudo calcular el area de la geometria "+g);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return area;
	}
	public static GeometryCollection toGeometryCollection(List<Geometry> list) {
		Geometry[] array = list.stream().filter(g->g!=null).toArray(size->new Geometry[size]);
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		//Geometry[] array =list.toArray(new Geometry[list.size()]);
		GeometryCollection collection = fact.createGeometryCollection(array );
		return collection;
	}

	/**
	 * metodo que se usa en extraer contorno de labor.
	 * @param aUnir la labor que contiene las geometrias a unir
	 * @param bounds el sector de la labor a inspeccionar
	 * @return
	 */
	public static Geometry unirCascading(Labor<?> aUnir,Envelope bounds) {
		try {
//			Sector s = boundsToSector(bounds);
//			Sector[] parts = s.subdivide(2);//divide el sector en 4
			
			List<Geometry> boundsGeoms = new ArrayList<Geometry>();
			List<? extends LaborItem> boundsFeatures = (List<? extends LaborItem>)aUnir.cachedOutStoreQuery(bounds);
			if(boundsFeatures.size()==1)return boundsFeatures.get(0).getGeometry();
			if(ProyectionConstants.A_HAS(bounds.getArea()) > 1				
					&& boundsFeatures.size()>1000) {//divido hasta que cubre 1has
				//			System.out.println("bounds area = "+ProyectionConstants.A_HAS(bounds.getArea()));
				//si es mayor a 100m2 divido en 4
				List<Envelope> envelopes = splitEnvelope(bounds);
				List<Geometry> geomsEnvelopes = envelopes.parallelStream().map(e->{
					Geometry eGeom = unirCascading(aUnir,e);
					return eGeom;
				}).collect(Collectors.toList());
				boundsGeoms.addAll(geomsEnvelopes);
//				for(Envelope e:envelopes) {
//					Geometry eGeom = unirCascading(aUnir,e);
//					if(eGeom !=null) {
//						boundsGeoms.add(eGeom);
//					}
//				}

			} else {
				//List<LaborItem> boundsFeatures = (List<LaborItem>)aUnir.cachedOutStoreQuery(bounds);
				if(boundsFeatures.size()>0) {
					//			System.out.println("joining "+boundsFeatures.size());
					boundsGeoms.addAll( boundsFeatures.parallelStream()
							.map(i->i.getGeometry())					
							.collect(Collectors.toList()));
				}
			}


			//System.out.println("juntando "+boundsGeoms.size()+" geoms");
			Geometry union = null; 
			//union = toGeometryCollection(boundsGeoms).buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
			try {
				//union = unirGeometrias(boundsGeoms);
				Double buffer = ProyectionConstants.metersToLongLat(0.25);
				union = toGeometryCollection(boundsGeoms).buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
				
			}catch(Exception e ) {
				e.printStackTrace();
			}
			return union;
		}catch(Exception e ) {
			e.printStackTrace();
			return null;
		}	
	}

	public static Sector boundsToSector(Envelope bounds) {
		Sector s = Sector.fromDegrees(bounds.getMinY(),
							  bounds.getMaxY(),
							  bounds.getMinX(),
							  bounds.getMaxX());
		return s;
	}



	public static List<Envelope> splitEnvelope(Envelope e){
		List<Envelope> result = new ArrayList<Envelope>();
		double ancho = e.getWidth()/2;
		double alto = e.getHeight()/2;		
	
//		for (int row = 0; row < 2; row++) {
//			for (int col = 0; col < 2; col++) {
//				result.add(new Envelope(
//						e.getMinX() + ancho * col,
//						e.getMinX() + ancho * col + ancho,
//						e.getMinY() + alto * row,
//						e.getMinY() + alto * row + alto
//						));
//			}
//		}
	        
		for(double x = e.getMinX(); x <= e.getMaxX()-ancho; x+=ancho) {
			for(double y = e.getMinY(); y <= e.getMaxY()-alto; y+=alto) {			
				result.add(new Envelope(x,x+ancho,y,y+ancho));
			}
		}
		return result;
	}

	public static void main(String [] args) {
		Envelope e = new Envelope(0,10,0,10);
		List<Envelope> envelopes = splitEnvelope(e);
		//Arrays.toString
		//	String s = Arrays.asList(j).stream().collect(Collectors.joining("</td><td>","<td>","</td>"));
		logger.fine("envelopes created = "+envelopes);
	}

	public static Geometry unirGeometrias(List<Geometry> aUnir) {
		if(aUnir!=null && aUnir.size()==1) {
			return aUnir.get(0);
		}
		try {
			double dTolerance = ProyectionConstants.metersToLongLat(10);
			
			List<Geometry> aUnird = aUnir.stream().filter( g-> 
							g!=null && !g.isEmpty()
					).map(g->{
				try {
					if(!g.isEmpty()) {
//						Densifier densifier = new Densifier(g);
//						//densifier.setValidate(false);
//						densifier.setDistanceTolerance();
						g=Densifier.densify(g, dTolerance); 
						//densifier.getResultGeometry();//si la geometria es grande esto devuelve POLYGON EMPTY?
					}
				}catch(Exception e) {
					logger.warning("fallo densifier con "+g);
					//e.printStackTrace();
				}
				return  g;			
			}).collect(Collectors.toList());
			//XXX cuando llega aca de una siembra que tiene solo un poligono devuelve POLYGON EMPTY

			//		GeometryFactory fact = ProyectionConstants.getGeometryFactory();		
			//		Geometry[] geomArray = aUnir.toArray(new Geometry[aUnir.size()]);//put into an array
			//		GeometryCollection collection = fact.createGeometryCollection(geomArray);//create a collection
			GeometryCollection collection = toGeometryCollection(aUnird);
			Double buffer = ProyectionConstants.metersToLongLat(0.25);

			Geometry union =collection.buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
			Geometry boundary = union.getBoundary().buffer(buffer,1,BufferParameters.CAP_FLAT);
			Geometry dif=union.difference(boundary);
			if(dif.getCoordinates().length>100) {
				dif=GeometryHelper.douglassPeuckerSimplify(dif,ProyectionConstants.metersToLongLat(0.025));//esto mejora mucho las subsiguientes operaciones
			}
			dif = PolygonValidator.validate(dif);			
			if(dif.isValid()) {
				//System.out.println("dif es valid");
				return dif;
			}else {
				//System.out.println("dif no es valid");
				return union;
			}
			//buffered = CascadedPolygonUnion.union(geometriesCat);
			//Geometry union = collection.buffer(0);//ProyectionConstants.metersToLongLat(20));
			//TODO hacer un buffer 0.25
			//y despues hacer un dif contra el boundary buffer 0.25 para evitar que crezcan los items
			//System.out.println("geometria densa unida "+union);
			//return dif;
		}catch(Exception e) {
			logger.warning("fallo collection buffer uniendo de a una "+aUnir);
			//e.printStackTrace();
			Geometry union=null;

			//			Geometry[] unionContainer = aUnir.parallelStream().collect(
			//					()->new Geometry[1],
			//					(arr,g)->{
			//						if(arr[0]==null) {
			//							arr[0]=g;
			//						}else {
			//							try {
			//								arr[0]=arr[0].union(g);
			//							}catch(Exception e2) {
			//								e2.printStackTrace();
			//							}
			//						}
			//					},
			//					(arr1,arr2)->{
			//						try {
			//							arr1[0]=arr1[0].union(arr2[0]);
			//						}catch(Exception e2) {
			//							e2.printStackTrace();
			//						}
			//					}
			//					);
			//			union=unionContainer[0];

			for(Geometry g:aUnir) {
				if(union==null) {
					union=g;
				}else {
					try {
						union=union.union(g);
					}catch(Exception e2) {
//						try{
//							buffered = EnhancedPrecisionOp.buffer(colectionCat, buffer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
//						}catch(Exception e2){
//							e2.printStackTrace();
//						}
						
						e2.printStackTrace();
					}
				}
			}
			return union;
		}
	}

	/**
	 * metodo que se llama al construir la grilla en correlacionarLayers
	 * @param labor
	 * @return
	 */
	public static synchronized Geometry extractContornoGeometry(Labor<?> labor) {
		logger.fine("extrayendo contorno de labor "+labor);
		try{					
			ReferencedEnvelope bounds = labor.outCollection.getBounds();
			//hace la union de todas las geometrias
			List<? extends LaborItem> items = labor.cachedOutStoreQuery(bounds);//demora
			Geometry cascadedUnion =null;
		//	long init =System.currentTimeMillis();
		//	 cascadedUnion = convexHull(items);//demora
		//	 System.out.println("termine convex hull");
		//	long endConvexHull =System.currentTimeMillis();
			cascadedUnion = unirCascading(labor,bounds);
			//List<Geometry> boundaries = items.stream().map(i->i.getGeometry()).collect(Collectors.toList());//rapido
			//cascadedUnion = toGeometryCollection(boundaries).buffer(ProyectionConstants.metersToLongLat(1), 1, BufferParameters.CAP_FLAT);
			//no termina nunca
			
		//	long endUnirCascading =System.currentTimeMillis();
		//	System.out.println("tarde "+(endConvexHull-init)+" s en hacer convexHull");
		//	System.out.println("tarde "+(endUnirCascading-endConvexHull)+" s en hacer unirCascading");
			if(cascadedUnion.getNumGeometries()==1) {
				return cascadedUnion;
			} else {
				logger.fine("despues de hacer unir cascading sigue teniendo mas de una geometria "+cascadedUnion.getNumGeometries());
			}
			//hago un buffer de las que quedan
			// un buffer de 20mts es bastante
			cascadedUnion = cascadedUnion.buffer(ProyectionConstants.metersToLongLat(20));
			//extrae el boundary del buffer
			Geometry boundary = cascadedUnion.getBoundary();
			//hace un buffer del bundary
			boundary = boundary.buffer(ProyectionConstants.metersToLongLat(20));
			//le saco el bundary al buffer de la union
			cascadedUnion = cascadedUnion.difference(boundary);
			//lo simplifico
			cascadedUnion= simplificarContorno(cascadedUnion);
			return PolygonValidator.validate(cascadedUnion);
			//return cascadedUnion;
		}catch(Exception e){
			ReferencedEnvelope bounds = labor.outCollection.getBounds();			
			e.printStackTrace();
			return constructPolygon(bounds);
		}
	}

	/**
	 * metodo que devuelve el poligono convexo mas chico que engloba los items.
	 * @param items
	 * @return devuelve el poligono convexo mas chico que contiene los items.
	 */
	public static Geometry convexHull(List<? extends LaborItem> items) {
		List<Geometry> geoms = items.stream().map(i->i.getGeometry()).collect(Collectors.toList());
		GeometryCollection colectionCat = GeometryHelper.toGeometryCollection(geoms);
		Geometry cascadedUnion = colectionCat.convexHull();
		return cascadedUnion;
	}
	/**
	 * metodo llamado en labor.getContorno
	 * @param labor
	 */
	//	public static void extractContorno(Labor<?> labor) {
	//		//TODO compute contorno
	//		//		List<Geometry> geometriesCat = new ArrayList<Geometry>();
	//		//		SimpleFeatureIterator it = labor.outCollection.features();
	//
	//
	//
	//		//		while(it.hasNext()){
	//		//			SimpleFeature f=it.next();
	//		//			geometriesCat.add((Geometry)f.getDefaultGeometry());
	//		//		}
	//		//		it.close();		
	//
	//		try{					
	//			ReferencedEnvelope bounds = labor.outCollection.getBounds();
	//			//System.out.println("outCollectionBounds "+bounds);
	//			Geometry cascadedUnion = unirCascading(labor,bounds);
	//			//Geometry buffered = GeometryHelper.unirGeometrias(geometriesCat);
	//			//CascadedPolygonUnion.union(geometriesCat);
	//			//sino le pongo buffer al resumir geometrias me quedan rectangulos medianos
	//			//				buffered = buffered.buffer(
	//			//						ProyectionConstants.metersToLongLat(0.25),
	//			//						1,BufferParameters.CAP_SQUARE);
	//			//buffered =GeometryHelper.simplificarContorno(buffered);
	//			Poligono contorno =GeometryHelper.constructPoligono(cascadedUnion);
	//			//	simplificarPoligono(contorno);
	//			contorno.setNombre(labor.getNombre());
	//			//labor.setContorno(contorno);
	//		}catch(Exception e){
	//			e.printStackTrace();
	//		}
	//	}

	public static void simplificarPoligono(Poligono p) {
		Geometry g = p.toGeometry();
		//g=g.buffer(ProyectionConstants.metersToLongLat(10));

		//TODO remover un punto si el area que forma el triangulo con sus vecinos es suficientemente pequenia
		g=GeometryHelper.removeSmallTriangles(g, (0.005)/ProyectionConstants.A_HAS());

		g=GeometryHelper.douglassPeuckerSimplify(g,ProyectionConstants.metersToLongLat(5));
		//g=GeometryHelper.removeClosePoints(g, ProyectionConstants.metersToLongLat(2));
		//g=GeometryHelper.removeSinglePoints(g, ProyectionConstants.metersToLongLat(2));
		//g=GeometryHelper.reduceAlignedPoints(g, 0.2);
		Poligono pol =GeometryHelper.constructPoligono(g);
		if(p.getLayer()!=null) {
			MeasureTool measureTool = (MeasureTool) p.getLayer().getValue(PoligonLayerFactory.MEASURE_TOOL);
			measureTool.setPositions((ArrayList<? extends Position>) pol.getPositions());
		}
	}

	public static Geometry removeSmallTriangles(Geometry g, double minLongLatArea) {
		Geometry ret=null;
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);
		boolean changed = false;

		changed = false;
		//double minTriangleHas = ProyectionConstants.A_HAS(minLongLatArea);
		//System.out.println("minTriangleHas ="+minTriangleHas);
		//System.out.println("bounds length "+boundCoords.length);
		for(int i=1;i<boundCoords.length-1;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			ProyectionConstants.setLatitudCalculo(c0.y);
			Coordinate c1 = boundCoords[i];
			Coordinate c2 = boundCoords[i+1];
			Coordinate[] tCoords = {c0,c1,c2,c0};
			try {
				Geometry triangle = fact.createPolygon(tCoords);//.createPolygon({c0,c1,c2});
				//double triangleArea = triangle.getArea();
				//double triangleHas = ProyectionConstants.A_HAS(triangleArea);
				//System.out.println("triangleHas ="+triangleHas);
				if(triangle.getArea()>minLongLatArea) {
					vertices.add(c1);
				} else {

					changed=true;
				}
			}catch(Exception e) {
				vertices.add(c1);
				e.printStackTrace();
			}

		}

		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * The Douglas-Peucker algorithm uses a point-to-edge distance tolerance. 
	 * The algorithm starts with a crude simplification that is the single edge joining the 
	 * first and last vertices of the original polyline. 
	 * It then computes the distance of all intermediate vertices to that edge.
	 * The vertex that is furthest away from that edge,
	 * and that has a computed distance that is larger than a specified tolerance,
	 * will be marked as a key and added to the simplification.
	 * This process will recurse for each edge in the current simplification, 
	 * until all vertices of the original polyline are within tolerance of the
	 * simplification results.
	 * @param g
	 * @return simplified geometry
	 */
	public static Geometry douglassPeuckerSimplify(Geometry g) {
		//org.locationtech.jts.simplify.
		//DouglasPeuckerSimplifier simp;
		return DouglasPeuckerSimplifier.simplify(g, 0.000001);

	}

	/**
	 * 
	 * @param g geometria a simplificar
	 * @param tolerance distancia en grados
	 * @return geometria simplificada
	 */
	public static Geometry douglassPeuckerSimplify(Geometry g,double tolerance) {
		//org.locationtech.jts.simplify.
		//DouglasPeuckerSimplifier simp;
		return DouglasPeuckerSimplifier.simplify(g, tolerance);

	}

	/**
	 * metod para importar poligonos desde KML
	 * @param coordinates
	 * @param holes
	 * @return
	 */
	public static Poligono constructPolygon(PositionList coordinates,List<PositionList> holes) {		
		logger.fine("convirtiendo PositionList a poligono "+coordinates);		
		List<Position> positions = new ArrayList<Position>();
		for(Position pos: coordinates.list) {
			positions.add(pos);
		}
		List<List<Position>> huecos = new ArrayList<List<Position>>();
		for(PositionList hole : holes) {
			List<Position> holePositions = new ArrayList<Position>();
			for(Position pos : hole.list) {
				holePositions.add(pos);
			}
			huecos.add(holePositions);
		}
//		positions.add(positions.get(0));


//		for(int r=0;r<pol.getNumInteriorRing();r++) {
//			List<Position> hole =new ArrayList<Position>();	
//			LineString ring = pol.getInteriorRingN(r);
//			Coordinate[] ringCoords = ring.reverse().getCoordinates();
//			for(int i=0;i<ringCoords.length;i++) {
//				Coordinate c = ringCoords[i];
//				hole.add(Position.fromDegrees(c.y, c.x));
//			}
//			//hole.add(hole.get(0));
//			insertHole(positions,hole);
//		}			



		Poligono p = new Poligono();
		p.setPositions(positions);		
		p.setHuecos(huecos);
	//	p.setArea(GeometryHelper.getHas(g));
		return p;
	}

	/**
	 * Une los anillos exteriores de un multipoligono en un solo contorno
	 * conectando las partes en los puntos mas cercanos.
	 */
	public static Poligono unirAnillosExteriores(Geometry g) {
		return ExtraerPoligonosDeLaborTask.geometryToPoligonoOLD(g);
	}

	/**
	 * Devuelve todas las partes de un poligono (incluye multipoligonos) para operaciones geometricas.
	 */
	public static List<Geometry> geometriesFromPoligono(Poligono poli) {
		Geometry g = poli.getGeometry();
		if(g == null || g.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(PolygonValidator.geometryToFlatPolygons(g));
	}

	/**
	 * Crea un poligono por cada parte desconectada del poligono original.
	 * @return lista vacia si el poligono tiene una sola parte
	 */
	public static List<Poligono> explotarPoligono(Poligono original) {
		if(original == null) {
			return Collections.emptyList();
		}
		List<Polygon> parts = PolygonValidator.geometryToFlatPolygons(original.getGeometry());
		if(parts.size() <= 1) {
			return Collections.emptyList();
		}
		String baseName = original.getNombre();
		List<Poligono> result = new ArrayList<>(parts.size());
		for(int i = 0; i < parts.size(); i++) {
			Poligono partPoli = ExtraerPoligonosDeLaborTask.geometryToPoligono(parts.get(i));
			partPoli.setNombre(baseName + " (" + (i + 1) + ")");
			partPoli.setLote(original.getLote());
			result.add(partPoli);
		}
		return result;
	}

	/**
	 * metodo que convierte un poligono a una geometria es el que se usa en Poligono.toGeometry()
	 * @param poli
	 * @return
	 */
	public static Geometry poligonotoGeometry(Poligono poli){
		try {
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();// GeometryFactory();

			List<? extends Position> positions = poli.getPositions();
			if(positions==null||positions.size()==0)return null;
			Coordinate[] coordinates = positionListToCoordinateArray(positions);
			LinearRing[] holes = null;
			if(poli.getHuecos()!=null){
				holes = new LinearRing[poli.getHuecos().size()];
				for(int i=0;i<poli.getHuecos().size();i++){
					List<? extends Position> hole = poli.getHuecos().get(i);
					Coordinate[] holeCoordinates = positionListToCoordinateArray(hole);
					holes[i]=fact.createLinearRing(holeCoordinates);
				}
			}
			LinearRing shell = fact.createLinearRing(coordinates);
			Polygon poly = fact.createPolygon(shell, holes);
			//Polygon poly = fact.createPolygon(coordinates, holes);
			//Polygon poly = fact.createPolygon(coordinates);	
			return poly;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static Coordinate[] positionListToCoordinateArray(List<? extends Position> positions) {
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = positionToCoordinate(p);

			coordinates[i]=c;
		}
		coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
		return coordinates;
	}

	public static Coordinate positionToCoordinate(Position p) {
		Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());
		return c;
	}

    public static Geometry getGeometryFromSurfacePolygon(SurfacePolygon surfaceShape) {
		try {
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();// GeometryFactory();
			List<Iterable<? extends LatLon>> boundaries =  surfaceShape.getBoundaries();
			List<? extends Position> positions =(List<Position>) boundaries.get(0);
			if(positions==null||positions.size()==0)return null;
			Coordinate[] coordinates = positionListToCoordinateArray(positions);
			LinearRing[] holes = null;
			if(boundaries.size()>1){
				holes = new LinearRing[boundaries.size()-1];
				for(int i=1;i<boundaries.size();i++){
					List<? extends Position> hole = (List<Position>) boundaries.get(i);
					Coordinate[] holeCoordinates = positionListToCoordinateArray(hole);
					holes[i - 1]=fact.createLinearRing(holeCoordinates);
				}				
			}
			LinearRing shell = fact.createLinearRing(coordinates);
			Polygon poly = fact.createPolygon(shell, holes);
			//Polygon poly = fact.createPolygon(coordinates, holes);
			//Polygon poly = fact.createPolygon(coordinates);	
			return poly;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}       
    }

	public static Geometry getGeometryFromSurfacePolygons(List<SurfacePolygon> surfaceShapes) {
		if (surfaceShapes == null || surfaceShapes.isEmpty()) {
			return null;
		}
		if (surfaceShapes.size() == 1) {
			return getGeometryFromSurfacePolygon(surfaceShapes.get(0));
		}

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		List<Polygon> polygons = new ArrayList<>();
		for (SurfacePolygon shape : surfaceShapes) {
			Geometry geometry = getGeometryFromSurfacePolygon(shape);
			if (geometry instanceof Polygon polygon) {
				polygons.add(polygon);
			} else if (geometry != null) {
				polygons.addAll(PolygonValidator.geometryToFlatPolygons(geometry));
			}
		}
		if (polygons.isEmpty()) {
			return null;
		}
		if (polygons.size() == 1) {
			return polygons.get(0);
		}
		return fact.createMultiPolygon(polygons.toArray(Polygon[]::new));
	}

	public static SurfacePolygon createSurfacePolygonFromPolygon(Polygon pol) {
		SurfacePolygon shape = new SurfacePolygon();
		shape.setLocations(geometryToPositions(pol.getExteriorRing()));
		for(int i = 0; i < pol.getNumInteriorRing(); i++) {
			shape.addInnerBoundary(geometryToPositions(pol.getInteriorRingN(i)));
		}
		return shape;
	}

	public static List<SurfacePolygon> createSurfacePolygonsFromPoligono(Poligono poli) {
		Geometry g = poli.getGeometry();
		if(g != null && !g.isEmpty() && g.getNumGeometries() > 1) {
			List<SurfacePolygon> shapes = new ArrayList<>();
			for(Polygon part : PolygonValidator.geometryToFlatPolygons(g)) {
				shapes.add(createSurfacePolygonFromPolygon(part));
			}
			return shapes;
		}
		List<SurfacePolygon> shapes = new ArrayList<>();
		shapes.add(createSurfacePolygonFromPoligono(poli));
		return shapes;
	}

	public static SurfacePolygon createSurfacePolygonFromPoligono(Poligono poli) {
		List<Position> positions = poli.getPositions();
		SurfacePolygon shape = new SurfacePolygon();
		shape.setLocations(positions);
		for(List<Position> hole : poli.getHuecos()){
			shape.addInnerBoundary(hole);
		}
		return shape;
	}

	/**
	 * Limite de partes por geometria al exportar prescripciones a monitores de campo.
	 * Algunos equipos rechazan shapefiles cuyas geometrias tienen mas de 50 subpartes.
	 */
	public static final int MAX_PRESCRIPTION_GEOMETRY_PARTS = 50;

	/**
	 * Limite de anillos interiores (huecos) por poligono al exportar prescripciones.
	 * Algunos monitores rechazan poligonos con demasiados huecos.
	 */
	public static final int MAX_PRESCRIPTION_INTERIOR_RINGS = 50;

	/**
	 * Buffer minimo para fusionar partes cercanas al exportar prescripciones.
	 * Reduce cantidad de subpartes y peso del shapefile sin recortar zonas lejanas.
	 */
	public static final double PRESCRIPTION_EXPORT_MERGE_BUFFER_METERS = 0.25;

	/**
	 * Fusiona partes cercanas aplicando un buffer positivo minimo.
	 * Solo actua cuando la geometria tiene mas de una parte; una sola parte se devuelve
	 * sin modificar para no inflar artificialmente su contorno.
	 *
	 * @param geometry geometria de entrada; puede ser {@code null}
	 * @param bufferMeters distancia del buffer en metros
	 * @return geometria fusionada o la original si no habia nada que unir
	 */
	public static Geometry mergeNearbyGeometryParts(Geometry geometry, double bufferMeters) {
		if (geometry == null || geometry.isEmpty() || geometry.getNumGeometries() <= 1) {
			return geometry;
		}
		double bufferDistance = ProyectionConstants.metersToLongLat(bufferMeters);
		try {
			return geometry.buffer(bufferDistance);
		} catch (RuntimeException e) {
			try {
				return EnhancedPrecisionOp.buffer(geometry, bufferDistance);
			} catch (RuntimeException e2) {
				return geometry;
			}
		}
	}

	/**
	 * Atajo de exportacion: fusiona partes cercanas con
	 * {@link #PRESCRIPTION_EXPORT_MERGE_BUFFER_METERS}.
	 */
	public static Geometry mergeNearbyPrescriptionGeometryParts(Geometry geometry) {
		return mergeNearbyGeometryParts(geometry, PRESCRIPTION_EXPORT_MERGE_BUFFER_METERS);
	}

	/**
	 * Reduce una geometria multiparte conservando como maximo {@code maxParts} partes.
	 * Si hay mas partes, se descartan las de menor area para no perder las zonas
	 * agronomicamente mas relevantes.
	 * <p>
	 * Este metodo no fusiona partes: solo recorta. Para exportacion usar
	 * {@link #limitPrescriptionGeometryParts(Geometry)}, que primero intenta unir
	 * fragmentos cercanos y recien despues aplica este limite.
	 *
	 * @param geometry geometria de entrada; puede ser {@code null}
	 * @param maxParts cantidad maxima de partes a conservar
	 * @return la geometria original si no supera el limite; una sola parte o un
	 *         multipoligono con las partes mas grandes si fue necesario recortar
	 */
	public static Geometry limitGeometryParts(Geometry geometry, int maxParts) {
		// Nada que recortar: geometria ausente, vacia o limite invalido.
		if (geometry == null || geometry.isEmpty() || maxParts < 1) {
			return geometry;
		}
		int partCount = geometry.getNumGeometries();
		// Caso comun: la geometria ya cumple el limite del monitor.
		if (partCount <= maxParts) {
			return geometry;
		}
		// Descomponer en partes individuales para poder ordenarlas por importancia.
		List<Geometry> parts = new ArrayList<>(partCount);
		for (int i = 0; i < partCount; i++) {
			parts.add(geometry.getGeometryN(i));
		}
		// Priorizar las partes mas grandes; las chicas suelen ser artefactos o islas menores.
		parts.sort((g1, g2) -> Double.compare(g2.getArea(), g1.getArea()));
		List<Geometry> kept = parts.subList(0, maxParts);
		// Evitar envolver una sola parte en un multipoligono innecesario.
		if (kept.size() == 1) {
			return kept.get(0);
		}
		// Reconstruir una geometria valida con las partes conservadas.
		return geometry.getFactory().buildGeometry(new ArrayList<>(kept));
	}

	/**
	 * Reduce los huecos de un poligono conservando como maximo {@code maxInteriorRings}
	 * anillos interiores. Si hay mas, se descartan los de menor area.
	 *
	 * @param polygon poligono de entrada; puede ser {@code null}
	 * @param maxInteriorRings cantidad maxima de huecos a conservar
	 * @return el poligono original si no supera el limite, o uno nuevo con los huecos mas grandes
	 */
	public static Polygon limitPolygonInteriorRings(Polygon polygon, int maxInteriorRings) {
		if (polygon == null || maxInteriorRings < 0 || polygon.getNumInteriorRing() <= maxInteriorRings) {
			return polygon;
		}
		LinearRing shell = polygon.getFactory().createLinearRing(polygon.getExteriorRing().getCoordinateSequence());
		List<LinearRing> rings = new ArrayList<>(polygon.getNumInteriorRing());
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			LinearRing hole = polygon.getFactory().createLinearRing(
					polygon.getInteriorRingN(i).getCoordinateSequence());
			rings.add(hole);
		}
		rings.sort((r1, r2) -> Double.compare(interiorRingArea(r2), interiorRingArea(r1)));
		LinearRing[] kept = rings.subList(0, maxInteriorRings).toArray(new LinearRing[0]);
		return polygon.getFactory().createPolygon(shell, kept);
	}

	private static double interiorRingArea(LinearRing ring) {
		return ring.getFactory().createPolygon(ring).getArea();
	}

	/**
	 * Aplica {@link #limitPolygonInteriorRings(Polygon, int)} a cada poligono de una geometria.
	 *
	 * @param geometry geometria de entrada; puede ser {@code null}
	 * @param maxInteriorRings cantidad maxima de huecos por poligono
	 * @return geometria con huecos acotados
	 */
	public static Geometry limitGeometryInteriorRings(Geometry geometry, int maxInteriorRings) {
		if (geometry == null || geometry.isEmpty() || maxInteriorRings < 0) {
			return geometry;
		}
		if (geometry instanceof Polygon) {
			return limitPolygonInteriorRings((Polygon) geometry, maxInteriorRings);
		}
		List<Geometry> parts = new ArrayList<>(geometry.getNumGeometries());
		for (int i = 0; i < geometry.getNumGeometries(); i++) {
			Geometry part = geometry.getGeometryN(i);
			if (part instanceof Polygon) {
				parts.add(limitPolygonInteriorRings((Polygon) part, maxInteriorRings));
			} else {
				parts.add(part);
			}
		}
		if (parts.size() == 1) {
			return parts.get(0);
		}
		return geometry.getFactory().buildGeometry(parts);
	}

	/**
	 * Atajo de exportacion: acota huecos con {@link #MAX_PRESCRIPTION_INTERIOR_RINGS}.
	 */
	public static Geometry limitPrescriptionInteriorRings(Geometry geometry) {
		return limitGeometryInteriorRings(geometry, MAX_PRESCRIPTION_INTERIOR_RINGS);
	}

	/**
	 * Variante para listas ya aplanadas: acota huecos en cada poligono.
	 */
	public static List<Polygon> limitFlatPolygonsInteriorRings(List<Polygon> polygons, int maxInteriorRings) {
		if (polygons == null || polygons.isEmpty() || maxInteriorRings < 0) {
			return polygons;
		}
		boolean changed = false;
		List<Polygon> limited = new ArrayList<>(polygons.size());
		for (Polygon polygon : polygons) {
			Polygon trimmed = limitPolygonInteriorRings(polygon, maxInteriorRings);
			if (trimmed != polygon) {
				changed = true;
			}
			limited.add(trimmed);
		}
		return changed ? limited : polygons;
	}

	/**
	 * Prepara una geometria para exportar prescripciones en tres pasos:
	 * <ol>
	 *   <li>fusionar partes cercanas con buffer para achicar el shapefile</li>
	 *   <li>si aun supera {@link #MAX_PRESCRIPTION_GEOMETRY_PARTS}, descartar las mas chicas</li>
	 *   <li>si algun poligono supera {@link #MAX_PRESCRIPTION_INTERIOR_RINGS}, descartar huecos chicos</li>
	 * </ol>
	 *
	 * @param geometry geometria de entrada; puede ser {@code null}
	 * @return geometria apta para exportacion
	 */
	public static Geometry limitPrescriptionGeometryParts(Geometry geometry) {
		Geometry merged = mergeNearbyPrescriptionGeometryParts(geometry);
		Geometry partsLimited = limitGeometryParts(merged, MAX_PRESCRIPTION_GEOMETRY_PARTS);
		return limitPrescriptionInteriorRings(partsLimited);
	}

	/**
	 * Variante para listas ya aplanadas por {@link PolygonValidator#geometryToFlatPolygons}.
	 * Cuando hay mas poligonos de los permitidos, primero intenta fusionar los cercanos
	 * recomponiendo una geometria multiparte y aplicando buffer; solo si aun sobran
	 * partes descarta las mas chicas.
	 *
	 * @param polygons lista de poligonos; puede ser {@code null}
	 * @param maxParts cantidad maxima de poligonos a conservar
	 * @return la lista original si no supera el limite, o una nueva lista con las
	 *         partes mas grandes
	 */
	public static List<Polygon> limitFlatPolygons(List<Polygon> polygons, int maxParts) {
		if (polygons == null || polygons.size() <= maxParts) {
			return polygons;
		}
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Geometry merged = mergeNearbyGeometryParts(
				fact.createGeometryCollection(polygons.toArray(new Polygon[0])),
				PRESCRIPTION_EXPORT_MERGE_BUFFER_METERS);
		List<Polygon> mergedFlat = PolygonValidator.geometryToFlatPolygons(merged);
		if (mergedFlat.size() <= maxParts) {
			return mergedFlat;
		}
		// Copiar para no mutar la lista original del item exportado.
		List<Polygon> sorted = new ArrayList<>(mergedFlat);
		sorted.sort((p1, p2) -> Double.compare(p2.getArea(), p1.getArea()));
		// Devolver una lista nueva: el caller puede seguir iterando sin sorpresas.
		return new ArrayList<>(sorted.subList(0, maxParts));
	}

	/**
	 * Atajo de exportacion: aplica {@link #limitFlatPolygons(List, int)} con el
	 * limite estandar de prescripciones ({@link #MAX_PRESCRIPTION_GEOMETRY_PARTS}).
	 *
	 * @param polygons lista de poligonos; puede ser {@code null}
	 * @return lista acotada al limite de exportacion de prescripciones
	 */
	public static List<Polygon> limitPrescriptionFlatPolygons(List<Polygon> polygons) {
		return limitFlatPolygonsInteriorRings(
				limitFlatPolygons(polygons, MAX_PRESCRIPTION_GEOMETRY_PARTS),
				MAX_PRESCRIPTION_INTERIOR_RINGS);
	}
}
