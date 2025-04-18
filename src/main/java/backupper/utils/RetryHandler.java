package backupper.utils;

import java.util.function.Supplier;

/**
 * Utility class for handling retry logic for operations that might fail.
 * Implements an exponential backoff strategy for retries, with configurable
 * maximum retry attempts and base delay between retries.
 */
public class RetryHandler {

	private final LoggingUtil logger;

	private final int maxRetries;
	private final int baseRetryDelayMs;

	/**
	 * Creates a new RetryHandler with the specified parameters.
	 *
	 * @param maxRetries       The maximum number of retry attempts
	 * @param baseRetryDelayMs The base delay in milliseconds between retry attempts
	 */
	public RetryHandler(int maxRetries, int baseRetryDelayMs) {
		this.logger = new LoggingUtil(this.getClass());
		this.maxRetries = maxRetries;
		this.baseRetryDelayMs = baseRetryDelayMs;
		logger.debug("Created RetryHandler with maxRetries={}, baseRetryDelayMs={}", maxRetries, baseRetryDelayMs);
	}

	/**
	 * Executes an operation with retry logic.
	 * If the operation fails, it will be retried up to the maximum number of attempts
	 * with an exponential backoff delay between attempts.
	 *
	 * @param <T>                  The return type of the operation
	 * @param operation            The operation to execute, provided as a Supplier
	 * @param operationDescription A description of the operation for logging purposes
	 * @param fileName             The name of the file being processed (for logging)
	 * @return true if the operation succeeded, false if it failed after all retry attempts
	 */
	public <T> boolean executeWithRetry(Supplier<T> operation, String operationDescription, String fileName) {
		logger.debug("Executing operation with retry: {}", operationDescription);
		int attempt = 0;
		while (attempt < maxRetries) {
			try {
				T result = operation.get();
				logger.debug("Operation succeeded: {}", operationDescription);
				return (result instanceof Boolean) ? (Boolean) result : true;
			} catch (Exception e) {
				attempt++;
				logger.warn("Attempt {}/{} failed for {}: {}",
						attempt, maxRetries, operationDescription, e.getMessage());

				if (attempt >= maxRetries) {
					logger.error("Failed to complete {} after {} attempts. Skipping...", operationDescription, maxRetries);
					return false; // Ensure method returns false on failure
				} else {
					try {
						int delay = baseRetryDelayMs * (int) Math.pow(2, attempt - 1);
						logger.debug("Retrying {} in {} ms...", operationDescription, delay);
						Thread.sleep(delay);
					} catch (InterruptedException ie) {
						logger.error("Retry interrupted for {}", fileName);
						Thread.currentThread().interrupt();
						return false; // Ensure method returns false if interrupted
					}
				}
			}
		}
		return false; // If the loop completes, return false
	}
}