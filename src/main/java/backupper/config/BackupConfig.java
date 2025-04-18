package backupper.config;

import backupper.utils.LoggingUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Configuration class for the backup application.
 * Handles loading and validating configuration from JSON files and provides
 * access to configuration properties.
 */
public class BackupConfig {

	private final LoggingUtil logger;

	@JsonProperty("sourceDir")
	@SuppressWarnings("unused")
	private Path sourceDir;

	@JsonProperty("destinationDir")
	@SuppressWarnings("unused")
	private Path destinationDir;

	private Path versionedBackupDir;

	@JsonProperty("maxBackupsToKeep")
	private final int maxBackupsToKeep; // Default value of 5

	@JsonProperty("discordWebhookUrl")
	@SuppressWarnings("unused")
	private String discordWebhookUrl;

	@JsonProperty("discordUserId")
	@SuppressWarnings("unused")
	private long discordUserId;

	@JsonProperty("threadPoolSize")
	private final int threadPoolSize; // Default to available processors

	@JsonProperty("exclusionPatterns")
	private final List<String> exclusionPatterns = new ArrayList<>();

	@JsonProperty("maxRetries")
	private final int maxRetries; // Default value of 3

	@JsonProperty("retryDelayMs")
	private final int retryDelayMs; // Default value of 1000ms

	/**
	 * Default constructor used by Jackson for JSON deserialization.
	 * Sets default values for configuration properties.
	 */
	public BackupConfig() {
		this.logger = new LoggingUtil(this.getClass());
		logger.debug("Creating BackupConfig with default values");
		maxBackupsToKeep = 5;
		threadPoolSize = Runtime.getRuntime().availableProcessors();
		maxRetries = 3;
		retryDelayMs = 1000;
	}

	/**
	 * Gets the source directory for the backup.
	 *
	 * @return The source directory path
	 */
	public Path getSourceDir() {
		logger.debug("Getting source directory: {}", sourceDir);
		return sourceDir;
	}

	/**
	 * Gets the destination directory for backups.
	 *
	 * @return The destination directory path
	 */
	public Path getDestinationDir() {
		logger.debug("Getting destination directory: {}", destinationDir);
		return destinationDir;
	}

	/**
	 * Gets the versioned backup directory for the current backup operation.
	 *
	 * @return The versioned backup directory path
	 */
	public Path getVersionedBackupDir() {
		logger.debug("Getting versioned backup directory: {}", versionedBackupDir);
		return versionedBackupDir;
	}

	/**
	 * Gets the maximum number of backups to keep.
	 *
	 * @return The maximum number of backups to keep
	 */
	public int getMaxBackupsToKeep() {
		return maxBackupsToKeep;
	}

	/**
	 * Gets the Discord webhook URL for notifications.
	 *
	 * @return The Discord webhook URL
	 */
	public String getDiscordWebhookUrl() {
		return discordWebhookUrl;
	}

	/**
	 * Gets the Discord user ID for mentions.
	 *
	 * @return The Discord user ID
	 */
	public long getDiscordUserId() {
		return discordUserId;
	}

	/**
	 * Gets the number of threads to use for file operations.
	 *
	 * @return The thread pool size
	 */
	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	/**
	 * Gets the patterns for files/directories to exclude from backup.
	 *
	 * @return The list of exclusion patterns
	 */
	public List<String> getExclusionPatterns() {
		return exclusionPatterns;
	}

	/**
	 * Gets the maximum number of retry attempts for failed operations.
	 *
	 * @return The maximum number of retries
	 */
	public int getMaxRetries() {
		return maxRetries;
	}

	/**
	 * Gets the delay in milliseconds between retry attempts.
	 *
	 * @return The retry delay in milliseconds
	 */
	public int getRetryDelayMs() {
		return retryDelayMs;
	}

