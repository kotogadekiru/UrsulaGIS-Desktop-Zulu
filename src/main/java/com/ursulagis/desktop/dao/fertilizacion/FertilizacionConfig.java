package com.ursulagis.desktop.dao.fertilizacion;

import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;

import javafx.beans.property.SimpleObjectProperty;

public class FertilizacionConfig extends CosechaConfig {
//TODO agregar las keys a las propiedades especificas de la labor de fertilizacion
	//ej: costo pasada, precioFert

	public static enum UnidadPrecio { Kg, Tn, Litros }

	private static final String PRECIO_INSUMO_UNIDAD_KEY = "FERT_INSUMO_UNIDAD_KEY";

	/**
	 * kg/L según el fertilizante; si falta o el valor no es positivo se usa 1.0.
	 */
	public static double kgPorLitroFertilizante(Fertilizante f) {
		if (f == null) {
			return 1.0;
		}
		double d = f.getDensidad();
		return d > 0 ? d : 1.0;
	}

	private SimpleObjectProperty<UnidadPrecio> precioFertilizanteUnitProperty;

	/**
	 * hace referencia al archivo donde se guardan las configuraciones
	 */
	//Configuracion config;
	public FertilizacionConfig(){
	super();
	//config = Configuracion.getInstance();//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL

		UnidadPrecio configured = UnidadPrecio.Kg;
		String defaultUnit = config.getPropertyOrDefault(PRECIO_INSUMO_UNIDAD_KEY, UnidadPrecio.Kg.name());
		if (defaultUnit != null) {
			try {
				configured = UnidadPrecio.valueOf(defaultUnit);
			} catch (IllegalArgumentException e) {
				configured = UnidadPrecio.Kg;
			}
		}
		precioFertilizanteUnitProperty = new SimpleObjectProperty<>(configured);
		precioFertilizanteUnitProperty.addListener((obs, o, n) -> {
			if (n != null) {
				config.setProperty(PRECIO_INSUMO_UNIDAD_KEY, n.name());
			}
		});
	}

	public SimpleObjectProperty<UnidadPrecio> precioFertilizanteUnitProperty() {
		return precioFertilizanteUnitProperty;
	}

	/**
	 * Convierte una dosis ingresada en la unidad de insumo configurada a kg/ha (unidad interna de {@code FertilizacionItem}).
	 * La densidad del fertilizante solo aplica si la unidad es litros.
	 */
	public static double doseFromUserUnitToKgHa(double value, UnidadPrecio unit, Fertilizante fertilizante) {
		if (unit == null) {
			return value;
		}
		switch (unit) {
		case Tn:
			return value * 1000.0;
		case Litros:
			return value * kgPorLitroFertilizante(fertilizante);
		case Kg:
		default:
			return value;
		}
	}
	
	
//	public void save(){
//		config.save();
//	}


	
}
