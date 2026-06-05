package com.ursulagis.desktop.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.cnh.voyager2.VoyagerHarvestDatasetSummary;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;

/**
 * Lets the user pick one or more harvest datasets when a Voyager card contains more than one.
 */
public final class VoyagerHarvestDatasetDialog {

    private VoyagerHarvestDatasetDialog() {
    }

    /**
     * @param datasets harvest datasets from the card (must not be empty)
     * @return selected datasets, empty if cancelled or none selected
     */
    public static List<VoyagerHarvestDatasetSummary> choose(List<VoyagerHarvestDatasetSummary> datasets) {
        if (datasets == null || datasets.isEmpty()) {
            return Collections.emptyList();
        }
        if (datasets.size() == 1) {
            return Collections.singletonList(datasets.get(0));
        }

        AtomicReference<List<VoyagerHarvestDatasetSummary>> result =
                new AtomicReference<>(Collections.emptyList());
        Runnable showDialog = () -> {
            ListView<VoyagerHarvestDatasetSummary> listView =
                    new ListView<>(FXCollections.observableArrayList(datasets));
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(VoyagerHarvestDatasetSummary item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDisplayLabel());
                }
            });
            listView.setPrefSize(640, 260);
            listView.getSelectionModel().selectFirst();

            VBox content = new VBox(8,
                    new Label(Messages.getString("CosechaGUIController.importarVoyagerDatasetHint")),
                    new Label(Messages.getString("CosechaGUIController.importarVoyagerDatasetMultiHint")),
                    listView);
            content.setPadding(new Insets(10));

            Dialog<List<VoyagerHarvestDatasetSummary>> dialog = new Dialog<>();
            dialog.setTitle(Messages.getString("CosechaGUIController.importarVoyager"));
            dialog.setHeaderText(Messages.getString("CosechaGUIController.importarVoyagerDatasetHeader"));
            dialog.getDialogPane().setContent(content);
            dialog.initOwner(JFXMain.stage);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) {
                    return null;
                }
                List<VoyagerHarvestDatasetSummary> selected =
                        new ArrayList<>(listView.getSelectionModel().getSelectedItems());
                return selected.isEmpty() ? null : selected;
            });

            result.set(dialog.showAndWait().orElse(Collections.emptyList()));
        };

        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    showDialog.run();
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Collections.emptyList();
            }
        }
        List<VoyagerHarvestDatasetSummary> chosen = result.get();
        return chosen != null ? chosen : Collections.emptyList();
    }
}
