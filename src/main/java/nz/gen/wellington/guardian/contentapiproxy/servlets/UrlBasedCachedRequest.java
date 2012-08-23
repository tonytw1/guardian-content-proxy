package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

public abstract class UrlBasedCachedRequest extends HttpServlet {	// TODO inline
	
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Logger.getLogger(UrlBasedCachedRequest.class);
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
		
		final String output = getContent(request);
		if (output != null) {
			log.debug("Outputing content: " + output.length() + " characters");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/xml");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Cache-Control", "max-age=600");
			response.addHeader("Etag", DigestUtils.md5Hex(output));
			PrintWriter writer = response.getWriter();
			writer.print(output);
			writer.flush();

		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	protected abstract String getContent(HttpServletRequest request);

}
