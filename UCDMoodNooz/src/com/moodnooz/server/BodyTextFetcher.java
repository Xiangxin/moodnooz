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
	
	public static String getBodyText(String link, String source) {
		String body = null;
		
		try {
			String rawText = getRawText(link);
			rawText = rawText.replace("\r", "");
			rawText = rawText.replace("\n", "");
			
			Pattern bodyPattern;
			Matcher bodyMatcher;
			StringBuilder builder = new StringBuilder();
			
			if(source.equals("a")) {
				// <p CLASS="no_name"> body </p> </section>
				
				bodyPattern = Pattern.compile("<p\\s*[Cc][Ll][Aa][Ss][Ss]\\s*=\\s*\"no_name\"\\s*>.+<\\/p>\\s*(<[^<]+>\\s*)*<\\/section>");
				bodyMatcher = bodyPattern.matcher(rawText);
				if(bodyMatcher.find()) {
					body = bodyMatcher.group();
					
					// remove image description
					body = body.replaceAll("<li\\s*[^>]*\\s>\\s*<img.+\\s*<\\s*/\\s*li\\s*>", "");	
					// remove javascript
					body = body.replaceAll("<script\\s*.+</script>", "");				
					// remove everything that is in <p class = "some_other_names"> </p>
					body = body.replaceAll("<p\\s*[Cc][Ll][Aa][Ss][Ss]\\s*=\\s*\"[^Nn]*\"\\s*>\\s*[^<>]+\\s*<\\/p>", "");
					
				}
			} else if(source.equals("b")) {
				// Guardian
				bodyPattern = Pattern.compile("<div\\s*id=\\s*\\\"article-body-blocks\\\">\\s*.+\\s*<\\/div>");
				bodyMatcher = bodyPattern.matcher(rawText);
				if(bodyMatcher.find()) {
					body = bodyMatcher.group();
					// rawBody is in a form of <div id="article-body-blocks">...<p>real_body</p>...</div>
				}
				
				
				bodyPattern = Pattern.compile("(<p>\\s*.+\\s*<\\/p>){3,}\\s*.+\\s*<\\/div>");
				bodyMatcher = bodyPattern.matcher(body);
				if(bodyMatcher.find()) {
					body = bodyMatcher.group();
					// rawBody is in a form of <p>...</p><p>...</p><p>...</p> ... <p>...</p>
				}
				
				int firstIndex = body.indexOf("</div>");
				body = body.substring(0, firstIndex);
				
			} else if(source.equals("c")) {
				bodyPattern = Pattern.compile("<p\\s*class=\"introduction\"[^>]*>.+<!--\\s*.*\\s*story-body\\s*-->");
				bodyMatcher = bodyPattern.matcher(rawText);
				if(bodyMatcher.find()) {
					body = bodyMatcher.group();
					bodyPattern = Pattern.compile("<p>[^<]+</p>");
					bodyMatcher = bodyPattern.matcher(body);
					
					while(bodyMatcher.find()) {
						builder = builder.append(bodyMatcher.group());
					}
				}
				body = builder.toString();
			}
			
			if(body != null) {
								
				// removes html tags
				body = body.replaceAll("</p>\\s*", "\n")
						.replaceAll("<BR/>\\s*", "\n")
						.replaceAll("<br/>\\s*", "\n")
						.replaceAll("<\\s*\\/{0,1}[^>]*>", "")
						.replaceAll("&nbsp;", " ")
						.replaceAll("&quot;", "\"")
						.replaceAll("\n\\s+", "\n") // remove consecutive spaces after a newline																			
						.replaceAll("\\s{3,}", " "); // remove consecutive spaces
				
				// decode string (Irish Times needs it)
				body = NumericCharacterReference.decode(body, '?');
			}

			return body.trim();
			
		} catch(Exception e) {}
	
		return body;
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
