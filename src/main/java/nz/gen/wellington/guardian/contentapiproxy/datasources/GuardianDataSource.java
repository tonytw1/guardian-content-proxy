package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;


public interface GuardianDataSource {

	public List<Article> getArticles(SearchQuery query);
	public List<Tag> getSectionRefinements(String section);

}
