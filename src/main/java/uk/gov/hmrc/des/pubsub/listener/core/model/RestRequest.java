package uk.gov.hmrc.des.pubsub.listener.core.model;

import java.util.Map;

public class RestRequest {
	private Map<String, String> headers;
	private String body;
	private String method;
	private String endpoint;

	private RestRequest() {
	}
	
	public RestRequest(Map<String, String> headers, String body, String method, String endpoint) {
		this();
		this.headers = headers;
		this.body = body;
		this.method = method;
		this.endpoint = endpoint;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

}
