package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;

public abstract class CacheAwareProxyServlet extends HttpServlet {
	
	Logger log = Logger.getLogger(SearchProxyServlet.class);
	
	private static final long serialVersionUID = 1320784761324501050L;
	private static final int OUTGOING_TTL = 300;
	
	protected MemcacheService cache;
		
	protected void cacheContent(String queryCacheKey, String content) {
		log.info("Caching results for call: " + queryCacheKey);
		cache.put(queryCacheKey, content, Expiration.byDeltaSeconds(OUTGOING_TTL));
	}
	
}
