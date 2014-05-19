package com.toohill.gopro.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class GoProClient {
	static String TAG = "GOPROCLIENT";

	// Host
	public String host;
	public String port;
	public String password;

	public String endpoint;
	public String statsendpoint;

	// Endpoint paths
	public String previewPath;
	public String statsPath;
	public String cmdPath;

	private DefaultHttpClient httpClient;

	public GoProClient(String host, String port, String password) {
		this.host = host;
		this.port = port;
		this.password = password;

		previewPath = "/live/amba.m3u8";
		statsPath = "/CMD?t=PWD";
		cmdPath = "/CMD?t=PWD&p=%VAL";

		endpoint = "http://%s:%s";
		statsendpoint = "http://%s";

		httpClient = buildHttpClient(1000);
	}

	public byte[] getStatsBytes() throws ClientProtocolException, IOException {
		byte[] content = EntityUtils.toByteArray((HttpEntity) httpClient
				.execute(new HttpGet(buildStatsUrl())).getEntity());
		return content;
	}

	public GoPro getStats() throws ClientProtocolException, IOException {
		byte[] ba = getStatsBytes();
		GoPro gp = null;
		if (ba != null && ba.length > 0) {
			gp = new GoPro(ba[1], ba[3], ba[4], ba[5], ba[6], ba[7], ba[8],
					ba[9], ba[13], ba[14], ba[16], ba[17], ba[18], ba[19]);
		}
		return gp;
	}

	public boolean verifyServerUp() {
		HttpResponse response;
		try {
			response = httpClient.execute(new HttpGet(buildCheckUrl()));
		} catch (Exception ex) {
			Log.d(TAG, "GoPro server is not available");
			return false;
		}

		if (response != null)
			return true;

		return false;
	}

	private String buildStatsUrl() {
		return String.format(statsendpoint, host)
				+ statsPath.replace("CMD", "camera/se")
						.replace("PWD", password);
	}

	private String buildCheckUrl() {
		return String.format(endpoint, host, port);
	}

	private DefaultHttpClient buildHttpClient(int timeout) {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
		HttpConnectionParams.setSoTimeout(httpParameters, timeout);
		return new DefaultHttpClient(httpParameters);
	}

	public void close() {
	}

}
