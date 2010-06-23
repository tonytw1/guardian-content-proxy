package nz.gen.wellington.guardian.contentapiproxy.servlets;

public class SearchQuery {

	private String tag;
	private String section;
	private boolean showAllFields;	
	private boolean showAllTags;
	private Integer pageSize;
	
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
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
