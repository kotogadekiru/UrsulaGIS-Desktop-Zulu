package com.ursulagis.desktop.gui.chat;

import com.ursulagis.desktop.chat.UrsulaPersonality;
import com.ursulagis.desktop.chat.ai.AiApiKeys;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
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
		if (DeepSeekApiKeyHelper.isDeepSeekProvider()
				&& !AiApiKeys.hasDeepSeekKey()
				&& !DeepSeekApiKeyHelper.ensureConfigured(owner)) {
			panel.appendMessage(UrsulaPersonality.roleName(),
					msg("Chat.apiKeyRequired", "Necesito la API key de DeepSeek para continuar."));
		}
		chatStage.show();
	}

	/**
	 * Moves the chat window aside and focuses the main application so the user can interact with the map.
	 */
	public static void yieldToMainStage() {
		if (chatStage == null || !chatStage.isShowing()) {
			return;
		}
		Stage main = JFXMain.stage;
		if (main == null || !main.isShowing()) {
			chatStage.setIconified(true);
			return;
		}

		Rectangle2D screen = Screen.getPrimary().getVisualBounds();
		double chatWidth = chatStage.getWidth() > 0 ? chatStage.getWidth() : chatStage.getMinWidth();
		double chatHeight = chatStage.getHeight() > 0 ? chatStage.getHeight() : chatStage.getMinHeight();
		double gap = 8;

		double x = main.getX() + main.getWidth() + gap;
		double y = main.getY();

		if (x + chatWidth > screen.getMaxX()) {
			x = Math.max(screen.getMinX(), screen.getMaxX() - chatWidth - 12);
		}
		if (y + chatHeight > screen.getMaxY()) {
			y = Math.max(screen.getMinY(), screen.getMaxY() - chatHeight - 12);
		}

		chatStage.setX(x);
		chatStage.setY(y);
		main.toFront();
		main.requestFocus();
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
