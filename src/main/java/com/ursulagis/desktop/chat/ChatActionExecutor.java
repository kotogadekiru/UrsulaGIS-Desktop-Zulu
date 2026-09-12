package com.ursulagis.desktop.chat;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.controller.ConfigGUI;
import com.ursulagis.desktop.gui.onboarding.OnboardingAchievements;
import com.ursulagis.desktop.tasks.ExportLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.ClonarLaborMapTask;
import com.ursulagis.desktop.tasks.procesar.ResumirLaborMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.Voyager2Settings;

/**
 * Executes a {@link ParsedIntent} by resolving map targets and calling the
 * existing Ursula GUI controllers and background tasks. Returns an
 * {@link ActionExecutionResult} with the chat message and whether UI was launched.
 */
public class ChatActionExecutor {

	private static final String SHARE_AFTER_IMPORT_HINT =
			"Cuando termine la importación, pedime compartirla.";

	private final JFXMain main;

	/**
	 * @param main application main window used to reach GUI controllers, the map,
	 *             and layer panel while executing chat actions
	 */
	public ChatActionExecutor(JFXMain main) {
		this.main = main;
	}

	/**
	 * Runs a single action or an ordered chain from {@code intent}.
	 * Import+share pairs of the same domain are handled specially when possible.
	 */
	public ActionExecutionResult executeAll(ParsedIntent intent, MapLayerContext layerContext) {
		List<UrsulaAction> actions = intent.getActions();
		if (actions.size() <= 1) {
			return execute(intent.withAction(actions.get(0)), layerContext);
		}
		ActionExecutionResult importShare = tryImportAndShareChain(actions, intent, layerContext);
		if (importShare != null) {
			return importShare;
		}

		StringBuilder sb = new StringBuilder();
		boolean anyLaunched = false;
		for (int i = 0; i < actions.size(); i++) {
			UrsulaAction a = actions.get(i);
			ActionExecutionResult r = execute(intent.withAction(a), layerContext);
			if (i > 0) {
				sb.append('\n');
			}
			sb.append(i + 1).append(". ").append(r.message());
			anyLaunched |= r.launched();
		}
		return new ActionExecutionResult(sb.toString(), anyLaunched);
	}

	/**
	 * Handles [IMPORT_X, COMPARTIR_X] for the same domain when the pair is exact.
	 * Siembra uses callback-after-import; cosecha/fert/pulv share if already loaded,
	 * otherwise open import and ask the user to share after.
	 *
	 * @return result when handled, or {@code null} to fall through to sequential execute
	 */
	private ActionExecutionResult tryImportAndShareChain(
			List<UrsulaAction> actions, ParsedIntent intent, MapLayerContext layerContext) {
		if (actions.size() != 2) {
			return null;
		}
		UrsulaAction first = actions.get(0);
		UrsulaAction second = actions.get(1);

		if (first == UrsulaAction.IMPORT_SIEMBRA && second == UrsulaAction.COMPARTIR_SIEMBRA) {
			return importYCompartirSiembra(layerContext);
		}
		if (first == UrsulaAction.IMPORT_COSECHA && second == UrsulaAction.COMPARTIR_COSECHA) {
			return importOrShareCosecha(layerContext);
		}
		if (first == UrsulaAction.IMPORT_FERTILIZACION && second == UrsulaAction.COMPARTIR_FERTILIZACION) {
			return importOrShareFertilizacion(layerContext, intent);
		}
		if (first == UrsulaAction.IMPORT_PULVERIZACION && second == UrsulaAction.COMPARTIR_PULVERIZACION) {
			return importOrSharePulverizacion(layerContext, intent);
		}
		return null;
	}

	private ActionExecutionResult importOrShareCosecha(MapLayerContext layerContext) {
		Optional<CosechaLabor> active = LaborTargetResolver.resolve(layerContext, null, true)
				.filter(CosechaLabor.class::isInstance)
				.map(CosechaLabor.class::cast);
		if (active.isPresent()) {
			ActionContext ctx = new ActionContext(main, null, layerContext);
			ctx.setLabor(active.get());
			ctx.setCosecha(active.get());
			return compartirCosecha(ctx);
		}
		main.cosechaGUIController.doOpenCosecha(null);
		return ActionExecutionResult.launched(
				"Diálogo de importación de cosecha abierto. " + SHARE_AFTER_IMPORT_HINT);
	}

	private ActionExecutionResult importOrShareFertilizacion(
			MapLayerContext layerContext, ParsedIntent intent) {
		Optional<FertilizacionLabor> fert = LaborTargetResolver.resolve(layerContext, intent.getTargetName(), false)
				.filter(FertilizacionLabor.class::isInstance)
				.map(FertilizacionLabor.class::cast);
		if (fert.isPresent()) {
			main.fertilizacionGUIController.doCompartirFertilizacion(fert.get());
			return ActionExecutionResult.launched("Compartiendo fertilización " + nameOf(fert.get()) + "...");
		}
		main.fertilizacionGUIController.doOpenFertMap(null);
		return ActionExecutionResult.launched(
				"Diálogo de importación de fertilización abierto. " + SHARE_AFTER_IMPORT_HINT);
	}

	private ActionExecutionResult importOrSharePulverizacion(
			MapLayerContext layerContext, ParsedIntent intent) {
		Optional<PulverizacionLabor> pulv = LaborTargetResolver.resolve(layerContext, intent.getTargetName(), false)
				.filter(PulverizacionLabor.class::isInstance)
				.map(PulverizacionLabor.class::cast);
		if (pulv.isPresent()) {
			main.pulverizacionGUIController.doCompartirPulverizacion(pulv.get());
			return ActionExecutionResult.launched("Compartiendo pulverización " + nameOf(pulv.get()) + "...");
		}
		main.pulverizacionGUIController.doOpenPulvMap(null);
		return ActionExecutionResult.launched(
				"Diálogo de importación de pulverización abierto. " + SHARE_AFTER_IMPORT_HINT);
	}

