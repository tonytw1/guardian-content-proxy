package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.ArticleSectionSorter;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@SuppressWarnings("serial")
@Singleton
public class FavouritesServlet extends HttpServlet {

	private static final int OUTGOING_TTL = 300;

	private static final int MAX_FAVOURITES_SIZE = 20;

	Logger log = Logger.getLogger(FavouritesServlet.class);

	private GuardianDataSource datasource;
	private MemcacheService cache;
	private ArticleSectionSorter articleSectionSorter;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public FavouritesServlet(RssDataSource datasource, ArticleSectionSorter articleSectionSorter, ArticleToXmlRenderer articleToXmlRenderer) {
		this.datasource = datasource;
		this.cache = MemcacheServiceFactory.getMemcacheService();
		this.articleSectionSorter = articleSectionSorter;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/favourites")) {
			
            final String queryCacheKey = getQueryCacheKey(request);
            String output = (String) cache.get(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            }
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);
				List<String> favouriteSections = parseSectionsFromRequest(request);				
				List<String> favouriteTags = parseTagsFromRequest(request);
				
				List<Article> combined = populateFavouriteArticles(favouriteSections, favouriteTags);
											
				combined = articleSectionSorter.sort(combined);
				if (MAX_FAVOURITES_SIZE < combined.size()) {
					log.info("Limiting articles to: " + MAX_FAVOURITES_SIZE);
					combined = combined.subList(0, MAX_FAVOURITES_SIZE);
				}
				
				output = articleToXmlRenderer.outputXml(combined, null);
				if (output != null) {
					log.info("Caching results for call: " + queryCacheKey);
					cache.put(queryCacheKey, output, Expiration.byDeltaSeconds(OUTGOING_TTL));
				}
			}
			
			if (output != null) {
				log.info("Outputing content: " + output.length() + " characters");
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
				PrintWriter writer = response.getWriter();
				writer.print(output);
				writer.flush();
				
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);				
			}
		}
		
		return;
	}


	private List<String> parseSectionsFromRequest(HttpServletRequest request) {
		return commaSeperatedToList(request.getParameter("sections"));
	}

	private List<String> parseTagsFromRequest(HttpServletRequest request) {
		return commaSeperatedToList(request.getParameter("tags"));
	}
	
	private List<String> commaSeperatedToList(final String commaSeperated) {
		List<String> list = new ArrayList<String>();
		if (commaSeperated != null) {
			for (String section : commaSeperated.split(",")) {
				list.add(section);
			}
		}
		return list;
	}
	

	private List<Article> populateFavouriteArticles(List<String> favouriteSections, List<String> favouriteTags) {
		List<Article> combined = new ArrayList<Article>();
		for (String favouriteSection : favouriteSections) {
			SearchQuery query = new SearchQuery();
			query.setTag(favouriteSection);
			List<Article> articles = datasource.getArticles(query);					
			putLatestThreeStoriesOntoList(combined, articles);
		}
		
		for (String favouriteTag : favouriteTags) {
			SearchQuery query = new SearchQuery();
			query.setTag(favouriteTag);
			List<Article> articles = datasource.getArticles(query);					
			putLatestThreeStoriesOntoList(combined, articles);
		}
		
		SearchQuery latestQuery = new SearchQuery();
		putLatestThreeStoriesOntoList(combined, datasource.getArticles(latestQuery));		
		return combined;
	}

		
	private void putLatestThreeStoriesOntoList(List<Article> combined, List<Article> articles) {
		if (articles == null) {
			return;
		}
		final int numberToAdd = (articles.size() < 3) ? articles.size() : 3;
		for (int i = 0; i < numberToAdd; i++) {
			Article article = articles.get(i);
			if (article.getSection() != null) {
				combined.add(article);
			}
		}
	}


	private String getQueryCacheKey(HttpServletRequest request) {
		final String cacheKey = request.getRequestURI() + request.getQueryString();
		log.debug("Cache key is: " + cacheKey);
		return cacheKey;
	}
	
}