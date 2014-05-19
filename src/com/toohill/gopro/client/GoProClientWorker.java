package com.toohill.gopro.client;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class GoProClientWorker {
	static String TAG = "GOPROCLIENTWORKER";

	public static byte[] getStats(String url) throws ClientProtocolException,
			IOException {
		HttpResponse response = createGETClient(url);
		byte[] content = EntityUtils.toByteArray(response.getEntity());
		return content;
	}

	public GoPro mapBytes(byte[] ba) {
		GoPro gp = null;
		if (ba != null && ba.length > 0) {
			gp = new GoPro(ba[1], ba[3], ba[4], ba[5], ba[6], ba[7],
					ba[8], ba[9], ba[13], ba[14], ba[16], ba[17], ba[18],
					ba[19]);
		}
		return gp;
	}

	public static boolean verifyServerUp(String url) {
		HttpResponse response;
		try {
			response = createGETClient(url);
		} catch (Exception ex) {
			Log.d(TAG, "Image server is not available");
			return false;
		}

		if (response != null)
			return true;

		return false;
	}

	private static HttpResponse createGETClient(String url)
			throws ClientProtocolException, IOException {
		HttpGet httpRequest = null;
		httpRequest = new HttpGet(url);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;
		response = (HttpResponse) httpclient.execute(httpRequest);
		return response;
	}

}
