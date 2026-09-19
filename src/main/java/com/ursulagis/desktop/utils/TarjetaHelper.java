package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

import com.ursulagis.desktop.dao.config.Configuracion;

import java.util.logging.Logger;

/**
 * File-server tarjeta helpers.
 * <p>
 * {@code TokenTarjeta} is stored separately from {@code USER}: it is initialized
 * once from the current user number, then kept stable so uploads keep working
 * if {@code USER} later changes. {@code UuidTarjeta} is the tarjeta id returned
 * by the server for that token.
 */
public class TarjetaHelper {
	private static final Logger logger = Logger.getLogger(TarjetaHelper.class.getName());

	private static final String UUID_TARJETA = "UuidTarjeta";
	/** Stable token used with the file server; initialized from USER once. */
	private static final String TOKEN_TARJETA = "TokenTarjeta";
	public static final String BASE_URL = "https://www.ursulagis.com";
	//public static final String BASE_URL = "http://localhost:5000";
	//public static final String BASE_URL = "https://sheltered-mesa-69562-dev-514e4d674053.herokuapp.com";

	public static String REGISTRAR_TARJETA_URL = BASE_URL + "/api/file_server/registrar_tarjeta/";
	public static String REGISTRAR_ARCHIVO_URL = BASE_URL + "/api/file_server/upload_file/";

