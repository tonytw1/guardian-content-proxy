package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

public class HttpForbiddenException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private String errorMessage;
	
	public HttpForbiddenException(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String getMessage() {
		return errorMessage;
	}
	
}
