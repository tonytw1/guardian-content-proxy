package nz.gen.wellington.guardian.contentapiproxy.caching;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;

public class Cache {

	private static final String KEY_PREFIX = "GUARDIANLITE5:";
	
	private MemcacheService cache;
	
	@Inject
	public Cache() {
		cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	public void put(String url, String content, int ttl) {
		Expiration expiration = Expiration.byDeltaSeconds(ttl);
		cache.put(makeCachekKey(url), content, expiration);
	}

	public String get(String url) {
		return (String) cache.get(makeCachekKey(url)); 
	}
	
	public boolean contains(String key) {
		return cache.contains(makeCachekKey(key));
	}
	
	private String makeCachekKey(String url) {
		return KEY_PREFIX + url;
	}
	
}
