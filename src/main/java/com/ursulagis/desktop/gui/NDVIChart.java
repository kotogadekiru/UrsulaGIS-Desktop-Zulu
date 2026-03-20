package com.ursulagis.desktop.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import com.ursulagis.desktop.tasks.ProcessMapTask;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import com.ursulagis.desktop.utils.DaylightCalculator;
import com.ursulagis.desktop.utils.ExcelHelper;
//agrege que extende de vbox 
public class NDVIChart extends VBox {
	private WorldWindow wwd;
	private LineChart<Number,Number> lineChart =null;


	public NDVIChart(WorldWindow _wwd) {
		super ();//sueper
		this.wwd=_wwd;
		//	this.layerPanel=_lP;

	}

	public void doShowNDVIChart(boolean acumulado) {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		List<SurfaceImageLayer> ndviLayers = extractLayers();
	
		//System.out.println("mostrar grafico");
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel(Messages.getString("JFXMain.show_ndvi_chart.Fecha"));//	NDVIHistoChart.fecha"));
		xAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number epochDay) {
				try {
					return LocalDate.ofEpochDay(epochDay.longValue()).toString();
				}catch(Exception e) {
					return "";
				}				
			}
			

			@Override
			public Number fromString(String string) {
				try {
				return LocalDate.parse(string).toEpochDay();
				}catch(Exception e) {
					return 0;
				}
			
			}			
		});
		
		xAxis.setLowerBound(Double.MAX_VALUE);	
		xAxis.setAutoRanging(false);
		
		final NumberAxis yAxis = new NumberAxis();
		if(!acumulado) {
			//yAxis.setLabel("NDVI");
			yAxis.setLabel(Messages.getString("NDVIHistoChart.NDVI"));
		} else {//acumulado
			yAxis.setLabel(Messages.getString("NDVIHistoChart.DIAS_NDVI"));
			//yAxis.setLabel("DIAS NDVI 100%");//TODO traducir	
		}
	
		ObservableList<Series<Number, Number>> data = FXCollections.observableArrayList();// new ArrayList<Series<Number, Number>>();

		Map<String, List<SurfaceImageLayer>>  contornoMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					return lNdvi.getContorno().getNombre();// fecha me devuelve siempre hoy por eso no hace la animacion
				}));

		final Double[] latitudeForDaylight = new Double[1];

		contornoMap.keySet().stream().forEach((c)->{
			XYChart.Series<Number,Number> sr = new XYChart.Series<Number,Number>(); 
			sr.setName(c );
			LocalDate[] lastFecha =new LocalDate[1];//.now();
			SimpleDoubleProperty ndviAcumProp = new SimpleDoubleProperty(0);
		
			contornoMap.get(c).stream()
			.map((layer)->(Ndvi)layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR))
			.sorted((n1,n2)->n1.compareTo(n2))
			.forEachOrdered(lNdvi->{
				try {
					Position centerPosition = (Position) lNdvi.getLayer().getValue(ProcessMapTask.ZOOM_TO_KEY);
					if (centerPosition != null && latitudeForDaylight[0] == null) {
						latitudeForDaylight[0] = centerPosition.getLatitude().degrees;
					}
				if (acumulado == true) {
					LocalDate fecha = lNdvi.getFecha();
					double dias=5/2;//el minimo aporte que deberia tener una imagen es 5 dias
					if(lastFecha[0]==null) {
						lastFecha[0]=fecha;					
					} else {										
						if(centerPosition!=null) {	
							DaylightCalculator daylightCalculator = new DaylightCalculator(centerPosition.getLatitude().degrees);
							//double daylightHours = daylightCalculator.getTotalDaylightHours(lastFecha[0], fecha);
							double totalRadiationMj = daylightCalculator.getTotalSolarRadiationMjBetween(lastFecha[0], fecha);
							dias = totalRadiationMj;//(daylightHours / 24);
							lastFecha[0]=fecha;
						}else{
							dias = java.time.temporal.ChronoUnit.DAYS.between(lastFecha[0], fecha)/2;//12hs de luz por dia	
						}
					}
					//acumulo el ndvi	
					ndviAcumProp.set(ndviAcumProp.get()+lNdvi.getMeanNDVI().doubleValue()*dias);
				}else {
					ndviAcumProp.set( lNdvi.getMeanNDVI().doubleValue());
				}	
				xAxis.setLowerBound(Math.min(xAxis.getLowerBound(),lNdvi.getFecha().toEpochDay()-5));
				xAxis.setUpperBound(Math.max(xAxis.getUpperBound(),lNdvi.getFecha().toEpochDay()+5));
				BigDecimal bd = new BigDecimal(ndviAcumProp.get()).setScale(2, RoundingMode.HALF_EVEN);
				
				sr.getData().add(new XYChart.Data<Number, Number>(lNdvi.getFecha().toEpochDay(), bd.doubleValue()));
				}catch(Exception e) {
					System.err.println("Excepcion para "+lNdvi.getNombre());
					e.printStackTrace();
				}
			});
			data.add(sr);	
		});

		// Secondary axis: daylight hours per date (scaled to 0–24 for right axis)
		Set<Long> uniqueEpochDays = new TreeSet<>();
		for (XYChart.Series<Number, Number> s : data) {
			for (XYChart.Data<Number, Number> d : s.getData()) {
				uniqueEpochDays.add(d.getXValue().longValue());
			}
		}
		double latitude = latitudeForDaylight[0] != null ? latitudeForDaylight[0] : 0.0;
		DaylightCalculator daylightCalculator = new DaylightCalculator(latitude);
		double maxPrimaryY = data.stream()
				.flatMap(s -> s.getData().stream())
				.mapToDouble(d -> d.getYValue().doubleValue())
				.max().orElse(1.0);
		if (maxPrimaryY <= 0) maxPrimaryY = 1.0;
		XYChart.Series<Number, Number> daylightSeries = new XYChart.Series<>();
		String radiationSeriesName = Messages.getString("NDVIHistoChart.RadiationMj");
		daylightSeries.setName(radiationSeriesName);
		double maxRadiationMj = 50.0; // typical max for scaling right axis
		for (Long epochDay : uniqueEpochDays) {
			LocalDate date = LocalDate.ofEpochDay(epochDay);
			double radiationMj = daylightCalculator.getTotalSolarRadiationMjPerM2(date);
			// Scale to primary Y range so right axis aligns
			daylightSeries.getData().add(new XYChart.Data<Number, Number>(epochDay, (radiationMj / maxRadiationMj) * maxPrimaryY));
		}
		data.add(daylightSeries);

		xAxis.setTickLabelRotation(90);
		xAxis.setTickUnit(5);
		
		lineChart = new LineChart<Number, Number>(xAxis, yAxis,data);				
		lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);

		NumberAxis rightAxis = new NumberAxis(0, maxRadiationMj, 10);
		rightAxis.setSide(Side.RIGHT);
		rightAxis.setLabel(radiationSeriesName);
		rightAxis.setAutoRanging(false);
		rightAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number value) {
				return value.intValue() + "";
			}
			@Override
			public Number fromString(String string) {
				try {
					return Integer.parseInt(string.trim());
				} catch (Exception e) {
					return 0;
				}
			}
		});

		// Wrap right axis so it aligns with the chart plot area (not full height including legend)
		VBox rightAxisWrapper = new VBox(rightAxis);
		rightAxisWrapper.setFillWidth(true);
		rightAxisWrapper.setAlignment(Pos.CENTER_RIGHT);
		Runnable alignAxisToPlot = () -> {
			Node plot = lineChart.lookup(".chart-plot-background");
			if (plot != null) {
				Bounds bInChart = lineChart.sceneToLocal(plot.localToScene(plot.getBoundsInLocal()));
				double topInset = bInChart.getMinY();
				double bottomInset = lineChart.getHeight() - bInChart.getMaxY();
				rightAxisWrapper.setPadding(new Insets(Math.max(0, topInset), 0, Math.max(0, bottomInset), 0));
				rightAxis.setPrefHeight(Math.max(1, bInChart.getHeight()));
			}
		};
		lineChart.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(alignAxisToPlot));
		lineChart.boundsInLocalProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(alignAxisToPlot));
		Platform.runLater(alignAxisToPlot);

		BorderPane chartPane = new BorderPane();
		chartPane.setCenter(lineChart);
		chartPane.setRight(rightAxisWrapper);

		VBox vbox = new VBox(chartPane);
		VBox.setVgrow(chartPane, Priority.ALWAYS);
		VBox.setVgrow(vbox, Priority.ALWAYS);
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcel();});
		right.getChildren().add(exportButton);
		BorderPane bottom = new BorderPane();
		//bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(5,5,5,5));
		vbox.getChildren().add(bottom);
		//this.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
		//String style= this.getStyle();
		
		// this.getStylesheets().add("""
		// 					.onHover{
		// 			-fx-background-color: BLACK;
		// 			-fx-font-size: 20;
		// 		}
		// 	""");
		this.getChildren().add(vbox);
		VBox.setVgrow(vbox, Priority.ALWAYS);
		
		/**
         * Browsing through the Data and applying ToolTip
         * as well as the class on hover
         */
        for (XYChart.Series<Number, Number> s : lineChart.getData()) {
            for (XYChart.Data<Number, Number> d : s.getData()) {
                String tooltipText = s.getName()
                        + "\n" + (s.getName().equals(radiationSeriesName) ? String.format("%.1f", d.getYValue().doubleValue() * maxRadiationMj / maxPrimaryY) 
						: Messages.getString("NDVIHistoChart.NDVI") + d.getYValue())
                        + "\n" + Messages.getString("JFXMain.show_ndvi_chart.Fecha") + ": " + toString(d.getXValue());
                Tooltip.install(d.getNode(), new Tooltip(tooltipText));

                //Adding class on hover
                d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));

                //Removing class on exit
                d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
            }
        }
		

	
		System.out.println("Mostre grafico");


	}
	
	
	private String toString(Number epochDay) {
		try {
			return LocalDate.ofEpochDay(epochDay.longValue()).toString();
		}catch(Exception e) {
			return "";
		}				
	}

	private void doExportarExcel() {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeriesList(this.lineChart.getData());
	}
	
	public List<SurfaceImageLayer> extractLayers() {
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				//l.setEnabled(false);
				ndviLayers.add((SurfaceImageLayer) l);
			}
		}	

		//System.out.println("mostrando la evolucion de "+ndviLayers.size()+" layers");
		ndviLayers.sort(new NdviLayerComparator());
		return ndviLayers;
	}

	public class NdviLayerComparator implements Comparator<Layer>{
		DateTimeFormatter df =null;// DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		public NdviLayerComparator() {
			df = DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		}

		@Override
		public int compare(Layer c1, Layer c2) {			
			String l1Name =c1.getName();
			String l2Name =c2.getName();

			Object labor1 = c1.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Object labor2 = c2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);


			if(labor1 != null && labor1 instanceof Ndvi && 
					labor2 != null && labor2 instanceof Ndvi ){
				Ndvi ndvi1 = (Ndvi)labor1;
				Ndvi ndvi2 = (Ndvi)labor2;

				try{
					return ndvi1.getFecha().compareTo(ndvi2.getFecha());
				} catch(Exception e){
					//System.err.println("no se pudo comparar las fechas de los ndvi. comparando nombres"); //$NON-NLS-1$
				}
				// comparar por el valor del layer en vez del nombre del layer
				try{
					LocalDate d1 = LocalDate.parse(l1Name.substring(l1Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					LocalDate d2 = LocalDate.parse(l2Name.substring(l2Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					return d1.compareTo(d2);
				} catch(Exception e){
					//no se pudo parsear como fecha entonces lo interpreto como string.
					e.printStackTrace();
				}
			}
			return l1Name.compareToIgnoreCase(l2Name);
		}
	}
}
