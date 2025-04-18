package backupper.services;

import backupper.utils.LoggingUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Processes directories during backup operations by traversing the directory structure,
 * applying exclusion patterns, and delegating file copying tasks to a FileCopier.
 * This class handles the recursive traversal of directories and ensures that
 * destination directories are created as needed.
 */
public class DirectoryProcessor {

	private final LoggingUtil logger;

	private final FileCopier fileCopier;
	private final List<Pattern> exclusionPatterns;

	/**
	 * Constructs a DirectoryProcessor with a FileCopier and a list of exclusion patterns.
	 *
	 * @param fileCopier              The FileCopier instance used to handle file copying operations
	 * @param exclusionPatternStrings List of regex patterns as strings that define which files/directories to exclude
	 */
	public DirectoryProcessor(FileCopier fileCopier, List<String> exclusionPatternStrings) {
		this.logger = new LoggingUtil(this.getClass());
		this.fileCopier = fileCopier;
		this.exclusionPatterns = new ArrayList<>();

		// Compile all exclusion patterns
		if (exclusionPatternStrings != null) {
			for (String pattern : exclusionPatternStrings) {
				try {
					this.exclusionPatterns.add(Pattern.compile(pattern));
					logger.debug("Added exclusion pattern: {}", pattern);
				} catch (Exception e) {
					logger.warn("Invalid exclusion pattern: {}", pattern, e);
				}
			}
		}
	}

	/**
	 * Processes a directory by creating the destination directory if it doesn't exist
	 * and then iterating through all entries in the source directory.
	 * For each entry, it checks if it should be excluded based on the exclusion patterns.
	 * If not excluded, it either recursively processes subdirectories or submits files for copying.
	 *
	 * @param sourceDir      The source directory to process
	 * @param destinationDir The destination directory where files will be copied
	 * @throws IOException If an I/O error occurs during directory creation or traversal
	 */
	public void processDirectory(Path sourceDir, Path destinationDir) throws IOException {
		if (!Files.exists(destinationDir)) {
			Files.createDirectories(destinationDir);
			logger.debug("Created directory: {}", destinationDir);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
			for (Path entry : stream) {
				// Check if the file/directory should be excluded
				if (shouldExclude(entry)) {
					logger.info("Excluding: {}", entry);
					continue;
				}

				Path newDest = destinationDir.resolve(entry.getFileName());
				if (Files.isDirectory(entry)) {
					processDirectory(entry, newDest);
				} else {
					fileCopier.submitFileForCopy(entry, newDest);
				}
			}
		}
	}

	/**
	 * Determines whether a file or directory should be excluded from the backup process
	 * based on the configured exclusion patterns.
	 *
	 * @param path The path to check against exclusion patterns
	 * @return true if the path should be excluded, false otherwise
	 */
	private boolean shouldExclude(Path path) {
		if (exclusionPatterns.isEmpty()) {
			return false;
		}

		String pathStr = path.toString();
		for (Pattern pattern : exclusionPatterns) {
			if (pattern.matcher(pathStr).matches()) {
				return true;
			}
		}
		return false;
	}
}