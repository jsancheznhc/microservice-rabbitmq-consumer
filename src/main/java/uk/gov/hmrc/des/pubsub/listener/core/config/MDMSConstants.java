package uk.gov.hmrc.des.pubsub.listener.core.config;

/**
 * @author RC00278
 */
public interface MDMSConstants {

	public static final String CONFIG_REST_ENDPOINT = "uk.gov.hmrc.mdms.rest.endpoint";
	public static final String CONFIG_REST_CREDENTIALS = "uk.gov.hmrc.mdms.rest.credentials";
	public static final String CONFIG_REST_TIMEOUT = "uk.gov.hmrc.mdms.rest.timeout";

	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";

	// MDC constants
	public static final String RETRIES = "Retries";

	public enum Status {
		SUCCESS(200, "Ok, Successful response"), BAD_REQUEST(400, "Bad Request"),
		DLQ(403, "Forbidden, Server refuse action [DELETE PAYLOAD]"), FISNISHED_RETRIES(410, "Gone [FINISHED RETRY]"),
		SERVER_ERROR(500, "Internal Server Error"), RETRY(503, "Service Unavailable [RETRY PAYLOAD]"),
		UNKNOWN(520, "Unknown Error has occured"), DIAGNOSTIC(999, "Diagnostic"),
		DEFAULT(-1, "Default status if http code not present");

		private int code;
		private String description;

		private Status(int code) {
			this.code = code;
			this.description = "unknown";
		}

		private Status(String description) {
			this.code = 999;
			this.description = description;
		}

		private Status(int code, String description) {
			this.code = code;
			this.description = description;
		}

		public static Status fromCode(int code) {
			for (Status status : Status.values()) {
				if (status.code == code) {
					return status;
				}
			}
			return UNKNOWN;
		}

		public int getCode() {
			return code;
		}

		public String getDescription() {
			return description;
		}

	}

}
