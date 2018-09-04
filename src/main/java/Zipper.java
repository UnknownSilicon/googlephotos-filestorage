import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Zipper {

	public Zipper() {

	}

	public String zip(File source) throws ZipException, IOException {
		Path tempDir = Files.createTempDirectory("gfs");

		String outputDir = tempDir.toString();

		ZipFile file = new ZipFile(outputDir + "/" + source.getName() + ".zip");

		long maxBytes = Main.MAX_SIZE*3-20;

		file.createZipFile(source, new ZipParameters(), true, maxBytes);

		return outputDir;
	}

	public void unzip(String sourceDir) throws ZipException {
		ZipFile file = new ZipFile(sourceDir);

		file.extractAll(System.getProperty("user.dir"));
	}
}
