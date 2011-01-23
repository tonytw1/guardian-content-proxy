package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ContentApiDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.SectionDateRefinement;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class SearchProxyServlet extends CacheAwareProxyServlet {
		
	private GuardianDataSource rssDataSource;
	private GuardianDataSource contentApiDataSource;

	private ArticleToXmlRenderer articleToXmlRenderer;
	
	@Inject
	public SearchProxyServlet(RssDataSource rssDataSource, ContentApiDataSource contentApiDataSource, ArticleToXmlRenderer articleToXmlRenderer) {
		super();
		this.rssDataSource = rssDataSource;
		this.contentApiDataSource = contentApiDataSource;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/search")) {					
            SearchQuery query = getSearchQueryFromRequest(request);

            List<GuardianDataSource> datasources = new ArrayList<GuardianDataSource>();
            datasources.add(rssDataSource);
            datasources.add(contentApiDataSource);
            
			for (GuardianDataSource dataSource : datasources) {

				if (dataSource.isSupported(query)) {

					final String queryCacheKey = getQueryCacheKey(request);
					String output = cacheGet(queryCacheKey);
					if (output != null) {
						log.info("Returning cached results for call url: "
								+ queryCacheKey);
					}

					if (output == null) {
						log.info("Building result for call: " + queryCacheKey);
						output = getContent(query, dataSource);
						if (output != null) {
							cacheContent(queryCacheKey, output);
						}
					}

					if (output != null) {
						log.info("Outputing content: " + output.length()
								+ " characters");
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
		List<Article> articles = datasource.getArticles(query);
		if (articles == null) {
			return null;
		}
		
		Map<String, List<Refinement>> refinements = null;

		final boolean isSectionQuery = query.getSections() != null && query.getSections().size() == 1;
		if (isSectionQuery) {
			
			String sectionId = query.getSections().get(0);
			refinements = datasource.getSectionRefinements(sectionId);
			refinements.put("date", generateDateRefinementsForSection(sectionId, query.getFromDate()));
			
		} else if (query.getTags() != null && query.getTags().size() == 1) {
			refinements = datasource.getTagRefinements(query.getTags().get(0));
		}
		
		
		
		return articleToXmlRenderer.outputXml(articles, datasource.getDescription(), refinements, query.isShowAllFields());
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
	
	
	private SearchQuery getSearchQueryFromRequest(HttpServletRequest request) {
		SearchQuery query = new SearchQuery();
		if (request.getParameter("section") != null) {
			extractIds(request.getParameter("section"), query);
		}
		if (request.getParameter("tag") != null) {
			extractIds(request.getParameter("tag"), query);
		}
		
		if (request.getParameter("page-size") != null) {
			try {
				Integer pageSize = Integer.parseInt(request.getParameter("page-size"));
				log.debug("Query page size set to: " + pageSize);
				query.setPageSize(pageSize);
			} catch (NumberFormatException e) {
			}
		}
		
		if (request.getParameter("from-date") != null) {
			query.setFromDate(new DateTime(request.getParameter("from-date")));
			log.debug("Query from date set to: " + query.getFromDate().toString("yyyy-MM-dd"));
		}
		
		if (request.getParameter("to-date") != null) {
			query.setToDate(new DateTime(request.getParameter("to-date")));
			log.debug("Query to date set to: " + query.getToDate().toString("yyyy-MM-dd"));
		}
		
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
				
		return query;
	}


	private void extractIds(String parameter, SearchQuery query) {
		parameter = URLDecoder.decode(parameter);
		log.info("Parameter: " + parameter);
		String[] fields = parameter.split("\\|");
		List<String> asList = Arrays.asList(fields);
		log.info(asList);
		for (String field : asList) {
			log.info("Field: " + field);

			String[] sectionAndTagIds = field.split("/");
			String sectionId = sectionAndTagIds[0];
			
			if (sectionAndTagIds.length > 1) {
				String tagId = sectionAndTagIds[1];
				if (sectionId.equals(tagId)) {
					query.addSection(sectionId);
				} else {
					query.addTag(field);
				}
				
			} else {
				query.addSection(sectionId);
			}			
		}
	}

}