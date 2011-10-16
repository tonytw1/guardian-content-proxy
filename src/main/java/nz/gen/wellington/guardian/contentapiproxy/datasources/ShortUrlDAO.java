package nz.gen.wellington.guardian.contentapiproxy.datasources;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ShortUrlDAO {

	private static final int SHORT_URL_TTL = 60 * 60 * 24 * 30;
	private static final String CACHE_PREFIX = "SHORTURL:";

	private final Logger log = Logger.getLogger(ShortUrlDAO.class);
	private Cache cache;
	
	@Inject
	public ShortUrlDAO(Cache cache) {
		this.cache = cache;
	}
	
	public void storeShortUrl(String contentId, String shortUrl) {
		cache.put(getCacheKeyFor(contentId), shortUrl, SHORT_URL_TTL);
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
