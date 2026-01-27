package com.ursulagis.desktop.dao.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@NamedQueries({
	@NamedQuery(name=Plaga.FIND_ALL, query="SELECT p FROM Plaga p ORDER BY lower(p.nombre)"),
	@NamedQuery(name=Plaga.FIND_NAME, query="SELECT p FROM Plaga p where p.nombre = :name"),
}) 
public class Plaga implements Comparable<Plaga> {
	public static final String FIND_ALL = "Plaga.findAll";
	public static final String FIND_NAME = "Plaga.findName";
	
	@Id @GeneratedValue
	private Long id = null;
	
	private String nombre = new String();
	
	// Umbral de daño (porcentaje o valor numérico según el contexto)
	private Double umbralDanio = 0.0;
	
	// Agroquímicos registrados para el tratamiento de esta plaga
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
		name = "plaga_agroquimico",
		joinColumns = @JoinColumn(name = "plaga_id"),
		inverseJoinColumns = @JoinColumn(name = "agroquimico_id")
	)
	private List<Agroquimico> agroquimicosRegistrados = new ArrayList<Agroquimico>();
	
	public Plaga() {
	}
	
	public Plaga(String nombre) {
		this.nombre = nombre;
	}
	
	@Override
	public int compareTo(Plaga arg0) {
		if (arg0 != null && arg0.nombre != null) {
			return this.nombre.compareTo(arg0.nombre);
		}
		return -1;
	}
	
	@Override
	public String toString() {
		return nombre;
	}
	
	/**
	 * Retorna una representación en string de los agroquímicos registrados
	 * para ser usado en tablas y visualizaciones
	 */
	public String getAgroquimicosRegistradosString() {
		if(agroquimicosRegistrados == null || agroquimicosRegistrados.isEmpty()) {
			return "";
		}
		return agroquimicosRegistrados.stream()
				.map(Agroquimico::getNombre)
				.collect(java.util.stream.Collectors.joining(", "));
	}
}
