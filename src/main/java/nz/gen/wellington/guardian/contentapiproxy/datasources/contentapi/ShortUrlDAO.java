package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import nz.gen.wellington.guardian.contentapiproxy.datasources.FreeTierContentApi;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

// TODO prefix cache ids
public class ShortUrlDAO {

	private static final int SHORT_URL_TTL = 60 * 60 * 24;
	private static final int SHORT_URL_MISS_TTL = 60;

	private final Logger log = Logger.getLogger(ShortUrlDAO.class);
	private MemcacheService cache;
	private FreeTierContentApi contentApi;
	
	@Inject
	public ShortUrlDAO(FreeTierContentApi freeTierContentApi) {
		this.contentApi = freeTierContentApi;
		cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	public void storeShortUrl(String contentId, String shortUrl) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_TTL);
		cache.put(contentId, shortUrl, expiration);
		log.info("Cached short url: " + contentId + " -> " + shortUrl);
	}

	public String getShortUrlFor(String contentId) {
		if (cache.contains(contentId)) {
			final String shortUrl = (String) cache.get(contentId);
			if (shortUrl != null) {
				return shortUrl;
			}
		}
		
		final String shortUrl = contentApi.getShortUrlFor(contentId);
		if (shortUrl != null) {
			storeShortUrl(contentId, shortUrl);
			return shortUrl;
			
		} else {
			cacheFailedLookup(contentId);
		}
		return null;
	}
	
	private void cacheFailedLookup(String contentId) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_MISS_TTL);
		cache.put(contentId, null, expiration);
		log.info("Cached miss for content id: " + contentId);
	}

}