	/**
	 * Ensures {@code TokenTarjeta} and {@code UuidTarjeta} are set.
	 * Token is copied from {@code USER} only when missing; UUID is registered once.
	 */
	public static void initTarjeta() {
		String tokenTarjeta = ensureTokenTarjeta();
		if (tokenTarjeta.isBlank()) {
			logger.warning("no se puede registrar tarjeta: TokenTarjeta/USER no configurado");
		} else {
			String uuidTarjeta = Configuracion.activeConfig().getProperty(UUID_TARJETA).trim();
			if (uuidTarjeta.isBlank()) {
				GenericUrl url = new GenericUrl(REGISTRAR_TARJETA_URL);
				url.set("token", tokenTarjeta);
				logger.fine("llamand la url " + url.build());
				byte[] bytes = null;
				try {
					bytes = "register tarjeta".getBytes("UTF8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				final HttpContent content = new ByteArrayContent("application/json", bytes);
				HttpResponse res = makeJsonPostRequest(url, content);
				if (res == null) {
					logger.warning("no se pudo registrar la tarjeta: response null");
				} else {
					try {
						String tarjetaUuid = res.parseAsString();
						res.disconnect();
						if (tarjetaUuid == null || tarjetaUuid.isBlank()) {
							logger.warning("registro de tarjeta rechazo: " + tarjetaUuid);
						} else {
							Configuracion config = Configuracion.activeConfig();
							config.setProperty(UUID_TARJETA, tarjetaUuid.trim());
							logger.fine("cree la tarjeta " + tarjetaUuid + " con TokenTarjeta " + tokenTarjeta);
							config.save();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Uploads {@code f} to {@code destUrl} on the file server.
	 * @return true if the server accepted the file; false otherwise
	 */
	public static boolean uploadFile(File f, String destUrl) {
		if (f == null || !f.exists()) {
			logger.warning("no se puede subir archivo: file null o inexistente");
			return false;
		}
		initTarjeta();
		String tokenTarjeta = Configuracion.activeConfig().getProperty(TOKEN_TARJETA).trim();
		String uuidTarjeta = Configuracion.activeConfig().getProperty(UUID_TARJETA).trim();
		if (tokenTarjeta.isBlank() || uuidTarjeta.isBlank()) {
			logger.warning("no se puede subir archivo: TokenTarjeta o UuidTarjeta invalido");
			return false;
		}

		GenericUrl url = new GenericUrl(REGISTRAR_ARCHIVO_URL);
		// Use stored TokenTarjeta (not current USER) so USER changes do not break uploads
		url.put("token", tokenTarjeta);
		url.put("uuid", uuidTarjeta);
		url.put("url", destUrl);

		try (FileInputStream fin = new FileInputStream(f)) {
			MultipartContent.Part part = new MultipartContent.Part()
					.setContent(new InputStreamContent("application/octet-stream", fin))
					.setHeaders(new HttpHeaders().set(
							"Content-Disposition",
							String.format("form-data; name=\"file\"; filename=\"%s\"", f.getName())));
			MultipartContent content = new MultipartContent()
					.setMediaType(new HttpMediaType("multipart/form-data")
							.setParameter("boundary", UUID.randomUUID().toString()))
					.addPart(part);

			HttpResponse response = makeBinaryPostRequest(url, content, f.getName());
			if (response == null) {
				logger.warning("no se pudo subir " + f.getName() + ": response null");
				return false;
			}
			String body = response.parseAsString();
			if (isUploadErrorBody(body)) {
				logger.warning("no se pudo subir " + f.getName() + ": " + body);
				return false;
			}
			logger.fine("file uploaded to " + f.getName() + " " + body);
			return true;
		} catch (Exception e) {
			logger.warning("no se pudo subir " + f.getName() + ": " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Uploads {@code f} under {@code destDir} (e.g. {@code /labores}) and returns the relative path
	 * used by orden payloads (e.g. {@code /labores/foo.png}).
	 * Returns {@code null} if the file is missing or the upload fails.
	 */
	public static String uploadFileToDir(File f, String destDir) {
		if (f == null || !f.exists()) {
			return null;
		}
		String dir = destDir == null ? "" : destDir;
		while (dir.endsWith("/")) {
			dir = dir.substring(0, dir.length() - 1);
		}
		if (!uploadFile(f, dir)) {
			return null;
		}
		return dir + "/" + f.getName();
	}

	private static boolean isUploadErrorBody(String body) {
		if (body == null || body.isBlank()) {
			return false;
		}
		String lower = body.toLowerCase();
		return lower.contains("bad token") || lower.contains("bad uuid");
	}

	/**
	 * Returns TokenTarjeta, creating it from config {@code USER} the first time
	 * it is missing. Fails if {@code USER} is blank. If a legacy UuidTarjeta
	 * exists without TokenTarjeta, clears the UUID so it is re-registered.
	 */
	private static String ensureTokenTarjeta() {
		Configuracion config = Configuracion.activeConfig();
		String token = config.getProperty(TOKEN_TARJETA).trim();
		if (!token.isBlank()) {
			return token;
		} else {
			String user = config.getProperty("USER");
			if (user == null || user.trim().isEmpty()) {
				logger.severe("no se puede inicializar TokenTarjeta: USER no configurado");
				return "";
			} else {
				user = user.trim();
				config.setProperty(TOKEN_TARJETA, user);
				if (!config.getProperty(UUID_TARJETA).trim().isBlank()) {
					logger.info("UuidTarjeta sin TokenTarjeta; se vuelve a registrar con el USER actual");
					config.setProperty(UUID_TARJETA, "");
				}
				config.save();
				logger.fine("TokenTarjeta inicializado desde USER: " + user);
				return user;
			}
		}
	}

	private static String tokenTarjeta() {
		return Configuracion.activeConfig().getProperty(TOKEN_TARJETA).trim();
	}

	private static HttpResponse makeBinaryPostRequest(GenericUrl url, HttpContent req_content, String fileName) {
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						HttpHeaders headers = request.getHeaders();
						headers.set("USER", tokenTarjeta());
						String fileLength = "0";
						try {
							fileLength = Long.toString(req_content.getLength());
						} catch (IOException e) {
							e.printStackTrace();
						}
						headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"; size=" + fileLength);
					}
				});

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);
			response = request.execute();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}

	private static HttpResponse makeJsonPostRequest(GenericUrl url, HttpContent req_content) {
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
						HttpHeaders headers = request.getHeaders();
						headers.set("USER", tokenTarjeta());
					}
				});

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);
			response = request.execute();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}
}
