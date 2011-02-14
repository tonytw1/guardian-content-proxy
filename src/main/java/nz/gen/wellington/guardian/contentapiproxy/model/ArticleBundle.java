package nz.gen.wellington.guardian.contentapiproxy.model;

import java.util.List;

import nz.gen.wellington.guardian.model.Article;

public class ArticleBundle {
	
	private List<Article> articles;
	private String description;
		
	public ArticleBundle(List<Article> articles, String description) {
		this.articles = articles;
		this.description = description;
	}

	public ArticleBundle(List<Article> articles) {
		this.articles = articles;
	}

	public List<Article> getArticles() {
		return articles;
	}

	public String getDescription() {
		return description;
	}
	
}
