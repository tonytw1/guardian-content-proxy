package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class AboutDataSource {
	
	private static final String ABOUT_RSS_FEED = "http://eelpieconsulting.co.uk/category/development/guardian-lite/feed/atom/";
	
	private static Logger log = Logger.getLogger(AboutDataSource.class);
	
	private CachingHttpFetcher httpFetcher;
	private String description;
	
	
	@Inject
	public AboutDataSource(CachingHttpFetcher httpFetcher) {
		this.httpFetcher = httpFetcher;
	}
		
	public List<Article> getArticles() {
		final String callUrl = ABOUT_RSS_FEED;
		
		log.info("Fetching articles from: " + callUrl);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		
		if (content != null) {		
			StringReader reader = new StringReader(content);
			try {
		
				SyndFeedInput input = new SyndFeedInput();
				input.setPreserveWireFeed(true);				
				SyndFeed feed = input.build(reader);
				
				description = feed.getDescription();
				
				List<Article> articles = new ArrayList<Article>();
				
				@SuppressWarnings("unchecked")
				List<SyndEntry> entries = feed.getEntries();				
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
		article.setHeadline(HtmlCleaner.stripHtml(item.getTitle()));
		article.setPubDate(new DateTime(item.getPublishedDate()));
		
		Entry atomEntry = (Entry) item.getWireEntry();
		article.setStandfirst(HtmlCleaner.stripHtml(atomEntry.getSummary().getValue()));
	
		Content body = (Content) atomEntry.getContents().get(0);
		article.setDescription(HtmlCleaner.stripHtml(body.getValue()));
		return article;
	}


	public String getDescription() {
		return description;
	}
	
}
