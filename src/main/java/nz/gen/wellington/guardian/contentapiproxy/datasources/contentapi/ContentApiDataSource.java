package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SectionCleaner;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;

import com.google.inject.Inject;

public class ContentApiDataSource extends AbstractGuardianDataSource {
	
	@Inject
	public ContentApiDataSource(ContentApi contentApi, SectionCleaner sectionCleaner) {
		this.contentApi = contentApi;
		this.sectionCleaner = sectionCleaner;
	}

	public ArticleBundle getArticles(SearchQuery query) {
		return new ArticleBundle(contentApi.getArticles(query));
	}
	
	public boolean isSupported(SearchQuery query) {
		return true;
	}
	
}
