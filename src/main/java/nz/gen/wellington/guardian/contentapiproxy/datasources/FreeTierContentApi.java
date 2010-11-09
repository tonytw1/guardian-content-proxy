package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class FreeTierContentApi {
	
	private Logger log = Logger.getLogger(FreeTierContentApi.class);
	
	private List<String> badSections = Arrays.asList("Community", "Crosswords", "Extra", "Help", "Info", "Local", "From the Guardian", "From the Observer", "News", "Weather");	
	private static final String API_HOST = "http://content.guardianapis.com";
	private final String[] permittedRefinementTypes = {"keyword", "blog", "contributor"};
	
	private CachingHttpFetcher httpFetcher;
	
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
						
						Map<String, Section> sections = new TreeMap<String, Section>();
						for (int i = 0; i < results.length(); i++) {
							JSONObject section = results.getJSONObject(i);
														
							Section loadedSection = new Section(
									section.getString("id"),
									HtmlCleaner.stripHtml(section.getString("webTitle")));
							
							if (!badSections.contains(loadedSection.getName())) {
								sections.put(section.getString("id"), loadedSection);
							}							
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
	
	
	public Map<String, List<Tag>> getSectionRefinements(String sectionId) {		
		String callUrl = buildSectionRefinementQueryUrl(sectionId);
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}
	
	
	public Map<String, List<Tag>> getTagRefinements(String tagId) {		
		String callUrl = buildTagRefinementQueryUrl(tagId);
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}

	
	private Map<String, List<Tag>> processRefinements(String callUrl) {
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		if (content == null) {
			log.warn("Failed to fetch url: " + callUrl);
			return null;
		}
		
		JSONObject json;
		try {
			json = new JSONObject(content);
			JSONObject response = json.getJSONObject("response");
				
			if (json != null && isResponseOk(json)) {

				Map<String, List<Tag>> refinements = new HashMap<String, List<Tag>>();
				if (response.has("refinementGroups")) {
					JSONArray refinementGroups = response.getJSONArray("refinementGroups");
					for (int i = 0; i < refinementGroups.length(); i++) {
						JSONObject refinementGroup = refinementGroups.getJSONObject(i);
						String type = refinementGroup.getString("type");
						

						boolean isPermittedRefinementType = Arrays.asList(permittedRefinementTypes).contains(type);
						if (isPermittedRefinementType) {
							
							List<Tag> tags = refinements.get(type);
							if (tags == null) {
								tags = new ArrayList<Tag>();
								refinements.put(type, tags);
							}
							
							JSONArray refinementsJSON = refinementGroup.getJSONArray("refinements");
							for (int j = 0; j < refinementsJSON.length(); j++) {
								JSONObject refinement = refinementsJSON.getJSONObject(j);
								tags.add(
									new Tag(refinement.getString("displayName"), refinement.getString("id"), null, type)
									);									
							}
						}
						
					}
				}
				return refinements;
			}
		} catch (JSONException e) {
			log.error(e.getMessage());
		}
		
		return null;
	}

	
	private String buildApiSectionsQueryUrl() throws UnsupportedEncodingException {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/sections");
		queryUrl.append("?format=json");
		return queryUrl.toString();
	}
	
	
	private String buildSectionRefinementQueryUrl(String sectionId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		queryUrl.append("?tag=type%2Farticle");
		queryUrl.append("&section=" + sectionId);
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		queryUrl.append("&from-date=" + new DateTime().minusDays(7).toString("yyyy-MM-dd"));
		return queryUrl.toString();
	}
	
	
	private String buildTagRefinementQueryUrl(String tagId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		try {
			queryUrl.append("?tag=type%2Farticle," + URLEncoder.encode(tagId, "UTF8"));
		} catch (UnsupportedEncodingException e) {			
		}
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		return queryUrl.toString();
	}
	
	private boolean isResponseOk(JSONObject json) {
		try {
			JSONObject response = json.getJSONObject("response");
			String status = response.getString("status");
			return status != null && status.equals("ok");
		} catch (JSONException e) {
			return false;
		}
	}

}
