package backupper.services;

import backupper.utils.ChecksumVerifier;
import backupper.utils.LoggingUtil;
import backupper.utils.RetryHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handles the copying of individual files during the backup process.
 * This class provides functionality to submit file copy tasks to an executor service,
 * copy files with retry logic, verify file integrity using checksums,
 * and track failed file operations.
 */
public class FileCopier {

	private final LoggingUtil logger;

	private final RetryHandler retryHandler;
	private final ChecksumVerifier checksumVerifier;
	private final ExecutorService executorService;
	private final List<Path> failedFiles;

	/**
	 * Constructs a FileCopier with the necessary parts for file copying operations.
	 *
	 * @param retryHandler     The retry handler used to retry failed operations
	 * @param checksumVerifier The checksum verifier used to verify file integrity
	 * @param executorService  The executor service used to submit file copy tasks
	 * @param failedFiles      A shared list to track files that failed to copy
	 */
	public FileCopier(RetryHandler retryHandler, ChecksumVerifier checksumVerifier,
	                  ExecutorService executorService, List<Path> failedFiles) {
		this.logger = new LoggingUtil(this.getClass());
		this.retryHandler = retryHandler;
		this.checksumVerifier = checksumVerifier;
		this.executorService = executorService;
		this.failedFiles = failedFiles;
	}

	/**
	 * Submits a file copy task to the executor service.
	 * The actual copying is performed asynchronously by the copyFileWithRetry method.
	 *
	 * @param sourceFile      The source file to copy
	 * @param destinationFile The destination path where the file will be copied
	 */
	public void submitFileForCopy(Path sourceFile, Path destinationFile) {
		logger.debug("Submitting file for copy: {} -> {}", sourceFile.getFileName(), destinationFile);
		executorService.submit(() -> copyFileWithRetry(sourceFile, destinationFile));
	}

	/**
	 * Copies a file with retry logic and checksum verification.
	 * If the copy operation fails or the checksum verification fails, the operation is retried
	 * according to the retry policy defined in the RetryHandler.
	 * If all retries fail, the file is added to the failedFiles list.
	 *
	 * @param sourceFile      The source file to copy
	 * @param destinationFile The destination path where the file will be copied
	 */
	private void copyFileWithRetry(Path sourceFile, Path destinationFile) {
		boolean success = retryHandler.executeWithRetry(() -> {
			try {
				Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
				logger.debug("File copied: {} → {}", sourceFile.getFileName(), destinationFile);
			} catch (IOException e) {
				logger.error("Failed to copy file: {}", sourceFile.getFileName(), e);
				throw new RuntimeException("File copy failed: " + sourceFile.getFileName(), e); // Throw exception to trigger retry
			}

			if (Files.isDirectory(sourceFile)) {
				logger.debug("Skipping checksum verification for directory: {}", sourceFile.getFileName());
				return true;
			}

			boolean checksumVerified = checksumVerifier.verifyFileIntegrity(sourceFile, destinationFile);
			if (!checksumVerified) {
				logger.warn("Checksum verification failed for {}. Triggering retry...", sourceFile.getFileName());
				throw new RuntimeException("Checksum verification failed: " + sourceFile.getFileName()); // Throw exception to trigger retry
			}

			return true;
		}, "Copying file " + sourceFile.getFileName(), sourceFile.getFileName().toString());

		if (!success) {
			synchronized (failedFiles) {
				failedFiles.add(sourceFile);
			}
		}
	}
}