package nz.gen.wellington.guardian.contentapiproxy.model;

public class Section {
	
	private String id;
	private String name;
	
	public Section(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String apiUrl) {
		this.id = apiUrl;
	}
	
}
