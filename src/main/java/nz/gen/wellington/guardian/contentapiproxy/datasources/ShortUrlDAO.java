package nz.gen.wellington.guardian.contentapiproxy.datasources;

import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

public class ShortUrlDAO {

	private static final int SHORT_URL_TTL = 60 * 60 * 24 * 30;
	private static final int SHORT_URL_MISS_TTL = 60 * 60;
	private static final String CACHE_PREFIX = "SHORTURL:";

	private final Logger log = Logger.getLogger(ShortUrlDAO.class);
	private MemcacheService cache;
	private FreeTierContentApi freeTierContentApi;
	
	@Inject
	public ShortUrlDAO(FreeTierContentApi freeTierContentApi) {
		this.freeTierContentApi = freeTierContentApi;
		cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	public void storeShortUrl(String contentId, String shortUrl) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_TTL);
		cache.put(getCacheKeyFor(contentId), shortUrl, expiration);
		log.info("Cached short url: " + contentId + " -> " + shortUrl);
	}


	public String getShortUrlFor(String contentId) throws HttpForbiddenException {
		String shortUrl = (String) cache.get(getCacheKeyFor(contentId));
		if (cache.contains(getCacheKeyFor(contentId))) {
			return shortUrl;
		}
		
		shortUrl = freeTierContentApi.getShortUrlFor(contentId);
		if (shortUrl != null) {
			storeShortUrl(contentId, shortUrl);
			return shortUrl;			
		} else {
			cacheFailedLookup(contentId);
		}
		return null;
	}
	
	private String getCacheKeyFor(String contentId) {
		return CACHE_PREFIX + contentId;
	}
	
	private void cacheFailedLookup(String contentId) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_MISS_TTL);
		cache.put(getCacheKeyFor(contentId), null, expiration);
		log.info("Cached miss for content id: " + contentId);
	}

}
