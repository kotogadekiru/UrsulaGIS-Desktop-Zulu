package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Separate window for the Ursula assistant. Can be opened at startup or from the configuration menu.
 */
public final class UrsulaChatWindow {

	private static Stage chatStage;

	private UrsulaChatWindow() {
	}

	public static void show(JFXMain main) {
		Platform.runLater(() -> showOnFxThread(main));
	}

	private static void showOnFxThread(JFXMain main) {
		if (chatStage != null && chatStage.isShowing()) {
			chatStage.toFront();
			chatStage.requestFocus();
			return;
		}

		Stage owner = JFXMain.stage;
		ChatPanel panel = new ChatPanel();
		new ChatController(main, panel);

		chatStage = new Stage();
		if (owner != null) {
			chatStage.initOwner(owner);
			chatStage.initModality(Modality.NONE);
		}
		chatStage.setTitle(UrsulaPersonality.roleName());
		if (owner != null && !owner.getIcons().isEmpty()) {
			chatStage.getIcons().addAll(owner.getIcons());
		}
		Scene scene = new Scene(panel, 520, 420);
		chatStage.setScene(scene);
		chatStage.setMinWidth(400);
		chatStage.setMinHeight(320);
		chatStage.setOnHidden(e -> chatStage = null);
		chatStage.show();
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
