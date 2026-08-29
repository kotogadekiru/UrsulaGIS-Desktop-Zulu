package com.ursulagis.desktop.gui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.margen.Margen;
import gov.nasa.worldwind.layers.Layer;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.MargenConfigDialogController;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.tasks.procesar.SumarMargenesMapTask;

import java.util.logging.Logger;
public class MargenGUIController extends AbstractGUIController {
	private static final Logger logger = Logger.getLogger(MargenGUIController.class.getName());



	public MargenGUIController(JFXMain _main) {
		super(_main);
	}

	public void addMargenRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction(
				Messages.getString("JFXMain.sumar"), 
				(l)->doSumarMargenes(l),
				2));//min 2 layers activos para que se muestre
		

//
//		rootNodeP.add(new LayerAction(
//				Messages.getString("JFXMain.unirCosechas"),
//				(layer)->{
//					this.doUnirCosechas(null);
//					return "joined";	
//				},
//				2));
//		rootNodeP.add(new LayerAction(
//				Messages.getString("JFXMain.sumarCosechas"),
//				(layer)->{
//					this.doSumarCosechas();
//					return "joined";	
//				},
//				2));

		getLayerPanel().addAccionesClase(rootNodeP,Margen.class);
	}

	public void addAccionesMargen(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> margenesP = new ArrayList<LayerAction>();
		predicates.put(Margen.class, margenesP);
		/**
		 *Accion que permite editar un mapa de rentabilidad
		 */
		margenesP.add(LayerAction.constructPredicate(
				Messages.getString("JFXMain.editMargenAction"),
				(layer)->{	
					doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "margen editado" + layer.getName(); 
				}
				));
		
		/**
		 *Accion que permite resumir por categoria un mapa de rentabilidad
		 */
		//se reemplaza por la accion generica de resumirLabor
//		margenesP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
//			doResumirMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "margen resumido" + layer.getName(); 
//		}));
	}
	
	private void doEditMargin(Margen margen) {		
		logger.fine("editingMargins"); 
		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			logger.fine("el dialogo termino con cancel asi que no continuo con el calculo de los margenes"); 
			return;
		}							
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			this.main.wwjPanel.repaint();
			logger.fine("EditMarginTask succeeded"); 
			OnboardingAchievements.getInstance().unlock(JFXMain.stage, OnboardingAchievements.FIRST_MARGEN_EDITED);
			playSound();
		});
		executorPool.execute(uMmTask);
	}

	/** Shows a persisted margen using the same {@link OpenMargenMapTask} path as edit/import. */
	public void showMargenLabor(Margen labor) {
		if (labor == null) {
			return;
		}
		if (!main.genericGUIController.prepareLaborForShow(labor)) {
			return;
		}
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(labor);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			insertBeforeCompass(getWwd(), labor.getLayer());
			this.getLayerPanel().update(this.getWwd());
			viewGoTo(labor);
			uMmTask.uninstallProgressBar();
			playSound();
		});
		executorPool.execute(uMmTask);
	}
	
	private String doSumarMargenes(Layer l) {
		List<Margen> margenes = main.getMargenesSeleccionados();
		logger.fine("editingMargins"); 
							
		SumarMargenesMapTask uMmTask = new SumarMargenesMapTask(margenes);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			Margen ret = (Margen)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			//this.wwjPanel.repaint();

			OnboardingAchievements.getInstance().unlock(JFXMain.stage, OnboardingAchievements.FIRST_MARGEN_SUMMED);
			playSound();
		});
		executorPool.execute(uMmTask);
		return "sume Margenes";
	}



	


}
