package com.dea42.aitools.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

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
import org.json.JSONArray;
import org.json.JSONException;
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
	private String markedFolder = "./marked";
	private String labeledFolder = "./labeled";
	private String debugFolder = "./debug";

	public String getMarkedFolder() {
		return markedFolder;
	}

	public void setMarkedFolder(String markedFolder) {
		this.markedFolder = markedFolder;
	}

	public String getLabeledFolder() {
		return labeledFolder;
	}

	public void setLabeledFolder(String labeledFolder) {
		this.labeledFolder = labeledFolder;
	}

	public String getDebugFolder() {
		return debugFolder;
	}

	public void setDebugFolder(String debugFolder) {
		this.debugFolder = debugFolder;
	}

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

	/*
	 * {"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,
	 * "x_max":512,"y_max":358}
	 */
	public BufferedImage markImg(String model, BufferedImage img, JSONObject jobj, Color c) {
		// Obtain the Graphics2D context associated with the BufferedImage.
		Graphics2D g = img.createGraphics();

		// Draw on the BufferedImage via the graphics context.
		int x = jobj.optInt("x_min");
		int y = jobj.optInt("y_min");
		int width = jobj.optInt("x_max") - x;
		int height = jobj.optInt("y_max") - y;

		g.drawRect(x, y, width, height);
		g.drawString(model+"_"+jobj.getString("label"), x, y);
		// Clean up -- dispose the graphics context that was created.
		g.dispose();

		return img;
	}

	/**
	 * Get the detections of an images as individual files as
	 * debugFolder/class/idx.fileName<br>
	 * Mainly used for easily checking
	 * 
	 * @param file to read from
	 * @param ja   JSONArray of detections
	 * @return number of detections that match the class the source file came from.
	 * @throws JSONException
	 * @throws IOException
	 */
	public int cropImgs(File file, JSONArray ja) throws JSONException, IOException {
		BufferedImage img = ImageIO.read(file);
		String cls = file.getParentFile().getName();
		int matches = 0;
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jobj = ja.getJSONObject(i);
			int x = jobj.optInt("x_min");
			int y = jobj.optInt("y_min");
			int width = jobj.optInt("x_max") - x;
			int height = jobj.optInt("y_max") - y;
			String dcls = jobj.getString("label");
			if (cls.equals(dcls))
				matches++;
			BufferedImage simg = img.getSubimage(x, y, width, height);
			writeImg(getDebugFolder(), dcls + "/" + i + "." + file.getName(), simg);
		}
		return matches;
	}

	public void writeImg(String folder, String path, BufferedImage bufferedImage) throws IOException {
		RenderedImage rendImage = bufferedImage;

		File file = new File(folder, path);
		File dir = file.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		ImageIO.write(rendImage, "jpg", file);
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

	public String getNewFolder() {
		return newFolder;
	}

	public void setNewFolder(String newFolder) {
		this.newFolder = newFolder;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public JSONObject detect(String model, String imagePath, String minConfidence) throws IOException {
		MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);
		MultipartEntityBuilder data = addImage(reqEntity, imagePath);
		data.addPart("confidence", new StringBody(minConfidence, ContentType.WILDCARD));
		return new JSONObject(doPost(data.build(), host + "/v1/vision/" + model));
	}
}
