package com.ursulagis.desktop.gui.nww;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.render.Highlightable;
import gov.nasa.worldwind.render.SurfacePolygon;

/**
 * Highlights picked objects on rollover. When the picked object belongs to a
 * {@link MeasureToolForShape} multipolygon group, all parts are highlighted together.
 */
public class MultiPartHighlightController implements SelectListener {

    protected WorldWindow wwd;
    protected String eventAction = SelectEvent.ROLLOVER;
    protected List<Object> highlightedObjects = new ArrayList<>();

    public MultiPartHighlightController(WorldWindow wwd, String eventAction) {
        this.wwd = wwd;
        this.eventAction = eventAction;
        this.wwd.addSelectListener(this);
    }

    public void dispose() {
        this.wwd.removeSelectListener(this);
    }

    @Override
    public void selected(SelectEvent event) {
        if (this.eventAction != null && !this.eventAction.equals(event.getEventAction())) {
            return;
        }

        highlightGroup(event.getTopObject());
    }

    protected void highlightGroup(Object topObject) {
        List<Object> nextGroup = resolveHighlightGroup(topObject);
        if (isSameHighlightGroup(this.highlightedObjects, nextGroup)) {
            return;
        }

        unhighlightAll();
        this.highlightedObjects = nextGroup;
        for (Object object : this.highlightedObjects) {
            doHighlight(object);
        }
        this.wwd.redraw();
    }

    protected List<Object> resolveHighlightGroup(Object topObject) {
        if (topObject instanceof AVList) {
            Object group = ((AVList) topObject).getValue(MeasureToolForShape.SURFACE_SHAPE_GROUP);
            if (group instanceof MeasureToolForShape) {
                List<Object> shapes = new ArrayList<>();
                for (SurfacePolygon shape : ((MeasureToolForShape) group).getSurfaceShapes()) {
                    shapes.add(shape);
                }
                return shapes;
            }
        }

        if (topObject != null) {
            return Collections.singletonList(topObject);
        }
        return Collections.emptyList();
    }

    protected boolean isSameHighlightGroup(List<Object> currentGroup, List<Object> nextGroup) {
        if (currentGroup.size() != nextGroup.size()) {
            return false;
        }
        for (Object object : nextGroup) {
            if (!currentGroup.contains(object)) {
                return false;
            }
        }
        return true;
    }

    protected void doHighlight(Object object) {
        if (object instanceof Highlightable) {
            ((Highlightable) object).setHighlighted(true);
        }
    }

    protected void unhighlightAll() {
        for (Object object : this.highlightedObjects) {
            if (object instanceof Highlightable) {
                ((Highlightable) object).setHighlighted(false);
            }
        }
    }
}
