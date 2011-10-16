package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapi.urls.ContentApiStyleUrlBuilder;
import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApiKeyPool;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class TagsProxyServlet extends UrlBasedCachedRequest {
	
	static Logger log = Logger.getLogger(TagsProxyServlet.class);

	private CachingHttpFetcher httpFetcher;
	private ContentApiKeyPool contentApiKeyPool;
	
	@Inject
	public TagsProxyServlet(Cache cache, CachingHttpFetcher httpFetcher, ContentApiKeyPool contentApiKeyPool) {
		super(cache);
		this.httpFetcher = httpFetcher;
		this.contentApiKeyPool = contentApiKeyPool;
	}
	
	protected String getContent(HttpServletRequest request) {
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(ContentApi.API_HOST, contentApiKeyPool.getAvailableApiKey());
		urlBuilder.setSearchTerm(request.getParameter("q"));
		urlBuilder.setFormat("json");

		final String queryUrl = urlBuilder.toTagSearchQueryUrl();
		log.info("Tag query url is: " + queryUrl);
		try {
			return httpFetcher.fetchContent(queryUrl, "UTF-8");
		} catch (HttpForbiddenException e) {
			return null;
		}
	}
	
}