package nz.gen.wellington.guardian.contentapiproxy.model;


public class Tag {

	private String id;
	private String type;
	private String name;
	private Section section;

	public Tag(String name, String id, Section section, String type) {
		this.name = name;
		this.id = id;
		this.section = section;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public Section getSection() {
		return section;
	}

	public void setSection(Section section) {
		this.section = section;
	}

	public boolean isSectionTag() {
		if (section != null) {
			final String sectionTagId = section.getId() + "/" + section.getId();
			return id.equals(sectionTagId);			
		}
		return false;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
