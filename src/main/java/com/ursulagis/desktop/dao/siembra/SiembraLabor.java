package com.ursulagis.desktop.dao.siembra;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
public class SiembraLabor extends Labor<SiembraItem> {
//	private static final String SEMILLAS_POR_BOLSA_KEY = "SEMILLAS_POR_BOLSA";

	//esta columna es la que viene con los mapas de siembra //mentira las siembras vienen con Rate o AppIdRate o AppRate
	public static final String COLUMNA_KG_SEMILLA = "kgSemHa";//"SemillaK";//kgSemHa
	//public static final String COLUMNA_DOSIS_SEMILLA_ML = SiembraLabor.COLUMNA_SEM_10METROS;//"SemillaM";
	
	//esta columna es la que voy a exportar
	//public static final String COLUMNA_BOLSAS_HA = "BolsasHa";//=semillasMetro*(ProyectionConstants.METROS2_POR_HA/entreSurco)/semillasPorBolsa;
	public static final String COLUMNA_PRECIO_SEMILLA = "PrecioSemilla";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLabor";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";
	
//keys configuracion
	public static final String ENTRE_SURCO_DEFAULT_KEY = "ENTRE_SURCO_DEFAULT";
	public static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	public static final String SEMILLA_DEFAULT = "SEMILLA_DEFAULT";
//	private static final String SEMILLAS_POR_BOLSA_DEFAULT = "80000";
//	private static final String ENTRE_SURCO_DEFAULT = "0.525";

	public static final String COLUMNA_DOSIS_LINEA = "Fert L";
	public static final String COLUMNA_DOSIS_COSTADO= "Fert C";
	public static final String COLUMNA_SEM_10METROS = "Sem10m";
	public static final String COLUMNA_MILES_SEM_HA = "MilSemHa";
	public static final String COLUMNA_SEM_HA = "SemHa";
	public static final String COLUMNA_SEM_ML = "Sem1m";


//	public  SimpleDoubleProperty entreSurco =null; 
//	public  SimpleDoubleProperty semillasPorBolsa=null;
	@Transient
	public StringProperty colDosisSemilla;
//	@Transient
//	public Property<Semilla> semillaProperty=null;
	
	@ManyToOne
	private Semilla semilla=null;
	@ManyToOne
	private Fertilizante fertLinea=null;
	//private Double cantidadFertilizanteLinea=0.0;
	@ManyToOne
	private Fertilizante fertCostado=null;
	//private Double cantidadFertilizanteCostado=0.0;
	
	private Double entreSurco = null;// Double.valueOf(0.42);
	private Double plantasPorMetro = Double.valueOf(300);
	/** Unidad de dosis de la labor (nombre de {@link SiembraConfig.Unidad}), persistida en DB. */
	private String dosisUnidad = null;
	
	
	public SiembraLabor() {
		super();
		if (!DAH.shouldSkipEntityInit()) {
			initConfig();
		}
	}

	public SiembraLabor(FileDataStore store) {
		super(store);
		//this.setInStore(store);// esto configura el nombre	
		initConfig();
	}
	
	public SiembraLabor(SiembraLabor s) {
		super(s);	
		initConfig();
		setPlantasPorMetro(s.getPlantasPorMetro());
		
		setEntreSurco(s.getEntreSurco());
		setSemilla(s.getSemilla());
		setPrecioInsumo(s.getPrecioInsumo());
		setPrecioLabor(s.getPrecioLabor());
		setFecha(s.getFecha());
		setFertLinea(s.getFertLinea());
		setFertCostado(s.getFertCostado());
		setClasificador(s.getClasificador().clone());
		setDosisUnidad(s.getDosisUnidad());
		if (s.getDosisUnidad() != null) {
			applyPersistedDosisUnidad();
		}
	}

