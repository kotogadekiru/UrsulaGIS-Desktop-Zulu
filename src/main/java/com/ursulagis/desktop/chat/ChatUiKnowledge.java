package com.ursulagis.desktop.chat;

/**
 * Existing Ursula GIS UI behaviors the chat model should know about.
 */
public final class ChatUiKnowledge {

	private ChatUiKnowledge() {
	}

	public static String layerPanelSection() {
		return """
				Layer panel (árbol de capas):
				- Las capas se agrupan en ramas por tipo (Cosechas, Polígonos, NDVI, Recorrida, etc.).
				- Hacer clic en el checkbox del nodo de una rama activa o desactiva TODAS las capas hijas de esa rama a la vez.
				- El checkbox de una capa individual solo cambia esa capa.
				- Las capas habilitadas (marcadas) son las activas en el mapa; las deshabilitadas quedan ocultas.
				- Clic derecho en un nodo de rama muestra acciones que aplican a las capas hijas seleccionadas/activas.
				When the user asks to enable/disable/show/hide all layers of a type, tell them to use the branch checkbox in the layer tree.
				""";
	}

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
}
