package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.ChatActionExecutor;
import com.ursulagis.desktop.chat.IntentParser;
import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.chat.MapLayerContext;
import com.ursulagis.desktop.chat.MapLayerContextBuilder;
import com.ursulagis.desktop.chat.ParsedIntent;
import com.ursulagis.desktop.chat.ai.AiClient;
import com.ursulagis.desktop.chat.ai.AiClientFactory;
import com.ursulagis.desktop.chat.ai.AiProvider;
import com.ursulagis.desktop.chat.ai.AiResponse;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Handles user messages: AI intent parsing on a background thread, action execution on FX thread.
 */
public class ChatController {

	private final JFXMain main;
	private final ChatPanel panel;
	private final ChatActionExecutor executor;
	private AiProvider currentProvider = AiProvider.MOCK;

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
		panel.getSendButton().setOnAction(e -> sendMessage());
		panel.getInputArea().setOnKeyPressed(e -> {
			if (e.isControlDown() && e.getCode().toString().equals("ENTER")) {
				sendMessage();
				e.consume();
			}
		});
		panel.getProviderCombo().getSelectionModel().selectedItemProperty().addListener((obs, old, nu) -> {
			currentProvider = mapProvider(nu);
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
		panel.getSendButton().setDisable(true);
		panel.setStatus(msg("Chat.statusThinking", "Thinking..."));

		MapLayerContext layerContext = MapLayerContextBuilder.from(main);

		Task<ParsedIntentResult> task = new Task<>() {
			@Override
			protected ParsedIntentResult call() {
				AiClient client = AiClientFactory.create(currentProvider);
				IntentParser parser = new IntentParser(client, layerContext);
				ParsedIntent intent = parser.parse(text.trim());
				return new ParsedIntentResult(intent, parser.getLastResponse(), layerContext);
			}
		};

		task.setOnSucceeded(e -> {
			ParsedIntentResult parsed = task.getValue();
			Platform.runLater(() -> {
				String result = executor.execute(parsed.intent(), parsed.layerContext());
				panel.appendMessage(UrsulaPersonality.roleName(), formatReply(parsed.response(), parsed.intent(), result));
				panel.setStatus(msg("Chat.statusReady", "Ready"));
				panel.getSendButton().setDisable(false);
			});
		});
		task.setOnFailed(e -> {
			Throwable ex = task.getException();
			panel.appendMessage(msg("Chat.roleError", "Error"), ex != null ? ex.getMessage() : msg("Chat.unknownError", "Unknown error"));
			panel.setStatus(msg("Chat.statusError", "Error"));
			panel.getSendButton().setDisable(false);
		});

		Thread t = new Thread(task, "ursula-chat");
		t.setDaemon(true);
		t.start();
	}

	private static String formatReply(AiResponse aiResponse, ParsedIntent intent, String executionResult) {
		String voice = intent.getMessage();
		String body;
		if (voice != null && !voice.isBlank()) {
			if (executionResult != null && !executionResult.isBlank() && !executionResult.equals(voice)) {
				body = voice + "\n" + executionResult;
			} else {
				body = voice;
			}
		} else {
			body = executionResult != null ? executionResult : "";
		}
		return body + "\n[acción: " + intent.getAction().name()
				+ ", confianza: " + String.format("%.0f%%", intent.getConfidence() * 100)
				+ ", " + aiResponse.getProvider().getDisplayName()
				+ ", " + aiResponse.getLatencyMs() + "ms]";
	}

	private record ParsedIntentResult(ParsedIntent intent, AiResponse response, MapLayerContext layerContext) {
	}

	private static AiProvider mapProvider(String label) {
		if (label == null) {
			return AiProvider.MOCK;
		}
		String mockLabel = msg("Chat.providerMock", "Mock (local)");
		String openAiLabel = msg("Chat.providerOpenAi", "ChatGPT (mock)");
		String claudeLabel = msg("Chat.providerClaude", "Claude (mock)");
		if (label.equals(openAiLabel) || label.startsWith("ChatGPT")) {
			return AiProvider.OPENAI;
		}
		if (label.equals(claudeLabel) || label.startsWith("Claude")) {
			return AiProvider.CLAUDE;
		}
		if (label.equals(mockLabel) || label.startsWith("Mock")) {
			return AiProvider.MOCK;
		}
		return AiProvider.MOCK;
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
