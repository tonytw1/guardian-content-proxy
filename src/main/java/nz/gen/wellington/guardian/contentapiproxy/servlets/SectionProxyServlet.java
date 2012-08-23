package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.rss.RssDataSource;
import nz.gen.wellington.guardian.contentapiproxy.output.SectionsToJSONRenderer;
import nz.gen.wellington.guardian.model.Section;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SectionProxyServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(SectionProxyServlet.class);

	private GuardianDataSource dataSource;
	private SectionsToJSONRenderer sectionsToJSONRenderer;
	
	@Inject
	public SectionProxyServlet(RssDataSource dataSource, SectionsToJSONRenderer sectionsToJSONRenderer) {
		this.dataSource = dataSource;
		this.sectionsToJSONRenderer = sectionsToJSONRenderer;
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		
		Map<String, Section> sections = dataSource.getSections();
		if (sections != null) {
			final String content = sectionsToJSONRenderer.outputJSON(sections);
	
			if (content != null) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Cache-Control", "max-age=1800");
				response.addHeader("Etag", DigestUtils.md5Hex(content));
				PrintWriter writer = response.getWriter();
				writer.print(content);
				writer.flush();
				return;
			}
		
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}

}
