package com.ursulagis.desktop.chat;

/**
 * Static knowledge snippets about Ursula GIS UI behaviors that the chat model
 * (and offline guidance) should follow — layer tree, margin workflows, and
 * fertilized-seeding steps — injected into system prompts.
 */
public final class ChatUiKnowledge {

	/** Prevents instantiation. */
	private ChatUiKnowledge() {
	}

	/**
	 * How the layer tree branch checkboxes work, plus common action pitfalls
	 * (activate polygons by area vs create; siembra share vs margin).
	 */
	public static String layerPanelSection() {
		return """
				Layer panel (árbol de capas):
				- Las capas se agrupan en ramas por tipo (Cosechas, Polígonos, NDVI, Recorrida, etc.).
				- Hacer clic en el checkbox del nodo de una rama activa o desactiva TODAS las capas hijas de esa rama a la vez.
				- El checkbox de una capa individual solo cambia esa capa.
				- Las capas habilitadas (marcadas) son las activas en el mapa; las deshabilitadas quedan ocultas.
				- Clic derecho en un nodo de rama muestra acciones que aplican a las capas hijas seleccionadas/activas.
				When the user asks to enable/disable/show/hide all layers of a type, tell them to use the branch checkbox in the layer tree.
				When the user asks to activate polygons with area/superficie greater than zero, use action ACTIVAR_POLIGONOS_SUPERFICIE (not CREAR_POLIGONO).
				When the user asks to load/import and share a seeding map (siembra), use IMPORT_SIEMBRA and/or COMPARTIR_SIEMBRA — not GENERAR_MARGEN/Rentabilidades.
				The chat may include excerpts from official PDF manuals and video-tutorial transcripts (.txt) in docs/; transcript filenames describe the workflow (e.g. importar_cosecha.txt).
				""";
	}

	/**
	 * Distinguishes generating a margin map (Herramientas → Rentabilidades)
	 * from importing a margin shapefile so the model does not mix the two.
	 */
	public static String marginMapSection() {
		return """
				Margin map (mapa de márgenes) — two different workflows:
				- GENERATE (generar/crear/calcular): activate the needed labor layers in the tree (cosecha, siembra, fertilización, pulverización), then menu Herramientas → Rentabilidades. Configure the dialog and wait for processing. This is the primary answer when the user asks how to generate or create a margin map.
				- IMPORT (importar/abrir shapefile): menu Importar → Margen, select a shapefile, confirm the configuration dialog.
				- EDIT existing margin: right-click a margin layer → Editar margen.
				- SUM multiple margins: select two or more margin maps, right-click the Margen branch root → Sumar seleccionados.
				When the user says generar, crear or calcular a margin map, do NOT answer with import steps first.
				""";
	}

	/**
	 * Ordered overview of the multi-step “siembra fertilizada por ambientes” flow
	 * used when the chat cannot run the full orchestration automatically.
	 */
	public static String siembraFertilizadaWorkflowSection() {
		return """
				Fertilized seeding workflow (siembra fertilizada por ambientes):
				1. Select polygon by name (e.g. Regalada) — match loaded polygons or offer closest names.
				2. Download NDVI for the soybean campaign (e.g. 25/26): Polígonos → Obtener NDVI or Herramientas → bulk NDVI.
				3. Convert the NDVI layer with the highest mean NDVI to harvest: right-click NDVI → Convertir NDVI a Cosecha; crop soja; yield in t/ha (4600 kg/ha = 4.6 t/ha).
				4. On that harvest: Recomendar Fert. P Reposición; choose monoammonium phosphate (fosfato monoamónico) as P source.
				5. Convert lomas polygon to seeding: wheat, row spacing 0.19 m, seed Baguette 620 2627.
				6. Convert other environment polygons (non-lomas): seed Pehuen (confirm variety).
				7. For each seeding + P fertilization pair: Siembras → Generar Siembra Fertilizada (confirm in-line fert).
				Ask user to confirm polygon, seed variety, or fert source when ambiguous.
				""";
	}
}
