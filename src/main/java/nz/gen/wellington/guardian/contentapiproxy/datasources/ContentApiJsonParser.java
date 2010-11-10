package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.Map;
import java.util.TreeMap;

import nz.gen.wellington.guardian.contentapiproxy.model.Section;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContentApiJsonParser {
		
	public boolean isResponseOk(JSONObject json) {
		try {
			JSONObject response = json.getJSONObject("response");
			String status = response.getString("status");
			return status != null && status.equals("ok");
		} catch (JSONException e) {
			return false;
		}
	}

	public Map<String, Section> extractSections(JSONObject json) throws JSONException {
		JSONObject jsonResponse = json.getJSONObject("response");
		JSONArray results = jsonResponse.getJSONArray("results");		
		Map<String, Section> sections = new TreeMap<String, Section>();
		for (int i = 0; i < results.length(); i++) {
			JSONObject section = results.getJSONObject(i);												
			Section loadedSection = new Section(section.getString("id"), section.getString("webTitle"));
			sections.put(loadedSection.getId(), loadedSection);
		}
		return sections;
	}

}
