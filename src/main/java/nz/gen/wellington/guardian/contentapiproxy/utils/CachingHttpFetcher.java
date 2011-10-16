package nz.gen.wellington.guardian.contentapiproxy.utils;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class CachingHttpFetcher extends HttpFetcher {
	
	private final Logger log = Logger.getLogger(CachingHttpFetcher.class);
	
	private static final int DEFAULT_TTL = 600;
	private Cache cache;
	
	@Inject
	public CachingHttpFetcher(Cache cache) {
		this.cache = cache;
	}
	
	public String fetchContent(String url, String charEncoding) throws HttpForbiddenException {
		log.info("Called for url '" + url);

		final String content = fetchFromCache(url);
		if (content != null) {
			log.info("Found content for url '" + url + "' in cache");
			return content;
		}
		
		log.info("Attempting to live fetch url: " + url);
		final String fetchedContent = super.fetchContent(url, charEncoding);

		if (fetchedContent != null) {
			cache.put(url, fetchedContent, DEFAULT_TTL);
			log.info("Cached url: " + url);
		}
		return fetchedContent;		
	}
	
    public String fetchFromCache(String url) {
    	log.debug("Looking in cache for url '" + url);
    	return (String) cache.get(url);    	
    }
    
}
