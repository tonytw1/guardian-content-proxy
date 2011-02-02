package nz.gen.wellington.guardian.contentapiproxy.model;

public class MediaElement {
	
	private String type;
	private String file;
	private String caption;
	private Integer width;
	private Integer height;
		
	public MediaElement(String type, Integer width, Integer height, String file, String caption) {
		this.type = type;
		this.width = width;
		this.height = height;
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

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}
		
}
