package nz.gen.wellington.guardian.contentapiproxy.requests;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class RequestQueryParser {

	static Logger log = Logger.getLogger(RequestQueryParser.class);
	
	private Pattern complexTagParameterPattern = Pattern.compile("\\((.*?)\\),\\((.*?)\\)");
	
	public SearchQuery getSearchQueryFromRequest(HttpServletRequest request) {
		SearchQuery query = new SearchQuery();
		if (request.getParameter("section") != null) {
			extractOrSeperatedTagsIds(query, request.getParameter("section"));
		}
		
		final String tagParameter = request.getParameter("tag");
		if (tagParameter != null) {
			
			if (isCompoundTagQuery(tagParameter)) {
				log.info(tagParameter + " is a compound tag query");
				final String leftMostTagGroup = extractLeftMostTagGroupFromComplexTagQuery(tagParameter);
				final String rightMostTagGroup = extractRightMostTagGroupFromComplexTagQuery(tagParameter);

				log.info("Leftmost tag group is: " + leftMostTagGroup);
				extractOrSeperatedTagsIds(query, leftMostTagGroup);
				
				log.info("Rightmore tag group is: " + rightMostTagGroup);
				if (rightMostTagGroup != null && rightMostTagGroup.equals("type/gallery")) {
					query.setCombinerTag(rightMostTagGroup);
				}
				
			} else {
				extractOrSeperatedTagsIds(query, tagParameter);
			}
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
	
	private boolean isCompoundTagQuery(String tagParameter) {
		return complexTagParameterPattern.matcher(tagParameter).matches();
		
	}
	
	private String extractLeftMostTagGroupFromComplexTagQuery(String tagParameter) {
		 Matcher matcher = complexTagParameterPattern.matcher(tagParameter);
		 if (matcher.matches()) {
			 return matcher.group(1);
		 }
		 return null;
	}
	
	
	private String extractRightMostTagGroupFromComplexTagQuery(String tagParameter) {
		 Matcher matcher = complexTagParameterPattern.matcher(tagParameter);
		 if (matcher.matches()) {
			 return matcher.group(2);
		 }
		 return null;
	}

	private void extractOrSeperatedTagsIds(SearchQuery query, String parameter) {
		parameter = URLDecoder.decode(parameter);
		parameter = parameter.replaceAll("\\(", "");	// TODO
		parameter = parameter.replaceAll("\\)", "");

		log.info("Parameter: " + parameter);
		String[] fields = parameter.split("\\|");
		List<String> asList = Arrays.asList(fields);
		for (String field : asList) {
			log.info("Field: " + field);			
			Tag tag = new Tag(null, field, null, null);
			query.addTag(tag);				
		}
	}
	
}
