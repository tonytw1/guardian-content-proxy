package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapi.urls.ContentApiStyleUrlBuilder;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApiKeyPool;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import uk.co.eelpieconsulting.common.http.HttpFetchException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TagsProxyServlet extends UrlBasedCachedRequest {
	
	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(TagsProxyServlet.class);

	private CachingHttpFetcher httpFetcher;
	private ContentApiKeyPool contentApiKeyPool;
	
	@Inject
	public TagsProxyServlet(CachingHttpFetcher httpFetcher, ContentApiKeyPool contentApiKeyPool) {
		this.httpFetcher = httpFetcher;
		this.contentApiKeyPool = contentApiKeyPool;
	}
	
	protected String getContent(HttpServletRequest request) {
		final ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(ContentApi.API_HOST, contentApiKeyPool.getAvailableApiKey());
		urlBuilder.setSearchTerm(request.getParameter("q") != null ? request.getParameter("q") : "");
		urlBuilder.setFormat("json");
		
		final String queryUrl = urlBuilder.toTagSearchQueryUrl();
		log.debug("Tag query url is: " + queryUrl);
		try {
			return httpFetcher.fetchContent(queryUrl);
			
		} catch (HttpFetchException e) {
			log.warn("Failed to fetch url; returning null: " + queryUrl);
			return null;
		}
	}
	
}