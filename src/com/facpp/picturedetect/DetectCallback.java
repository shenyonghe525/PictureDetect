package com.facpp.picturedetect;

import org.json.JSONObject;
import android.os.Handler;
/**
 *  定义获取到图片信息后的回调接口
 * @author shenyonghe
 *
 * 2015-7-6
 */
public interface DetectCallback {
	void detectResult(JSONObject rst, Handler handler);
}
