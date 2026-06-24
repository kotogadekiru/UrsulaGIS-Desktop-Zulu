package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.dao.Poligono;

class SiembraFertilizadaWorkflowGuideTest {

	@Test
	@DisplayName("complex siembra fertilizada prompt is detected")
	void matchesComplexPrompt() {
		String prompt = """
				vamos a crear una siembra fertilizada para la regalada con baguete en las lomas
				y pehuen en las zonas buenas reponiendo fosforo segun extraccion de la soja 25/26
				rindio promedio 4600kg/ha usa la imagen ndvi de mayor valor promedio""";
		assertTrue(SiembraFertilizadaWorkflowGuide.matches(prompt));
	}

	@Test
	@DisplayName("4600 kg/ha converts to 4.6 t/ha for harvest dialog")
	void yieldConversion() {
		assertEquals(4.6, SiembraFertilizadaWorkflowGuide.extractYieldTn("rindio 4600kg/ha"), 0.01);
	}

	@Test
	@DisplayName("guidance mentions Rentabilidades steps and polygon matching")
	void guidanceIncludesWorkflowSteps() {
		MapLayerContext ctx = new MapLayerContext(List.of(
				new LoadedLayerInfo("Regalada Norte", "Poligono", true, new Poligono()),
				new LoadedLayerInfo("Regalada Lomas", "Poligono", false, new Poligono())), null);

		String guidance = SiembraFertilizadaWorkflowGuide.buildGuidance(
				"siembra fertilizada regalada lomas baguette pehuen ndvi fosforo 4600kg/ha", ctx);

		assertTrue(guidance.contains("Regalada"));
		assertTrue(guidance.contains("Convertir NDVI a Cosecha"));
		assertTrue(guidance.contains("Recomendar Fert. P"));
		assertTrue(guidance.contains("4,6"));
		assertTrue(guidance.contains("Generar Siembra Fertilizada"));
	}
}
