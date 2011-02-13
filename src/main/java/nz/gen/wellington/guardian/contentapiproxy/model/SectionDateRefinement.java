package nz.gen.wellington.guardian.contentapiproxy.model;

import nz.gen.wellington.guardian.model.Tag;

import org.joda.time.DateTime;

public class SectionDateRefinement implements Refinement {

	private Tag tag;
	private String displayName;
	private DateTime fromDate;
	private DateTime toDate;

	public SectionDateRefinement(Tag tag, String displayName, DateTime fromDate, DateTime toDate) {
		this.tag = tag;
		this.displayName = displayName;
		this.fromDate = fromDate;
		this.toDate = toDate;	
	}

	public Tag getTag() {
		return tag;
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
