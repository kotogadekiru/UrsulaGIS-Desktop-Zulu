package com.ursulagis.desktop.gui.onboarding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ursulagis.desktop.gui.Messages;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog that shows all onboarding achievements: obtained and missing.
 * Can be shown on startup or from a menu.
 */
public final class AchievementsOverviewDialog {

    private static final String TITLE_KEY = "Onboarding.overviewTitle";
    private static final String GROUP_KEY_PREFIX = "Onboarding.group.";
    private static final String MESSAGE_KEY_PREFIX = "Onboarding.achievement.";
    private static final String HINT_KEY_PREFIX = "Onboarding.hint.";

    private AchievementsOverviewDialog() {}

    /**
     * Show the achievements overview dialog. Call on JavaFX thread.
     *
     * @param owner parent stage (can be null)
     */
    public static void show(Stage owner) {
        String title = msg(TITLE_KEY, "Your achievements");
        String clearLabel = msg("Onboarding.clear", "Clear achievements");
        String dontShowLabel = msg("Onboarding.dontShowAtStart", "Don't show at startup");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setResizable(true);
        
        dialog.setTitle(title);
        ButtonType clearType = new ButtonType(clearLabel, ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(clearType, ButtonType.CLOSE);
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.NONE);
        }
        dialog.getDialogPane().setPrefSize(620, 480);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        final OnboardingAchievements achievements = OnboardingAchievements.getInstance();
        ScrollPane scrollPane = buildContent(achievements, dontShowLabel);
        dialog.getDialogPane().setContent(scrollPane);

        Runnable refreshOnUnlock = () -> {
            if (dialog.isShowing()) {
                dialog.getDialogPane().setContent(buildContent(achievements, dontShowLabel));
            }
        };
        achievements.addOnUnlockListener(refreshOnUnlock);
        dialog.setOnHidden(e -> achievements.removeOnUnlockListener(refreshOnUnlock));

        Button clearButton = (Button) dialog.getDialogPane().lookupButton(clearType);
        if (clearButton != null) {
            clearButton.setOnAction(e -> {
                achievements.resetAll();
                dialog.close();
                // Re-open to show updated (all missing) state
                show(owner);
            });
        }

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setDefaultButton(true);
        }
        dialog.show();
    }

    /**
     * Builds the scrollable content for the overview from current achievement state.
     * Achievements are grouped by controller; each row shows icon (✓/○) + text.
     */
    private static ScrollPane buildContent(OnboardingAchievements achievements, String dontShowLabel) {
        Map<String, List<String>> byController = new LinkedHashMap<>();
        for (String controllerId : OnboardingAchievements.getControllerIdsInOrder()) {
            byController.put(controllerId, new ArrayList<>());
        }
        for (String id : achievements.getAllAchievementIds()) {
            String controllerId = OnboardingAchievements.getControllerId(id);
            if (controllerId != null) {
                byController.get(controllerId).add(id);
            }
        }
        int totalObtained = 0;
        int total = 0;
        for (List<String> ids : byController.values()) {
            total += ids.size();
            for (String id : ids) {
                if (achievements.isUnlocked(id)) totalObtained++;
            }
        }

        VBox content = new VBox(16);
        content.setPadding(new Insets(12, 16, 16, 16));
        content.setMinWidth(520);
        content.setPrefWidth(560);

        String pointsText = msg("Onboarding.points", "Points: {0}/{1}")
                .replace("{0}", String.valueOf(totalObtained))
                .replace("{1}", String.valueOf(total));
        Label pointsLabel = new Label(pointsText);
        pointsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.05em;");
        content.getChildren().add(pointsLabel);

        CheckBox cbDontShow = new CheckBox(dontShowLabel);
        cbDontShow.setSelected(!achievements.isShowAtStart());
        cbDontShow.selectedProperty().addListener((obs, oldV, newV) ->
            achievements.setShowAtStart(!newV)
        );

        // One section per controller: all achievements with icon (✓ obtained, ○ missing)
        for (String controllerId : OnboardingAchievements.getControllerIdsInOrder()) {
            List<String> ids = byController.get(controllerId);
            if (ids.isEmpty()) continue;

            String groupTitle = msg(GROUP_KEY_PREFIX + controllerId, controllerId);
            Label groupLabel = new Label(groupTitle);
            groupLabel.getStyleClass().add("achievement-section-header");
            groupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.15em;");
            content.getChildren().add(groupLabel);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(6);
            int row = 0;
            for (String id : ids) {
                boolean unlocked = achievements.isUnlocked(id);
                Label icon = new Label(unlocked ? "✓" : "○");
                icon.setStyle(unlocked ? "-fx-text-fill: green; -fx-font-weight: bold;" : "-fx-text-fill: gray;");
                Label text = new Label(achievementMessage(id));
                text.setWrapText(true);
                text.setMaxWidth(420);
                if (!unlocked) text.setStyle("-fx-opacity: 0.9;");
                text.setTooltip(new Tooltip(hintMessage(id)));
                grid.add(icon, 0, row);
                grid.add(text, 1, row);
                row++;
            }
            content.getChildren().add(grid);
        }

        content.getChildren().add(new Separator());
        content.getChildren().add(cbDontShow);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(400);
        return scrollPane;
    }

    private static String msg(String key, String fallback) {
        String s = Messages.getString(key);
        return (s != null && !s.equals(key)) ? s : fallback;
    }

    private static String achievementMessage(String achievementId) {
        return msg(MESSAGE_KEY_PREFIX + achievementId, achievementId);
    }

    private static String hintMessage(String achievementId) {
        return msg(HINT_KEY_PREFIX + achievementId, "");
    }
}
