package com.ursulagis.desktop.chat;

import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiProvider;
import com.ursulagis.desktop.chat.ai.AiResponse;

/**
 * Generates step-by-step UI guidance when no concrete chat action can be executed.
 */
public final class ChatGuidanceService {

	private ChatGuidanceService() {
	}

	public static String generate(
			AiClient client,
			String userQuery,
			String codeContext,
			MapLayerContext layerContext) {
		if (client.getProvider() == AiProvider.MOCK) {
			return guidanceWithoutAi(userQuery, codeContext, layerContext);
		}
		AiResponse response = client.completePlain(
				buildSystemPrompt(codeContext, layerContext, userQuery), userQuery);
		String text = response.getContent();
		return text == null || text.isBlank()
				? guidanceWithoutAi(userQuery, codeContext, layerContext)
				: text.trim();
	}

	public static String guidanceWithoutAi(String userQuery, String codeContext, MapLayerContext layerContext) {
		return localGuidance(userQuery, codeContext, layerContext);
	}

	static String buildSystemPrompt(String codeContext, MapLayerContext layerContext, String userQuery) {
		StringBuilder sb = new StringBuilder();
		sb.append(UrsulaPersonality.systemPromptPreamble()).append('\n');
		sb.append("The user asked for something that cannot be launched automatically from chat.\n");
		sb.append("Using the GitHub source code and achievement hints below, write numbered step-by-step instructions ");
		sb.append("for the Ursula GIS desktop app. Reference real menu names, controller methods, and UI labels ");
		sb.append("found in the code when possible. Be practical, concise, and encouraging.\n");
		sb.append("Respond in plain text only (no JSON). Use 3-8 numbered steps.\n");
		sb.append(ChatUiKnowledge.marginMapSection()).append("\n\n");
		if (codeContext != null && !codeContext.isBlank()) {
			sb.append("GitHub source context:\n").append(codeContext).append("\n\n");
		}
		sb.append("Relevant achievement hints (onboarding logros):\n");
		sb.append(AchievementIntentCatalog.buildRelevantHintsForQuery(userQuery)).append('\n');
		if (layerContext != null) {
			sb.append('\n').append(layerContext.toPromptSection()).append('\n');
		}
		return sb.toString();
	}

	private static String localGuidance(String userQuery, String codeContext, MapLayerContext layerContext) {
		if (AchievementIntentCatalog.isMarginGenerationQuery(userQuery)) {
			return marginGenerationGuidance(layerContext);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("No encontré una acción automática en el chat, pero te guío paso a paso:\n\n");

		var nearest = AchievementIntentCatalog.findNearest(userQuery);
		if (nearest.isPresent()) {
			String hint = AchievementIntentCatalog.achievementHint(nearest.get().achievementId());
			sb.append("1. ").append(hint.isBlank() ? nearest.get().suggestedReply() : hint).append('\n');
			sb.append("2. Revisá el menú o capa activa según lo que tengas cargado en el mapa.\n");
			sb.append("3. Si necesitás más detalle, elegí el proveedor DeepSeek para que analice el código en GitHub.\n");
		} else {
			sb.append("1. Abrí el menú relacionado con tu tarea (Polígonos, Cosecha, NDVI, Recorrida, etc.).\n");
			sb.append("2. Verificá qué capas tenés activas en el árbol de capas; podés activar o desactivar una rama entera con el checkbox del nodo de la rama.\n");
			sb.append("3. Buscá la acción equivalente en Herramientas o en el menú contextual de la capa.\n");
			sb.append("4. Para ayuda detallada basada en código, usá DeepSeek como proveedor.\n");
		}

		if (layerContext != null && !layerContext.getLayers().isEmpty()) {
			sb.append("\n").append(layerContext.formatLayerList());
		}
		if (codeContext != null && codeContext.contains("---")) {
			sb.append("\n\n(Referencia: código consultado en GitHub ")
					.append(GitHubRepoConfig.OWNER).append("/").append(GitHubRepoConfig.REPO).append(")");
		}
		return sb.toString();
	}

	private static String marginGenerationGuidance(MapLayerContext layerContext) {
		StringBuilder sb = new StringBuilder();
		sb.append("Para generar un mapa de márgenes:\n\n");
		sb.append("1. Activá en el árbol de capas las labores que querés incluir: cosecha, siembra, fertilización y/o pulverización ");
		sb.append("(podés activar una rama entera con el checkbox del nodo de la rama).\n");
		sb.append("2. Andá al menú **Herramientas** y elegí **Rentabilidades**.\n");
		sb.append("3. Configurá el diálogo de margen y confirmá; esperá a que termine el procesamiento.\n");
		sb.append("4. El mapa aparecerá en la rama **Margen** del árbol de capas.\n\n");
		sb.append("(Importar un shapefile es otro camino: menú **Importar** → **Margen**.)");
		if (layerContext != null && !layerContext.getLayers().isEmpty()) {
			sb.append("\n\n").append(layerContext.formatLayerList());
		}
		return sb.toString();
	}
}
