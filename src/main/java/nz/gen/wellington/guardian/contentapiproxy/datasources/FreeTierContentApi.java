package nz.gen.wellington.guardian.contentapiproxy.datasources;

import com.google.inject.Inject;

import nz.gen.wellington.guardian.contentapi.parsing.ContentApiStyleJSONParser;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

// TODO This probably isn't very free at all as implemented - needs an empty key pool jammed in it or something...
public class FreeTierContentApi extends ContentApi {

	@Inject
	public FreeTierContentApi(CachingHttpFetcher httpFetcher, ContentApiStyleJSONParser contentApiJsonParser, ContentApiKeyPool contentApiKeyPool, ShortUrlDAO shortUrlDAO) {
		super(httpFetcher, contentApiJsonParser, contentApiKeyPool, shortUrlDAO);
	}
	
}
