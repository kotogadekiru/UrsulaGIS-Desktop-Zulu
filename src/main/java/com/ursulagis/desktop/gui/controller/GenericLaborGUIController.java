package com.ursulagis.desktop.gui.controller;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.geotools.api.data.FileDataStore;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

//import gov.nasa.worldwind.util.measure.MeasureTool;
import com.ursulagis.desktop.gui.CosechaHistoChart;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.PoligonLayerFactory;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.gui.utils.DoubleTableColumn;
import com.ursulagis.desktop.gui.utils.NombreTableColumn;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import com.ursulagis.desktop.gui.nww.MeasureTool;
import com.ursulagis.desktop.gui.nww.MeasureToolForShape;
import com.ursulagis.desktop.tasks.ExportLaborMapTask;
import com.ursulagis.desktop.tasks.GenerarReportePDFTask;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.tasks.procesar.ClonarLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.JuntarShapefilesTask;
import com.ursulagis.desktop.tasks.procesar.OutliersLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.ResumirLaborMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.PDFHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;



public class GenericLaborGUIController extends AbstractGUIController {


	public GenericLaborGUIController(JFXMain _main) {
		super(_main);
	}

	public void addAccionesLabor(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> laboresP = new ArrayList<LayerAction>();
		predicates.put(Labor.class, laboresP);
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.goToLayerAction"),(layer)->{	
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
				viewGoTo((Labor<?>) layerObject);
			}
			return "went to " + layer.getName(); 
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.GuardarLabor"),(layer)->{
			main.enDesarrollo();
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doGuardarLabor((Labor<?>) layerObject);
			}
			return "guarde labor " + layer.getName();
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.ClonarLabor"),(layer)->{

			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doClonarLabor((Labor<?>) layerObject);
			}
			return "clone labor " + layer.getName();
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.ResumirLabor"),(layer)->{

			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doResumirLabor((Labor<LaborItem>) layerObject);
			}
			return "resumi labor " + layer.getName();
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.OutliersLabor"),(layer)->{

			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doOutliersLabor((Labor<LaborItem>) layerObject);
			}
			return "filtre outliers labor " + layer.getName();
		}));




		/**
		 * Accion que muesta el histograma
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showHistogramaLaborAction"),(layer)->{//	this::applyHistogramaCosecha);//(layer)->applyHistogramaCosecha(layer));
			showHistoLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "histograma mostrado" + layer.getName(); 
		}));

		/**
		 * Accion que genera un reporte PDF con la vista del mapa centrada en la labor y el histograma.
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.reportePDFAction"), (layer) -> {
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject != null && Labor.class.isAssignableFrom(layerObject.getClass())) {
				doGenerarReportePDF((Labor<?>) layerObject);
			}
			return "reporte PDF generado " + layer.getName();
		}));

		/**
		 * Accion que permite extraer los poligonos de una cosecha para guardar
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.extraerPoligonoAction"),(layer)->{
			main.poligonoGUIController.doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
		}));


		/**
		 * Accion que permite extraer el contorno de una cosecha
		 * es solo de prueba. se puede realizar extrayendo poligonos y uniendolos
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("PoligonGUIController.extraerContornoAction"),(layer)->{
			main.poligonoGUIController.doExtraerContorno((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
		}));

		/**
		 * Accion que permite cortar una labor por el poligono/s seleccionado
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.cortarCosechaAction"),(layer)->{			
			main.poligonoGUIController.doCortarLaborPorPoligono((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor cortada" + layer.getName(); 

		}));

		/**
		 * Accion permite exportar la labor como shp
		 */
		laboresP.add(new LayerAction((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportLaborAction");  
			} else{
				doExportLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName(); 
			}},Messages.getString("JFXMain.exportLaborAction")));

		/**
		 * Accion muestra una tabla con los datos de la cosecha
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showTableLayerAction"),(layer)->{
			doShowDataTable((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Tabla mostrada" + layer.getName(); 
		}));

		/**
		 * Accion permite obtener ndvi
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.downloadNDVI"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
			if(o instanceof Labor){
				main.ndviGUIController.doGetNdviTiffFile(o);
			}
			return "ndvi obtenido" + layer.getName();	 
		}));
	}

	public void addAccionesGenericas(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> todosP = new ArrayList<LayerAction>();
		predicates.put(Object.class, todosP);
		/**
		 * Accion que permite quitar un item del arbol
		 */
		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.removeLayerAction"),l->doRemoveLayer(l)));

		//editar opacidad
		//JFXMain.layerTransparencia=Transparencia
		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.layerTransparencia"),(layer)->{
			showTransparenciaSlider(layer);
			return "layer transparente" + layer.getName()+" "+layer.getOpacity(); 
		}));
	}

	public void showTransparenciaSlider(Layer layer) {
		double op = layer.getOpacity();
		//double newOp = op*0.5;

		Slider slider = new Slider(0, 1, op);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);
		slider.setMajorTickUnit(0.25f);
		slider.setBlockIncrement(0.1f);
		Scene sc = new Scene(slider,600,50);
		//TODO fixme no se ve un layer a travez del otro
		slider.valueProperty().addListener((obs,n,o)->{

			layer.setOpacity(n.doubleValue());//newOp>0.1?newOp:1);
			this.getWwd().redraw();
			System.out.println("layer transparente" + layer.getName()+" "+layer.getOpacity());
		});
		Stage stage = new Stage();
		stage.setScene(sc);
		stage.initOwner(JFXMain.stage);
		stage.getIcons().addAll(JFXMain.stage.getIcons());
		stage.setTitle(Messages.getString("JFXMain.layerTransparencia")+" "+layer.getName());
		stage.show();
	}	

	private String doRemoveLayer(Layer layer) {
		
			getWwd().getModel().getLayers().remove(layer);
			Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
				Labor<?> l = (Labor<?>)layerObject;	
				l.dispose();
			}
			if(layerObject instanceof Poligono){
				Poligono poli = (Poligono) layerObject;
				poli.setActivo(false);
				if(poli.getId()!=null){
					DAH.save(poli);
				}
			}
			if(layerObject instanceof Ndvi){
				Ndvi ndvi = (Ndvi) layerObject;
				ndvi.setActivo(false);
				if(ndvi.getId()!=null){
					try {
						DAH.save(ndvi);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			Object mtObj = layer.getValue(PoligonLayerFactory.MEASURE_TOOL);		
			if(mtObj!=null && mtObj instanceof MeasureToolForShape) {
				MeasureToolForShape mt = (MeasureToolForShape)mtObj;
				mt.setCreationMode(false);
				mt.dispose();
			} else if(mtObj!=null && mtObj instanceof MeasureTool) {
				MeasureTool mt = (MeasureTool)mtObj;
				mt.setArmed(false);
				mt.dispose();
			}

			layer.dispose();
			getLayerPanel().update(getWwd());
			return "layer removido" + layer.getName(); 
	}

	private void doClonarLabor(Labor<?> labor) {
		ClonarLaborMapTask umTask = new ClonarLaborMapTask(labor);
		umTask.installProgressBar(progressBox);

		//	testLayer();
		umTask.setOnSucceeded(handler -> {
			labor.getLayer().setEnabled(false);
			Labor<?> ret = (Labor<?>)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);
	}

	private Map<String,Object> createMap(String nombre,Object value){		
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		map.put("KEY", nombre);
		map.put("VALUE", value);
		return map;
	}
	
	private void doOutliersLabor(Labor<LaborItem> labor) {
		Double anchoFiltroOuliers=50.0;
		Double minValue=Double.MIN_VALUE;
		Double maxValue=Double.MAX_VALUE;;
		Double toleranciaCoeficienteVariacion=0.0;

		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();	 
		data.add(createMap("Ancho Filtro", anchoFiltroOuliers));
		data.add(createMap("Min Value", minValue));
		data.add(createMap("Max Value", maxValue));
		data.add(createMap("Tolerancia", toleranciaCoeficienteVariacion));						

		TableView<Map<String,Object>> tabla = new TableView<Map<String,Object>>( 
				FXCollections.observableArrayList(data)
				);
		tabla.setEditable(true);
		NombreTableColumn nombreColl = new NombreTableColumn("KEY");
		nombreColl.setText("Parametro");
		tabla.getColumns().add(nombreColl);
		DoubleTableColumn<Map<String, Object>> anchoColl = DoubleTableColumn.createMapTableColumn("VALUE");
		anchoColl.setText("Valor");
		tabla.getColumns().add(anchoColl);
	

		Scene scene = new Scene(tabla, 800, 600);
		Stage tablaStage = new Stage();
		tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
		tablaStage.setTitle("Parametros");//Messages.getString("Recorrida.asignarValores")); 
		tablaStage.setScene(scene);

		tablaStage.showAndWait();

		OutliersLaborMapTask uMmTask = new OutliersLaborMapTask(
				labor,
				anchoFiltroOuliers,
				minValue,
				maxValue,
				toleranciaCoeficienteVariacion);

		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {

			Labor<?> ret = (Labor<?>)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();			

			this.getLayerPanel().update(this.getWwd());
			playSound();
			viewGoTo(ret);
			System.out.println("hice outliers en la labor"); 
		});
		executorPool.execute(uMmTask);
	}
	

	private void doResumirLabor(Labor<LaborItem> labor) {		
		ResumirLaborMapTask uMmTask = new ResumirLaborMapTask(labor);

		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			labor.getLayer().setEnabled(false);
			Labor<?> ret = (Labor<?>)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();			
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			playSound();
			viewGoTo(ret);
			System.out.println("ProcessMarginTask succeeded"); 
		});
		executorPool.execute(uMmTask);
	}

	private void doGuardarLabor(Labor<?> labor) {
		File zipFile = FileHelper.zipLaborToTmpDir(labor);//ok funciona
		byte[] byteArray = FileHelper.fileToByteArray(zipFile);		
		labor.setContent(byteArray);
		DAH.save(labor);//No se guardan las labores porque no extienden de entidad
	}

	public void doJuntarShapefiles() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		File shapeFile = FileHelper.getNewShapeFile("union");

		JuntarShapefilesTask task = new JuntarShapefilesTask(stores,shapeFile);
		task.installProgressBar(progressBox);

		task.setOnSucceeded(handler -> {
			playSound();
			task.uninstallProgressBar();
		});
		executorPool.execute(task);
	}

	private void showHistoLabor(Labor<?> cosechaLabor) {	
		Platform.runLater(()->{
			CosechaHistoChart histoChart = new CosechaHistoChart(cosechaLabor);
			Stage histoStage = new Stage();
			histoStage.setTitle(Messages.getString("CosechaHistoChart.Title"));
			histoStage.getIcons().addAll(JFXMain.stage.getIcons());
			histoStage.initOwner(JFXMain.stage);
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
	}

	private void doExportLabor(Labor<?> laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  FileHelper.getNewShapeFile(nombre);

		ExportLaborMapTask ehTask = new ExportLaborMapTask(laborToExport,shapeFile);
		ehTask.installProgressBar(progressBox);

		ehTask.setOnSucceeded(handler -> {
			playSound();
			ehTask.uninstallProgressBar();
		});
		executorPool.execute(ehTask);
	}

	/**
	 * metodo que toma una labor y muestra una tabla con los campos de la labor
	 * @param labor
	 */
	private void doShowDataTable(Labor<?> labor) {		   
		SmartTableView.showLaborTable(labor);
	}

	/**
	 * Centra la vista en la labor, captura el mapa y el histograma, y genera un PDF.
	 */
	private void doGenerarReportePDF(Labor<?> labor) {

		LayerList layers = this.getWwd().getModel().getLayers();
		layers.stream().filter(l->{
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			return l.isEnabled() && o!=null;
		}).forEach(l->l.setEnabled(false));


		// Desactivar todas las capas de labores y NDVI (por valor en LABOR_LAYER_IDENTIFICATOR) para que no se superpongan
		//Layer targetLayer = labor.getLayer();
		labor.getLayer().setEnabled(true);
		// Activar solo la labor del reporte y ajustar la vista para que quepa en pantalla
		//targetLayer.setEnabled(true);
		main.viewGoToFit(labor);
		getWwd().redraw();
		main.wwjPanel.repaint();

		Platform.runLater(() -> {
			// Dar tiempo al redibujado del mapa antes de capturar
			try{Thread.sleep(2000);}catch(InterruptedException e){e.printStackTrace();}
			Platform.runLater(() -> {
				SnapshotParameters params = new SnapshotParameters();
				params.setFill(Color.TRANSPARENT);
				javafx.scene.Node mapNode = main.getMapSnapshotNode();
				if (mapNode == null) {
					mapNode = main.getSplitPane();
				}
				WritableImage mapWritable = mapNode.snapshot(params, null);
				if (mapWritable == null) {
					return;
				}
				BufferedImage mapBuf = SwingFXUtils.fromFXImage(mapWritable, null);

				CosechaHistoChart histoChart = new CosechaHistoChart(labor);
				new Scene(histoChart, 800, 450); // scene needed for proper layout
				histoChart.applyCss();
				histoChart.layout();
				WritableImage histoWritable = histoChart.snapshot(params, null);
				BufferedImage histoBuf = histoWritable != null ? SwingFXUtils.fromFXImage(histoWritable, null) : null;
				java.util.List<Object[]> histogramTableData = histoChart.getHistogramTableData();

				// Generar en carpeta temporal; el usuario puede guardarlo desde el visor si le sirve
				String safeName = labor.getNombre().replaceAll("[^a-zA-Z0-9._-]", "_");
				if (safeName.length() > 50) {
					safeName = safeName.substring(0, 50);
				}
				File tmpDir = new File(System.getProperty("java.io.tmpdir"));
				File outputFile = new File(tmpDir, "reporte_" + safeName + "_" + System.currentTimeMillis() + ".pdf");
				File finalOutput = outputFile;
				BufferedImage finalMapBuf = mapBuf;
				BufferedImage finalHistoBuf = histoBuf;
				String laborName = labor.getNombre();

				GenerarReportePDFTask pdfTask = new GenerarReportePDFTask(
						finalOutput, finalMapBuf, finalHistoBuf, laborName, histogramTableData);
				pdfTask.installProgressBar(progressBox);
				pdfTask.setOnSucceeded(handler -> {
					pdfTask.uninstallProgressBar();
					playSound();
					File result = pdfTask.getValue();
					if (result != null && result.exists()) {
						try {
							Desktop.getDesktop().open(result);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				pdfTask.setOnFailed(handler -> {
					pdfTask.uninstallProgressBar();
					Throwable e = pdfTask.getException();
					if (e != null) {
						e.printStackTrace();
					}
					javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
					alert.setHeaderText(Messages.getString("GenericLaborGUIController.reportePDFError"));
					alert.setContentText(e != null ? e.getMessage() : "");
					alert.initOwner(JFXMain.stage);
					alert.show();
				});
				executorPool.execute(pdfTask);
				layers.forEach(l->l.setEnabled(true));
			});
		});
	}

}
