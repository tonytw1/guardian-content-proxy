package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import uk.co.eelpieconsulting.common.shorturls.AbstractRedirectResolver;

public class GuardianFeedsUrlResolver extends AbstractRedirectResolver {

	@Override
	public boolean isValid(String url) {
		return url.startsWith("http://feeds.guardian.co.uk/") || url.startsWith("http://feeds.theguardian.com");
	}
	
}
