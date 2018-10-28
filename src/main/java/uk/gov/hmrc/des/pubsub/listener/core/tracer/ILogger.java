package uk.gov.hmrc.des.pubsub.listener.core.tracer;

public interface ILogger {
	// getters
	/**
	 * @return the label
	 */
	public String getLabel();
	/**
	 * @return the httpStatus
	 */
	public int getHttpStatus();
	// debug methods
	void debug(String msg);

	void debug(String msg, Object arg);

	void debug(String msg, Object arg1, Object arg2);

	void debug(String msg, Object... args);

	void debug(String msg, Throwable t);

	// info methods
	void info(String msg);

	void info(String msg, Object arg);

	void info(String msg, Object arg1, Object arg2);

	void info(String msg, Object... args);

	void info(String msg, Throwable t);

	// warn methods
	void warn(String msg);

	void warn(String msg, Object arg);

	void warn(String msg, Object arg1, Object arg2);

	void warn(String msg, Object... args);

	void warn(String msg, Throwable t);

	// error methods
	void error(String msg);

	void error(String msg, Object arg);

	void error(String msg, Object arg1, Object arg2);

	void error(String msg, Object... args);

	void error(String msg, Throwable t);
}
