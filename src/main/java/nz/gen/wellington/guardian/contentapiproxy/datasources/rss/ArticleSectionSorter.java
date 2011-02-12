package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Section;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class ArticleSectionSorter {
	
	Logger log = Logger.getLogger(ArticleSectionSorter.class);

	public List<Article> sort(List<Article> articles) {
		LinkedList<Article> results = new LinkedList<Article>();

		SortedMap<DateTime, Article> sorted = new TreeMap<DateTime, Article>();
		for (Article article : articles) {
			sorted.put(new DateTime(article.getPubDate()), article);
		}
		
		SortedMap<DateTime, Article> trimmed = trim(sorted, 15);
		
		while (!trimmed.isEmpty()) {
			Article latest = trimmed.get(trimmed.lastKey());
			addArticlesForSection(trimmed, results, latest.getSection());
		}
		return results;
	}
	
	
	private SortedMap<DateTime, Article> trim(SortedMap<DateTime, Article> sorted, int limit) {		
		if (sorted.size() <= limit) {
			return sorted;
		}
		
		SortedMap<DateTime, Article> trimmed = new TreeMap<DateTime, Article>();
		for (int i = 0; i < limit; i++) {
			Article article = sorted.get(sorted.lastKey());
			sorted.remove(article.getPubDate());
			trimmed.put(new DateTime(article.getPubDate()), article);
		}
		return trimmed;
	}

	
	private void addArticlesForSection(SortedMap<DateTime, Article> topStories, LinkedList<Article> results, Section section) {
		List<Article> sectionArticles = new ArrayList<Article>();
		for (Article article : new LinkedList<Article>(topStories.values())) {
			if (article.getSection().getId().equals(section.getId())) {
				sectionArticles.add(article);
				topStories.remove(article.getPubDate());
			}
		}
		Collections.reverse(sectionArticles);
		results.addAll(sectionArticles);
	}

}
