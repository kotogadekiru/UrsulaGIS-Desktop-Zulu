package com.ursulagis.desktop.chat.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiembraFertilizadaOrchestratorTest {

	@Test
	@DisplayName("parses yield and campaign from Regalada prompt")
	void parsesRequest() {
		String prompt = """
				siembra fertilizada regalada soja 25/26 4600kg/ha baguette pehuen fosfato monoamonico""";
		SiembraFertilizadaWorkflowRequest req = SiembraFertilizadaWorkflowRequest.parse(prompt);

		assertEquals("regalada", req.fieldName());
		assertEquals(4.6, req.yieldTn(), 0.01);
		assertEquals("soja", req.harvestCrop());
		assertEquals("Fosfato monoamonico", req.fertSourceKey());
	}
}
