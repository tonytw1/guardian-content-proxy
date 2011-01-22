package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;

public class ContentApiDataSource implements GuardianDataSource {

	private ContentApi contentApi;
	
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

	public Map<String, List<Refinement>> getSectionRefinements(String section) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Section> getSections() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, List<Refinement>> getTagRefinements(String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSupported(SearchQuery query) {
		return true;
	}
		
}