	/**
	 * Generates a versioned backup directory with a timestamp.
	 * Creates the directory if it doesn't exist.
	 *
	 * @throws RuntimeException If the directory cannot be created, or if the destination directory is not set
	 */
	public void generateVersionedBackupDir() {
		logger.info("Generating versioned backup directory");
		String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmm_ss").format(new Date());
		this.versionedBackupDir = destinationDir.resolve("docker-backup_" + timestamp);
		logger.debug("Generated versioned backup path: {}", versionedBackupDir);

		try {
			if (!Files.exists(versionedBackupDir)) {
				Files.createDirectories(versionedBackupDir);
				logger.debug("Created versioned backup directory: {}", versionedBackupDir);
			}
		} catch (IOException e) {
			logger.error("Failed to create versioned backup directory: {}", e.getMessage());
			throw new RuntimeException("Could not create versioned backup directory", e);
		} catch (NullPointerException e) {
			logger.error("Destination directory is not set. Please set the 'destinationDir' property in the configuration file.");
			throw new RuntimeException("Destination directory is not set", e);
		}
	}

	/**
	 * Loads configuration from a JSON file.
	 *
	 * @param resourcePath The path to the JSON configuration file
	 * @return The loaded BackupConfig object
	 * @throws IOException If the configuration file cannot be found or parsed
	 */
	public static BackupConfig loadFromJson(String resourcePath) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		LoggingUtil logger = new LoggingUtil(BackupConfig.class);
		logger.info("Loading configuration from: {}", resourcePath);

		// First, try to load from the current directory (where the JAR is located)
		InputStream inputStream = null;
		try {
			Path currentDir = Path.of(System.getProperty("user.dir"));
			Path configPath = currentDir.resolve(resourcePath);
			logger.debug("Trying to load config from filesystem: {}", configPath);
			if (Files.exists(configPath)) {
				logger.info("Found configuration file in current directory: {}", configPath);
				inputStream = Files.newInputStream(configPath);
			}
		} catch (Exception e) {
			logger.warn("Failed to load config from filesystem: {}", e.getMessage());
		}

		// If not found in filesystem, try to load from classpath resources
		if (inputStream == null) {
			logger.debug("Trying to load config from classpath resources");
			inputStream = BackupConfig.class.getClassLoader().getResourceAsStream(resourcePath);
		}

		if (inputStream == null) {
			logger.error("Configuration file not found: {}", resourcePath);
			throw new IOException("ERROR: Configuration file '" + resourcePath + "' not found in resources. "
					+ "Make sure 'config.json' is inside 'src/main/resources/' or in same directory as the executable jar.");
		}

		try {
			logger.debug("Parsing JSON configuration");
			BackupConfig config = objectMapper.readValue(inputStream, BackupConfig.class);
			logger.info("Configuration loaded successfully");

			// Validate directories
			validatePaths(config, logger);

			// Generate versioned backup dir
			config.generateVersionedBackupDir();

			return config;
		} catch (Exception e) {
			logger.error("Failed to parse JSON configuration: {}", e.getMessage());
			throw new IOException("ERROR: Failed to parse JSON. Check 'config.json' for syntax errors.\n" + e.getMessage());
		}
	}

	/**
	 * Validates the source and destination directories.
	 * Creates the destination directory if it doesn't exist.
	 *
	 * @param config The BackupConfig object containing the paths to validate
	 * @param logger The logger to use for logging
	 * @throws IOException If the paths are invalid or cannot be created
	 */
	private static void validatePaths(BackupConfig config, LoggingUtil logger) throws IOException {
		logger.info("Validating source and destination directories");
		Path sourcePath = config.getSourceDir();
		Path destPath = config.getDestinationDir();

		logger.debug("Validating source directory: {}", sourcePath);
		if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
			logger.error("Source directory invalid: {}", sourcePath);
			throw new IOException("ERROR: Source directory does not exist or is not a directory: " + sourcePath);
		}

		logger.debug("Validating destination directory: {}", destPath);
		if (!Files.exists(destPath)) {
			Files.createDirectories(destPath);
			logger.info("Created missing destination directory: {}", destPath);
		} else if (!Files.isDirectory(destPath)) {
			logger.error("Destination path exists but is not a directory: {}", destPath);
			throw new IOException("ERROR: Destination path exists but is not a directory: " + destPath);
		}

		logger.info("Source and destination directories are valid.");
	}
}