package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

public class RssEntryToArticleConvertorTest extends TestCase {
	
	private SyndFeed feed;
	private RssEntryToArticleConvertor convertor;
	private Map<String, Section> sections;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String content = loadContent("rss.htm").toString();
		
		SyndFeedInput input = new SyndFeedInput();
		feed = input.build(new StringReader(content));
		convertor = new RssEntryToArticleConvertor();
		sections = new HashMap<String, Section>();
		sections.put("poltics", new Section("politics", "Politics"));
	}
	
	public void testShouldExtractAllFieldsCorrectly() throws Exception {
		SyndEntry firstEntry = (SyndEntry) feed.getEntries().get(0);
		
		Article article = convertor.entryToArticle(firstEntry, sections);
		assertEquals("Cameron defends Osborne's budget 'to protect the poor'", article.getHeadline());
		assertEquals("politics", article.getSection().getId());
		assertEquals("Andrew Sparrow", article.getByline());
		
		assertEquals(7, article.getTags().size());
				
		assertEquals("All the news from Westminster including minute-by-minute coverage of PMQs and all the latest reaction to the budget\n\nRead a summary of events so far", article.getStandfirst());
	//	assertEquals(new DateTime(2010, 6, 23, 7, 3, 39, 0), article.getPubDate());
	}

	
	public void testShouldIgnoreNonArticles() throws Exception {
		SyndEntry galleryEntry = (SyndEntry) feed.getEntries().get(11);
		assertNull(convertor.entryToArticle(galleryEntry, sections));
	}
	
	protected StringBuffer loadContent(String filename) throws IOException {
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
