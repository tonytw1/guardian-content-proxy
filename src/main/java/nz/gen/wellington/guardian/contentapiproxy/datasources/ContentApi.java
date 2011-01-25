package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;
import nz.gen.wellington.guardian.contentapiproxy.model.TagRefinement;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class ContentApi {
	
	private static final String API_KEY = "";

	private static Logger log = Logger.getLogger(ContentApi.class);
	
	private final String[] permittedRefinementTypes = {"keyword", "blog", "contributor", "section"};
	
	protected ContentApiUrlBuilder contentApiUrlBuilder;
	private CachingHttpFetcher httpFetcher;
	private ContentApiJsonParser contentApiJsonParser;
	
	@Inject
	public ContentApi(CachingHttpFetcher httpFetcher, ContentApiJsonParser contentApiJsonParser) {
		this.httpFetcher = httpFetcher;
		this.contentApiJsonParser = contentApiJsonParser;
		this.contentApiUrlBuilder = new ContentApiUrlBuilder(API_KEY);
	}
	
	
	public List<Article> getArticles(SearchQuery query) {
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(ContentApiUrlBuilder.API_HOST, API_KEY);
		
		urlBuilder.setFormat("json");
		urlBuilder.setShowAll(query.isShowAllFields());
		
		if (query.getFromDate() != null) {
			urlBuilder.setFromDate(query.getFromDate().toString("yyyy-MM-dd"));
		}
		if (query.getToDate() != null) {
			urlBuilder.setToDate(query.getToDate().toString("yyyy-MM-dd"));
		}

		urlBuilder.setPageSize(query.getPageSize());
		
		if (query.getSections() != null && !query.getSections().isEmpty()) {
			for (String sectionId : query.getSections()) {
				urlBuilder.addSection(sectionId);			
			}
		}
		
		if (query.getTags() != null && !query.getTags().isEmpty()) {
			for (String tagId : query.getTags()) {
				urlBuilder.addTag(tagId);	
			}
		}
		
		final String callUrl = urlBuilder.toSearchQueryUrl();
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
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
	
	
	public Map<String, List<Refinement>> getSectionRefinements(String sectionId) {		
		String callUrl = contentApiUrlBuilder.buildSectionRefinementQueryUrl(sectionId);
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}
	
	
	public Map<String, List<Refinement>> getTagRefinements(String tagId) {		
		String callUrl = contentApiUrlBuilder.buildTagRefinementQueryUrl(tagId);
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}

	
	private Map<String, List<Refinement>> processRefinements(String callUrl) {
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
	
}