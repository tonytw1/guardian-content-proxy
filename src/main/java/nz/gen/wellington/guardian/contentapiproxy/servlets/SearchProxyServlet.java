package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.DateRefinementImprover;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ContentApiDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.output.ArticleToXmlRenderer;
import nz.gen.wellington.guardian.contentapiproxy.requests.RequestQueryParser;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Refinement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class SearchProxyServlet extends CacheAwareProxyServlet {
	
	private static Logger log = Logger.getLogger(SearchProxyServlet.class);
		
	private GuardianDataSource rssDataSource;
	private GuardianDataSource contentApiDataSource;
	private RequestQueryParser requestQueryParser;
	private ArticleToXmlRenderer articleToXmlRenderer;
	private DateRefinementImprover dateRefinementImprover;
	
	@Inject
	public SearchProxyServlet(RssDataSource rssDataSource, ContentApiDataSource contentApiDataSource, ArticleToXmlRenderer articleToXmlRenderer, RequestQueryParser requestQueryParser, DateRefinementImprover dateRefinementImprover) {
		super();
		this.rssDataSource = rssDataSource;
		this.contentApiDataSource = contentApiDataSource;
		this.articleToXmlRenderer = articleToXmlRenderer;
		this.requestQueryParser = requestQueryParser;
		this.dateRefinementImprover = dateRefinementImprover;
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/search")) {					
            SearchQuery query = requestQueryParser.getSearchQueryFromRequest(request);

            List<GuardianDataSource> datasources = new ArrayList<GuardianDataSource>();
            datasources.add(rssDataSource);
            datasources.add(contentApiDataSource);
            
			for (GuardianDataSource dataSource : datasources) {

				if (dataSource.isSupported(query)) {

					final String queryCacheKey = getQueryCacheKey(request);
					String output = cacheGet(queryCacheKey);
					if (output != null) {
						log.info("Returning cached results for call url: " + queryCacheKey);
					}
					
					if (output == null) {
						log.info("Building result for call: " + queryCacheKey);
						output = getContent(query, dataSource);
						if (output != null) {
							cacheContent(queryCacheKey, output);
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
						return;
					}
				}
			}
		}
		
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);				
		return;
	}

	
	private String getContent(SearchQuery query, GuardianDataSource datasource) {
		log.info("Getting content for query: " + query.toString());
		List<Article> articles = datasource.getArticles(query);
		if (articles == null) {
			return null;
		}
		
		log.info("Getting refinements");
		Map<String, List<Refinement>> refinements = datasource.getRefinements(query);
		
		if (refinements != null) {
			refinements.remove("date");
			if (query.isSingleTagOrSectionQuery()) {
				List<Refinement> dateRefinements = dateRefinementImprover.generateDateRefinementsForTag(query);
				if (dateRefinements != null) {
					refinements.put("date", dateRefinements);
				}
			}
		}
		
		return articleToXmlRenderer.outputXml(articles, datasource.getDescription(), refinements, query.isShowAllFields());
	}
	
}