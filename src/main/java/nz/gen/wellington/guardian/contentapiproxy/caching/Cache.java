package nz.gen.wellington.guardian.contentapiproxy.caching;

import java.io.IOException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class Cache {

	private final Logger log = Logger.getLogger(Cache.class);
	
	private static final String KEY_PREFIX = "GUARDIANLITE5:";	// TODO hardcoded version number
	
	private MemcachedClient memcachedClient;
		
	@Inject
	public Cache() {
	}
	
	public void put(String url, String content, int ttl) {
		try {
			getClient().set(makeCachekKey(url), ttl, content);
		} catch (IOException e) {
			log.error(e);
		}
	}

	public String get(String url) {
		try {
			return (String) getClient().get(makeCachekKey(url));
		} catch (IOException e) {
			log.error(e);
		} 
		return null;
	}
	
	@Deprecated 	// TODO Not atomic - calling code should be doing this
	public boolean contains(String key) {
		return get(key) != null;
	}
	
	private String makeCachekKey(String url) {
		String key = KEY_PREFIX + DigestUtils.shaHex(url);
		log.info(url + " key is: " + key);
		return key;
	}
	
	private MemcachedClient getClient() throws IOException {
		if (memcachedClient == null) {
			memcachedClient= new MemcachedClient( AddrUtil.getAddresses("localhost:11211"));
		}
		return memcachedClient;
	}

}
