package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class RequestQueryParser {

	static Logger log = Logger.getLogger(RequestQueryParser.class);
	
	public SearchQuery getSearchQueryFromRequest(HttpServletRequest request) {
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
