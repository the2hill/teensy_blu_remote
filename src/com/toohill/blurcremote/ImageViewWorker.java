package com.toohill.blurcremote;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageViewWorker {
	static String TAG = "IMAGEVIEWWORKER";

	public static Bitmap getBitmapFromUrl(String url)  {
		
		HttpResponse response = null;
		try {
			response = createGETClient(url);
		} catch (Exception e1) {
			Log.w(TAG, "Exception communicating with the host");
		} 
		
		if (response != null) {
			HttpEntity entity = response.getEntity();
			BufferedHttpEntity bufHttpEntity = null;
			try {
				bufHttpEntity = new BufferedHttpEntity(entity);
			} catch (IOException e) {
				Log.d(TAG, "Connection error during image retrieval.");
			}

			InputStream instream = null;
			try {
				instream = bufHttpEntity.getContent();
			} catch (IOException e) {
				Log.d(TAG, "Connection error during image retrieval.");
			}

			return BitmapFactory.decodeStream(instream);
		}

		return null;
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
