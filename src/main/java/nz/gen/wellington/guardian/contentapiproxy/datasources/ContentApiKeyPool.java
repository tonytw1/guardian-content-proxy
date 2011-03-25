package nz.gen.wellington.guardian.contentapiproxy.datasources;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

public class ContentApiKeyPool {
	
	private final Logger log = Logger.getLogger(ContentApiKeyPool.class);
	
	private static final String CACHE_KEY_PREFIX = "expired-api-key:";
	public static final String API_KEY = "";	
	private static final int DEFAULT_TTL = 60 * 60;
	
	private MemcacheService cache;

	@Inject
	public ContentApiKeyPool() {	
		cache = MemcacheServiceFactory.getMemcacheService();
		cache.clearAll();
	}
	
	public String getAvailableApiKey() {		
		final String availableApiKey = API_KEY;
		if (cache.contains(CACHE_KEY_PREFIX + availableApiKey)) {
			log.info("Api key is marked as over rate; returning null: " + availableApiKey);
			return null;
		}
		return availableApiKey;		
	}
	
	public void markKeyAsBeenOverRate(String apiKey) {
		Expiration expiration = Expiration.byDeltaSeconds(DEFAULT_TTL);
		cache.put(CACHE_KEY_PREFIX + apiKey, 1, expiration);
	}
	
}
