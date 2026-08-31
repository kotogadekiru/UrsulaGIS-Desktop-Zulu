package com.ursulagis.desktop.tasks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.api.StandardResponse;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.DAH;


import java.util.logging.Logger;
public class UpdateRecorridaTask extends Task<String> {
	private static final Logger logger = Logger.getLogger(UpdateRecorridaTask.class.getName());



	//private static final String GET_RECORRIDAS_BY_ID_URL = "https://www.ursulagis.com/api/recorridas/id/";
	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";

	//public static final String BASE_URL="https://sheltered-mesa-69562-dev-514e4d674053.herokuapp.com/";
	//public static final String BASE_URL="http://localhost:5000/";
	public static final String BASE_URL="https://www.ursulagis.com";
	private static final String API_RECORRIDAS_UUID = "/api/recorridas/uuid/";
	/** Mismo endpoint que usa la pagina web para listar muestras (altas/bajas). */
	private static final String API_GET_MUESTRAS = "/api/recorridas/getMuestras/";
	//public static final String DOWNLOAD_URL = BASE_URL+API_RECORRIDAS_UUID;
	//public static final String INSERT_URL = "http://localhost:5000/api/recorridas/insert/";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;


	private Recorrida recorrida =null;


	public UpdateRecorridaTask(Recorrida recorrida) {
		this.recorrida = recorrida;

		logger.fine("actualizando recorrida "+recorrida.getNombre());
		logger.fine("muestras "+recorrida.getMuestras().size());
	}

