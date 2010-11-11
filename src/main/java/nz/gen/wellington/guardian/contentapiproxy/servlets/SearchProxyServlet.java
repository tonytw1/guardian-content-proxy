package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.ArticleSectionSorter;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class SearchProxyServlet extends CacheAwareProxyServlet {
	
	private static final int DEFAULT_PAGE_SIZE = 10;
	
	private GuardianDataSource datasource;
	private ArticleSectionSorter articleSectionSorter;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public SearchProxyServlet(RssDataSource datasource, ArticleSectionSorter articleSectionSorter, ArticleToXmlRenderer articleToXmlRenderer) {
		super();
		this.datasource = datasource;
		this.articleSectionSorter = articleSectionSorter;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/search")) {					
            SearchQuery query = getSearchQueryFromRequest(request);

            // TODO 'true' is not compatible with the Content API spec. Migrate away from this.
            query.setShowAllFields(false); 
            final String showFieldsParameter = request.getParameter("show-fields");
			if (showFieldsParameter != null && (showFieldsParameter.equals("all") || showFieldsParameter.equals("true"))) {
                query.setShowAllFields(true);
            }
            
            // TODO 'true' is not compatible with the Content API spec. Migrate away from this.
			query.setShowAllTags(false);
            String showAllTagsParameter = request.getParameter("show-tags");
			if (showAllTagsParameter != null && (showAllTagsParameter.equals("all") || showAllTagsParameter.equals("true"))) {
                query.setShowAllTags(true);
            }
			
            final String queryCacheKey = getQueryCacheKey(request);
            String output = cacheGet(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            } 
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);	
				output = getContent(query);
				if (output != null) {
					cacheContent(queryCacheKey, output);
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
		if (articles == null) {
			return null;
		}
		
		articles = articleSectionSorter.sort(articles);
		
		int pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
		if (pageSize < articles.size()) {
			log.info("Limiting articles to: " + pageSize);
			articles = articles.subList(0, pageSize);
		}
				
		Map<String, List<Tag>> refinements = null;
		if (query.getSection() != null) {
			refinements = datasource.getSectionRefinements(query.getSection());

		} else if (query.getTag() != null) {
			refinements = datasource.getTagRefinements(query.getTag());
		}
		
		String description = null;
		if (isNotDefaultMeaninglessDescription(datasource.getDescription())) {
			description = datasource.getDescription();
		}
		return articleToXmlRenderer.outputXml(articles, description, refinements, query.isShowAllFields());
	}
	
	
	private boolean isNotDefaultMeaninglessDescription(String description) {
		return description != null && !(description.startsWith("Articles published by guardian.co.uk")
				|| description.startsWith("Latest news and features from guardian.co.uk, the world's leading liberal voice"));
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