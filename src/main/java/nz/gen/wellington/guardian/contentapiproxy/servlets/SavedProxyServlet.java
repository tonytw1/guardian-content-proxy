package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.SavedContentDataSource;
import nz.gen.wellington.guardian.contentapiproxy.output.ArticleToXmlRenderer;
import nz.gen.wellington.guardian.model.Article;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SavedProxyServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(SavedProxyServlet.class);
	
	private SavedContentDataSource datasource;
	private ArticleToXmlRenderer articleToXmlRenderer;
	
	@Inject
	public SavedProxyServlet(SavedContentDataSource datasource, ArticleToXmlRenderer articleToXmlRenderer) {
		this.datasource = datasource;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
		
		if (request.getParameter("content") != null) {			
			final String output = getContent(request.getParameter("content"));
			
			if (output != null) {
				log.info("Outputting content: " + output.length() + " characters");
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
		
		return;
	}

	private String getContent(String content) {
		content = URLDecoder.decode(content);		
		List<String> articleIds = Arrays.asList(content.split(","));		
		List<Article> articles = datasource.getArticles(articleIds);
		if (articles == null) {
			return null;
		}		
		return articleToXmlRenderer.outputXml(articles, null, null, true);
	}
	
}