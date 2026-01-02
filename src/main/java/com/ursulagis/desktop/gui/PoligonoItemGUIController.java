package com.ursulagis.desktop.gui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.gui.controller.AbstractGUIController;
import com.ursulagis.desktop.gui.nww.LayerAction;

import gov.nasa.worldwind.layers.Layer;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Clase que maneja los eventos directos sobre los polígonos
 */
public class PoligonoItemGUIController extends AbstractGUIController {
	Map<Class<?>,List<LayerAction>> predicates = new HashMap<Class<?>,List<LayerAction>>();

	public PoligonoItemGUIController(JFXMain _main) {
		super(_main);

		
		main.poligonoGUIController.addAccionesPoligonos(predicates);
		main.genericGUIController.addAccionesGenericas(predicates);
	}

	/**
	 * Llamado desde ToolTipController al hacer right click sobre el polígono
	 * Show a dialog with actions for a polygon (edit button)
	 * @param poli the Poligono to show actions for
	 */
	public void showDialog(Layer nuLayer) {
		Platform.runLater(() -> {
			try {
				// Get the Poligono from the layer
				Object layerObject = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (!(layerObject instanceof Poligono)) {
					return;
				}
				//Poligono poli = (Poligono) layerObject;

				ContextMenu menu = new ContextMenu();
				List<LayerAction> actions = predicates.get(Poligono.class);
				if (actions != null) {
					for(LayerAction a: actions) {
						MenuItem menuItem = new MenuItem(a.apply(null));
						menuItem.setOnAction(e -> a.apply(nuLayer));
						menu.getItems().add(menuItem);
					}
				}
				for(LayerAction a: predicates.get(Object.class)) {
					MenuItem menuItem = new MenuItem(a.apply(null));
					menuItem.setOnAction(e -> a.apply(nuLayer));
					menu.getItems().add(menuItem);
				}
				
				// Display ContextMenu at current mouse position
				Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
				menu.show(JFXMain.stage, mouseLocation.getX(), mouseLocation.getY());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
