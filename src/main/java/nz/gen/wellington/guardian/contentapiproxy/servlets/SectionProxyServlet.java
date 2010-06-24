package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ContentApiDataSource;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@SuppressWarnings("serial")
@Singleton
public class SectionProxyServlet extends ApiProxyServlet {

	protected static final String API_HOST = "http://content.guardianapis.com";

	Logger log = Logger.getLogger(SectionProxyServlet.class);

	CachingHttpFetcher httpFetcher;
	GuardianDataSource datasource;
	
	@Inject
	public SectionProxyServlet(CachingHttpFetcher httpFetcher, ContentApiDataSource datasource) {
		this.httpFetcher = httpFetcher;
		this.datasource = datasource;	// TODO migrate to this.
	}
	
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		
		if (request.getRequestURI().equals("/sections")) {
					
			final String content = httpFetcher.fetchContent(buildApiSectionsQueryUrl("techdev"), "UTF-8");
			if (content != null) {
				response.setStatus(HttpServletResponse.SC_OK);
				PrintWriter writer = response.getWriter();
				writer.print(content);
				writer.flush();
				return;						
			}			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;			
		}
		
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return;
	}
	

	private String buildApiSectionsQueryUrl(String apikey) throws UnsupportedEncodingException {
		StringBuilder queryUrl = new StringBuilder(API_HOST + "/sections");
		queryUrl.append("?api-key=" + URLEncoder.encode(apikey, "UTF-8"));
		queryUrl.append("&format=json");
		return queryUrl.toString();
	}

}
