package nz.gen.wellington.guardian.contentapiproxy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


import org.joda.time.DateTime;

public class Article implements Serializable {

	private static final long serialVersionUID = 5L;
	
	String id;
	String title;
	String byline;
	DateTime pubDate;
	String standfirst;
	String description;
	
	Section section;
	List<Tag> tags;
	String thumbnailUrl;
	String mainImageUrl;
	String caption;
	
	List<MediaElement> mediaElements;
	
	public Article() {
		tags = new ArrayList<Tag>();
		mediaElements = new ArrayList<MediaElement>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public String getStandfirst() {
		return standfirst;
	}

	public void setStandfirst(String standfirst) {
		this.standfirst = standfirst;
	}

	public DateTime getPubDate() {
		return pubDate;
	}

	public void setPubDate(DateTime dateTime) {
		this.pubDate = dateTime;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Tag> getTags() {
		return tags;
	}
	
	public void addTag(Tag tag) {
		tags.add(tag);
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getMainImageUrl() {
		return mainImageUrl;
	}
	
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public void setMainImageUrl(String mainImageUrl) {
		this.mainImageUrl = mainImageUrl;
	}

	public String getPubDateString() {
		if (this.pubDate != null) {
			return pubDate.toString("EEEE d MMMM yyyy HH.mm");			
		}
		return null;
	}

	public String getByline() {
		return byline;
	}

	public void setByline(String byline) {
		this.byline = byline;
	}

	public Section getSection() {
		return section;
	}

	public void setSection(Section section) {
		this.section = section;
	}

	public void addMediaElement(MediaElement picture) {
		mediaElements.add(picture);		
	}

	public List<MediaElement> getMediaElements() {
		return mediaElements;
	}
	
}
