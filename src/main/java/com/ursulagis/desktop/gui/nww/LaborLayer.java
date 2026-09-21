package com.ursulagis.desktop.gui.nww;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.awt.AbstractViewInputHandler;
import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.utils.ProyectionConstants;
public class LaborLayer extends RenderableLayer {
	private static final Logger logger = Logger.getLogger(LaborLayer.class.getName());

	private RenderableLayer analyticSurfaceLayer=null;
	private RenderableLayer extrudedPolygonsLayer=null;
	private int elementsCount=0;
	private boolean showOnlyExtudedPolygons=false;
	private int screenPixelsSectorMinSize=3000;//2000 queda bueno
	/** Default extruded feature budget; adapts from rebuild time. */
	public static final int DEFAULT_MAX_EXTRUDED_ELEMENTS = 100000;
	public static final int MIN_MAX_EXTRUDED_ELEMENTS = 2000;
	public static final int MAX_MAX_EXTRUDED_ELEMENTS = 5000000;
	/** Target rebuild budget (ms); over → shrink cap, under → grow cap. */
	public static final long EXTRUDED_REBUILD_TARGET_MS = 2000;
	/** Adaptive max extruded features per rebuild (viewport shrinks until under this). */
	private static volatile int maxExtrudedElements = DEFAULT_MAX_EXTRUDED_ELEMENTS;

	public static int getMaxExtrudedElements() {
		return maxExtrudedElements;
	}

	/**
	 * Update the feature-cap target from a measured draw duration.
	 * Does not trigger a rebuild — the new cap applies on the next sector rebuild only.
	 * Over target → reduce; under target → increase (clamped).
	 */
	public static void adjustMaxExtrudedElements(long renderMs) {
		int current = maxExtrudedElements;
		int next;
		if (renderMs > EXTRUDED_REBUILD_TARGET_MS) {
			next = Math.max(MIN_MAX_EXTRUDED_ELEMENTS, (int) (current * 0.90));
		} else {
			next = Math.min(MAX_MAX_EXTRUDED_ELEMENTS, (int) (current * 1.10));
		}
		if (next != current) {
			maxExtrudedElements = next;
			logger.fine("maxExtrudedElements " + current + " → " + next
					+ " (renderMs=" + renderMs + ", applies next rebuild)");
		}
	}

	/**
	 * True when the user is (or is about to start) panning/zooming the map.
	 * Used to abort long extruded rebuild/draw so mouse drag can begin promptly.
	 */
	public static boolean isViewInteractionActive(DrawContext dc) {
		if (dc == null) {
			return false;
		}
		View view = dc.getView();
		if (view instanceof BasicView) {
			ViewInputHandler handler = ((BasicView) view).getViewInputHandler();
			if (handler != null) {
				if (handler instanceof AbstractViewInputHandler
						&& ((AbstractViewInputHandler) handler).getMouseDownPoint() != null) {
					return true;
				}
				if (handler.isAnimating()) {
					return true;
				}
			}
		}
		// Rebuild/draw runs on the AWT/GL thread; pending input sits in the queue until we return.
		try {
			EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
			return eq.peekEvent(MouseEvent.MOUSE_PRESSED) != null
					|| eq.peekEvent(MouseEvent.MOUSE_DRAGGED) != null
					|| eq.peekEvent(MouseEvent.MOUSE_WHEEL) != null;
		} catch (Exception e) {
			return false;
		}
	}

	long vS =0;
	private boolean extrudedRendered=false;

	/**
	 * Nested layers are not in the WW layer list — forward preRender so SurfaceImage
	 * can upload textures. Temporarily enable even if the last frame disabled it.
	 */
	@Override
	public void preRender(DrawContext dc) {
		if (!this.isEnabled() || dc == null) {
			return;
		}
		if (analyticSurfaceLayer != null) {
			boolean wasEnabled = analyticSurfaceLayer.isEnabled();
			analyticSurfaceLayer.setEnabled(true);
			try {
				analyticSurfaceLayer.preRender(dc);
			} finally {
				analyticSurfaceLayer.setEnabled(wasEnabled);
			}
		}
		if (extrudedPolygonsLayer != null && extrudedPolygonsLayer.isEnabled()) {
			extrudedPolygonsLayer.preRender(dc);
		}
		super.preRender(dc);
	}

