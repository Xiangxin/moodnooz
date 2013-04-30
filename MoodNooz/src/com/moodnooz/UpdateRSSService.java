package com.moodnooz;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdateRSSService extends Service {
	
	public static final String TAG = UpdateRSSService.class.getSimpleName();
	public static final long PERIOD = 2 * 60 * 60 * 1000; // 2hrs
	public static Timer timer;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "service on create");
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
//				String url = MoodNoozUtils.BASE_URL + "?action=update";
//				DefaultHttpClient httpClient = new DefaultHttpClient();
//				HttpGet httpGet = new HttpGet(url);
//				HttpResponse httpResponse = null;
//				Log.i(TAG, "request: " + httpGet.getRequestLine().toString());
//				try {
//					httpResponse = httpClient.execute(httpGet);
//					Log.i(TAG, "update RSS response: " + httpResponse.getStatusLine().getStatusCode());
//					
//					String body = MoodNoozUtils.getStringResponseData(httpResponse);
//					Log.d(TAG, "update RSS response body: " + body);
//					
//				} catch (Exception e) {
//					Log.e(TAG, e.getLocalizedMessage());
//				}
			}
		}, 0, PERIOD);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// don't provide any binding
		return null;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.i(TAG, "service stops");
    }   
}
