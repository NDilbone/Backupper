package backupper.utils;

import java.time.Duration;

/**
 * Utility class for formatting time durations into human-readable strings.
 * Provides methods to convert Duration objects into formatted strings with
 * appropriate time units (minutes, seconds, milliseconds).
 */
public class TimeFormatter {

	/**
	 * Formats a Duration object into a human-readable string.
	 * The format will include minutes, seconds, and milliseconds as appropriate:
	 * - Minutes and seconds are always shown if non-zero
	 * - Milliseconds are only shown if both minutes and seconds are zero
	 * - Singular/plural forms are used correctly (e.g., "1 minute" vs. "2 minutes")
	 * - If the duration is zero, returns "0 seconds"
	 *
	 * @param duration The Duration object to format
	 * @return A formatted string representation of the duration
	 */
	public static String formatDuration(Duration duration) {
		long seconds = duration.getSeconds();
		long minutes = seconds / 60;
		long remainingSeconds = seconds % 60;
		long millis = duration.toMillisPart();

		StringBuilder result = new StringBuilder();

		if (minutes > 0) {
			result.append(minutes).append(minutes == 1 ? " minute" : " minutes");
		}
		if (remainingSeconds > 0) {
			if (!result.isEmpty()) result.append(", ");
			result.append(remainingSeconds).append(remainingSeconds == 1 ? " second" : " seconds");
		}
		if (millis > 0 && minutes == 0 && remainingSeconds == 0) {
			// Only show milliseconds if both minutes and seconds are zero
			result.append(millis).append(millis == 1 ? " millisecond" : " milliseconds");
		}

		return result.isEmpty() ? "0 seconds" : result.toString();
	}
}