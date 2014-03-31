package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import java.util.List;

import org.joda.time.DateTime;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SectionCleaner;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Article;

import com.google.inject.Inject;
import com.google.inject.internal.Lists;

public class ContentApiDataSource extends AbstractGuardianDataSource {
	
	@Inject
	public ContentApiDataSource(ContentApi contentApi, SectionCleaner sectionCleaner) {
		this.contentApi = contentApi;
		this.sectionCleaner = sectionCleaner;
	}

	public ArticleBundle getArticles(SearchQuery query) {
		final List<Article> articles = Lists.newArrayList(decommissioningNotice());
		articles.addAll(contentApi.getArticles(query));	
		return new ArticleBundle(articles);
	}

	private Article decommissioningNotice() {
		Article decommisioningNotice = new Article();
		decommisioningNotice.setHeadline("This application will be withdrawn from service on the 5th of May");
		decommisioningNotice.setPubDate(new DateTime().toDate());
		decommisioningNotice.setDescription("<p>The Guardian has recently made changes to the source data which this application consumes.</p>" +
				"<p>Not all Guardian content is available for redistribution by 3rd party application such as this one. Users requiring a complete set of content should move to one of the official Guardian applications.</p>" +
				"<p>When this application was developed the Guardian did not provide an official Android app. Now that they do the developers of this application would like to devote their efforts to other projects. As such, this application will no longer be available after the 5th of May 2014.</p>");
		return decommisioningNotice;
	}
	
	public boolean isSupported(SearchQuery query) {
		return true;
	}
	
}
