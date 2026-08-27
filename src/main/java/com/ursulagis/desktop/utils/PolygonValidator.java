package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import java.util.logging.Logger;
public class PolygonValidator {
	private static final Logger logger = Logger.getLogger(PolygonValidator.class.getName());


	public static List<Polygon> geometryToFlatPolygons(Geometry itemGeometry){
		List<Polygon> ret=new ArrayList<Polygon>();

		if(itemGeometry == null){
			return ret;
		}

		if(itemGeometry instanceof Polygon) {
			// Single-part polygon - add it directly
			Polygon pi =(Polygon)itemGeometry;
			ret.add(polygonToFlatPolygon(pi));
		} else if(itemGeometry instanceof MultiPolygon){
			// MultiPolygon - recursively process each geometry
			MultiPolygon mp = (MultiPolygon)itemGeometry;
			for(int i=0;i<mp.getNumGeometries();i++){
				Geometry gi=mp.getGeometryN(i);
				ret.addAll(geometryToFlatPolygons(gi));
			}
		} else if(itemGeometry instanceof GeometryCollection) {
			// Handle GeometryCollection by processing each geometry recursively
			GeometryCollection gc = (GeometryCollection)itemGeometry;
			for(int i=0;i<gc.getNumGeometries();i++){
				Geometry gi=gc.getGeometryN(i);
				ret.addAll(geometryToFlatPolygons(gi));
			}
		} else if(itemGeometry.getNumGeometries() > 1){
			// Handle any other geometry type that contains multiple geometries
			for(int i=0;i<itemGeometry.getNumGeometries();i++){
				Geometry gi=itemGeometry.getGeometryN(i);
				ret.addAll(geometryToFlatPolygons(gi));
			}
		} else {
			logger.fine("geometry no es multiPolygon ni poligon "+ itemGeometry);
		}
		return ret;
	}

	/**
	 * metodo que toma un poligono y delvuelve un poligono igual nuevo pero sin la coordenada z.
	 * @param pi
	 * @return
	 */
	public static Polygon polygonToFlatPolygon(Polygon pi){
		GeometryFactory fact = pi.getFactory();		
		LinearRing shell = fact.createLinearRing(coordsToFlat( pi.getExteriorRing().getCoordinates()));
		LinearRing[] holes = new LinearRing[pi.getNumInteriorRing()];
		for(int i=0;i<pi.getNumInteriorRing();i++){
			holes[i]=fact.createLinearRing(coordsToFlat( pi.getInteriorRingN(i).getCoordinates()));
		}
		Polygon p = pi.getFactory().createPolygon(shell,holes);
		return p;
	}

	/**
	 * metodo que elimina la coordenada z de las coordenadas
	 * @param boundaryCoords
	 * @return
	 */
	public static Coordinate[] coordsToFlat(Coordinate[] boundaryCoords) {		
		Coordinate[] coordinates = new Coordinate[boundaryCoords.length];
		for(int i =0;i<boundaryCoords.length;i++){						
			Coordinate c = boundaryCoords[i];
			coordinates[i]=new Coordinate(c.x,c.y);
		}
		return coordinates;
	}

	/** Strip Z/M so JTS overlay and validation run in 2D. */
	public static Geometry force2D(Geometry geom) {
		if (geom == null) {
			return null;
		}
		if (geom instanceof Polygon) {
			return polygonToFlatPolygon((Polygon) geom);
		}
		if (geom instanceof MultiPolygon) {
			int n = geom.getNumGeometries();
			Polygon[] polys = new Polygon[n];
			for (int i = 0; i < n; i++) {
				polys[i] = polygonToFlatPolygon((Polygon) geom.getGeometryN(i));
			}
			return geom.getFactory().createMultiPolygon(polys);
		}
		Geometry copy = geom.copy();
		copy.apply(new CoordinateFilter() {
			@Override
			public void filter(Coordinate coord) {
				try {
					coord.setZ(Double.NaN);
				} catch (IllegalArgumentException ignored) {
					// XY-only coordinate
				}
				try {
					coord.setM(Double.NaN);
				} catch (IllegalArgumentException ignored) {
					// no M ordinate
				}
			}
		});
		return copy;
	}



