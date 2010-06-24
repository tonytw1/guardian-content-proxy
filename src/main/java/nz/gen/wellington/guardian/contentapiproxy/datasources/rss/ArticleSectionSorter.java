package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.joda.time.DateTime;

import nz.gen.wellington.guardian.contentapiproxy.model.Article;
import nz.gen.wellington.guardian.contentapiproxy.model.Section;

public class ArticleSectionSorter {

	public List<Article> sort(List<Article> articles) {
		SortedMap<DateTime, Article> sorted = new TreeMap<DateTime, Article>();
		for (Article article : articles) {
			sorted.put(article.getPubDate(), article);
		}

		LinkedList<Article> results = new LinkedList<Article>();

		while (!sorted.isEmpty()) {
			Article latest = sorted.get(sorted.lastKey());
			addArticlesForSection(sorted, results, latest.getSection());
		}

		return results;
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
