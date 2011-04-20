package nz.gen.wellington.guardian.contentapiproxy.datasources;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

public class ShortUrlDAO {

	private static final int SHORT_URL_TTL = 60 * 60 * 24 * 30;
	private static final String CACHE_PREFIX = "SHORTURL:";

	private final Logger log = Logger.getLogger(ShortUrlDAO.class);
	private MemcacheService cache;
	
	@Inject
	public ShortUrlDAO() {
		cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	public void storeShortUrl(String contentId, String shortUrl) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_TTL);
		cache.put(getCacheKeyFor(contentId), shortUrl, expiration);
		log.info("Cached short url: " + contentId + " -> " + shortUrl);
	}


	public String getShortUrlFor(String contentId) {
		String shortUrl = (String) cache.get(getCacheKeyFor(contentId));
		if (cache.contains(getCacheKeyFor(contentId))) {
			return shortUrl;
		}
		return null;
	}
	
	private String getCacheKeyFor(String contentId) {
		return CACHE_PREFIX + contentId;
	}
	
}
