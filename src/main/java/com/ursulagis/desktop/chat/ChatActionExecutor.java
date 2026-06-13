package com.ursulagis.desktop.chat;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;
import com.ursulagis.desktop.tasks.ExportLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.ClonarLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.ResumirLaborMapTask;
import com.ursulagis.desktop.utils.FileHelper;

/**
 * Resolves entities and delegates parsed intents to existing Ursula controllers and tasks.
 */
public class ChatActionExecutor {

	private final JFXMain main;

	public ChatActionExecutor(JFXMain main) {
		this.main = main;
	}

	public String execute(ParsedIntent intent, MapLayerContext layerContext) {
		ActionContext ctx = new ActionContext(main, intent.getTargetName(), layerContext);
		resolveTargets(ctx, intent.getAction());

		return switch (intent.getAction()) {
			case HELP -> helpText(ctx.getLayerContext());
			case LIST_LAYERS -> ctx.getLayerContext().formatLayerList();
			case IMPORT_COSECHA -> {
				main.cosechaGUIController.doOpenCosecha(null);
				yield "Diálogo de importación de cosecha abierto.";
			}
			case IMPORT_COSECHA_VOYAGER -> {
				main.cosechaGUIController.doOpenCosechaVoyager();
				yield "Importación Voyager iniciada.";
			}
			case IMPORT_RECORRIDA -> {
				main.recorridaGUIController.doOpenRecorridaMap(null);
				yield "Diálogo de importación de recorrida abierto.";
			}
			case IMPORT_NDVI -> {
				main.ndviGUIController.doOpenNDVITiffFiles();
				yield "Diálogo de importación NDVI abierto.";
			}
			case IMPORT_SUELO -> {
				main.sueloGUIController.doOpenSoilMap(null);
				yield "Diálogo de importación de suelo abierto.";
			}
			case BULK_NDVI_DOWNLOAD -> {
				main.ndviGUIController.doBulkNDVIDownload();
				yield "Descarga masiva de NDVI iniciada.";
			}
			case BALANCE_NUTRIENTES -> {
				main.sueloGUIController.doProcesarBalanceNutrientes();
				yield "Balance de nutrientes en proceso.";
			}
			case JUNTAR_SHAPES -> {
				main.genericGUIController.doJuntarShapefiles();
				yield "Unión de shapefiles iniciada.";
			}
			case MEDIR_DISTANCIA -> {
				main.poligonoGUIController.doMedirDistancia();
				yield "Herramienta de medición activada.";
			}
			case CREAR_POLIGONO -> {
				main.poligonoGUIController.doCrearPoligono();
				yield "Herramienta de polígono activada.";
			}
			case SHOW_LABORES_TABLE -> {
				main.configGUIController.doShowLaboresTable();
				yield "Tabla de labores abierta.";
			}
			case GO_TO_LAYER -> goToLabor(ctx);
			case RESUMIR_LABOR -> resumirLabor(ctx);
			case EXPORT_LABOR -> exportLabor(ctx);
			case CLONAR_LABOR -> clonarLabor(ctx);
			case DOWNLOAD_NDVI -> downloadNdvi(ctx);
			case COMPARTIR_COSECHA -> compartirCosecha(ctx);
			case UPDATE_RECORRIDA -> updateRecorrida(ctx);
			case EXPORT_RECORRIDA -> exportRecorrida(ctx);
			case UNKNOWN -> intent.getMessage();
		};
	}

	private void resolveTargets(ActionContext ctx, UrsulaAction action) {
		if (action.requiresLabor() || action.requiresCosecha()) {
			resolveLabor(ctx, action.requiresCosecha());
		}
		if (action.requiresRecorrida()) {
			resolveRecorrida(ctx);
		}
	}

	private void resolveLabor(ActionContext ctx, boolean cosechaOnly) {
		findLoadedLabor(ctx, cosechaOnly).ifPresent(l -> {
			ctx.setLabor(l);
			if (l instanceof CosechaLabor c) {
				ctx.setCosecha(c);
			}
		});
	}

	private void resolveRecorrida(ActionContext ctx) {
		findLoadedRecorrida(ctx).ifPresent(ctx::setRecorrida);
	}

