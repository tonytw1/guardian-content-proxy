package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.util.Map;

import nz.gen.wellington.guardian.contentapi.cleaning.HtmlCleaner;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.MediaElement;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.sun.syndication.feed.module.DCModule;
import com.sun.syndication.feed.module.mediarss.MediaEntryModuleImpl;
import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndEntry;

public class RssEntryToArticleConvertor {

	private static Logger log = Logger.getLogger(RssEntryToArticleConvertor.class);
	
	private static final String URL_PREFIX = "http://www.guardian.co.uk/";
	
	private HtmlCleaner htmlCleaner;
	
	@Inject
	public RssEntryToArticleConvertor(HtmlCleaner htmlCleaner) {
		this.htmlCleaner = htmlCleaner;
	}
	
	public Article entryToArticle(SyndEntry item, Map<String, Section> sections) {		
		DCModule dcModule = (DCModule) item.getModule("http://purl.org/dc/elements/1.1/");
		if (dcModule == null || dcModule.getType() == null || !(dcModule.getType().equals("Article") || dcModule.getType().equals("Gallery"))) {
			return null;
		}
		
		Article article = new Article();
		if (item.getLink().startsWith(URL_PREFIX)) {
			article.setId(item.getLink().replace(URL_PREFIX, ""));
			article.setWebUrl(item.getLink());			
		}
		article.setHeadline(htmlCleaner.stripHtml(item.getTitle()));
		article.setPubDate(item.getPublishedDate());
		article.setByline(htmlCleaner.stripHtml(item.getAuthor()));

		setSectionFromDCSubject(dcModule, article, sections);
		
		final String description = item.getDescription().getValue();
		processBody(description, article, sections);
		
		processMediaElements(item, article);
		
		
		if (dcModule.getType().equals("Gallery")) {
			article.addTag(new Tag("Gallery", "type/gallery", null, "type"));
		}
				
		if (article.getPubDate() == null) {
			log.warn("Dropping article with null publication date: " + item.getTitle());
			return null;
		}
		
		if (article.getSection() == null) {
			log.warn("Dropping article with null section: " + item.getTitle());
			return null;
		}
		return article;
	}


	private void processMediaElements(SyndEntry item, Article article) {
		MediaEntryModuleImpl mediaModule = (MediaEntryModuleImpl) item.getModule(MediaModule.URI);
        if (mediaModule != null) {
        
	         log.debug("Found media module");        
	         MediaContent[] mediaContents = mediaModule.getMediaContents();
	         if (mediaContents.length > 0) {

	        	 MediaContent firstMediaContent = mediaContents[0];	        	 
	        	 extractContentItemThumbnailFromFirstMediaContent(article, firstMediaContent);
	        	 
	        	 for (int i = 0; i < mediaContents.length; i++) {
	        		 MediaContent mediaContent = mediaContents[i];
	        		 
	        		 final boolean mediaElementIsPicture = mediaContent.getType() != null && mediaContent.getType().startsWith("image");
	        		 if (mediaElementIsPicture) {
	        			 UrlReference reference = (UrlReference) mediaContent.getReference();
		        		 Metadata metadata = mediaContent.getMetadata();

		        		 MediaElement picture = new MediaElement("picture", reference.getUrl().toExternalForm(), metadata.getDescription(), mediaContent.getWidth(), mediaContent.getHeight());        			 
	        			 if ( mediaContent.getMetadata() != null && mediaContent.getMetadata().getThumbnail() != null && mediaContent.getMetadata().getThumbnail().length > 0) {
	        				 picture.setThumbnail(mediaContent.getMetadata().getThumbnail()[0].getUrl().toExternalForm());
	        			 }
	        			 
	        			 article.addMediaElement(picture);
	        		 }
	        	 }
	         }
        }

	}

	private void extractContentItemThumbnailFromFirstMediaContent(Article article, MediaContent firstMediaContent) {		
		log.debug("Extracting thumbnail for: " + article.getHeadline());
		if (firstMediaContent.getType().startsWith("image")) {			
			// Thumbnail handling is different for articles and galleries.
			
			final boolean isGallery = article.getId() != null && article.getId().contains("gallery");	// TODO use DC type
			if (isGallery) {
				log.debug("Content item is a gallery");
				// Each media content item is a full sized image with a thumbnail field set.
				// TODO how to this with the RSS API?
				if (firstMediaContent.getMetadata() != null) {
					log.debug("Content item metadata is not null");

					Thumbnail[] thumbnails = firstMediaContent.getMetadata().getThumbnail();
					log.debug("First media content thumbnails: " + thumbnails);

					if (thumbnails != null && thumbnails.length > 0) {
						Thumbnail thumbnail = thumbnails[0];
						log.debug("First thumbnail: " + thumbnail);
						article.setThumbnailUrl(thumbnail.getUrl().toExternalForm());
					}
				}
				
			} else {
				// The first media content for an article is a thumbnail sized media content item.
				UrlReference reference = (UrlReference) firstMediaContent.getReference();
				if (firstMediaContent.getWidth() == 140 && firstMediaContent.getHeight() == 84) {
					article.setThumbnailUrl(reference.getUrl().toExternalForm());
				}
			}
		 }
	}


