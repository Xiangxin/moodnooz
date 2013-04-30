package com.moodnooz.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xiangxinsun 
 * This class offers a static method that takes in a url and
 * returns the body text of the linked article.
 */
public class BodyTextFetcher {
	
	public static String getBodyText(String url, String source) {
		try {
			String rawText = getRawText(url);
			rawText = rawText.replace("\r", "");
			rawText = rawText.replace("\n", "");
			String rawBody = null;
			
			Pattern bodyPattern;
			Matcher bodyMatcher;
			
			// Irish Times:
			bodyPattern = Pattern.compile("<p\\s*class\\s*=\\s*\"headline-info\"\\s*.+\\s*<\\/p>.+<ul[^>]+>");
			bodyMatcher = bodyPattern.matcher(rawText);
			if(bodyMatcher.find()) {
				rawBody = bodyMatcher.group();
				// rawBody is in a form of <p class="headline-info">...</p><p>real_body</p><ul...>
				
				if(rawBody != null) {
					
					// remove headline info
					Pattern tempPattern = Pattern.compile("<p>.+");
					Matcher tempMatcher = tempPattern.matcher(rawBody);
					if(tempMatcher.find()) {
						rawBody = tempMatcher.group();
						// rawBody now is in a form of <p>real_body</p><ul...>
					}
				}
			} else {
				
				if(isIrishTimes(source)) {
					bodyPattern = Pattern.compile("<p>\\s*.+\\s*<\\/p>.*<ul[^>]+>"); //old
					bodyMatcher = bodyPattern.matcher(rawText);
					if(bodyMatcher.find()) {
						rawBody = bodyMatcher.group();
						// rawBody is in a form of <p>...</p><p>real_body</p><ul...>
					} else {
						bodyPattern = Pattern.compile("<section>\\s*<p[^>]*>\\s*.+\\s*<\\/p>\\s*<\\/section>"); //new
						bodyMatcher = bodyPattern.matcher(rawText);
						if(bodyMatcher.find()) {
							rawBody = bodyMatcher.group();
							// rawBody is in a form of <section><p (class = balbla)>real_body</p></section>
						}
					}
				} else {
					// Guardian
					bodyPattern = Pattern.compile("<div\\s*id=\\s*\\\"article-body-blocks\\\">\\s*.+\\s*<\\/div>");
					bodyMatcher = bodyPattern.matcher(rawText);
					if(bodyMatcher.find()) {
						rawBody = bodyMatcher.group();
						// rawBody is in a form of <div id="article-body-blocks"><p>real_body</p></div>
					}
					
					bodyPattern = Pattern.compile("(<p>\\s*.+\\s*<\\/p>){3,}");
					bodyMatcher = bodyPattern.matcher(rawText);
					if(bodyMatcher.find()) {
						rawBody = bodyMatcher.group();
						// rawBody is in a form of <p>...</p><p>...</p><p>...</p> ... <p>...</p>
					}
				}
			}

			if(rawBody != null) {
				// removes html tags
				rawBody = rawBody.replaceAll("</p>", "\n");
				rawBody = rawBody.replaceAll("<\\s*\\/{0,1}[^>]*>", "");
				rawBody = rawBody.replaceAll("&nbsp;", " ");
				
				// decode string (Irish Times needs it)
				rawBody = NumericCharacterReference.decode(rawBody, '?');
				
				return rawBody.trim();
			}		
			
		} catch (IOException e) {}
		
		return null;
	}
	
	
	private static boolean isIrishTimes(String source) {
		return source.equals(UCDMoodNoozServlet.SOURCE_THE_IRISH_TIME);
	}


	private static String getRawText(String urlString) throws IOException {

		URL url = new URL(urlString);

		URLConnection connection = url.openConnection();
		
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), "UTF-8"));
			
		StringBuffer stringBuffer = new StringBuffer();
		String inputLine;
		// read from buffer line by line
		while ((inputLine = bufferedReader.readLine()) != null) {
			stringBuffer.append(inputLine + "\n");
		}
		bufferedReader.close();
		return stringBuffer.toString();
	}
	
}