	private void initConfig() {
		this.productoLabor=DAH.getProductoLabor(ProductoLabor.LABOR_DE_SIEMBRA);
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colDosisSemilla = PropertyHelper.initStringProperty(SiembraLabor.COLUMNA_KG_SEMILLA, properties, availableColums);
		colAmount= new SimpleStringProperty(SiembraLabor.COLUMNA_KG_SEMILLA);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		// Preferir unidad ya persistida en la labor; si no hay, inferir de columna Ursula.
		if (dosisUnidad != null && !dosisUnidad.isBlank()) {
			applyPersistedDosisUnidad();
		} else {
			applyDosisUnitFromColumn(colDosisSemilla.get());
		}
		colDosisSemilla.addListener((obs, oldCol, newCol) -> applyDosisUnitFromColumn(newCol));
		getConfiguracion().dosisUnitProperty().addListener((obs, oldU, newU) -> {
			if (newU != null) {
				dosisUnidad = newU.name();
			}
		});

		String semillaKEY = properties.getPropertyOrDefault(SiembraLabor.SEMILLA_DEFAULT,Semilla.SEMILLA_DE_MAIZ);
		Semilla sDefault =DAH.getSemilla(semillaKEY);
		this.setSemilla(sDefault);
	}

	/**
	 * Si la columna de dosis es un campo estándar de Ursula, fija la unidad
	 * correspondiente. Para columnas genéricas (Rate, etc.) no cambia la unidad.
	 */
	public void applyDosisUnitFromColumn(String column) {
		SiembraConfig.Unidad inferred = unidadFromDosisColumn(column);
		if (inferred != null) {
			getConfiguracion().dosisUnitProperty().set(inferred);
			this.dosisUnidad = inferred.name();
		}
	}

	/**
	 * Aplica {@link #dosisUnidad} (campo persistido) a la property de configuración.
	 */
	public void applyPersistedDosisUnidad() {
		if (dosisUnidad == null || dosisUnidad.isBlank()) {
			return;
		}
		try {
			SiembraConfig.Unidad unit = SiembraConfig.Unidad.valueOf(dosisUnidad);
			getConfiguracion().dosisUnitProperty().set(unit);
		} catch (IllegalArgumentException e) {
			// valor legado o inválido: no tocar la config
		}
	}

	/**
	 * Tras escribir items en kg/Ha (unidad interna), deja la labor lista para
	 * persistir/recargar sin re-convertir la dosis.
	 */
	public void markInternalDosisAsKgHa() {
		getConfiguracion().dosisUnitProperty().set(SiembraConfig.Unidad.kgHa);
		this.dosisUnidad = SiembraConfig.Unidad.kgHa.name();
	}

	/**
	 * @return unidad implícita de la columna, o null si no es una columna Ursula conocida
	 */
	public static SiembraConfig.Unidad unidadFromDosisColumn(String column) {
		if (column == null) {
			return null;
		}
		if (COLUMNA_KG_SEMILLA.equals(column)) {
			return SiembraConfig.Unidad.kgHa;
		}
		if (COLUMNA_MILES_SEM_HA.equals(column)) {
			return SiembraConfig.Unidad.milPlaHa;
		}
		if (COLUMNA_SEM_10METROS.equals(column)) {
			return SiembraConfig.Unidad.pla10MtLineal;
		}
		if (COLUMNA_SEM_ML.equals(column)) {
			return SiembraConfig.Unidad.pla1MtLineal;
		}
		return null;
	}

	@Override
	protected void onBeforePersist() {
		SiembraConfig.Unidad unit = getConfiguracion().dosisUnitProperty().get();
		if (unit != null) {
			dosisUnidad = unit.name();
		}
	}

	@Override
	protected void onAfterLoad() {
		// Si el ctor saltó init (bootstrap/tx), completar propiedades transient
		if (colDosisSemilla == null) {
			initConfig();
		}
		if (dosisUnidad != null && !dosisUnidad.isBlank()) {
			applyPersistedDosisUnidad();
		} else if (colDosisSemilla != null) {
			applyDosisUnitFromColumn(colDosisSemilla.get());
		}
	}

