package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;

import org.apache.log4j.Logger;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.api.images.ImagesService.OutputEncoding;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ThumbnailProxyServlet extends HttpServlet{

	private static Logger log = Logger.getLogger(SearchProxyServlet.class);
	
	private static final long serialVersionUID = 1L;
	private static final int THUMBNAIL_TTL = 60 * 60 * 24;
	
	private CachingHttpFetcher httpFetcher;
	private ImagesService imagesService;
	
	private MemcacheService cache;

	@Inject
	public ThumbnailProxyServlet(CachingHttpFetcher httpFetcher) {
		this.httpFetcher = httpFetcher;
		this.imagesService = ImagesServiceFactory.getImagesService();
		this.cache = MemcacheServiceFactory.getMemcacheService();
	}
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String sourceUrl = req.getParameter("file");
			
			if (sourceUrl == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			
			if (!sourceUrl.startsWith("http://static.guim.co.uk/")) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			if (cache.contains(sourceUrl)) {
				byte[] bytes = cacheGet(sourceUrl);
				outputAsJpeg(bytes,resp);
				return;				
			}

			byte[] bytes = null;
			bytes = httpFetcher.fetchBytes(sourceUrl);
			if (bytes != null) {
		        Image orginalImage = ImagesServiceFactory.makeImage(bytes);	        
		        if (orginalImage != null) {
		        	byte[] imageData = makeThumbnailImageFor(orginalImage).getImageData();
		        	cacheContent(sourceUrl, imageData);		        	
					outputAsJpeg(imageData, resp);
		        	return;
		        }
			}
			
		} catch (HttpForbiddenException e) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
			
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	
	private void outputAsJpeg(byte[] bytes, ServletResponse resp) throws IOException {
		resp.setContentType("image/jpeg");
    	resp.getOutputStream().write(bytes);
    	return;
	}


	protected void cacheContent(String queryCacheKey, byte[] bytes) {
		log.info("Caching results for call: " + queryCacheKey);
		cache.put(queryCacheKey, bytes, Expiration.byDeltaSeconds(THUMBNAIL_TTL));
	}
	
	
	private byte[] cacheGet(String queryCacheKey) {
		log.info("Getting Cached results for call: " + queryCacheKey);
		return (byte[]) cache.get(queryCacheKey);
	}
	

	private Image makeThumbnailImageFor(Image orginalImage) {		
		int newHeight = 80;
		Transform resize = ImagesServiceFactory.makeResize(0, newHeight);				
		Image newImage = imagesService.applyTransform(resize, orginalImage, OutputEncoding.JPEG);
		return newImage;
	}
	
}
