package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;


public interface GuardianDataSource {

	public String getContent(SearchQuery query);
	public List<Article> getArticles(SearchQuery query);

}