	/** Enable, preRender (texture upload), then render SurfaceImage / AnalyticSurface. */
	private void renderSurfaceImage(DrawContext dc) {
		if (analyticSurfaceLayer == null) {
			return;
		}
		analyticSurfaceLayer.setEnabled(true);
		analyticSurfaceLayer.preRender(dc);
		analyticSurfaceLayer.render(dc);
	}

	@Override
	public void setOpacity(double op) {		
	    /**
	     * Opacity is not applied to layers of this type because each renderable typically has its own opacity control.
	     *
	     * @param opacity the current opacity value, which is ignored by this layer.
	     */
		super.setOpacity(op);
		logger.fine("setting opacity para labor layer "+op);
		if(extrudedPolygonsLayer!=null) {
			extrudedPolygonsLayer.setOpacity(op);
			
			
			for(Renderable r:extrudedPolygonsLayer.getRenderables()) {
				if(ExtrudedPolygon.class.isAssignableFrom(r.getClass())) {					
					ExtrudedPolygon renderablePolygon = (ExtrudedPolygon)r;
				
					//gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon;
				
		
					ShapeAttributes sideAttributes = renderablePolygon.getSideAttributes();
//					if(sideAttributes==null) {
//						sideAttributes=new BasicShapeAttributes();
//					}
//					sideAttributes.setOutlineWidth(0.01);
//					sideAttributes.setOutlineOpacity(0.01);
//					sideAttributes.setDrawInterior(true);
//					sideAttributes.setDrawOutline(false);
					sideAttributes.setInteriorOpacity(op);					
					renderablePolygon.setSideAttributes(sideAttributes);
					
					ShapeAttributes outerAttributes = renderablePolygon.getAttributes();
//					outerAttributes.setOutlineWidth(0.01);
//					outerAttributes.setOutlineOpacity(0.01);
//					outerAttributes.setDrawInterior(true);
//					outerAttributes.setDrawOutline(false);
					outerAttributes.setInteriorOpacity(op);
					renderablePolygon.setAttributes(outerAttributes);
			
				}
			}
			
			
		}
		if(analyticSurfaceLayer!=null) {
			SurfaceImageLayer imageLayer = (SurfaceImageLayer)analyticSurfaceLayer;
			imageLayer.setOpacity(op);
			for(Renderable r:imageLayer.getRenderables()) {
				if(ExportableAnalyticSurface.class.isAssignableFrom(r.getClass())) {					
					ExportableAnalyticSurface s = (ExportableAnalyticSurface)r;
					AnalyticSurfaceAttributes att = s.getSurfaceAttributes();
					att.setInteriorOpacity(op);
					s.setSurfaceAttributes(att);
				} else if (r instanceof SurfaceImage) {
					((SurfaceImage) r).setOpacity(op);
				}
			}
	
		}		
	}

	@Override
	public void dispose() {
		logger.fine("disposing of LaborLayer");
		if(extrudedPolygonsLayer!=null) {
			extrudedPolygonsLayer.dispose();
			extrudedPolygonsLayer=null;
		}
		if(analyticSurfaceLayer!=null) {
			analyticSurfaceLayer.dispose();
			analyticSurfaceLayer=null;
		}
		super.dispose();
	}

