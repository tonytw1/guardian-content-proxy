package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlCleaner {

	static Pattern p = Pattern.compile("</p>");
	static Pattern br = Pattern.compile("<br />");

	static Pattern tags = Pattern.compile("<.*?>");

	public static String stripHtml(String content) {
		if (content == null) {
			return null;
		}
		content = p.matcher(content).replaceAll("\n\n");
		content = br.matcher(content).replaceAll("\n");

		content = StringEscapeUtils.unescapeHtml(tags.matcher(content)
				.replaceAll(""));

		content = content.replaceAll("&amp;", "&");
		content = content.replaceAll("&nbsp;", " ");
		return content.trim();
	}

}
