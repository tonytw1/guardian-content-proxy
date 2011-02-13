package nz.gen.wellington.guardian.contentapiproxy.utils;

import java.util.List;

import nz.gen.wellington.guardian.model.Article;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

public class ContentChecksumCalculator {
	
	public String calculateChecksum(List<Article> articles) {
		StringBuilder hashContent = new StringBuilder();
		for (Article article : articles) {
			hashContent.append(article.getHeadline());
			if (article.getPubDate() != null) {
				hashContent.append(new DateTime(article.getPubDate()).toString());
			}
		}
		return DigestUtils.md5Hex(hashContent.toString());
	}

}
