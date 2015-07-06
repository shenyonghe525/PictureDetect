package com.facpp.picturedetect;

import java.io.ByteArrayOutputStream;
import org.json.JSONObject;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

public class FaceppDetect {
	DetectCallback callback = null;

	private Handler mhandler;
	private Bitmap mBitmap;

	public FaceppDetect(Handler handler,Bitmap bitmap) {
		this.mhandler = handler;
		this.mBitmap = bitmap;
	}

	public void setDetectCallback(DetectCallback detectCallback) {
		callback = detectCallback;
	}

	public void detect(final Bitmap image) {

		new Thread(new Runnable() {

			public void run() {
				HttpRequests httpRequests = new HttpRequests(
						"59201af14f9820236ad794d22f89345b",
						"LnXzQHuLSMFSHPjGMrIpjpwpx8c0fm8q", true, false);

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				float scale = Math.min(1,
						Math.min(600f / mBitmap.getWidth(), 600f / mBitmap.getHeight()));
				Matrix matrix = new Matrix();
				matrix.postScale(scale, scale);

				Bitmap imgSmall = Bitmap.createBitmap(mBitmap, 0, 0,
						mBitmap.getWidth(), mBitmap.getHeight(), matrix, false);

				imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
				byte[] array = stream.toByteArray();

				try {
					// detect
					JSONObject result = httpRequests
							.detectionDetect(new PostParameters().setImg(array));
					// finished , then call the callback function
					if (callback != null) {
						callback.detectResult(result,mhandler);
					}
				} catch (FaceppParseException e) {
					e.printStackTrace();
					Message message = Message.obtain();
					message.what = MainActivity.MSG_ERROR;
					mhandler.sendMessage(message);
				}

			}
		}).start();
	}
}
