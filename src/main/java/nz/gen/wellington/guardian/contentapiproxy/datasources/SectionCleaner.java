package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.inject.Inject;

import nz.gen.wellington.guardian.contentapi.cleaning.HtmlCleaner;
import nz.gen.wellington.guardian.model.Section;

public class SectionCleaner {
	
	private List<String> BAD_SECTIONS_IDS = Arrays.asList("community", "crosswords", "extra", "help", "info", "local", "news", "weather");

	private HtmlCleaner htmlCleaner;
	
	@Inject
	public SectionCleaner(HtmlCleaner htmlCleaner) {
		this.htmlCleaner = htmlCleaner;
	}

	public Map<String, Section> cleanSections(Map<String, Section> sections) {
		if (sections != null) {
			sections = removeBadSections(sections);
			sections = stripHtmlFromSectionNames(sections);
		}
 		return sections;
	}
	
	private Map<String, Section> stripHtmlFromSectionNames(Map<String, Section> sections) {
		Map<String, Section> cleanedSections = new TreeMap<String, Section>();						
		for (String sectionName : sections.keySet()) {
			Section section = sections.get(sectionName);
			section.setName(htmlCleaner.stripHtml(section.getName()));
			cleanedSections.put(section.getId(), section);
		}
		return cleanedSections;
	}
	
	
	private Map<String, Section> removeBadSections(Map<String, Section> sections) {
		Map<String, Section> allowedSections = new TreeMap<String, Section>();						
		for (String sectionId : sections.keySet()) {
			if (!BAD_SECTIONS_IDS.contains(sectionId)) {
				Section section = sections.get(sectionId);
				allowedSections.put(section.getId(), section);				
			}
		}
		return allowedSections;
	}
	
}