	private Optional<Labor<?>> findLoadedLabor(ActionContext ctx, boolean cosechaOnly) {
		MapLayerContext mapCtx = ctx.getLayerContext();
		String targetName = ctx.getTargetName();

		if (targetName != null && !targetName.isBlank()) {
			Optional<LoadedLayerInfo> byName = mapCtx.findByName(targetName, true);
			if (byName.isPresent()) {
				return toLabor(byName.get(), cosechaOnly);
			}
		}

		Optional<LoadedLayerInfo> selected = mapCtx.getSelectedLayer();
		if (selected.isPresent()) {
			Optional<Labor<?>> fromSelected = toLabor(selected.get(), cosechaOnly);
			if (fromSelected.isPresent()) {
				return fromSelected;
			}
		}

		Optional<LoadedLayerInfo> singleActive = mapCtx.getSingleActiveLabor(cosechaOnly);
		if (singleActive.isPresent()) {
			return toLabor(singleActive.get(), cosechaOnly);
		}

		List<LoadedLayerInfo> loaded = mapCtx.getLabors(cosechaOnly);
		if (loaded.size() == 1) {
			return toLabor(loaded.get(0), cosechaOnly);
		}
		return Optional.empty();
	}

	private Optional<Recorrida> findLoadedRecorrida(ActionContext ctx) {
		MapLayerContext mapCtx = ctx.getLayerContext();
		String targetName = ctx.getTargetName();

		if (targetName != null && !targetName.isBlank()) {
			Optional<LoadedLayerInfo> byName = mapCtx.findByName(targetName, true);
			if (byName.isPresent() && byName.get().getEntity() instanceof Recorrida r) {
				return Optional.of(r);
			}
		}

		Optional<LoadedLayerInfo> selected = mapCtx.getSelectedLayer();
		if (selected.isPresent() && selected.get().getEntity() instanceof Recorrida r) {
			return Optional.of(r);
		}

		Optional<LoadedLayerInfo> singleActive = mapCtx.getSingleActiveRecorrida();
		if (singleActive.isPresent() && singleActive.get().getEntity() instanceof Recorrida r) {
			return Optional.of(r);
		}

		List<LoadedLayerInfo> loaded = mapCtx.getRecorridas();
		if (loaded.size() == 1 && loaded.get(0).getEntity() instanceof Recorrida r) {
			return Optional.of(r);
		}
		return Optional.empty();
	}

	private static Optional<Labor<?>> toLabor(LoadedLayerInfo info, boolean cosechaOnly) {
		Object entity = info.getEntity();
		if (!(entity instanceof Labor<?> labor)) {
			return Optional.empty();
		}
		if (cosechaOnly && !(entity instanceof CosechaLabor)) {
			return Optional.empty();
		}
		return Optional.of(labor);
	}

	private static String ambiguousLaborMessage(ActionContext ctx, boolean cosechaOnly) {
		List<LoadedLayerInfo> loaded = ctx.getLayerContext().getLabors(cosechaOnly);
		if (loaded.isEmpty()) {
			return "No hay capas de ese tipo cargadas en el mapa.";
		}
		String options = loaded.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
		return "Hay varias capas cargadas. Especifica el nombre o activa solo una: " + options;
	}