	public void render(DrawContext dc){
		if(!this.isEnabled())return;
		long vsNow =dc.getView().getViewStateID();

		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		boolean interacting = isViewInteractionActive(dc);
		// Skip extruded drawing while dragging/animating so AnalyticSurface stays responsive.
		boolean closeEnough = eyeElevation < screenPixelsSectorMinSize
				&& this.vS == vsNow
				&& !interacting;

		if (interacting) {
			// Abort any in-progress extruded rebuild/draw, then show SurfaceImage immediately.
			if (extrudedPolygonsLayer != null) {
				extrudedPolygonsLayer.render(dc);
			}
			renderSurfaceImage(dc);
		} else if (analyticSurfaceLayer == null || closeEnough) {
			// When zoomed in, drive extruded render so it can rebuild for the visible sector.
			// If the extruded layer stays empty (cap / debounce / no features), keep SurfaceImage on.
			if (extrudedPolygonsLayer != null) {
				extrudedPolygonsLayer.render(dc);
			}
			boolean hasExtruded = extrudedPolygonsLayer != null
					&& extrudedPolygonsLayer.getNumRenderables() > 0;
			if (hasExtruded) {
				if (analyticSurfaceLayer != null) {
					analyticSurfaceLayer.setEnabled(false);
				}
			} else if (analyticSurfaceLayer != null) {
				renderSurfaceImage(dc);
			}
		} else {
			renderSurfaceImage(dc);
		}
		this.vS = vsNow;
		//		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		//		if(extrudedPolygonsLayer!=null && analyticSurfaceLayer!=null){
		//			if (showOnlyExtudedPolygons || elementsCount<MAX_EXTRUDED_ELEMENTS || eyeElevation < screenPixelsSectorMinSize ){
		//				//System.out.println("dibujando para screenSize = " +sectorPixelSizeInWindow);
		//				//extrudedPolygonsLayer.setEnabled(true);
		//				//analyticSurfaceLayer.setEnabled(false);
		//				try{
		//					extrudedPolygonsLayer.render(dc);
		//					//showOnlyExtudedPolygons=false;
		//				}catch(Exception e){
		//					System.out.println("no se pudo dibujar el extudedPolygonsLayer");
		//					e.printStackTrace();
		//				}
		//
		//			}else{
		//				//analyticSurfaceLayer.setEnabled(true);
		//				//extrudedPolygonsLayer.setEnabled(false);					
		//				analyticSurfaceLayer.render(dc);	
		//
		//				Timer t = new Timer();
		//				t.schedule(new TimerTask(){
		//
		//					@Override
		//					public void run() {					
		//						showOnlyExtudedPolygons=true;
		//					//	extrudedPolygonsLayer.setEnabled(true);
		//					//	analyticSurfaceLayer.setEnabled(false);
		//
		//						System.out.println("tratando de abilitar extudedPolygonsLayer");
		//					}
		//
		//				}, 1000);
		//
		//
		//			}				
		//		}
		//		
		super.render(dc);
	}

