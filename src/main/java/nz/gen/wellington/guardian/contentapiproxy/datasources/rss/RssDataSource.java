package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SectionCleaner;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ShortUrlDAO;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ShortUrlDecorator;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;

import uk.co.eelpieconsulting.common.http.HttpFetchException;

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
	private DescriptionFilter descriptionFilter;
	private ArticleSectionSorter articleSectionSorter;
	private ShortUrlDecorator shortUrlDecorator;
	private ShortUrlDAO shortUrlDAO;
	
	@Inject
	public RssDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor, ContentApi contentApi, DescriptionFilter descriptionFilter, ArticleSectionSorter articleSectionSorter, SectionCleaner sectionCleaner, ShortUrlDecorator shortUrlDecorator, ShortUrlDAO shortUrlDAO) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
		this.contentApi = contentApi;
		this.descriptionFilter = descriptionFilter;
		this.articleSectionSorter = articleSectionSorter;
		this.sectionCleaner = sectionCleaner;
		this.shortUrlDecorator = shortUrlDecorator;
		this.shortUrlDAO = shortUrlDAO;
	}
	
	public boolean isSupported(SearchQuery query) {
		return !query.hasDateRefinement();
	}
	
	public ArticleBundle getArticles(SearchQuery query) {
		ArticleBundle rawArticleBundle = fetchArticlesForQuery(query);
		if (rawArticleBundle != null) {
			final List<Article> sortedAndTrimmedArticleList = sortAndTrimArticleList(query, rawArticleBundle.getArticles());
			
			shortUrlDecorator.decorateArticlesWithLocallyAvailableShortUrls(sortedAndTrimmedArticleList);			
			final int missingShortUrlsCount = countMissingShortUrls(sortedAndTrimmedArticleList);
			if (missingShortUrlsCount > 0) {
				log.info("After shorturl decoration " + missingShortUrlsCount + "/" + sortedAndTrimmedArticleList.size() + " still had no short url. Fetching articles set from content api and redecorating");
				for (Article article : sortedAndTrimmedArticleList) {
					if (article.getShortUrl() == null) {
						final String contentId = article.getId();
						final String shortUrlFromContentApi = contentApi.getShortUrlFor(contentId);
						if (shortUrlFromContentApi != null) {
							shortUrlDAO.storeShortUrl(contentId, shortUrlFromContentApi);
						}
					}
				}
				
				shortUrlDecorator.decorateArticlesWithLocallyAvailableShortUrls(sortedAndTrimmedArticleList);
				
				final int missingShortUrlsCountAfterContentApiFetch = countMissingShortUrls(sortedAndTrimmedArticleList);
				log.info("After content api fetch and redecoration " + missingShortUrlsCountAfterContentApiFetch + "/" + sortedAndTrimmedArticleList.size() + " still had no short url.");
			}
			
			return new ArticleBundle(sortedAndTrimmedArticleList, rawArticleBundle.getDescription());
		}
				
		return null;
	}

	private int countMissingShortUrls(List<Article> articles) {
		int missingShortUrlsCount = 0;
		for (Article article : articles) {
			if (article.getShortUrl() == null) {
				missingShortUrlsCount++;
			}
		}
		return missingShortUrlsCount;
	}
	
	
	private ArticleBundle fetchArticlesForQuery(SearchQuery query) {
		ArticleBundle articleBundle = null;
		if (query.isSingleTagOrSectionQuery() || query.isTopStoriesQuery() || query.isTagCombinerQuery()) {
			String callUrl = buildQueryUrl(query);
			log.info("Fetching articles from: " + callUrl);
			String content;
			try {
				content = httpFetcher.fetchContent(callUrl);
			} catch (HttpFetchException e) {
				log.warn("Failed to fetch url; returning null: " + callUrl);
				return null;
			}
			
			if (content != null) {
				articleBundle = extractArticlesFromRss(content);			
			} else {
				log.warn("Failed to fetch content from: " + callUrl);		
			}
			
		} else {
			articleBundle = populateFavouriteArticles(query.getTags(), query.getPageSize());
		}				
		return articleBundle;
	}
	
	
	private List<Article> sortAndTrimArticleList(SearchQuery query, List<Article> articles) {
		int pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
		return articleSectionSorter.sortAndTrim(articles, pageSize);		
	}
	
	
	private ArticleBundle extractArticlesFromRss(final String content) {
		try {
			final SyndFeedInput input = new SyndFeedInput();
			final SyndFeed feed = input.build(new StringReader(content));
			
			@SuppressWarnings("unchecked")
			List<SyndEntry> entries = feed.getEntries();
			log.debug("Found " + entries.size() + " content items");
			
			Map<String, Section> sections = getSections();
			if (sections == null) {
				log.warn("Sections are not available - articles cannot be processed");
				return null;
			}
			
			List<Article> articles = new ArrayList<Article>();
			for (int i = 0; i < entries.size(); i++) {
				SyndEntry item = entries.get(i);
				Article article = rssEntryConvertor.entryToArticle(item, sections);				
				if (article != null) {
					if (article.getId() != null) {
						articles.add(article);
					} else {
						log.warn("Ignoring feed item which gave null content id: " + item.getTitle());
					}
					
				} else {
					log.warn("Ignoring feed item which gave null article: " + item.getTitle());
				}
			}
			
			final String description = descriptionFilter.filterOutMeaninglessDescriptions(feed.getDescription());
			return new ArticleBundle(articles, description);
			
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

		if (queryUrl.toString().equals("http://www.guardian.co.uk")) {
			queryUrl.append("/theguardian");	
		}

                queryUrl.append("/rss");

		return queryUrl.toString();
	}
	
	
	private ArticleBundle populateFavouriteArticles(List<Tag> favouriteTags, int size) {
		log.info("Fetching favourites: " + favouriteTags);
		List<Article> combined = new ArrayList<Article>();
		
		int numberFromEachFavourite = 3;
		int numberOfFavourites = favouriteTags.size();
		if (!(numberOfFavourites > 0)) {
			return null;
		}
				
		numberFromEachFavourite = (size / numberOfFavourites) + 1;
		if (numberFromEachFavourite < 3) {
			numberFromEachFavourite=3;
		}
		
		for (Tag favouriteTag : favouriteTags) {
			SearchQuery query = new SearchQuery();
			query.setTags(Arrays.asList(favouriteTag));
			ArticleBundle favouriteTagsArticles = this.fetchArticlesForQuery(query);
			if (favouriteTagsArticles != null) {
				putLatestThreeStoriesOntoList(combined, favouriteTagsArticles.getArticles(), numberFromEachFavourite);
			} else {
				log.warn("Failed to fetch articles for tag: " + favouriteTag.getId());
			}
		}
		
		return new ArticleBundle(combined);
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
