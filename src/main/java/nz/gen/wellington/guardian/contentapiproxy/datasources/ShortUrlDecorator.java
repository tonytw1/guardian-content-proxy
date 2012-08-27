package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;

import nz.gen.wellington.guardian.model.Article;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ShortUrlDecorator {
	
	private static Logger log = Logger.getLogger(ShortUrlDecorator.class);

	private ShortUrlDAO shortUrlDao;
	
	@Inject
	public ShortUrlDecorator(ShortUrlDAO shortUrlDao) {
		this.shortUrlDao = shortUrlDao;
	}

	public void decorateArticlesWithLocallyAvailableShortUrls(List<Article> articles) {
		log.debug("Decorating " + articles.size() + " articles with short urls");
		for (Article article : articles) {
			decorateArticleWithShortUrlIfAvailable(article);			
		}
	}
		
	private void decorateArticleWithShortUrlIfAvailable(Article article) {
		if (article.getId() != null) {
			String shortUrlFor = shortUrlDao.getShortUrlFor(article.getId());
			if (shortUrlFor != null) {
				article.setShortUrl(shortUrlFor);		
			}
		}
	}

}
