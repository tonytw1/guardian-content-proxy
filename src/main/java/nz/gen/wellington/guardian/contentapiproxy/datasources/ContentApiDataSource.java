package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import nz.gen.wellington.guardian.contentapiproxy.servlets.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class ContentApiDataSource implements GuardianDataSource {
	
	protected static final String API_HOST = "http://content.guardianapis.com";
	protected static final String API_KEY = "";
		
	Logger log = Logger.getLogger(ContentApiDataSource.class);

	CachingHttpFetcher httpFetcher;

	@Inject
	public ContentApiDataSource(CachingHttpFetcher httpFetcher) {
		this.httpFetcher = httpFetcher;
	}

	
	public String getContent(SearchQuery query) {	
		String callUrl = buildApiSearchQueryUrl(query);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");		
		if (content != null) {		
				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && isResponseOk(json)) {						
					JSONObject jsonResponse = json.getJSONObject("response");
					JSONArray results = jsonResponse.getJSONArray("results");
					populateMediaElements(results);							
					return new JsonToXmlTranscoder().jsonToXml(json);					
				}
				
			} catch (JSONException e) {
				log.info("JSON error while processing call url: " + callUrl);		
				return null;
			}
		}
		return null;
	}
	
	
	@Override
	public String getQueryCacheKey(SearchQuery query) {
		return buildApiSearchQueryUrl(query);
	}


	private String buildApiSearchQueryUrl(SearchQuery query) {
		StringBuilder queryUrl;
		try {
			queryUrl = new StringBuilder(API_HOST + "/search?api-key=" + URLEncoder.encode(API_KEY, "UTF-8"));
			queryUrl.append("&format=json");
			
			if (query.getTag() != null) {
				queryUrl.append("&tag=" + URLEncoder.encode(query.getTag(), "UTF-8"));
			}
			
			if (query.getSection() != null) {
				queryUrl.append("&section=" + URLEncoder.encode(query.getSection(), "UTF-8"));
			}
			
			if (query.isShowAllFields()) {
				queryUrl.append("&show-fields=all");
			}
			
			if (query.isShowAllTags()) {
				queryUrl.append("&show-tags=all");
			}
			
			if (query.getPageSize() != null) {
				queryUrl.append("&page-size=" + query.getPageSize());			
			}
			
			return queryUrl.toString();
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	

		
	void populateMediaElements(JSONArray results) throws JSONException {
		for (int i = 0; i < results.length(); i++) {
			JSONObject result = results.getJSONObject(i);
			final String id = result.getString("id");
			final String apiUrl = buildApiContentQueryUrl(id, API_KEY);

			final String content = httpFetcher.fetchContent(apiUrl, "UTF-8");
			if (content != null) {
				JSONObject json = new JSONObject(content);
				if (json != null && isResponseOk(json)) {

					JSONObject response = json.getJSONObject("response");
					JSONObject jsonContent = response.getJSONObject("content");

					if (jsonContent.has("mediaAssets")) {
						JSONArray mediaAssets = jsonContent.getJSONArray("mediaAssets");
						result.put("mediaAssets", mediaAssets);
					}
				}
			}
		}
	}
	
	
	private String buildApiContentQueryUrl(String id, String apikey) {
		try {
			StringBuilder queryUrl = new StringBuilder(API_HOST + "/" + id);
			queryUrl.append("?api-key=" + URLEncoder.encode(apikey, "UTF-8"));
			queryUrl.append("&format=json");
			queryUrl.append("&show-media=all");		
			return queryUrl.toString();
		} catch (UnsupportedEncodingException e) {
			return null;
		}
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
