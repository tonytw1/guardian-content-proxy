package nz.gen.wellington.guardian.contentapiproxy.datasources;

import com.google.inject.Inject;

import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

public class FreeTierContentApi extends ContentApi {

	@Inject
	public FreeTierContentApi(CachingHttpFetcher httpFetcher, ContentApiJsonParser contentApiJsonParser) {
		super(httpFetcher, contentApiJsonParser);
		contentApiUrlBuilder = new ContentApiUrlBuilder(null);
	}
	
}
