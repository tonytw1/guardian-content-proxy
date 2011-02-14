package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.model.Article;

public class ShortUrlDecorator {
	
	private static Logger log = Logger.getLogger(RssDataSource.class);

	private ShortUrlDAO shortUrlDao;
	
	@Inject
	public ShortUrlDecorator(ShortUrlDAO shortUrlDao) {
		this.shortUrlDao = shortUrlDao;
	}

	public void decorateArticlesWithShortUrls(List<Article> articles) {
		log.info("Decorating " + articles.size() + " articles with short urls");
		for (Article article : articles) {
			try {
				decorateArticleWithShortUrlIfAvailable(article);
			} catch (HttpForbiddenException e) {
				log.warn("Aborting short url decoration as we received a forbidden error from the api");
			}
		}
	}
		
	private void decorateArticleWithShortUrlIfAvailable(Article article) throws HttpForbiddenException {
		if (article.getId() != null) {
			String shortUrlFor = shortUrlDao.getShortUrlFor(article.getId());
			if (shortUrlFor != null) {
				article.setShortUrl(shortUrlFor);		
			}
		}
	}

}
