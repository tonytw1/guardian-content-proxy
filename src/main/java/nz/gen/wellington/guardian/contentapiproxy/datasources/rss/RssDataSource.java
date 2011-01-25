package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ShortUrlDAO;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.SectionDateRefinement;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

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
	private ShortUrlDAO shortUrlDao;
	private ArticleSectionSorter articleSectionSorter;

		
	@Inject
	public RssDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor, ContentApi contentApi, DescriptionFilter descriptionFilter, ShortUrlDAO shortUrlDao, ArticleSectionSorter articleSectionSorter) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
		this.contentApi = contentApi;
		this.descriptionFilter = descriptionFilter;
		this.shortUrlDao = shortUrlDao;
		this.articleSectionSorter = articleSectionSorter;
	}
	
	
	public boolean isSupported(SearchQuery query) {
		return !query.hasDateRefinement();
	}


	public List<Article> getArticles(SearchQuery query) {
		List<Article> articles = null;
		if (query.isSingleTagOrSectionQuery()) {
			String callUrl = buildQueryUrl(query);
			log.info("Fetching articles from: " + callUrl);
			final String content = httpFetcher.fetchContent(callUrl, "UTF-8");		
			if (content != null) {
				articles = extractArticlesFromRss(content);			
			} else {
				log.warn("Failed to fetch content from: " + callUrl);		
			}
			
		} else {
			articles =  populateFavouriteArticles(query.getSections(), query.getTags(), query.getPageSize());
		}
		
		if (articles == null) {
			return null;
		}
				
		articles = sortAndTrimArticleList(query, articles);		
		decorateArticlesWithShortUrls(articles);
		return articles;
	}
	
	
	@Override
	public Map<String, List<Refinement>> getRefinements(SearchQuery query) {
		Map<String, List<Refinement>> refinements = super.getRefinements(query);
		if (refinements != null) {
			if (query.isSectionQuery()) {			
				String sectionId = query.getSections().get(0);
				refinements.put("date", generateDateRefinementsForSection(sectionId, query.getFromDate()));
			}
		}
		return refinements;
	}


	public String getDescription() {
		return descriptionFilter.filterOutMeaninglessDescriptions(description);
	}
	

	private void decorateArticlesWithShortUrls(List<Article> articles) {
		log.info("Decorating " + articles.size() + " articles with short urls");
		for (Article article : articles) {
			decorateArticleWithShortUrlIfAvailable(article);		
		}
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
				
			description = feed.getDescription();
			
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
			
						
			// TODO only add short urls after trimming to size.
			return articles;
			
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
		} catch (FeedException e) {
			log.error(e.getMessage());
		}
		return null;
	}


	private void decorateArticleWithShortUrlIfAvailable(Article article) {
		if (article.getId() != null) {
			String shortUrlFor = shortUrlDao.getShortUrlFor(article.getId());
			if (shortUrlFor != null) {
				article.setShortUrl(shortUrlFor);
			
			} else if (article.getWebUrl() != null) {
				article.setShortUrl(article.getWebUrl());
			}
		
		}
	}
	
	@Deprecated
	private String buildQueryUrl(SearchQuery query) {
		StringBuilder queryUrl = new StringBuilder(API_HOST);
		if (query.getSections() != null && query.getSections().size() == 1) {
			queryUrl.append("/" + query.getSections().get(0));
		}
		if (query.getTags() != null && query.getTags().size() == 1) {
			queryUrl.append("/" + query.getTags().get(0));
		}
		queryUrl.append("/rss");
		return queryUrl.toString();
	}
	
	
	// TODO reduce visibilty when favourites servlet is depreciated.
	public List<Article> populateFavouriteArticles(List<String> favouriteSections, List<String> favouriteTags, int size) {
		log.info("Fetching favourites: " + favouriteSections + ", " + favouriteTags);
		List<Article> combined = new ArrayList<Article>();
		
		int numberFromEachFavourite = 3;
		int numberOfFavourites = favouriteSections.size() + favouriteTags.size();
		if (!(numberOfFavourites > 0)) {
			return combined;
		}
				
		numberFromEachFavourite = (size / numberOfFavourites) + 1;
		if (numberFromEachFavourite < 3) {
			numberFromEachFavourite=3;
		}
		
		for (String favouriteSection : favouriteSections) {
			SearchQuery query = new SearchQuery();
			query.setSections(Arrays.asList(favouriteSection));
			List<Article> articles = this.getArticles(query);					
			putLatestThreeStoriesOntoList(combined, articles, numberFromEachFavourite);
		}
		
		for (String favouriteTag : favouriteTags) {
			SearchQuery query = new SearchQuery();
			query.setTags(Arrays.asList(favouriteTag));
			List<Article> articles = this.getArticles(query);					
			putLatestThreeStoriesOntoList(combined, articles, numberFromEachFavourite);
		}
		
		return combined;
	}
	
	
	private List<Refinement> generateDateRefinementsForSection(String sectionId, DateTime fromDateTime) {
		// TODO create date refinements here.
		DateTime refinementBaseDate = new DateTime();		
		List<Refinement> dateRefinements = new ArrayList<Refinement>();		
		for (int i = 0; i <= 7; i++) {
			DateTime refinementDate = refinementBaseDate.minusDays(i);
			if (refinementDate.isBeforeNow()) {
				dateRefinements.add(new SectionDateRefinement(sectionId, refinementDate.toString("d MMM yyyy"), refinementDate, refinementDate));			
			}
		}		
		return dateRefinements;
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
