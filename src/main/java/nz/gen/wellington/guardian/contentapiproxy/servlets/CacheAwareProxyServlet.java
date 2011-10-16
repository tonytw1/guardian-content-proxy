package nz.gen.wellington.guardian.contentapiproxy.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public abstract class CacheAwareProxyServlet extends HttpServlet {
	
	static Logger log = Logger.getLogger(CacheAwareProxyServlet.class);
	
	private static final long serialVersionUID = 1320784761324501050L;
	private static final int OUTGOING_TTL = 300;
	
	private Cache cache;
	
	@Inject
	public CacheAwareProxyServlet(Cache cache) {
		super();
		this.cache = cache;
	}

	protected void cacheContent(String queryCacheKey, String content) {
		log.info("Caching results for call: " + queryCacheKey);		
		try {
			cache.put(queryCacheKey, content, OUTGOING_TTL);			
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
