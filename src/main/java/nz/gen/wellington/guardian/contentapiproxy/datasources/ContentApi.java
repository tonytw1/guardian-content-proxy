package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapi.parsing.ContentApiStyleJSONParser;
import nz.gen.wellington.guardian.contentapi.urls.ContentApiStyleUrlBuilder;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.TagRefinement;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class ContentApi {
	
	public static final String API_HOST = "http://content.guardianapis.com";
	public static final String API_KEY = "";

	private static Logger log = Logger.getLogger(ContentApi.class);
	
	private final String[] permittedRefinementTypes = {"keyword", "blog", "contributor", "section", "type"};
	
	private CachingHttpFetcher httpFetcher;
	private ContentApiStyleJSONParser contentApiJsonParser;
	
	@Inject
	public ContentApi(CachingHttpFetcher httpFetcher, ContentApiStyleJSONParser contentApiJsonParser) {
		this.httpFetcher = httpFetcher;
		this.contentApiJsonParser = contentApiJsonParser;
	}
	
	
	public List<Article> getArticles(SearchQuery query) {
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		
		urlBuilder.setFormat("json");
		urlBuilder.setShowAll(query.isShowAllFields());
		
		if (query.getFromDate() != null) {
			urlBuilder.setFromDate(query.getFromDate().toString("yyyy-MM-dd"));
		}
		if (query.getToDate() != null) {
			urlBuilder.setToDate(query.getToDate().toString("yyyy-MM-dd"));
		}

		urlBuilder.setPageSize(query.getPageSize());
		
		// TODO do the clients really use section queries anymore?
		if (query.getSections() != null && !query.getSections().isEmpty()) {
			for (String sectionId : query.getSections()) {
				urlBuilder.addSection(new Section(sectionId, sectionId));	// TODO Hmmmm		
			}
		}
		
		if (query.getTags() != null && !query.getTags().isEmpty()) {
			for (String tagId : query.getTags()) {
				urlBuilder.addTag(new Tag(null, tagId, null, null));	// TODO Hmmmm
			}
		}
		
		final String callUrl = urlBuilder.toSearchQueryUrl();
		String content = getContentFromUrlSuppressingHttpExceptions(callUrl);
		
		if (content != null) {				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && contentApiJsonParser.isResponseOk(json)) {						
					return contentApiJsonParser.extractContentItems(json);
				}
					
			} catch (JSONException e) {
				log.info("JSON error while processing call url: " + callUrl);
				log.info(e);
				return null;
			}				
		}		
		return null;
	}
	
	
	public Map<String, Section> getSections() {
		log.info("Fetching section list from free tier content api");
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.setFormat("json");
		final String callUrl = urlBuilder.toSectionsQueryUrl();
		final String content = getContentFromUrlSuppressingHttpExceptions(callUrl);
		if (content != null) {
			
			List<Section> sections = contentApiJsonParser.parseSectionsRequestResponse(content);
			log.info("Found " + sections.size() + " good sections");
			
			Map<String, Section> sectionsMap = new HashMap<String, Section>();
			for (Section section : sections) {
				sectionsMap.put(section.getId(), section);
			}				
			return sectionsMap;
		}
		return null;
	}
	
	
	public Article getArticle(String contentId) throws HttpForbiddenException {
		log.info("Fetching content item: " + contentId);
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.setContentId(contentId);
		urlBuilder.setFormat("json");
		final String callUrl = urlBuilder.toContentItemUrl();
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
	
	@Deprecated // TODO query for whole tags rather than interating over single records.
	public String getShortUrlFor(String contentId) throws HttpForbiddenException {
		log.info("Fetching short url for: " + contentId);
		Article article = this.getArticle(contentId);	
		if (article != null) {
			return article.getShortUrl();
		}
		return null;
	}
	
	public Map<String, List<Refinement>> getTagRefinements(String tagId) {	// TODO pass in the tag
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.addTag(new Tag(null, tagId, null, null));
		urlBuilder.setShowAll(false);
		urlBuilder.setShowRefinements(true);
		urlBuilder.setFormat("json");
		final String callUrl = urlBuilder.toSearchQueryUrl();		
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}

	
	private Map<String, List<Refinement>> processRefinements(String callUrl) {
		final String content = getContentFromUrlSuppressingHttpExceptions(callUrl);
		if (content == null) {
			log.warn("Failed to fetch url: " + callUrl);
			return null;
		}
		
		JSONObject json;
		try {
			json = new JSONObject(content);
			JSONObject response = json.getJSONObject("response");
				
			if (json != null && contentApiJsonParser.isResponseOk(json)) {
				Map<String, List<Refinement>> refinements = new HashMap<String, List<Refinement>>();
				
				if (response.has("refinementGroups")) {
					JSONArray refinementGroups = response.getJSONArray("refinementGroups");
					for (int i = 0; i < refinementGroups.length(); i++) {
						JSONObject refinementGroup = refinementGroups.getJSONObject(i);
						String type = refinementGroup.getString("type");
						
						boolean isPermittedRefinementType = Arrays.asList(permittedRefinementTypes).contains(type);
						if (isPermittedRefinementType) {
							
							List<Refinement> tagRefinements = refinements.get(type);
							if (tagRefinements == null) {
								tagRefinements = new ArrayList<Refinement>();
								refinements.put(type, tagRefinements);
							}
							
							JSONArray refinementsJSON = refinementGroup.getJSONArray("refinements");
							for (int j = 0; j < refinementsJSON.length(); j++) {
								JSONObject refinement = refinementsJSON.getJSONObject(j);
								tagRefinements.add(
										new TagRefinement(
											new Tag(refinement.getString("displayName"), refinement.getString("id"), null, type)
										)
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
	
	private String getContentFromUrlSuppressingHttpExceptions(final String callUrl) {
		String content;
		try {
			content = httpFetcher.fetchContent(callUrl, "UTF-8");
		} catch (HttpForbiddenException e1) {
			return null;			
		}
		return content;
	}
	
}
