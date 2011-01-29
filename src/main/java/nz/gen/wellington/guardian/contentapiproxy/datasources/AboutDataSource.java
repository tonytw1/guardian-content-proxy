package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.MediaElement;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;

import com.sun.syndication.feed.module.mediarss.MediaEntryModuleImpl;
import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class AboutDataSource {
	
	private static final String ABOUT_RSS_FEED = "http://eelpieconsulting.co.uk/category/development/guardian-lite/feed";
	
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
		String content;
		try {
			content = httpFetcher.fetchContent(callUrl, "UTF-8");
		} catch (HttpForbiddenException e1) {
			return null;
		}
		
		if (content != null) {		
			StringReader reader = new StringReader(content);
			try {
		
				SyndFeedInput input = new SyndFeedInput();
				input.setPreserveWireFeed(true);				
				SyndFeed feed = input.build(reader);
								
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
		
		article.setStandfirst(HtmlCleaner.stripHtml(item.getDescription().getValue()));
	
		if (item.getContents().size() > 0) {
	           SyndContent content = (SyndContent) item.getContents().get(0);
	           article.setDescription(HtmlCleaner.stripHtml(content.getValue()));
		}
		
		processMediaElements(item, article);
		return article;
	}
	
		
	private void processMediaElements(SyndEntry item, Article article) {

		MediaEntryModuleImpl mediaModule = (MediaEntryModuleImpl) item.getModule(MediaModule.URI);
		if (mediaModule != null) {

			log.info("Found media module");
			MediaContent[] mediaContents = mediaModule.getMediaContents();
			if (mediaContents.length > 0) {
				MediaContent mediaContent = mediaContents[0];

				log.info(mediaContent);
				UrlReference reference = (UrlReference) mediaContent.getReference();
				final String trailImageUrl = reference.getUrl().toExternalForm();
				log.info("Found trail image: " + trailImageUrl);
				article.setThumbnailUrl(trailImageUrl);

				for (int i = 0; i < mediaContents.length; i++) {
					mediaContent = mediaContents[i];
					reference = (UrlReference) mediaContent.getReference();
					Metadata metadata = mediaContent.getMetadata();
					MediaElement picture = new MediaElement("picture",reference.getUrl().toExternalForm(), metadata.getDescription());
					article.addMediaElement(picture);
				}
			}
		}

	}

	public String getDescription() {
		return description;
	}
	
}
