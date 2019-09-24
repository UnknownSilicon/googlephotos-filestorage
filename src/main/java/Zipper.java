import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class Zipper {

	Zipper() {

	}

	 String zip(File source) throws ZipException, IOException {
		Path tempDir = Files.createTempDirectory("gfs");

		String outputDir = tempDir.toString();

		ZipFile file = new ZipFile(outputDir + "/" + source.getName() + ".zip");

		long maxBytes = GooglePhotosFileStorage.MAX_SIZE*3-20;

		if (source.isDirectory()) {
			file.createZipFileFromFolder(source, new ZipParameters(), true, maxBytes);
		} else {
			file.createZipFile(source, new ZipParameters(), true, maxBytes);
		}



		System.gc();

		return outputDir;
	}

	 void unzip(File source) throws ZipException {
		ZipFile file = new ZipFile(source);

		String outputDir = System.getProperty("user.dir");

		file.extractAll(outputDir);

		System.gc();

	}
}
