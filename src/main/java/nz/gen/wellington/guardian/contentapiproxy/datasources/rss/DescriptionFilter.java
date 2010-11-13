package nz.gen.wellington.guardian.contentapiproxy.datasources.rss;

public class DescriptionFilter {
	
	public String filterOutMeaninglessDescriptions(String description) {
		if (!isDefaultMeaninglessDescription(description)) {
			return description;
		}
		return null;
	}

	private boolean isDefaultMeaninglessDescription(String description) {
		return description != null && (description.startsWith("Articles published by guardian.co.uk") || 
				description.startsWith("Latest news and features from guardian.co.uk, the world's leading liberal voice"));
	}

}
