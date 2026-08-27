package com.ursulagis.desktop.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;

import com.ursulagis.desktop.dao.config.Configuracion;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import java.util.logging.Logger;
/**
 * Clase que toma un archivo messages_[locale].properties y lo traduce al locale indicado usando google translator
 */
public class GoogleTranslatorHelper extends ProgresibleTask<File>{
	private static final Logger logger = Logger.getLogger(GoogleTranslatorHelper.class.getName());

	private static String key ="AIzaSyC6m54rSOpbe5Tar_b2O2XWGkxCn7BImnU";
	private static String project = "UrsulaGIS";
	private static String GEOCODE_API_GOOGLE_URL ="https://maps.googleapis.com/maps/api/geocode/json";//?address=";
	private static String TRANSLATE_API_GOOGLE_URL = "https://translation.googleapis.com/language/translate/v2";

	/** Locale codes not supported by Google Translate API -> fallback code to use for translation. */
	private static final Map<String, String> UNSUPPORTED_TO_FALLBACK = new HashMap<>();
	static {
		UNSUPPORTED_TO_FALLBACK.put("ast", "es");  // Asturian -> Spanish
		UNSUPPORTED_TO_FALLBACK.put("gv", "en");   // Manx -> English
		UNSUPPORTED_TO_FALLBACK.put("kw", "en");   // Cornish -> English
		UNSUPPORTED_TO_FALLBACK.put("sco", "en"); // Scots -> English
	}

	/** Language codes (ISO 639-1 or equivalent) supported by Google Cloud Translation API for UI locale selection. */
	private static final Set<String> SUPPORTED_LANGUAGE_CODES = Set.of(
			"af", "sq", "am", "ar", "hy", "az", "be", "bn", "bs", "bg", "my", "ca", "zh", "hr", "cs", "da", "nl", "en", "et",
			"fil", "fi", "fr", "fy", "gl", "ka", "de", "el", "gn", "gu", "ha", "he", "iw", "hi", "hu", "is", "ig", "id", "ga",
			"it", "ja", "kn", "km", "ko", "ky", "lo", "lv", "ln", "lt", "lb", "mk", "ms", "ml", "mt", "mr", "mn", "ne", "nb", "no",
			"fa", "pl", "pt", "pa", "ro", "ru", "gd", "sr", "sk", "sl", "so", "es", "sw", "sv", "tl", "tg", "ta", "te", "th",
			"tr", "uk", "ur", "uz", "vi", "cy", "zu"
	);

	/** Returns the set of language codes supported by Google Translate (for locale/language selection). */
	public static Set<String> getSupportedLanguageCodes() {
		return SUPPORTED_LANGUAGE_CODES;
	}

	private ResourceBundle baseBoundle=null;
	private Locale outLocale=null;

	public GoogleTranslatorHelper(ResourceBundle _baseBoundle,Locale _outLocale) {
		this.taskName="Traduciendo el archivo messages_"+_outLocale.getLanguage()+".properties";
		this.baseBoundle=_baseBoundle;
		this.outLocale=_outLocale;
	}

	// public static Position obtenerPositionDirect(String query){
	// 	GenericUrl url = new GenericUrl(GEOCODE_API_GOOGLE_URL);
	// 	url.put("address", query);
	// 	url.put("key", key);
	// 	System.out.println("buscando la traduccion de "+query+" con el url \n"+url);
	// 	HttpResponse response = makeRequest(url);
	// 	try {
	// 		return 	parseGeoCodeResponse(response);
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 		return null;
	// 	}


	// }

	// private static Position parseGeoCodeResponse(HttpResponse response) throws IOException {
	// 	GenericJson content = response.parseAs(GenericJson.class);
	// 	System.out.println("response content:\n"+content);

	// 	//{"results":[{"address_components":[{"long_name":"Pehuaj�","short_name":"Pehuaj�","types":["locality","political"]},{"long_name":"Pehuaj� Partido","short_name":"Pehuaj� Partido","types":["administrative_area_level_2","political"]},{"long_name":"Buenos Aires Province","short_name":"Buenos Aires Province","types":["administrative_area_level_1","political"]},{"long_name":"Argentina","short_name":"AR","types":["country","political"]}],"formatted_address":"Pehuaj�, Buenos Aires Province, Argentina","geometry":{"bounds":{"northeast":{"lat":-35.7909625,"lng":-61.8469892},"southwest":{"lat":-35.8613171,"lng":-61.9405142}},"location":{"lat":-35.8107166,"lng":-61.8987832},"location_type":"APPROXIMATE","viewport":{"northeast":{"lat":-35.7909625,"lng":-61.8469892},"southwest":{"lat":-35.8613171,"lng":-61.9405142}}},"place_id":"ChIJ86BrWCz4wJURA89cs7G_REg","types":["locality","political"]}],"status":"OK"}
	// 	ArrayMap<String,Object> data = (ArrayMap<String,Object>) content.getUnknownKeys();
	// 	for(String key :data.keySet()){
	// 		ArrayList<Object> val =(ArrayList<Object>) data.get(key);
	// 		for(Object o:val){
	// 			System.out.println("object: "+o);

