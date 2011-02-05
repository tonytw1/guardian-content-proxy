package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.joda.time.DateTime;

@Deprecated
public class ContentApiUrlBuilder {
	
	public static final String API_HOST = "http://content.guardianapis.com";
	private String apiKey;
		
	public ContentApiUrlBuilder(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public String buildApiSectionsQueryUrl() throws UnsupportedEncodingException {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/sections");
		queryUrl.append("?format=json");
		appendApiKey(queryUrl);
		return queryUrl.toString();
	}
	
	public String buildApiContentItemUrl(String contentId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/" + contentId);
		queryUrl.append("?format=json");
		queryUrl.append("&show-fields=all");
		appendApiKey(queryUrl);
		return queryUrl.toString();
	}
	
	public String buildSectionRefinementQueryUrl(String sectionId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		queryUrl.append("?tag=type%2Farticle|type%2Fgallery");
		queryUrl.append("&section=" + sectionId);
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		queryUrl.append("&from-date=" + new DateTime().minusDays(7).toString("yyyy-MM-dd"));
		appendApiKey(queryUrl);
		return queryUrl.toString();
	}
	
	
	public String buildTagRefinementQueryUrl(String tagId) {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/search");
		try {
			queryUrl.append("?tag=(type%2Farticle|type=%2Fgallery)," + URLEncoder.encode(tagId, "UTF8"));
		} catch (UnsupportedEncodingException e) {
		}
		queryUrl.append("&page-size=1");
		queryUrl.append("&show-refinements=all");
		queryUrl.append("&format=json");
		appendApiKey(queryUrl);
		return queryUrl.toString();
	}
	
	private void appendApiKey(StringBuilder queryUrl) {
		if (apiKey != null) {
			queryUrl.append("&api-key=" + apiKey);
		}
	}
	
}
