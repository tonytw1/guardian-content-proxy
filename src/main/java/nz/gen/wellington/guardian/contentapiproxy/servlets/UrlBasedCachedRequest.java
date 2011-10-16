package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.caching.Cache;

import org.apache.commons.codec.digest.DigestUtils;

@SuppressWarnings("serial")
public abstract class UrlBasedCachedRequest extends CacheAwareProxyServlet {

	public UrlBasedCachedRequest(Cache cache) {
		super(cache);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		final String queryCacheKey = getQueryCacheKey(request);
		String output = cacheGet(queryCacheKey);
		if (output != null) {
			log.info("Returning cached results for call url: " + queryCacheKey);
		}

		if (output == null) {
			log.info("Building result for call: " + queryCacheKey);
			output = getContent(request);
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

	protected abstract String getContent(HttpServletRequest request);

}
