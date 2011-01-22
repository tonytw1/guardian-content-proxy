package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class SearchQuery {

	private List<String> tags;
	private List<String> sections;
	private boolean showAllFields;	
	private boolean showAllTags;
	private Integer pageSize;
	private DateTime fromDate;
	private DateTime toDate;
	
	public SearchQuery() {
		sections = new ArrayList<String>();
		tags = new ArrayList<String>();
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	public List<String> getSections() {
		return sections;
	}

	public void setSections(List<String> sections) {
		this.sections = sections;
	}

	public boolean isShowAllFields() {
		return showAllFields;
	}

	public void setShowAllFields(boolean showAllFields) {
		this.showAllFields = showAllFields;
	}

	public boolean isShowAllTags() {
		return showAllTags;
	}

	public void setShowAllTags(boolean showAllTags) {
		this.showAllTags = showAllTags;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public void addSection(String sectionId) {
		this.sections.add(sectionId);
	}

	public void addTag(String tagId) {
		this.tags.add(tagId);
	}

	public boolean hasDateRefinement() {
		return fromDate != null || toDate != null;
	}

	public DateTime getFromDate() {
		return fromDate;
	}

	public void setFromDate(DateTime fromDate) {
		this.fromDate = fromDate;
	}

	public DateTime getToDate() {
		return toDate;
	}

	public void setToDate(DateTime toDate) {
		this.toDate = toDate;
	}
	
}
