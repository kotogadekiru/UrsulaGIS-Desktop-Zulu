package com.ursulagis.desktop.gui;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.dao.config.Campania;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Empresa;
import com.ursulagis.desktop.dao.config.Establecimiento;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Lote;
import com.ursulagis.desktop.dao.config.Plaga;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.utils.DAH;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Form dialogs for creating config entities from SmartTableView "New" buttons.
 */
public final class ConfigFormDialogs {

	private ConfigFormDialogs() {}

	public static Optional<Empresa> promptEmpresa(Window owner) {
		TextField nombre = textField("");
		return show(owner, Messages.getString("ConfigGUI.nuevaEmpresa"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> new Empresa(nombre.getText().trim()));
	}

	public static Optional<Campania> promptCampania(Window owner) {
		TextField nombre = textField("");
		DatePicker inicio = new DatePicker(LocalDate.now());
		DatePicker fin = new DatePicker(LocalDate.now().plusMonths(6));
		return show(owner, Messages.getString("ConfigGUI.nuevaCampania"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.inicio"), inicio);
					addRow(grid, 2, Messages.getString("ConfigGUI.form.fin"), fin);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Campania c = new Campania(nombre.getText().trim());
					c.setInicio(toCalendar(inicio.getValue()));
					c.setFin(toCalendar(fin.getValue()));
					return c;
				});
	}

	public static Optional<Establecimiento> promptEstablecimiento(Window owner) {
		TextField nombre = textField("");
		ComboBox<Empresa> empresa = combo(DAH.getAllEmpresas());
		TextField supTotal = numberField(0);
		TextField supAgricola = numberField(0);
		TextField supGanadera = numberField(0);
		TextField supDesperdicio = numberField(0);
		return show(owner, Messages.getString("ConfigGUI.nuevoEstablecimiento"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.empresa"), empresa);
					addRow(grid, 2, Messages.getString("ConfigGUI.form.superficieTotal"), supTotal);
					addRow(grid, 3, Messages.getString("ConfigGUI.form.superficieAgricola"), supAgricola);
					addRow(grid, 4, Messages.getString("ConfigGUI.form.superficieGanadera"), supGanadera);
					addRow(grid, 5, Messages.getString("ConfigGUI.form.superficieDesperdicio"), supDesperdicio);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Establecimiento e = new Establecimiento(nombre.getText().trim());
					e.setEmpresa(empresa.getValue());
					e.setSuperficieTotal(parseDouble(supTotal));
					e.setSuperficieAgricola(parseDouble(supAgricola));
					e.setSuperficieGanadera(parseDouble(supGanadera));
					e.setSuperficieDesperdicio(parseDouble(supDesperdicio));
					return e;
				});
	}

	public static Optional<Lote> promptLote(Window owner) {
		TextField nombre = textField("");
		ComboBox<Establecimiento> establecimiento = combo(DAH.getAllEstablecimientos());
		TextField superficie = numberField(0);
		return show(owner, Messages.getString("ConfigGUI.nuevoLote"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.establecimiento"), establecimiento);
					addRow(grid, 2, Messages.getString("JFXMain.superficie"), superficie);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Lote l = new Lote(nombre.getText().trim());
					l.setEstablecimiento(establecimiento.getValue());
					l.setSuperficie(parseDouble(superficie));
					return l;
				});
	}

	public static Optional<Asignacion> promptAsignacion(Window owner) {
		ComboBox<Lote> lote = combo(DAH.getAllLotes());
		ComboBox<Campania> campania = combo(DAH.getAllCampanias());
		ComboBox<Cultivo> cultivo = combo(DAH.getAllCultivos());
		return show(owner, Messages.getString("JFXMain.configAsignacionMI"),
				grid -> {
					addRow(grid, 0, Messages.getString("ConfigGUI.form.lote"), lote);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.campania"), campania);
					addRow(grid, 2, Messages.getString("HarvestConfigDialogController.cultivoLabel"), cultivo);
				},
				() -> lote.getValue() != null
						&& (campania.getValue() != null || cultivo.getValue() != null),
				() -> {
					Asignacion a = new Asignacion();
					a.setLote(lote.getValue());
					a.setCampania(campania.getValue());
					a.setCultivo(cultivo.getValue());
					return a;
				});
	}

	public static Optional<Plaga> promptPlaga(Window owner) {
		TextField nombre = textField("");
		TextField umbral = numberField(0);
		return show(owner, Messages.getString("Plaga.nueva"),
				grid -> {
					addRow(grid, 0, Messages.getString("Plaga.Nombre"), nombre);
					addRow(grid, 1, Messages.getString("Plaga.UmbralDanio"), umbral);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Plaga p = new Plaga(nombre.getText().trim());
					p.setUmbralDanio(parseDouble(umbral));
					return p;
				});
	}

	public static Optional<Cultivo> promptCultivo(Window owner) {
		TextField nombre = textField("");
		CheckBox estival = new CheckBox();
		estival.setSelected(true);
		TextField absN = numberField(0);
		TextField extN = numberField(0);
		TextField absP = numberField(0);
		TextField extP = numberField(0);
		TextField absK = numberField(0);
		TextField extK = numberField(0);
		TextField absS = numberField(0);
		TextField extS = numberField(0);
		TextField aporteMO = numberField(0);
		TextField rinde = numberField(0);
		return show(owner, Messages.getString("ConfigGUI.nuevoCultivo"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.estival"), estival);
					addRow(grid, 2, "Abs N", absN);
					addRow(grid, 3, "Ext N", extN);
					addRow(grid, 4, "Abs P", absP);
					addRow(grid, 5, "Ext P", extP);
					addRow(grid, 6, "Abs K", absK);
					addRow(grid, 7, "Ext K", extK);
					addRow(grid, 8, "Abs S", absS);
					addRow(grid, 9, "Ext S", extS);
					addRow(grid, 10, Messages.getString("ConfigGUI.form.aporteMO"), aporteMO);
					addRow(grid, 11, Messages.getString("ConfigGUI.form.rindeEsperado"), rinde);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Cultivo c = new Cultivo(nombre.getText().trim());
					c.setEstival(estival.isSelected());
					c.setAbsN(parseDouble(absN));
					c.setExtN(parseDouble(extN));
					c.setAbsP(parseDouble(absP));
					c.setExtP(parseDouble(extP));
					c.setAbsK(parseDouble(absK));
					c.setExtK(parseDouble(extK));
					c.setAbsS(parseDouble(absS));
					c.setExtS(parseDouble(extS));
					c.setAporteMO(parseDouble(aporteMO));
					c.setRindeEsperado(parseDouble(rinde));
					return c;
				});
	}

	public static Optional<Fertilizante> promptFertilizante(Window owner) {
		TextField nombre = textField("");
		TextField porcN = numberField(0);
		TextField porcP = numberField(0);
		TextField porcK = numberField(0);
		TextField porcS = numberField(0);
		TextField densidad = numberField(1);
		TextField porcCa = numberField(0);
		TextField porcMg = numberField(0);
		TextField porcB = numberField(0);
		TextField porcCl = numberField(0);
		TextField porcCo = numberField(0);
		TextField porcCu = numberField(0);
		TextField porcFe = numberField(0);
		TextField porcMn = numberField(0);
		TextField porcMo = numberField(0);
		TextField porcZn = numberField(0);
		return show(owner, Messages.getString("ConfigGUI.nuevoFertilizante"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, "N %", porcN);
					addRow(grid, 2, "P %", porcP);
					addRow(grid, 3, "K %", porcK);
					addRow(grid, 4, "S %", porcS);
					addRow(grid, 5, Messages.getString("ConfigGUI.form.densidad"), densidad);
					addRow(grid, 6, "Ca %", porcCa);
					addRow(grid, 7, "Mg %", porcMg);
					addRow(grid, 8, "B %", porcB);
					addRow(grid, 9, "Cl %", porcCl);
					addRow(grid, 10, "Co %", porcCo);
					addRow(grid, 11, "Cu %", porcCu);
					addRow(grid, 12, "Fe %", porcFe);
					addRow(grid, 13, "Mn %", porcMn);
					addRow(grid, 14, "Mo %", porcMo);
					addRow(grid, 15, "Zn %", porcZn);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Fertilizante f = new Fertilizante(
							nombre.getText().trim(),
							parseDouble(porcN),
							parseDouble(porcP),
							parseDouble(porcK),
							parseDouble(porcS));
					f.setDensidad(parseDouble(densidad));
					f.setPorcCa(parseDouble(porcCa));
					f.setPorcMg(parseDouble(porcMg));
					f.setPorcB(parseDouble(porcB));
					f.setPorcCl(parseDouble(porcCl));
					f.setPorcCo(parseDouble(porcCo));
					f.setPorcCu(parseDouble(porcCu));
					f.setPorcFe(parseDouble(porcFe));
					f.setPorcMn(parseDouble(porcMn));
					f.setPorcMo(parseDouble(porcMo));
					f.setPorcZn(parseDouble(porcZn));
					return f;
				});
	}

