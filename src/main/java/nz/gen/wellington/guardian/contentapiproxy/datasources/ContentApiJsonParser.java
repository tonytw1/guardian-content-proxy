package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContentApiJsonParser {
	
	private static Logger log = Logger.getLogger(ContentApiJsonParser.class);
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
		
	public boolean isResponseOk(JSONObject json) {
		try {
			JSONObject response = json.getJSONObject("response");
			String status = response.getString("status");
			return status != null && status.equals("ok");
		} catch (JSONException e) {
			return false;
		}
	}

	public Map<String, Section> extractSections(JSONObject json) throws JSONException {
		JSONObject jsonResponse = json.getJSONObject("response");
		JSONArray results = jsonResponse.getJSONArray("results");		
		Map<String, Section> sections = new TreeMap<String, Section>();
		for (int i = 0; i < results.length(); i++) {
			JSONObject section = results.getJSONObject(i);												
			Section loadedSection = new Section(section.getString("id"), section.getString("webTitle"));
			sections.put(loadedSection.getId(), loadedSection);
		}
		return sections;
	}

	public Article extractContentItem(JSONObject json) throws JSONException {
		JSONObject jsonResponse = json.getJSONObject("response");
		JSONObject content = jsonResponse.getJSONObject("content");
		
		Article article = new Article();
		article.setId(content.getString("id"));
		article.setPubDate(new DateTime(parseDate(content.getString("webPublicationDate"))));
		article.setWebUrl(content.getString("webUrl"));

		JSONObject fields = content.getJSONObject("fields");
		article.setHeadline(fields.getString("headline"));
		if (fields.has("byline")) {
			article.setByline(fields.getString("byline"));
		}
		article.setStandfirst(fields.getString("standfirst"));
		if (fields.has("thumbnail")) {
			article.setThumbnailUrl(fields.getString("thumbnail"));
		}
		article.setDescription(fields.getString("body"));
		if (fields.has("shortUrl")) {
			article.setShortUrl(fields.getString("shortUrl"));
		}
		return article;
	}
	
	
	
	public static Date parseDate(String dateString) {
		 SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
			 if (dateString.endsWith("Z")) {
				 dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				 dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
			 }		 
			 try {
				return dateFormat.parse(dateString);
			} catch (ParseException e) {
				 log.error("Failed to parse date '" + dateString +  "': " + e.getMessage());
			}
		
		return null;
	}

}
