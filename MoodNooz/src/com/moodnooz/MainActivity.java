package com.moodnooz;

import java.util.Vector;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
	public static final String NAME_SEARCH_STRING = "searchString";
	public static final String NAME_DATE_FILTER_STRING = "dateFilterString";
		
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
    
    public void infoButtonClicked(View view) {
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
    		Intent resultIntent = new Intent(this, SearchResultActivity.class);
    		resultIntent.putExtra(NAME_SEARCH_STRING, searchString);
    		resultIntent.putExtra(NAME_DATE_FILTER_STRING, dateFilterString);
    		startActivity(resultIntent);		
    	} else {
			Toast.makeText(
					MainActivity.this,
					"Network is not available. Please check internet connection.",
					Toast.LENGTH_LONG).show();
    	}
    }
}
