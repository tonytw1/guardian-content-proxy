package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Article;
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
			return createYearDateRefinementsForTag(query, tag, fromDate, toDate);
		} else if (yearDateRefinementGiven) {
			return createMonthDateRefinementsForTagAndYear(query, tag, fromDate);
		} else if (monthDateRefinementGiven) {
			return createWeekDateRefinementsForTagAndMonth(query, tag, fromDate);
		} else {
			return createDayDateRefinementsForTagAndWeek(query, tag, fromDate, toDate);
		}
	}
	
	private List<Refinement> createWeekDateRefinementsForTagAndMonth(SearchQuery query, Tag tag, DateTime fromDate) {
		DateTime week = new DateTime(fromDate);		
		List<Refinement> weekRefinements = new ArrayList<Refinement>();
		while (week.isBefore(fromDate.plusMonths(1))) {	// TODO overlaps with first week of next month
			String id = "date/weekof/" + week.toString("YYYY-MM-dd");			
			final String refinedUrl = "http://4.guardian-lite.appspot.com/search&format=xml" +  
				"&from-date=" + week.toString("yyyy-MM-dd") + 
				"&to-date=" + week.plusWeeks(1).minusDays(1).toString("yyyy-MM-dd") +
				"&tag=" + tag.getId();			
						
			int count = getRefinementCountForTagDateRange(query, week, week.plusWeeks(1));
			if (count > 0) {
				Refinement weekRefinement = createRefinementForWeek(week, id, refinedUrl, count);
				log.debug("Adding week refinement: " + weekRefinement.getDisplayName());
				weekRefinements.add(weekRefinement);
			}
			week = week.plusWeeks(1);
		}
		Collections.reverse(weekRefinements);
		return weekRefinements;
	}

	

	private List<Refinement> createDayDateRefinementsForTagAndWeek(SearchQuery query, Tag tag, DateTime fromDate, DateTime toDate) {
		DateTime day = new DateTime(fromDate);
		List<Refinement> dayRefinements = new ArrayList<Refinement>();
		while (!day.isAfter(toDate)) {
			String id = "date/" + day.toString("YYYY-MM-dd");			
			String refinedUrl = "http://4.guardian-lite.appspot.com/search&format=xml" +  
				"&from-date=" + day.toString("yyyy-MM-dd") +
				"&to-date=" + day.toString("yyyy-MM-dd") + 
				"&tag=" + tag.getId();

			int count = getRefinementCountForTagDateRange(query, day, day);
			if (count > 0) {
				Refinement dayRefinement = createRefinementForDay(day, id, refinedUrl, count);
				log.debug("Adding month refinement: " + dayRefinement.getDisplayName());
				dayRefinements.add(dayRefinement);
			}
			day = day.plusDays(1);
		}
		Collections.reverse(dayRefinements);
		return dayRefinements;
	}
	

	private List<Refinement> createMonthDateRefinementsForTagAndYear(SearchQuery query, Tag tag, DateTime fromDate) {
		DateTime month = new DateTime(fromDate);
		
		List<Refinement> monthRefinements = new ArrayList<Refinement>();
		while (month.isBefore(fromDate.plusYears(1))) {
			String id = "date/" + month.toString("YYYY-MM");			
			String refinedUrl = "http://4.guardian-lite.appspot.com/search&format=xml" +  
				"&from-date=" + month.toString("yyyy-MM-dd") +
				"&to-date=" + month.plusMonths(1).minusDays(1).toString("yyyy-MM-dd") +
				"&tag=" + tag.getId();
			
			int count = getRefinementCountForTagDateRange(query, month, month.plusMonths(1));
			if (count > 0) {
				Refinement monthRefinement = createRefinementForMonth(month, id, refinedUrl, count);
				log.debug("Adding month refinement: " + monthRefinement.getDisplayName());
				monthRefinements.add(monthRefinement);
			}
			month = month.plusMonths(1);
		}
		Collections.reverse(monthRefinements);
		return monthRefinements;
	}

	
	private List<Refinement> createYearDateRefinementsForTag(SearchQuery query, Tag tag, DateTime fromDate, DateTime toDate) {
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
	
	
	private int getRefinementCountForTagDateRange(SearchQuery query, DateTime fromDate, DateTime toDate) {
		query.setFromDate(fromDate);	// TODO do this on a copy!
		query.setToDate(toDate);
		return contentApi.getArticleCount(query);		
	}

	
	private Refinement createRefinementForMonth(DateTime month, String id, String refinedUrl, int count) {
		return new Refinement("date", id, month.toString("MMM YYYY"), refinedUrl, count);
	}
	private Refinement createRefinementForDay(DateTime day, String id, String refinedUrl, int count) {
		return new Refinement("date", id, day.toString("d MMM YYYY"), refinedUrl, count);
	}
	private Refinement createRefinementForWeek(DateTime week, String id, final String refinedUrl, int count) {
		return new Refinement("date", id, "Week beginning " + week.toString("dd MMM YYYY"), refinedUrl, count);
	}

}
