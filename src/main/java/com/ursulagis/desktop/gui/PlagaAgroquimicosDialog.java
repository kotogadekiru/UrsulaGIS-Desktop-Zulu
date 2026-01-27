package com.ursulagis.desktop.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Plaga;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import com.ursulagis.desktop.utils.DAH;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PlagaAgroquimicosDialog extends Dialog<ButtonType> {
	
	public PlagaAgroquimicosDialog(Plaga plaga) {
		super();
		
		this.setTitle(Messages.getString("JFXMain.397") + ": " + plaga.getNombre()); // "Configurar Agroquímicos"
		this.setHeaderText(Messages.getString("JFXMain.398")); // "Seleccione los agroquímicos registrados para esta plaga"
		
		VBox mainVBox = new VBox(10);
		mainVBox.setPadding(new Insets(10));
		
		// Umbral de daño
		HBox umbralBox = new HBox(10);
		Label umbralLabel = new Label(Messages.getString("JFXMain.399")); // "Umbral de Daño:"
		TextField umbralField = new TextField(plaga.getUmbralDanio() != null ? plaga.getUmbralDanio().toString() : "0.0");
		umbralBox.getChildren().addAll(umbralLabel, umbralField);
		
		// Listas de agroquímicos (solo activos)
		ObservableList<Agroquimico> availableAgroquimicos = FXCollections.observableArrayList(DAH.getAgroquimicosActivos());
		ObservableList<Agroquimico> registeredAgroquimicos = FXCollections.observableArrayList();
		
		// Inicializar con los agroquímicos ya registrados (solo los activos)
		if(plaga.getAgroquimicosRegistrados() != null && !plaga.getAgroquimicosRegistrados().isEmpty()) {
			// Filtrar solo los activos de los registrados
			List<Agroquimico> activosRegistrados = plaga.getAgroquimicosRegistrados().stream()
					.filter(Agroquimico::isActivo)
					.collect(Collectors.toList());
			registeredAgroquimicos.addAll(activosRegistrados);
			availableAgroquimicos.removeAll(activosRegistrados);
		}
		
		// Tabla de agroquímicos disponibles
		SmartTableView<Agroquimico> availableTable = new SmartTableView<Agroquimico>(
				availableAgroquimicos,
				Arrays.asList("Id", "Activo"),
				Arrays.asList("Nombre", "Activos", "BandaToxicologica"),
				Arrays.asList(
						Messages.getString("Agroquimico.Nombre"),
						Messages.getString("Agroquimico.Activos"),
						Messages.getString("Agroquimico.BandaToxicologica")));
		availableTable.setEditable(false);
		availableTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		availableTable.setPrefWidth(400);
		
		// Tabla de agroquímicos registrados
		SmartTableView<Agroquimico> registeredTable = new SmartTableView<Agroquimico>(
				registeredAgroquimicos,
				Arrays.asList("Id", "Activo"),
				Arrays.asList("Nombre", "Activos", "BandaToxicologica"),
				Arrays.asList(
						Messages.getString("Agroquimico.Nombre"),
						Messages.getString("Agroquimico.Activos"),
						Messages.getString("Agroquimico.BandaToxicologica")));
		registeredTable.setEditable(false);
		registeredTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		registeredTable.setPrefWidth(300);
		
		// Botones para mover agroquímicos
		Button addButton = new Button();
		addButton.setMaxWidth(Double.MAX_VALUE);
		addButton.setText(Messages.getString("doBulkNDVIDownload.addSelectedButton") + " -->");
		addButton.setOnAction((ae) -> {
			ObservableList<Agroquimico> selected = availableTable.getSelectionModel().getSelectedItems();
			registeredAgroquimicos.addAll(selected);
			availableAgroquimicos.removeAll(selected);
			availableTable.refresh();
			registeredTable.refresh();
		});
		
		Button removeButton = new Button();
		removeButton.setMaxWidth(Double.MAX_VALUE);
		removeButton.setText("<-- " + Messages.getString("doBulkNDVIDownload.remSelectedButton"));
		removeButton.setOnAction((ae) -> {
			ObservableList<Agroquimico> selected = registeredTable.getSelectionModel().getSelectedItems();
			availableAgroquimicos.addAll(selected);
			registeredAgroquimicos.removeAll(selected);
			availableTable.refresh();
			registeredTable.refresh();
		});
		
		// Panel central con botones
		BorderPane centerPanel = new BorderPane();
		centerPanel.setPrefWidth(120);
		VBox buttonsBox = new VBox(addButton, removeButton);
		buttonsBox.setSpacing(10);
		buttonsBox.setPadding(new Insets(0, 10, 10, 10));
		buttonsBox.setAlignment(Pos.CENTER);
		centerPanel.setCenter(buttonsBox);
		
		// Labels para las tablas
		Label availableLabel = new Label(Messages.getString("JFXMain.400")); // "Agroquímicos Disponibles:"
		Label registeredLabel = new Label(Messages.getString("JFXMain.396")); // "Agroquímicos Registrados"
		
		VBox availableBox = new VBox(5, availableLabel, availableTable);
		VBox.setVgrow(availableTable, Priority.ALWAYS);
		
		VBox registeredBox = new VBox(5, registeredLabel, registeredTable);
		VBox.setVgrow(registeredTable, Priority.ALWAYS);
		
		// SplitPane con las dos tablas y el panel central
		SplitPane splitPane = new SplitPane(availableBox, centerPanel, registeredBox);
		splitPane.setDividerPositions(0.4, 0.55);
		
		mainVBox.getChildren().addAll(umbralBox, splitPane);
		VBox.setVgrow(splitPane, Priority.ALWAYS);
		
		this.getDialogPane().setContent(mainVBox);
		this.getDialogPane().setPrefSize(800, 600);
		this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		// Agregar icono al diálogo
		this.setOnShown(e -> {
			Stage stage = ((Stage) this.getDialogPane().getScene().getWindow());
			stage.getIcons().addAll(JFXMain.stage.getIcons());
		});
		
		this.setResultConverter(dialogButton -> {
			if(dialogButton == ButtonType.OK) {
				try {
					DAH.beginTransaction();
					// Actualizar umbral de daño
					Double umbral = Double.parseDouble(umbralField.getText());
					plaga.setUmbralDanio(umbral);
					
					// Actualizar agroquímicos registrados
					plaga.setAgroquimicosRegistrados(new ArrayList<>(registeredAgroquimicos));
					
					DAH.save(plaga);
					DAH.commitTransaction();
				} catch(Exception e) {
					DAH.rollbackTransaction();
					e.printStackTrace();
					Alert error = new Alert(AlertType.ERROR);
					error.setContentText(Messages.getString("JFXMain.401")); // "Error al guardar"
					error.showAndWait();
				}
			}
			return dialogButton;
		});
	}
	
	public static void show(Plaga plaga) {
		Platform.runLater(() -> {
			PlagaAgroquimicosDialog dialog = new PlagaAgroquimicosDialog(plaga);
			dialog.showAndWait();
		});
	}
}
