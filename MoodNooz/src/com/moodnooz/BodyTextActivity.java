package com.moodnooz;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BodyTextActivity extends Activity {
	public static final String TAG = BodyTextActivity.class.getSimpleName();
	String link, source;
	TextView titleView;
	TextView sourceView;
	TextView dateView;
	TextView linkView;
	TextView bodyView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.body_text_layout);
        
        titleView = (TextView)findViewById(R.id.body_title);
        sourceView = (TextView)findViewById(R.id.body_source);
        dateView = (TextView)findViewById(R.id.body_date);
        linkView = (TextView)findViewById(R.id.body_link);
        bodyView = (TextView)findViewById(R.id.body);
        
        titleView.setText(getIntent().getStringExtra(SearchResultActivity.EXTRA_TITLE));
        source = getIntent().getStringExtra(SearchResultActivity.EXTRA_SOURCE);
        sourceView.setText(source);
        dateView.setText(getIntent().getStringExtra(SearchResultActivity.EXTRA_DATE));
        link = getIntent().getStringExtra(SearchResultActivity.EXTRA_LINK);
        linkView.setText(link);
        bodyView.setText(getBodyText());
	}

	private CharSequence getBodyText() {
		
		return null;
	}
}
