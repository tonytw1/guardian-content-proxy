package nz.gen.wellington.guardian.contentapiproxy.output;

import java.util.Map;

import nz.gen.wellington.guardian.model.Section;

import org.json.JSONArray;
import org.json.JSONObject;

public class SectionsToJSONRenderer {

	public String outputJSON(Map<String, Section> sections) {
		try {
		
			JSONObject json = new JSONObject();
			
			JSONArray results = new JSONArray();
			for (String sectionId : sections.keySet()) {
				JSONObject section = new JSONObject();
				section.put("id", sectionId);
				section.put("webTitle", sections.get(sectionId).getName());
				results.put(section);
			}
			
			JSONObject response = new JSONObject();
			response.put("status", "ok");
			response.put("results", results);
			
			json.put("response", response);
			return json.toString();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

}
