package nz.gen.wellington.guardian.contentapiproxy.output;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nz.gen.wellington.guardian.contentapiproxy.utils.ContentChecksumCalculator;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.MediaElement;
import nz.gen.wellington.guardian.model.Refinement;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;

public class ArticleToXmlRenderer {
	
	private static Logger log = Logger.getLogger(ArticleToXmlRenderer.class);

	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	private ContentChecksumCalculator contentCheckSumCalculator;
	private String serverName;
	
	@Inject
	public ArticleToXmlRenderer(ContentChecksumCalculator contentCheckSumCalculator) {
		this.contentCheckSumCalculator = contentCheckSumCalculator;
		this.serverName = "4.guardian-lite.appspot.com";	// TODO how can you inject this?
	}
	
	public String outputXml(List<Article> articles, String description, Map<String, List<Refinement>> refinements, boolean showAllFields) {
		
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		StringWriter output = new StringWriter();
		XMLStreamWriter writer;
		try {
			writer = xof.createXMLStreamWriter(output);
			writer.writeStartElement("response");
			
			writer.writeStartElement("results");
			writer.writeAttribute("checksum", contentCheckSumCalculator.calculateChecksum(articles));
			if (description != null && showAllFields) {
				writer.writeAttribute("description", description);
			}
			if (showAllFields) {
				for (Article article : articles) {
					if (article != null) {
						articleToXml(writer, article);
					}
				}				
			}
			writer.writeEndElement();
			
			if (showAllFields) {
				writeRefinements(refinements, writer);
			}
				
			writer.writeEndElement();
			writer.close();
			return output.toString();
			
		} catch (XMLStreamException e) {
			log.error(e.getMessage());
		}
		return null;
	}


	private void writeRefinements(Map<String, List<Refinement>> refinements, XMLStreamWriter writer) throws XMLStreamException {
		if (refinements != null && !refinements.isEmpty()) {
			writer.writeStartElement("refinement-groups");
								
			for (String refinementType : refinements.keySet()) {					
				writer.writeStartElement("refinement-group");
				writer.writeAttribute("type", refinementType);
		
				writer.writeStartElement("refinements");
				for (Refinement refinement : refinements.get(refinementType)) {
					writer.writeStartElement("refinement");
					writer.writeAttribute("id", refinement.getId());
					writer.writeAttribute("type", refinement.getType());
					writer.writeAttribute("display-name", refinement.getDisplayName());
					writer.writeAttribute("refined-url", refinement.getRefinedUrl());						
					writer.writeEndElement();
				}
				writer.writeEndElement();
				writer.writeEndElement();
			}			
			writer.writeEndElement();						
		}
	}


	private void articleToXml(XMLStreamWriter writer, Article article) throws XMLStreamException {
		 writer.writeStartElement("content");
		 if (article.getId() != null) writer.writeAttribute("id", article.getId());
		 if (article.getWebUrl() != null) writer.writeAttribute("web-url", article.getWebUrl());

		 if (article.getSection() != null) writer.writeAttribute("section-id", article.getSection().getId());		 
		 if (article.getPubDate() != null) {
			 writer.writeAttribute("web-publication-date", new DateTime(article.getPubDate()).toString(DATE_TIME_FORMAT));
		 }
		 writer.writeStartElement("fields");
		 if (article.getShortUrl() != null) {
			 writeFieldElement(writer, "short-url", article.getShortUrl());
		 }
		 writeFieldElement(writer, "headline", article.getHeadline());
		 if (article.getByline() != null) writeFieldElement(writer, "byline", article.getByline());
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
				 writeMediaElement(writer, mediaElement);
			 }			 
			 writer.writeEndElement();
		 }
		 		 
         writer.writeEndElement();
	}


	private void writeMediaElement(XMLStreamWriter writer, MediaElement mediaElement) throws XMLStreamException {
		writer.writeStartElement("asset");
		writer.writeAttribute("type", mediaElement.getType());
		writer.writeAttribute("file", mediaElement.getFile());
		 
		writer.writeStartElement("fields");
		if (mediaElement.getWidth() != null) {
			writeFieldElement(writer, "width", mediaElement.getWidth().toString());
		}
		if (mediaElement.getHeight() != null) {
			writeFieldElement(writer, "height", mediaElement.getHeight().toString());
		}
		writeFieldElement(writer, "caption", mediaElement.getCaption());
		writeFieldElement(writer, "thumbnail", "http://" + serverName + "/thumb?file=" + encodeValue(mediaElement.getFile()));		
		writer.writeEndElement();
		
		writer.writeEndElement();
	}


	private String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}

	private void writeFieldElement(XMLStreamWriter writer, String fieldname, String value) throws XMLStreamException {
		writer.writeStartElement("field");
		writer.writeAttribute("name", fieldname);
		writer.writeCharacters(value);
		writer.writeEndElement();
	}

}
