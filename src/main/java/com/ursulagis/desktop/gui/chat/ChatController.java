package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.AchievementIntentCatalog;
import com.ursulagis.desktop.chat.AchievementIntentMatch;
import com.ursulagis.desktop.chat.ActionExecutionResult;
import com.ursulagis.desktop.chat.ChatActionExecutor;
import com.ursulagis.desktop.chat.ChatGuidanceService;
import com.ursulagis.desktop.chat.GitHubCodeContextBuilder;
import com.ursulagis.desktop.chat.IntentParser;
import com.ursulagis.desktop.chat.ManualContextBuilder;
import com.ursulagis.desktop.chat.UrsulaAction;
import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.chat.LaborTargetResolver;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.MapLayerContextBuilder;
import com.ursulagis.desktop.chat.ParsedIntent;
import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiClientFactory;
import com.ursulagis.desktop.chat.ai.AiApiKeys;
import com.ursulagis.desktop.chat.ai.AiResponse;
import com.ursulagis.desktop.chat.workflow.ChatWorkflowSession;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

import java.util.Optional;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;

/**
 * Handles user messages: AI intent parsing on a background thread, action execution on FX thread.
 */
public class ChatController {

	private final JFXMain main;
	private final ChatPanel panel;
	private final ChatActionExecutor executor;

	public ChatController(JFXMain main, ChatPanel panel) {
		this.main = main;
		this.panel = panel;
		this.executor = new ChatActionExecutor(main);
		ChatWorkflowSession.bind(main, msg -> Platform.runLater(() -> {
			panel.appendMessage(UrsulaPersonality.roleName(), msg);
			panel.setStatus(msg("Chat.statusReady", "Ready"));
		}));
		wireUi();
		showWelcome();
	}

	private void showWelcome() {
		panel.appendMessage(UrsulaPersonality.roleName(), UrsulaPersonality.greeting());
	}

