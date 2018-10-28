package uk.gov.hmrc.des.pubsub.listener.core.tracer;

import org.slf4j.LoggerFactory;

import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants;

/**
 * Class for management the logger for specific level and category
 * 
 * @author RC00278
 * @since 1.0.0
 *
 */
public class LoggerManager {
	
	// constants default values messages
	public static final String EMPTY = "_EMPTY_";
	
	@SuppressWarnings("rawtypes")
	static class LoggerImpl implements ILogger {
		private Class clazz;
		private String label;
		private int httpStatus;

		/**
		 * Constructor to instantiate specific logger for class to trace
		 * 
		 * @param clazz
		 */
		public LoggerImpl(Class clazz) {
			this.clazz = clazz;
			this.label = null;
			this.httpStatus = MDMSConstants.Status.DEFAULT.getCode();
		}

		/**
		 * Constructor to instantiate specific logger for class and label to trace
		 * 
		 * @param clazz
		 * @param label
		 */
		public LoggerImpl(Class clazz, String label) {
			this.clazz = clazz;
			this.label = label;
			this.httpStatus = -1;
		}

		/**
		 * Constructor to instantiate specific logger for class, label and http status
		 * to trace
		 * 
		 * @param clazz
		 * @param label
		 * @param httpStatus
		 */
		public LoggerImpl(Class clazz, String label, int httpStatus) {
			this.clazz = clazz;
			this.label = label;
			this.httpStatus = httpStatus;
		}

		/**
		 * @param label
		 *            the label to set
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @return the httpStatus
		 */
		public int getHttpStatus() {
			return httpStatus;
		}

		@Override
		public void debug(String msg) {
			LoggerFactory.getLogger(clazz).debug(LoggerManager.formatMessage(this, msg));
		}

		@Override
		public void debug(String msg, Object arg) {
			LoggerFactory.getLogger(clazz).debug(LoggerManager.formatMessage(this, msg), arg);
		}

		@Override
		public void debug(String msg, Object arg1, Object arg2) {
			LoggerFactory.getLogger(clazz).debug(LoggerManager.formatMessage(this, msg), arg1, arg2);
		}

		@Override
		public void debug(String msg, Object... args) {
			LoggerFactory.getLogger(clazz).debug(LoggerManager.formatMessage(this, msg), args);
		}

		@Override
		public void debug(String msg, Throwable t) {
			LoggerFactory.getLogger(clazz).debug(LoggerManager.formatMessage(this, msg), t);
		}

		@Override
		public void info(String msg) {
			LoggerFactory.getLogger(clazz).info(LoggerManager.formatMessage(this, msg));
		}

		@Override
		public void info(String msg, Object arg) {
			LoggerFactory.getLogger(clazz).info(LoggerManager.formatMessage(this, msg), arg);
		}

		@Override
		public void info(String msg, Object arg1, Object arg2) {
			LoggerFactory.getLogger(clazz).info(LoggerManager.formatMessage(this, msg), arg1, arg2);
		}

		@Override
		public void info(String msg, Object... args) {
			LoggerFactory.getLogger(clazz).info(LoggerManager.formatMessage(this, msg), args);
		}

		@Override
		public void info(String msg, Throwable t) {
			LoggerFactory.getLogger(clazz).info(LoggerManager.formatMessage(this, msg), t);
		}

		@Override
		public void warn(String msg) {
			LoggerFactory.getLogger(clazz).warn(LoggerManager.formatMessage(this, msg));
		}

		@Override
		public void warn(String msg, Object arg) {

			LoggerFactory.getLogger(clazz).warn(LoggerManager.formatMessage(this, msg), arg);
		}

		@Override
		public void warn(String msg, Object arg1, Object arg2) {
			LoggerFactory.getLogger(clazz).warn(LoggerManager.formatMessage(this, msg), arg1, arg2);
		}

		@Override
		public void warn(String msg, Object... args) {
			LoggerFactory.getLogger(clazz).warn(LoggerManager.formatMessage(this, msg), args);
		}

		@Override
		public void warn(String msg, Throwable t) {
			LoggerFactory.getLogger(clazz).warn(LoggerManager.formatMessage(this, msg), t);
		}

		@Override
		public void error(String msg) {
			LoggerFactory.getLogger(clazz).error(LoggerManager.formatMessage(this, msg));
		}

		@Override
		public void error(String msg, Object arg) {
			LoggerFactory.getLogger(clazz).error(LoggerManager.formatMessage(this, msg), arg);
		}

		@Override
		public void error(String msg, Object arg1, Object arg2) {
			LoggerFactory.getLogger(clazz).error(LoggerManager.formatMessage(this, msg), arg1, arg2);
		}

		@Override
		public void error(String msg, Object... args) {
			LoggerFactory.getLogger(clazz).error(LoggerManager.formatMessage(this, msg), args);
		}

		@Override
		public void error(String msg, Throwable t) {
			LoggerFactory.getLogger(clazz).error(LoggerManager.formatMessage(this, msg), t);
		}
	}

	/**
	 * Format message out with label and message
	 * 
	 * @param label
	 * @param msg
	 * @return final message formated
	 */
	private static String formatMessage(ILogger logger, String msg) {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		if (logger.getLabel() != null) {
			sb.append("\"label\":\"").append(logger.getLabel()).append("\", ");
		}
		if (msg != null) {
			sb.append("\"message\":\"").append(msg).append("\"");;
		}
		// always put httpStatus, the default value is -1
		if (logger.getHttpStatus() != MDMSConstants.Status.DIAGNOSTIC.getCode() && logger.getHttpStatus() != MDMSConstants.Status.DEFAULT.getCode()) {
			sb.append(", ").append("\"httpStatus\":\"").append(logger.getHttpStatus()).append("\"");
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public static ILogger getLogger(Class<?> clazz) {
		return new LoggerImpl(clazz == null ? LoggerManager.class : clazz);
	}

	/**
	 * 
	 * @param clazz
	 * @param label
	 * @return
	 */
	public static ILogger getLogger(Class<?> clazz, String label) {
		return new LoggerImpl(clazz == null ? LoggerManager.class : clazz, label);
	}

	/**
	 * 
	 * @param clazz
	 * @param label
	 * @param httpStatus
	 * @return
	 */
	public static ILogger getLogger(Class<?> clazz, String label, int httpStatus) {
		return new LoggerImpl(clazz == null ? LoggerManager.class : clazz, label, httpStatus != 0 ? httpStatus : MDMSConstants.Status.DIAGNOSTIC.getCode());
	}

}
