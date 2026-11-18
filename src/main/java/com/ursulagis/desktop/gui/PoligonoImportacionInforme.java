package com.ursulagis.desktop.gui;

import java.text.NumberFormat;
import java.util.Map;

import com.ursulagis.desktop.dao.Poligono;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

/**
 * Texto del informe que se muestra al terminar de importar polígonos.
 */
public final class PoligonoImportacionInforme {
	private PoligonoImportacionInforme() {
	}

	public static void agregar(Map<String, String> informe, String archivo, String detalle) {
		String clave = archivo == null || archivo.isBlank() ? "sin archivo" : archivo;
		informe.merge(clave, detalle, (previo, nuevo) -> previo + "\n" + nuevo);
	}

	public static String detalle(Poligono poli) {
		String nombre = poli.getNombre() == null || poli.getNombre().isBlank() ? "sin nombre" : poli.getNombre();
		NumberFormat formato = Messages.getNumberFormat();
		String texto = nombre + " — " + formato.format(poli.getArea()) + " ha";
		int huecos = poli.getHuecos() == null ? 0 : poli.getHuecos().size();
		if (huecos == 1) {
			texto += " — 1 hueco";
		} else if (huecos > 1) {
			texto += " — " + huecos + " huecos";
		}
		return texto;
	}

	public static String mensaje(Exception e) {
		if (e == null) {
			return "error desconocido";
		}
		String mensaje = e.getMessage();
		if (mensaje == null || mensaje.isBlank()) {
			return e.getClass().getSimpleName();
		}
		return mensaje;
	}

	public static void mostrarInformeImportacionPoligonos(Map<String, String> importados, Map<String, String> omitidos) {
		Map<String, String> cargados = importados == null ? Map.of() : importados;
		Map<String, String> descartados = omitidos == null ? Map.of() : omitidos;
		int cantidadImportados = cantidad(cargados);
		int cantidadOmitidos = cantidad(descartados);
		Platform.runLater(() -> {
			Alert alert = new Alert(cantidadImportados == 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
			alert.initOwner(JFXMain.stage);
			alert.setTitle("UrsulaGIS");
			alert.setHeaderText(encabezado(cantidadImportados, cantidadOmitidos));
			TextArea detalle = new TextArea(texto(cargados, descartados));
			detalle.setEditable(false);
			detalle.setWrapText(true);
			detalle.setPrefRowCount(Math.min(16, Math.max(6, cantidadImportados + cantidadOmitidos + 4)));
			detalle.setPrefWidth(520);
			alert.getDialogPane().setContent(detalle);
			alert.setResizable(true);
			alert.showAndWait();
		});
	}

	public static String encabezado(int importados, int omitidos) {
		if (importados == 0 && omitidos == 0) {
			return "No se importó ningún polígono";
		}
		if (importados == 1 && omitidos == 0) {
			return "Se importó 1 polígono";
		}
		if (omitidos == 0) {
			return "Se importaron " + importados + " polígonos";
		}
		if (importados == 0) {
			return "No se pudo importar ningún polígono (" + omitidos + " omitidos)";
		}
		return "Se importaron " + importados + " polígonos. Omitidos: " + omitidos;
	}

	public static String texto(Map<String, String> importados, Map<String, String> omitidos) {
		StringBuilder informe = new StringBuilder();
		if (importados != null && !importados.isEmpty()) {
			informe.append("Importados\n");
			appendAgrupado(informe, importados);
		}
		if (omitidos != null && !omitidos.isEmpty()) {
			if (informe.length() > 0) {
				informe.append("\n");
			}
			informe.append("Omitidos\n");
			appendAgrupado(informe, omitidos);
		}
		if (informe.length() == 0) {
			return "El archivo no contenía polígonos.";
		}
		return informe.toString();
	}

	private static int cantidad(Map<String, String> items) {
		int total = 0;
		for (String detalle : items.values()) {
			if (detalle == null || detalle.isEmpty()) {
				continue;
			}
			total += detalle.split("\n", -1).length;
		}
		return total;
	}

	private static void appendAgrupado(StringBuilder texto, Map<String, String> items) {
		for (Map.Entry<String, String> entrada : items.entrySet()) {
			String[] lineas = entrada.getValue() == null ? new String[0] : entrada.getValue().split("\n", -1);
			texto.append(entrada.getKey()).append(" (").append(lineas.length).append(")\n");
			int mostrados = 0;
			for (String detalle : lineas) {
				if (mostrados == 200) {
					texto.append("  … y ").append(lineas.length - mostrados).append(" más\n");
					break;
				}
				texto.append("  • ").append(detalle).append("\n");
				mostrados++;
			}
		}
	}
}
