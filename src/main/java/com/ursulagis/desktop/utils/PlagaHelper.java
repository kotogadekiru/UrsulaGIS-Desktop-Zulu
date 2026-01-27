package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Plaga;
import com.ursulagis.desktop.gui.Messages;

public class PlagaHelper {
	
	/**
	 * Retorna un mapa con las principales plagas y sus productos registrados para tratamiento
	 * @return Map con las plagas por defecto y sus agroquímicos asociados
	 */
	public static Map<String, Plaga> getPlagasDefault() {
		HashMap<String, Plaga> plagas = new HashMap<String, Plaga>();
		
		// Obtener los agroquímicos disponibles
		Map<String, Agroquimico> agroquimicos = AgroquimicoHelper.getAgroquimicosDefault();
		
		// PLAGAS DE INSECTOS
		
		// Orugas (Spodoptera, Helicoverpa, etc.)
		Plaga orugas = new Plaga(Messages.getString("Plaga.Nombre.Orugas"));
		orugas.setUmbralDanio(5.0);
		orugas.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosOrugas = new ArrayList<Agroquimico>();
		agroquimicosOrugas.add(agroquimicos.get("Coragen(lts)"));
		agroquimicosOrugas.add(agroquimicos.get("Abamectina 1,8(lts)"));
		agroquimicosOrugas.add(agroquimicos.get("Opera(lts)"));
		orugas.setAgroquimicosRegistrados(agroquimicosOrugas);
		plagas.put("Orugas", orugas);
		
		// Pulgones
		Plaga pulgones = new Plaga(Messages.getString("Plaga.Nombre.Pulgones"));
		pulgones.setUmbralDanio(10.0);
		pulgones.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosPulgones = new ArrayList<Agroquimico>();
		agroquimicosPulgones.add(agroquimicos.get("Dinotefuran(lts)"));
		agroquimicosPulgones.add(agroquimicos.get("Abamectina 1,8(lts)"));
		pulgones.setAgroquimicosRegistrados(agroquimicosPulgones);
		plagas.put("Pulgones", pulgones);
		
		// Trips
		Plaga trips = new Plaga(Messages.getString("Plaga.Nombre.Trips"));
		trips.setUmbralDanio(3.0);
		trips.setUnidadUmbralDanio("individuos/m²");
		List<Agroquimico> agroquimicosTrips = new ArrayList<Agroquimico>();
		agroquimicosTrips.add(agroquimicos.get("Abamectina 1,8(lts)"));
		agroquimicosTrips.add(agroquimicos.get("Opera(lts)"));
		trips.setAgroquimicosRegistrados(agroquimicosTrips);
		plagas.put("Trips", trips);
		
		// Mosca blanca
		Plaga moscaBlanca = new Plaga(Messages.getString("Plaga.Nombre.MoscaBlanca"));
		moscaBlanca.setUmbralDanio(5.0);
		moscaBlanca.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosMoscaBlanca = new ArrayList<Agroquimico>();
		agroquimicosMoscaBlanca.add(agroquimicos.get("Dinotefuran(lts)"));
		agroquimicosMoscaBlanca.add(agroquimicos.get("Abamectina 1,8(lts)"));
		moscaBlanca.setAgroquimicosRegistrados(agroquimicosMoscaBlanca);
		plagas.put("MoscaBlanca", moscaBlanca);
		
		// Ácaros
		Plaga acaros = new Plaga(Messages.getString("Plaga.Nombre.Acaros"));
		acaros.setUmbralDanio(5.0);
		acaros.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosAcaros = new ArrayList<Agroquimico>();
		agroquimicosAcaros.add(agroquimicos.get("Abamectina 1,8(lts)"));
		acaros.setAgroquimicosRegistrados(agroquimicosAcaros);
		plagas.put("Acaros", acaros);
		
		// PLAGAS DE MALEZAS
		
		// Malezas de hoja ancha
		Plaga malezasHojaAncha = new Plaga(Messages.getString("Plaga.Nombre.MalezasHojaAncha"));
		malezasHojaAncha.setUmbralDanio(10.0);
		malezasHojaAncha.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosMalezasAncha = new ArrayList<Agroquimico>();
		agroquimicosMalezasAncha.add(agroquimicos.get("RoundUp(lts)"));
		agroquimicosMalezasAncha.add(agroquimicos.get("Glifosato 66%(lts)"));
		agroquimicosMalezasAncha.add(agroquimicos.get("Atrazina(lts)"));
		agroquimicosMalezasAncha.add(agroquimicos.get("Fomesafen(lts)"));
		agroquimicosMalezasAncha.add(agroquimicos.get("Benazolin(lts)"));
		malezasHojaAncha.setAgroquimicosRegistrados(agroquimicosMalezasAncha);
		plagas.put("MalezasHojaAncha", malezasHojaAncha);
		
		// Malezas de hoja angosta (gramíneas)
		Plaga malezasHojaAngosta = new Plaga(Messages.getString("Plaga.Nombre.MalezasHojaAngosta"));
		malezasHojaAngosta.setUmbralDanio(10.0);
		malezasHojaAngosta.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosMalezasAngosta = new ArrayList<Agroquimico>();
		agroquimicosMalezasAngosta.add(agroquimicos.get("Cletodim(lts)"));
		agroquimicosMalezasAngosta.add(agroquimicos.get("Haloxifop 90% (Galant max)(lts)"));
		agroquimicosMalezasAngosta.add(agroquimicos.get("RoundUp(lts)"));
		agroquimicosMalezasAngosta.add(agroquimicos.get("Glifosato 66%(lts)"));
		malezasHojaAngosta.setAgroquimicosRegistrados(agroquimicosMalezasAngosta);
		plagas.put("MalezasHojaAngosta", malezasHojaAngosta);
		
		// Malezas resistentes
		Plaga malezasResistentes = new Plaga(Messages.getString("Plaga.Nombre.MalezasResistentes"));
		malezasResistentes.setUmbralDanio(5.0);
		malezasResistentes.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosMalezasResistentes = new ArrayList<Agroquimico>();
		agroquimicosMalezasResistentes.add(agroquimicos.get("Fomesafen(lts)"));
		agroquimicosMalezasResistentes.add(agroquimicos.get("Benazolin(lts)"));
		agroquimicosMalezasResistentes.add(agroquimicos.get("Atrazina(lts)"));
		malezasResistentes.setAgroquimicosRegistrados(agroquimicosMalezasResistentes);
		plagas.put("MalezasResistentes", malezasResistentes);
		
		// PLAGAS DE ENFERMEDADES
		
		// Enfermedades fúngicas
		Plaga enfermedadesFungicas = new Plaga(Messages.getString("Plaga.Nombre.EnfermedadesFungicas"));
		enfermedadesFungicas.setUmbralDanio(5.0);
		enfermedadesFungicas.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosFungicas = new ArrayList<Agroquimico>();
		agroquimicosFungicas.add(agroquimicos.get("Rizospray extremo(lts)"));
		enfermedadesFungicas.setAgroquimicosRegistrados(agroquimicosFungicas);
		plagas.put("EnfermedadesFungicas", enfermedadesFungicas);
		
		// Roya
		Plaga roya = new Plaga(Messages.getString("Plaga.Nombre.Roya"));
		roya.setUmbralDanio(5.0);
		roya.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosRoya = new ArrayList<Agroquimico>();
		agroquimicosRoya.add(agroquimicos.get("Rizospray extremo(lts)"));
		roya.setAgroquimicosRegistrados(agroquimicosRoya);
		plagas.put("Roya", roya);
		
		// Oídio
		Plaga oidio = new Plaga(Messages.getString("Plaga.Nombre.Oidio"));
		oidio.setUmbralDanio(5.0);
		oidio.setUnidadUmbralDanio("%");
		List<Agroquimico> agroquimicosOidio = new ArrayList<Agroquimico>();
		agroquimicosOidio.add(agroquimicos.get("Rizospray extremo(lts)"));
		oidio.setAgroquimicosRegistrados(agroquimicosOidio);
		plagas.put("Oidio", oidio);
		
		// PLAGAS ESPECÍFICAS COMUNES
		
		// Cogollero del maíz
		Plaga cogollero = new Plaga(Messages.getString("Plaga.Nombre.Cogollero"));
		cogollero.setUmbralDanio(3.0);
		cogollero.setUnidadUmbralDanio("individuos/m²");
		List<Agroquimico> agroquimicosCogollero = new ArrayList<Agroquimico>();
		agroquimicosCogollero.add(agroquimicos.get("Coragen(lts)"));
		agroquimicosCogollero.add(agroquimicos.get("Abamectina 1,8(lts)"));
		cogollero.setAgroquimicosRegistrados(agroquimicosCogollero);
		plagas.put("Cogollero", cogollero);
		
		// Bolillero
		Plaga bolillero = new Plaga(Messages.getString("Plaga.Nombre.Bolillero"));
		bolillero.setUmbralDanio(3.0);
		bolillero.setUnidadUmbralDanio("individuos/m²");
		List<Agroquimico> agroquimicosBolillero = new ArrayList<Agroquimico>();
		agroquimicosBolillero.add(agroquimicos.get("Coragen(lts)"));
		agroquimicosBolillero.add(agroquimicos.get("Opera(lts)"));
		bolillero.setAgroquimicosRegistrados(agroquimicosBolillero);
		plagas.put("Bolillero", bolillero);
		
		// Chinche
		Plaga chinche = new Plaga(Messages.getString("Plaga.Nombre.Chinche"));
		chinche.setUmbralDanio(2.0);
		chinche.setUnidadUmbralDanio("individuos/m²");
		List<Agroquimico> agroquimicosChinche = new ArrayList<Agroquimico>();
		agroquimicosChinche.add(agroquimicos.get("Dinotefuran(lts)"));
		agroquimicosChinche.add(agroquimicos.get("Abamectina 1,8(lts)"));
		chinche.setAgroquimicosRegistrados(agroquimicosChinche);
		plagas.put("Chinche", chinche);
		
		return plagas;
	}
}
