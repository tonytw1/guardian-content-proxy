package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.HtmlCleaner;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ShortUrlDAO;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class RssDataSource implements GuardianDataSource {

	private static Logger log = Logger.getLogger(RssDataSource.class);
	
	private static final String API_HOST = "http://www.guardian.co.uk";
	private static final int DEFAULT_PAGE_SIZE = 10;
	
	// TODO push section filtering to it's own class
	private List<String> badSectionNames = Arrays.asList("community", "crosswords", "extra", "help", "info", "local", "theguardian", "theobserver", "news", "weather");
	
	private CachingHttpFetcher httpFetcher;
	private RssEntryToArticleConvertor rssEntryConvertor;
	private ContentApi contentApi;
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
	
	
	public List<Article> getArticles(SearchQuery query) {		
		boolean isSingleTagOrSectionQuery = isSingleTagOrSectionQuery(query);
		
		List<Article> articles = null;
		if (isSingleTagOrSectionQuery) {
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


	private boolean isSingleTagOrSectionQuery(SearchQuery query) {
		int count = 0;
		if (query.getSections() != null) {
			count = count + query.getSections().size();
		}
		if (query.getTags() != null) {
			count = count + query.getTags().size();
		}
		return count <= 1;
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
	
	
	public Map<String, Section> getSections() {		
		Map<String, Section> sections = contentApi.getSections();
		if (sections != null) {
			sections = stripHtmlFromSectionNames(sections);
			sections = removeBadSections(sections);			
		}
 		return sections;		
	}
		
	public Map<String, List<Refinement>> getSectionRefinements(String sectionId) {
		return contentApi.getSectionRefinements(sectionId);
	}
	
	public Map<String, List<Refinement>> getTagRefinements(String tagId) {
		return contentApi.getTagRefinements(tagId);
	}

	public String getDescription() {
		return descriptionFilter.filterOutMeaninglessDescriptions(description);
	}
	
	
	private Map<String, Section> stripHtmlFromSectionNames(Map<String, Section> sections) {
		Map<String, Section> cleanedSections = new TreeMap<String, Section>();						
		for (String sectionName : sections.keySet()) {
			Section section = sections.get(sectionName);
			section.setName(HtmlCleaner.stripHtml(section.getName()));
			cleanedSections.put(section.getId(), section);
		}
		return cleanedSections;
	}
	
	
	private Map<String, Section> removeBadSections(Map<String, Section> sections) {
		Map<String, Section> allowedSections = new TreeMap<String, Section>();						
		for (String sectionIds : sections.keySet()) {
			if (!badSectionNames.contains(sectionIds)) {
				Section section = sections.get(sectionIds);
				allowedSections.put(section.getId(), section);				
			}
		}
		return allowedSections;
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
