package nz.gen.wellington.guardian.contentapiproxy.utils;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.FeedsPortalUrlResolver;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.GuardianFeedsUrlResolver;

import org.apache.log4j.Logger;

import uk.co.eelpieconsulting.common.shorturls.ShortUrlResolverService;

import com.google.inject.Inject;

public class CachingShortUrlResolver {
	
	private final Logger log = Logger.getLogger(CachingShortUrlResolver.class);
	
	private static final int DEFAULT_TTL = 60 * 60 * 24;

	private ShortUrlResolverService shortUrlResolverService;
	private Cache cache;
	
	@Inject
	public CachingShortUrlResolver(Cache cache) {
		this.cache = cache;
		shortUrlResolverService = new ShortUrlResolverService(new GuardianFeedsUrlResolver(), new FeedsPortalUrlResolver());
	}
	
	public String resolve(String url) {
		log.debug("Called for url '" + url);
		
		final String content = fetchFromCache(url);
		if (content != null) {
			log.debug("Found content for url '" + url + "' in cache");
			return content;
		}
		
		log.debug("Attempting to resolve url: " + url);
		final String fetchedContent = shortUrlResolverService.resolveUrl(url);

		if (fetchedContent != null) {
			cache.put(url, fetchedContent, DEFAULT_TTL);
			log.debug("Cached url: " + url);
		}
		return fetchedContent;		
	}
	
    private String fetchFromCache(String url) {
    	log.debug("Looking in cache for url '" + url);
    	return (String) cache.get(url);    	
    }
    
}
