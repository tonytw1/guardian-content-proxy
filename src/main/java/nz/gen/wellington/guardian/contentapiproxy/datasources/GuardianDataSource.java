package nz.gen.wellington.guardian.contentapiproxy.datasources;

import nz.gen.wellington.guardian.contentapiproxy.servlets.SearchQuery;


public interface GuardianDataSource {

	public String getContent(SearchQuery query);

}
