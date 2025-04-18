package backupper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for logging that wraps SLF4J Logger functionality.
 * Provides simplified access to common logging levels.
 */
public class LoggingUtil {

	private final Logger logger;

	/**
	 * Creates a new LoggingUtil instance for the specified class.
	 *
	 * @param clazz The class to create the logger for
	 */
	public LoggingUtil(Class<?> clazz) {
		this.logger = LoggerFactory.getLogger(clazz);
	}

	/**
	 * Logs a message at INFO level.
	 *
	 * @param message The message format string
	 * @param args    Arguments referenced by the format specifiers in the message string
	 */
	public void info(String message, Object... args) {
		logger.info(message, args);
	}

	/**
	 * Logs a message at DEBUG level.
	 *
	 * @param message The message format string
	 * @param args    Arguments referenced by the format specifiers in the message string
	 */
	public void debug(String message, Object... args) {
		logger.debug(message, args);
	}

	/**
	 * Logs a message at WARN level.
	 *
	 * @param message The message format string
	 * @param args    Arguments referenced by the format specifiers in the message string
	 */
	public void warn(String message, Object... args) {
		logger.warn(message, args);
	}

	/**
	 * Logs a message at ERROR level.
	 *
	 * @param message The message format string
	 * @param args    Arguments referenced by the format specifiers in the message string
	 */
	public void error(String message, Object... args) {
		logger.error(message, args);
	}

	/**
	 * Logs a message with an exception at ERROR level.
	 *
	 * @param message   The message format string
	 * @param throwable The exception to log
	 * @param args      Arguments referenced by the format specifiers in the message string
	 */
	public void error(String message, Throwable throwable, Object... args) {
		logger.error(message, args, throwable);
	}
}