package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.List;

public class SearchQuery {

	private List<String> tags;
	private List<String> sections;
	private boolean showAllFields;	
	private boolean showAllTags;
	private Integer pageSize;
	
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
	
}
