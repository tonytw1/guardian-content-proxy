package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class FreeTierContentApi {

	private static final String API_HOST = "http://content.guardianapis.com";

	Logger log = Logger.getLogger(FreeTierContentApi.class);

	CachingHttpFetcher httpFetcher;
	
	
	@Inject
	public FreeTierContentApi(CachingHttpFetcher httpFetcher) {
		this.httpFetcher = httpFetcher;
	}

	
	public Map<String, Section> getSections() {
		log.info("Fetching section list from free tier content api");
		try {
			final String callUrl = buildApiSectionsQueryUrl();
			final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
			if (content != null) {
				
				try {
					JSONObject json = new JSONObject(content);
					if (json != null && isResponseOk(json)) {						
						JSONObject jsonResponse = json.getJSONObject("response");
						JSONArray results = jsonResponse.getJSONArray("results");
						
						Map<String, Section> sections = new HashMap<String, Section>();
						for (int i = 0; i < results.length(); i++) {
							JSONObject section = results.getJSONObject(i);
							sections.put(section.getString("id"), new Section(
									section.getString("id"),
									section.getString("webTitle")));							
						}
						log.info("Found " + sections.size() + " sections");
						return sections;
						
					}
					
				} catch (JSONException e) {
					log.info("JSON error while processing call url: " + callUrl);		
					return null;
				}
				
			}
			
		} catch (UnsupportedEncodingException e) {			
		}
		return null;		
	}
	
	
	private String buildApiSectionsQueryUrl() throws UnsupportedEncodingException {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/sections");
		queryUrl.append("?format=json");
		return queryUrl.toString();
	}
	
	
	boolean isResponseOk(JSONObject json) {
		try {
			JSONObject response = json.getJSONObject("response");
			String status = response.getString("status");
			return status != null && status.equals("ok");
		} catch (JSONException e) {
			return false;
		}
	}
	
}