	public static Optional<Agroquimico> promptAgroquimico(Window owner) {
		TextField nombre = textField("");
		TextField numRegistro = textField("");
		TextField empresa = textField("");
		TextField activos = textField("");
		TextField banda = textField("");
		CheckBox activo = new CheckBox();
		activo.setSelected(true);
		return show(owner, Messages.getString("ConfigGUI.nuevoAgroquimico"),
				grid -> {
					addRow(grid, 0, Messages.getString("Agroquimico.Nombre"), nombre);
					addRow(grid, 1, Messages.getString("ConfigGUI.form.numRegistro"), numRegistro);
					addRow(grid, 2, Messages.getString("ConfigGUI.form.empresa"), empresa);
					addRow(grid, 3, Messages.getString("Agroquimico.Activos"), activos);
					addRow(grid, 4, Messages.getString("Agroquimico.BandaToxicologica"), banda);
					addRow(grid, 5, Messages.getString("ConfigGUI.form.activo"), activo);
				},
				() -> nombre.getText() != null && !nombre.getText().isBlank(),
				() -> {
					Agroquimico a = new Agroquimico(nombre.getText().trim());
					a.setNumRegistro(blankToNull(numRegistro.getText()));
					a.setEmpresa(blankToNull(empresa.getText()));
					a.setActivos(blankToNull(activos.getText()));
					a.setBandaToxicologica(blankToNull(banda.getText()));
					a.setActivo(activo.isSelected());
					return a;
				});
	}

