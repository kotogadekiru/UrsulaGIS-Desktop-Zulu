package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.AchievementIntentCatalog;
import com.ursulagis.desktop.chat.AchievementIntentMatch;
import com.ursulagis.desktop.chat.ActionExecutionResult;
import com.ursulagis.desktop.chat.ChatActionExecutor;
import com.ursulagis.desktop.chat.ChatGuidanceService;
import com.ursulagis.desktop.chat.GitHubCodeContextBuilder;
import com.ursulagis.desktop.chat.IntentParser;
import com.ursulagis.desktop.chat.UrsulaAction;
import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.MapLayerContextBuilder;
import com.ursulagis.desktop.chat.ParsedIntent;
import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiClientFactory;
import com.ursulagis.desktop.chat.ai.AiResponse;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

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
		panel.appendMessage(msg("Chat.roleUser", "You"), text.trim());
		panel.clearInput();
	//	panel.getSendButton().setDisable(true);
		panel.setStatus(msg("Chat.statusThinking", "Thinking..."));

		MapLayerContext layerContext = MapLayerContextBuilder.from(main);

		Task<ParsedIntentResult> task = new Task<>() {
			@Override
			protected ParsedIntentResult call() {
				AiClient client = AiClientFactory.createConfigured();
				String codeContext = GitHubCodeContextBuilder.buildForQuery(text.trim());
				IntentParser parser = new IntentParser(client, layerContext, codeContext);
				ParsedIntent intent = parser.parse(text.trim());
				if (intent.getAction() == UrsulaAction.UNKNOWN) {
					var catalogMatch = AchievementIntentCatalog.match(text.trim());
					if (catalogMatch.isPresent()) {
						AchievementIntentMatch m = catalogMatch.get();
						intent = new ParsedIntent(m.action(), intent.getTargetName(), m.score(), m.suggestedReply());
					}
				}
				String guidance = null;
				if (intent.getAction() == UrsulaAction.UNKNOWN) {
					guidance = ChatGuidanceService.generate(client, text.trim(), codeContext, layerContext);
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