	private void wireUi() {
	//	panel.getSendButton().setOnAction(e -> sendMessage());
		panel.getInputArea().setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER && !e.isControlDown()) {
				sendMessage();
				e.consume();
			}
		});
		panel.getDontShowAtStartCheckBox().selectedProperty().addListener((obs, old, selected) ->
				ChatPreferences.getInstance().setShowAtStart(!selected));
	}

	public void sendMessage() {
		String text = panel.getInputText();
		if (text == null || text.isBlank()) {
			return;
		}
		if (DeepSeekApiKeyHelper.isDeepSeekProvider()
				&& !AiApiKeys.hasDeepSeekKey()
				&& !DeepSeekApiKeyHelper.ensureConfigured(JFXMain.stage)) {
			panel.appendMessage(UrsulaPersonality.roleName(),
					msg("Chat.apiKeyRequired", "Necesito la API key de DeepSeek para continuar."));
			panel.setStatus(msg("Chat.statusReady", "Ready"));
			return;
		}

		panel.appendMessage(msg("Chat.roleUser", "You"), text.trim());
		panel.clearInput();
	//	panel.getSendButton().setDisable(true);
		panel.setStatus(msg("Chat.statusThinking", "Thinking..."));

		MapLayerContext layerContext = MapLayerContextBuilder.from(main);

		var workflowResult = ChatWorkflowSession.handle(
				main, text.trim(), layerContext);
		if (workflowResult != null) {
			panel.appendMessage(UrsulaPersonality.roleName(), workflowResult.message());
			panel.setStatus(msg("Chat.statusReady", "Ready"));
			if (workflowResult.launched()) {
				UrsulaChatWindow.yieldToMainStage();
			}
			return;
		}

		if (tryExecuteLocalIntent(text.trim(), layerContext)) {
			return;
		}

		Task<ParsedIntentResult> task = new Task<>() {
			@Override
			protected ParsedIntentResult call() {
				AiClient client = AiClientFactory.createConfigured();
				String codeContext = GitHubCodeContextBuilder.buildForQuery(text.trim());
				String manualContext = ManualContextBuilder.buildForQuery(text.trim());
				IntentParser parser = new IntentParser(client, layerContext, codeContext, manualContext);
				ParsedIntent intent = parser.parse(text.trim());
				if (intent.getAction() == UrsulaAction.GENERAR_MARGEN
						&& AchievementIntentCatalog.isSiembraShareOrImportQuery(text.trim())) {
					var override = AchievementIntentCatalog.match(text.trim());
					if (override.isPresent()
							&& (override.get().action() == UrsulaAction.COMPARTIR_SIEMBRA
									|| override.get().action() == UrsulaAction.IMPORT_SIEMBRA)) {
						AchievementIntentMatch m = override.get();
						intent = new ParsedIntent(m.action(), null, m.score(), m.suggestedReply());
					}
				}
				if (intent.getAction() == UrsulaAction.CREAR_POLIGONO
						&& AchievementIntentCatalog.isActivatePolygonsWithAreaQuery(text.trim())) {
					var override = AchievementIntentCatalog.match(text.trim());
					if (override.isPresent()
							&& override.get().action() == UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
						AchievementIntentMatch m = override.get();
						intent = new ParsedIntent(m.action(), null, m.score(), m.suggestedReply());
					}
				}
				if (intent.getAction() == UrsulaAction.UNKNOWN) {
					var catalogMatch = AchievementIntentCatalog.match(text.trim());
					if (catalogMatch.isPresent()) {
						AchievementIntentMatch m = catalogMatch.get();
						String target = LaborTargetResolver.sanitizeTargetName(intent.getTargetName());
						intent = new ParsedIntent(m.action(), target, m.score(), m.suggestedReply());
					}
				}
				String guidance = null;
				if (intent.getAction() == UrsulaAction.UNKNOWN) {
					guidance = ChatGuidanceService.generate(client, text.trim(), codeContext, manualContext, layerContext);
				}
				return new ParsedIntentResult(intent, parser.getLastResponse(), layerContext, guidance);
			}
		};

		task.setOnSucceeded(e -> {
			ParsedIntentResult parsed = task.getValue();
			Platform.runLater(() -> {
				if (parsed.guidance() != null) {
					panel.appendMessage(UrsulaPersonality.roleName(), parsed.guidance());
					panel.setStatus(msg("Chat.statusReady", "Ready"));
					return;
				}
				ActionExecutionResult result = executor.execute(parsed.intent(), parsed.layerContext());
				panel.appendMessage(UrsulaPersonality.roleName(),
						formatReply(parsed.intent(), result.message()));
				panel.setStatus(msg("Chat.statusReady", "Ready"));
			//	panel.getSendButton().setDisable(false);
				if (result.launched()) {
					UrsulaChatWindow.yieldToMainStage();
				}
			});
		});
		task.setOnFailed(e -> {
			Throwable ex = task.getException();
			panel.appendMessage(msg("Chat.roleError", "Error"), ex != null ? ex.getMessage() : msg("Chat.unknownError", "Unknown error"));
			panel.setStatus(msg("Chat.statusError", "Error"));
			//panel.getSendButton().setDisable(false);
		});

		Thread t = new Thread(task, "ursula-chat");
		t.setDaemon(true);
		t.start();
	}

	private boolean tryExecuteLocalIntent(String userText, MapLayerContext layerContext) {
		if (tryActivatePolygons(userText, layerContext)) {
			return true;
		}
		return trySiembraImportShare(userText, layerContext);
	}

	private boolean tryActivatePolygons(String userText, MapLayerContext layerContext) {
		if (!AchievementIntentCatalog.isActivatePolygonsWithAreaQuery(userText)) {
			return false;
		}
		Optional<AchievementIntentMatch> match = AchievementIntentCatalog.match(userText);
		if (match.isEmpty() || match.get().action() != UrsulaAction.ACTIVAR_POLIGONOS_SUPERFICIE) {
			return false;
		}
		AchievementIntentMatch m = match.get();
		ParsedIntent intent = new ParsedIntent(m.action(), null, m.score(), m.suggestedReply());
		ActionExecutionResult result = executor.execute(intent, layerContext);
		panel.appendMessage(UrsulaPersonality.roleName(), formatReply(intent, result.message()));
		panel.setStatus(msg("Chat.statusReady", "Ready"));
		if (result.launched()) {
			UrsulaChatWindow.yieldToMainStage();
		}
		return true;
	}

	private boolean trySiembraImportShare(String userText, MapLayerContext layerContext) {
		if (!AchievementIntentCatalog.isSiembraShareOrImportQuery(userText)) {
			return false;
		}
		String n = AchievementIntentCatalog.normalize(userText);
		boolean share = n.contains("compartir");
		boolean load = n.contains("cargar") || n.contains("importar") || n.contains("abrir");

		ActionExecutionResult result;
		ParsedIntent intent;
		if (share && load) {
			result = executor.importYCompartirSiembra(layerContext);
			intent = new ParsedIntent(UrsulaAction.IMPORT_SIEMBRA, null, 1.0,
					"¡Dale! Importo la siembra y al terminar la comparto.");
		} else if (share) {
			Optional<AchievementIntentMatch> match = AchievementIntentCatalog.match(userText);
			if (match.isEmpty() || match.get().action() != UrsulaAction.COMPARTIR_SIEMBRA) {
				return false;
			}
			intent = new ParsedIntent(match.get().action(), null, match.get().score(), match.get().suggestedReply());
			result = executor.execute(intent, layerContext);
		} else if (load) {
			Optional<AchievementIntentMatch> match = AchievementIntentCatalog.match(userText);
			if (match.isEmpty() || match.get().action() != UrsulaAction.IMPORT_SIEMBRA) {
				return false;
			}
			intent = new ParsedIntent(match.get().action(), null, match.get().score(), match.get().suggestedReply());
			result = executor.execute(intent, layerContext);
		} else {
			return false;
		}

		panel.appendMessage(UrsulaPersonality.roleName(), formatReply(intent, result.message()));
		panel.setStatus(msg("Chat.statusReady", "Ready"));
		if (result.launched()) {
			UrsulaChatWindow.yieldToMainStage();
		}
		return true;
	}

	private static String formatReply(ParsedIntent intent, String executionResult) {
		String voice = intent.getMessage();
		if (voice != null && !voice.isBlank()) {
			if (executionResult != null && !executionResult.isBlank() && !executionResult.equals(voice)) {
				return voice + "\n" + executionResult;
			}
			return voice;
		}
		return executionResult != null ? executionResult : "";
	}

	private record ParsedIntentResult(
			ParsedIntent intent,
			AiResponse response,
			MapLayerContext layerContext,
			String guidance) {
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