	public static Optional<Semilla> promptSemilla(Window owner) {
		TextField nombre = textField("");
		ComboBox<Cultivo> cultivo = combo(DAH.getAllCultivos());
		TextField pesoDeMil = numberField(150);
		TextField pg = numberField(1);
		return show(owner, Messages.getString("ConfigGUI.nuevaSemilla"),
				grid -> {
					addRow(grid, 0, Messages.getString("SiembraConfigDialog.nombreLabel"), nombre);
					addRow(grid, 1, Messages.getString("HarvestConfigDialogController.cultivoLabel"), cultivo);
					addRow(grid, 2, Messages.getString("SiembraConfigDialogController.PMS"), pesoDeMil);
					addRow(grid, 3, Messages.getString("SiembraConfigDialogController.PG"), pg);
				},
				() -> {
					if (nombre.getText() == null || nombre.getText().isBlank() || cultivo.getValue() == null) {
						return false;
					}
					try {
						double peso = PropertyHelper.parseDouble(pesoDeMil.getText()).doubleValue();
						double pgVal = PropertyHelper.parseDouble(pg.getText()).doubleValue();
						return peso > 0 && pgVal > 0 && pgVal <= 1;
					} catch (Exception e) {
						return false;
					}
				},
				() -> {
					Semilla semilla = new Semilla(nombre.getText().trim(), cultivo.getValue());
					semilla.setPesoDeMil(PropertyHelper.parseDouble(pesoDeMil.getText()).doubleValue());
					semilla.setPG(PropertyHelper.parseDouble(pg.getText()).doubleValue());
					return semilla;
				});
	}

	private static <T> Optional<T> show(Window owner, String title,
			java.util.function.Consumer<GridPane> contentBuilder,
			Supplier<Boolean> valid,
			Supplier<T> builder) {
		Dialog<T> dialog = new Dialog<>();
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 10, 0, 10));
		contentBuilder.accept(grid);
		dialog.getDialogPane().setContent(grid);

		dialog.setOnShowing(e -> {
			Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
			stage.getIcons().addAll(JFXMain.stage.getIcons());
		});
		if (owner != null) {
			dialog.initOwner(owner);
		}

		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
			if (!Boolean.TRUE.equals(valid.get())) {
				event.consume();
			}
		});

		dialog.setResultConverter(bt -> ButtonType.OK.equals(bt) ? builder.get() : null);
		return dialog.showAndWait();
	}

	private static void addRow(GridPane grid, int row, String label, Node field) {
		grid.add(new Label(label), 0, row);
		if (field instanceof TextField || field instanceof ComboBox || field instanceof DatePicker) {
			GridPane.setHgrow(field, Priority.ALWAYS);
			if (field instanceof javafx.scene.layout.Region r) {
				r.setMaxWidth(Double.MAX_VALUE);
			}
		}
		grid.add(field, 1, row);
	}

	private static TextField textField(String initial) {
		TextField tf = new TextField(initial);
		tf.setMaxWidth(Double.MAX_VALUE);
		return tf;
	}

	private static TextField numberField(double initial) {
		return textField(PropertyHelper.formatDouble(Double.valueOf(initial)));
	}

	private static <T> ComboBox<T> combo(List<T> items) {
		ComboBox<T> cb = new ComboBox<>(FXCollections.observableArrayList(items));
		cb.setMaxWidth(Double.MAX_VALUE);
		if (!items.isEmpty()) {
			cb.getSelectionModel().select(0);
		}
		return cb;
	}

	private static double parseDouble(TextField field) {
		try {
			return PropertyHelper.parseDouble(field.getText()).doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private static Calendar toCalendar(LocalDate date) {
		Calendar cal = Calendar.getInstance();
		if (date != null) {
			cal.setTime(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		}
		return cal;
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}
}
