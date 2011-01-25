package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;

import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.SectionDateRefinement;

public abstract class AbstractGuardianDataSource implements GuardianDataSource {
	
	// TODO push section filtering to it's own class
	private List<String> badSectionNames = Arrays.asList("community", "crosswords", "extra", "help", "info", "local", "theguardian", "theobserver", "news", "weather");
	
	
	protected ContentApi contentApi;

	
	public Map<String, Section> getSections() {		
		Map<String, Section> sections = contentApi.getSections();
		if (sections != null) {
			sections = stripHtmlFromSectionNames(sections);
			sections = removeBadSections(sections);			
		}
 		return sections;		
	}
	
		
	public Map<String, List<Refinement>> getRefinements(SearchQuery query) {		
		final boolean isSectionQuery = query.getSections() != null && query.getSections().size() == 1;
		if (isSectionQuery) {
			
			String sectionId = query.getSections().get(0);
			Map<String, List<Refinement>> refinements = getSectionRefinements(sectionId);
			refinements.put("date", generateDateRefinementsForSection(sectionId, query.getFromDate()));
			
		} else if (query.getTags() != null && query.getTags().size() == 1) {
			return getTagRefinements(query.getTags().get(0));
		}
		return null;
	}
	
	
	public Map<String, List<Refinement>> getSectionRefinements(String sectionId) {
		return contentApi.getSectionRefinements(sectionId);
	}
	
	public Map<String, List<Refinement>> getTagRefinements(String tagId) {
		return contentApi.getTagRefinements(tagId);
	}
	
	
	private Map<String, Section> stripHtmlFromSectionNames(Map<String, Section> sections) {
		Map<String, Section> cleanedSections = new TreeMap<String, Section>();						
		for (String sectionName : sections.keySet()) {
			Section section = sections.get(sectionName);
			section.setName(HtmlCleaner.stripHtml(section.getName()));
			cleanedSections.put(section.getId(), section);
		}
		return cleanedSections;
	}
	
	
	private Map<String, Section> removeBadSections(Map<String, Section> sections) {
		Map<String, Section> allowedSections = new TreeMap<String, Section>();						
		for (String sectionIds : sections.keySet()) {
			if (!badSectionNames.contains(sectionIds)) {
				Section section = sections.get(sectionIds);
				allowedSections.put(section.getId(), section);				
			}
		}
		return allowedSections;
	}
	
		
	// TODO this should be on the RSS datasource
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
	
}
