package com.ursulagis.desktop.chat;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.controller.ConfigGUI;
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

	public ActionExecutionResult execute(ParsedIntent intent, MapLayerContext layerContext) {
		ActionContext ctx = new ActionContext(main, intent.getTargetName(), layerContext);
		resolveTargets(ctx, intent.getAction());

		return switch (intent.getAction()) {
			case HELP -> ActionExecutionResult.notLaunched(helpText(ctx.getLayerContext()));
			case LIST_LAYERS -> ActionExecutionResult.notLaunched(ctx.getLayerContext().formatLayerList());
			case IMPORT_COSECHA -> {
				main.cosechaGUIController.doOpenCosecha(null);
				yield ActionExecutionResult.launched("Diálogo de importación de cosecha abierto.");
			}
			case IMPORT_SIEMBRA -> importSiembra(ctx, false);
			case IMPORT_COSECHA_VOYAGER -> {
				main.cosechaGUIController.doOpenCosechaVoyager();
				yield ActionExecutionResult.launched("Importación Voyager iniciada.");
			}
			case IMPORT_RECORRIDA -> {
				main.recorridaGUIController.doOpenRecorridaMap(null);
				yield ActionExecutionResult.launched("Diálogo de importación de recorrida abierto.");
			}
			case IMPORT_NDVI -> {
				main.ndviGUIController.doOpenNDVITiffFiles();
				yield ActionExecutionResult.launched("Diálogo de importación NDVI abierto.");
			}
			case IMPORT_SUELO -> {
				main.sueloGUIController.doOpenSoilMap(null);
				yield ActionExecutionResult.launched("Diálogo de importación de suelo abierto.");
			}
			case IMPORT_MARGEN -> {
				main.configGUIController.doOpenMarginMap();
				yield ActionExecutionResult.launched("Diálogo de importación de margen abierto.");
			}
			case GENERAR_MARGEN -> {
				main.configGUIController.doProcessMargin();
				yield ActionExecutionResult.launched("Rentabilidades iniciado con las capas activas.");
			}
			case BULK_NDVI_DOWNLOAD -> {
				main.ndviGUIController.doBulkNDVIDownload();
				yield ActionExecutionResult.launched("Descarga masiva de NDVI iniciada.");
			}
			case DOWNLOAD_NDVI_ASIGNACIONES -> downloadNdviAsignaciones(intent);
			case BALANCE_NUTRIENTES -> {
				main.sueloGUIController.doProcesarBalanceNutrientes();
				yield ActionExecutionResult.launched("Balance de nutrientes en proceso.");
			}
			case JUNTAR_SHAPES -> {
				main.genericGUIController.doJuntarShapefiles();
				yield ActionExecutionResult.launched("Unión de shapefiles iniciada.");
			}
			case MEDIR_DISTANCIA -> {
				main.poligonoGUIController.doMedirDistancia();
				yield ActionExecutionResult.launched("Herramienta de medición activada.");
			}
			case CREAR_POLIGONO -> {
				main.poligonoGUIController.doCrearPoligono();
				yield ActionExecutionResult.launched("Herramienta de polígono activada.");
			}
			case CONVERTIR_POLIGONO_A_COSECHA -> {
				main.poligonoGUIController.doConvertirPoligonosACosecha();
				yield ActionExecutionResult.launched("Conversión de polígonos a cosecha iniciada.");
			}
			case CONVERTIR_POLIGONO_A_SIEMBRA -> {
				main.poligonoGUIController.doConvertirPoligonosASiembra();
				yield ActionExecutionResult.launched("Conversión de polígonos a siembra iniciada.");
			}
			case CONVERTIR_POLIGONO_A_FERTILIZACION -> {
				main.poligonoGUIController.doConvertirPoligonosAFertilizacion();
				yield ActionExecutionResult.launched("Conversión de polígonos a fertilización iniciada.");
			}
			case CONVERTIR_POLIGONO_A_PULVERIZACION -> {
				main.poligonoGUIController.doConvertirPoligonosAPulverizacion();
				yield ActionExecutionResult.launched("Conversión de polígonos a pulverización iniciada.");
			}
			case IMPORT_POLIGONO -> {
				main.poligonoGUIController.doImportarPoligonos(null);
				yield ActionExecutionResult.launched("Diálogo de importación de polígonos abierto.");
			}
			case ACTIVAR_POLIGONOS_SUPERFICIE -> {
				int count = main.poligonoGUIController.activarPoligonosConSuperficieMayorA(0);
				yield ActionExecutionResult.launched(count > 0
						? "Activé **" + count + "** polígono(s) con superficie mayor a 0 ha."
						: "No hay polígonos con superficie mayor a 0 ha cargados en el mapa.");
			}
			case SHOW_LABORES_TABLE -> {
				main.configGUIController.doShowLaboresTable();
				yield ActionExecutionResult.launched("Tabla de labores abierta.");
			}
			case COMPARE_ACTIVE_LAYERS -> {
				main.configGUIController.showMultiLayerHistoChart();
				yield ActionExecutionResult.launched("Abriendo comparación de capas activas (histograma multilayer).");
			}
			case CONFIG_ASIGNACION -> {
				ConfigGUI.doConfigAsignacion();
				yield ActionExecutionResult.launched("Ventana de Asignación abierta para asignar actividades a lotes.");
			}
			case GO_TO_LAYER -> goToLabor(ctx);
			case RESUMIR_LABOR -> resumirLabor(ctx);
			case EXPORT_LABOR -> exportLabor(ctx);
			case CLONAR_LABOR -> clonarLabor(ctx);
			case DOWNLOAD_NDVI -> downloadNdvi(ctx);
			case COMPARTIR_COSECHA -> compartirCosecha(ctx);
			case COMPARTIR_SIEMBRA -> compartirSiembra(ctx);
			case UPDATE_RECORRIDA -> updateRecorrida(ctx);
			case EXPORT_RECORRIDA -> exportRecorrida(ctx);
			case UNKNOWN -> ActionExecutionResult.notLaunched(intent.getMessage());
		};
	}

	private void resolveTargets(ActionContext ctx, UrsulaAction action) {
		if (action == UrsulaAction.COMPARTIR_SIEMBRA) {
			resolveSiembra(ctx);
		} else if (action.requiresLabor() || action.requiresCosecha()) {
			resolveLabor(ctx, action.requiresCosecha());
		}
		if (action.requiresRecorrida()) {
			resolveRecorrida(ctx);
		}
	}

	private void resolveSiembra(ActionContext ctx) {
		LaborTargetResolver.resolveActiveSiembra(ctx.getLayerContext())
				.or(() -> LaborTargetResolver.resolve(ctx.getLayerContext(), ctx.getTargetName(), false)
						.filter(SiembraLabor.class::isInstance)
						.map(SiembraLabor.class::cast))
				.ifPresent(ctx::setLabor);
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
		return LaborTargetResolver.resolve(ctx.getLayerContext(), ctx.getTargetName(), cosechaOnly);
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
		return LaborTargetResolver.ambiguousLaborMessage(ctx.getLayerContext(), ctx.getTargetName(), cosechaOnly);
	}

	private ActionExecutionResult goToLabor(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.viewGoTo(ctx.getLabor());
		return ActionExecutionResult.launched("Vista centrada en " + nameOf(ctx.getLabor()) + ".");
	}

	@SuppressWarnings("unchecked")
	private ActionExecutionResult resumirLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		if (!isLayerActive(ctx, labor)) {
			return ActionExecutionResult.notLaunched(
					"La capa \"" + nameOf(labor) + "\" está cargada pero inactiva. Actívala en el árbol de capas o especifica otra.");
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
		return ActionExecutionResult.launched("Resumiendo labor " + nameOf(labor) + "...");
	}

	private ActionExecutionResult exportLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
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
		return ActionExecutionResult.launched("Exportando " + nameOf(labor) + " a shapefile...");
	}

	private ActionExecutionResult clonarLabor(ActionContext ctx) {
		Labor<?> labor = ctx.getLabor();
		if (labor == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
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
		return ActionExecutionResult.launched("Clonando labor " + nameOf(labor) + "...");
	}

	private ActionExecutionResult downloadNdvi(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.ndviGUIController.doGetNdviTiffFile(ctx.getLabor());
		return ActionExecutionResult.launched("Descarga de NDVI iniciada para " + nameOf(ctx.getLabor()) + ".");
	}

	private ActionExecutionResult downloadNdviAsignaciones(ParsedIntent intent) {
		String sourceText = intent.getSourceUserText() != null ? intent.getSourceUserText() : intent.getMessage();
		AsignacionNdviRequest req = AsignacionNdviRequest.parse(
				sourceText,
				intent.getCampaniaName(),
				intent.getCultivoName(),
				intent.getBeginDate(),
				intent.getEndDate());

		if (req.campaniaName() == null || req.campaniaName().isBlank()) {
			ChatPendingFollowUp.rememberNdviAsignacionNeedsCampania(req, sourceText);
			return ActionExecutionResult.notLaunched(
					"No encontré una campaña en el proyecto. ¿Cuál campaña querés usar? (ej. 26/27)");
		}
		if (!req.hasPeriod()) {
			ChatPendingFollowUp.rememberNdviAsignacionNeedsCampania(req, sourceText);
			return ActionExecutionResult.notLaunched(
					"Indicá el período a descargar (ej. \"últimas imágenes\" o \"desde 2025-11-01 hasta 2026-03-31\").");
		}

		List<Poligono> contornos = req.findContornos();
		if (contornos.isEmpty()) {
			ChatPendingFollowUp.rememberNdviAsignacionNeedsCampania(req, sourceText);
			String cultivoPart = req.cultivoName() != null ? " y cultivo " + req.cultivoName() : "";
			return ActionExecutionResult.notLaunched(
					"No encontré contornos en Asignación para campaña " + req.campaniaName() + cultivoPart
							+ ". ¿Cuál campaña querés usar? (ej. 26/27)");
		}

		ChatPendingFollowUp.clear();

		List<Poligono> missingLayer = new ArrayList<>();
		for (Poligono p : contornos) {
			if (p.getLayer() == null) {
				missingLayer.add(p);
			}
		}
		if (!missingLayer.isEmpty()) {
			main.poligonoGUIController.showPoligonos(missingLayer);
		}

		LocalDate begin = req.begin();
		LocalDate end = req.end();
		for (Poligono contorno : contornos) {
			main.poligonoGUIController.downloadNdviForPoligono(contorno, begin, end, null);
		}
		OnboardingAchievements.getInstance().unlock(
				JFXMain.stage, OnboardingAchievements.FIRST_NDVI_ASIGNACIONES_DOWNLOADED);

		String cultivoPart = req.cultivoName() != null ? ", cultivo " + req.cultivoName() : "";
		return ActionExecutionResult.launched(
				"Descargando NDVI de " + contornos.size() + " contorno(s) de campaña "
						+ req.campaniaName() + cultivoPart
						+ " (" + begin + " → " + end + ").");
	}

	private ActionExecutionResult compartirCosecha(ActionContext ctx) {
		if (ctx.getCosecha() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, true));
		}
		main.cosechaGUIController.doCompartirCosecha(ctx.getCosecha());
		return ActionExecutionResult.launched("Compartiendo cosecha " + nameOf(ctx.getCosecha()) + "...");
	}

	private ActionExecutionResult importSiembra(ActionContext ctx, boolean shareAfterImport) {
		if (shareAfterImport) {
			main.siembraGUIController.doOpenSiembraMap(null, imported ->
					main.siembraGUIController.doCompartirSiembra(imported));
			return ActionExecutionResult.launched(
					"Seleccioná el SHP de siembra. Al terminar la importación la comparto automáticamente (QR).");
		}
		main.siembraGUIController.doOpenSiembraMap(null, null);
		return ActionExecutionResult.launched("Diálogo de importación de siembra abierto.");
	}

	private ActionExecutionResult compartirSiembra(ActionContext ctx) {
		if (!(ctx.getLabor() instanceof SiembraLabor siembra)) {
			return ActionExecutionResult.notLaunched(LaborTargetResolver.ambiguousSiembraMessage(ctx.getLayerContext()));
		}
		main.siembraGUIController.doCompartirSiembra(siembra);
		return ActionExecutionResult.launched("Compartiendo siembra **" + nameOf(siembra) + "**...");
	}

	public ActionExecutionResult importYCompartirSiembra(MapLayerContext layerContext) {
		Optional<SiembraLabor> active = LaborTargetResolver.resolveActiveSiembra(layerContext);
		if (active.isPresent()) {
			ActionContext ctx = new ActionContext(main, null, layerContext);
			ctx.setLabor(active.get());
			return compartirSiembra(ctx);
		}
		ActionContext ctx = new ActionContext(main, null, layerContext);
		return importSiembra(ctx, true);
	}

	private ActionExecutionResult updateRecorrida(ActionContext ctx) {
		if (ctx.getRecorrida() == null) {
			List<LoadedLayerInfo> recorridas = ctx.getLayerContext().getRecorridas();
			if (recorridas.isEmpty()) {
				return ActionExecutionResult.notLaunched("No hay recorridas cargadas en el mapa. Importa una primero.");
			}
			String options = recorridas.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
			return ActionExecutionResult.notLaunched(
					"Hay varias recorridas cargadas. Especifica el nombre o activa solo una: " + options);
		}
		main.recorridaGUIController.doUpdateRecorrida(ctx.getRecorrida());
		return ActionExecutionResult.launched("Sincronizando recorrida " + ctx.getRecorrida().getNombre() + "...");
	}

	private ActionExecutionResult exportRecorrida(ActionContext ctx) {
		if (ctx.getRecorrida() == null) {
			List<LoadedLayerInfo> recorridas = ctx.getLayerContext().getRecorridas();
			if (recorridas.isEmpty()) {
				return ActionExecutionResult.notLaunched("No hay recorridas cargadas en el mapa.");
			}
			String options = recorridas.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
			return ActionExecutionResult.notLaunched("Especifica qué recorrida exportar: " + options);
		}
		main.recorridaGUIController.doExportRecorrida(ctx.getRecorrida());
		return ActionExecutionResult.launched("Exportando recorrida " + ctx.getRecorrida().getNombre() + "...");
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
				+ AchievementIntentCatalog.buildHelpBullets()
				+ "\n\n"
				+ layerContext.formatLayerList();
	}
}
