package nz.gen.wellington.guardian.contentapiproxy.model;

public class MediaElement {
	
	String type;
	String file;
		
	public MediaElement(String type, String file) {
		this.type = type;
		this.file = file;
	}

	public String getType() {
		return type;
	}

	public String getFile() {
		return file;
	}	

}
