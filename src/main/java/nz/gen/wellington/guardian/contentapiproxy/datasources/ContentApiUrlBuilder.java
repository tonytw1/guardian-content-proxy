package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.joda.time.DateTime;

public class ContentApiUrlBuilder {
	
	private static final String API_HOST = "http://content.guardianapis.com";
	
	public String buildApiSectionsQueryUrl() throws UnsupportedEncodingException {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/sections");
		queryUrl.append("?format=json");
		return queryUrl.toString();
	}

	
	public String buildSectionRefinementQueryUrl(String sectionId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		queryUrl.append("?tag=type%2Farticle");
		queryUrl.append("&section=" + sectionId);
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		queryUrl.append("&from-date=" + new DateTime().minusDays(7).toString("yyyy-MM-dd"));
		return queryUrl.toString();
	}
	
	
	public String buildTagRefinementQueryUrl(String tagId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		try {
			queryUrl.append("?tag=type%2Farticle," + URLEncoder.encode(tagId, "UTF8"));
		} catch (UnsupportedEncodingException e) {			
		}
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		return queryUrl.toString();
	}
		
}
