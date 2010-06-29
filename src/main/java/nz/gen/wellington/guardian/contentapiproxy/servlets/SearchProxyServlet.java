package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
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
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@SuppressWarnings("serial")
@Singleton
public class SearchProxyServlet extends HttpServlet {

	private static final int OUTGOING_TTL = 300;
	
	private static final int DEFAULT_PAGE_SIZE = 10;
	
	Logger log = Logger.getLogger(SearchProxyServlet.class);

	private GuardianDataSource datasource;
	private MemcacheService cache;
	private ArticleSectionSorter articleSectionSorter;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public SearchProxyServlet(RssDataSource datasource, ArticleSectionSorter articleSectionSorter, ArticleToXmlRenderer articleToXmlRenderer) {
		this.datasource = datasource;
		this.cache = MemcacheServiceFactory.getMemcacheService();
		this.articleSectionSorter = articleSectionSorter;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/search")) {
					
            SearchQuery query = getSearchQueryFromRequest(request);
            query.setShowAllFields(false);
            if (request.getParameter("show-fields") != null && request.getParameter("show-fields").equals("true")) {
                query.setShowAllFields(true);
            }
            
            query.setShowAllTags(false);
            if (request.getParameter("show-tags") != null && request.getParameter("show-tags").equals("true")) {
                query.setShowAllTags(true);
            }
                        
            final String queryCacheKey = getQueryCacheKey(request);
            String output = (String) cache.get(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            } 
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);	
				output = getContent(query);
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

	
	private String getContent(SearchQuery query) {
		List<Article> articles = datasource.getArticles(query);				
		articles = articleSectionSorter.sort(articles);
		
		int pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
		if (pageSize < articles.size()) {
			log.info("Limiting articles to: " + pageSize);
			articles = articles.subList(0, pageSize);
		}
				
		List<Tag> refinements = null;
		if (query.getSection() != null) {
			refinements = datasource.getSectionRefinements(query.getSection());

		} else if (query.getTag() != null) {
			refinements = datasource.getTagRefinements(query.getTag());
		}
		
		return articleToXmlRenderer.outputXml(articles, refinements, query.isShowAllFields());
	}

	
	private String getQueryCacheKey(HttpServletRequest request) {
		final String cacheKey = request.getRequestURI() + request.getQueryString();
		log.debug("Cache key is: " + cacheKey);
		return cacheKey;
	}


	private SearchQuery getSearchQueryFromRequest(HttpServletRequest request) {
		SearchQuery query = new SearchQuery();
		if (request.getParameter("section") != null) {
			query.setSection(request.getParameter("section"));
		}
		if (request.getParameter("tag") != null) {
			query.setTag(request.getParameter("tag"));
		}
		
		if (request.getParameter("page-size") != null) {
			try {
				Integer pageSize = Integer.parseInt(request.getParameter("page-size"));
				log.debug("Query page size set to: " + pageSize);
				query.setPageSize(pageSize);
			} catch (NumberFormatException e) {
			}
		}
		return query;
	}

}