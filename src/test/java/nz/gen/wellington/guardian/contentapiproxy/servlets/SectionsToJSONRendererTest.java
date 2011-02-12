package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import nz.gen.wellington.guardian.model.Section;

public class SectionsToJSONRendererTest extends TestCase {
	
	public void testShouldRenderJSON() throws Exception {
		Map<String, Section> sections = new HashMap<String, Section>();
		Section section = new Section("environment", "Environment");
		sections.put(section.getId(), section);
		SectionsToJSONRenderer renderer = new SectionsToJSONRenderer();
		assertNotNull(renderer.outputJSON(sections));
	}

}
