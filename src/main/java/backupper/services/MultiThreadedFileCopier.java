package backupper.services;

import backupper.defaults.ICopier;
import backupper.utils.ChecksumVerifier;
import backupper.utils.LoggingUtil;
import backupper.utils.RetryHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A multithreaded implementation of the ICopier interface that provides parallel file copying capabilities.
 * This class uses a thread pool to copy files concurrently, improving performance for large backup operations.
 * It handles retry logic, checksum verification, and tracks failed file operations.
 */
public class MultiThreadedFileCopier implements ICopier {

	private final LoggingUtil logger;

	private final ExecutorService executorService;
	public static final List<Path> failedFiles = new ArrayList<>();
	private final DirectoryProcessor directoryProcessor;

	/**
	 * Constructs a MultiThreadedFileCopier with custom configuration parameters.
	 *
	 * @param threadPoolSize    The number of threads to use for parallel file copying
	 * @param maxRetries        The maximum number of retry attempts for failed operations
	 * @param retryDelayMs      The delay in milliseconds between retry attempts
	 * @param exclusionPatterns List of regex patterns defining files/directories to exclude from backup
	 */
	public MultiThreadedFileCopier(int threadPoolSize, int maxRetries, int retryDelayMs, List<String> exclusionPatterns) {
		this.logger = new LoggingUtil(this.getClass());
		logger.info("Initializing with thread pool size: {}, max retries: {}, retry delay: {}ms",
				threadPoolSize, maxRetries, retryDelayMs);
		this.executorService = Executors.newFixedThreadPool(threadPoolSize);
		RetryHandler retryHandler = new RetryHandler(maxRetries, retryDelayMs);
		ChecksumVerifier checksumVerifier = new ChecksumVerifier();
		FileCopier fileCopier = new FileCopier(retryHandler, checksumVerifier, executorService, failedFiles);
		this.directoryProcessor = new DirectoryProcessor(fileCopier, exclusionPatterns);
	}

	/**
	 * Copies files from the source directory to the destination directory using multiple threads.
	 * This method processes the directory structure, submits file copy tasks to the thread pool,
	 * waits for all tasks to complete, and logs any failed operations.
	 *
	 * @param sourceDir      The source directory containing files to copy
	 * @param destinationDir The destination directory where files will be copied
	 * @throws IOException If an I/O error occurs during directory processing
	 */
	@Override
	public void copyFiles(Path sourceDir, Path destinationDir) throws IOException {
		directoryProcessor.processDirectory(sourceDir, destinationDir);

		executorService.shutdown();
		logger.debug("Waiting for all tasks to complete...");
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				logger.warn("Timeout! Not all files were copied.");
			}
		} catch (InterruptedException e) {
			logger.error("Interrupted while waiting for tasks to complete.", e);
			Thread.currentThread().interrupt();
		}

		logFailedFiles();
	}

	/**
	 * Logs information about the success or failure of the file copying operation.
	 * If all files were copied successfully, it logs a success message.
	 * If any files failed to copy, it logs the number of failed files and their paths.
	 */
	private void logFailedFiles() {
		if (failedFiles.isEmpty()) {
			logger.info("All files copied successfully!");
		} else {
			logger.warn("Backup completed with {} failed file(s):", failedFiles.size());
			for (Path failedFile : failedFiles) {
				logger.warn("- {}", failedFile);
			}
		}
	}
}