package uk.gov.hmrc.des.pubsub.listener.core.exceptions;

public class UnknownBackendResponseException extends Exception {

	private static final long serialVersionUID = 6308742610549238030L;

	public UnknownBackendResponseException() {
		super();
	}

	public UnknownBackendResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownBackendResponseException(String message) {
		super(message);
	}

	public UnknownBackendResponseException(Throwable cause) {
		super(cause);
	}

}
