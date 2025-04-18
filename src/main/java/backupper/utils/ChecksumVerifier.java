package backupper.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for verifying file integrity by comparing checksums.
 * Uses SHA-256 algorithm to compute file checksums and compare them
 * to ensure files have been copied correctly.
 */
public class ChecksumVerifier {

	private final LoggingUtil logger;

	/**
	 * Creates a new ChecksumVerifier instance.
	 */
	public ChecksumVerifier() {
		this.logger = new LoggingUtil(this.getClass());
		logger.debug("ChecksumVerifier initialized");
	}

	/**
	 * Verifies the integrity of a copied file by comparing its checksum with the original file.
	 * Directories are skipped and considered valid.
	 *
	 * @param originalFile The original source file
	 * @param copiedFile   The copied file to verify
	 * @return true if the checksums match or if both paths are directories, false otherwise
	 */
	public boolean verifyFileIntegrity(Path originalFile, Path copiedFile) {
		logger.debug("Verifying file integrity: {} -> {}", originalFile, copiedFile);

		try {
			if (Files.isDirectory(originalFile) || Files.isDirectory(copiedFile)) {
				logger.debug("Skipping directory checksum verification");
				return true; // Skip directories
			}

			logger.debug("Computing checksum for original file: {}", originalFile);
			String originalChecksum = computeSHA256(originalFile);

			logger.debug("Computing checksum for copied file: {}", copiedFile);
			String copiedChecksum = computeSHA256(copiedFile);

			if (originalChecksum.equals(copiedChecksum)) {
				logger.debug("File checksum verified: {}", copiedFile.getFileName());
				return true;
			} else {
				logger.warn("Checksum mismatch: {} (original) != {} (copied)", originalChecksum, copiedChecksum);
				return false;
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			logger.error("Error computing checksum for file {}: {}", originalFile.getFileName(), e.getMessage());
			return false;
		}
	}

	/**
	 * Computes the SHA-256 checksum of a file.
	 * Uses streaming to efficiently handle large files.
	 *
	 * @param file The file to compute the checksum for
	 * @return The SHA-256 checksum as a hexadecimal string
	 * @throws IOException              If an I/O error occurs while reading the file
	 * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available
	 */
	private String computeSHA256(Path file) throws IOException, NoSuchAlgorithmException {
		logger.debug("Computing SHA-256 checksum for: {}", file);
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		// Use streaming to handle large files efficiently
		try (InputStream is = Files.newInputStream(file)) {
			byte[] buffer = new byte[8192]; // 8KB buffer
			int bytesRead;
			long totalBytesRead = 0;

			while ((bytesRead = is.read(buffer)) != -1) {
				digest.update(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
			}

			logger.debug("Read {} bytes from file: {}", totalBytesRead, file);
		}

		byte[] hash = digest.digest();

		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			hexString.append(String.format("%02x", b));
		}

		String checksum = hexString.toString();
		logger.debug("Computed checksum: {}", checksum);
		return checksum;
	}
}