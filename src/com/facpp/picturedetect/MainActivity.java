package com.facpp.picturedetect;

import java.io.ByteArrayOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

/**
 * A simple demo, get a picture form your phone<br />
 * Use the facepp api to detect<br />
 * Find all face on the picture, and mark them out.
 * 
 * @author shenyonghe
 */
public class MainActivity extends Activity implements OnClickListener {

	final private int PICTURE_CHOOSE = 1;

	private ImageView imageView = null;
	private Bitmap img = null;
	private Button buttonDetect = null;
	private TextView textView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
	}

	private void initViews() {
		Button getImage = (Button) this.findViewById(R.id.btn_getImage);
		getImage.setOnClickListener(this);
		textView = (TextView) this.findViewById(R.id.textView1);
		buttonDetect = (Button) this.findViewById(R.id.btn_detect);
		buttonDetect.setVisibility(View.INVISIBLE);
		buttonDetect.setOnClickListener(this);
		imageView = (ImageView) this.findViewById(R.id.imageView1);
		imageView.setImageBitmap(img);
	}

	// 选取图片
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		// the image picker callback
		if (requestCode == PICTURE_CHOOSE) {
			if (intent != null) {
				Cursor cursor = getContentResolver().query(intent.getData(),
						null, null, null, null);
				cursor.moveToFirst();
				int idx = cursor.getColumnIndex(ImageColumns.DATA);
				String fileSrc = cursor.getString(idx);

				Options options = new Options();
				options.inJustDecodeBounds = true;
				img = BitmapFactory.decodeFile(fileSrc, options);
				// scale size to read
				options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
						(double) options.outWidth / 1024f,
						(double) options.outHeight / 1024f)));
				options.inJustDecodeBounds = false;
				img = BitmapFactory.decodeFile(fileSrc, options);
				textView.setText("Clik Detect. ==>");

				imageView.setImageBitmap(img);
				buttonDetect.setVisibility(View.VISIBLE);
			} else {
				System.out.println("idButSelPic Photopicker canceled");
			}
		}
	}

	private class FaceppDetect {
		DetectCallback callback = null;

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
					float scale = Math.min(
							1,
							Math.min(600f / img.getWidth(),
									600f / img.getHeight()));
					Matrix matrix = new Matrix();
					matrix.postScale(scale, scale);

					Bitmap imgSmall = Bitmap.createBitmap(img, 0, 0,
							img.getWidth(), img.getHeight(), matrix, false);

					imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] array = stream.toByteArray();

					try {
						// detect
						JSONObject result = httpRequests
								.detectionDetect(new PostParameters()
										.setImg(array));
						// finished , then call the callback function
						if (callback != null) {
							callback.detectResult(result);
						}
					} catch (FaceppParseException e) {
						e.printStackTrace();
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								textView.setText("Network error.");
							}
						});
					}

				}
			}).start();
		}
	}

	// 定义获取到图片信息后的回调接口
	public interface DetectCallback {
		void detectResult(JSONObject rst);
	}

	//获取到图片信息后的处理
	DetectCallback detectCallback = new DetectCallback() {

		public void detectResult(JSONObject rst) {
			System.out.println("get face Info:\n" + rst);
			// use the red paint
			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 100f);
			// create a new canvas
			Bitmap bitmap = Bitmap.createBitmap(img.getWidth(),
					img.getHeight(), img.getConfig());
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(img, new Matrix(), null);
			try {
				// find out all faces
				final int count = rst.getJSONArray("face").length();
				final int[] age = new int[count];
				for (int i = 0; i < count; ++i) {
					float x, y, w, h;
					// get the center point
					x = (float) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getJSONObject("center")
							.getDouble("x");
					y = (float) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getJSONObject("center")
							.getDouble("y");
					int value = (int) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("attribute").getJSONObject("age")
							.getInt("value");
					int range = (int) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("attribute").getJSONObject("age")
							.getInt("range");
					age[i] = value + range;
					System.out.println("年龄:" + age[i]);
					// get face size
					w = (float) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getDouble("width");
					h = (float) rst.getJSONArray("face").getJSONObject(i)
							.getJSONObject("position").getDouble("height");

					// change percent value to the real size
					x = x / 100 * img.getWidth();
					w = w / 100 * img.getWidth() * 0.7f;
					y = y / 100 * img.getHeight();
					h = h / 100 * img.getHeight() * 0.7f;

					// draw the box to mark it out
					canvas.drawLine(x - w, y - h, x - w, y + h, paint);
					canvas.drawLine(x - w, y - h, x + w, y - h, paint);
					canvas.drawLine(x + w, y + h, x - w, y + h, paint);
					canvas.drawLine(x + w, y + h, x + w, y - h, paint);
				}
				// save new image
				img = bitmap;
				MainActivity.this.runOnUiThread(new Runnable() {

					public void run() {
						// show the image
						imageView.setImageBitmap(img);
						textView.setText("你看起来只有" + age[0] + "岁哦!");
					}
				});

			} catch (JSONException e) {
				e.printStackTrace();
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						textView.setText("Error.");
					}
				});
			}

		}
	};

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_getImage:
			// get a picture form your phone
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/*");
			startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
			break;

		case R.id.btn_detect:
			textView.setText("Waiting ...");
			FaceppDetect faceppDetect = new FaceppDetect();
			faceppDetect.setDetectCallback(detectCallback);
			faceppDetect.detect(img);
			break;
		}
	}

}
