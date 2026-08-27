package com.ursulagis.desktop.gui.utils;

import java.util.Optional;
import java.util.function.Consumer;

import org.controlsfx.control.RangeSlider;

import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.util.logging.Logger;
/**
 * Misma estructura que {@link DateRangeSlider}, para filtrar por un rango numérico (% nublado).
 */
public class DoubleRangeSlider {
	private static final Logger logger = Logger.getLogger(DoubleRangeSlider.class.getName());

	private Double min, max, low, high;
	private RangeSlider innerSlider;
	private StringProperty rangeProperty;
	private Consumer<Void> onUpdate;

	public DoubleRangeSlider(Double _min, Double _max, Double _low, Double _high) {
		min = _min;
		max = _max;
		low = _low;
		high = _high;
		innerSlider = new RangeSlider(min, max, low, high);
		innerSlider.setShowTickMarks(true);
		innerSlider.setShowTickLabels(true);
		double deltaTick = innerSlider.getMax() - innerSlider.getMin();
		logger.fine("delta ticks " + deltaTick);
		innerSlider.setMajorTickUnit(deltaTick / 5);

		innerSlider.setLabelFormatter(createConverter());

		innerSlider.highValueProperty().addListener((obs, n, o) -> {
			updateLowHigh();
			if (onUpdate != null) this.onUpdate.accept(null);
		});
		innerSlider.lowValueProperty().addListener((obs, n, o) -> {
			updateLowHigh();
			if (onUpdate != null) this.onUpdate.accept(null);
		});
	}

	private void updateLowHigh() {
		this.high = innerSlider.getHighValue();
		this.low = innerSlider.getLowValue();
		rangeProperty.set(getRangeString());
	}

	private String getRangeString() {
		return format(low) + " ~ " + format(high) + "%";
	}

	private static String format(Double value) {
		return String.format("%.0f", value);
	}

	public void showSlider() {
		this.rangeProperty = new SimpleStringProperty(getRangeString());

		innerSlider.setPrefWidth(500);
		HBox hb = new HBox(innerSlider);
		HBox.setHgrow(innerSlider, Priority.ALWAYS);
		VBox vb = new VBox();
		vb.getChildren().add(hb);
		VBox.setVgrow(hb, Priority.ALWAYS);
		vb.setPadding(new Insets(20));

		Alert rangeDialog = new Alert(AlertType.CONFIRMATION);
		rangeDialog.getDialogPane().setContent(vb);
		rangeDialog.setHeaderText(rangeProperty.get());
		rangeDialog.setResizable(true);
		rangeDialog.setTitle(Messages.getString("NdviGUIController.filtrarNublado"));

		rangeDialog.initOwner(JFXMain.stage);
		rangeDialog.initModality(Modality.NONE);
		this.rangeProperty.addListener((obs, old, n) -> {
			rangeDialog.setHeaderText(n);
		});

		updateLowHigh();
		Optional<ButtonType> res = rangeDialog.showAndWait();
		if (res.get().equals(ButtonType.OK)) {
			updateLowHigh();
			logger.fine("seleccione nublado low " + low + " high " + high);
		} else {
			logger.fine("ok button not selected");
			logger.fine(res.get().getText() + " pressed");
		}
	}

	private static StringConverter<Number> createConverter() {
		StringConverter<Number> converter = new StringConverter<Number>() {

			@Override
			public String toString(Number number) {
				return format(number.doubleValue()) + "%";
			}

			@Override
			public Number fromString(String string) {
				String cleaned = string.replace("%", "").trim();
				return Double.parseDouble(cleaned);
			}
		};
		return converter;
	}

	public Double getLow() {
		return this.low;
	}

	public Double getHigh() {
		return this.high;
	}

	public void setOnUpdate(Consumer<Void> _onUpdate) {
		this.onUpdate = _onUpdate;
	}
}
