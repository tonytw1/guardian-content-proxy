package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Refinement;
import nz.gen.wellington.guardian.model.Section;

public interface GuardianDataSource {
	
	public Map<String, Section> getSections();
	public boolean isSupported(SearchQuery query);
	public ArticleBundle getArticles(SearchQuery query);	
	public Map<String, List<Refinement>> getRefinements(SearchQuery query);

}
