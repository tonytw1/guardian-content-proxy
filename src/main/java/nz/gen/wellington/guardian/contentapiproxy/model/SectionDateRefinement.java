package nz.gen.wellington.guardian.contentapiproxy.model;

import org.joda.time.DateTime;

public class SectionDateRefinement implements Refinement {

	private String sectionId;
	private String displayName;
	private DateTime fromDate;
	private DateTime toDate;

	public SectionDateRefinement(String sectionId, String displayName, DateTime fromDate, DateTime toDate) {
		this.sectionId = sectionId;
		this.displayName = displayName;
		this.fromDate = fromDate;
		this.toDate = toDate;	
	}

	public String getSectionId() {
		return sectionId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public DateTime getFromDate() {
		return fromDate;
	}

	public DateTime getToDate() {
		return toDate;
	}
	
}
