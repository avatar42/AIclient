package com.dea42.aitools.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * AI image client
 *
 */
public class Client {
	private static final Marker CLIENT_MARKER = MarkerFactory.getMarker("CLIENT_MARKER");
	private final Logger log = LoggerFactory.getLogger(getClass());
	private String newFolder = "./new";
	private String host = "http://10.10.2.183:32168";

	public Client(String host, String newFolder) {
		this.newFolder = newFolder;
		this.host = host;
	}
	
	public Client() {
			ResourceBundle bundle = ResourceBundle.getBundle("client");
			this.host = bundle.getString("host");
			this.newFolder = bundle.getString("folder.new");
	}

	private String doPost(HttpEntity params, String url) throws IOException {
		CloseableHttpResponse response = null;
		String responseStr = null;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httppost = new HttpPost(new URI(url));
			httppost.addHeader("Accept", "*/*");
			httppost.addHeader("Accept-Encoding", "gzip, deflate, br");

			httppost.setEntity(params);

			response = httpclient.execute(httppost);
			int respCode = response.getStatusLine().getStatusCode();
			log.info(CLIENT_MARKER, "response code:{}", respCode);
			if (respCode == HttpStatus.SC_OK || respCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseStr = EntityUtils.toString(entity, StandardCharsets.US_ASCII);
					StringBuilder sb = new StringBuilder();
					for (byte b : responseStr.getBytes()) {
						sb.append(String.format("%02x", b));
					}
					log.info(CLIENT_MARKER, "responseStr:{}", sb);
				}
			}
		} catch (URISyntaxException e) {
			log.error("Failed posting URL", e);
		} finally {
			httpclient.close();
		}
		log.info(CLIENT_MARKER, "responseStr:{}", responseStr);
		return responseStr;

	}

	private MultipartEntityBuilder addImage(MultipartEntityBuilder reqEntity, String imageName) throws IOException {
		File fileToUse = new File(newFolder, imageName);
		if (fileToUse.exists()) {
			log.info(CLIENT_MARKER, "loading:{}", fileToUse);
			FileBody data = new FileBody(fileToUse, ContentType.DEFAULT_BINARY, fileToUse.getName());

			reqEntity.addPart("image", data);
			return reqEntity;
		}
		throw new IOException(fileToUse.getAbsolutePath() + " does not exist");
	}

	public JSONObject detect(String model, String imagePath, String minConfidence) throws IOException {
		MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);
		MultipartEntityBuilder data = addImage(reqEntity, imagePath);
		data.addPart("confidence", new StringBody(minConfidence, ContentType.WILDCARD));
		return new JSONObject(doPost(data.build(), host + "/v1/vision/" + model));
	}
}