	/**
	 * Get / create a valid version of the geometry given. If the geometry is a polygon or multi polygon, self intersections /
	 * inconsistencies are fixed. Otherwise the geometry is returned.
	 * 
	 * @param geom
	 * @return a valid geometry or null if not posible
	 */
	@SuppressWarnings("unchecked")
	public static Geometry validate(Geometry geom){
		try {
			geom = force2D(geom);
			Geometry fixed = GeometryFixer.fix(geom);
			if (fixed instanceof Polygon || fixed instanceof MultiPolygon) {
				try {
					if (fixed.isValid()) {
						fixed.normalize();
						return fixed;
					}
				} catch (Exception ignored) {
					// fall through to polygonizer
				}
			}
			if(geom instanceof Polygon){
				try {
				if(geom.isValid()){//exception por cannot compute quadrant for point (0.0, 0.0)
					geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
					return geom; // If the polygon is valid just return it
				}
				}catch(Exception e) {
					logger.fine("no puedo ver si geom.isValid "+geom);
				//	e.printStackTrace();
				}
				Polygonizer polygonizer = new Polygonizer();
			
				addPolygon((Polygon)geom, polygonizer);
				return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
			}else if(geom instanceof MultiPolygon){
				try {
					if(geom!=null && geom.isValid()){
					geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
					return geom; // If the multipolygon is valid just return it
				}}catch(Exception ex){
					logger.fine("no puedo ver si geom.isValid "+geom);
					//ex.printStackTrace();
				}
				Polygonizer polygonizer = new Polygonizer();
				for(int n = geom.getNumGeometries(); n-- > 0;){
					addPolygon((Polygon)geom.getGeometryN(n), polygonizer);
				}
				return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
			}else{
				return geom; // In my case, I only care about polygon / multipolygon geometries
			}
		}catch(Exception e) {//java.lang.IllegalArgumentException: Cannot compute the quadrant for point ( 0.0, 0.0 )
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Add all line strings from the polygon given to the polygonizer given
	 * 
	 * @param polygon polygon from which to extract line strings
	 * @param polygonizer polygonizer
	 */
	static void addPolygon(Polygon polygon, Polygonizer polygonizer){
		addLineString(polygon.getExteriorRing(), polygonizer);
		for(int n = polygon.getNumInteriorRing(); n-- > 0;){
			addLineString(polygon.getInteriorRingN(n), polygonizer);
		}
	}

	/**
	 * Add the linestring given to the polygonizer
	 * 
	 * @param linestring line string
	 * @param polygonizer polygonizer
	 */
	static void addLineString(LineString lineString, Polygonizer polygonizer){

		if(lineString instanceof LinearRing){ // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing
			lineString = lineString.getFactory().createLineString(lineString.getCoordinateSequence());
		}

		// unioning the linestring with the point makes any self intersections explicit.
		Geometry toAdd;
		try {
			Point point = lineString.getFactory().createPoint(lineString.getCoordinateN(0));
			toAdd = lineString.union(point);
		} catch (RuntimeException e) {
			toAdd = lineString;
		}
		polygonizer.add(toAdd);
	}

	/**
	 * Get a geometry from a collection of polygons.
	 * 
	 * @param polygons collection
	 * @param factory factory to generate MultiPolygon if required
	 * @return null if there were no polygons, the polygon if there was only one, or a MultiPolygon containing all polygons otherwise
	 */
	static Geometry toPolygonGeometry(Collection<Polygon> polygons, GeometryFactory factory){
		switch(polygons.size()){
		case 0:
			return null; // No valid polygons!
		case 1:
			return polygonToFlatPolygon(polygons.iterator().next());
		default:
			Polygon[] flat = new Polygon[polygons.size()];
			int i = 0;
			for (Polygon p : polygons) {
				flat[i++] = polygonToFlatPolygon(p);
			}
			GeometryCollection collection = factory.createGeometryCollection(flat);
			try {
				return collection.union();
			} catch (RuntimeException e) {
				try {
					return EnhancedPrecisionOp.buffer(collection, 0);
				} catch (RuntimeException e2) {
					return factory.createMultiPolygon(flat);
				}
			}
		}
	}
}