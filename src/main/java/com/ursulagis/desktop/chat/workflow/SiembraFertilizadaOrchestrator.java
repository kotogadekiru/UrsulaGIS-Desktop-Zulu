package com.ursulagis.desktop.chat.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.SiembraConfigPrefill;
import com.ursulagis.desktop.gui.chat.WorkflowPolygonChoiceDialog;

import javafx.application.Platform;

/**
 * Executes the fertilized seeding workflow one step at a time on the FX thread.
 */
public final class SiembraFertilizadaOrchestrator {

	private final SiembraFertilizadaWorkflowRequest request;
	private SiembraFertilizadaWorkflowStep step = SiembraFertilizadaWorkflowStep.SELECT_FIELD_POLYGON;
	private Poligono fieldPolygon;
	private Poligono lomasPolygon;
	private final List<Poligono> goodZonePolygons = new ArrayList<>();
	private int goodZoneIndex;
	private int siembraFertPairIndex;
	private boolean waitingAsync;

	public SiembraFertilizadaOrchestrator(SiembraFertilizadaWorkflowRequest request) {
		this.request = request != null ? request : SiembraFertilizadaWorkflowRequest.parse("");
	}

	public SiembraFertilizadaWorkflowStep getCurrentStep() {
		return step;
	}

	public boolean isWaitingAsync() {
		return waitingAsync;
	}

	public WorkflowStepResult executeStep(JFXMain main, MapLayerContext layerContext) {
		if (step == SiembraFertilizadaWorkflowStep.DONE) {
			return WorkflowStepResult.done("El flujo de siembra fertilizada ya finalizó. Podés iniciar uno nuevo repitiendo el pedido.");
		}
		if (waitingAsync) {
			return WorkflowStepResult.step(step, step,
					"Esperando que termine el proceso en curso…", false, true, false);
		}

		return switch (step) {
			case SELECT_FIELD_POLYGON -> selectFieldPolygon(main, layerContext);
			case DOWNLOAD_NDVI -> downloadNdvi(main);
			case CONVERT_NDVI_TO_HARVEST -> convertNdviToHarvest(main, layerContext);
			case RECOMMEND_FERT_P -> recommendFertP(main);
			case CREATE_SIEMBRA_LOMAS -> createSiembraLomas(main, layerContext);
			case CREATE_SIEMBRA_GOOD_ZONES -> createSiembraGoodZones(main, layerContext);
			case GENERATE_SIEMBRA_FERTILIZADA -> generateSiembraFertilizada(main);
			case DONE -> WorkflowStepResult.done("Flujo completado.");
		};
	}

