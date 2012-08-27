package nz.gen.wellington.guardian.contentapiproxy.utils;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.log4j.Logger;

import uk.co.eelpieconsulting.common.http.HttpFetchException;
import uk.co.eelpieconsulting.common.http.HttpFetcher;

import com.google.inject.Inject;

public class CachingHttpFetcher extends HttpFetcher {
	
	private final Logger log = Logger.getLogger(CachingHttpFetcher.class);
	
	private static final int DEFAULT_TTL = 600;
	private Cache cache;
	
	@Inject
	public CachingHttpFetcher(Cache cache) {
		super();
		this.cache = cache;
	}
	
	public String fetchContent(String url) throws HttpFetchException {
		log.debug("Called for url '" + url);
		
		final String content = fetchFromCache(url);
		if (content != null) {
			log.debug("Found content for url '" + url + "' in cache");
			return content;
		}
		
		log.debug("Attempting to live fetch url: " + url);
		final String fetchedContent = super.fetchContent(url);

		if (fetchedContent != null) {
			cache.put(url, fetchedContent, DEFAULT_TTL);
			log.debug("Cached url: " + url);
		}
		return fetchedContent;		
	}
	
    public String fetchFromCache(String url) {
    	log.debug("Looking in cache for url '" + url);
    	return (String) cache.get(url);    	
    }
    
}
