package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;

public interface GuardianDataSource {
	
	public Map<String, Section> getSections();
	public List<Article> getArticles(SearchQuery query);
	public Map<String, List<Refinement>> getSectionRefinements(String section);
	public Map<String, List<Refinement>> getTagRefinements(String tag);
	public String getDescription();

}
