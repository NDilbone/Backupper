package backupper.defaults;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for file copying operations.
 * Implementations of this interface handle the copying of files from a source directory to a destination directory.
 */
public interface ICopier {

	/**
	 * Copies files from the source directory to the destination directory.
	 *
	 * @param sourceDir      The source directory containing files to be copied
	 * @param destinationDir The destination directory where files will be copied to
	 * @throws IOException If an I/O error occurs during the copying process
	 */
	void copyFiles(Path sourceDir, Path destinationDir) throws IOException;
}