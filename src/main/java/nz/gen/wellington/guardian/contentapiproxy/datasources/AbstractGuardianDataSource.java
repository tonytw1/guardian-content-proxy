package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Refinement;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public abstract class AbstractGuardianDataSource implements GuardianDataSource {

	private static Logger log = Logger.getLogger(AbstractGuardianDataSource.class);
	
	protected ContentApi contentApi;
	protected SectionCleaner sectionCleaner;
	
	public Map<String, Section> getSections() {		
		Map<String, Section> sections = contentApi.getSections();
		return sectionCleaner.cleanSections(sections);		
	}
	
	public Map<String, List<Refinement>> getRefinements(SearchQuery query) {
		if (query.isSingleTagQuery()) {
			Map<String, List<Refinement>> refinements = getTagRefinements(query.getTags().get(0));
			return refinements;			
		}
		return null;
	}
	
	private Map<String, List<Refinement>> getTagRefinements(Tag tag) {
		return contentApi.getTagRefinements(tag, new DateTime().minusWeeks(2), new DateTime()
		);
	}
	
}
