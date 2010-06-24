package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nz.gen.wellington.guardian.contentapiproxy.datasources.FreeTierContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.MediaElement;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;
import nz.gen.wellington.guardian.contentapiproxy.servlets.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class RssDataSource implements GuardianDataSource {

	private static final String API_HOST = "http://www.guardian.co.uk";
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private CachingHttpFetcher httpFetcher;
	private RssEntryToArticleConvertor rssEntryConvertor;
	private ArticleSectionSorter articleSectionSorter;
	private FreeTierContentApi freeTierContentApi;
	
	Logger log = Logger.getLogger(RssDataSource.class);

	
	@Inject
	public RssDataSource(CachingHttpFetcher httpFetcher, RssEntryToArticleConvertor rssEntryConvertor, ArticleSectionSorter articleSectionSorter, FreeTierContentApi freeTierContentApi) {
		this.httpFetcher = httpFetcher;
		this.rssEntryConvertor = rssEntryConvertor;
		this.articleSectionSorter = articleSectionSorter;
		this.freeTierContentApi = freeTierContentApi;
	}
	

	@Override
	public String getContent(SearchQuery query) {
		String callUrl = buildApiSearchQueryUrl(query);
		log.info("Fetching articles from: " + callUrl);
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		
		if (content != null) {		
			StringReader reader = new StringReader(content);
		
			try {
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(reader);
				
				Map<String, Section> sections = freeTierContentApi.getSections();
				List<Article> articles = new ArrayList<Article>();
				List entries = feed.getEntries();
				log.info("Found " + entries.size() + " content items");
				for (int i = 0; i < entries.size(); i++) {
					SyndEntry item = (SyndEntry) entries.get(i);
					Article article = rssEntryConvertor.entryToArticle(item, sections);
					if (article != null) {
						articles.add(article);
					}
				}
				
				articles = articleSectionSorter.sort(articles);
				//if (query.getPageSize() < articles.size()) {
				//	articles = articles.subList(0, query.getPageSize());
				//}
				return articlesToXml(articles);
								
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage());
			} catch (FeedException e) {
				log.error(e.getMessage());
			} catch (XMLStreamException e) {
				log.error(e.getMessage());
			}			
			
		}
		return null;
	}

	
	private String articlesToXml(List<Article> articles) throws XMLStreamException {
		if (articles == null) {
			return null;
		}
		
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		StringWriter output = new StringWriter();
		XMLStreamWriter writer = xof.createXMLStreamWriter(output);

		writer.writeStartElement("response");
		writer.writeStartElement("results");

		for (Article article : articles) {
			if (article != null) {
				articleToXml(writer, article);
			}
		}
				
		writer.writeEndElement();
		writer.writeEndElement();
		writer.close();

		return output.toString();
	}

	
	private void articleToXml(XMLStreamWriter writer, Article article) throws XMLStreamException {
		if (article.getSection() == null) {
			log.warn("Article has no section: " + article.getTitle());
			return;
		}
		 writer.writeStartElement("content");		 
         writer.writeAttribute("id", article.getId());         		 
		 writer.writeAttribute("section-id", article.getSection().getId());
		 writer.writeAttribute("web-publication-date", article.getPubDate().toString(DATE_TIME_FORMAT));

		 writer.writeStartElement("fields");
		 writeFieldElement(writer, "headline", article.getTitle());
		 writeFieldElement(writer, "byline", article.getByline());
		 writeFieldElement(writer, "standfirst", article.getStandfirst());

		 if (article.getThumbnailUrl() != null) {
			 writeFieldElement(writer, "thumbnail", article.getThumbnailUrl());
		 }
		 
		 writeFieldElement(writer, "body", article.getDescription());
		 writer.writeEndElement();
		 
		 writer.writeStartElement("tags");
		 for (Tag tag : article.getTags()) {
			 writer.writeStartElement("tag");
				writer.writeAttribute("id", tag.getId());
				writer.writeAttribute("type", tag.getType());
				writer.writeAttribute("web-title", tag.getName());
				if (tag.getSection() != null) {
					writer.writeAttribute("section-id", tag.getSection().getId());
					writer.writeAttribute("section-name", tag.getSection().getName());
				}
			 writer.writeEndElement();			 
		 }
		 writer.writeEndElement();
		 
		 if (!article.getMediaElements().isEmpty()) {
			 writer.writeStartElement("mediaAssets");
			 for (MediaElement mediaElement : article.getMediaElements()) {
				 writer.writeStartElement("asset");
				 writer.writeAttribute("type", mediaElement.getType());
				 writer.writeAttribute("file", mediaElement.getFile());
				 writer.writeEndElement();
			 }			 
			 writer.writeEndElement();
		 }
		 		 
         writer.writeEndElement();
	}

	private void writeFieldElement(XMLStreamWriter writer, String fieldname, String value) throws XMLStreamException {
		writer.writeStartElement("field");
		writer.writeAttribute("name", fieldname);
		writer.writeCharacters(value);
		writer.writeEndElement();
	}

	
	@Override
	public String getQueryCacheKey(SearchQuery query) {
		return buildApiSearchQueryUrl(query);
	}

		
	private String buildApiSearchQueryUrl(SearchQuery query) {
		StringBuilder queryUrl = new StringBuilder(API_HOST);
		if (query.getSection() != null) {
			queryUrl.append("/" + query.getSection());
		}
		if (query.getTag() != null) {
			queryUrl.append("/" + query.getTag());
		}
		queryUrl.append("/rss");
		return queryUrl.toString();
	}
	
}
