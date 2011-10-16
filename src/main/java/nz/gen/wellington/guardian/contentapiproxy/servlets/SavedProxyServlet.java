package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SavedContentDataSource;
import nz.gen.wellington.guardian.contentapiproxy.output.ArticleToXmlRenderer;
import nz.gen.wellington.guardian.model.Article;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class SavedProxyServlet extends CacheAwareProxyServlet {
		
	private SavedContentDataSource datasource;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public SavedProxyServlet(Cache cache, SavedContentDataSource datasource, ArticleToXmlRenderer articleToXmlRenderer) {
		super(cache);
		this.datasource = datasource;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/saved") && request.getParameter("content") != null) {
			
            final String queryCacheKey = getQueryCacheKey(request);
            String output = cacheGet(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            }
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);	
				output = getContent(request.getParameter("content"));
				if (output != null) {
					cacheContent(queryCacheKey, output);
				}				
			}
			
			if (output != null) {
				log.info("Outputing content: " + output.length() + " characters");
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
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