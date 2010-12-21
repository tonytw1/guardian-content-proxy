package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class SavedContentDataSource {
		
	private static Logger log = Logger.getLogger(SavedContentDataSource.class);
	
	private FreeTierContentApi contentApi;
	
	@Inject
	public SavedContentDataSource(FreeTierContentApi contentApi) {
		this.contentApi = contentApi;
	}
		
	public List<Article> getArticles(List<String> articleIds) {		
		List<Article> savedArticles = new ArrayList<Article>();		
		for (String articleId : articleIds) {
			savedArticles.add(fetchArticle(articleId));
		}
		return savedArticles;
	}

	private Article fetchArticle(String contentId) {
		log.info("Fetching content item: " + contentId);
		return contentApi.getArticle(contentId);
	}
	
	public String getDescription() {
		return null;
	}
	
}
