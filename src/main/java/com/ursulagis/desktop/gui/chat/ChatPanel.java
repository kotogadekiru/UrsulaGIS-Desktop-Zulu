package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.gui.Messages;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Chat UI shown in {@link UrsulaChatWindow}.
 */
public class ChatPanel extends VBox {

	private static final String HISTORY_BG = "#f5f6f8";
	private static final String BOT_BUBBLE_BG = "#eef1f6";
	private static final String BOT_BUBBLE_BORDER = "#d4dae3";
	private static final String USER_BUBBLE_BG = "#e3f2e8";
	private static final String USER_BUBBLE_BORDER = "#b9d4c4";
	private static final String ERROR_BUBBLE_BG = "#fdecea";
	private static final String ERROR_BUBBLE_BORDER = "#f5c6c2";
	private static final String DEFAULT_MESSAGE_TEXT = "#2b2b2b";
	private static final String USER_MESSAGE_TEXT = "#1f4d32";
	private static final String ERROR_MESSAGE_TEXT = "#7f1d1d";
	private static final String BOT_MESSAGE_TEXT = "#2a3441";

	private final VBox historyBox = new VBox(8);
	private final ScrollPane historyScroll = new ScrollPane(historyBox);
	private final TextArea inputArea = new TextArea();
	//private final Button sendButton = new Button();
	private final Label statusLabel = new Label();
	private final CheckBox dontShowAtStartCheckBox = new CheckBox();

	public ChatPanel() {
		super(8);
		setPadding(new Insets(12));

		//Label title = new Label(UrsulaPersonality.roleName());
		//title.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");

		historyBox.setFillWidth(true);
		historyBox.setPadding(new Insets(4));

		historyScroll.setFitToWidth(true);
		historyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		historyScroll.setStyle("-fx-background-color: " + HISTORY_BG + "; -fx-background: " + HISTORY_BG + ";");
		historyScroll.setMinHeight(180);
		VBox.setVgrow(historyScroll, Priority.ALWAYS);

		inputArea.setWrapText(true);
		inputArea.setPrefRowCount(2);
		inputArea.setPromptText(msg("Chat.inputPrompt", "Type a command, e.g. import harvest map"));
		inputArea.setStyle(
				"-fx-control-inner-background: white; "
						+ "-fx-background-color: white; "
						+ "-fx-text-box-border: #c8c8c8; "
						+ "-fx-text-fill: " + DEFAULT_MESSAGE_TEXT + ";");

		statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

		dontShowAtStartCheckBox.setText(msg("Chat.dontShowAtStart", "Don't show at startup"));
		dontShowAtStartCheckBox.setSelected(!ChatPreferences.getInstance().isShowAtStart());

		getChildren().addAll(
				//title,
				historyScroll,
				inputArea,
				statusLabel,
				new Separator(),
				dontShowAtStartCheckBox);
	}

	public TextArea getInputArea() {
		return inputArea;
	}

	//	public Button getSendButton() {
	//		return sendButton;
	//	}

	public Label getStatusLabel() {
		return statusLabel;
	}

	public CheckBox getDontShowAtStartCheckBox() {
		return dontShowAtStartCheckBox;
	}

