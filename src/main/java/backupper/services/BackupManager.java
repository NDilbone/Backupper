package backupper.services;

import backupper.config.BackupConfig;
import backupper.defaults.ICopier;
import backupper.utils.LoggingUtil;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Service class responsible for managing the backup process.
 * Coordinates the file copying operation using the configured copier implementation.
 */
public class BackupManager {

	private final LoggingUtil logger;

	private final ICopier fileCopier;
	private final BackupConfig config;

	/**
	 * Creates a new BackupManager with the specified file copier and configuration.
	 *
	 * @param fileCopier The file copier implementation to use
	 * @param config     The backup configuration
	 */
	public BackupManager(ICopier fileCopier, BackupConfig config) {
		this.logger = new LoggingUtil(this.getClass());
		this.fileCopier = fileCopier;
		this.config = config;
		logger.debug("BackupManager initialized with copier: {} and config source: {}, destination: {}",
				fileCopier.getClass().getSimpleName(), config.getSourceDir(), config.getDestinationDir());
	}

	/**
	 * Runs the backup process, copying files from the source directory to the destination directory.
	 *
	 * @param destinationDir The destination directory for the backup
	 */
	public void runBackup(Path destinationDir) {
		logger.info("Starting backup from {} to {}", config.getSourceDir(), destinationDir);
		try {
			logger.debug("Delegating file copying to: {}", fileCopier.getClass().getSimpleName());
			fileCopier.copyFiles(config.getSourceDir(), destinationDir);
			logger.info("Backup completed successfully");
		} catch (IOException e) {
			logger.error("Backup failed: {}", e.getMessage(), e);
			throw new RuntimeException("Backup operation failed", e);
		}
	}
}