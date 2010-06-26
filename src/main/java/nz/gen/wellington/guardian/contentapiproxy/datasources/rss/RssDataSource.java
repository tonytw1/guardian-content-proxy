package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.datasources.FreeTierContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;
import nz.gen.wellington.guardian.contentapiproxy.servlets.ArticleToXmlRenderer;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class RssDataSource implements GuardianDataSource {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private static final String API_HOST = "http://www.guardian.co.uk";
	
	private CachingHttpFetcher httpFetcher;
	private RssEntryToArticleConvertor rssEntryConvertor;
	private ArticleSectionSorter articleSectionSorter;
	private FreeTierContentApi freeTierContentApi;
	private ArticleToXmlRenderer articleToXmlRenderer;
	
	Logger log = Logger.getLogger(RssDataSource.class);

	
	@Inject
	public RssDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor, ArticleSectionSorter articleSectionSorter, FreeTierContentApi freeTierContentApi, ArticleToXmlRenderer articleToXmlRenderer) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
		this.articleSectionSorter = articleSectionSorter;
		this.freeTierContentApi = freeTierContentApi;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	
	
	public String getContent(SearchQuery query) {
		List<Article> articles = getArticles(query);				
		articles = articleSectionSorter.sort(articles);
		
		int pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
		if (pageSize < articles.size()) {
			log.info("Limiting articles to: " + pageSize);
			articles = articles.subList(0, pageSize);
		}
				
		List<Tag> refinements = null;
		if (query.getSection() != null) {
			refinements = freeTierContentApi.getSectionRefinements(query.getSection());
		}
		
		return articleToXmlRenderer.outputXml(articles, refinements);		
	}


	public List<Article> getArticles(SearchQuery query) {
		String callUrl = buildApiSearchQueryUrl(query);
		log.info("Fetching articles from: " + callUrl);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		
		if (content != null) {		
			StringReader reader = new StringReader(content);
			try {
		
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(reader);

				Map<String, Section> sections = freeTierContentApi.getSections();
				
				List<Article> articles = new ArrayList<Article>();
				List entries = feed.getEntries();
				log.info("Found " + entries.size() + " content items");
				for (int i = 0; i < entries.size(); i++) {
					SyndEntry item = (SyndEntry) entries.get(i);
					Article article = rssEntryConvertor.entryToArticle(item, sections);
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

	
	private String buildApiSearchQueryUrl(SearchQuery query) {
		StringBuilder queryUrl = new StringBuilder(API_HOST);
		if (query.getSection() != null) {
			queryUrl.append("/" + query.getSection());
		}
		if (query.getTag() != null) {
			queryUrl.append("/" + query.getTag());
		}
		queryUrl.append("/rss");
		return queryUrl.toString();
	}
	
}