	private void processBody(final String description, Article article, Map<String, Section> sections) {
		Parser parser = new Parser();
		 try {
			extractStandfirstAndBodyText(description, article, parser);		
			extractTagsFromRelatedDiv(description, article, parser, sections);
			
		} catch (ParserException e) {
			log.error(e.getMessage());
		}
	}


	private void extractStandfirstAndBodyText(final String description,
			Article article, Parser parser) throws ParserException {
		parser.setInputHTML(description);
		NodeFilter standfirstFilter = new HasAttributeFilter("class", "standfirst");
		NodeList list = parser.extractAllNodesThatMatch(standfirstFilter);
		if (list.size() > 0) {
			final String standfirst = list.elementAt(0).toHtml();
			article.setStandfirst(htmlCleaner.stripHtml(standfirst));
			description.replace(standfirst, "");
		}
		
		StringBuilder body = new StringBuilder();
		parser.setInputHTML(description);
		NodeIterator elements = parser.elements();
		while (elements.hasMoreNodes()) {
			Node node = elements.nextNode();
			if (node instanceof org.htmlparser.Tag) {
				org.htmlparser.Tag tag = (org.htmlparser.Tag) node;
				final String tagClass = tag.getAttribute("class");
				if (tagClass == null || !(
						tagClass.equals("standfirst") || 
						tagClass.equals("track") ||
						tagClass.equals("related") || 
						tagClass.equals("terms") || 
						tagClass.equals("author"))) {
					body.append(tag.toHtml());
				}
			}
		}
		
		body.append("<p>&copy; Guardian News & Media Limited " + new DateTime().toString("yyyy") + "</p>");
		article.setDescription(htmlCleaner.stripHtml(body.toString()));
	}


	private void extractTagsFromRelatedDiv(final String description, Article article, Parser parser, Map<String, Section> sections) throws ParserException {
		parser.setInputHTML(description);
		NodeFilter relatedFilter = new HasAttributeFilter("class", "related");
		NodeList list = parser.extractAllNodesThatMatch(relatedFilter);
		if (list.size() > 0) {
			org.htmlparser.Tag related = (org.htmlparser.Tag) list.elementAt(0);
			
			Node tagList = related.getFirstChild();
			NodeList tags = tagList.getChildren();
			for (int i = 0; i < tags.size(); i++) {
				org.htmlparser.Tag tag = (org.htmlparser.Tag) tags.elementAt(i);
				
				org.htmlparser.Tag href = (org.htmlparser.Tag) tag.getFirstChild();
				String id = href.getAttribute("href");
				id = id.replace(URL_PREFIX, "");

				final String sectionId = id.split("/")[0];
				final String tagName = href.toPlainTextString();
				if (sectionId.equals("profile")) {
					article.addTag(new Tag(tagName, id, null, "contributor"));

				} else {
					Section section = sections.get(sectionId);
					article.addTag(new Tag(tagName, id, section, "keyword"));
				}
			}
		}
		
		parser.setInputHTML(description);
		NodeFilter authorsFilter = new HasAttributeFilter("class", "author");
		list = parser.extractAllNodesThatMatch(authorsFilter);
		if (list.size() > 0) {
			
			for (int i = 0; i < list.size(); i++) {
				org.htmlparser.Tag tag = (org.htmlparser.Tag) list.elementAt(i);
				
				org.htmlparser.Tag href = (org.htmlparser.Tag) tag.getFirstChild();
				String id = href.getAttribute("href");
				id = id.replace(URL_PREFIX, "");

				String sectionId = id.split("/")[0];
				final String tagName = href.toPlainTextString();
				
				if (sectionId.equals("profile")) {
					article.addTag(new Tag(tagName, id, null, "contributor"));

				} else {
					Section section = sections.get(sectionId);
					article.addTag(new Tag(tagName, id, section, "keyword"));
				}
			}
		}		
	}
	
	
	private void setSectionFromDCSubject(DCModule dcModule, Article article, Map<String, Section> sections) {
		String sectionName = htmlCleaner.stripHtml(dcModule.getSubject().getValue());
		log.debug("Looking for article section of name: " + sectionName);
		article.setSection(getSectionByName(sections, sectionName));
	}
	
	
	private Section getSectionByName(Map<String, Section> sections, String sectionName) {
		if (sections == null) {
			log.warn("Could not resolve section as sections map is null");
			return null;
		}
		
		for (String sectionId : sections.keySet()) {
			Section section = sections.get(sectionId);
			if (section.getName().equals(sectionName)) {
				log.debug("Found section: " + section.getName() + " (" + section.getId() + ")");
				return section;
			}
		}
		log.warn("Article has an unknown section name: " + sectionName);
		return null;
	}
	
}
