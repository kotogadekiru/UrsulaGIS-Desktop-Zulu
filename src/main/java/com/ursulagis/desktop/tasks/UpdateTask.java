package com.ursulagis.desktop.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpIOExceptionHandler;
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

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class UpdateTask  extends Task<File>{
	private static final String DEFAULT_UPDATE_URL ="https://www.ursulagis.com/update/";//TODO cambiar a https
	//private static final String DEFAULT_UPDATE_URL = "http://localhost:5000/update/";
	/** Base URL for {@link #checkForUpdate()} (trailing slash). Overridden in tests via {@link #setUpdateUrlForTests}. */
	private static volatile String updateUrl = DEFAULT_UPDATE_URL;
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private static final Logger logger = Logger.getLogger(UpdateTask.class.getName());

	public static String lastVersionURL=null;
	private static boolean isUpdateAvailable=false;

	private static String lastVersionNumber;

	public File call()  {
		//lastVersionURL="http://s3-sa-east-1.amazonaws.com/ursulagis/downloads/UrsulaGIS0.2.18.jar";
		logger.fine("descargando: "+lastVersionURL);
		GenericUrl url = new GenericUrl(UpdateTask.lastVersionURL);
		HttpRequestFactory requestFactory = createRequestFactory();
		File fout=null;
		try {
			HttpRequest request = requestFactory.buildGetRequest(url);

			HttpResponse response = request.execute();
			GenericUrl reqUrl = response.getRequest().getUrl();
			List<String> parts = reqUrl.getPathParts();
			String fileName = parts.get(parts.size()-1);

			InputStream is = response.getContent();

			//ubico el archivo en appdata/ursulagis
			fout = new File(Configuracion.ursulaGISFolder+File.separator+fileName);

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fout));
			byte[] bytesIn = new byte[4096];
			int read = 0;
			HttpHeaders headers = response.getHeaders();
			//	System.out.println("headers: "+headers.getCacheControl());
			long ava = headers.getContentLength();
			int readTot=0;
			//			long iniT = System.currentTimeMillis();
			//			long nowT = iniT;
			//			double rate=0;

			NumberFormat dc = NumberFormat.getInstance();
			dc.setGroupingUsed(true);

			while ((read = readRetry(is, bytesIn)) != -1) {//java.net.SocketTimeoutException: Read timed out
				//		System.out.println("read ="+read);
				readTot+=read;
				//				nowT = System.currentTimeMillis();
				//				rate = read/(nowT-iniT+1);
				//				iniT=nowT;

				//	System.out.println("read "+dc.format(readTot)+" out of "+dc.format(ava)+" "+dc.format(rate)+" KB/s");

				super.updateProgress(readTot,ava);//readTot, ava);
				bos.write(bytesIn, 0, read);
			}

			is.close();
			bos.close();
			//Executes the specified string command in a separate process.

			// ProcessBuilder pb = new ProcessBuilder(fout.getPath());
			//pb.start();

		} catch (Exception e) {
			e.printStackTrace();
			//			bos.close();
			//			is.close();
			if(fout!=null)fout.delete();
			fout=null;
		} 
		if(fout!=null){
			instalarNuevaVersion(fout);

		}
		return fout;
	}

	private void instalarNuevaVersion(File fout) {
		String lowerName = fout.getName().toLowerCase(Locale.ROOT);
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		try {
			if (os.contains("win")) {
				installWindows(fout, lowerName);
			} else if (os.contains("mac") || os.contains("darwin")) {
				installMacos(fout, lowerName);
			} else if (os.contains("linux")) {
				installLinux(fout, lowerName);
			} else {
				openWithDefaultApplication(fout);
			}
		} catch (IOException e) {
			e.printStackTrace();
			openWithDefaultApplication(fout);
		}
	}

	/**
	 * Windows: MSI uses the same uninstall-then-install sequence as before (via {@code cmd /c});
	 * self-extracting EXE installers are launched directly.
	 */
	private void installWindows(File fout, String lowerName) throws IOException {
		if (lowerName.endsWith(".msi")) {
			String p = fout.getAbsolutePath();
			logger.fine("ejecutando instalador MSI: " + p);
			ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
					"msiexec.exe /x \"" + p + "\" /q /norestart && msiexec.exe /i \"" + p + "\"");
			pb.start();
		} else if (lowerName.endsWith(".exe")) {
			logger.fine("ejecutando instalador: " + fout.getAbsolutePath());
			ProcessBuilder pb = new ProcessBuilder(fout.getAbsolutePath());
			pb.directory(fout.getParentFile());
			pb.start();
		} else {
			openWithDefaultApplication(fout);
		}
	}

	/** macOS: {@code open} runs .pkg / .dmg with the default handler; other types fall back to Desktop. */
	private void installMacos(File fout, String lowerName) throws IOException {
		if (lowerName.endsWith(".pkg") || lowerName.endsWith(".dmg") || lowerName.endsWith(".zip")) {
			logger.fine("abriendo con open: " + fout.getAbsolutePath());
			new ProcessBuilder("open", fout.getAbsolutePath()).start();
		} else {
			openWithDefaultApplication(fout);
		}
	}

	/** Linux: AppImage is marked executable and run; packages are opened with the default handler (e.g. Software). */
	private void installLinux(File fout, String lowerName) throws IOException {
		if (lowerName.endsWith(".appimage")) {
			if (!fout.setExecutable(true)) {
				logger.warning("No se pudo marcar como ejecutable: " + fout);
			}
			logger.fine("ejecutando AppImage: " + fout.getAbsolutePath());
			new ProcessBuilder(fout.getAbsolutePath()).start();
			return;
		}
		try {
			logger.fine("abriendo con xdg-open: " + fout.getAbsolutePath());
			new ProcessBuilder("xdg-open", fout.getAbsolutePath()).start();
		} catch (IOException e) {
			openWithDefaultApplication(fout);
		}
	}

	private void openWithDefaultApplication(File fout) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				logger.fine("abriendo con Desktop: " + fout.getAbsolutePath());
				Desktop.getDesktop().open(fout);
			}
		} catch (IOException | HeadlessException e) {
			e.printStackTrace();
		}
	}


	private HttpRequestFactory createRequestFactory() {
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		//	JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						//	request.setParser(new JsonObjectParser(JSON_FACTORY));
						//request.setRetryOnExecuteIOException(true);
						request.setIOExceptionHandler(
								new HttpIOExceptionHandler(){
									@Override
									public boolean handleIOException(HttpRequest arg0, boolean arg1)
											throws IOException {
										return true;
									}

								});
						request.setContentLoggingLimit(0);
						request.setConnectTimeout(60000);
					//	request.setReadTimeout(60000);
					//	request.setNumberOfRetries(200);
					}
				});
		return requestFactory;
	}


	private int readRetry(InputStream is, byte[] bytesIn) {
		int read =-1;
		for(int i=0;i<10;i++){
			try{
				read= is.read(bytesIn);
				return read;
			}catch(Exception e){
				logger.fine("fallo read "+i);
			}
		}//fin del for trate de leer 10 veces.
		return -1;
	}


	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Actualizando");
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			logger.fine("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}

	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}

	/**
	 * Value for the {@code PLATFORM} update URL parameter: OS family and CPU word size
	 * (e.g. {@code windows_x64}, {@code mac_aarch64}, {@code linux_x64}).
	 */
	private static String platformQueryValue() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		if (osName.contains("windows")) {
			if ("amd64".equals(arch) || "x86_64".equals(arch)) {
				return "windows_x64";
			}
			if ("aarch64".equals(arch) || "arm64".equals(arch)) {
				return "windows_aarch64";
			}
			return "windows_x86";
		}
		if (osName.contains("mac") || osName.contains("darwin")) {
			if ("aarch64".equals(arch) || "arm64".equals(arch)) {
				return "mac_aarch64";
			}
			return "mac_x64";
		}
		if (osName.contains("linux")) {
			if ("aarch64".equals(arch) || "arm64".equals(arch)) {
				return "linux_aarch64";
			}
			if ("amd64".equals(arch) || "x86_64".equals(arch)) {
				return "linux_x64";
			}
			if ("i386".equals(arch) || "x86".equals(arch) || "i686".equals(arch)) {
				return "linux_x86";
			}
			return "linux_" + arch;
		}
		return "unknown";
	}

	public static String checkForUpdate() {		
		String message =null;
		//TODO si ya se habia invocado no volver a llamar.
		if(lastVersionNumber == null) {
			GenericUrl url = new GenericUrl(updateUrl);//"http://www.ursulagis.com/update");
			url.put("VERSION", JFXMain.VERSION);
			
			String usr = getUserNumber();
			url.put("USER", usr);
			url.put("PLATFORM", platformQueryValue());
			
			logger.fine("calling url=> "+url);
			//http://localhost:5000/update?VERSION=0.2.26&USER=693,468
			//http://www.ursulagis.com/update?VERSION=0.2.20
			HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
			JsonFactory JSON_FACTORY = new JacksonFactory();
			HttpRequestFactory requestFactory =
					HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
						@Override
						public void initialize(HttpRequest request) {
							request.setParser(new JsonObjectParser(JSON_FACTORY));
							//						  request.setConnectTimeout(0);
							//					      request.setReadTimeout(0);
						}
					});
	
			try {
				HttpRequest request = requestFactory.buildGetRequest(url);
				HttpResponse response = request.execute();		
				
				GenericJson content = null;
			
				content = response.parseAs(GenericJson.class);//FIXME Unexpected character ('w' (code 119)): was expecting comma to separate OBJECT entries
				UpdateTask.lastVersionNumber =(String) content.get("lastVersionNumber");
	
				 message = (String)content.get("mensaje");
			
				
				if(versionToDouble(lastVersionNumber)>versionToDouble(JFXMain.VERSION)){
					UpdateTask.lastVersionURL =(String)content.get("lastVersionURL");
					UpdateTask.isUpdateAvailable = true;				
				}
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}	
			}
			/*
			 else //lastVersionNumber != null
			if(versionToDouble(lastVersionNumber)>versionToDouble(JFXMain.VERSION)){			
				return "";	
			}*/
			return message;
	}

	/**
	 * hacer un llamado a www.ursulagis.com/update y chequear la ultima version con esta version
	 * actualiza la variable de lastVersion
	 * @return si la ultima version es mas grande que esta version devolver true
	 */
	public static boolean isUpdateAvailable() {
		return UpdateTask.isUpdateAvailable;
		/*
		//System.out.println("viendo si necesito hacer update");
		//TODO si ya se habia invocado no volver a llamar.
		if(lastVersionNumber ==null) {
		GenericUrl url = new GenericUrl(UPDATE_URL);//"http://www.ursulagis.com/update");// "http://www.lanacion.com.ar");
		url.put("VERSION", JFXMain.VERSION);
		
		String usr = getUserNumber();
		url.put("USER", usr);
		
		System.out.println("calling url=> "+url);
		//http://localhost:5000/update?VERSION=0.2.26&USER=693,468
		//http://www.ursulagis.com/update?VERSION=0.2.20
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						//						  request.setConnectTimeout(0);
						//					      request.setReadTimeout(0);
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse response = request.execute();		
			
			GenericJson content = null;
			try{
				content = response.parseAs(GenericJson.class);//FIXME Unexpected character ('w' (code 119)): was expecting comma to separate OBJECT entries
				UpdateTask.lastVersionNumber =(String) content.get("lastVersionNumber");

				String message = (String)content.get("mensaje");
				if(message!=null){
					showWelcomeMessage(message);				
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			if(versionToDouble(lastVersionNumber)>versionToDouble(JFXMain.VERSION)){
				UpdateTask.lastVersionURL =(String)content.get("lastVersionURL");
				return true;	
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}	
		} else if(versionToDouble(lastVersionNumber)>versionToDouble(JFXMain.VERSION)){			
			return true;	
		}
		return false;
		 */
	}

	public static String getUserNumber() {
//		DecimalFormat userNumberFormat = new DecimalFormat("0,000");
//		userNumberFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
//		userNumberFormat.setGroupingUsed(true);
//		
//		String userString = userNumberFormat.format(Math.random()*1000*1000);
		String userString = UUID.randomUUID().toString();
		Configuracion conf = Configuracion.getInstance();
		conf.loadProperties();
		String usr = conf.getPropertyOrDefault("USER", userString);//si no existia la clave se crea una nueva
		conf.save();
		return usr;
	}

	

	public static Double versionToDouble(String ver){
		ver= ver.replace(" dev", "");
		String[] v =ver.split("\\.");
		String ret = v[0]+".";
		for(int i=1;i<v.length;i++){
			ret=ret.concat(v[i]);
		}
		try{
			DecimalFormat dc = new DecimalFormat("0.####");
			dc.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
			dc.setGroupingUsed(true);
			//System.out.println("versionAsDouble "+ dc.parse(ret).doubleValue());//versionAsDouble 0.2241
			return dc.parse(ret).doubleValue();
			//return Double.parseDouble(ret);//ret contiene 0.224111 etc
		}catch(Exception e){
			e.printStackTrace();
			return -1.0;
		}
	}

	/** Resets one-shot update-check state. Used by tests in the same package. */
	static void resetUpdateCheckStateForTests() {
		lastVersionNumber = null;
		lastVersionURL = null;
		isUpdateAvailable = false;
	}

	/** Points {@link #checkForUpdate()} at a URL (e.g. embedded test server). Pass null to restore default. */
	static void setUpdateUrlForTests(String url) {
		updateUrl = (url != null && !url.isEmpty()) ? url : DEFAULT_UPDATE_URL;
	}
}
