package com.ursulagis.desktop.gui.chat;

import java.util.Optional;

import com.ursulagis.desktop.gui.Messages;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * Prompts the user for a DeepSeek API key when none is configured.
 */
public final class DeepSeekApiKeyDialog extends Dialog<String> {

	private final PasswordField apiKeyField = new PasswordField();

	private DeepSeekApiKeyDialog() {
		setTitle(msg("Chat.apiKeyDialogTitle", "DeepSeek API key"));
		setHeaderText(msg("Chat.apiKeyDialogHeader", "Configure DeepSeek for Ursula IA"));
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().setContent(buildContent());

		Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
		okButton.setDisable(true);
		apiKeyField.textProperty().addListener((obs, old, value) ->
				okButton.setDisable(value == null || value.isBlank()));

		setResultConverter(button -> button == ButtonType.OK ? apiKeyField.getText().trim() : null);
	}

	public static Optional<String> prompt(Window owner) {
		DeepSeekApiKeyDialog dialog = new DeepSeekApiKeyDialog();
		if (owner != null) {
			dialog.initOwner(owner);
		}
		return dialog.showAndWait();
	}

	private GridPane buildContent() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 0, 0, 0));

		Label info = new Label(msg("Chat.apiKeyDialogInfo",
				"No API key was found in ai-keys.properties. Enter your DeepSeek key; it will be saved in the application configuration."));
		info.setWrapText(true);
		info.setMaxWidth(420);

		Label keyLabel = new Label(msg("Chat.apiKeyDialogLabel", "API key:"));
		apiKeyField.setPromptText(msg("Chat.apiKeyDialogPrompt", "sk-..."));
		apiKeyField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(apiKeyField, Priority.ALWAYS);

		grid.add(info, 0, 0, 2, 1);
		grid.add(keyLabel, 0, 1);
		grid.add(apiKeyField, 1, 1);
		return grid;
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
