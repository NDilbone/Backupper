package backupper.services;

import backupper.utils.LoggingUtil;
import backupper.utils.TimeFormatter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for sending notifications about backup operations to Discord.
 * Uses Discord webhooks to send formatted messages with information about
 * backup completion status, duration, and any failed files.
 */
public class NotificationService {

	private final LoggingUtil logger;

	private final String webhookUrl;
	private final long discordUserId;

	/**
	 * Creates a new NotificationService with the specified webhook URL and user ID.
	 *
	 * @param webhookUrl    The Discord webhook URL
	 * @param discordUserId The Discord user ID to mention in notifications
	 */
	public NotificationService(String webhookUrl, long discordUserId) {
		this.logger = new LoggingUtil(this.getClass());
		this.webhookUrl = webhookUrl;
		this.discordUserId = discordUserId;
		logger.debug("Initialized NotificationService with webhook URL and user ID: {}", discordUserId);
	}

	/**
	 * Sends a simple notification message to Discord.
	 *
	 * @param message The message to send
	 */
	public void sendNotification(String message) {
		logger.debug("Sending simple notification: {}", message);
		String jsonPayload = createEmbedPayload(message);
		sendToDiscord(jsonPayload);
	}

	/**
	 * Sends a notification about failed files during the backup process.
	 * If no files failed, sends a success notification.
	 *
	 * @param failedFiles List of files that failed to copy
	 * @param duration    The duration of the backup operation
	 */
	public void sendFailedFilesNotification(List<Path> failedFiles, Duration duration) {
		logger.debug("Sending failed files notification with {} failed files",
				failedFiles != null ? failedFiles.size() : 0);

		if (failedFiles == null || failedFiles.isEmpty()) {
			logger.debug("No failed files found, sending success notification");
			sendNotification("- All files copied successfully!\n- Backup took **" + TimeFormatter.formatDuration(duration) + "**");
			return;
		}

		// Format the failed files list
		logger.debug("Formatting {} failed files for notification", failedFiles.size());
		String failedFilesMessage = failedFiles.stream()
				.map(Path::toString)
				.collect(Collectors.joining("\n")); // Use double backslash for newlines

		// Ensure a message does not exceed Discord's 1024-character field limit
		if (failedFilesMessage.length() > 1024) {
			logger.debug("Failed files message exceeds Discord limit, truncating");
			failedFilesMessage = failedFilesMessage.substring(0, 1021) + "..."; // Truncate if too long
		}

		String jsonPayload = createEmbedWithField(
				"- **" + failedFiles.size() + "** file(s) failed to copy\n- Backup took **" + TimeFormatter.formatDuration(duration) + "**",
				failedFilesMessage
				// Red color for errors
		);

		sendToDiscord(jsonPayload);
	}

	/**
	 * Sends a JSON payload to Discord via webhook.
	 *
	 * @param jsonPayload The JSON payload containing the embed data
	 */
	private void sendToDiscord(String jsonPayload) {
		logger.debug("Sending payload to Discord webhook");
		try {
			URL url = new URL(webhookUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			String fullJsonPayload = "{"
					+ "\"content\": \"<@" + discordUserId + ">\","
					+ "\"embeds\": " + jsonPayload
					+ "}";

			logger.debug("Full JSON Payload:\n{}", fullJsonPayload);

			// Send JSON payload as is
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(fullJsonPayload.getBytes(StandardCharsets.UTF_8));
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == 204) {
				logger.info("Notification sent successfully");
			} else {
				logger.warn("Failed to send notification. HTTP Response Code: {}", responseCode);
			}
		} catch (Exception e) {
			logger.error("Error sending Discord notification: {}", e.getMessage(), e);
		}
	}

	/**
	 * Creates a JSON payload for a Discord embed with title and description.
	 *
	 * @param description The description of the embed
	 * @return A JSON string representing the embed
	 */
	private String createEmbedPayload(String description) {
		logger.debug("Creating embed payload with title: {}", "Backup Completed");
		return "[{"
				+ "\"title\": " + escapeJson("Backup Completed") + ","
				+ "\"description\": " + escapeJson(description) + ","
				+ "\"color\": " + 65280 + ","
				+ "\"timestamp\": " + escapeJson(Instant.now().toString()) + ","
				+ "\"footer\": {"
				+ "\"text\": \"Backup completed\""
				+ "}"
				+ "}]";
	}

	/**
	 * Creates a JSON payload for a Discord embed with title, description, and a field.
	 *
	 * @param description The description of the embed
	 * @param fieldValue  The value of the field
	 * @return A JSON string representing the embed
	 */
	private String createEmbedWithField(String description, String fieldValue) {
		logger.debug("Creating embed with field. Title: {}, Field: {}", "Backup Completed with Errors", "Failed Files");
		return "[{"
				+ "\"title\": " + escapeJson("Backup Completed with Errors") + ","
				+ "\"description\": " + escapeJson(description) + ","
				+ "\"color\": " + 16711680 + ","
				+ "\"fields\": [{"
				+ "\"name\": " + escapeJson("Failed Files") + ","
				+ "\"value\": " + escapeJson(fieldValue) + ","
				+ "\"inline\": false"
				+ "}],"
				+ "\"timestamp\": " + escapeJson(Instant.now().toString()) + ","
				+ "\"footer\": {"
				+ "\"text\": \"Backup completed\""
				+ "}"
				+ "}]";
	}

	/**
	 * Escapes special characters in a string for use in JSON.
	 *
	 * @param text The text to escape
	 * @return The escaped text, surrounded by quotes
	 */
	private String escapeJson(String text) {
		return "\"" + text
				.replace("\\", "\\\\")  // Escape backslashes
				.replace("\"", "\\\"")   // Escape quotes
				.replace("\n", "\\n")    // Escape newlines
				+ "\"";
	}
}