package backupper.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Utility class for managing and cleaning up old backup directories.
 * Maintains a specified maximum number of backups by removing the oldest ones.
 */
public class BackupCleaner {

	private final LoggingUtil logger;

	private final Path backupBaseDir;
	private final int maxBackups;

	/**
	 * Creates a new BackupCleaner instance.
	 *
	 * @param backupBaseDir The base directory containing backup folders
	 * @param maxBackups    The maximum number of backups to keep
	 */
	public BackupCleaner(Path backupBaseDir, int maxBackups) {
		this.logger = new LoggingUtil(this.getClass());
		this.backupBaseDir = backupBaseDir;
		this.maxBackups = maxBackups;
		logger.info("Initialized BackupCleaner with base directory: {} and max backups: {}", backupBaseDir, maxBackups);
	}

	/**
	 * Cleans up old backup directories, keeping only the most recent ones up to the maximum specified.
	 *
	 * @return A boolean array where:
	 * - index 0: true if any backups were deleted, false otherwise
	 * - index 1: true if any deletion operations failed, false otherwise
	 */
	public boolean[] cleanupOldBackups() {
		logger.info("Starting cleanup of old backups in {}", backupBaseDir);
		boolean[] backupDeleted = new boolean[]{false};

		try (Stream<Path> stream = Files.list(backupBaseDir)) {
			Path[] backups = stream
					.filter(Files::isDirectory) // Only keep directories
					.sorted(Comparator.comparingLong(this::getLastModifiedTime)) // Sort by the last modified time (oldest first)
					.toArray(Path[]::new);

			logger.debug("Found {} backup directories", backups.length);

			if (backups.length > maxBackups) {
				int backupsToDelete = backups.length - maxBackups;
				logger.info("Deleting {} old backups...", backupsToDelete);

				for (int i = 0; i < backupsToDelete; i++) {
					backupDeleted = deleteBackup(backups[i]);
				}
			} else {
				logger.info("No backups need to be deleted. Current count ({}) is within limit ({})",
						backups.length, maxBackups);
			}

		} catch (IOException e) {
			logger.error("Error while cleaning up backups: {}", e.getMessage());
		}

		logger.info("Backup cleanup completed");
		return backupDeleted;
	}

	/**
	 * Gets the last modified time of a path.
	 *
	 * @param path The path to check
	 * @return The last modified time in milliseconds, or Long.MAX_VALUE if an error occurs
	 */
	private long getLastModifiedTime(Path path) {
		logger.debug("Getting last modified time for {}", path);
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			logger.error("Failed to get last modified time for {}: {}", path, e.getMessage());
			return Long.MAX_VALUE;
		}
	}

	/**
	 * Deletes a backup directory and all its contents.
	 *
	 * @param backupDir The backup directory to delete
	 * @return A boolean array where:
	 * - index 0: true if any files were deleted, false otherwise
	 * - index 1: true if any deletion operations failed, false otherwise
	 */
	private boolean[] deleteBackup(Path backupDir) {
		logger.info("Attempting to delete backup directory: {}", backupDir);
		AtomicBoolean backupDeleted = new AtomicBoolean(false);
		AtomicBoolean backupDeletionFailed = new AtomicBoolean(false);

		try (Stream<Path> paths = Files.walk(backupDir)) {
			paths.sorted(Comparator.reverseOrder()) // Delete files first, then directories
					.forEach(path -> {
						try {
							Files.delete(path);
							logger.debug("Deleted file: {}", path);
							backupDeleted.set(true);
						} catch (IOException e) {
							logger.error("Failed to delete {}: {}", path, e.getMessage());
							backupDeletionFailed.set(true);
						}
					});

			logger.info("Deleted old backup: {}", backupDir);

		} catch (IOException e) {
			logger.error("Error deleting backup {}: {}", backupDir, e.getMessage());
		}
		return new boolean[]{backupDeleted.get(), backupDeletionFailed.get()};
	}
}