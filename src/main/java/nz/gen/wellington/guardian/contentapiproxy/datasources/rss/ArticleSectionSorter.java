package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;

import org.apache.log4j.Logger;

public class ArticleSectionSorter {
	
	Logger log = Logger.getLogger(ArticleSectionSorter.class);
	
	public List<Article> sortAndTrim(List<Article> articles, int limit) {
		LinkedList<Article> results = new LinkedList<Article>();

		SortedMap<Date, Article> sorted = new TreeMap<Date, Article>();
		for (Article article : articles) {
			sorted.put(article.getPubDate(), article);
		}
		
		SortedMap<Date, Article> trimmed = trim(sorted, limit);
		
		while (!trimmed.isEmpty()) {
			Article latest = trimmed.get(trimmed.lastKey());
			addArticlesForSection(trimmed, results, latest.getSection());
		}
		return results;
	}
	
	
	private SortedMap<Date, Article> trim(SortedMap<Date, Article> sorted, int limit) {		
		if (sorted.size() <= limit) {
			return sorted;
		}
		
		SortedMap<Date, Article> trimmed = new TreeMap<Date, Article>();
		for (int i = 0; i < limit; i++) {
			Article article = sorted.get(sorted.lastKey());
			sorted.remove(article.getPubDate());
			trimmed.put(article.getPubDate(), article);
		}
		return trimmed;
	}
	
	private void addArticlesForSection(SortedMap<Date, Article> topStories, LinkedList<Article> results, Section section) {
		List<Article> sectionArticles = new ArrayList<Article>();
		for (Article article : new LinkedList<Article>(topStories.values())) {					
			final boolean articleIsForSection = (article.getSection() != null && article.getSection().getId().equals(section.getId())) || (article.getSection() == null && section == null);
			if (articleIsForSection) {
				sectionArticles.add(article);
				topStories.remove(article.getPubDate());
			}
		}
		Collections.reverse(sectionArticles);
		results.addAll(sectionArticles);
	}

}
