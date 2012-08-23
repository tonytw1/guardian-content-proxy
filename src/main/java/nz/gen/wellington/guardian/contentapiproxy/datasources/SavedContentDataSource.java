package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.model.Article;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;

public class SavedContentDataSource {
		
	private static Logger log = Logger.getLogger(SavedContentDataSource.class);
	
	private ContentApi contentApi;
	private ShortUrlDAO shortUrlDao;
	
	@Inject
	public SavedContentDataSource(ContentApi contentApi, ShortUrlDAO shortUrlDao) {
		this.contentApi = contentApi;
		this.shortUrlDao = shortUrlDao;
	}
		
	public List<Article> getArticles(List<String> articleIds) {		
		List<Article> savedArticles = new ArrayList<Article>();		
		for (String articleId : articleIds) {
			Article article = fetchArticle(articleId);
			if (article != null) {
				savedArticles.add(article);
			} else {
				savedArticles.add(createMissingArticlePlaceholderFor(articleId));
			}
		}
		return savedArticles;
	}

	private Article createMissingArticlePlaceholderFor(String articleId) {
		Article missingArticle = new Article();
		missingArticle.setId(articleId);
		missingArticle.setHeadline("Missing article");
		missingArticle.setPubDate(new DateTime().toDate());
		missingArticle.setStandfirst("This article could not be retrieved. It may no longer be available. (" + articleId + ")");
		return missingArticle;
	}

	private Article fetchArticle(String contentId) {
		log.info("Fetching content item: " + contentId);
		Article article = contentApi.getArticle(contentId, false);	
		
		if (article != null && article.getShortUrl() != null) {
			shortUrlDao.storeShortUrl(article.getId(), article.getShortUrl());
		}
		return article;
	}
	
	public String getDescription() {
		return null;
	}
	
}
