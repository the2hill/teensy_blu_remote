package com.toohill.blurcremote;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageViewThread extends Thread {
	String url;
	Activity context;
	ImageView imageView;
	ImageButton cameraButton;
	Bitmap bitmap;
	boolean isRunning = true;

	public ImageViewThread(Activity context, ImageView imageView,
			ImageButton cameraButton, String url) {
		this.url = url;
		this.context = (BluRCActivity) context;
		this.imageView = imageView;
		this.cameraButton = cameraButton;
	}

	public void run() {	
		try {
			bitmap = ImageViewWorker.getBitmapFromUrl(url);
			if (imageView != null || !ImageViewWorker.verifyServerUp(url)) {
				this.isRunning = false;
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						imageView.setImageBitmap(null);
						cameraButton.setSelected(false);
					}
				});
				return;
			}
			while (isRunning) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (bitmap != null) {
							imageView.setImageBitmap(bitmap);
							if (!cameraButton.isSelected()) cameraButton.setSelected(true);
						}
					}
				});
				Thread.sleep(33);
			}
		} catch (Exception e) {
			this.isRunning = false;

			if (e instanceof InterruptedException) {
				Log.w("Image Thread was interrupted, shutting down...", e);
			} 
			Log.e("ClientActivity", "C: Image Thread Exception", e);
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (bitmap != null) {
						imageView.setImageBitmap(null);
						cameraButton.setSelected(false);
						// bm.recycle();
					}
				}
			});
			return;
		}
	}

	public boolean isRunning() {
		return this.isRunning;
	}
}