	private String goToLabor(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ambiguousLaborMessage(ctx, false);
		}
		main.viewGoTo(ctx.getLabor());
		return "Vista centrada en " + nameOf(ctx.getLabor()) + ".";
	}

	@SuppressWarnings("unchecked")
	private String resumirLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ambiguousLaborMessage(ctx, false);
		}
		if (!isLayerActive(ctx, labor)) {
			return "La capa \"" + nameOf(labor) + "\" está cargada pero inactiva. Actívala en el árbol de capas o especifica otra.";
		}
		ResumirLaborMapTask task = new ResumirLaborMapTask((Labor<LaborItem>) labor);
		task.installProgressBar(JFXMain.progressBox);
		task.setOnSucceeded(handler -> {
			labor.getLayer().setEnabled(false);
			Labor<?> ret = (Labor<?>) handler.getSource().getValue();
			task.uninstallProgressBar();
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());
			OnboardingAchievements.getInstance().unlock(JFXMain.stage, OnboardingAchievements.FIRST_GENERIC_LABOR_SUMMARIZED);
			main.playSound();
			main.viewGoTo(ret);
		});
		JFXMain.executorPool.execute(task);
		return "Resumiendo labor " + nameOf(labor) + "...";
	}

	private String exportLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ambiguousLaborMessage(ctx, false);
		}
		File shapeFile = FileHelper.getNewShapeFile(labor.getNombre());
		ExportLaborMapTask task = new ExportLaborMapTask(labor, shapeFile);
		task.installProgressBar(JFXMain.progressBox);
		task.setOnSucceeded(handler -> {
			OnboardingAchievements.getInstance().unlock(JFXMain.stage, OnboardingAchievements.FIRST_GENERIC_LABOR_EXPORTED);
			main.playSound();
			task.uninstallProgressBar();
		});
		JFXMain.executorPool.execute(task);
		return "Exportando " + nameOf(labor) + " a shapefile...";
	}

	private String clonarLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ambiguousLaborMessage(ctx, false);
		}
		ClonarLaborMapTask task = new ClonarLaborMapTask(labor);
		task.installProgressBar(JFXMain.progressBox);
		task.setOnSucceeded(handler -> {
			labor.getLayer().setEnabled(false);
			Labor<?> ret = (Labor<?>) handler.getSource().getValue();
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());
			task.uninstallProgressBar();
			main.viewGoTo(ret);
			OnboardingAchievements.getInstance().unlock(JFXMain.stage, OnboardingAchievements.FIRST_GENERIC_LABOR_CLONED);
			main.playSound();
		});
		JFXMain.executorPool.execute(task);
		return "Clonando labor " + nameOf(labor) + "...";
	}

	private String downloadNdvi(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ambiguousLaborMessage(ctx, false);
		}
		main.ndviGUIController.doGetNdviTiffFile(ctx.getLabor());
		return "Descarga de NDVI iniciada para " + nameOf(ctx.getLabor()) + ".";
	}

	private String compartirCosecha(ActionContext ctx) {
		if (ctx.getCosecha() == null) {
			return ambiguousLaborMessage(ctx, true);
		}
		main.cosechaGUIController.doCompartirCosecha(ctx.getCosecha());
		return "Compartiendo cosecha " + nameOf(ctx.getCosecha()) + "...";
	}

	private String updateRecorrida(ActionContext ctx) {
		if (ctx.getRecorrida() == null) {
			List<LoadedLayerInfo> recorridas = ctx.getLayerContext().getRecorridas();
			if (recorridas.isEmpty()) {
				return "No hay recorridas cargadas en el mapa. Importa una primero.";
			}
			String options = recorridas.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
			return "Hay varias recorridas cargadas. Especifica el nombre o activa solo una: " + options;
		}
		main.recorridaGUIController.doUpdateRecorrida(ctx.getRecorrida());
		return "Sincronizando recorrida " + ctx.getRecorrida().getNombre() + "...";
	}

	private String exportRecorrida(ActionContext ctx) {
		if (ctx.getRecorrida() == null) {
			List<LoadedLayerInfo> recorridas = ctx.getLayerContext().getRecorridas();
			if (recorridas.isEmpty()) {
				return "No hay recorridas cargadas en el mapa.";
			}
			String options = recorridas.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
			return "Especifica qué recorrida exportar: " + options;
		}
		main.recorridaGUIController.doExportRecorrida(ctx.getRecorrida());
		return "Exportando recorrida " + ctx.getRecorrida().getNombre() + "...";
	}

	private static boolean isLayerActive(ActionContext ctx, Labor<?> labor) {
		return ctx.getLayerContext().getLayers().stream()
				.filter(info -> info.getEntity() == labor)
				.anyMatch(LoadedLayerInfo::isActive);
	}

	private static String nameOf(Labor<?> labor) {
		return labor.getNombre() != null ? labor.getNombre() : "sin nombre";
	}

	private static String helpText(MapLayerContext layerContext) {
		return UrsulaPersonality.helpIntro() + "\n"
				+ "• importar cosecha / importar recorrida / importar NDVI\n"
				+ "• resumir capa activa / exportar capa [nombre]\n"
				+ "• ir a capa [nombre] / clonar capa [nombre]\n"
				+ "• sincronizar recorrida activa / capas cargadas\n"
				+ "• balance de nutrientes / descargar NDVI masivo\n"
				+ "• medir distancia / crear polígono\n\n"
				+ layerContext.formatLayerList();
	}
}
