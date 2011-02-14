package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SectionCleaner;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ShortUrlDecorator;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class RssDataSource extends AbstractGuardianDataSource {

	private static Logger log = Logger.getLogger(RssDataSource.class);
	
	private static final String API_HOST = "http://www.guardian.co.uk";
	private static final int DEFAULT_PAGE_SIZE = 10;
	
	private CachingHttpFetcher httpFetcher;
	private RssEntryToArticleConvertor rssEntryConvertor;
	private String description;
	private DescriptionFilter descriptionFilter;
	private ArticleSectionSorter articleSectionSorter;
	private ShortUrlDecorator shortUrlDecorator;	
	
	@Inject
	public RssDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor, ContentApi contentApi, DescriptionFilter descriptionFilter, ArticleSectionSorter articleSectionSorter, SectionCleaner sectionCleaner, ShortUrlDecorator shortUrlDecorator) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
		this.contentApi = contentApi;
		this.descriptionFilter = descriptionFilter;
		this.articleSectionSorter = articleSectionSorter;
		this.sectionCleaner = sectionCleaner;
		this.shortUrlDecorator = shortUrlDecorator;
	}
	
	public boolean isSupported(SearchQuery query) {
		return !query.hasDateRefinement();
	}


	public ArticleBundle getArticles(SearchQuery query) {
		List<Article> articles = fetchArticlesForQuery(query);			
		articles = sortAndTrimArticleList(query, articles);
		//shortUrlDecorator.decorateArticlesWithShortUrls(articles);
		return new ArticleBundle(articles,  descriptionFilter.filterOutMeaninglessDescriptions(description));
	}
	
	
	private List<Article> fetchArticlesForQuery(SearchQuery query) {
		List<Article> articles = new ArrayList<Article>();
		if (query.isSingleTagOrSectionQuery() || query.isTopStoriesQuery() || query.isTagCombinerQuery()) {
			String callUrl = buildQueryUrl(query);
			log.info("Fetching articles from: " + callUrl);
			String content;
			try {
				content = httpFetcher.fetchContent(callUrl, "UTF-8");
			} catch (HttpForbiddenException e) {
				return null;
			}
			
			if (content != null) {
				articles = extractArticlesFromRss(content);			
			} else {
				log.warn("Failed to fetch content from: " + callUrl);		
			}
			
		} else {
			articles = populateFavouriteArticles(query.getTags(), query.getPageSize());
		}		
		if (articles == null) {
			return null;
		}
		
		
		return articles;
	}
	
	
	private List<Article> sortAndTrimArticleList(SearchQuery query, List<Article> articles) {
		articles = articleSectionSorter.sort(articles);		
		int pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
		if (pageSize < articles.size()) {
			log.info("Limiting articles to: " + pageSize);
			articles = articles.subList(0, pageSize);
		}
		return articles;
	}
	
	
	private List<Article> extractArticlesFromRss(final String content) {
		SyndFeedInput input = new SyndFeedInput();		
		try {
			SyndFeed feed = input.build(new StringReader(content));
				
			description = feed.getDescription();	// TODO not thread safe
			
			List<Article> articles = new ArrayList<Article>();
			
			@SuppressWarnings("unchecked")
			List<SyndEntry> entries = feed.getEntries();
			log.info("Found " + entries.size() + " content items");
			
			Map<String, Section> sections = getSections();
			if (sections == null) {
				log.warn("Sections are not available - articles cannot be processed");
				return null;
			}
			
			for (int i = 0; i < entries.size(); i++) {
				SyndEntry item = entries.get(i);
				Article article = rssEntryConvertor.entryToArticle(item, sections);
				
				if (article != null && article.getSection() != null) {
					articles.add(article);
				}
			}			
			return articles;
			
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
		} catch (FeedException e) {
			log.error(e.getMessage());
		}
		return null;
	}
	
	
	private String buildQueryUrl(SearchQuery query) {
		StringBuilder queryUrl = new StringBuilder(API_HOST);
		if (query.isSingleTagQuery()) {

			if (query.isTagCombinerQuery()) {
				queryUrl.append("/" + query.getTags().get(0).getId() + "+content/gallery");
			} else {

				Tag tag = query.getTags().get(0);
				
				String[] splits = tag.getId().split("/");
				log.info(splits[0]);
				log.info(splits[1]);
				
				boolean tagisSectionKeyword = splits[0].equals(splits[1]);
				if (tagisSectionKeyword) {
					queryUrl.append("/" + tag.getId().split("/")[0]);
				} else {				
					queryUrl.append("/" + tag.getId());
				}		
			}
			queryUrl.append("/rss");
			return queryUrl.toString();
		}
		
		if (query.getTags() != null && query.getTags().size() == 1) {
			Tag tag = query.getTags().get(0);
			if (tag.isSectionKeyword()) {
				queryUrl.append("/" + tag.getId().split("/")[0]);
			} else {
				queryUrl.append("/" + tag.getId().split("/")[0]);
			}
		}
		queryUrl.append("/rss");
		return queryUrl.toString();
	}
	
	
	private List<Article> populateFavouriteArticles(List<Tag> favouriteTags, int size) {
		log.info("Fetching favourites: " + favouriteTags);
		List<Article> combined = new ArrayList<Article>();
		
		int numberFromEachFavourite = 3;
		int numberOfFavourites = favouriteTags.size();
		if (!(numberOfFavourites > 0)) {
			return combined;
		}
				
		numberFromEachFavourite = (size / numberOfFavourites) + 1;
		if (numberFromEachFavourite < 3) {
			numberFromEachFavourite=3;
		}
		
		for (Tag favouriteTag : favouriteTags) {
			SearchQuery query = new SearchQuery();
			query.setTags(Arrays.asList(favouriteTag));
			List<Article> articles = this.fetchArticlesForQuery(query);					
			putLatestThreeStoriesOntoList(combined, articles, numberFromEachFavourite);
		}		
		return combined;
	}
	
	
	private void putLatestThreeStoriesOntoList(List<Article> combined, List<Article> articles, int number) {
		if (articles == null) {
			return;
		}
		final int numberToAdd = (articles.size() < number) ? articles.size() : number;
		for (int i = 0; i < numberToAdd; i++) {
			Article article = articles.get(i);
			if (article.getSection() != null) {
				combined.add(article);
			}
		}
	}
	
}
