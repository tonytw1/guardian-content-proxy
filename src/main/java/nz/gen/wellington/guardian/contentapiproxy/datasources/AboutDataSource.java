package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssEntryToArticleConvertor;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class AboutDataSource {
	
	private CachingHttpFetcher httpFetcher;
	private RssEntryToArticleConvertor rssEntryConvertor;
	private String description;
	
	Logger log = Logger.getLogger(AboutDataSource.class);
	
	@Inject
	public AboutDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
	}
	
	
	public List<Article> getArticles() {
		final String callUrl = "http://eelpieconsulting.co.uk/rss";
		
		log.info("Fetching articles from: " + callUrl);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		
		if (content != null) {		
			StringReader reader = new StringReader(content);
			try {
		
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(reader);
				
				description = feed.getDescription();
				
				List<Article> articles = new ArrayList<Article>();
				List entries = feed.getEntries();
				log.info("Found " + entries.size() + " content items");
				for (int i = 0; i < entries.size(); i++) {
					SyndEntry item = (SyndEntry) entries.get(i);
					Article article = entryToArticle(item);
					if (article != null) {
						articles.add(article);
					}
				}
				return articles;			
				
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage());
			} catch (FeedException e) {
				log.error(e.getMessage());
			}
		}
		return null;
	}
	
	private Article entryToArticle(SyndEntry item) {
		Article article = new Article();
		article.setTitle(item.getTitle());
		article.setPubDate(new DateTime(item.getPublishedDate()));
		article.setStandfirst(item.getDescription().getValue());
		article.setDescription(item.getDescription().getValue());
		return article;
	}


	public String getDescription() {
		return description;
	}
	
}
