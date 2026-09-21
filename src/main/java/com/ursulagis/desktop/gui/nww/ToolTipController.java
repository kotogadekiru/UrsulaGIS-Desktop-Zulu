/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package com.ursulagis.desktop.gui.nww;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.LaborItemGUIController;
import com.ursulagis.desktop.gui.PoligonLayerFactory;
import com.ursulagis.desktop.gui.PoligonoItemGUIController;
import com.ursulagis.desktop.tasks.ProcessMapTask;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * Controls display of tool tips on picked objects. Any shape implementing {@link AVList} can participate. Shapes
 * provide tool tip text in their AVList for either or both of hover and rollover events. The keys associated with the
 * text are specified to the constructor.
 * <p>
 * For AnalyticSurface labors (where the surface pick is not feature-accurate), resolves the underlying
 * {@link LaborItem} from the cursor geographic position. Also listens to mouse motion so tooltips still work
 * when the view is high and no extruded polygons are pickable (WorldWind may not fire rollover in that case).
 *
 * @author tag
 * @version $Id: ToolTipController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ToolTipController implements SelectListener, Disposable, MouseListener, MouseMotionListener
{
	private static final Logger logger = Logger.getLogger(ToolTipController.class.getName());

    protected WorldWindow wwd;
    protected String hoverKey = AVKey.HOVER_TEXT;
    protected String rolloverKey = AVKey.ROLLOVER_TEXT;
    protected Object lastRolloverObject;
    protected Object lastHoverObject;
    protected AnnotationLayer layer;
    protected ToolTipAnnotation annotation;
	private Object lastRightClickObject;
	protected JFXMain main;

    /**
     * Create a controller for a specified {@link WorldWindow} that displays tool tips on hover and/or rollover.
     *
     * @param wwd         the World Window to monitor.
     * @param rolloverKey the key to use when looking up tool tip text from the shape's AVList when a rollover event
     *                    occurs. May be null, in which case a tool tip is not displayed for rollover events.
     * @param hoverKey    the key to use when looking up tool tip text from the shape's AVList when a hover event
     *                    occurs. May be null, in which case a tool tip is not displayed for hover events.
     * @param main 
     */
    public ToolTipController(WorldWindow wwd, String rolloverKey, String hoverKey, JFXMain _main){
        this.wwd = wwd;
        this.hoverKey = hoverKey;
        this.rolloverKey = rolloverKey;
        this.main=_main;
        this.wwd.addSelectListener(this);
        if (this.wwd.getInputHandler() != null) {
        	this.wwd.getInputHandler().addMouseListener(this);
        	this.wwd.getInputHandler().addMouseMotionListener(this);
        }
    }

    /**
     * Create a controller for a specified {@link WorldWindow} that displays "DISPLAY_NAME" on rollover.
     *
     * @param wwd         the World Window to monitor.
     */
    public ToolTipController(WorldWindow wwd) {
        this.wwd = wwd;
        this.rolloverKey = AVKey.DISPLAY_NAME;

        this.wwd.addSelectListener(this);
        if (this.wwd.getInputHandler() != null) {
        	this.wwd.getInputHandler().addMouseListener(this);
        	this.wwd.getInputHandler().addMouseMotionListener(this);
        }
    }

    public void dispose(){
        this.wwd.removeSelectListener(this);
        if (this.wwd.getInputHandler() != null) {
        	this.wwd.getInputHandler().removeMouseListener(this);
        	this.wwd.getInputHandler().removeMouseMotionListener(this);
        }
    }

    protected String getHoverText(SelectEvent event)  {
        return event.getTopObject() != null && event.getTopObject() instanceof AVList ?
            ((AVList) event.getTopObject()).getStringValue(this.hoverKey) : null;
    }

    protected String getRolloverText(SelectEvent event) {
    	Object obj = event.getTopObject();
    	String ret=null;
    	if(obj instanceof AVList) {
    		ret = ((AVList) obj).getStringValue(this.rolloverKey);
    	}
    	return ret;
    }

    public void selected(SelectEvent event) {
        try {
            // Rollover fires while moving; hover fires after a dwell. Both must show tooltips.
            if (event.isRollover() || event.isHover()) {
            	this.handleRollover(event);
            }

           // Right-click for labor items is handled only in mouseClicked (below).
           // Handling it here too opened a second ContextMenu that stayed stuck.
           if(event.isRightClick()) {
        	   this.handleRigthClick(event);
           }
            
        } catch (Exception e) {
            Logging.logger().warning(e.getMessage() != null ? e.getMessage() : e.toString());
        }

    }

	protected void handleRigthClick(SelectEvent event)  {
		// LaborItem menus are opened from mouseClicked only — do not duplicate here.

        if (event.getTopObject() != null && event.getTopObject() instanceof AVList) {
            this.lastRightClickObject = event.getTopObject();

            // Check if it's a SurfacePolygon (polygon)
            if (this.lastRightClickObject instanceof SurfacePolygon) {
                SurfacePolygon surfacePolygon = (SurfacePolygon) this.lastRightClickObject;
                
                // Find the layer that contains this SurfacePolygon
                RenderableLayer polygonLayer = findLayerForSurfacePolygon(surfacePolygon);
                if (polygonLayer != null) {
                    Object layerObject = polygonLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
                    if (layerObject instanceof Poligono) {
                        if (main != null) {
                            PoligonoItemGUIController controller = new PoligonoItemGUIController(main);
                            controller.showDialog(polygonLayer);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Find the RenderableLayer that contains the given SurfacePolygon
     * @param surfacePolygon the SurfacePolygon to find the layer for
     * @return the RenderableLayer containing the polygon, or null if not found
     */
    private RenderableLayer findLayerForSurfacePolygon(SurfacePolygon surfacePolygon) {
        if (this.wwd == null || this.wwd.getModel() == null) {
            return null;
        }
        
        LayerList layers = this.wwd.getModel().getLayers();
        for (Layer layer : layers) {
            if (layer instanceof RenderableLayer) {
                RenderableLayer renderableLayer = (RenderableLayer) layer;
                
                // Check if this layer has a MeasureToolForShape that contains this SurfacePolygon
                if (renderableLayer.hasKey(PoligonLayerFactory.MEASURE_TOOL)) {
                    Object measureToolObj = renderableLayer.getValue(PoligonLayerFactory.MEASURE_TOOL);
                    if (measureToolObj instanceof MeasureToolForShape) {
                        MeasureToolForShape measureTool = (MeasureToolForShape) measureToolObj;
                        if (measureTool.getSurfaceShape() == surfacePolygon) {
                            return renderableLayer;
                        }
                    }
                }
                
                // Also check if the SurfacePolygon is directly in this layer's renderables
                for (Renderable renderable : renderableLayer.getRenderables()) {
                    if (renderable == surfacePolygon) {
                        return renderableLayer;
                    }
                }
            }
        }
        return null;
    }
    
	/**
	 * metodo que se usa para mostrar los tooltips de los layers
	 * @param event
	 */
	protected void handleRollover(SelectEvent event)  {
		LaborItem laborItem = resolveLaborItem(event);
		String rolloverText = null;
		Object rolloverObject = laborItem != null ? laborItem : event.getTopObject();

		if (laborItem != null) {
			rolloverText = ProcessMapTask.createTooltipForLaborItem(laborItem.getGeometry(), laborItem);
		} else if (rolloverObject instanceof gov.nasa.worldwind.render.SurfacePolygon) {
			rolloverText = createTooltipForSurfacePolygon(
					(gov.nasa.worldwind.render.SurfacePolygon) rolloverObject);
		} else if (rolloverObject instanceof ReusableExtrudedPolygon) {
			LaborItem dao = (LaborItem) ((ReusableExtrudedPolygon) rolloverObject)
					.getValue(ProcessMapTask.LABOR_ITEM_AVKey);
			if (dao != null) {
				rolloverText = ProcessMapTask.createTooltipForLaborItem(dao.getGeometry(), dao);
				rolloverObject = dao;
			}
		}

		if (WWUtil.isEmpty(rolloverText)) {
			if (this.lastRolloverObject != null) {
				this.hideToolTip();
				this.lastRolloverObject = null;
				this.wwd.redraw();
			}
			return;
		}

		if (this.lastRolloverObject != null && this.lastRolloverObject == rolloverObject) {
			if (annotation != null && event.getPickPoint() != null) {
				annotation.setScreenPoint(event.getPickPoint());
				this.wwd.redraw();
			}
			return;
		}

		this.hideToolTip();
		this.lastRolloverObject = rolloverObject;
		this.showToolTip(event, rolloverText.replace("\\n", "\n"));
		this.wwd.redraw();
	}

    /**
     * Resolve the LaborItem under the cursor from the pick object, or by spatial
     * query when AnalyticSurface/terrain is on top of the labor.
     */
    private LaborItem resolveLaborItem(SelectEvent event) {
    	Object top = event != null ? event.getTopObject() : null;
    	if (top instanceof LaborItem) {
    		return (LaborItem) top;
    	}
    	if (top instanceof AVList) {
    		Object v = ((AVList) top).getValue(ProcessMapTask.LABOR_ITEM_AVKey);
    		if (v instanceof LaborItem) {
    			return (LaborItem) v;
    		}
    	}
    	// Scan full pick list (LaborItem may be under terrain)
    	if (event != null && event.getObjects() != null) {
    		for (PickedObject po : event.getObjects()) {
    			if (po == null) {
    				continue;
    			}
    			Object o = po.getObject();
    			if (o instanceof LaborItem) {
    				return (LaborItem) o;
    			}
    			if (o instanceof AVList) {
    				Object v = ((AVList) o).getValue(ProcessMapTask.LABOR_ITEM_AVKey);
    				if (v instanceof LaborItem) {
    					return (LaborItem) v;
    				}
    			}
    		}
    	}
    	return findLaborItemAtPosition(positionFromEvent(event));
    }

    private Position positionFromEvent(SelectEvent event) {
    	Position cur = this.wwd.getCurrentPosition();
    	if (cur != null) {
    		return cur;
    	}
    	if (event != null && event.getPickPoint() != null && this.wwd.getView() != null) {
    		return this.wwd.getView().computePositionFromScreenPoint(
    				event.getPickPoint().x, event.getPickPoint().y);
    	}
    	return null;
    }

    private Position positionFromMouse(MouseEvent e) {
    	Position cur = this.wwd.getCurrentPosition();
    	if (cur != null) {
    		return cur;
    	}
    	if (e != null && this.wwd.getView() != null) {
    		return this.wwd.getView().computePositionFromScreenPoint(e.getX(), e.getY());
    	}
    	return null;
    }

    private LaborItem findLaborItemAtPosition(Position pos) {
    	if (pos == null || this.wwd == null) {
    		return null;
    	}
    	for (Labor<?> labor : LaborItemGUIController.getLaboresCargadas(this.wwd)) {
    		if (labor.getLayer() == null || !labor.getLayer().isEnabled()) {
    			continue;
    		}
    		LaborItem item = LaborLayer.findLaborItemAt(labor, pos);
    		if (item != null) {
    			return item;
    		}
    	}
    	return null;
    }

    private LaborItem resolveLaborItemFromPickList() {
    	PickedObjectList pol = this.wwd.getObjectsAtCurrentPosition();
    	if (pol == null) {
    		return null;
    	}
    	for (PickedObject po : pol) {
    		if (po == null) {
    			continue;
    		}
    		Object o = po.getObject();
    		if (o instanceof LaborItem) {
    			return (LaborItem) o;
    		}
    		if (o instanceof AVList) {
    			Object v = ((AVList) o).getValue(ProcessMapTask.LABOR_ITEM_AVKey);
    			if (v instanceof LaborItem) {
    				return (LaborItem) v;
    			}
    		}
    	}
    	return null;
    }

    protected void handleHover(SelectEvent event) {
    	//System.out.println("hover");
        if (this.lastHoverObject != null){
            if (this.lastHoverObject == event.getTopObject())
                return;

            this.hideToolTip();
            this.lastHoverObject = null;
            //this.wwd.redraw();
        }

        if (getHoverText(event) != null) {
            this.lastHoverObject = event.getTopObject();
            this.showToolTip(event, getHoverText(event).replace("\\n", "\n"));
            //this.wwd.redraw();
        }
        this.wwd.redraw();
    }

    protected void showToolTip(SelectEvent event, String text) {
        if (WWUtil.isEmpty(text)) {
    		return;
    	}
       
        if (layer == null) {
            layer = new AnnotationLayer();
            layer.setPickEnabled(false);
            this.addLayer(layer);
        }else {
        	layer.setEnabled(true);
        }

        if (annotation != null) {
            annotation.setText(text);
        }  else  {
            annotation = new ToolTipAnnotation(text);
            logger.fine("creando nuevo tooltip");
            layer.addAnnotation(annotation);
        }
        if (event != null && event.getPickPoint() != null) {
        	annotation.setScreenPoint(event.getPickPoint());
        }
    }

    protected void showToolTipAtScreenPoint(java.awt.Point screenPoint, String text) {
    	if (WWUtil.isEmpty(text)) {
    		return;
    	}
        if (layer == null) {
            layer = new AnnotationLayer();
            layer.setPickEnabled(false);
            this.addLayer(layer);
        } else {
        	layer.setEnabled(true);
        }
        if (annotation != null) {
            annotation.setText(text);
        } else {
            annotation = new ToolTipAnnotation(text);
            layer.addAnnotation(annotation);
        }
        if (screenPoint != null) {
        	annotation.setScreenPoint(screenPoint);
        }
        this.wwd.redraw();
    }

    protected void hideToolTip() {
        if (this.layer != null) {
        	layer.setEnabled(false);
        }
    }

    protected void addLayer(Layer layer)
    {
        if (!this.wwd.getModel().getLayers().contains(layer))
            ApplicationTemplate.insertBeforeCompass(this.wwd, layer);
    }

    protected void removeLayer(Layer layer)
    {
        this.wwd.getModel().getLayers().remove(layer);
    }
    
    /**
     * Create tooltip text for SurfacePolygon shapes
     * @param surfacePolygon the SurfacePolygon to create tooltip for
     * @return tooltip text
     */
    protected String createTooltipForSurfacePolygon(gov.nasa.worldwind.render.SurfacePolygon surfacePolygon) {
        StringBuilder sb = new StringBuilder();
        
        // Get name if available
        String name = surfacePolygon.getStringValue(AVKey.DISPLAY_NAME);
        if (name != null && !name.isEmpty()) {
            sb.append("Name: ").append(name).append("\n");
        }
        
        // Get area if available
        String area = surfacePolygon.getStringValue("AREA");
        if (area != null && !area.isEmpty()) {
            sb.append("Area: ").append(area).append("\n");
        }
        
        // Get perimeter if available
        String perimeter = surfacePolygon.getStringValue("PERIMETER");
        if (perimeter != null && !perimeter.isEmpty()) {
            sb.append("Perimeter: ").append(perimeter).append("\n");
        }
        
        // Get any custom properties
        if (surfacePolygon.hasKey("DESCRIPTION")) {
            String description = surfacePolygon.getStringValue("DESCRIPTION");
            if (description != null && !description.isEmpty()) {
                sb.append("Description: ").append(description);
            }
        }
        
        return sb.length() > 0 ? sb.toString() : "Surface Polygon";
    }

	// --- MouseListener: WorldWind often does not fire SelectEvent right-click ---

	@Override
	public void mouseClicked(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e)) {
			return;
		}
		try {
			LaborItem item = resolveLaborItemFromPickList();
			if (item == null) {
				item = findLaborItemAtPosition(positionFromMouse(e));
			}
			if (item != null) {
				this.lastRightClickObject = item;
				LaborItemGUIController controller = new LaborItemGUIController(main);
				controller.showDialog(item);
			}
		} catch (Exception ex) {
			Logging.logger().warning(ex.getMessage() != null ? ex.getMessage() : ex.toString());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) { /* unused */ }

	@Override
	public void mouseReleased(MouseEvent e) { /* unused */ }

	@Override
	public void mouseEntered(MouseEvent e) { /* unused */ }

	@Override
	public void mouseExited(MouseEvent e) {
		this.hideToolTip();
		this.lastRolloverObject = null;
		if (this.wwd != null) {
			this.wwd.redraw();
		}
	}

	// --- MouseMotionListener: capture hover when AnalyticSurface is shown at high altitude
	// (no extruded pickables → WorldWind may not fire SelectEvent rollover/hover) ---

	@Override
	public void mouseMoved(MouseEvent e) {
		updateLaborTooltipFromScreenPoint(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		updateLaborTooltipFromScreenPoint(e.getPoint());
	}

	/**
	 * Position-based tooltip update independent of WorldWind pick/SelectEvent.
	 * Needed when eye elevation &gt; ~3km and extruded polygons were never built.
	 */
	private void updateLaborTooltipFromScreenPoint(java.awt.Point screenPoint) {
		if (screenPoint == null || this.wwd == null) {
			return;
		}
		try {
			LaborItem item = resolveLaborItemFromPickList();
			if (item == null) {
				item = findLaborItemAtPosition(positionFromScreenPoint(screenPoint));
			}

			if (item == null) {
				if (this.lastRolloverObject != null || this.layer != null || this.annotation != null) {
					this.hideToolTip();
					this.lastRolloverObject = null;
					this.wwd.redraw();
				}
				return;
			}

			String text = ProcessMapTask.createTooltipForLaborItem(item.getGeometry(), item);
			if (WWUtil.isEmpty(text)) {
				return;
			}

			if (this.lastRolloverObject == item) {
				if (this.annotation != null) {
					this.annotation.setScreenPoint(screenPoint);
					this.wwd.redraw();
				}
				return;
			}

			this.lastRolloverObject = item;
			this.showToolTipAtScreenPoint(screenPoint, text.replace("\\n", "\n"));
		} catch (Exception ex) {
			Logging.logger().warning(ex.getMessage() != null ? ex.getMessage() : ex.toString());
		}
	}

	private Position positionFromScreenPoint(java.awt.Point screenPoint) {
		Position cur = this.wwd.getCurrentPosition();
		if (cur != null) {
			return cur;
		}
		if (screenPoint != null && this.wwd.getView() != null) {
			return this.wwd.getView().computePositionFromScreenPoint(screenPoint.x, screenPoint.y);
		}
		return null;
	}
}
