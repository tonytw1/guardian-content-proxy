package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.MediaElement;
import nz.gen.wellington.guardian.contentapiproxy.model.Tag;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ArticleToXmlRenderer {
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	Logger log = Logger.getLogger(ArticleToXmlRenderer.class);

	private ContentChecksumCalculator contentCheckSumCalculator;
	
	@Inject
	public ArticleToXmlRenderer(ContentChecksumCalculator contentCheckSumCalculator) {
		this.contentCheckSumCalculator = contentCheckSumCalculator;
	}


	public String outputXml(List<Article> articles, String description, Map<String, List<Tag>> refinements, boolean showAllFields) {
		if (articles == null) {
			return null;
		}
		
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
				if (refinements != null && !refinements.isEmpty()) {
					writer.writeStartElement("refinement-groups");
										
					for (String refinementType : refinements.keySet()) {					
						writer.writeStartElement("refinement-group");
						writer.writeAttribute("type", refinementType);
				
						writer.writeStartElement("refinements");
						for (Tag tag : refinements.get(refinementType)) {
							writer.writeStartElement("refinement");
							writer.writeAttribute("id", tag.getId());
							writer.writeAttribute("display-name", tag.getName());
							//writer.writeAttribute("section-id", tag.getSection().getId());
							writer.writeEndElement();
						}
						writer.writeEndElement();
						writer.writeEndElement();
					}
					
					writer.writeEndElement();						
				}
			}
				
			writer.writeEndElement();
			writer.close();
			return output.toString();
			
		} catch (XMLStreamException e) {
			log.error(e.getMessage());
		}
		return null;
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
				 
				 writer.writeStartElement("fields");
				 writeFieldElement(writer, "caption", mediaElement.getCaption());
				 writer.writeEndElement();

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

}
