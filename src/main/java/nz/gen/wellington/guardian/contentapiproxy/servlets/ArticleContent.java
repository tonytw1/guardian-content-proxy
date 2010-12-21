package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.Serializable;

public class ArticleContent implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String content;
	private String etag;

	public ArticleContent(String content, String etag) {
		this.content = content;
		this.etag = etag;
	}
	
	public String getContent() {
		return content;
	}

	public String getEtag() {
		return etag;
	}
	
}
