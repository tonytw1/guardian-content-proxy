package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Refinement;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;

public class DateRefinementImprover {

	private static Logger log = Logger.getLogger(DateRefinementImprover.class);

	private ContentApi contentApi;
	
	@Inject
	public DateRefinementImprover(ContentApi contentApi) {
		this.contentApi = contentApi;
	}

	public List<Refinement> generateDateRefinementsForTag(SearchQuery query) {
		log.info("Generating date refinements for query: " + query.toString());
				
		Tag tag = query.getTags().get(0);
		DateTime fromDate = query.getFromDate();
		DateTime toDate = query.getToDate();
		
		boolean notDateRefinementGiven = fromDate == null && toDate == null;
	
		boolean yearDateRefinementGiven = fromDate != null && toDate != null && 
			fromDate.getDayOfMonth() == 1 && fromDate.getMonthOfYear() == 1 && 
			toDate.getDayOfMonth() == 31 && toDate.getMonthOfYear() == 12;
		
		boolean monthDateRefinementGiven =  fromDate != null && toDate != null && 
			fromDate.getYear() == toDate.getYear() && 
			fromDate.getMonthOfYear() == toDate.getMonthOfYear() &&
			fromDate.getDayOfMonth() == 1 && toDate.getDayOfMonth() >= 28;
			
		if (notDateRefinementGiven) {
			return createYearDateRefinementsForTag(tag, fromDate, toDate);
		} else if (yearDateRefinementGiven) {
			return createMonthDateRefinementsForTagAndYear(tag, fromDate);
		} else if (monthDateRefinementGiven) {
			return createDayDateRefinementsForTagAndYear(tag, fromDate);
		}
		
		return null;
	}


	private List<Refinement> createDayDateRefinementsForTagAndYear(Tag tag, DateTime fromDate) {
		DateTime day = new DateTime(fromDate);
		List<Refinement> dayRefinements = new ArrayList<Refinement>();
		while (day.isBefore(fromDate.plusMonths(1))) {
			String id = "date/" + day.toString("YYYY-MM-dd");			
			String refinedUrl = "http://4.guardian-lite.appspot.com/search&format=xml" +  
				"&from-date=" + day.toString("yyyy-MM-dd") +
				"&to-date=" + day.toString("yyyy-MM-dd") + 
				"&tag=" + tag.getId();
			
			Refinement dayRefinement = new Refinement("date", id, day.toString("d MMM YYYY"), refinedUrl, 1);
			log.debug("Adding month refinement: " + dayRefinement.getDisplayName());
			dayRefinements.add(dayRefinement);
			day = day.plusDays(1);
		}
		return dayRefinements;
	}

	private List<Refinement> createMonthDateRefinementsForTagAndYear(Tag tag, DateTime fromDate) {
		DateTime month = new DateTime(fromDate);
		
		List<Refinement> monthRefinements = new ArrayList<Refinement>();
		while (month.isBefore(fromDate.plusYears(1))) {
			String id = "date/" + month.toString("YYYY-MM");			
			String refinedUrl = "http://4.guardian-lite.appspot.com/search&format=xml" +  
				"&from-date=" + month.toString("yyyy-MM-dd") +
				"&to-date=" + month.plusMonths(1).minusDays(1).toString("yyyy-MM-dd") +
				"&tag=" + tag.getId();
			
			Refinement monthRefinement = new Refinement("date", id, month.toString("MMM YYYY"), refinedUrl, 1);
			log.debug("Adding month refinement: " + monthRefinement.getDisplayName());
			monthRefinements.add(monthRefinement);
			month = month.plusMonths(1);
		}
		return monthRefinements;
	}
	
	private List<Refinement> createYearDateRefinementsForTag(Tag tag, DateTime fromDate, DateTime toDate) {
		Map<String, List<Refinement>> tagRefinements = contentApi.getTagRefinements(tag, fromDate, toDate);
		if (tagRefinements.containsKey("date")) {
			List<Refinement> yearRefinements = new ArrayList<Refinement>();
			for (Refinement refinement : tagRefinements.get("date")) {
				if (refinement.getDisplayName().matches("\\d\\d\\d\\d")) {
					log.info("Adding year date refinement: " + refinement.getDisplayName());
					yearRefinements.add(refinement);
				}
			}
			return yearRefinements;
		}
		return null;
	}

}