	private WorkflowStepResult selectFieldPolygon(JFXMain main, MapLayerContext layerContext) {
		List<Poligono> candidates = WorkflowLayerHelper.findPolygonsStrict(main, request.fieldName());
		if (candidates.isEmpty()) {
			candidates = WorkflowLayerHelper.findPolygons(main, request.fieldName());
		}
		if (candidates.isEmpty()) {
			candidates = WorkflowLayerHelper.findPolygons(layerContext, request.fieldName());
		}
		if (candidates.isEmpty()) {
			return WorkflowStepResult.step(step, step,
					"No hay polígonos cargados. Importá los ambientes de **"
							+ request.fieldName() + "** y volvé a pedir el flujo.",
					false, false, true);
		}

		if (candidates.size() == 1) {
			fieldPolygon = candidates.get(0);
		} else {
			Poligono preferred = WorkflowLayerHelper.preferFieldContour(candidates);
			Optional<Poligono> chosen = preferred != null
					? Optional.of(preferred)
					: resolvePolygon(candidates, request.fieldName());
			if (chosen.isEmpty()) {
				return WorkflowStepResult.step(step, step, "Selección de polígono cancelada.", false, false, true);
			}
			fieldPolygon = chosen.get();
		}

		WorkflowLayerHelper.setOnlyPolygonsEnabled(main, List.of(fieldPolygon));
		WorkflowLayerHelper.zoomToPoligono(main, fieldPolygon);

		lomasPolygon = resolvePolygon(
				WorkflowLayerHelper.findPolygonsStrict(main, "loma"), "lomas").orElse(null);
		goodZonePolygons.clear();
		goodZonePolygons.addAll(WorkflowLayerHelper.findPolygonsStrict(main, request.fieldName()).stream()
				.filter(p -> p != fieldPolygon && (lomasPolygon == null || p != lomasPolygon))
				.toList());
		if (goodZonePolygons.isEmpty()) {
			goodZonePolygons.addAll(WorkflowLayerHelper.findPolygonsStrict(main, "pehuen"));
		}
		if (goodZonePolygons.isEmpty()) {
			goodZonePolygons.addAll(WorkflowLayerHelper.findPolygonsStrict(main, "buena"));
		}

		step = SiembraFertilizadaWorkflowStep.DOWNLOAD_NDVI;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.SELECT_FIELD_POLYGON,
				step,
				"Activé el polígono **" + fieldPolygon.getNombre() + "**."
						+ (lomasPolygon != null ? " Lomas: **" + lomasPolygon.getNombre() + "**." : "")
						+ (goodZonePolygons.isEmpty() ? "" : " Zonas buenas: "
								+ goodZonePolygons.stream().map(Poligono::getNombre).collect(Collectors.joining(", ")))
						+ ".",
				true, false, false);
	}

	private WorkflowStepResult downloadNdvi(JFXMain main) {
		if (fieldPolygon == null) {
			step = SiembraFertilizadaWorkflowStep.SELECT_FIELD_POLYGON;
			return executeStep(main, null);
		}

		Optional<Ndvi> existing = WorkflowLayerHelper.findBestNdvi(main);
		if (existing.isPresent()) {
			step = SiembraFertilizadaWorkflowStep.CONVERT_NDVI_TO_HARVEST;
			return WorkflowStepResult.step(
					SiembraFertilizadaWorkflowStep.DOWNLOAD_NDVI,
					step,
					"Ya hay NDVI cargado (**" + existing.get().getNombre() + "**). Salto la descarga.",
					false, false, false);
		}

		waitingAsync = true;
		main.poligonoGUIController.downloadNdviForPoligono(
				fieldPolygon,
				request.ndviBegin(),
				request.ndviEnd(),
				() -> onAsyncComplete(SiembraFertilizadaWorkflowStep.CONVERT_NDVI_TO_HARVEST));

		step = SiembraFertilizadaWorkflowStep.CONVERT_NDVI_TO_HARVEST;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.DOWNLOAD_NDVI,
				step,
				"Descargando NDVI para **" + fieldPolygon.getNombre() + "** ("
						+ request.ndviBegin() + " → " + request.ndviEnd() + ").",
				true, true, false);
	}

	private WorkflowStepResult convertNdviToHarvest(JFXMain main, MapLayerContext layerContext) {
		Optional<Ndvi> ndvi = WorkflowLayerHelper.findBestNdvi(main);
		if (ndvi.isEmpty()) {
			ndvi = WorkflowLayerHelper.findBestNdvi(layerContext);
		}
		if (ndvi.isEmpty()) {
			return WorkflowStepResult.step(step, step,
					"No hay capas NDVI listas. Esperá a que termine la descarga o cargá NDVI manualmente.",
					false, false, true);
		}

		double yield = request.yieldTn() > 0 ? request.yieldTn() : 4.6;
		waitingAsync = true;
		Ndvi selected = ndvi.get();
		main.ndviGUIController.convertNdviToCosechaProgrammatic(
				selected, yield, request.harvestCrop(),
				() -> onAsyncComplete(SiembraFertilizadaWorkflowStep.RECOMMEND_FERT_P));

		step = SiembraFertilizadaWorkflowStep.RECOMMEND_FERT_P;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.CONVERT_NDVI_TO_HARVEST,
				step,
				"Convirtiendo **" + selected.getNombre() + "** a cosecha de **" + request.harvestCrop()
						+ "** con **" + yield + " t/ha**.",
				true, true, false);
	}

	private WorkflowStepResult recommendFertP(JFXMain main) {
		Optional<CosechaLabor> cosecha = WorkflowLayerHelper.findLatestCosecha(main);
		if (cosecha.isEmpty()) {
			return WorkflowStepResult.step(step, step,
					"No encontré la cosecha generada. Completá el paso anterior o activá la capa de cosecha.",
					false, false, true);
		}

		waitingAsync = true;
		main.cosechaGUIController.recommendFertPRepProgrammatic(
				cosecha.get(), request.fertSourceKey(),
				() -> onAsyncComplete(SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_LOMAS));

		step = SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_LOMAS;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.RECOMMEND_FERT_P,
				step,
				"Generando recomendación **Fert. P Reposición** con **" + request.fertSourceKey()
						+ "** sobre **" + cosecha.get().getNombre() + "**.",
				true, true, false);
	}

	private WorkflowStepResult createSiembraLomas(JFXMain main, MapLayerContext layerContext) {
		if (lomasPolygon == null) {
			lomasPolygon = resolvePolygon(
					WorkflowLayerHelper.findPolygonsStrict(main, "loma"), "lomas").orElse(null);
		}
		if (lomasPolygon == null) {
			step = SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_GOOD_ZONES;
			goodZoneIndex = 0;
			return WorkflowStepResult.step(
					SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_LOMAS,
					step,
					"No encontré polígono de **lomas**. Salto al paso de zonas buenas.",
					false, false, false);
		}

		WorkflowLayerHelper.setOnlyPolygonsEnabled(main, List.of(lomasPolygon));
		main.poligonoGUIController.doConvertirPoligonosASiembra(
				new SiembraConfigPrefill(request.lomasSeed(), request.rowSpacingM()),
				() -> Platform.runLater(ChatWorkflowSession::resumeAfterAsync));

		step = SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_GOOD_ZONES;
		goodZoneIndex = 0;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_LOMAS,
				step,
				"**Convertir a Siembra** — lomas (**" + lomasPolygon.getNombre() + "**).\n"
						+ "Prellenado: cultivo **" + request.seedingCrop() + "**, entresurco **"
						+ request.rowSpacingM() + " m**, semilla **" + request.lomasSeed() + "**.\n"
						+ "Confirmá el diálogo y la dosis objetivo.",
				true, false, true);
	}

	private WorkflowStepResult createSiembraGoodZones(JFXMain main, MapLayerContext layerContext) {
		if (goodZonePolygons.isEmpty()) {
			goodZonePolygons.addAll(WorkflowLayerHelper.findPolygons(main, request.fieldName()).stream()
					.filter(p -> lomasPolygon == null || p != lomasPolygon)
					.filter(p -> fieldPolygon == null || p != fieldPolygon)
					.toList());
		}
		if (goodZoneIndex >= goodZonePolygons.size()) {
			step = SiembraFertilizadaWorkflowStep.GENERATE_SIEMBRA_FERTILIZADA;
			siembraFertPairIndex = 0;
			return WorkflowStepResult.step(
					SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_GOOD_ZONES,
					step,
					"Siembras por ambiente listas. Siguiente: generar siembra fertilizada.",
					false, false, false);
		}

		Poligono poly = goodZonePolygons.get(goodZoneIndex);
		WorkflowLayerHelper.setOnlyPolygonsEnabled(main, List.of(poly));
		main.poligonoGUIController.doConvertirPoligonosASiembra(
				new SiembraConfigPrefill(request.goodZoneSeed(), request.rowSpacingM()),
				() -> Platform.runLater(ChatWorkflowSession::resumeAfterAsync));
		goodZoneIndex++;

		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.CREATE_SIEMBRA_GOOD_ZONES,
				step,
				"**Convertir a Siembra** — ambiente **" + poly.getNombre() + "**.\n"
						+ "Semilla prellenada: **" + request.goodZoneSeed() + "**.\n"
						+ "Confirmá el diálogo y la dosis objetivo"
						+ (goodZoneIndex < goodZonePolygons.size() ? " (quedan más ambientes)." : "."),
				true, false, true);
	}

	private WorkflowStepResult generateSiembraFertilizada(JFXMain main) {
		List<SiembraLabor> siembras = main.getSiembrasSeleccionadas();
		if (siembras.isEmpty()) {
			siembras = allSiembras(main);
		}
		List<FertilizacionLabor> ferts = main.getFertilizacionesSeleccionadas();
		if (ferts.isEmpty()) {
			ferts = allFertilizaciones(main);
		}

		if (siembraFertPairIndex >= siembras.size()) {
			step = SiembraFertilizadaWorkflowStep.DONE;
			return WorkflowStepResult.done(
					"Flujo de siembra fertilizada completado para " + siembras.size() + " ambiente(s).");
		}

		SiembraLabor siembra = siembras.get(siembraFertPairIndex);
		FertilizacionLabor fert = ferts.isEmpty() ? null : ferts.get(Math.min(siembraFertPairIndex, ferts.size() - 1));

		WorkflowLayerHelper.setLaborEnabled(siembra, true);
		if (fert != null) {
			WorkflowLayerHelper.setLaborEnabled(fert, true);
		}
		WorkflowLayerHelper.refreshLayerPanel(main);

		siembraFertPairIndex++;
		final SiembraFertilizadaWorkflowStep nextStep = siembraFertPairIndex >= siembras.size()
				? SiembraFertilizadaWorkflowStep.DONE
				: SiembraFertilizadaWorkflowStep.GENERATE_SIEMBRA_FERTILIZADA;

		waitingAsync = true;
		main.siembraGUIController.generarSiembraFertilizadaProgrammatic(true,
				() -> onAsyncComplete(nextStep));

		step = nextStep;
		return WorkflowStepResult.step(
				SiembraFertilizadaWorkflowStep.GENERATE_SIEMBRA_FERTILIZADA,
				nextStep,
				"Generando siembra fertilizada para **" + siembra.getNombre() + "**"
						+ (fert != null ? " + **" + fert.getNombre() + "**" : "")
						+ " (fertilización en línea)."
						+ (nextStep == SiembraFertilizadaWorkflowStep.DONE ? " ¡Último ambiente!" : ""),
				true, true, nextStep != SiembraFertilizadaWorkflowStep.DONE);
	}

	private void onAsyncComplete(SiembraFertilizadaWorkflowStep nextStep) {
		Platform.runLater(() -> {
			waitingAsync = false;
			step = nextStep;
			ChatWorkflowSession.resumeAfterAsync();
		});
	}

	private Optional<Poligono> resolvePolygon(List<Poligono> candidates, String hint) {
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}
		return WorkflowPolygonChoiceDialog.choose(candidates, hint);
	}

	@SuppressWarnings("unchecked")
	private static List<FertilizacionLabor> allFertilizaciones(JFXMain main) {
		return (List<FertilizacionLabor>) (List<?>) main.getObjectFromLayersOfClass(FertilizacionLabor.class);
	}

	@SuppressWarnings("unchecked")
	private static List<SiembraLabor> allSiembras(JFXMain main) {
		return (List<SiembraLabor>) (List<?>) main.getObjectFromLayersOfClass(SiembraLabor.class);
	}
}
