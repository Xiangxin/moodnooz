package com.moodnooz;

import java.util.ArrayList;
import java.util.List;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SearchResultActivity extends Activity {
	
	public static final String TAG = SearchResultActivity.class.getSimpleName();
	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_SOURCE = "source";
	public static final String EXTRA_SIMP_SOURCE = "simpSource";
	public static final String EXTRA_DATE= "date";
	public static final String EXTRA_LINK = "link";
	
	SearchResultAdapter adapter;
	ProgressBar progress;
	LinearLayout searchFailLayout;
	TextView searchFailTextView;
	ListView searchResultList;
	View summaryButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.search_result_layout);
        
        progress = (ProgressBar) findViewById(R.id.progress);
        searchFailLayout = (LinearLayout) findViewById(R.id.search_fail_layout);
        searchFailTextView = (TextView) findViewById(R.id.fail_msg);
        Drawable img = getResources().getDrawable(R.drawable.sad_face);
    	img.setBounds(0, 0, 50, 50);
    	searchFailTextView.setCompoundDrawables(img, null, null, null);
    	summaryButton = findViewById(R.id.summary_button);
        searchResultList = (ListView) findViewById(R.id.search_result_list);
        
        String searchString = getIntent().getStringExtra(MainActivity.NAME_SEARCH_STRING);
        String dateFilterString = getIntent().getStringExtra(MainActivity.NAME_DATE_FILTER_STRING);
        new SendSearchStringToServerTask().execute(new String[]{searchString, dateFilterString});
        
        setupListView();
	}
	
	private void setupListView() {
		if(searchResultList == null)
			searchResultList = (ListView) findViewById(R.id.search_result_list);
		searchResultList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Document doc = adapter.getItem(pos);
				Intent intent = new Intent(SearchResultActivity.this, BodyTextActivity.class);
				intent.putExtra(EXTRA_TITLE, doc.title);
				intent.putExtra(EXTRA_SIMP_SOURCE, doc.simplifiedSource);
				intent.putExtra(EXTRA_SOURCE, doc.source);
				intent.putExtra(EXTRA_DATE, doc.date);
				intent.putExtra(EXTRA_LINK, doc.link);
				startActivity(intent);
			}
		});
	}
	
	public void backButtonClicked(View view) {
		finish();
	}
	
	public void summaryButtonClicked(View view) {
		Toast.makeText(SearchResultActivity.this, "Not implemented yet :(", Toast.LENGTH_LONG).show();
	}

	public class SendSearchStringToServerTask extends AsyncTask<String, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(String... params) {
			if(params == null) {
				Log.e(TAG, "Array of two string constraints is null!");
				return null;
			}
			
			String searchString = params[0];
			String dateFilterString = params[1];

			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpost = new HttpPost(MoodNoozUtils.BASE_URL);
			HttpResponse httpResponse = null;

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
			nameValuePairs.add(new BasicNameValuePair("string",
					getSanitizedQueryString(searchString)));
			nameValuePairs.add(new BasicNameValuePair("period",
					dateFilterString));

			try {
				httpost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpResponse = httpclient.execute(httpost);

				Log.i(TAG, "server response code: "
						+ httpResponse.getStatusLine().getStatusCode());

				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					String responseBody = MoodNoozUtils
							.getStringResponseData(httpResponse);
					// Log.i(TAG, "response body: \"" + responseBody + "\"");
					return ((JSONObject) new JSONTokener(responseBody)
							.nextValue());
				}
			} catch (Exception e) {
				Log.e(TAG, "error at SendSearchStringToServerTask doInBackground()"
								+ e.getLocalizedMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(JSONObject responseObject) {
			if(progress != null)
				progress.setVisibility(View.GONE);
			
			if (responseObject == null) {
				Log.i(TAG, "server respond body is null!");
				searchResultList.setVisibility(View.GONE);
				summaryButton.setVisibility(View.INVISIBLE);
				searchFailLayout.setVisibility(View.VISIBLE);
				searchFailTextView.setText(getString(R.string.search_error));
				return;
			}
				
			try {
				JSONArray documents = responseObject.getJSONArray("documents");
				
				if(documents == null || documents.length() == 0) {
					Log.i(TAG, "no documents!");
					searchResultList.setVisibility(View.GONE);
					summaryButton.setVisibility(View.INVISIBLE);
					searchFailLayout.setVisibility(View.VISIBLE);
					searchFailTextView.setText(getString(R.string.no_result));
					return;
				}
				
				summaryButton.setVisibility(View.VISIBLE);
				searchResultList.setVisibility(View.VISIBLE);
				searchFailLayout.setVisibility(View.GONE);
				
				List<Document> adapterFood = new ArrayList<Document>();
				for (int i = 0; i < documents.length(); i++) {
					JSONObject doc = documents.getJSONObject(i);
					
					adapterFood.add(new Document(doc.getString("link"), doc
							.getString("title"), doc.getString("source"), doc
							.getString("date"), doc.getString("description"),
							null));
				}
				
				adapter = new SearchResultAdapter(SearchResultActivity.this, adapterFood);
				searchResultList.setAdapter(adapter);
				
			} catch (Exception e) {
				searchResultList.setVisibility(View.GONE);
				summaryButton.setVisibility(View.INVISIBLE);
				searchFailLayout.setVisibility(View.VISIBLE);
				searchFailTextView.setText(getString(R.string.search_error));
			}
			
			try {
				JSONObject words = responseObject.getJSONObject("words");

				JSONArray essential = (JSONArray) words.get("essential");
				JSONArray positive = (JSONArray) words.get("positive");
				JSONArray negative = (JSONArray) words.get("negative");
				JSONArray both = (JSONArray) words.get("both");
				
				SharedPreferences prefs = MoodNoozUtils.getSharedPreferences(getApplicationContext());
				prefs.edit().clear().commit();
				
				int i;
				for(i = 0; i < essential.length(); i++) {
					prefs.edit().putString(essential.getString(i), "e");
				}
				for(i = 0; i < positive.length(); i++) {
					prefs.edit().putString(positive.getString(i), "p");
				}
				for(i = 0; i < negative.length(); i++) {
					prefs.edit().putString(negative.getString(i), "n");
				}
				for(i = 0; i < both.length(); i++) {
					prefs.edit().putString(both.getString(i), "b");
				}
				prefs.edit().commit();
				Log.i(TAG, "essential: " + essential.toString());
				Log.i(TAG, "positive: " + positive.toString());
				Log.i(TAG, "negative: " + negative.toString());
				Log.i(TAG, "both: " + both.toString());
			} catch (JSONException e) {
				Log.e(TAG, "error at SendSearchStringToServerTask onPostExecute() parsing words: "
								+ e.getMessage());
			}
		}
	}
    
    private String getSanitizedQueryString(String searchString) {
    	String sanitizedString = new String(searchString);
    	/*
    	 * 
    	 * import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;


public class sanitise {
	
public static String splitter(String query) throws Exception{
	
	query = query.toLowerCase();
	int count = 0;
	char current;
	String buffer = "", root = "";
	while (count < query.length()) {
		current = query.charAt(count);
		
			if (isAlphanumeric(current) || current == ' ') {
				buffer = buffer + current;
			} else if(current == '+' || current == '-' || current == '?') {
				if(isAlphanumeric(query.charAt(count + 1)))
					buffer = buffer + current;
			}
			
		count++;
	}
	
	System.out.println(buffer);

	String[] tokens = buffer.split(" ");
	for(String token : tokens) {

		System.out.println(token.length());
		if(token == null || token.length() == 0)
			continue;
		if(token.charAt(0) == '+' || token.charAt(0) == '-' || token.charAt(0) == '?') {
			token = token.charAt(0) + getRootForm(token.substring(1));
		} else {
			token = getRootForm(token);
		}
	}
	
	System.out.println(buffer);
	
	Set<String> set = new HashSet<String>();
	for(String token : tokens) {
		set.add(token);
	}
	
	String result = "";
	Iterator<String> iter = set.iterator();
	while(iter.hasNext()) {
		result += iter.next() + " ";
	}
	
	
	System.out.println(buffer);
	return result;
}


private static boolean isAlphanumeric(char c) {
	return ('a' <= c && c <= 'z') || ('0' <= c && c <= '9');
}

public static String getRootForm(String w)throws Exception{
		
		boolean root = false;
		while (root == false){
		BufferedReader br = new BufferedReader(new FileReader("irregular.txt"));
		String line, buff;
		while ((line = br.readLine()) != null) {
			 StringTokenizer t = new StringTokenizer(line, "  	\t", false);
			 buff = t.nextToken();
			 while (t.hasMoreTokens()){
				 if(w.equals(t.nextToken())){
					w = buff;
					root = true;
				 }
			 }
		}
		br.close();
		
		if (w.length() > 3){
			
			while(root == false){
				 String end = w.substring(w.length() - 3);
				 String ed = w.substring(w.length() - 2);
				 String s = w.substring(w.length() - 1);
				 				   
				  if (s.equals("s")){
				   
				   if(end.equals("ses") || end.equals("xes") || end.equals("zes") || end.equals("hes") || end.equals("ies")){
				   
					   if(end.equals("ses") || end.equals("xes") || end.equals("zes")){
				        	w = w.substring(0, w.length() - 2);
				        	root = true;
				        	
				         }
					   else if(end.equals("hes") ){
				        	 end = w.substring(w.length() - 4);
				        	 if (end.equals("thes")){
				 	        	w = w.substring(0, w.length() - 2);
					        	root = true;
				        	 }
				         }
					   else if (end.equals("ies")){
				        	 w = w.substring(0, w.length() - 3) + "y";
				        	 root = true;
				         }
				   				   
				   }
				   
				   else{
				   w = w.substring(0, w.length() - 1);
		        	root = true;
				  } 
				   
				  }
				  				 
	         else if(end.equals("ing")){
	        	 w = w.substring(0, w.length() - 3);
	        	 if(w.charAt(w.length()- 1) == w.charAt(w.length()- 2)){
	        		 w = w.substring(0, w.length() - 1);
	        	 }
	        	 root = true;
	         }
	         else if (end.equals("ied")){
	        	 w = w.substring(0, w.length() - 3) + "y";
	        	 root = true;
	         }
	         else if (ed.equals("ed")){
	        	 w = w.substring(0, w.length() - 2);
	        	 if(w.charAt(w.length()- 1) == w.charAt(w.length()- 2)){
	        		 w = w.substring(0, w.length() - 1);
	        	 }
	        	 root = true;	        	 
	         }
	         else{
	        	 root = true;
	         }
			}
			
		}
		else{
			root = true;
		}
		}
		w = w.toLowerCase();
		return w;
	}

public static void main(String[] args) throws Exception {
	
	// LoDon + traffic \"\"$%££%^&&$£ hah
	// London +tTraffic ??easy easy
	// cats cat +cAt
	
	String lol = "drappery horsed +pixies";
	lol = splitter(lol);
		
}
}

    	 */
    	return sanitizedString;
    }
}
