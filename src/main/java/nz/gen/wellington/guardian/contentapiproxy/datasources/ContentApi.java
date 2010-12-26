package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class ContentApi {
	
	private static Logger log = Logger.getLogger(ContentApi.class);
	
	private final String[] permittedRefinementTypes = {"keyword", "blog", "contributor", "section"};
	
	protected ContentApiUrlBuilder contentApiUrlBuilder;
	private CachingHttpFetcher httpFetcher;
	private ContentApiJsonParser contentApiJsonParser;
	
	@Inject
	public ContentApi(CachingHttpFetcher httpFetcher, ContentApiJsonParser contentApiJsonParser) {
		this.httpFetcher = httpFetcher;
		this.contentApiJsonParser = contentApiJsonParser;
		this.contentApiUrlBuilder = new ContentApiUrlBuilder("");
	}
	
	public Map<String, Section> getSections() {
		log.info("Fetching section list from free tier content api");
		try {
			final String callUrl = contentApiUrlBuilder.buildApiSectionsQueryUrl();
			final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
			if (content != null) {
				
				try {
					JSONObject json = new JSONObject(content);
					if (json != null && contentApiJsonParser.isResponseOk(json)) {						
						Map<String, Section> sections = contentApiJsonParser.extractSections(json);					
						log.info("Found " + sections.size() + " good sections");
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
	
	
	public Article getArticle(String contentId) {
		log.info("Fetching content item: " + contentId);
		final String callUrl = contentApiUrlBuilder.buildApiContentItemUrl(contentId);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		if (content != null) {				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && contentApiJsonParser.isResponseOk(json)) {						
					return contentApiJsonParser.extractContentItem(json);
				}
					
			} catch (JSONException e) {
				log.info("JSON error while processing call url: " + callUrl);
				log.info(e);
				return null;
			}				
		}		
		return null;		
	}
	
	
	public String getShortUrlFor(String contentId) {
		log.info("Fetching short url for: " + contentId);
		Article article = this.getArticle(contentId);	
		if (article != null) {
			return article.getShortUrl();
		}
		return null;
	}
	
	
	public Map<String, List<Tag>> getSectionRefinements(String sectionId) {		
		String callUrl = contentApiUrlBuilder.buildSectionRefinementQueryUrl(sectionId);
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}
	
	
	public Map<String, List<Tag>> getTagRefinements(String tagId) {		
		String callUrl = contentApiUrlBuilder.buildTagRefinementQueryUrl(tagId);
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
				
			if (json != null && contentApiJsonParser.isResponseOk(json)) {
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
	
}