	@Override
	@Transient
	public String getTypeDescriptors() {
		String type = SiembraLabor.COLUMNA_SEM_10METROS + ":Double,"
				+SiembraLabor.COLUMNA_KG_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_DOSIS_LINEA + ":Double,"
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ SiembraLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	@Transient
	public SiembraItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		
		SiembraItem siembraItem = new SiembraItem(next);
		super.constructFeatureContainerStandar(siembraItem,next,newIDS);
	//	System.out.println("Attributes: "+next.getType().getTypes());
		//next.getType().getTypes().
		//TODO si tiene semillas cada 10mts calcular la dosis en kg segun en ancho y el peso de la semilla seleccionada
		//TODO si tiene dosis semilla calcular las semillas por metro
		//siembraItem.setDosisML(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_SEMILLA_ML))/10);
		if(siembraItem.getDosisML()!=0.0) {
			//Double semillasMetro = bolsasHa*(ProyectionConstants.METROS2_POR_HA/entreSurco.get())/semillasPorBolsa.get();
			Double dosisSemillakgHa = siembraItem.getDosisML()
					*ProyectionConstants.METROS2_POR_HA
					*(semilla.getPesoDeMil()/(1000*1000))/entreSurco;
			siembraItem.setDosisHa(dosisSemillakgHa);
		} else {
			siembraItem.setDosisHa( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_KG_SEMILLA)));	
			
			Double kgM2 = siembraItem.getDosisHa()/ProyectionConstants.METROS2_POR_HA;//kg/m2
			double semM2= (1000*1000*kgM2)/semilla.getPesoDeMil();//sem/m2
		//	)*(1000*1000))*entreSurco;
			siembraItem.setDosisML(semM2*entreSurco);// 1/entresurco=ml/m2 => sem/m2
		}
		
		siembraItem.setDosisFertLinea( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_LINEA)));
		siembraItem.setDosisFertCostado( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_COSTADO)));
		setPropiedadesLabor(siembraItem);
		return siembraItem;
	}
	
	public void setPropiedadesLabor(SiembraItem si){
		si.setLabor(this);
		si.setPrecioInsumo(this.getPrecioInsumo());
		si.setCostoLaborHa(this.getPrecioLabor());	
	}

	@Override
	@Transient
	public SiembraItem constructFeatureContainer(SimpleFeature next) {
		SiembraItem si = new SiembraItem(next);
		super.constructFeatureContainer(si,next);

		si.setDosisHa(LaborItem.getDoubleFromObj(next.getAttribute(colDosisSemilla.get())));	
			
		Double kgM2 = si.getDosisHa()/ProyectionConstants.METROS2_POR_HA;//kg/m2
		double semM2= (1000*1000*kgM2)/semilla.getPesoDeMil();//sem/m2
		
		si.setDosisML(semM2*entreSurco);// 1/entresurco=ml/m2 => sem/m2
		
		si.setDosisFertLinea( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_LINEA)));
		si.setDosisFertCostado( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_COSTADO)));
		setPropiedadesLabor(si);
		return si;
	}

	public void constructClasificador() {
		super.constructClasificador(config.getConfigProperties()
				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.clasficicadores[0]));
	}

	@Transient
	public SiembraConfig getConfiguracion() {
		return (SiembraConfig) config;
	}
	
	@Transient
	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_KG_SEMILLA);		
		return requiredColumns;
	}

	@Override
	protected Double initPrecioLaborHa() {
		return PropertyHelper.initDouble(SiembraLabor.COSTO_LABOR_SIEMBRA,"0",config.getConfigProperties());
	}


	@Override
	protected Double initPrecioInsumo() {
		return PropertyHelper.initDouble(SiembraLabor.COLUMNA_PRECIO_SEMILLA, "0", config.getConfigProperties()); 
		//return initDoubleProperty(Margen.COSTO_TN_KEY,  "0", );
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}
	

	
	public Double getCantLabor(ToDoubleFunction<SiembraItem> get) {
		Double ret = 0.0;
		//Geometry contorno = GeometryHelper.extractContornoGeometry(this);
		
		List<SiembraItem> items = this.cachedOutStoreQuery(this.outCollection.getBounds());
		try {
		ret = items.parallelStream().collect(
				()->0.0,
				(d,i)->d+=get.applyAsDouble(i),
				(d1,d2)->d1+=d2 
				);
		}catch(Exception e){
			e.printStackTrace();
		}
		return ret;

	}
	
	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new SiembraConfig();
		}
		return config;
	}
}
