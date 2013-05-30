package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import uk.co.eelpieconsulting.common.shorturls.AbstractRedirectResolver;

public class FeedsPortalUrlResolver extends AbstractRedirectResolver {

	@Override
	public boolean isValid(String url) {
		return url.startsWith("http://guardian.co.uk.feedsportal.com/");
	}

}