	public void appendMessage(String role, String text) {
		VBox bubble = buildMessageBubble(role, text);
		historyBox.getChildren().add(bubble);
		scrollHistoryToBottom();
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

	private VBox buildMessageBubble(String role, String text) {
		Label roleLabel = new Label(role);
		roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");

		TextArea body = createSelectableBody(role, text);

		VBox bubble = new VBox(4, roleLabel, body);
		bubble.setPadding(new Insets(8, 10, 8, 10));
		bubble.setMaxWidth(Double.MAX_VALUE);
		applyBubbleStyle(bubble, roleLabel, role);
		return bubble;
	}

	private TextArea createSelectableBody(String role, String text) {
		TextArea area = new TextArea(text);
		area.setEditable(false);
		area.setWrapText(true);
		area.setPadding(Insets.EMPTY);
		int lines = Math.max(1, text.split("\n", -1).length);
		int wrappedLines = Math.max(lines, (int) Math.ceil(text.length() / 52.0));
		area.setPrefRowCount(Math.min(wrappedLines, 24));
		area.setMaxWidth(Double.MAX_VALUE);
		String textColor = resolveMessageTextColor(role);
		area.setStyle(selectableBodyStyle(textColor));
		applyTextAreaTextColor(area, textColor);
		return area;
	}

	private void applyTextAreaTextColor(TextArea area, String textColor) {
		Runnable apply = () -> {
			Node text = area.lookup(".text");
			if (text != null) {
				text.setStyle("-fx-fill: " + textColor + ";");
			}
		};
		if (area.getScene() != null) {
			Platform.runLater(apply);
		} else {
			area.sceneProperty().addListener((obs, oldScene, newScene) -> {
				if (newScene != null) {
					Platform.runLater(apply);
					Platform.runLater(this::scrollHistoryToBottom);
				}
			});
		}
	}

	private String resolveMessageTextColor(String role) {
		String userRole = msg("Chat.roleUser", "You");
		String errorRole = msg("Chat.roleError", "Error");
		if (role.equals(userRole) || "Tú".equals(role) || "You".equals(role)) {
			return USER_MESSAGE_TEXT;
		}
		if (role.equals(errorRole) || "Error".equals(role)) {
			return ERROR_MESSAGE_TEXT;
		}
		return BOT_MESSAGE_TEXT;
	}

	private static String selectableBodyStyle(String textColor) {
		return "-fx-control-inner-background: transparent; "
				+ "-fx-background-color: transparent; "
				+ "-fx-text-box-border: transparent; "
				+ "-fx-focus-color: transparent; "
				+ "-fx-faint-focus-color: transparent; "
				+ "-fx-highlight-fill: #b4d5fe; "
				+ "-fx-highlight-text-fill: " + DEFAULT_MESSAGE_TEXT + "; "
				+ "-fx-text-fill: " + textColor + "; "
				+ "-fx-font-size: 0.95em;";
	}

	private void applyBubbleStyle(VBox bubble, Label roleLabel, String role) {
		String userRole = msg("Chat.roleUser", "You");
		String errorRole = msg("Chat.roleError", "Error");
		if (role.equals(userRole) || "Tú".equals(role) || "You".equals(role)) {
			bubble.setStyle(bubbleStyle(USER_BUBBLE_BG, USER_BUBBLE_BORDER));
			roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em; -fx-text-fill: #2e6b45;");
			return;
		}
		if (role.equals(errorRole) || "Error".equals(role)) {
			bubble.setStyle(bubbleStyle(ERROR_BUBBLE_BG, ERROR_BUBBLE_BORDER));
			roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em; -fx-text-fill: #b71c1c;");
			return;
		}
		bubble.setStyle(bubbleStyle(BOT_BUBBLE_BG, BOT_BUBBLE_BORDER));
		roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em; -fx-text-fill: #3d5a80;");
	}

	private static String bubbleStyle(String background, String border) {
		return "-fx-background-color: " + background + "; "
				+ "-fx-background-radius: 8; "
				+ "-fx-border-color: " + border + "; "
				+ "-fx-border-radius: 8; "
				+ "-fx-border-width: 1;";
	}

	private void scrollHistoryToBottom() {
		Runnable scroll = () -> {
			historyScroll.applyCss();
			historyBox.applyCss();
			historyScroll.layout();
			historyScroll.setVvalue(1.0);
		};
		if (Platform.isFxApplicationThread()) {
			scroll.run();
			Platform.runLater(scroll);
		} else {
			Platform.runLater(() -> {
				scroll.run();
				Platform.runLater(scroll);
			});
		}
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
