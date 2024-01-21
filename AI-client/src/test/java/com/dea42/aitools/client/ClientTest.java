package com.dea42.aitools.client;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit test for client.
 */
public class ClientTest {
	Client obj = new Client();

	/**
	 * Basic Detect Test
	 * 
	 * @throws IOException
	 */
	@Test
	public void detectionTest() throws IOException {
		JSONObject resp = obj.detect("detection", "animal/cat/cat_black/PTZ510.20230510_050201.9815571.3.jpg", "0.55");
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
		JSONObject resp = obj.detect("custom/RMRR", "animal/cat/cat_black/PTZ510.20230510_050201.9815571.3.jpg",
				"0.45");
		// {"message":"Found cat_grey","count":1,"predictions":[{"confidence":0.49429088830947876,"label":"cat_grey","x_min":383,"y_min":294,"x_max":512,"y_max":358}],"success":true,"processMs":730,"inferenceMs":730,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":750}
		assertEquals("message wrong", "Found cat_grey", (String) resp.get("message"));
	}

	/**
	 * Basic RMRR Test
	 * 
	 * @throws IOException
	 */
	@Test
	public void darkTest() throws IOException {
		JSONObject resp = obj.detect("custom/dark", "animal/cat/cat_black/PTZ510.20230510_050201.9815571.3.jpg",
				"0.74");
		// {"message":"Found Cat","count":1,"predictions":[{"confidence":0.7439730763435364,"label":"Cat","x_min":376,"y_min":285,"x_max":520,"y_max":361}],"success":true,"processMs":3297,"inferenceMs":2380,"code":200,"command":"custom","moduleId":"ObjectDetectionYolo","executionProvider":"CPU","canUseGPU":false,"analysisRoundTripMs":3318}
		assertEquals("message wrong", "Found Cat", (String) resp.get("message"));
	}
}