	// 			ArrayMap<String,Object> valMap = (ArrayMap<String,Object>) o;
	// 			ArrayMap<String,Object> geometry = (ArrayMap<String, Object>)valMap.get("geometry");
	// 			ArrayMap<String,Object> location = (ArrayMap<String, Object>)geometry.get("location");
	// 			BigDecimal lat = (BigDecimal) location.get("lat");
	// 			BigDecimal lng = (BigDecimal) location.get("lng");

	// 			//			    "lat" : 37.4224764,
	// 			//	            "lng" : -122.0842499
	// 			System.out.println("lat: "+lat+ " lon: "+lng);
	// 			return Position.fromDegrees(lat.doubleValue(), lng.doubleValue());
	// 		}

	// 	}
	// 	return null;
	// }

	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeRequest(GenericUrl url){
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			return request.execute();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}	
	}
	/**
	 * Translates text using Google Cloud Translate REST API with API key
	 * @param s text to translate
	 * @return s traducido al outLocale
	 */
	private String traducir(String s) {
		if (s == null || s.trim().isEmpty()) {
			return s;
		}
		
		try {
			// Use Google Cloud Translate REST API v2 with API key
			String targetLang = outLocale.getLanguage();
			String fallback = UNSUPPORTED_TO_FALLBACK.get(targetLang.toLowerCase(Locale.ROOT));
			if (fallback != null) {
				targetLang = fallback;
			}
			GenericUrl url = new GenericUrl(TRANSLATE_API_GOOGLE_URL);
			url.put("key", key);
			url.put("q", s);
			url.put("target", targetLang);
			url.put("format", "text");
			
			HttpResponse response = makeRequest(url);
			if (response == null) {
				logger.warning("Failed to get response from Google Translate API");
				return s;
			}
			
			GenericJson content = response.parseAs(GenericJson.class);
			ArrayMap<String, Object> data = (ArrayMap<String, Object>) content.getUnknownKeys();
			
			// Parse response: {"data": {"translations": [{"translatedText": "..."}]}}
			ArrayMap<String, Object> dataMap = (ArrayMap<String, Object>) data.get("data");
			if (dataMap != null) {
				ArrayList<Object> translations = (ArrayList<Object>) dataMap.get("translations");
				if (translations != null && !translations.isEmpty()) {
					ArrayMap<String, Object> translation = (ArrayMap<String, Object>) translations.get(0);
					String translatedText = (String) translation.get("translatedText");
					if (translatedText != null) {
						return translatedText;
					}
				}
			}
			
			logger.warning("Failed to parse translation response");
			return s;
		} catch (Exception e) {
			logger.warning("Error translating with Google Cloud Translate API: " + e.getMessage());
			e.printStackTrace();
			// Fallback: return original text if translation fails
			return s;
		}
	}

	@Override
	protected File call() {
		// TODO generate messages_[loc].properties File
		//Configuracion config = Configuracion.getInstance();
		String fileName = Configuracion.ursulaGISFolder+"\\messages_"+outLocale.getLanguage()+".properties";
		logger.fine("writing file "+fileName);
		File ret = new File(fileName);
		//todo recorrer outlocale 
		Enumeration<String> keys = baseBoundle.getKeys();
		List<String> keyList = Collections.list(keys);
		super.updateProgress(0, keyList.size());			
		try (OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(ret), StandardCharsets.UTF_8)) {
			// Use sequential stream since we're writing to a file
			// Process each key and write the translated entry
			for (String key : keyList) {
				String value = baseBoundle.getString(key);
				String translatedValue = traducir(value);	
				writer.write(key + "=" + translatedValue + "\n");
				super.updateProgress(keyList.indexOf(key)+1,keyList.size());
			}
		} catch (Exception e) {
			logger.warning("Error writing translation file: " + e.getMessage());
			e.printStackTrace();
		}
		//traducir lo que viene despues del =
		//insertar la linea en ret
		return ret;
	}

	public static void main(String[] args) {
		Locale loc = new Locale("DE");
		ResourceBundle baseBoundle1 = Messages.getBoundle();
		GoogleTranslatorHelper t = new GoogleTranslatorHelper(baseBoundle1,loc);
		t.call();
	}

}



/**
  from deep_translator import GoogleTranslator

# Funci�n para traducir las l�neas que no han sido traducidas
def auto_translate_line(line):
    if "=" in line:
        key, value = line.split("=", 1)
        translated_value = translations.get(value.strip())
        if not translated_value:
            translated_value = GoogleTranslator(source='es', target='fr').translate(value.strip())
        return f"{key}={translated_value}\n"
    return line

# Re-traducir todo el contenido incluyendo las traducciones autom�ticas
translated_content_auto = ''.join([auto_translate_line(line) for line in content.splitlines()])

# Guardar el contenido completamente traducido en un nuevo archivo
translated_file_path_auto = '/mnt/data/messages_fr_auto.properties'
with open(translated_file_path_auto, 'w', encoding='ISO-8859-1') as file:
    file.write(translated_content_auto)

translated_file_path_auto

 */
