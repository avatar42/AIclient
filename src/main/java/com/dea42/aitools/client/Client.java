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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
	private Path newFolderPath;
	private String markedFolder = "./marked";
	private String labeledFolder = "./labeled";
	private String debugFolder = "./debug";
	private Map<String, Integer> classes = new HashMap<>();
	private int lastCls = 0;
	private static final String CUSTOM ="custom/";

	private String host = "http://10.10.2.183:32168";

	public Client(String host, String newFolder, String markedFolder, String labeledFolder, String debugFolder,
			Map<String, Integer> classes) {
		this.newFolder = newFolder;
		this.host = host;
		this.markedFolder = markedFolder;
		this.labeledFolder = labeledFolder;
		this.debugFolder = debugFolder;
		this.classes = classes;
		this.newFolderPath = Paths.get(this.newFolder);
	}

	public Client() {
		ResourceBundle bundle = ResourceBundle.getBundle("client");
		this.host = bundle.getString("host");
		this.newFolder = bundle.getString("folder.new");
		this.markedFolder = bundle.getString("folder.marked");
		this.labeledFolder = bundle.getString("folder.labeled");
		this.debugFolder = bundle.getString("folder.debug");
		try {
			String[] cls = bundle.getStringArray("classes");
			int i = 1;
			for (String c : cls) {
				classes.put(c, i++);
			}
			lastCls = i;
		} catch (Exception e) {
			// ignore
		}
		this.newFolderPath = Paths.get(this.newFolder);
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
			log.debug(CLIENT_MARKER, "response code:{}", respCode);
			if (respCode == HttpStatus.SC_OK || respCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseStr = EntityUtils.toString(entity, StandardCharsets.US_ASCII);
					StringBuilder sb = new StringBuilder();
					for (byte b : responseStr.getBytes()) {
						sb.append(String.format("%02x", b));
					}
					log.debug(CLIENT_MARKER, "responseStr:{}", sb);
				}
			}
		} catch (URISyntaxException e) {
			log.error("Failed posting URL", e);
		} finally {
			httpclient.close();
		}
		log.debug(CLIENT_MARKER, "responseStr:{}", responseStr);
		return responseStr;

	}

	/**
	 * Draw the detections on an image.
	 * 
	 * @param model       name to label with
	 * @param img         BufferedImage to draw on
	 * @param predictions JSONArray with detections info like
	 *                    [{"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,"x_max":512,"y_max":358}]
	 * @param c           Color to draw in.
	 * @return marked up BufferedImage
	 */
	public BufferedImage markImg(String model, BufferedImage img, JSONArray predictions, Color c) {
		// Obtain the Graphics2D context associated with the BufferedImage.
		Graphics2D g = img.createGraphics();
		g.setColor(c);
		for (int i = 0; i < predictions.length(); i++) {
			JSONObject jobj = predictions.getJSONObject(i);
			// Draw on the BufferedImage via the graphics context.
			int x = jobj.optInt("x_min");
			int y = jobj.optInt("y_min");
			int width = jobj.optInt("x_max") - x;
			int height = jobj.optInt("y_max") - y;

			g.drawRect(x, y, width, height);
			g.drawString(model + "_" + jobj.getString("label"), x, y);
		}
		// Clean up -- dispose the graphics context that was created.
		g.dispose();
		return img;
	}

	/**
	 * Get the detections of an images as individual files as
	 * debugFolder/class/idx.fileName<br>
	 * Mainly used for easily checking
	 * 
	 * @param file        to read from
	 * @param predictions JSONArray of detections
	 * @param model       name of model that made the detections
	 * @return number of detections that match the class the source file came from.
	 * @throws JSONException
	 * @throws IOException
	 */
	public int cropImgs(File file, JSONArray predictions, String model) throws JSONException, IOException {
		BufferedImage img = ImageIO.read(file);
		String cls = file.getParentFile().getName();
		int matches = 0;
		for (int i = 0; i < predictions.length(); i++) {
			JSONObject jobj = predictions.getJSONObject(i);
			int x = jobj.optInt("x_min");
			int y = jobj.optInt("y_min");
			int width = jobj.optInt("x_max") - x;
			int height = jobj.optInt("y_max") - y;
			String dcls = jobj.getString("label");
			if (cls.equals(dcls))
				matches++;
			BufferedImage simg = img.getSubimage(x, y, width, height);
			int conf = (int) (jobj.getFloat("confidence") * 100);
			File out = new File(getDebugFolder(), dcls + "/" + model + "." + i + "." + conf + "." + file.getName());
			writeImg(out, simg);
			jobj.put("image", out.getPath());
		}
		return matches;
	}

	public void writeMap(File file, JSONArray predictions, String model) throws JSONException, IOException {
		BufferedImage img = ImageIO.read(file);
		StringBuilder tags = new StringBuilder();
		int h = img.getHeight();
		int w = img.getWidth();
		for (int i = 0; i < predictions.length(); i++) {
			JSONObject jobj = predictions.getJSONObject(i);
			int xmin = jobj.optInt("x_min");
			int ymin = jobj.optInt("y_min");
			int xmax = jobj.optInt("x_max") - xmin;
			int ymax = jobj.optInt("y_max") - ymin;
			float xcenter = (xmin + (float) (xmax - xmin) / 2) / w;
			float ycenter = (ymin + (float) (ymax - ymin) / 2) / h;
			float xwidth = (float) (xmax - xmin) / w;
			float yheight = (float) (ymax - ymin) / h;
			String dcls = jobj.getString("label");
			int idx = classes.getOrDefault(dcls, 0);
			if (idx == 0) {
				classes.put(dcls, lastCls++);
			}
			tags.append(idx + " " + String.format("%.6f", xcenter) + " " + String.format("%.6f", ycenter) + " "
					+ String.format("%.6f", xwidth) + " " + String.format("%.6f%n", yheight));
			int u = dcls.indexOf('_');
			if (u > 0) {
				dcls = dcls.substring(0, u);
				idx = classes.getOrDefault(dcls, 0);
				if (idx == 0) {
					classes.put(dcls, lastCls++);
				}
				tags.append(idx + " " + String.format("%.6f", xcenter) + " " + String.format("%.6f", ycenter) + " "
						+ String.format("%.6f", xwidth) + " " + String.format("%.6f%n", yheight));
			}
		}
		if (tags.length() > 0)
			Files.write(Paths.get(getMarkedFolder(), model + "." + file.getName() + ".txt"),
					tags.toString().getBytes());
	}

	public void writeImg(File file, BufferedImage bufferedImage) throws IOException {
		RenderedImage rendImage = bufferedImage;

		File dir = file.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		ImageIO.write(rendImage, "jpg", file);
	}

	private MultipartEntityBuilder addImage(MultipartEntityBuilder reqEntity, String imageName) throws IOException {
		File fileToUse = new File(imageName);
		if (fileToUse.exists()) {
			log.debug(CLIENT_MARKER, "loading:{}", fileToUse);
			FileBody data = new FileBody(fileToUse, ContentType.DEFAULT_BINARY, fileToUse.getName());

			reqEntity.addPart("image", data);
			return reqEntity;
		}
		throw new IOException(fileToUse.getAbsolutePath() + " does not exist");
	}

	public JSONObject detectCustom(String model, String imagePath, String minConfidence) throws IOException {
		return detect(CUSTOM + model, imagePath, minConfidence);
	}

	public JSONObject detect(String model, String imagePath, String minConfidence) throws IOException {
		MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT);
		MultipartEntityBuilder data = addImage(reqEntity, imagePath);
		data.addPart("confidence", new StringBody(minConfidence, ContentType.WILDCARD));
		return new JSONObject(doPost(data.build(), host + "/v1/vision/" + model));
	}

	/**
	 * Load image from newPath, detect objects, create a marked up image, map and cropped images then return the details in an JSONObject 
	 * @param newPath
	 * @param model
	 * @param minConfidence
	 * @return JSONObject with details of what was found and file paths
	 * @throws JSONException
	 * @throws IOException
	 */
	public JSONObject loadImage(Path newPath, String model, String minConfidence) throws JSONException, IOException {
		JSONObject details = new JSONObject();
		File newFile = newPath.toFile();
		Path basePath = newFolderPath.relativize(newPath);

		BufferedImage img = ImageIO.read(newFile);

		JSONObject resp = detect(model, newFile.getPath(), minConfidence);
		if (model.startsWith(CUSTOM)) {
			model = model.substring(CUSTOM.length());
		}
		
		JSONArray predictions = resp.getJSONArray("predictions");
		details.put("model", model);
		details.put("detections", predictions);
		markImg(model, img, predictions, Color.BLUE);
		File markedFile = new File(getMarkedFolder(), basePath.toFile().getPath());
		writeImg(markedFile, img);
		cropImgs(newFile, predictions, model);
		writeMap(newFile, predictions, model);

		return details;
	}

	public String getNewFolder() {
		return newFolder;
	}

	public void setNewFolder(String newFolder) {
		this.newFolder = newFolder;
		this.newFolderPath = Paths.get(this.newFolder);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

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

	public Map<String, Integer> getClasses() {
		return classes;
	}

	public void setClasses(Map<String, Integer> classes) {
		this.classes = classes;
		for (String k : classes.keySet()) {
			if (classes.get(k) > lastCls)
				lastCls = classes.get(k);
		}
	}

}
