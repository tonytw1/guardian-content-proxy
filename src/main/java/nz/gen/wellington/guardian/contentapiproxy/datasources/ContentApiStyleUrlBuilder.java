package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ContentApiStyleUrlBuilder {
		
	private static final String SEARCH_QUERY = "search";
	private static final String SECTIONS_QUERY = "sections";
	private static final String TAGS = "tags";
	private static final String OR = "|";
	
	private String apiHost;
	private String apiKey;
	private String format = "xml";

	private List<String> sections;
	private List<String> tags;
	private boolean showAll;
	private boolean showRefinements;
	private Integer pageSize;
	private String searchTerm;
	private String fromDate;
	private String toDate;
	
	public ContentApiStyleUrlBuilder(String apiHost, String apiKey) {
		this.apiHost = apiHost;
		this.apiKey = apiKey;
		this.sections = new ArrayList<String>();
		this.tags = new ArrayList<String>();
		this.showAll = false;
		this.showRefinements = false;
	}
	
	public String toSearchQueryUrl() {
		StringBuilder uri = new StringBuilder("/" + SEARCH_QUERY);
		appendCoreParameters(uri);
		
		StringBuilder sectionsParameter = new StringBuilder();			
		StringBuilder tagsParameter = new StringBuilder();			

		if (!tags.isEmpty()) {
			for (String tag : tags) {
				tagsParameter.append(tag);
				tagsParameter.append(OR);		
			}
		}
				
		if (!sections.isEmpty()) {
			for (String sectionId : sections) {
				sectionsParameter.append(sectionId);
				sectionsParameter.append(OR);
			}
		}
		
		if (sectionsParameter.length() > 0) {
			String sections = sectionsParameter.substring(0, sectionsParameter.length()-1);
			try {
				sections = URLEncoder.encode(sections, "UTF8");
			} catch (UnsupportedEncodingException e) {
			}
			uri.append("&section=");
			uri.append(sections);				
		}
		
		if (tagsParameter.length() > 0) {
			String tags = tagsParameter.substring(0, tagsParameter.length()-1);
			try {
				tags = URLEncoder.encode(tags, "UTF8");
			} catch (UnsupportedEncodingException e) {
			}
			uri.append("&tag=");
			uri.append(tags);				
		}
		
		if (fromDate != null) {
			uri.append("&from-date=" + fromDate);
		}
		
		if (toDate != null) {
			uri.append("&to-date=" + toDate);
		}
		
		if (searchTerm != null) {
			uri.append("&q=" + URLEncoder.encode(searchTerm));
		}
		
		return prependHost(uri.toString());
	}
	
	
	public String toTagSearchQueryUrl() {		
		StringBuilder uri = new StringBuilder("/" + TAGS);
		appendCoreParameters(uri);
				
		List<String> allowedTagTypes = Arrays.asList("keyword", "contributor", "blog");		// TODO should be settable using the builder pattern
		if (!allowedTagTypes.isEmpty()) {
			uri.append("&type=");
			for (Iterator<String> iterator = allowedTagTypes.iterator(); iterator.hasNext();) {
				uri.append(iterator.next());
				if (iterator.hasNext()) {
					uri.append(URLEncoder.encode(","));
				}				
			}
		}
		
		uri.append("&q=" + URLEncoder.encode(searchTerm));		
		return prependHost(uri.toString());
	}
	

	public String toSectionsQueryUrl() {
		StringBuilder uri = new StringBuilder("/" + SECTIONS_QUERY);
		appendCoreParameters(uri);
		return prependHost(uri.toString());
	}
	
	private String prependHost(String uri) {
		return apiHost + uri;
	}

	public void addSection(String sectionId) {
		sections.add(sectionId);
	}

	public void addTag(String tagId) {
		tags.add(tagId);
	}
	
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}
	
	public void setShowRefinements(boolean showRefinements) {
		this.showRefinements = showRefinements;
	}
	
	public void setFormat(String format) {
		this.format = format;
	}
	
	private void appendCoreParameters(StringBuilder url) {
		url.append("?format=" + format);
		if (pageSize != null) {
			url.append("&page-size=" + pageSize);		
		}
		if (showAll) {
			url.append("&show-fields=all");
			url.append("&show-tags=all");
		}
		
		if (apiKey != null && !apiKey.trim().equals("")) {
			url.append("&api-key=" + apiKey);
		}
		
		if (showRefinements) {
			url.append("&show-refinements=all");
		}
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public void setFromDate(String date) {
		this.fromDate = date;
	}

	public void setToDate(String date) {
		this.toDate = date;		
	}

}
