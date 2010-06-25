package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.FreeTierContentApi;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@SuppressWarnings("serial")
@Singleton
public class SectionProxyServlet extends HttpServlet {

	Logger log = Logger.getLogger(SectionProxyServlet.class);

	CachingHttpFetcher httpFetcher;
	FreeTierContentApi contentApi;
	
	@Inject
	public SectionProxyServlet(CachingHttpFetcher httpFetcher, FreeTierContentApi contentApi) {
		this.httpFetcher = httpFetcher;
		this.contentApi = contentApi;
	}
	
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		
		if (request.getRequestURI().equals("/sections")) {
					
			final String content = httpFetcher.fetchContent(contentApi.buildApiSectionsQueryUrl(), "UTF-8");
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
	
}
