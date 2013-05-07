package com.moodnooz.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.PutException;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.StatusCode;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/**
 * @author xiangxinsun
 * This class receives get requests containing query string and
 * pass back a list of IDs to the client 
 */
@SuppressWarnings("serial")
public class UCDMoodNoozServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UCDMoodNoozServlet.class.getName());
	
	public static final String PARAM_ACTION = "action";
	public static final String PARAM_STRING = "string";
	public static final String PARAM_URL = "url";
	public static final String PARAM_SOURCE = "source";
	public static final String PARAM_PERIOD = "period"; // either "today", "this_week", "this_month" or "this_year"
	public static final String TYPE_QUERY = "query";
	public static final String TYPE_UPDATE = "update";
	
	public static final String FILE_NAME_POSITIVE = "/WEB-INF/positive map tabbed.idx";
	public static final String FILE_NAME_NEGATIVE = "/WEB-INF/negative map tabbed.idx";
	
	public static final String COL_LINK = "url";
	public static final String COL_TITLE = "title";
	public static final String COL_SOURCE = "source";
	public static final String COL_DATE = "date";
	public static final String COL_DESCRIPTION = "description";
	public static final String COL_BODY = "body";
	
	public static final String SOURCE_THE_IRISH_TIME = "a";
	public static final String SOURCE_GUARDIAN = "b";
	public static final String SOURCE_BBC = "c";
	
	public static final int LIMIT = 100;
	PrintWriter getWriter;
		
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		getWriter = resp.getWriter();
		//getWriter.println("raw query : " + req.toString());		
		String type = req.getParameter(PARAM_ACTION);
		if(type != null && type.equalsIgnoreCase(TYPE_UPDATE)) {
			int random = (int) (Math.random() * 3);
			try {
				//deleteAll();
				if(random == 0) {
					fetch("http://www.irishtimes.com/cmlink/news-1.1319192", SOURCE_THE_IRISH_TIME);
					fetch("http://feeds.guardian.co.uk/theguardian/world/rss", SOURCE_GUARDIAN);
					fetch("http://feeds.bbci.co.uk/news/rss.xml?edition=int", SOURCE_BBC);
				} else if(random == 1) {
					fetch("http://feeds.bbci.co.uk/news/rss.xml?edition=int", SOURCE_BBC);
					fetch("http://www.irishtimes.com/cmlink/news-1.1319192", SOURCE_THE_IRISH_TIME);
					fetch("http://feeds.guardian.co.uk/theguardian/world/rss", SOURCE_GUARDIAN);
				} else {
					fetch("http://feeds.guardian.co.uk/theguardian/world/rss", SOURCE_GUARDIAN);
					fetch("http://feeds.bbci.co.uk/news/rss.xml?edition=int", SOURCE_BBC);
					fetch("http://www.irishtimes.com/cmlink/news-1.1319192", SOURCE_THE_IRISH_TIME);	
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
	}

	public void fetch(String url, String source) {
		
		try {
			URL rssfeed = new URL(url);
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document document = builder.parse(rssfeed.openStream());
			
			NodeList nodes = document.getElementsByTagName("item");
			
			//getWriter.write("node length: " + nodes.getLength() + "\n");

			for(int i = 0; i < nodes.getLength(); i++) {
				
				Element element = (Element)nodes.item(i);
				
				//getWriter.write("\nelement" + i + "\n ");
				
				Document doc = null;
				try {
					String link = UCDMoodNoozUtils.getElementValue(element,"link").trim();
					//getWriter.write(link + "\n");
					String title = UCDMoodNoozUtils.getElementValue(element,"title").trim();
					String dateString = UCDMoodNoozUtils.getElementValue(element,"pubDate").trim();		
					Date date = UCDMoodNoozUtils.getDateFromString(dateString);
					String description = UCDMoodNoozUtils.getElementValue(element,"description").trim();
					if(source.equals("b")) {
						Pattern pattern = Pattern.compile("<p\\s*[Cc][Ll][Aa][Ss][Ss]\\s*=\\s*\"standfirst\"\\s*>[^<]+<\\s*\\/{0,1}[^>]*\\s*>");
						Matcher matcher = pattern.matcher(description);
						if(matcher.find()) {
							description = matcher.group();
							description = description.replaceAll("<\\s*\\/{0,1}[^>]*\\s*>", "");
						}
					}
					
					//getWriter.write(description + "\n");
					
					String body = BodyTextFetcher.getBodyText(link, source);
					
					doc = Document.newBuilder().setId(link)
						    .addField(Field.newBuilder().setName("link").setAtom(link))
						    .addField(Field.newBuilder().setName("title").setAtom(title))
						    .addField(Field.newBuilder().setName("date").setDate(date))
						    .addField(Field.newBuilder().setName("source").setAtom(source))
						    .addField(Field.newBuilder().setName("description").setAtom(description))
						    .addField(Field.newBuilder().setName("body").setText(body))
						    .build();
					
				} catch (Exception e1) {
					//getWriter.write("aha! " + e1.getMessage() + "\n");
				}
			    
				try {
					getIndex().put(doc);
				} catch (PutException e) {
					if (StatusCode.TRANSIENT_ERROR.equals(e
							.getOperationResult().getCode())) {
						e.printStackTrace();
					}
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Index getIndex() {
	    IndexSpec indexSpec = IndexSpec.newBuilder().setName("webdoc").build();
	    return SearchServiceFactory.getSearchService().getIndex(indexSpec);
	}
	
	// Simulate post request : 
	//curl -v -H "Accept: application/json" -H "Content-type: application/json" -X POST -d ' {"string":"london +traffic -real ?easy government", "period:this_month"}' http://localhost:8888
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		
		String queryString = req.getParameter(PARAM_STRING);
		String period = req.getParameter(PARAM_PERIOD);
		
		String url = req.getParameter(PARAM_URL);
		String simpSource = req.getParameter(PARAM_SOURCE);
		
//		debug :
//		writer.write("Received request: " + req.toString());
//		@SuppressWarnings("unchecked")
//		Enumeration<String> en = req.getParameterNames();
//	    while (en.hasMoreElements()){
//	    	writer.write("param name: " + en.nextElement());
//	    }
//		writer.write("Received query string: " + queryString + " and period: " + period);
		
		
		if (url != null && simpSource != null) {
			// writer.write("Received query link: " + url + " and source: " + simpSource);
			try {
				resp.setCharacterEncoding("UTF-8");
				resp.setContentType("text/plain");
				writer.write(BodyTextFetcher.getBodyText(url, simpSource));
			} catch (Exception e) {}
			return;
		}
	
		if(queryString == null) return;
		queryString = queryString.toLowerCase();
				
		// server response
		JSONObject object = new JSONObject(); 
		Vector<String> essential = new Vector<String>();
		Vector<String> positive = new Vector<String>();
		Vector<String> negative = new Vector<String>();
		Vector<String> both = new Vector<String>();
		JSONArray documents = new JSONArray();		
		
		// initialize positive and negative affect maps
		ServletContext context = getServletContext();
		InputStream is = context.getResourceAsStream(FILE_NAME_POSITIVE);
		UCDMoodNoozUtils.initializeMap(is, true);
		is = context.getResourceAsStream(FILE_NAME_NEGATIVE);
		UCDMoodNoozUtils.initializeMap(is, false);
		
		UCDMoodNoozUtils.initializeIrregularVerbMap();
		
		StringBuilder queryStringBuilder = new StringBuilder();
		String dateFilterString = UCDMoodNoozUtils.getDateFilterString(period);
		if(dateFilterString != null)
			queryStringBuilder.append(dateFilterString + " ");
		String[] words = queryString.split(" ");
		if(words != null) {
			for(int i = 0; i < words.length; i++) {
				String word = words[i];
				Vector<String> associations = null; 
				if(word.startsWith("+")) {
					word = UCDMoodNoozUtils.getRootForm(word.substring(1));
					associations = UCDMoodNoozUtils.getPositiveAssociation(word);
					positive.addAll(associations);
				} else if(word.startsWith("-")) {
					word = UCDMoodNoozUtils.getRootForm(word.substring(1));
					associations = UCDMoodNoozUtils.getNegativeAssociation(word);
					negative.addAll(associations);
				} else if(word.startsWith("?")) {
					word = UCDMoodNoozUtils.getRootForm(word.substring(1));
					associations = UCDMoodNoozUtils.getAssociation(word);
					both.addAll(associations);
				} else {
					essential.add(word);
					queryStringBuilder.append("body:" + word + " ");
				}
				
				if(associations != null) {
					queryStringBuilder.append("body:(");
					for(int j = 0; j < associations.size(); j++) {
						String asso = associations.get(j);
						queryStringBuilder.append(asso);
						if(j < associations.size() - 1)
							queryStringBuilder.append(" OR ");
					}
					queryStringBuilder.append(") ");
				}
			}
			queryString = queryStringBuilder.toString();
//			writer.write("built search string : " + queryString);
			
				
//			SortOptions sortOptions = SortOptions.newBuilder()
//					.addSortExpression(SortExpression.newBuilder()
//									.setExpression(String.format(
//									    "%s + rating * 0.01", SortExpression.SCORE_FIELD_NAME))
//									.setDirection(SortExpression.SortDirection.DESCENDING)
//									.setDefaultValueNumeric(0)).setLimit(LIMIT).build();
//			
//			QueryOptions options = QueryOptions.newBuilder().setLimit(LIMIT)
//					.setFieldsToSnippet("body")
//					.setFieldsToReturn("link", "title", "source", "date", "description")
//					.setSortOptions(sortOptions).build();
//			
//			Query query = Query.newBuilder().setOptions(options).build(queryString);
			
			Results<ScoredDocument> results = getIndex().search(queryString);	
			if (results != null) {
				for (ScoredDocument scoredDocument : results) {
					JSONObject doc = new JSONObject();
					try {
						doc.put("link", scoredDocument.getOnlyField("link").getAtom());
						doc.put("title", scoredDocument.getOnlyField("title").getAtom());
						doc.put("source", scoredDocument.getOnlyField("source").getAtom());
						doc.put("date", scoredDocument.getOnlyField("date").getDate().toString());
						doc.put("description", scoredDocument.getOnlyField("description").getAtom());
						documents.put(doc);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			
			// server response:
			try {
				JSONObject wordsToHighlight = new JSONObject();
				wordsToHighlight.put("essential", essential);
				wordsToHighlight.put("positive", positive);
				wordsToHighlight.put("negative", negative);
				wordsToHighlight.put("both", both);
				object.put("words", wordsToHighlight);
				object.put("documents", documents);
				
				log.info("Returning json: " + object.toString());

				resp.setCharacterEncoding("UTF-8");
				resp.setContentType("application/json");
				writer.write(object.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void deleteAll() {
		try {
		    while (true) {
		        List<String> docIds = new ArrayList<String>();
		        // Return a set of document IDs.
		        GetRequest request = GetRequest.newBuilder().setReturningIdsOnly(true).build();
		        GetResponse<Document> response = getIndex().getRange(request);
		        if (response.getResults().isEmpty()) {
		            break;
		        }
		        for (Document doc : response) {
		            docIds.add(doc.getId());
		        }
		        getIndex().delete(docIds);
		    }
		} catch (RuntimeException e) {
		    
		}
	}
}
