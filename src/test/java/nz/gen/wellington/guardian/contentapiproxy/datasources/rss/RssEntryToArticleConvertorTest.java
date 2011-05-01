package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import nz.gen.wellington.guardian.contentapi.cleaning.HtmlCleaner;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.MediaElement;
import nz.gen.wellington.guardian.model.Section;

import org.junit.Before;
import org.junit.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

public class RssEntryToArticleConvertorTest {
	
	private RssEntryToArticleConvertor convertor;
	private Map<String, Section> sections;
	
	private SyndFeed feed;
	private SyndFeed galleryFeed;
	
	@Before
	public void setUp() throws Exception {
		convertor = new RssEntryToArticleConvertor(new HtmlCleaner());
		sections = new HashMap<String, Section>();
		sections.put("poltics", new Section("politics", "Politics"));
		sections.put("media", new Section("media", "Media"));
		sections.put("tv-and-radio", new Section("tv-and-radio", "Television & radio"));
		
		String content = loadContent("rss.htm").toString();
		SyndFeedInput input = new SyndFeedInput();
		feed = input.build(new StringReader(content));
		
		content = loadContent("gallery-rss.htm").toString();
		input = new SyndFeedInput();
		galleryFeed = input.build(new StringReader(content));				
	}
	
	@Test
	public void shouldExtractAllFieldsCorrectly() throws Exception {
		SyndEntry firstEntry = (SyndEntry) feed.getEntries().get(0);		
		Article article = convertor.entryToArticle(firstEntry, sections);
		
		assertEquals("Cameron defends Osborne's budget 'to protect the poor'", article.getHeadline());
		assertEquals("politics", article.getSection().getId());
		assertEquals("Andrew Sparrow", article.getByline());
		assertEquals("All the news from Westminster including minute-by-minute coverage of PMQs and all the latest reaction to the budget\n\nRead a summary of events so far", article.getStandfirst());
		assertEquals(7, article.getTags().size());				
		// TODO This assert suffers from an BST problem which would be nice to work out an answer to.
		//assertEquals(new DateTime(2010, 6, 23, 7, 3, 39, 0), article.getPubDate());	
	}

	@Test
	public void articleThumbnailUrlShouldBeSetToFirstThumbnailSizedMediaElementsUrl() throws Exception {
		SyndEntry firstEntry = (SyndEntry) feed.getEntries().get(0);		
		Article article = convertor.entryToArticle(firstEntry, sections);	
		assertEquals("http://static.guim.co.uk/sys-images/Politics/Pix/pictures/2010/6/22/1277210802573/Budget-2010-George-Osborn-002.jpg", article.getThumbnail());
	}
	
	@Test
	public void galleryThumbnailShouldBeSetToTheThumbnailUrlOfTheFirstMediaElement() throws Exception {
		SyndEntry firstEntry = (SyndEntry) galleryFeed.getEntries().get(0);		
		Article article = convertor.entryToArticle(firstEntry, sections);
		assertEquals("http://static.guim.co.uk/sys-images/Media/Pix/pictures/2011/4/26/1303818961303/Misfits-001-thumb-338.jpg", article.getThumbnail());
	}
	
	
	@Test
	public void galleryMediaElementsShouldHaveDescriptionsAndThumbnailsCorrectlySet() throws Exception {
		SyndEntry firstEntry = (SyndEntry) galleryFeed.getEntries().get(0);
		assertNotNull(firstEntry);
		
		Article article = convertor.entryToArticle(firstEntry, sections);
		
		MediaElement firstMediaElement = article.getMediaElements().get(0);		
		assertEquals("Misfits lands four Bafta nominations, including supporting actress for Lauren Socha (right) and supporting actor Robert Sheehan (second right)", firstMediaElement.getCaption());		
		assertEquals("http://static.guim.co.uk/sys-images/Media/Pix/pictures/2011/4/26/1303818961303/Misfits-001-thumb-338.jpg", firstMediaElement.getThumbnail());

	}
	
	
	
	
	@Test
	public void testShouldIgnoreNonArticles() throws Exception {
		SyndEntry galleryEntry = (SyndEntry) feed.getEntries().get(11);
		assertNull(convertor.entryToArticle(galleryEntry, sections));
	}
		
	private StringBuffer loadContent(String filename) throws IOException {
        StringBuffer content = new StringBuffer();
        File contentFile = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        Reader freader = new FileReader(contentFile);
        BufferedReader in = new BufferedReader(freader);
        String str;
        while ((str = in.readLine()) != null) {
                content.append(str);
                content.append("\n");
        }
        in.close();
        freader.close();
        return content;
	}

}
