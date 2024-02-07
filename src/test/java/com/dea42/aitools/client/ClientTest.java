package com.dea42.aitools.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for client.
 */
public class ClientTest {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private Client obj = new Client();
	private File testFile1 = new File(obj.getNewFolder(), "animal/cat/cat_black/PTZ510.20230510_050201.9815571.3.jpg");
	private File testFile2 = new File(obj.getNewFolder(), "animal/raccoon/HV420.20230722_214738243.2756.jpg");
	private File markedFile1 = new File(obj.getMarkedFolder(),
			"animal/cat/cat_black/PTZ510.20230510_050201.9815571.3.jpg");
	private File markedFile2 = new File(obj.getMarkedFolder(), "animal/raccoon/HV420.20230722_214738243.2756.jpg");
	private Map<String, Integer> classes = new HashMap<>();

	public ClientTest() {
		classes.put("raccoon", 2);
		classes.put("cat_black", 10);
		classes.put("cat_grey", 11);
		classes.put("cat", 100);
		classes.put("dog", 104);

		obj.setClasses(classes);
	}

	/**
	 * Quick check that classes where loaded into Client
	 */
	@Test
	public void classesTest() {
		assertEquals("classes loaded", this.classes.size(), obj.getClasses().size());
		assertEquals("raccoon loaded", 2, (int) obj.getClasses().get("raccoon"));
		assertEquals("cat_black loaded", 10, (int) obj.getClasses().get("cat_black"));
		assertEquals("cat_grey loaded", 11, (int) obj.getClasses().get("cat_grey"));
		assertEquals("cat loaded", 100, (int) obj.getClasses().get("cat"));
		assertEquals("dog loaded", 104, (int) obj.getClasses().get("dog"));
	}

	/**
	 * Basic Detect Test
	 * 
	 * @throws IOException
	 */
	@Test
	public void detectionTest() throws IOException {
		JSONObject resp = obj.detect("detection", testFile1.getPath(), "0.55");
		// {"message":"Found cat, dog,
		// bowl","count":3,"predictions":[{"confidence":0.4594840705394745,"label":"cat","x_min":379,"y_min":295,"x_max":514,"y_max":357},{"confidence":0.5552480220794678,"label":"dog","x_min":380,"y_min":295,"x_max":511,"y_max":357},{"confidence":0.7309744358062744,"label":"bowl","x_min":321,"y_min":391,"x_max":361,"y_max":423}],"success":true,"processMs":714,"inferenceMs":714,"code":200,"command":"detect","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":732}
		assertEquals("message wrong", "Found cat, dog, bowl", (String) resp.get("message"));
	}

	/**
	 * Basic RMRR Test
	 * 
	 * @throws IOException
	 */
	@Test
	public void RMRRTest() throws IOException {
		JSONObject resp = obj.detectCustom("RMRR", testFile1.getPath(), "0.45");
		// {"message":"Found
		// cat_grey","count":1,"predictions":[{"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,"x_max":512,"y_max":358}],"success":true,"processMs":730,"inferenceMs":730,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":750}
		assertEquals("message wrong", "Found cat_grey", (String) resp.get("message"));
	}

	/**
	 * Basic dark Test
	 * 
	 * @throws IOException
	 */
	@Test
	public void darkTest() throws IOException {
		JSONObject resp = obj.detectCustom("dark", testFile1.getPath(), "0.74");
		// {"message":"Found
		// Cat","count":1,"predictions":[{"confidence":0.7439730763435364,"label":"Cat","x_min":376,"y_min":285,"x_max":520,"y_max":361}],"success":true,"processMs":3297,"inferenceMs":2380,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":3318}
		assertEquals("message wrong", "Found Cat", (String) resp.get("message"));
	}

	@Test
	public void RMRRmarkupTest() throws IOException {
		JSONObject resp = obj.detectCustom("RMRR", testFile1.getPath(), "0.45");
		// {"message":"Found
		// cat_grey","count":1,"predictions":[{"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,"x_max":512,"y_max":358}],"success":true,"processMs":730,"inferenceMs":730,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":750}
		assertEquals("message wrong", "Found cat_grey", (String) resp.get("message"));

		BufferedImage img = ImageIO.read(testFile1);
		img = obj.markImg("RMRR", img, resp.getJSONArray("predictions"), Color.BLUE);
		obj.writeImg(markedFile1, img);
	}

	@Test
	public void RMRRcropTest() throws IOException {
		JSONObject resp = obj.detectCustom("RMRR", testFile1.getPath(), "0.45");
		// {"message":"Found
		// cat_grey","count":1,"predictions":[{"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,"x_max":512,"y_max":358}],"success":true,"processMs":730,"inferenceMs":730,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":750}
		assertEquals("message wrong", "Found cat_grey", (String) resp.get("message"));

		int matches = obj.cropImgs(testFile1, resp.getJSONArray("predictions"), "RMRR");
		assertEquals("matches", 0, matches);
	}

	@Test
	public void complexTest() throws IOException {
		BufferedImage img = ImageIO.read(testFile2);

		JSONObject resp = obj.detectCustom("RMRR", testFile2.getPath(), "0.45");
		assertTrue("Class wrong", resp.getString("message").contains("raccoon"));
		JSONArray predictions = resp.getJSONArray("predictions");
		assertEquals("predictions", 7, predictions.length());
		img = obj.markImg("RMRR", img, predictions, Color.BLUE);
//		obj.writeImg(obj.getMarkedFolder(), testFile2, img);
		int matches = obj.cropImgs(testFile2, predictions, "RMRR");
		assertEquals("matches", 4, matches);
		obj.writeMap(testFile2, predictions, "RMRR");
		List<String> lines = Files
				.readAllLines(Paths.get(obj.getMarkedFolder(), "RMRR" + "." + testFile2.getName() + ".txt"));
		assertEquals("found classes", 10, lines.size());

		resp = obj.detect("detection", testFile2.getPath(), "0.45");
		assertTrue("Class wrong", resp.getString("message").contains("cat"));
		predictions = resp.getJSONArray("predictions");
		assertEquals("predictions", 5, predictions.length());
		img = obj.markImg("detection", img, predictions, Color.BLUE);
		obj.writeImg(markedFile2, img);
		int matches2 = obj.cropImgs(testFile2, predictions, "detection");
		assertEquals("matches", 0, matches2);
		obj.writeMap(testFile2, predictions, "detection");

		lines = Files.readAllLines(Paths.get(obj.getMarkedFolder(), "detection" + "." + testFile2.getName() + ".txt"));
		assertEquals("found classes", 5, lines.size());
		assertEquals("total classes", 7, obj.getClasses().size());
	}

	@Test
	public void loadTest() throws IOException {
		JSONObject jo = obj.loadImage(testFile2.toPath(), "custom/RMRR", "0.45");
		assertNotNull(jo);
		log.info("Details:" + jo.toString(4));
	}

}
