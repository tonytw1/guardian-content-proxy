package nz.gen.wellington.guardian.contentapiproxy.datasources;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ContentApiKeyPool {
	
	private final Logger log = Logger.getLogger(ContentApiKeyPool.class);
	
	private static final String CACHE_KEY_PREFIX = "expired-api-key:";
	public static final String API_KEY = "";	
	private static final int DEFAULT_TTL = 60 * 60;
	
	private Cache cache;
	
	@Inject
	public ContentApiKeyPool(Cache cache) {
		this.cache = cache;
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
		cache.put(CACHE_KEY_PREFIX + apiKey, "1", DEFAULT_TTL);
	}
	
}
