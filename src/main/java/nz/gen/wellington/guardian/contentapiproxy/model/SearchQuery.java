package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.joda.time.DateTime;

public class SearchQuery {

	private List<Tag> tags;
	private String combinerTag;
	private boolean showAllFields;	
	private boolean showAllTags;
	private Integer pageSize;
	private DateTime fromDate;
	private DateTime toDate;
	
	public SearchQuery() {
		tags = new ArrayList<Tag>();
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
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
	
	public void addTag(Tag tag) {
		this.tags.add(tag);
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

	public boolean isSingleSectionQuery() {
		return isSingleTagQuery() && tags.get(0).isSectionKeyword();
	}
	
	public boolean isSingleTagQuery() {
		return getTags() != null && getTags().size() == 1;
	}
	
	public boolean isSingleTagOrSectionQuery() {
		int count = getTags().size();
		return count <= 1;
	}

	public boolean isTopStoriesQuery() {
		if (getTags() != null) {
			for (Tag tag : getTags()) {
				if (!tag.isContentTypeTag()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean isTagCombinerQuery() {
		return combinerTag != null;
	}

	public String getCombinerTag() {
		return combinerTag;
	}

	public void setCombinerTag(String combinerTag) {
		this.combinerTag = combinerTag;
	}
		
}
