package com.ursulagis.desktop.gui.onboarding;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;

/**
 * Shows a gamified "Achievement Unlocked" dialog when the user completes an onboarding action for the first time.
 */
public final class AchievementUnlockedDialog {

    // Reuse main app icon as trophy to ensure resource exists on classpath
    private static final String TROPHY_RESOURCE = "trophy.png";
    private static final int IMAGE_SIZE = 64;

    private AchievementUnlockedDialog() {}

    /**
     * Show the achievement dialog. Must be called on the JavaFX application thread.
     *
     * @param owner         parent stage (can be null)
     * @param achievementId one of OnboardingAchievements.FIRST_*
     */
    public static void show(Stage owner, String achievementId) {
        String titleKey = "Onboarding.achievementUnlocked";
        String title = Messages.getString(titleKey);
        if (title == null || title.equals(titleKey)) title = "Achievement Unlocked!";

        String messageKey = "Onboarding.achievement." + achievementId;
        String message = Messages.getString(messageKey);
        if (message == null || message.equals(messageKey)) message = achievementId;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.APPLICATION_MODAL);
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 0, 0, 0));

        try {
            java.io.InputStream stream = JFXMain.class.getResourceAsStream(TROPHY_RESOURCE);
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(IMAGE_SIZE);
                    iv.setFitHeight(IMAGE_SIZE);
                    iv.setPreserveRatio(true);
                    content.getChildren().add(iv);
                }
            }
        } catch (Exception ignored) {
            // no image - dialog still shows text
        }

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(360);
        content.getChildren().add(msgLabel);

        StackPane root = new StackPane();
        root.getChildren().add(content);

        alert.getDialogPane().setContent(root);
        alert.getDialogPane().setPrefWidth(400);

        playFireworks(root);

        alert.showAndWait();
    }

    private static void playFireworks(StackPane root) {
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Random random = new Random();
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            gc.clearRect(0, 0, w, h);
            for (int i = 0; i < 18; i++) {
                double x = random.nextDouble() * w;
                double y = random.nextDouble() * h;
                double radius = 4 + random.nextDouble() * 10;
                Color color = Color.hsb(random.nextDouble() * 360, 0.8, 1.0, 0.9);
                gc.setFill(color);
                gc.fillOval(x - radius / 2, y - radius / 2, radius, radius);
            }
        }));
        timeline.setCycleCount(18);
        timeline.setOnFinished(e -> {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            root.getChildren().remove(canvas);
        });
        timeline.play();
    }
}
