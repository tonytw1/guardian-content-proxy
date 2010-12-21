package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

public class ContentChecksumCalculator {
	
	public String calculateChecksum(List<Article> articles) {
		StringBuilder hashContent = new StringBuilder();
		for (Article article : articles) {
			hashContent.append(article.getHeadline());
			if (article.getPubDateString() != null) {
				hashContent.append(article.getPubDateString().toString());
			}
		}
		return DigestUtils.md5Hex(hashContent.toString());
	}

}