	@Override
	public void pick(DrawContext dc, java.awt.Point point) {
		if (!this.isEnabled() || dc == null || point == null) {
			return;
		}

		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		boolean closeEnough = eyeElevation < screenPixelsSectorMinSize;
		boolean hasExtruded = extrudedPolygonsLayer != null
				&& extrudedPolygonsLayer.getNumRenderables() > 0;
		boolean showingExtruded = analyticSurfaceLayer == null || (closeEnough && hasExtruded);

		if (showingExtruded) {
			if (extrudedPolygonsLayer != null) {
				extrudedPolygonsLayer.pick(dc, point);
			}
			return;
		}

		// AnalyticSurface mode (incl. eye > 3km / extruded never built):
		// resolve the feature under the cursor — AnalyticSurface pick is not feature-accurate.
		pickLaborItemUnderCursor(dc, point);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void pickLaborItemUnderCursor(DrawContext dc, java.awt.Point point) {
		Object laborObj = this.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		if (!(laborObj instanceof Labor)) {
			if (analyticSurfaceLayer != null) {
				analyticSurfaceLayer.pick(dc, point);
			}
			return;
		}

		Position pos = dc.getView().computePositionFromScreenPoint(point.x, point.y);
		if (pos == null) {
			return;
		}

		LaborItem item = findLaborItemAt((Labor) laborObj, pos);
		if (item == null) {
			return;
		}

		Color pickColor = dc.getUniquePickColor();
		PickedObject po = new PickedObject(pickColor.getRGB(), item, pos, false);
		po.setOnTop();
		po.setParentLayer(this);
		dc.addPickedObject(po);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static LaborItem findLaborItemAt(Labor labor, Position pos) {
		if (labor == null || pos == null) {
			return null;
		}
		double lat = pos.getLatitude().degrees;
		double lon = pos.getLongitude().degrees;
		double dx = ProyectionConstants.metersToLong() * 5.0;
		double dy = ProyectionConstants.metersToLat() * 5.0;
		Envelope env = new Envelope(lon - dx, lon + dx, lat - dy, lat + dy);

		List items = labor.cachedOutStoreQuery(env);
		if (items == null || items.isEmpty()) {
			return null;
		}

		Point pt = ProyectionConstants.getGeometryFactory().createPoint(new Coordinate(lon, lat));
		// Only accept a hit if the cursor is on (or within ~1m of) the geometry.
		// Do not return a far "nearest" neighbor — that leaves tooltips stuck over empty space.
		double maxDist = Math.max(ProyectionConstants.metersToLong(), ProyectionConstants.metersToLat());
		LaborItem nearest = null;
		double bestDist = Double.MAX_VALUE;
		for (Object o : items) {
			if (!(o instanceof LaborItem)) {
				continue;
			}
			LaborItem item = (LaborItem) o;
			Geometry g = item.getGeometry();
			if (g == null || g.isEmpty()) {
				continue;
			}
			if (g.contains(pt) || g.covers(pt) || g.intersects(pt)) {
				return item;
			}
			double d = g.distance(pt);
			if (d < bestDist) {
				bestDist = d;
				nearest = item;
			}
		}
		return bestDist <= maxDist ? nearest : null;
	}

	/**
	 * @return the analyticSurfaceLayer
	 */
	public RenderableLayer getAnalyticSurfaceLayer() {
		return analyticSurfaceLayer;
	}

	/**
	 * @param analyticSurfaceLayer the analyticSurfaceLayer to set
	 */
	public void setAnalyticSurfaceLayer(RenderableLayer analyticSurfaceLayer) {
		this.analyticSurfaceLayer = analyticSurfaceLayer;
		// LaborLayer.pick resolves features under the cursor; surface itself stays non-pickable
		// (its native pick position is the sector centroid).
		if (this.analyticSurfaceLayer != null) {
			this.analyticSurfaceLayer.setPickEnabled(false);
		}
		this.setPickEnabled(true);
	}

	/**
	 * @return the extrudedPolygonsLayer
	 */
	public RenderableLayer getExtrudedPolygonsLayer() {
		return extrudedPolygonsLayer;
	}

	/**
	 * @param extrudedPolygonsLayer the extrudedPolygonsLayer to set
	 */
	public void setExtrudedPolygonsLayer(RenderableLayer extrudedPolygonsLayer) {	
		if(this.extrudedPolygonsLayer!=null) {
			this.renderables.remove(this.extrudedPolygonsLayer);

			this.extrudedPolygonsLayer.removeAllRenderables();
			this.extrudedPolygonsLayer.dispose();
			logger.fine("removing old extrudedPolygonsLayer");

		}
		this.extrudedPolygonsLayer = extrudedPolygonsLayer;
		this.elementsCount=extrudedPolygonsLayer.getNumRenderables();
		this.extrudedRendered=false;
	}

	/**
	 * @return the elementsCount
	 */
	public int getElementsCount() {
		return elementsCount;
	}

	/**
	 * @param elementsCount the elementsCount to set
	 */
	public void setElementsCount(int elementsCount) {
		this.elementsCount = elementsCount;
	}

	/**
	 * @return the showOnlyExtudedPolygons
	 */
	public boolean isShowOnlyExtudedPolygons() {
		return showOnlyExtudedPolygons;
	}

	/**
	 * @param showOnlyExtudedPolygons the showOnlyExtudedPolygons to set
	 */
	public void setShowOnlyExtudedPolygons(boolean showOnlyExtudedPolygons) {
		this.showOnlyExtudedPolygons = showOnlyExtudedPolygons;
	}

}
