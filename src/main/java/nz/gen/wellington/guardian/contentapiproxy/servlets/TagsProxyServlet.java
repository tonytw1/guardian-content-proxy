package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApiUrlBuilder;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class TagsProxyServlet extends UrlBasedCachedRequest {
	
	private CachingHttpFetcher httpFetcher;
	
	@Inject
	public TagsProxyServlet(CachingHttpFetcher httpFetcher) {
		super();
		this.httpFetcher = httpFetcher;
	}
	
	protected String getContent(HttpServletRequest request) {
		final String queryUrl = ContentApiUrlBuilder.API_HOST + request.getRequestURI() + "?" + request.getQueryString();
		log.info("Tag query url is: " + queryUrl);
		return httpFetcher.fetchContent(queryUrl, "UTF-8");
	}
	
}