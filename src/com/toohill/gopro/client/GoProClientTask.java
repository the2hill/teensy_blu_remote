package com.toohill.gopro.client;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class GoProClientTask extends AsyncTask<String, String, GoPro> {
	final String TAG = "GOPROTASK";
	private TextView battPercentView;

	public GoProClientTask(TextView battPercentView) {
		this.battPercentView = battPercentView;

	}

	@Override
	protected GoPro doInBackground(String... params) {
		GoPro gp = null;
		GoProClient gc = new GoProClient(params[0], params[1], params[2]);
		try {
			if (gc.verifyServerUp()) {
				gp = gc.getStats();
			}
		} catch (Exception e) {
			Log.d(TAG, "Failed getting stats");
		}
		return gp;
	}

	@Override
	protected void onPostExecute(GoPro goPro) {
		if (goPro != null) {
			battPercentView.setText(goPro.getBattPercent() + "%");
		}
	}

}
