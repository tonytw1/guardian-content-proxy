package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public abstract class CacheAwareProxyServlet extends HttpServlet {
	
	static Logger log = Logger.getLogger(CacheAwareProxyServlet.class);
	
	private static final long serialVersionUID = 1320784761324501050L;
	private static final int OUTGOING_TTL = 300;
	
	private MemcacheService cache;
		
	public CacheAwareProxyServlet() {
		super();
		this.cache = MemcacheServiceFactory.getMemcacheService();
	}


	protected void cacheContent(String queryCacheKey, String content) {
		log.info("Caching results for call: " + queryCacheKey);		
		try {
			cache.put(queryCacheKey, content, Expiration.byDeltaSeconds(OUTGOING_TTL));
		} catch (Exception e) {
			log.warn("Failed to cache content for: " + queryCacheKey, e);
		}
	}
	
	
	protected String cacheGet(String queryCacheKey) {
		log.info("Getting Cached results for call: " + queryCacheKey);
		return (String) cache.get(queryCacheKey);
	}
	
	
	protected final String getQueryCacheKey(HttpServletRequest request) {
		StringBuilder cacheKey = new StringBuilder(request.getRequestURI());
		if (request.getQueryString() != null) {
			cacheKey.append(request.getQueryString());
		}
		log.debug("Cache key is: " + cacheKey.toString());
		return cacheKey.toString();
	}
	
}
