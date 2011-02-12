package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.Refinement;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Section;

public abstract class AbstractGuardianDataSource implements GuardianDataSource {
	
	protected ContentApi contentApi;
	protected SectionCleaner sectionCleaner;
	
	public Map<String, Section> getSections() {		
		Map<String, Section> sections = contentApi.getSections();
		return sectionCleaner.cleanSections(sections);		
	}
	
	public Map<String, List<Refinement>> getRefinements(SearchQuery query) {
		if (query.isSingleSectionQuery()) {			
			String sectionId = query.getSections().get(0);
			return getTagRefinements(sectionId + "/" + sectionId);
			
		} else if (query.isSingleTagQuery()) {
			return getTagRefinements(query.getTags().get(0));
		}
		return null;
	}
	
	private Map<String, List<Refinement>> getTagRefinements(String tagId) {
		return contentApi.getTagRefinements(tagId);
	}
	
}
