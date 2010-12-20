package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.ArticleSectionSorter;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Deprecated
@SuppressWarnings("serial")
@Singleton
public class FavouritesServlet extends CacheAwareProxyServlet {
	
	Logger log = Logger.getLogger(FavouritesServlet.class);

	private GuardianDataSource datasource;
	private ArticleSectionSorter articleSectionSorter;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public FavouritesServlet(RssDataSource datasource, ArticleSectionSorter articleSectionSorter, ArticleToXmlRenderer articleToXmlRenderer) {
		super();
		this.datasource = datasource;
		this.articleSectionSorter = articleSectionSorter;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/favourites")) {			
            final String queryCacheKey = getQueryCacheKey(request);
            String output = cacheGet(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            }
            
			if (output == null) {
				log.info("Building result for call: " + queryCacheKey);
				List<String> favouriteSections = parseSectionsFromRequest(request);				
				List<String> favouriteTags = parseTagsFromRequest(request);
				
				List<Article> combined = ((RssDataSource) datasource).populateFavouriteArticles(favouriteSections, favouriteTags, 15);	// TODO Push size up
				if (combined != null) {					
					combined = articleSectionSorter.sort(combined);				
					boolean showAll = false;
					if (request.getParameter("show-fields") != null && (request.getParameter("show-fields").equals("true") || request.getParameter("show-fields").equals("all"))) {
						showAll = true;
					}
					
					output = articleToXmlRenderer.outputXml(combined, null, null, showAll);
					if (output != null) {
						cacheContent(queryCacheKey, output);
					}
				}
			}
			
			if (output != null) {
				log.info("Outputing content: " + output.length() + " characters");
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
				response.addHeader("Etag", DigestUtils.md5Hex(output));
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
				if (!section.trim().equals("")) {
					list.add(section);
				}
			}
		}
		return list;
	}
	
}