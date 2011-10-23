package nz.gen.wellington.guardian.contentapiproxy.datasources;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ContentApiKeyPool {
	
	private final Logger log = Logger.getLogger(ContentApiKeyPool.class);
	
	private static final String CACHE_KEY_PREFIX = "expired-api-key:";
	private static final int DEFAULT_TTL = 60 * 60;
	
	private Cache cache;
	private String apiKey;
	
	@Inject
	public ContentApiKeyPool(Cache cache, @Named("apiKey") String apiKey) {
		this.cache = cache;
		this.apiKey = apiKey;
	}
	
	public String getAvailableApiKey() {	
		final String availableApiKey = apiKey;
		if (cache.get(CACHE_KEY_PREFIX + availableApiKey) != null) {
			log.info("Api key is marked as over rate; returning null: " + availableApiKey);
			return null;
		}
		return availableApiKey;		
	}
	
	public void markKeyAsBeenOverRate(String apiKey) {
		cache.put(CACHE_KEY_PREFIX + apiKey, "1", DEFAULT_TTL);
	}
	
}
