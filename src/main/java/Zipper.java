import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Zipper {

	public Zipper() {

	}

	public String zip(File source) throws ZipException, IOException {
		Path tempDir = Files.createTempDirectory("gfs");

		String outputFile = tempDir.toString() + "/" + source.getName() + ".zip";

		ZipFile file = new ZipFile(outputFile);

		long maxBytes = Main.MAX_SIZE*3;

		file.createZipFile(source, null, true, maxBytes);

		return outputFile;
	}

	public void unzip(String sourceDir) throws ZipException {
		ZipFile file = new ZipFile(sourceDir);

		file.extractAll(System.getProperty("user.dir"));
	}
}
