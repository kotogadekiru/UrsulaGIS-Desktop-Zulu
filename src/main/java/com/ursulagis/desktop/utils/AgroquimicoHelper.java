package com.ursulagis.desktop.utils;

import java.util.HashMap;
import java.util.Map;

import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.gui.Messages;

public class AgroquimicoHelper {
	private static final String 	ROUND_UP_LTS = "RoundUp(lts)";
	private static final String 	SUPERWET_LTS = "Superwet(lts)";
	private static final String 	ATRAZINA_LTS = "Atrazina(lts)";
	private static final String 	CLETODIM_LTS = "Cletodim(lts)";
	private static final String 	RIZOSPRAY_EXTREMO_LTS = "Rizospray extremo(lts)";
	private static final String 	BENAZOLIN_LTS = "Benazolin(lts)";
	private static final String 	FOMESAFEN_LTS = "Fomesafen(lts)";
	private static final String 	GLIFOSATO_66_LTS = "Glifosato 66%(lts)";
	private static final String 	DINOTEFURAN_LTS = "Dinotefuran(lts)";
	private static final String 	ABAMECTINA_1_8_LTS = "Abamectina 1,8(lts)";
	private static final String 	CORAGEN_LTS = "Coragen(lts)";
	private static final String 	OPERA_LTS = "Opera(lts)";
	private static final String 	HALOXIFOP_90_GALANT_MAX_LTS = "Haloxifop 90% (Galant max)(lts)";

	public static Map<String,Agroquimico> getAgroquimicosDefault(){
		HashMap<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
		agroquimicos.put(ROUND_UP_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.RoundUp")));	
		agroquimicos.put(SUPERWET_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Superwet")));
		agroquimicos.put(ATRAZINA_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Atrazina")));
		agroquimicos.put(CLETODIM_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Cletodim")));
		
		agroquimicos.put(RIZOSPRAY_EXTREMO_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.RizosprayExtremo")));
		agroquimicos.put(BENAZOLIN_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Benazolin")));
		agroquimicos.put(FOMESAFEN_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Fomesafen")));
		agroquimicos.put(GLIFOSATO_66_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Glifosato66")));
		agroquimicos.put(DINOTEFURAN_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Dinotefuran")));
		agroquimicos.put(ABAMECTINA_1_8_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Abamectina18")));
		agroquimicos.put(CORAGEN_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Coragen")));
		agroquimicos.put(OPERA_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Opera")));		
		agroquimicos.put(HALOXIFOP_90_GALANT_MAX_LTS,new Agroquimico(Messages.getString("Agroquimico.Nombre.Haloxifop90GalantMax")));	
		return agroquimicos;
	}	

}
