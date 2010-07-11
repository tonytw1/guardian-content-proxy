package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.List;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

public class ContentChecksumCalculator {

	static Logger log = Logger.getLogger(ContentChecksumCalculator.class);
	
	public String calculateChecksum(List<Article> articles) {
		StringBuilder hashContent = new StringBuilder();
		for (Article article : articles) {
			hashContent.append(article.getTitle());
			hashContent.append(article.getPubDateString().toString());
		}
		return DigestUtils.md5Hex(hashContent.toString());
	}

}
