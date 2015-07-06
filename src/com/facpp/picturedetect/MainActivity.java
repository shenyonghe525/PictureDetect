package com.facpp.picturedetect;

import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A simple demo, get a picture form your phone<br />
 * Use the facepp api to detect<br />
 * Find all face on the picture, and mark them out.
 * 
 * @author shenyonghe
 */
@SuppressLint("HandlerLeak") public class MainActivity extends Activity implements OnClickListener,
		DetectCallback {

	final private int PICTURE_CHOOSE = 1;

	private ImageView imageView = null;
	private Bitmap img = null;
	private Button buttonDetect = null;
	private TextView textView = null;
	private int[] age;
	
	public static final int MSG_ERROR = 0x111;
	private static final int MSG_SUCEE = 0x112;
	private static final int MSG_NONE = 0x113;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
           switch (msg.what) {
		case MSG_ERROR:
			textView.setText("net error");
			break;
		case MSG_SUCEE:
			imageView.setImageBitmap(img);
			textView.setText("你看起来只有" + age[0] + "岁哦!");
			break;
		case MSG_NONE:
			textView.setText("没有找到任何人!");
			break;
		}
		};
	};

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
				cursor.close();
				// 压缩图片，因为每张图片不能超过3M
				Options options = new Options();
				options.inJustDecodeBounds = true;
				img = BitmapFactory.decodeFile(fileSrc, options);
				// 图片长宽方向缩小倍数（1M等于100万像素）
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
			FaceppDetect faceppDetect = new FaceppDetect(mHandler,img);
			faceppDetect.setDetectCallback(this);
			faceppDetect.detect(img);
			break;
		}
	}

	// 获取到图片信息后的处理(所属子线程)
	public void detectResult(JSONObject rst, Handler handler) {
		// TODO Auto-generated method stub
		System.out.println("get face Info:\n" + rst);
		// use the red paint
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 100f);
		// create a new canvas
		Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(),
				img.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(img, new Matrix(), null);
		try {
			// find out all faces
			final int count = rst.getJSONArray("face").length();
			if (count == 0) {
				Message message = Message.obtain();
				message.what = MSG_NONE;
				handler.sendMessage(message);
				return;
			}
			age = new int[count];
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
			Message message1 = Message.obtain();
			message1.arg1 = age[0];
			message1.what = MSG_SUCEE;
			handler.sendMessage(message1);
//			activity.runOnUiThread(new Runnable() {
//
//				public void run() {
//					// show the image
//					imageView.setImageBitmap(img);
//					textView.setText("你看起来只有" + age[0] + "岁哦!");
//				}
//			});

		} catch (JSONException e) {
			e.printStackTrace();
			Message message2 = Message.obtain();
			message2.what = MSG_ERROR;
			handler.sendMessage(message2);
//			activity.runOnUiThread(new Runnable() {
//				public void run() {
//					textView.setText("Error.");
//				}
//			});
		}
	}

}
