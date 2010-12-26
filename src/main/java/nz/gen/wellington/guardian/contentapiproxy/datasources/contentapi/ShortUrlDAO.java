package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

public class ShortUrlDAO {

	private static final int SHORT_URL_TTL = 60 * 60 * 24;

	private final Logger log = Logger.getLogger(ShortUrlDAO.class);
	private MemcacheService cache;
	
	@Inject	
	public ShortUrlDAO() {
		cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	public void storeShortUrl(String contentId, String shortUrl) {
		Expiration expiration = Expiration.byDeltaSeconds(SHORT_URL_TTL);
		cache.put(contentId, shortUrl, expiration);
		log.info("Cached short url: " + contentId + " -> " + shortUrl);
	}

	public String getShortUrlFor(String contentId) {
		return (String) cache.get(contentId);  
	}

}
