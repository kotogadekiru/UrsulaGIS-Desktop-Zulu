package com.ursulagis.desktop.gui.chat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.gui.JFXMain;

import javafx.scene.control.ChoiceDialog;

/**
 * Lets the user pick among polygon name candidates for a workflow step.
 */
public final class WorkflowPolygonChoiceDialog {

	private WorkflowPolygonChoiceDialog() {
	}

	public static Optional<Poligono> choose(List<Poligono> candidates, String hint) {
		if (candidates == null || candidates.isEmpty()) {
			return Optional.empty();
		}
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}
		List<String> labels = candidates.stream()
				.map(p -> p.getNombre() != null ? p.getNombre() : "(sin nombre)")
				.collect(Collectors.toList());
		ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
		dialog.setTitle("Elegir polígono");
		dialog.setHeaderText("Varios polígonos coinciden con \"" + hint + "\"");
		dialog.setContentText("Seleccioná el polígono correcto:");
		if (JFXMain.stage != null) {
			dialog.initOwner(JFXMain.stage);
		}
		Optional<String> selected = dialog.showAndWait();
		if (selected.isEmpty()) {
			return Optional.empty();
		}
		int idx = labels.indexOf(selected.get());
		return Optional.of(candidates.get(Math.max(idx, 0)));
	}
}