	@Override
	protected String call()  {
		try {
			// TODO call www.ursulagis.com/api/recorridas/insert/
			String baseUlr = getBaseUrl();

			GenericUrl url = new GenericUrl(baseUlr+API_RECORRIDAS_UUID+this.recorrida.getUuid());	

			HttpResponse response = makeGetRequest(url);
			InputStream resContent = response.getContent();
			Reader reader = new InputStreamReader(resContent);

			StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
			//System.out.println("standarResponse = "+standarResponse);

			StandardResponse.StatusResponse status = standarResponse.getStatus();
			//System.out.println("response status = "+status);
			if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {
				//com.google.api.client.util.ArrayMap data =(ArrayMap) resContent.get("data");
				JsonElement data = standarResponse.getData();

				if(data !=null) {
					Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies( getJSonStrategy()).create();
					Recorrida remoteRecorrida = gson.fromJson(data, Recorrida.class);

					String dbUrl = remoteRecorrida.getUrl();
					recorrida.setUrl(dbUrl);
					recorrida.setNombre(remoteRecorrida.getNombre());
					recorrida.setJsonAmb(remoteRecorrida.getJsonAmb());
					recorrida.setLatitude(remoteRecorrida.getLatitude());
					recorrida.setLongitude(remoteRecorrida.getLongitude());
					recorrida.setObservacion(remoteRecorrida.getObservacion());

					// La pagina web carga muestras desde getMuestras/{id}/; el GET por uuid suele devolver [].
					Long remoteId = extractRemoteId(data);
					List<Muestra> remoteMuestras = fetchRemoteMuestras(baseUlr, remoteId, gson);
					if (remoteMuestras == null) {
						remoteMuestras = extractRemoteMuestras(gson, data, remoteRecorrida);
						if (remoteMuestras == null || remoteMuestras.isEmpty()) {
							logger.warning("No se pudieron obtener muestras remotas; se conservan las locales");
						} else {
							applyRemoteMuestras(recorrida, remoteMuestras);
						}
					} else {
						applyRemoteMuestras(recorrida, remoteMuestras);
					}

					String urlGoto =dbUrl;// GET_RECORRIDAS_BY_ID_URL+id+"/";
					return urlGoto;
				}
				return "status Success but data null";
			} else {//status is not Success
				String message =standarResponse.getMessage();
				return status+" "+message;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String getBaseUrl() {
		String baseUlr=BASE_URL;
		try {
			URL requestUrl = new URL(recorrida.getUrl());			
			String portString = requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
			baseUlr = requestUrl.getProtocol() + "://" + requestUrl.getHost() +portString;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return baseUlr;
	}

	private static Long extractRemoteId(JsonElement data) {
		if (data == null || !data.isJsonObject()) {
			return null;
		}
		JsonElement idEl = data.getAsJsonObject().get("id");
		if (idEl == null || idEl.isJsonNull() || !idEl.isJsonPrimitive()) {
			return null;
		}
		try {
			return idEl.getAsLong();
		} catch (Exception e) {
			logger.warning("No se pudo leer id remoto de recorrida: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Descarga muestras desde el mismo endpoint que usa la pagina web.
	 * @return lista (posiblemente vacia) si el request fue OK; null si fallo
	 */
	private List<Muestra> fetchRemoteMuestras(String baseUrl, Long remoteId, Gson gson) {
		if (remoteId == null) {
			return null;
		}
		GenericUrl url = new GenericUrl(baseUrl + API_GET_MUESTRAS + remoteId + "/");
		HttpResponse response = makeGetRequest(url);
		if (response == null) {
			return null;
		}
		try (Reader reader = new InputStreamReader(response.getContent())) {
			JsonElement root = com.google.gson.JsonParser.parseReader(reader);
			JsonElement arrayEl = root;
			if (root.isJsonObject()) {
				JsonObject obj = root.getAsJsonObject();
				if (obj.has("data")) {
					arrayEl = obj.get("data");
				} else if (obj.has("muestras")) {
					arrayEl = obj.get("muestras");
				}
			}
			if (arrayEl == null || !arrayEl.isJsonArray()) {
				logger.warning("getMuestras no devolvio un array: " + root);
				return null;
			}
			List<Muestra> muestras = new ArrayList<>();
			for (JsonElement el : arrayEl.getAsJsonArray()) {
				muestras.add(parseRemoteMuestra(gson, el));
			}
			logger.fine("muestras remotas via getMuestras: " + muestras.size());
			return muestras;
		} catch (Exception e) {
			logger.warning("Error leyendo getMuestras/" + remoteId + ": " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static Muestra parseRemoteMuestra(Gson gson, JsonElement el) {
		Muestra muestra = gson.fromJson(el, Muestra.class);
		if (muestra == null) {
			muestra = new Muestra();
		}
		if (el != null && el.isJsonObject()) {
			JsonObject obj = el.getAsJsonObject();
			// La web suele enriquecer observacion en observacionJSON
			JsonElement obsJson = obj.get("observacionJSON");
			if (obsJson != null && !obsJson.isJsonNull()) {
				muestra.setObservacion(obsJson.isJsonPrimitive() ? obsJson.getAsString() : obsJson.toString());
			} else if (obj.has("observacion") && obj.get("observacion").isJsonObject()) {
				muestra.setObservacion(obj.get("observacion").toString());
			}
		}
		return muestra;
	}

	private static List<Muestra> extractRemoteMuestras(Gson gson, JsonElement data, Recorrida remoteRecorrida) {
		List<Muestra> muestras = remoteRecorrida.getMuestras();
		logger.fine("muestras remotas embebidas "+muestras);
		if (muestras != null && !muestras.isEmpty()) {
			return muestras;
		}
		if (data == null || !data.isJsonObject()) {
			return muestras != null ? muestras : new ArrayList<>();
		}
		JsonObject obj = data.getAsJsonObject();
		Type listType = new TypeToken<List<Muestra>>() {}.getType();
		for (String key : new String[] { "muestras", "Muestras" }) {
			if (obj.has(key) && obj.get(key).isJsonArray() && !obj.get(key).getAsJsonArray().isEmpty()) {
				return gson.fromJson(obj.get(key), listType);
			}
		}
		return muestras != null ? muestras : new ArrayList<>();
	}

	/**
	 * Reemplaza la lista local con la remota: agrega nuevas, actualiza existentes y
	 * elimina las que ya no estan en el servidor (orphanRemoval al guardar).
	 */
	private static void applyRemoteMuestras(Recorrida recorrida, List<Muestra> remoteMuestras) {
		if (remoteMuestras == null) {
			remoteMuestras = new ArrayList<>();
		}
		Map<String, Muestra> localByKey = new LinkedHashMap<>();
		for (Muestra m : recorrida.getMuestras()) {
			localByKey.put(muestraKey(m), m);
		}
		recorrida.getMuestras().clear();
		for (Muestra remote : remoteMuestras) {
			Muestra muestra = localByKey.remove(muestraKey(remote));
			if (muestra == null) {
				muestra = new Muestra();
			}
			muestra.setNombre(remote.getNombre());
			muestra.setSubNombre(remote.getSubNombre());
			muestra.setObservacion(remote.getObservacion());
			muestra.setLatitude(remote.getLatitude());
			muestra.setLongitude(remote.getLongitude());
			muestra.setRecorrida(recorrida);
			recorrida.getMuestras().add(muestra);
		}
		logger.fine("muestras locales tras sync: " + recorrida.getMuestras().size()
				+ " (remotas=" + remoteMuestras.size() + ", huerfanas locales=" + localByKey.size() + ")");
	}

	private static String muestraKey(Muestra m) {
		String nombre = m.getNombre() == null ? "" : m.getNombre();
		String subNombre = m.getSubNombre() == null ? "" : m.getSubNombre();
		return nombre + "\0" + subNombre;
	}

	private ExclusionStrategy getJSonStrategy() {
		ExclusionStrategy strategy = new ExclusionStrategy() {
			@Override
			public boolean shouldSkipClass(Class<?> arg0) {
				return false;
			}
			@Override
			public boolean shouldSkipField(FieldAttributes arg0) {
				if (arg0.getAnnotation(ManyToOne.class) != null)return true;//no subo referencias circulares
				if (arg0.getAnnotation(Id.class) != null)return true;//no subo las ids locales al servidor

				return false;
			}
		};
		return strategy;
	}


	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeGetRequest(GenericUrl url){
		logger.fine("calling get "+url);
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			response= request.execute();
		} catch (Exception e) {			
			logger.warning("Fallo el getUrl "+url);
			e.printStackTrace();

			return null;
		}	
		return response;
	}



	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makePostRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						HttpHeaders headers = request.getHeaders();//USER=693,468
						headers.set("USER", Configuracion.getInstance().getPropertyOrDefault("USER", "nonefound"));


					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			//request.getHeaders().set("USER", getUser());
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}


	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Compartiendo Recorrida "+this.recorrida.getNombre());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			logger.fine("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(MMG_GUI_EVENT_CLOSE_PNG));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}
	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}

}
