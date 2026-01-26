package com.ursulagis.desktop.gui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.utils.TooltipUtil;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.ExcelHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class MultiLayerHistoChart extends VBox {
	
	public static final String[] colors = {
		"rgb(244,109,67)",  //$NON-NLS-1$
		"rgb(253,174,97)", //$NON-NLS-1$
		"rgb(254,224,139)", //$NON-NLS-1$
		"rgb(255,255,191)", //$NON-NLS-1$
		"rgb(230,245,152)", //$NON-NLS-1$
		"rgb(171,221,164)", //$NON-NLS-1$
		"rgb(102,194,165)", //$NON-NLS-1$
		"rgb(50,136,189)", //$NON-NLS-1$
		"rgb(94,79,162)", //$NON-NLS-1$
		"rgb(158,1,66)", //$NON-NLS-1$
		"rgb(213,62,79)" //$NON-NLS-1$
	};

	private List<XYChart.Series<Number, Number>> seriesList = new ArrayList<>();
	private WorldWindow wwd;
	private Map<Labor<?>, LayerInfo> layerInfoMap = new HashMap<>();

	private static class LayerInfo {
		Double superficieTotal = 0.0;
		Double produccionTotal = 0.0;
		int numClasses;
		Double[] superficies;
		Double[] producciones;
		Labor<?> labor;
		
		LayerInfo(Labor<?> labor) {
			this.labor = labor;
			this.numClasses = labor.clasificador.getNumClasses();
			if(numClasses < colors.length) {
				superficies = new Double[numClasses];
				producciones = new Double[numClasses];
			} else {
				superficies = new Double[colors.length];
				producciones = new Double[colors.length];
			}
		}
	}

	public MultiLayerHistoChart(WorldWindow wwd) {
		super();
		this.wwd = wwd;
		
		// Get all active Labor layers
		List<Labor<?>> activeLabors = getActiveLabors();
		
		if (activeLabors.isEmpty()) {
			Label noLayersLabel = new Label(Messages.getString("MultiLayerHistoChart.NoActiveLayers")); //$NON-NLS-1$
			this.getChildren().add(noLayersLabel);
			return;
		}

		// Process each active layer to build histogram data
		for (Labor<?> labor : activeLabors) {
			processLabor(labor);
		}

		// Find the maximum number of classes across all layers
		int maxClasses = 0;
		for (Labor<?> labor : activeLabors) {
			maxClasses = Math.max(maxClasses, labor.clasificador.getNumClasses());
		}
		
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel(Messages.getString("CosechaHistoChart.10")); //$NON-NLS-1$ // Amount per Ha
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel(Messages.getString("CosechaHistoChart.11")); //$NON-NLS-1$ // Surface
		
		final LineChart<Number, Number> chart = new LineChart<Number, Number>(xAxis, yAxis);
		chart.setTitle(Messages.getString("MultiLayerHistoChart.Title")); //$NON-NLS-1$
		chart.setLegendVisible(true);
		
		// Create a series for each active layer
		int colorIndex = 0;
		for (Labor<?> labor : activeLabors) {
			XYChart.Series<Number, Number> series = createSeries(labor, maxClasses, colorIndex);
			if (series != null) {
				chart.getData().add(series);
				seriesList.add(series);
				colorIndex = (colorIndex + 1) % colors.length;
			}
		}

		VBox.setVgrow(chart, Priority.ALWAYS);
		this.getChildren().add(chart);
		
		// Add bottom panel with export button
		BorderPane bottom = new BorderPane();
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //$NON-NLS-1$
		exportButton.setOnAction(a -> doExportarExcel());
		right.getChildren().add(exportButton);
		bottom.setRight(right);
		bottom.setPadding(new Insets(2, 30, 30, 30));
		this.getChildren().add(bottom);
	}

	private List<Labor<?>> getActiveLabors() {
		List<Labor<?>> activeLabors = new ArrayList<>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Labor<?>) {
				activeLabors.add((Labor<?>) o);
			}
		}
		return activeLabors;
	}


	private void processLabor(Labor<?> labor) {
		LayerInfo info = new LayerInfo(labor);
		layerInfoMap.put(labor, info);

		SimpleFeatureIterator it = labor.outCollection.features();
		while (it.hasNext()) {
			SimpleFeature f = it.next();
			Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Double area = geometry.getArea() * ProyectionConstants.A_HAS();
			int categoria = labor.getClasificador().getCategoryFor(rinde);

			Double sup = categoria < info.superficies.length ? info.superficies[categoria] : info.superficies[info.superficies.length - 1];
			Double prod = categoria < info.producciones.length ? info.producciones[categoria] : info.producciones[info.producciones.length - 1];
			
			if (sup == null) sup = 0.0;
			if (prod == null) prod = 0.0;
			
			info.superficies[categoria] = sup + area;
			info.producciones[categoria] = prod + rinde * area;

			info.produccionTotal += rinde * area;
			info.superficieTotal += area;
		}
		it.close();
	}

	private XYChart.Series<Number, Number> createSeries(Labor<?> labor, int maxClasses, int colorIndex) {
		LayerInfo info = layerInfoMap.get(labor);
		if (info == null) {
			return null;
		}

		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		series.setName(labor.getNombre());

		TooltipUtil.setupCustomTooltipBehavior(50, 60000, 50);
		String color = colors[colorIndex % colors.length];

		// Create data points for all categories (using amount per ha as X value, surface as Y value)
		for (int categoryIndex = 0; categoryIndex < maxClasses; categoryIndex++) {
			Number superficie = 0.0;
			Number produccion = 0.0;
			Number rindePorHa = 0.0;
			String categoryName = "";
			
			if (categoryIndex < info.numClasses && categoryIndex < info.superficies.length) {
				superficie = info.superficies[categoryIndex] != null ? info.superficies[categoryIndex] : 0.0;
				produccion = info.producciones[categoryIndex] != null ? info.producciones[categoryIndex] : 0.0;
				// Calculate average amount per hectare for this category
				if (superficie.doubleValue() > 0) {
					rindePorHa = produccion.doubleValue() / superficie.doubleValue();
				}
				categoryName = labor.getClasificador().getCategoryNameFor(categoryIndex);
			}

			// X = amount per ha, Y = total surface area
			Data<Number, Number> cData = new XYChart.Data<>(rindePorHa, superficie);
			cData.setExtraValue(produccion);
			
			// Store category name in a way we can access it later
			final String finalCategoryName = categoryName;
			final LayerInfo finalInfo = info;
			final Labor<?> finalLabor = labor;
			
			cData.nodeProperty().addListener(new ChangeListener<Node>() {
				@Override
				public void changed(ObservableValue<? extends Node> ov, Node oldNode, Node newNode) {
					if (newNode != null) {
						newNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 3px;"); //$NON-NLS-1$ //$NON-NLS-2$
						NumberFormat df = Messages.getNumberFormat();

						double sup = cData.getYValue().doubleValue();
						double prod = (Double) cData.getExtraValue();
						double rinde = sup > 0 ? prod / sup : 0.0;
						double porcentaje = finalInfo.superficieTotal > 0 ? sup / finalInfo.superficieTotal * 100 : 0.0;

						Tooltip tooltip = new Tooltip(
								finalLabor.getNombre() + "\n" +
								(finalCategoryName.isEmpty() ? "" : finalCategoryName + "\n") +
								Messages.getString("CosechaHistoChart.10") + ": " + df.format(rinde) + "\n" +
								Messages.getString("CosechaHistoChart.21") + ": " + df.format(sup) + "\n" +
								df.format(porcentaje) + "% " + Messages.getString("MultiLayerHistoChart.OfTotal")); //$NON-NLS-1$
						tooltip.autoHideProperty().set(false);
						Tooltip.install(newNode, tooltip);
					}
				}
			});
			series.getData().add(cData);
		}

		return series;
	}

	private void doExportarExcel() {
		ExcelHelper xHelper = new ExcelHelper();
		// Convert to ObservableList for exportSeriesList
		javafx.collections.ObservableList<XYChart.Series<Number, Number>> observableList = 
			FXCollections.observableArrayList(seriesList);
		xHelper.exportSeriesList(observableList);
	}
}
