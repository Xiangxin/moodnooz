package com.moodnooz.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UCDMoodNoozUtils {

	static HashMap<String, Vector<String>> posMap = new HashMap<String, Vector<String>>();
	static HashMap<String, Vector<String>> negMap = new HashMap<String, Vector<String>>();
	static HashMap<String, String> irregularVerbMap = new HashMap<String, String>();
	public static final String FILE_NAME_IRREGULAR_VERB = "irregular verb.idx";
	
	/**
	 * @param dateString
	 * @return a java Date object that correspond to the dateString
	 * Convert the dateString to a Date object
	 */
	public static Date getDateFromString(String dateString) {
		// The Irish Time: "Mon, 15 Apr 2013 06:55:14 +0000"
		// Guardian / BBC: Wed, 17 Apr 2013 12:57:50 GMT
		
		String[] elements = dateString.split("[:\\s+]");
		int year = Integer.parseInt(elements[3]);
		int month = getMonth(elements[2].toLowerCase());
		int date = Integer.parseInt(elements[1]);
		int hour = Integer.parseInt(elements[4]);
		int minute = Integer.parseInt(elements[5]);
		int second = Integer.parseInt(elements[6]);
			
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, date, hour, minute, second);
		
		return cal.getTime();
	}

	public static int getMonth(String str) {
		if(str.equals("jan"))
			return Calendar.JANUARY;
		if(str.equals("feb"))
			return Calendar.FEBRUARY;
		if(str.equals("mar"))
			return Calendar.MARCH;
		if(str.equals("apr"))
			return Calendar.APRIL;
		if(str.equals("may"))
			return Calendar.MAY;
		if(str.equals("jun"))
			return Calendar.JUNE;
		if(str.equals("jul"))
			return Calendar.JULY;
		if(str.equals("aug"))
			return Calendar.AUGUST;
		if(str.equals("sep"))
			return Calendar.SEPTEMBER;
		if(str.equals("oct"))
			return Calendar.OCTOBER;
		if(str.equals("nov"))
			return Calendar.NOVEMBER;
		if(str.equals("dec"))
			return Calendar.DECEMBER;
		
		return -1;
	}
	
	public static String getElementValue(Element parent, String label) {
		Element e = (Element) parent.getElementsByTagName(label).item(0);
		
		try {
			Node child = e.getFirstChild();
			if (child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				return cd.getData();
			}
		} catch (Exception ex) {}
		return "";
	}
	
	public static void initializeMap(InputStream is, boolean isPositive) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = null;
			while((line = reader.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line, "\t\n", false);
				String key = null;
				Vector<String> tokens = new Vector<String>();
				if(tokenizer.hasMoreTokens()) 
					key = tokenizer.nextToken();
				
				while(tokenizer.hasMoreTokens()) {
					tokens.add(tokenizer.nextToken());
				}
				if(key != null) {
					if(isPositive)
						posMap.put(key, tokens);
					else
						negMap.put(key, tokens);
				}
			}
		} catch (Exception e) {}
	}
	
	public static void initializeIrregularVerbMap() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(FILE_NAME_IRREGULAR_VERB)));
			String line = null;
			while((line = reader.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line, "\t\n", false);
				String root = null;
				if(tokenizer.hasMoreTokens()) 
					root = tokenizer.nextToken().trim();
				
				while(tokenizer.hasMoreTokens()) {
					irregularVerbMap.put(tokenizer.nextToken().trim(), root);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File not found in the system.");
		} catch(IOException e1) {
			System.out.println("IOException");
		}
	}

	public static Vector<String> getPositiveAssociation(String word) {
		Vector<String> result = new Vector<String>();
		result.add(word);
		try {
			result.addAll(posMap.get(word));
		} catch (Exception e) { }
		return result;
	}
	
	public static Vector<String> getNegativeAssociation(String word) {
		Vector<String> result = new Vector<String>();
		result.add(word);
		try {
			result.addAll(negMap.get(word));
		} catch (Exception e) { }
		return result;
	}
	
	public static Vector<String> getAssociation(String word) {
		Vector<String> result = new Vector<String>();
		result.add(word);
		try {
			result.addAll(posMap.get(word));
			result.addAll(negMap.get(word));
		} catch (Exception e) { }
		return result;
	}
	
	/**
	 * @param period : today, this_week, this_month, this_year, none
	 * @return a date filter string for making query. e.g. date >= 2013-1-1 for this_year
	 * return null if period is none.
	 */
	@SuppressWarnings("deprecation")
	public static String getDateFilterString(String period) {
		if(period == null) return null;
		Calendar calendar = Calendar.getInstance();		
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1; // 1 - 12 
		int date = calendar.get(Calendar.DATE); // 1 - 31
		
		String dateFilterString = null;
		
		if(period.equalsIgnoreCase("today")) {
			dateFilterString = "date >= " + year + "-" + month + "-" + date; 
		} else if(period.contains("week")) {
			int offset = calendar.get(Calendar.DAY_OF_WEEK) - calendar.getFirstDayOfWeek();
			long timeInMillis = calendar.getTimeInMillis() - offset * 24 * 60 * 60 * 1000;
			Date firstDay = new Date(timeInMillis);
			dateFilterString = "date >= " + (firstDay.getYear() + 1900) + "-"
					+ firstDay.getMonth() + "-" + firstDay.getDate();
		} else if(period.contains("month")) {
			dateFilterString = "date >= " + year + "-" + month + "-1"; 
		} else if(period.contains("year")) {
			dateFilterString = "date >= " + year + "-1-1";
		}
		
		return dateFilterString;
	}

	public static boolean containsWord(String word) {
		return posMap.containsKey(word) || negMap.containsKey(word);
	}
	
	public static String getRootForm(String originalWord) {
		String word = originalWord.trim().toLowerCase();
		if(containsWord(word)) return originalWord;
		
		String modifiedWord = null;
		if(word.length() >= 4) {
			if (word.endsWith("thes") || word.endsWith("zes") || word.endsWith("ches") 
					|| word.endsWith("shes") || word.endsWith("ses") || word.endsWith("xes")) {
				modifiedWord = word.substring(0, word.length() - 2);
				if(containsWord(modifiedWord))
					return modifiedWord;
			} else if(word.endsWith("ies") || word.endsWith("ied") || word.endsWith("ily")) {
				modifiedWord = word.substring(0, word.length() - 3).concat("y");
				if(containsWord(modifiedWord))
					return modifiedWord;
			} else if(word.endsWith("ly")) {
				modifiedWord = word.substring(0, word.length() - 2);
				if(containsWord(modifiedWord))
					return modifiedWord;
			} else if(word.endsWith("s") || word.endsWith("y")) {
				modifiedWord = word.substring(0, word.length() - 1);
				if(containsWord(modifiedWord))
					return modifiedWord;
			} else if(word.endsWith("ing")) {
				modifiedWord = word.substring(0, word.length() - 3);
				if(containsWord(modifiedWord))
					return modifiedWord;
				modifiedWord = word.substring(0, word.length() - 3).concat("e");
				if(containsWord(modifiedWord))
					return modifiedWord;
				modifiedWord = word.substring(0, word.length() - 4);
				if(containsWord(modifiedWord))
					return modifiedWord;
			} else if(word.endsWith("ed")) {
				modifiedWord = word.substring(0, word.length() - 2);
				if(containsWord(modifiedWord))
					return modifiedWord;
				modifiedWord = word.substring(0, word.length() - 2).concat("e");
				if(containsWord(modifiedWord))
					return modifiedWord;
				modifiedWord = word.substring(0, word.length() - 3);
				if(containsWord(modifiedWord))
					return modifiedWord;
			} 
		}	
		modifiedWord = irregularVerbMap.get(word);
		if(modifiedWord == null)
			return word;
		else 
			return modifiedWord;
	}
}
