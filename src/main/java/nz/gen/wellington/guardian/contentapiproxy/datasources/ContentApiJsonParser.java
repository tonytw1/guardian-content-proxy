package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	
	public List<Article> extractContentItems(JSONObject json) throws JSONException {
		JSONObject jsonResponse = json.getJSONObject("response");
		JSONArray results = jsonResponse.getJSONArray("results");
		
		List<Article> articles = new ArrayList<Article>();
		for (int i = 0; i < results.length(); i++) {
			JSONObject content = results.getJSONObject(i);
			articles.add(parseContentItem(content));
		}
		return articles;
	}
	
	public Article extractContentItem(JSONObject json) throws JSONException {
		JSONObject jsonResponse = json.getJSONObject("response");
		JSONObject content = jsonResponse.getJSONObject("content");		
		return parseContentItem(content);
	}

	private Article parseContentItem(JSONObject content) throws JSONException {
		Article article = new Article();
		article.setId(getJsonStringIfPresent(content, "id"));
		article.setPubDate(new DateTime(parseDate(getJsonStringIfPresent(content, "webPublicationDate"))));
		article.setWebUrl(getJsonStringIfPresent(content, "webUrl"));
		
		JSONObject fields = content.getJSONObject("fields");		
		article.setHeadline(getJsonStringIfPresent(fields, "headline"));
		article.setByline(getJsonStringIfPresent(fields, "byline"));		
		article.setStandfirst(getJsonStringIfPresent(fields, "standfirst"));
		article.setThumbnailUrl(getJsonStringIfPresent(fields, "thumbnail"));
		article.setDescription(getJsonStringIfPresent(fields, "body"));
		article.setShortUrl(getJsonStringIfPresent(fields, "shortUrl"));		
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
	
	private String getJsonStringIfPresent(JSONObject json, String field) throws JSONException {
		if (json.has(field)) {
			return (String) json.get(field);
		}
		return null;
	}
	
}
