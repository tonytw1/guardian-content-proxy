package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.util.List;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

public class ContentChecksumCalculator {

	Logger log = Logger.getLogger(ContentChecksumCalculator.class);
	
	public String calculateChecksum(List<Article> articles) {
		StringBuilder hashContent = new StringBuilder();
		for (Article article : articles) {
			hashContent.append(article.getTitle());
			hashContent.append(article.getPubDateString().toString());
		}
		return DigestUtils.md5Hex(hashContent.toString());
	}

}
