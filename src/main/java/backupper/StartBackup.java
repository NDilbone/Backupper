package backupper;

import backupper.config.BackupConfig;
import backupper.defaults.ICopier;
import backupper.services.BackupManager;
import backupper.services.MultiThreadedFileCopier;
import backupper.services.NotificationService;
import backupper.utils.BackupCleaner;
import backupper.utils.LoggingUtil;
import backupper.utils.TimeFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Main application class for the backup system.
 * Orchestrates the backup process including configuration loading,
 * old backup cleanup, and execution of the backup operation.
 */
public class StartBackup {

	private static final LoggingUtil logger = new LoggingUtil(StartBackup.class);

	private static Path versionedBackupDir = null;
	private static Duration duration = Duration.ZERO;
	private static NotificationService notificationService = null;
	private static boolean exceptionCaught = false;

	/**
	 * Application entry point.
	 * Starts the backup process.
	 *
	 * @param args Command line arguments (not used)
	 */
	public static void main(String[] args) {
		logger.info("Application started");
		// Start the backup and get prerequisites
		executeBackup();
		logger.info("Application terminated");
	}

	/**
	 * Executes the backup process.
	 * This includes:
	 * 1. Loading configuration
	 * 2. Cleaning up old backups
	 * 3. Preparing the backup
	 * 4. Running backup
	 * 5. Sending notifications about the results
	 */
	private static void executeBackup() {
		logger.info("====================== Starting Backup Process ======================");

		try {
			// Load configuration
			logger.info("[1/4] Loading configuration...");
			BackupConfig config = BackupConfig.loadFromJson("config.json");
			logger.info("Configuration loaded successfully");

			// Initialize Notification Service
			logger.debug("Initializing notification service");
			if (config.getDiscordWebhookUrl() != null && !config.getDiscordWebhookUrl().isEmpty()) {
				notificationService = new NotificationService(
						config.getDiscordWebhookUrl(),
						config.getDiscordUserId()
				);
				logger.info("Notification service initialized");
			} else {
				logger.error("No Discord webhook URL configured");
				throw new RuntimeException("No Discord webhook URL configured.");
			}

			// Clean up old backups
			logger.info("[2/4] Cleaning up old backups...");
			int maxBackupsToKeep = config.getMaxBackupsToKeep();
			logger.debug("Creating backup cleaner with max backups: {}", maxBackupsToKeep);
			BackupCleaner cleaner = new BackupCleaner(config.getDestinationDir(), maxBackupsToKeep);
			boolean[] cleanupResults = cleaner.cleanupOldBackups();

			// Log cleanup results
			if (!cleanupResults[0]) {
				logger.info("No backups were deleted");
			} else if (cleanupResults[1]) {
				logger.warn("Some old backups could not be deleted. Check logs for details");
			} else {
				logger.info("Old backups deleted successfully");
			}

			// Initialize backup process
			logger.info("[3/4] Preparing backup...");
			logger.debug("Creating file copier with thread pool size: {}, max retries: {}, retry delay: {}ms",
					config.getThreadPoolSize(), config.getMaxRetries(), config.getRetryDelayMs());
			ICopier copier = new MultiThreadedFileCopier(
					config.getThreadPoolSize(),
					config.getMaxRetries(),
					config.getRetryDelayMs(),
					config.getExclusionPatterns()
			);
			logger.debug("Creating backup manager");
			BackupManager manager = new BackupManager(copier, config);

			// Retrieve versioned backup directory
			logger.info("[4/4] Retrieving versioned backup directory...");
			versionedBackupDir = config.getVersionedBackupDir();
			logger.debug("Backup directory: {}", versionedBackupDir);

			// Execute backup and measure duration
			logger.info("Starting backup execution");
			Instant start = Instant.now();
			manager.runBackup(versionedBackupDir);
			duration = Duration.between(start, Instant.now());
			logger.info("Backup execution completed in {}", TimeFormatter.formatDuration(duration));

		} catch (IOException e) {
			logger.error("Configuration error: {}", e.getMessage(), e);
			exceptionCaught = true;
		} catch (Exception e) {
			logger.error("Backup failed: {}", e.getMessage(), e);
			exceptionCaught = true;
		} finally {
			if (notificationService != null) {
				logger.debug("Sending notification with {} failed files",
						MultiThreadedFileCopier.failedFiles.size());
				notificationService.sendFailedFilesNotification(MultiThreadedFileCopier.failedFiles, duration);
				logger.info("Backup completed in {}.", TimeFormatter.formatDuration(duration));
				logger.info("Backup stored at: {}", versionedBackupDir);
			} else {
				logger.warn("Notification service not initialized, skipping notification");
			}
			if (exceptionCaught) {
				logger.error("Backup failed. See logs for details");
			}
			logger.info("====================== Backup Process Completed ======================");
		}
	}
}