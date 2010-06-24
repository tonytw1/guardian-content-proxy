package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonToXmlTranscoder {
	
	
	public String jsonToXml(JSONObject json) {
		try {
			XMLOutputFactory xof = XMLOutputFactory.newInstance();

			StringWriter output = new StringWriter();

			JSONObject response;
			response = json.getJSONObject("response");
			XMLStreamWriter writer = xof.createXMLStreamWriter(output);
			writer.writeStartElement("response");

			JSONArray results = response.getJSONArray("results");
			writer.writeStartElement("results");
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				writeContentElement(writer, result);
			}
			writer.writeEndElement();
			writer.writeEndElement();
			writer.close();

			return output.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	private void writeContentElement(XMLStreamWriter writer, JSONObject result) throws XMLStreamException, JSONException {
         writer.writeStartElement("content");
         writer.writeAttribute("id", result.getString("id"));
         writer.writeAttribute("section-id", result.getString("sectionId"));
         writer.writeAttribute("web-publication-date", result.getString("webPublicationDate"));

         if (result.has("fields")) {
	         JSONObject fields = result.getJSONObject("fields");
	         writer.writeStartElement("fields");
	         addFieldIfPresent(writer, fields, "headline");
	         addFieldIfPresent(writer, fields, "body");
	         addFieldIfPresent(writer, fields, "byline");
	         addFieldIfPresent(writer, fields, "standfirst");
	         addFieldIfPresent(writer, fields, "thumbnail");
	         addFieldIfPresent(writer, fields, "sectionId");
	         addFieldIfPresent(writer, fields, "sectionName"); 
	         writer.writeEndElement();
         }
         
         if (result.has("tags")) {
        	 JSONArray tags = result.getJSONArray("tags");
        	 writer.writeStartElement("tags");
	         for (int i = 0; i < tags.length(); i++) {
				JSONObject tag = tags.getJSONObject(i);
				 writer.writeStartElement("tag");
				 addAttributeIfPresent(writer, tag, "id", "id");
				 addAttributeIfPresent(writer, tag, "type", "type");
				 addAttributeIfPresent(writer, tag, "web-title", "webTitle");
				 addAttributeIfPresent(writer, tag, "section-id", "sectionId");
				 addAttributeIfPresent(writer, tag, "section-name", "sectionName");
		         writer.writeEndElement();
	         }
	         writer.writeEndElement();
         }
         
         if (result.has("mediaAssets")) {
        	 writer.writeStartElement("media-assets");

        	 JSONArray assets = result.getJSONArray("mediaAssets");
        	  for (int i = 0; i < assets.length(); i++) {
        		  JSONObject asset = assets.getJSONObject(i);
        		  writer.writeStartElement("asset");
        		  writer.writeAttribute("type", asset.getString("type"));
        		  writer.writeAttribute("index", asset.getString("index"));
        		  writer.writeAttribute("file", asset.getString("file"));
        		  writer.writeEndElement();        		  
        	  }
        	  writer.writeEndElement();  				
         }

         writer.writeEndElement();
 }

	
	
	private void addFieldIfPresent(XMLStreamWriter writer, JSONObject fields, String fieldname) throws XMLStreamException, JSONException {
        if (fields.has(fieldname)) {
                writer.writeStartElement("field");
                writer.writeAttribute("name", fieldname);
                writer.writeCharacters(fields.getString(fieldname));
                writer.writeEndElement();
        }
	}


	private void addAttributeIfPresent(XMLStreamWriter writer, JSONObject tag, String attributeName, String fieldname) throws XMLStreamException, JSONException {
        if (tag.has(fieldname)) {
        	writer.writeAttribute(attributeName, tag.getString(fieldname));              
        }
	}

}
