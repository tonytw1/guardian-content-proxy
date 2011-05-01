package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import static org.junit.Assert.assertEquals;
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
import nz.gen.wellington.guardian.model.Section;

import org.junit.Before;
import org.junit.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

public class RssEntryToArticleConvertorTest {
	
	private SyndFeed feed;
	private RssEntryToArticleConvertor convertor;
	private Map<String, Section> sections;
	
	@Before
	public void setUp() throws Exception {
		String content = loadContent("rss.htm").toString();
		
		SyndFeedInput input = new SyndFeedInput();
		feed = input.build(new StringReader(content));
		convertor = new RssEntryToArticleConvertor(new HtmlCleaner());
		sections = new HashMap<String, Section>();
		sections.put("poltics", new Section("politics", "Politics"));
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
