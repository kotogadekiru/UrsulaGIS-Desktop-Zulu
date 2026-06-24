package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;

class LaborTargetResolverTest {

	@Test
	@DisplayName("sanitizes generic cosecha activa target")
	void sanitizesGenericTarget() {
		assertEquals(null, LaborTargetResolver.sanitizeTargetName("cosecha activa"));
		assertEquals(null, LaborTargetResolver.sanitizeTargetName("la cosecha activa"));
	}

	@Test
	@DisplayName("resolves single active cosecha when user says cosecha activa")
	void resolvesSingleActiveCosecha() {
		CosechaLabor cosecha = new CosechaLabor();
		cosecha.setNombre("Regalada SP 2526 con huecos 24-02-2026");
		FertilizacionLabor fert = new FertilizacionLabor();
		fert.setNombre("Regalada SP 2526 con huecos 24-02-2026 Prescripción P");

		MapLayerContext ctx = new MapLayerContext(List.of(
				new LoadedLayerInfo(cosecha.getNombre(), "CosechaLabor", true, cosecha),
				new LoadedLayerInfo(fert.getNombre(), "FertilizacionLabor", false, fert)), null);

		Optional<com.ursulagis.desktop.dao.Labor<?>> resolved = LaborTargetResolver.resolve(
				ctx, "cosecha activa", false);

		assertTrue(resolved.isPresent());
		assertEquals(cosecha, resolved.get());
	}

	@Test
	@DisplayName("resolves by partial field name preferring active layer")
	void resolvesPartialNamePreferringActive() {
		CosechaLabor cosecha = new CosechaLabor();
		cosecha.setNombre("Regalada SP 2526 con huecos 24-02-2026");
		FertilizacionLabor fert = new FertilizacionLabor();
		fert.setNombre("Regalada SP 2526 con huecos 24-02-2026 Prescripción P");

		MapLayerContext ctx = new MapLayerContext(List.of(
				new LoadedLayerInfo(cosecha.getNombre(), "CosechaLabor", true, cosecha),
				new LoadedLayerInfo(fert.getNombre(), "FertilizacionLabor", false, fert)), null);

		Optional<com.ursulagis.desktop.dao.Labor<?>> resolved = LaborTargetResolver.resolve(
				ctx, "Regalada SP 2526 con huecos", false);

		assertTrue(resolved.isPresent());
		assertEquals(cosecha, resolved.get());
	}
}
