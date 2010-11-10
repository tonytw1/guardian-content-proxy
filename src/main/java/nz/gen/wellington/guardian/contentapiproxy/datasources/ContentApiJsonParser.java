package nz.gen.wellington.guardian.contentapiproxy.datasources;

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

}
