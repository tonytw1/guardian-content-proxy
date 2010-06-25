package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;

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

	Logger log = Logger.getLogger(SearchProxyServlet.class);

	GuardianDataSource datasource;
	MemcacheService cache;

	
	@Inject
	public SearchProxyServlet(RssDataSource datasource) {
		this.datasource = datasource;
		this.cache = MemcacheServiceFactory.getMemcacheService();
	}
	

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/search")) {
					
            SearchQuery query = getSearchQueryFromRequest(request);
            query.setShowAllFields(true);
            query.setShowAllTags(true);
                        
            final String queryCacheKey = getQueryCacheKey(request);
            String output = (String) cache.get(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            } 
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);				
	
				output = datasource.getContent(query);
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