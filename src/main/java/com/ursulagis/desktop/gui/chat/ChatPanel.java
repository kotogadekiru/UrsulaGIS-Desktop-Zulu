package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.gui.Messages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Chat UI shown in {@link UrsulaChatWindow}.
 */
public class ChatPanel extends VBox {

	private final TextArea historyArea = new TextArea();
	private final TextArea inputArea = new TextArea();
	private final ComboBox<String> providerCombo = new ComboBox<>();
	private final Button sendButton = new Button();
	private final Label statusLabel = new Label();
	private final CheckBox dontShowAtStartCheckBox = new CheckBox();

	public ChatPanel() {
		super(8);
		setPadding(new Insets(12));

		Label title = new Label(UrsulaPersonality.roleName());
		title.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");

		historyArea.setEditable(false);
		historyArea.setWrapText(true);
		historyArea.setPrefRowCount(12);
		historyArea.setPromptText(msg("Chat.historyPrompt", "Conversation..."));

		inputArea.setWrapText(true);
		inputArea.setPrefRowCount(3);
		inputArea.setPromptText(msg("Chat.inputPrompt", "Type a command, e.g. import harvest map"));

		ScrollPane historyScroll = new ScrollPane(historyArea);
		historyScroll.setFitToWidth(true);
		VBox.setVgrow(historyScroll, Priority.ALWAYS);

		providerCombo.getItems().addAll(
				msg("Chat.providerMock", "Mock (local)"),
				msg("Chat.providerOpenAi", "ChatGPT (mock)"),
				msg("Chat.providerClaude", "Claude (mock)"));
		providerCombo.getSelectionModel().selectFirst();
		providerCombo.setMaxWidth(Double.MAX_VALUE);

		sendButton.setText(msg("Chat.send", "Send"));

		HBox actions = new HBox(8, providerCombo, sendButton);
		actions.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(providerCombo, Priority.ALWAYS);

		statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

		dontShowAtStartCheckBox.setText(msg("Chat.dontShowAtStart", "Don't show at startup"));
		dontShowAtStartCheckBox.setSelected(!ChatPreferences.getInstance().isShowAtStart());

		getChildren().addAll(
				title,
				historyScroll,
				inputArea,
				actions,
				statusLabel,
				new Separator(),
				dontShowAtStartCheckBox);
	}

	public TextArea getHistoryArea() {
		return historyArea;
	}

	public TextArea getInputArea() {
		return inputArea;
	}

	public ComboBox<String> getProviderCombo() {
		return providerCombo;
	}

	public Button getSendButton() {
		return sendButton;
	}

	public Label getStatusLabel() {
		return statusLabel;
	}

	public CheckBox getDontShowAtStartCheckBox() {
		return dontShowAtStartCheckBox;
	}

	public void appendMessage(String role, String text) {
		historyArea.appendText(role + ": " + text + "\n\n");
	}

	public void clearInput() {
		inputArea.clear();
	}

	public String getInputText() {
		return inputArea.getText();
	}

	public void setStatus(String text) {
		statusLabel.setText(text);
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
