package uk.gov.hmrc.des.pubsub.listener.core.exceptions;

public class InvalidRestRequestException extends Exception {

	private static final long serialVersionUID = -3112532638377846050L;

	public InvalidRestRequestException() {
	}

	public InvalidRestRequestException(String message) {
		super(message);
	}

	public InvalidRestRequestException(Throwable cause) {
		super(cause);
	}

	public InvalidRestRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidRestRequestException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