	/**
	 * Dispatches {@code intent} to the matching controller/task path after
	 * resolving labor/cosecha/recorrida when the action requires them.
	 */
	public ActionExecutionResult execute(ParsedIntent intent, MapLayerContext layerContext) {
		ActionContext ctx = new ActionContext(main, intent.getTargetName(), layerContext);
		resolveTargets(ctx, intent.getAction());

		return switch (intent.getAction()) {
			case HELP -> ActionExecutionResult.notLaunched(helpText(ctx.getLayerContext()));
			case LIST_LAYERS -> ActionExecutionResult.notLaunched(ctx.getLayerContext().formatLayerList());
			case UNKNOWN -> ActionExecutionResult.notLaunched(intent.getMessage());

			// --- Import ---
			case IMPORT_COSECHA -> {
				main.cosechaGUIController.doOpenCosecha(null);
				yield ActionExecutionResult.launched("Diálogo de importación de cosecha abierto.");
			}
			case IMPORT_SIEMBRA -> importSiembra(ctx, false);
			case IMPORT_FERTILIZACION -> {
				main.fertilizacionGUIController.doOpenFertMap(null);
				yield ActionExecutionResult.launched("Diálogo de importación de fertilización abierto.");
			}
			case IMPORT_PULVERIZACION -> {
				main.pulverizacionGUIController.doOpenPulvMap(null);
				yield ActionExecutionResult.launched("Diálogo de importación de pulverización abierto.");
			}
			case IMPORT_COSECHA_VOYAGER -> {
				String unsupported = Voyager2Settings.unsupportedReason(JFXMain.config);
				if (unsupported != null) {
					yield ActionExecutionResult.notLaunched(
							Messages.getString("CosechaGUIController.importarVoyagerUnsupported")
									+ " " + unsupported);
				}
				main.cosechaGUIController.doOpenCosechaVoyager();
				yield ActionExecutionResult.launched("Importación Voyager iniciada.");
			}
			case IMPORT_RECORRIDA -> {
				main.recorridaGUIController.doOpenRecorridaMap(null);
				yield ActionExecutionResult.launched("Diálogo de importación de recorrida abierto.");
			}
			case LOAD_RECORRIDAS -> loadRecorridas(intent);
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
			case IMPORT_POLIGONO -> {
				main.poligonoGUIController.doImportarPoligonos(null);
				yield ActionExecutionResult.launched("Diálogo de importación de polígonos abierto.");
			}
			case BULK_NDVI_DOWNLOAD -> {
				main.ndviGUIController.doBulkNDVIDownload();
				yield ActionExecutionResult.launched("Descarga masiva de NDVI iniciada.");
			}
			case DOWNLOAD_NDVI_ASIGNACIONES -> downloadNdviAsignaciones(intent);
			case DOWNLOAD_NDVI -> downloadNdvi(ctx);

			// --- Polygon ---
			case CREAR_POLIGONO -> {
				main.poligonoGUIController.doCrearPoligono();
				yield ActionExecutionResult.launched("Herramienta de polígono activada.");
			}
			case MEDIR_DISTANCIA -> {
				main.poligonoGUIController.doMedirDistancia();
				yield ActionExecutionResult.launched("Herramienta de medición activada.");
			}
			case ACTIVAR_POLIGONOS_SUPERFICIE -> {
				int count = main.poligonoGUIController.activarPoligonosConSuperficieMayorA(0);
				yield ActionExecutionResult.launched(count > 0
						? "Activé **" + count + "** polígono(s) con superficie mayor a 0 ha."
						: "No hay polígonos con superficie mayor a 0 ha cargados en el mapa.");
			}
			case UNIR_POLIGONOS -> {
				main.poligonoGUIController.chatUnirPoligonos();
				yield ActionExecutionResult.launched("Unión de polígonos iniciada.");
			}
			case INTERSECTAR_POLIGONOS -> {
				main.poligonoGUIController.chatIntersectarPoligonos();
				yield ActionExecutionResult.launched("Intersección de polígonos iniciada.");
			}
			case EXTRAER_POLIGONOS -> extraerPoligonos(ctx);
			case EXTRAER_CONTORNO -> extraerContorno(ctx);
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
			case CONVERTIR_POLIGONO_A_SUELO -> convertirPoligonoASuelo();

			// --- Harvest ---
			case COMPARTIR_COSECHA -> compartirCosecha(ctx);
			case GRILLAR_COSECHA -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatGrillarCosechas(c);
				return ActionExecutionResult.launched("Grillando cosecha " + nameOf(c) + "...");
			});
			case SUMAR_COSECHAS -> {
				main.cosechaGUIController.chatSumarCosechas();
				yield ActionExecutionResult.launched("Suma de cosechas iniciada.");
			}
			case COSECHA_A_SUELO -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatCrearSueloFromHarvest(c);
				return ActionExecutionResult.launched("Creando suelo desde cosecha " + nameOf(c) + "...");
			});
			case EXPORT_COSECHA_PUNTOS -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatExportHarvestDePuntos(c);
				return ActionExecutionResult.launched("Exportando puntos de cosecha " + nameOf(c) + "...");
			});
			case RECOMENDAR_FERT_N -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatRecomendFertNFromHarvest(c);
				return ActionExecutionResult.launched("Recomendación de fertilización N desde " + nameOf(c) + "...");
			});
			case RECOMENDAR_FERT_P -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatRecomendFertPAbsFromHarvest(c);
				return ActionExecutionResult.launched("Recomendación de fertilización P desde " + nameOf(c) + "...");
			});
			case RECOMENDAR_FERT_P_BALANCE -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatRecomendFertPRepFromHarvest(c);
				return ActionExecutionResult.launched("Recomendación de balance P desde " + nameOf(c) + "...");
			});
			case COSECHA_A_COSECHA -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatCrearCosechaDesdeCosecha(c);
				return ActionExecutionResult.launched("Creando cosecha desde " + nameOf(c) + "...");
			});
			case COSECHA_A_FERTILIZACION -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatCrearFertilizacionFromHarvest(c);
				return ActionExecutionResult.launched("Creando fertilización desde cosecha " + nameOf(c) + "...");
			});
			case COSECHA_A_PULVERIZACION -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatCrearPulverizacionFromHarvest(c);
				return ActionExecutionResult.launched("Creando pulverización desde cosecha " + nameOf(c) + "...");
			});
			case SIEMBRA_DESDE_COSECHA -> requireCosecha(ctx, c -> {
				main.siembraGUIController.doCrearSiembra(c);
				return ActionExecutionResult.launched("Creando siembra desde cosecha " + nameOf(c) + "...");
			});

			// --- Fertilization ---
			case COMPARTIR_FERTILIZACION -> requireFert(ctx, f -> {
				main.fertilizacionGUIController.doCompartirFertilizacion(f);
				return ActionExecutionResult.launched("Compartiendo fertilización " + nameOf(f) + "...");
			});
			case EXPORT_FERTILIZACION -> requireFert(ctx, f -> {
				main.fertilizacionGUIController.doExportPrescripcionFertilizacion(f);
				return ActionExecutionResult.launched("Exportando fertilización " + nameOf(f) + "...");
			});
			case UNIR_FERTILIZACIONES -> {
				FertilizacionLabor fert = asFert(ctx.getLabor());
				main.fertilizacionGUIController.chatUnirFertilizaciones(fert);
				yield ActionExecutionResult.launched("Unión/grillado de fertilizaciones iniciado.");
			}
			case PARTIR_FERTILIZACION -> requireFert(ctx, f -> {
				main.fertilizacionGUIController.chatPartirFertilizacion(f);
				return ActionExecutionResult.launched("Partiendo fertilización " + nameOf(f) + "...");
			});
			case SIEMBRA_DESDE_FERTILIZACION -> requireFert(ctx, f -> {
				main.fertilizacionGUIController.chatGenerarSiembraDesdeFertilizacion(f);
				return ActionExecutionResult.launched("Creando siembra desde fertilización " + nameOf(f) + "...");
			});

			// --- Seeding ---
			case COMPARTIR_SIEMBRA -> compartirSiembra(ctx);
			case EXPORT_SIEMBRA -> requireSiembra(ctx, s -> {
				main.siembraGUIController.chatExportPrescripcionSiembra(s);
				return ActionExecutionResult.launched("Exportando siembra " + nameOf(s) + "...");
			});
			case UNIR_SIEMBRAS -> {
				SiembraLabor siembra = asSiembra(ctx.getLabor());
				main.siembraGUIController.chatUnirSiembras(siembra);
				yield ActionExecutionResult.launched("Unión de siembras iniciada.");
			}
			case GRILLAR_SIEMBRA -> requireSiembra(ctx, s -> {
				main.siembraGUIController.chatGrillarSiembras(s);
				return ActionExecutionResult.launched("Grillando siembra " + nameOf(s) + "...");
			});
			case EDITAR_SIEMBRA -> requireSiembra(ctx, s -> {
				main.siembraGUIController.chatEditSiembra(s);
				return ActionExecutionResult.launched("Editando siembra " + nameOf(s) + "...");
			});
			case GENERAR_SIEMBRA_FERTILIZADA -> {
				main.siembraGUIController.generarSiembraFertilizadaProgrammatic(true);
				yield ActionExecutionResult.launched("Generando siembra fertilizada...");
			}

			// --- Spray ---
			case COMPARTIR_PULVERIZACION -> requirePulv(ctx, p -> {
				main.pulverizacionGUIController.doCompartirPulverizacion(p);
				return ActionExecutionResult.launched("Compartiendo pulverización " + nameOf(p) + "...");
			});
			case EXPORT_PULVERIZACION -> requirePulv(ctx, p -> {
				main.pulverizacionGUIController.doExportarPrescPulverizacion(p);
				return ActionExecutionResult.launched("Exportando pulverización " + nameOf(p) + "...");
			});
			case EXPORT_PULVERIZACION_JSON -> requirePulv(ctx, p -> {
				main.pulverizacionGUIController.doExportarPrescPulverizacionJSON(p);
				return ActionExecutionResult.launched("Exportando pulverización JSON " + nameOf(p) + "...");
			});
			case UNIR_PULVERIZACIONES -> {
				PulverizacionLabor pulv = asPulv(ctx.getLabor());
				main.pulverizacionGUIController.chatUnirPulverizaciones(pulv);
				yield ActionExecutionResult.launched("Unión/grillado de pulverizaciones iniciado.");
			}
			case EDITAR_PULVERIZACION -> requirePulv(ctx, p -> {
				main.pulverizacionGUIController.doEditPulverizacion(p);
				return ActionExecutionResult.launched("Editando pulverización " + nameOf(p) + "...");
			});

			// --- Soil ---
			case BALANCE_NUTRIENTES -> {
				main.sueloGUIController.doProcesarBalanceNutrientes();
				yield ActionExecutionResult.launched("Balance de nutrientes en proceso.");
			}
			case EDITAR_SUELO -> requireSuelo(ctx, s -> {
				main.sueloGUIController.chatEditSuelo(s);
				return ActionExecutionResult.launched("Editando suelo " + nameOf(s) + "...");
			});
			case ESTIMAR_RENDIMIENTO_SUELO -> requireSuelo(ctx, s -> {
				main.sueloGUIController.chatEstimarPotencialRendimiento(s);
				return ActionExecutionResult.launched("Estimando rendimiento potencial desde " + nameOf(s) + "...");
			});

			// --- Recorrida ---
			case UPDATE_RECORRIDA -> updateRecorrida(ctx);
			case EXPORT_RECORRIDA -> exportRecorrida(ctx);
			case COMPARTIR_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.doCompartirRecorrida(r);
				return ActionExecutionResult.launched("Compartiendo recorrida " + r.getNombre() + "...");
			});
			case GUARDAR_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.chatSaveRecorrida(r);
				return ActionExecutionResult.launched("Guardando recorrida " + r.getNombre() + "...");
			});
			case INTERPOLAR_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.chatInterpolarRecorrida(r);
				return ActionExecutionResult.launched("Interpolando recorrida " + r.getNombre() + "...");
			});
			case ASIGNAR_VALORES_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.doAsignarValoresRecorrida(r);
				return ActionExecutionResult.launched("Asignando valores a recorrida " + r.getNombre() + "...");
			});

			// --- Margin ---
			case GENERAR_MARGEN -> {
				main.configGUIController.doProcessMargin();
				yield ActionExecutionResult.launched("Rentabilidades iniciado con las capas activas.");
			}
			case EDITAR_MARGEN -> requireMargen(ctx, m -> {
				main.margenGUIController.chatEditMargin(m);
				return ActionExecutionResult.launched("Editando margen " + nameOf(m) + "...");
			});
			case SUMAR_MARGENES -> {
				main.margenGUIController.chatSumarMargenes();
				yield ActionExecutionResult.launched("Suma de márgenes iniciada.");
			}

			// --- NDVI ---
			case CONVERTIR_NDVI_A_COSECHA -> convertirNdviACosecha(ctx);
			case CONVERTIR_NDVI_A_FERTILIZACION -> {
				Optional<Ndvi> ndvi = resolveNdvi(ctx);
				if (ndvi.isEmpty()) {
					yield ActionExecutionResult.notLaunched("No hay NDVI seleccionado o cargado en el mapa.");
				}
				main.ndviGUIController.chatConvertirNdviAFertilizacion(ndvi.get());
				yield ActionExecutionResult.launched("Convirtiendo NDVI a fertilización...");
			}
			case CONVERTIR_NDVI_ACUM_A_COSECHA -> {
				main.ndviGUIController.chatConvertirNdviAcumuladoACosecha();
				yield ActionExecutionResult.launched("Convirtiendo NDVI acumulado a cosecha...");
			}
			case EXPORT_NDVI -> {
				main.ndviGUIController.chatExportNdviExcel();
				yield ActionExecutionResult.launched("Exportación NDVI a Excel/KMZ iniciada.");
			}
			case EXPORT_NDVI_TIFF -> {
				Optional<Ndvi> ndvi = resolveNdvi(ctx);
				if (ndvi.isEmpty()) {
					yield ActionExecutionResult.notLaunched("No hay NDVI seleccionado o cargado en el mapa.");
				}
				main.ndviGUIController.chatExportarTiffFile(ndvi.get());
				yield ActionExecutionResult.launched("Exportando NDVI a TIFF...");
			}
			case SHOW_NDVI_CHART -> {
				main.ndviGUIController.chatShowNdviChart();
				yield ActionExecutionResult.launched("Mostrando gráfico NDVI.");
			}
			case SHOW_NDVI_ACUM_CHART -> {
				main.ndviGUIController.chatShowNdviAcumChart();
				yield ActionExecutionResult.launched("Mostrando gráfico NDVI acumulado.");
			}
			case SHOW_NDVI_EVOLUTION -> {
				main.ndviGUIController.chatShowNdviEvolution();
				yield ActionExecutionResult.launched("Mostrando evolución NDVI.");
			}
			case SHOW_NDVI_HISTOGRAM -> {
				Optional<Ndvi> ndvi = resolveNdvi(ctx);
				if (ndvi.isEmpty()) {
					yield ActionExecutionResult.notLaunched("No hay NDVI seleccionado o cargado en el mapa.");
				}
				main.ndviGUIController.chatShowHistoNDVI(ndvi.get());
				yield ActionExecutionResult.launched("Mostrando histograma NDVI.");
			}
			case FILTRAR_NDVI_FECHA -> {
				main.ndviGUIController.chatFiltrarFecha();
				yield ActionExecutionResult.launched("Filtro de NDVI por fecha abierto.");
			}
			case GUARDAR_NDVI -> {
				main.ndviGUIController.chatSaveSelectedNdvi();
				yield ActionExecutionResult.launched("Guardando NDVI seleccionado...");
			}

			// --- Config ---
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
			case EXPORT_PANTALLA -> {
				main.doSnapshot();
				yield ActionExecutionResult.launched("Diálogo para exportar la pantalla abierto (Exportar → Pantalla).");
			}
			case GENERAR_ORDEN_COMPRA -> {
				main.configGUIController.doGenerarOrdenDeCompra();
				yield ActionExecutionResult.launched("Generación de orden de compra iniciada.");
			}
			case COTIZAR_ORDEN_COMPRA -> {
				main.configGUIController.chatShowOrdenesCompra();
				yield ActionExecutionResult.launched("Tabla de órdenes de compra abierta para cotizar.");
			}
			case GO_TO_ADDRESS -> {
				main.configGUIController.showGoToDialog();
				yield ActionExecutionResult.launched("Diálogo Ir a dirección abierto.");
			}
			case CAMBIAR_PROYECTO -> {
				String msg = main.configGUIController.chatSelectDB();
				yield ActionExecutionResult.launched(msg != null ? msg : "Proyecto actualizado.");
			}
			case ACTUALIZAR_APP -> {
				main.configGUIController.chatUpdate();
				yield ActionExecutionResult.launched("Buscando actualizaciones...");
			}
			case CORRELACIONAR_CAPAS -> {
				main.configGUIController.chatCorrelacionarCapas();
				yield ActionExecutionResult.launched("Correlación de capas iniciada.");
			}
			case CAMBIAR_IDIOMA -> {
				ConfigGUI.doChangeLocale();
				yield ActionExecutionResult.launched("Cambio de idioma abierto.");
			}
			case IMPORT_IMAGERY -> {
				main.importImagery();
				yield ActionExecutionResult.launched("Importación de imagen abierta.");
			}
			case IMPORT_SIEMBRA_SRM -> {
				main.siembraGUIController.chatImportSiembraSrm();
				yield ActionExecutionResult.launched("Importación de siembra SRM abierta.");
			}
			case EXPORT_SIEMBRA_SRM -> requireSiembra(ctx, s -> {
				main.siembraGUIController.chatExportPrescripcionSiembraSrm(s);
				return ActionExecutionResult.launched("Exportando siembra SRM...");
			});
			case UNIR_COSECHAS -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatUnirCosechas(c);
				return ActionExecutionResult.launched("Unión de cosechas iniciada.");
			});
			case EDITAR_COSECHA -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatEditCosecha(c);
				return ActionExecutionResult.launched("Editando cosecha...");
			});
			case SHOW_COSECHA_ELEVATION_CHART -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatShowCosechaElevationChart(c);
				return ActionExecutionResult.launched("Gráfico rinde vs elevación abierto.");
			});
			case GENERAR_RECORRIDA_DIRIGIDA -> requireLabor(ctx, false, labor -> {
				main.cosechaGUIController.chatGenerarRecorridaDirigida(labor);
				return ActionExecutionResult.launched("Generando recorrida dirigida...");
			});
			case RECOMENDAR_FERT_K -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatRecomendFertKFromHarvest(c);
				return ActionExecutionResult.launched("Recomendación K iniciada.");
			});
			case RECOMENDAR_FERT_S -> requireCosecha(ctx, c -> {
				main.cosechaGUIController.chatRecomendFertSFromHarvest(c);
				return ActionExecutionResult.launched("Recomendación S iniciada.");
			});
			case EDITAR_FERTILIZACION -> requireFert(ctx, f -> {
				main.fertilizacionGUIController.chatEditFertilizacion(f);
				return ActionExecutionResult.launched("Editando fertilización...");
			});
			case GUARDAR_POLIGONO -> {
				List<Poligono> polis = main.getPoligonosSeleccionados();
				if (polis == null || polis.isEmpty()) {
					polis = main.poligonoGUIController.getEnabledPoligonos();
				}
				if (polis == null || polis.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono para guardarlo.");
				}
				main.poligonoGUIController.chatGuardarPoligono(polis.get(0));
				yield ActionExecutionResult.launched("Guardando polígono...");
			}
			case EDITAR_POLIGONO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono.");
				}
				main.poligonoGUIController.chatEditarPoligono(p.get());
				yield ActionExecutionResult.launched("Editando polígono...");
			}
			case CLONAR_POLIGONO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono.");
				}
				main.poligonoGUIController.chatClonarPoligono(p.get());
				yield ActionExecutionResult.launched("Clonando polígono...");
			}
			case SIMPLIFICAR_POLIGONO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono.");
				}
				main.poligonoGUIController.chatSimplificarPoligono(p.get());
				yield ActionExecutionResult.launched("Simplificando polígono...");
			}
			case EXPLOTAR_POLIGONO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono.");
				}
				main.poligonoGUIController.chatExplotarPoligono(p.get());
				yield ActionExecutionResult.launched("Explotando polígono...");
			}
			case GO_TO_POLIGONO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty() || p.get().getLayer() == null) {
					yield ActionExecutionResult.notLaunched("Seleccioná o activá un polígono.");
				}
				main.viewGoTo(p.get().getLayer());
				yield ActionExecutionResult.launched("Vista centrada en el polígono.");
			}
			case POLIGONO_A_RECORRIDA -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un camino o polígono.");
				}
				main.poligonoGUIController.doConvertirARecorrida(p.get());
				yield ActionExecutionResult.launched("Convirtiendo a recorrida...");
			}
			case POLIGONO_A_CIRCULO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un polígono.");
				}
				main.poligonoGUIController.doCrearCirculo(p.get());
				yield ActionExecutionResult.launched("Convirtiendo a círculo...");
			}
			case EDITAR_CAMINO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un camino.");
				}
				main.poligonoGUIController.chatEditarCamino(p.get());
				yield ActionExecutionResult.launched("Editando camino...");
			}
			case ACORTAR_CAMINO -> {
				Optional<Poligono> p = resolvePoligono();
				if (p.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un camino.");
				}
				main.poligonoGUIController.chatAcortarCamino(p.get());
				yield ActionExecutionResult.launched("Acortando camino...");
			}
			case FILTRAR_NDVI_NUBLADO -> {
				main.ndviGUIController.chatFiltrarNublado();
				yield ActionExecutionResult.launched("Filtro de nubes NDVI aplicado.");
			}
			case GO_TO_NDVI -> {
				Optional<Ndvi> ndvi = resolveNdvi(ctx);
				if (ndvi.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un NDVI.");
				}
				if (ndvi.get().getLayer() != null) {
					main.viewGoTo(ndvi.get().getLayer());
				}
				yield ActionExecutionResult.launched("Vista centrada en NDVI.");
			}
			case EDITAR_NDVI -> {
				Optional<Ndvi> ndvi = resolveNdvi(ctx);
				if (ndvi.isEmpty()) {
					yield ActionExecutionResult.notLaunched("Seleccioná un NDVI.");
				}
				main.ndviGUIController.chatEditarNdvi(ndvi.get());
				yield ActionExecutionResult.launched("Editando NDVI...");
			}
			case GO_TO_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.chatGoToRecorrida(r);
				return ActionExecutionResult.launched("Vista centrada en la recorrida.");
			});
			case EDITAR_RECORRIDA -> requireRecorrida(ctx, r -> {
				main.recorridaGUIController.chatEditarRecorridaTabla(r);
				return ActionExecutionResult.launched("Tabla de recorrida abierta.");
			});
			case CONFIG_CULTIVOS -> {
				ConfigGUI.doConfigCultivo();
				yield ActionExecutionResult.launched("Configuración de cultivos abierta.");
			}
			case CONFIG_FERTILIZANTES -> {
				ConfigGUI.doConfigFertilizantes();
				yield ActionExecutionResult.launched("Configuración de fertilizantes abierta.");
			}
			case CONFIG_AGROQUIMICOS -> {
				ConfigGUI.doConfigAgroquimicos();
				yield ActionExecutionResult.launched("Configuración de agroquímicos abierta.");
			}
			case CONFIG_SEMILLAS -> {
				ConfigGUI.doConfigSemillas();
				yield ActionExecutionResult.launched("Configuración de semillas abierta.");
			}
			case CONFIG_PLAGA -> {
				ConfigGUI.doConfigPlaga();
				yield ActionExecutionResult.launched("Configuración de plagas abierta.");
			}
			case CONFIG_EMPRESA -> {
				ConfigGUI.doConfigEmpresa();
				yield ActionExecutionResult.launched("Configuración de empresa abierta.");
			}
			case CONFIG_ESTABLECIMIENTO -> {
				ConfigGUI.doConfigEstablecimiento();
				yield ActionExecutionResult.launched("Configuración de establecimiento abierta.");
			}
			case CONFIG_LOTE -> {
				ConfigGUI.doConfigLote();
				yield ActionExecutionResult.launched("Configuración de lotes abierta.");
			}
			case CONFIG_CAMPANIA -> {
				ConfigGUI.doConfigCampania();
				yield ActionExecutionResult.launched("Configuración de campañas abierta.");
			}
			case CONFIG_POLIGONOS_TABLE -> {
				main.configGUIController.doConfigPoligonos();
				yield ActionExecutionResult.launched("Tabla de polígonos abierta.");
			}
			case SHOW_NDVI_TABLE -> {
				main.configGUIController.doShowNdviTable();
				yield ActionExecutionResult.launched("Tabla de NDVI abierta.");
			}
			case SHOW_RECORRIDAS_TABLE -> {
				main.configGUIController.doShowRecorridaTable();
				yield ActionExecutionResult.launched("Tabla de recorridas abierta.");
			}
			case SHOW_ORDENES_COMPRA -> {
				main.configGUIController.doShowOrdenesCompra();
				yield ActionExecutionResult.launched("Tabla de órdenes de compra abierta.");
			}
			case SHOW_ORDENES_PULVERIZACION -> {
				main.configGUIController.doShowOrdenesPulverizacionTable();
				yield ActionExecutionResult.launched("Tabla de órdenes de pulverización abierta.");
			}
			case SHOW_ORDENES_FERTILIZACION -> {
				main.configGUIController.doShowOrdenesFertilizacionTable();
				yield ActionExecutionResult.launched("Tabla de órdenes de fertilización abierta.");
			}
			case SHOW_ORDENES_SIEMBRA -> {
				main.configGUIController.doShowOrdenesSiembraTable();
				yield ActionExecutionResult.launched("Tabla de órdenes de siembra abierta.");
			}
			case SHOW_CONFIG_TABLE -> {
				main.configGUIController.chatShowConfiguracionTable();
				yield ActionExecutionResult.launched("Tabla de configuración abierta.");
			}
			case SHOW_ACHIEVEMENTS -> {
				com.ursulagis.desktop.gui.onboarding.AchievementsOverviewDialog.show(JFXMain.stage);
				yield ActionExecutionResult.launched("Logros de onboarding abiertos.");
			}
			case SHOW_ACERCA_DE -> {
				main.configGUIController.doShowAcercaDe();
				yield ActionExecutionResult.launched("Acerca de UrsulaGIS.");
			}
			case SHOW_LOG -> {
				main.configGUIController.chatShowLog();
				yield ActionExecutionResult.launched("Log de la aplicación abierto.");
			}
			case ACENTUAR_MEDIA_LABOR -> requireLabor(ctx, false, labor -> {
				@SuppressWarnings("unchecked")
				Labor<LaborItem> typed = (Labor<LaborItem>) labor;
				main.genericGUIController.chatAccentuateMeanLabor(typed);
				return ActionExecutionResult.launched("Acentuando media de " + nameOf(labor) + "...");
			});
			case SHOW_HISTOGRAMA_LABOR -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatShowHistoLabor(labor);
				return ActionExecutionResult.launched("Histograma de labor abierto.");
			});
			case SHOW_LABOR_TABLE -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatShowDataTable(labor);
				return ActionExecutionResult.launched("Tabla de datos de labor abierta.");
			});
			case CORTAR_LABOR_POR_POLIGONO -> requireLabor(ctx, false, labor -> {
				main.poligonoGUIController.chatCortarLaborPorPoligono(labor);
				return ActionExecutionResult.launched("Cortando labor por polígonos activos...");
			});
			case REMOVE_LAYER -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatRemoveLabor(labor);
				return ActionExecutionResult.launched("Capa removida del mapa.");
			});
			case LAYER_TRANSPARENCIA -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatShowTransparencia(labor);
				return ActionExecutionResult.launched("Control de transparencia abierto.");
			});

			// --- Generic labor ---
			case GO_TO_LAYER -> goToLabor(ctx);
			case RESUMIR_LABOR -> resumirLabor(ctx);
			case EXPORT_LABOR -> exportLabor(ctx);
			case CLONAR_LABOR -> clonarLabor(ctx);
			case GUARDAR_LABOR -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatGuardarLabor(labor);
				return ActionExecutionResult.launched("Guardando labor " + nameOf(labor) + "...");
			});
			case FILTRAR_OUTLIERS -> filtrarOutliers(ctx);
			case REPORTE_PDF_LABOR -> requireLabor(ctx, false, labor -> {
				main.genericGUIController.chatGenerarReportePDF(labor);
				return ActionExecutionResult.launched("Generando reporte PDF de " + nameOf(labor) + "...");
			});
			case JUNTAR_SHAPES -> {
				main.genericGUIController.doJuntarShapefiles();
				yield ActionExecutionResult.launched("Unión de shapefiles iniciada.");
			}
		};
	}

	private Optional<Poligono> resolvePoligono() {
		List<Poligono> selected = main.getPoligonosSeleccionados();
		if (selected != null && !selected.isEmpty()) {
			return Optional.of(selected.get(0));
		}
		List<Poligono> enabled = main.poligonoGUIController.getEnabledPoligonos();
		if (enabled != null && !enabled.isEmpty()) {
			return Optional.of(enabled.get(0));
		}
		return Optional.empty();
	}

	/** Fills {@link ActionContext} with labor/cosecha/recorrida as required by {@code action}. */
	private void resolveTargets(ActionContext ctx, UrsulaAction action) {
		if (action == UrsulaAction.COMPARTIR_SIEMBRA
				|| action == UrsulaAction.EXPORT_SIEMBRA
				|| action == UrsulaAction.EXPORT_SIEMBRA_SRM
				|| action == UrsulaAction.GRILLAR_SIEMBRA
				|| action == UrsulaAction.EDITAR_SIEMBRA) {
			resolveSiembra(ctx);
		} else if (action.requiresLabor() || action.requiresCosecha()) {
			resolveLabor(ctx, action.requiresCosecha());
		}
		if (action.requiresRecorrida()) {
			resolveRecorrida(ctx);
		}
	}

	/** Resolves an active or named {@link SiembraLabor} for share-siembra. */
	private void resolveSiembra(ActionContext ctx) {
		LaborTargetResolver.resolveActiveSiembra(ctx.getLayerContext())
				.or(() -> LaborTargetResolver.resolve(ctx.getLayerContext(), ctx.getTargetName(), false)
						.filter(SiembraLabor.class::isInstance)
						.map(SiembraLabor.class::cast))
				.ifPresent(ctx::setLabor);
	}

	/** Resolves labor (and cosecha when applicable) via {@link LaborTargetResolver}. */
	private void resolveLabor(ActionContext ctx, boolean cosechaOnly) {
		findLoadedLabor(ctx, cosechaOnly).ifPresent(l -> {
			ctx.setLabor(l);
			if (l instanceof CosechaLabor c) {
				ctx.setCosecha(c);
			}
		});
	}

	/** Resolves a scouting route by name, selection, or single active/loaded layer. */
	private void resolveRecorrida(ActionContext ctx) {
		findLoadedRecorrida(ctx).ifPresent(ctx::setRecorrida);
	}

	/** Labor matching the intent target via {@link LaborTargetResolver}. */
	private Optional<Labor<?>> findLoadedLabor(ActionContext ctx, boolean cosechaOnly) {
		return LaborTargetResolver.resolve(ctx.getLayerContext(), ctx.getTargetName(), cosechaOnly);
	}

	/**
	 * Resolves a recorrida by target name, tree selection, single active, or sole loaded layer.
	 */
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

	/** First selected NDVI, or first NDVI layer on the map. */
	private Optional<Ndvi> resolveNdvi(ActionContext ctx) {
		try {
			List<Ndvi> selected = main.getNdviSeleccionados();
			if (selected != null && !selected.isEmpty()) {
				return Optional.of(selected.get(0));
			}
		} catch (Exception ignored) {
			// selection APIs may throw when map is empty
		}
		List<LoadedLayerInfo> ndviLayers = ctx.getLayerContext().findNdviLayers();
		if (!ndviLayers.isEmpty() && ndviLayers.get(0).getEntity() instanceof Ndvi ndvi) {
			return Optional.of(ndvi);
		}
		return Optional.empty();
	}

	/** Casts a layer entity to {@link Labor}, optionally requiring {@link CosechaLabor}. */
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

	/** User-facing message when the requested labor cannot be uniquely resolved. */
	private static String ambiguousLaborMessage(ActionContext ctx, boolean cosechaOnly) {
		return LaborTargetResolver.ambiguousLaborMessage(ctx.getLayerContext(), ctx.getTargetName(), cosechaOnly);
	}

	@FunctionalInterface
	private interface LaborAction {
		ActionExecutionResult apply(Labor<?> labor);
	}

	@FunctionalInterface
	private interface CosechaAction {
		ActionExecutionResult apply(CosechaLabor cosecha);
	}

	@FunctionalInterface
	private interface FertAction {
		ActionExecutionResult apply(FertilizacionLabor fert);
	}

	@FunctionalInterface
	private interface SiembraAction {
		ActionExecutionResult apply(SiembraLabor siembra);
	}

	@FunctionalInterface
	private interface PulvAction {
		ActionExecutionResult apply(PulverizacionLabor pulv);
	}

	@FunctionalInterface
	private interface SueloAction {
		ActionExecutionResult apply(Suelo suelo);
	}

	@FunctionalInterface
	private interface MargenAction {
		ActionExecutionResult apply(Margen margen);
	}

	@FunctionalInterface
	private interface RecorridaAction {
		ActionExecutionResult apply(Recorrida recorrida);
	}

	private ActionExecutionResult requireLabor(ActionContext ctx, boolean cosechaOnly, LaborAction action) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, cosechaOnly));
		}
		return action.apply(ctx.getLabor());
	}

	private ActionExecutionResult requireCosecha(ActionContext ctx, CosechaAction action) {
		if (ctx.getCosecha() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, true));
		}
		return action.apply(ctx.getCosecha());
	}

	private ActionExecutionResult requireFert(ActionContext ctx, FertAction action) {
		FertilizacionLabor fert = asFert(ctx.getLabor());
		if (fert == null) {
			return ActionExecutionResult.notLaunched(
					"No encontré una fertilización activa. Activá una o nombrala.");
		}
		return action.apply(fert);
	}

	private ActionExecutionResult requireSiembra(ActionContext ctx, SiembraAction action) {
		SiembraLabor siembra = asSiembra(ctx.getLabor());
		if (siembra == null) {
			return ActionExecutionResult.notLaunched(LaborTargetResolver.ambiguousSiembraMessage(ctx.getLayerContext()));
		}
		return action.apply(siembra);
	}

	private ActionExecutionResult requirePulv(ActionContext ctx, PulvAction action) {
		PulverizacionLabor pulv = asPulv(ctx.getLabor());
		if (pulv == null) {
			return ActionExecutionResult.notLaunched(
					"No encontré una pulverización activa. Activá una o nombrala.");
		}
		return action.apply(pulv);
	}

	private ActionExecutionResult requireSuelo(ActionContext ctx, SueloAction action) {
		if (!(ctx.getLabor() instanceof Suelo suelo)) {
			return ActionExecutionResult.notLaunched(
					"No encontré un mapa de suelo activo. Activá uno o nombraló.");
		}
		return action.apply(suelo);
	}

	private ActionExecutionResult requireMargen(ActionContext ctx, MargenAction action) {
		if (!(ctx.getLabor() instanceof Margen margen)) {
			return ActionExecutionResult.notLaunched(
					"No encontré un margen activo. Activá uno o nombraló.");
		}
		return action.apply(margen);
	}

	private ActionExecutionResult requireRecorrida(ActionContext ctx, RecorridaAction action) {
		if (ctx.getRecorrida() == null) {
			List<LoadedLayerInfo> recorridas = ctx.getLayerContext().getRecorridas();
			if (recorridas.isEmpty()) {
				return ActionExecutionResult.notLaunched("No hay recorridas cargadas en el mapa.");
			}
			String options = recorridas.stream().map(LoadedLayerInfo::describe).collect(Collectors.joining(", "));
			return ActionExecutionResult.notLaunched(
					"Hay varias recorridas. Especificá el nombre o activá solo una: " + options);
		}
		return action.apply(ctx.getRecorrida());
	}

	private static FertilizacionLabor asFert(Labor<?> labor) {
		return labor instanceof FertilizacionLabor f ? f : null;
	}

	private static SiembraLabor asSiembra(Labor<?> labor) {
		return labor instanceof SiembraLabor s ? s : null;
	}

	private static PulverizacionLabor asPulv(Labor<?> labor) {
		return labor instanceof PulverizacionLabor p ? p : null;
	}

	private ActionExecutionResult extraerPoligonos(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.poligonoGUIController.doExtraerPoligonos(ctx.getLabor());
		return ActionExecutionResult.launched("Extrayendo polígonos de " + nameOf(ctx.getLabor()) + "...");
	}

	private ActionExecutionResult extraerContorno(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.poligonoGUIController.doExtraerContorno(ctx.getLabor());
		return ActionExecutionResult.launched("Extrayendo contorno de " + nameOf(ctx.getLabor()) + "...");
	}

	private ActionExecutionResult convertirPoligonoASuelo() {
		List<Poligono> enabled = main.poligonoGUIController.getEnabledPoligonos();
		if (enabled == null || enabled.isEmpty()) {
			List<Poligono> selected = main.getPoligonosSeleccionados();
			if (selected != null && !selected.isEmpty()) {
				enabled = selected;
			}
		}
		if (enabled == null || enabled.isEmpty()) {
			return ActionExecutionResult.notLaunched(
					"No hay un polígono activo. Activá uno o seleccionálo en el mapa.");
		}
		main.poligonoGUIController.chatCrearSueloFromPoligono(enabled.get(0));
		return ActionExecutionResult.launched("Creando suelo desde polígono...");
	}

	private ActionExecutionResult convertirNdviACosecha(ActionContext ctx) {
		Optional<Ndvi> ndvi = resolveNdvi(ctx);
		if (ndvi.isEmpty()) {
			return ActionExecutionResult.notLaunched("No hay NDVI seleccionado o cargado en el mapa.");
		}
		main.ndviGUIController.convertNdviToCosechaProgrammatic(ndvi.get(), 4.6, "soja", null);
		return ActionExecutionResult.launched(
				"Convirtiendo NDVI **" + ndvi.get().getNombre() + "** a cosecha...");
	}

	@SuppressWarnings("unchecked")
	private ActionExecutionResult filtrarOutliers(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.genericGUIController.chatOutliersLabor((Labor<LaborItem>) ctx.getLabor());
		return ActionExecutionResult.launched("Filtrando outliers de " + nameOf(ctx.getLabor()) + "...");
	}

	/** Zooms the map to the resolved labor layer. */
	private ActionExecutionResult goToLabor(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.viewGoTo(ctx.getLabor());
		return ActionExecutionResult.launched("Vista centrada en " + nameOf(ctx.getLabor()) + ".");
	}

	/** Runs {@link ResumirLaborMapTask} on the active resolved labor. */
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

	/** Exports the resolved labor to a user-chosen shapefile. */
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

	/** Clones the resolved labor onto the map. */
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

	/** Starts NDVI download for the footprint of the resolved labor. */
	private ActionExecutionResult downloadNdvi(ActionContext ctx) {
		if (ctx.getLabor() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, false));
		}
		main.poligonoGUIController.chatGetNdviTiffFile(ctx.getLabor());
		return ActionExecutionResult.launched("Descarga de NDVI iniciada para " + nameOf(ctx.getLabor()) + ".");
	}

	/**
	 * Downloads NDVI for assignment contours matching campaign/crop/period;
	 * may ask for campaign via {@link ChatPendingFollowUp} when filters are incomplete.
	 */
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

	/**
	 * Loads saved recorridas from the DB matching crop/lot keywords, or opens
	 * the recorrida table when no filter keywords are found.
	 */
	private ActionExecutionResult loadRecorridas(ParsedIntent intent) {
		String sourceText = intent.getSourceUserText() != null
				? intent.getSourceUserText()
				: (intent.getMessage() != null ? intent.getMessage() : "");
		List<Recorrida> all;
		try {
			all = DAH.getAllRecorridas();
		} catch (Exception e) {
			return ActionExecutionResult.notLaunched(
					"No pude leer las recorridas guardadas: " + e.getMessage());
		}
		if (all == null || all.isEmpty()) {
			return ActionExecutionResult.notLaunched(
					"No hay recorridas guardadas. Importá una primero (Herramientas → Recorrida).");
		}

		AsignacionNdviRequest filter = AsignacionNdviRequest.parse(
				sourceText,
				intent.getCampaniaName(),
				intent.getCultivoName(),
				null,
				null);
		Set<String> keywords = buildRecorridaKeywords(sourceText, filter, intent.getTargetName());
		List<Recorrida> matched = filterRecorridas(all, keywords);

		String normalized = AchievementIntentCatalog.normalize(sourceText);
		boolean wantsLatest = normalized.contains("ultima") || normalized.contains("ultimo");

		if (keywords.isEmpty()) {
			main.configGUIController.doShowRecorridaTable();
			return ActionExecutionResult.launched(
					"Abrí la tabla de recorridas (" + all.size() + "). Elegí cuáles mostrar en el mapa.");
		}

		if (matched.isEmpty()) {
			main.configGUIController.doShowRecorridaTable();
			return ActionExecutionResult.notLaunched(
					"No encontré recorridas que coincidan con "
							+ String.join(", ", keywords)
							+ ". Abrí la tabla de recorridas para que elijas.");
		}

		List<Recorrida> toLoad = matched;
		toLoad.sort(Comparator
				.comparing((Recorrida r) -> r.getId() == null ? 0L : r.getId())
				.reversed());
		if (wantsLatest && toLoad.size() > 5) {
			toLoad = new ArrayList<>(toLoad.subList(0, 5));
		}

		for (Recorrida r : toLoad) {
			main.recorridaGUIController.chatGoToRecorrida(r);
		}

		String names = toLoad.stream()
				.map(r -> r.getNombre() != null && !r.getNombre().isBlank() ? r.getNombre() : ("#" + r.getId()))
				.collect(Collectors.joining(", "));
		String filterPart = filter.cultivoName() != null ? " (cultivo " + filter.cultivoName() + ")" : "";
		return ActionExecutionResult.launched(
				"Cargando " + toLoad.size() + " recorrida(s)" + filterPart + ": " + names + ".");
	}

	/** Builds search keywords from target name, crop, assigned lots, and crop tokens in text. */
	private static Set<String> buildRecorridaKeywords(
			String sourceText, AsignacionNdviRequest filter, String targetName) {
		Set<String> keywords = new LinkedHashSet<>();
		if (targetName != null && !targetName.isBlank()) {
			keywords.add(AchievementIntentCatalog.normalize(targetName));
		}
		if (filter.cultivoName() != null && !filter.cultivoName().isBlank()) {
			keywords.add(AchievementIntentCatalog.normalize(filter.cultivoName()));
		}
		try {
			List<Asignacion> asignaciones = DAH.getAllAsignaciones();
			String wantCultivo = filter.cultivoName() != null
					? AchievementIntentCatalog.normalize(filter.cultivoName()) : null;
			for (Asignacion a : asignaciones) {
				if (wantCultivo != null) {
					if (a.getCultivo() == null || a.getCultivo().getNombre() == null) {
						continue;
					}
					String have = AchievementIntentCatalog.normalize(a.getCultivo().getNombre());
					if (!have.contains(wantCultivo) && !wantCultivo.contains(have)) {
						continue;
					}
				}
				if (a.getLote() != null && a.getLote().getNombre() != null) {
					String lote = AchievementIntentCatalog.normalize(a.getLote().getNombre());
					if (!lote.isBlank()) {
						keywords.add(lote);
					}
				}
			}
		} catch (Exception ignored) {
			// DB may be unavailable in tests / early startup
		}
		String n = AchievementIntentCatalog.normalize(sourceText == null ? "" : sourceText);
		for (String crop : List.of("soja", "maiz", "trigo", "girasol", "cebada", "sorgo")) {
			if (n.contains(crop)) {
				keywords.add(crop);
			}
		}
		keywords.removeIf(k -> k.length() < 3);
		return keywords;
	}

	/** Filters recorridas whose name/observation/date contain any keyword. */
	private static List<Recorrida> filterRecorridas(List<Recorrida> all, Set<String> keywords) {
		if (keywords == null || keywords.isEmpty()) {
			return List.of();
		}
		List<Recorrida> matched = new ArrayList<>();
		for (Recorrida r : all) {
			String haystack = AchievementIntentCatalog.normalize(
					safe(r.getNombre()) + " " + safe(r.getObservacion()) + " " + safe(r.getFechaString()));
			for (String kw : keywords) {
				if (haystack.contains(kw)) {
					matched.add(r);
					break;
				}
			}
		}
		return matched;
	}

	/** Null-safe string for building searchable recorrida haystacks. */
	private static String safe(String s) {
		return s == null ? "" : s;
	}

	/** Shares the resolved harvest map online. */
	private ActionExecutionResult compartirCosecha(ActionContext ctx) {
		if (ctx.getCosecha() == null) {
			return ActionExecutionResult.notLaunched(ambiguousLaborMessage(ctx, true));
		}
		main.cosechaGUIController.doCompartirCosecha(ctx.getCosecha());
		return ActionExecutionResult.launched("Compartiendo cosecha " + nameOf(ctx.getCosecha()) + "...");
	}

	/**
	 * Opens siembra import; when {@code shareAfterImport} is true, shares the
	 * imported map automatically after the dialog finishes.
	 */
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

	/** Shares the resolved seeding prescription (QR). */
	private ActionExecutionResult compartirSiembra(ActionContext ctx) {
		if (!(ctx.getLabor() instanceof SiembraLabor siembra)) {
			return ActionExecutionResult.notLaunched(LaborTargetResolver.ambiguousSiembraMessage(ctx.getLayerContext()));
		}
		main.siembraGUIController.doCompartirSiembra(siembra);
		return ActionExecutionResult.launched("Compartiendo siembra **" + nameOf(siembra) + "**...");
	}

	/**
	 * Shares the active siembra if one is unambiguous; otherwise opens import
	 * and shares automatically after load.
	 */
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

	/** Syncs the resolved scouting route from the cloud. */
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

	/** Exports the resolved scouting route. */
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

	/** Whether {@code labor} is currently enabled in the layer snapshot. */
	private static boolean isLayerActive(ActionContext ctx, Labor<?> labor) {
		return ctx.getLayerContext().getLayers().stream()
				.filter(info -> info.getEntity() == labor)
				.anyMatch(LoadedLayerInfo::isActive);
	}

	/** Display name of the labor, or {@code "sin nombre"} when unset. */
	private static String nameOf(Labor<?> labor) {
		return labor.getNombre() != null ? labor.getNombre() : "sin nombre";
	}

	/** Help intro plus achievement-derived bullets and the current layer list. */
	private static String helpText(MapLayerContext layerContext) {
		return UrsulaPersonality.helpIntro() + "\n"
				+ AchievementIntentCatalog.buildHelpBullets()
				+ "\n\n"
				+ layerContext.formatLayerList();
	}
}
