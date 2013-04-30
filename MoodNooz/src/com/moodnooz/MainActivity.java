package com.moodnooz;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String TAG = MainActivity.class.getSimpleName();
	public static final int tickSize = 25;
		
	EditText searchBox;
	String dateFilterString;
	String searchString;
	Dialog dateFilterDialog;
	View okBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, UpdateRSSService.class));
        searchBox = (EditText) findViewById(R.id.search_box);
        if(searchBox != null)
        	searchString = searchBox.getText().toString();
        dateFilterString = getString(R.string.none);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void info_button_Clicked(View view) {
    	Toast.makeText(MainActivity.this, "Not implemented yet :(", Toast.LENGTH_LONG).show();
    }

    public void calendarButtonClicked(View view) {
    	if(dateFilterDialog == null) {
    		dateFilterDialog = new Dialog(MainActivity.this, R.style.dialogstyle);
        	dateFilterDialog.setContentView(R.layout.date_filter_dialog);
    	}
    	
    	if(okBtn == null) {
        	okBtn = dateFilterDialog.findViewById(R.id.date_ok);
        	okBtn.setOnClickListener(new OnClickListener(){

    			@Override
    			public void onClick(View v) {
    		    	Log.i(TAG, "ok button clicked.");
    				dateFilterDialog.dismiss();
    			}		
        	});
    	}

    	final Drawable img = MainActivity.this.getResources().getDrawable(R.drawable.tick);
    	img.setBounds(0, 0, tickSize, tickSize);
    	final Vector<TextView> dateChoices = new Vector<TextView>();
    	
    	TextView today = (TextView) dateFilterDialog.findViewById(R.id.today);
    	TextView this_week = (TextView) dateFilterDialog.findViewById(R.id.this_week);
    	TextView this_month = (TextView) dateFilterDialog.findViewById(R.id.this_month);
    	TextView this_year = (TextView) dateFilterDialog.findViewById(R.id.this_year);
    	TextView none = (TextView) dateFilterDialog.findViewById(R.id.none);
    	
    	none.setCompoundDrawables(null , null, img, null);
    	
    	dateChoices.add(today);
    	dateChoices.add(this_week);
    	dateChoices.add(this_month);
    	dateChoices.add(this_year);
    	dateChoices.add(none);
    	
    	View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(TAG, "date chosen.");
				
				TextView chosen = (TextView) v;
				if(chosen == null) {
					Log.i(TAG, "chosen date view is null!");
					return;
				}
		    	
				for(TextView tv : dateChoices) {
					if(tv != null)
						tv.setCompoundDrawables(null, null, null, null);
				}
				
		    	chosen.setCompoundDrawables(null , null, img, null);
		    	
				dateFilterString = chosen.getText().toString();
			}
		};
    	
    	for(TextView tv : dateChoices) {
    		if(tv != null)
    			tv.setOnClickListener(listener);
    	}
    	
    	dateFilterDialog.show();
    }    
    
    public void searchButtonClicked(View view) {
    	searchString = searchBox.getText().toString();
    	Log.i(TAG, "date: " + dateFilterString + ", search string: " + searchString);
    	if(MoodNoozUtils.isNetworkAvailable(getApplicationContext())) {
    		new SendSearchStringToServerTask().execute();
    	} else {
			Toast.makeText(
					MainActivity.this,
					"Network is not available. Please check internet connection.",
					Toast.LENGTH_LONG).show();
    	}
    }
    
    public class SendSearchStringToServerTask extends AsyncTask<Void, Void, JSONObject> {
    	
		@Override
		protected JSONObject doInBackground(Void... params) {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpost = new HttpPost(MoodNoozUtils.BASE_URL);
			HttpResponse httpResponse = null;
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
			nameValuePairs.add(new BasicNameValuePair("string", getSanitizedQueryString()));
			nameValuePairs.add(new BasicNameValuePair("period", dateFilterString));
			
			try {
				httpost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpResponse = httpclient.execute(httpost);
				
				Log.i(TAG, "server response code: " + httpResponse.getStatusLine().getStatusCode());
				
				if(httpResponse.getStatusLine().getStatusCode() == 200) {
					String responseBody = MoodNoozUtils.getStringResponseData(httpResponse);
					Log.i(TAG, "response body: \"" + responseBody + "\"");
					return ((JSONObject) new JSONTokener(responseBody).nextValue());
				}
			} catch (Exception e) {
				Log.e(TAG, "error at SendSearchStringToServerTask doInBackground()"
								+ e.getLocalizedMessage());
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(JSONObject responseObject) {
			if(responseObject == null) {
				Toast.makeText(MainActivity.this, "Server response error", Toast.LENGTH_LONG).show();
				return;
			}
			
			try {
				JSONObject words = responseObject.getJSONObject("words");
				
				JSONArray essential =  (JSONArray) words.get("essential");
				JSONArray positive = (JSONArray) words.get("positive");
				JSONArray negative = (JSONArray) words.get("negative");
				JSONArray both = (JSONArray) words.get("both");
				
				Log.i(TAG, "essential: " + essential.toString());
				Log.i(TAG, "positive: " + positive.toString());
				Log.i(TAG, "negative: " + negative.toString());
				Log.i(TAG, "both: " + both.toString());

				JSONArray documents = responseObject.getJSONArray("documents");
				for(int i = 0; i < documents.length(); i++) {
					JSONObject doc = documents.getJSONObject(i);
					Log.i(TAG, doc.getString("title") + " | " + doc.getString("link")
									+ " | " + doc.getString("date") + " | "
									+ doc.getString("source") + " | "
									+ doc.getString("description"));
				}
			} catch (Exception e) {
				Log.e(TAG, "error at SendSearchStringToServerTask onPostExecute()"
								+ e.getLocalizedMessage());
			}
		}
    }
    
    private String getSanitizedQueryString() {
    	String sanitizedString = new String(searchString);
    	return sanitizedString;
    }
}
