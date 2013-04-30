package com.moodnooz;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;

public class MoodNoozUtils {
	
	public static final String BASE_URL = "http://ucdmoodnooz.appspot.com/ucdmoodnooz";
	
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager conMgr =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		boolean availability = false;
		try {
			if (conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() ==
				NetworkInfo.State.CONNECTED || conMgr.getNetworkInfo(ConnectivityManager
				.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
				availability = true;
			}
		} catch (Exception e) {
			return true;
		}
		return availability;
	}
	
	public static String getStringResponseData(HttpResponse httpResponse)
			throws ParseException, IOException {
		if(httpResponse == null) {
			return "httpResponse is null!";
		} else if(httpResponse.getEntity() == null) {
			return "httpResponse entity is null";
		} else {
			return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
		}
	}
}
