package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;

import com.google.inject.Inject;

public class ContentApiDataSource extends AbstractGuardianDataSource {
	
	@Inject
	public ContentApiDataSource(ContentApi contentApi) {
		this.contentApi = contentApi;
	}

	public List<Article> getArticles(SearchQuery query) {
		return contentApi.getArticles(query);
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, List<Refinement>> getSectionRefinements(String sectionId) {
		return contentApi.getSectionRefinements(sectionId);
	}
	
	public Map<String, List<Refinement>> getTagRefinements(String tagId) {
		return contentApi.getTagRefinements(tagId);
	}

	public boolean isSupported(SearchQuery query) {
		return true;
	}
		
}
