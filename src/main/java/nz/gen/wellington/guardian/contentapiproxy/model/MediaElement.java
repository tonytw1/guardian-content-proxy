package nz.gen.wellington.guardian.contentapiproxy.model;

public class MediaElement {
	
	private String type;
	private String file;
	private String caption;
		
	public MediaElement(String type, String file, String caption) {
		this.type = type;
		this.file = file;
		this.caption = caption;
	}

	public String getType() {
		return type;
	}

	public String getFile() {
		return file;
	}

	public String getCaption() {
		return caption;
	}
	
}
