package com.jakebacker.gpfs;

import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.types.proto.Album;
import net.lingala.zip4j.exception.ZipException;
import com.jakebacker.gpfs.photosAPI.DuplicateNameException;
import com.jakebacker.gpfs.photosAPI.Photos;
import com.jakebacker.gpfs.utility.Checksum;
import com.jakebacker.gpfs.utility.FastRGB;
import com.jakebacker.gpfs.utility.StringUtils;
import com.jakebacker.gpfs.utility.UpdateHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class GooglePhotosFileStorage {

	static final int MAX_SIZE = 16000000; // In pixels

	private static Photos photos;

	public static void main(String[] args) throws ZipException, NoSuchAlgorithmException, DuplicateNameException, InterruptedException, IOException {
		//GooglePhotosFileStorage gpfs = new GooglePhotosFileStorage();
	}

	public GooglePhotosFileStorage() {
		UpdateHandler updateHandler = new UpdateHandler();
		updateHandler.checkVersion();

		try {
			photos = new Photos();
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private static Album getAlbumFromInput(String input) throws DuplicateNameException{
		Album album = null;

		if (input.startsWith("*")) {
			album = photos.getExistingAlbumFromId(input.substring(1)); // Remove the *
		} else {
			album = photos.getExistingAlbum(input);
		}
		if (album==null) {
			System.out.println("Album does not exist! Try again");
		}
		return album;
	}

	/**
	 * Downloads a file from google photos
	 * @param name The file name or id including *
	 * @param outputDir The directory to output the file to
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws ZipException
	 * @throws InterruptedException
	 * @throws DuplicateNameException
	 */
	public void download(String name, File outputDir) throws IOException, NoSuchAlgorithmException, ZipException, InterruptedException, DuplicateNameException {
		Album album = getAlbumFromInput(name);

		long startTime = System.nanoTime();

		File dir;

		if (album == null) {
			System.out.println("File does not exist!");
			return;
		} else {
			dir = photos.downloadFiles(album);

			System.out.println("Successfully Downloaded Files!");
		}


		File[] files = dir.listFiles((dir1, n) -> {
			String lower = n.toLowerCase();

			boolean pngEnd = lower.endsWith(".png");

			if (pngEnd) {
				return true;
			}

			return false;
		});

		assert files != null;
		for (File f : files) {

			if (!f.exists()) {
				break;
			}

			File tempOutput = new File(dir + File.separator + f.getName().substring(0, f.getName().lastIndexOf("." + StringUtils.getFileExtension(f))));

			//FileInputStream fis = new FileInputStream(f);

			BufferedImage image = ImageIO.read(f);

			FastRGB fastRGB = new FastRGB(image);
			//fis.close();

			int size = image.getHeight() * image.getWidth();
			byte[] data = new byte[size * 3];

			int dataNum = 0;

			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					byte[] pixel = fastRGB.getRGB(x, y);

					data[dataNum] = pixel[0];
					data[dataNum + 1] = pixel[1];
					data[dataNum + 2] = pixel[2];

					dataNum += 3;
				}
			}

			byte[] checksum;

			int firstIndex = 0;
			int lastIndex = 0;


			int currentIndex = data.length - 1;
			while (lastIndex == 0) {
				if (data[currentIndex--] != 0) {
					lastIndex = currentIndex;
					firstIndex = currentIndex - 19;
				}
			}

			/* TODO: Figure out a better way to detect checksum | See below | The current "solution" will work for all files except for those with a perfect square image and the checksum ends in "255 255 255" or "FF FF FF"
			 */
			if ((data[lastIndex - 1] & 0xFF) == 255 && (data[lastIndex] & 0xFF) == 255 && (data[lastIndex + 1] & 0xFF) == 255) {
				// If the last three bytes are 255, then you are in the right place
				firstIndex -= 3;
			} else {
				/*lastIndex = data.length;
				firstIndex = lastIndex - 19;*/
			}

			checksum = Arrays.copyOfRange(data, firstIndex + 1, firstIndex + 21);

			byte[] newData = new byte[firstIndex + 1];

			for (int i = 0; i < firstIndex + 1; i++) {
				newData[i] = data[i];
			}

			// File should be encoded in data

			FileOutputStream fos = new FileOutputStream(tempOutput);

			fos.write(newData);
			fos.close();
			System.gc();

			byte[] newChecksum = Checksum.getFileChecksum(tempOutput);

			if (Arrays.equals(checksum, newChecksum)) {

				System.out.println("Checksum Valid");

			} else {
				System.out.println("Checksum invalid");

				System.out.println("Old Checksum: ");
				for (byte b : checksum) {
					System.out.print(b + " ");
				}

				System.out.println("\nNew Checksum");
				for (byte b : newChecksum) {
					System.out.print(b + " ");
				}
				System.out.println();
			}
		}

		Zipper zipper = new Zipper();

		try {
			File sourceFile = new File(files[files.length - 1].getAbsolutePath().substring(0, files[0].getAbsolutePath().lastIndexOf("." + StringUtils.getFileExtension(files[0]))));
			zipper.unzip(sourceFile, outputDir);

		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("File does not exist!");
			e.printStackTrace();
			System.exit(1);
		} catch (ZipException e) {
			System.out.println("Unable to extract");
		}

		/*File cwd = new File(System.getProperty("user.dir"));

		File[] delFiles = cwd.listFiles((dir12, name1) -> {
			String noPng = files[files.length - 1].getName().substring(0, files[0].getName().lastIndexOf("." + StringUtils.getFileExtension(files[0])));
			String noZip = noPng.substring(0, noPng.lastIndexOf("."));
			String lastThree = name1.substring(name1.length() - 3);

			*//*if (name.equals(noZip + "." + lastThree) & lastThree.charAt(0)=='z') {
				return true;
			}
			return false;*//*

			return name1.contains(noZip) && !name1.equals(noZip) && (lastThree.equals("png") || lastThree.startsWith("z"));

		});

		System.gc();
		assert delFiles != null;
		for (File f : delFiles) {

			forceDelete(f);
		}*/

		long endTime = System.nanoTime();

		double deltaTime = (double) (endTime - startTime) / 1000000000.0;

		System.out.println("Decoded in: " + deltaTime + " seconds");
	}

	/**
	 * Returns a list of file names on google photos
	 * @return The list of file names
	 */
	public ArrayList<String> listFiles() {
		return photos.getFileNames();
	}

	/**
	 * Upload a file to google photos
	 * @param file The file to upload
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws ZipException
	 * @throws InterruptedException
	 */
	public void upload(File file) throws IOException, NoSuchAlgorithmException, ZipException, InterruptedException { // This looks ready
		long startTime = System.nanoTime();
		if (file == null || !file.exists()) {
			System.out.println("File does not exist!");
			System.exit(1);
		}

		Zipper zipper = new Zipper();

		String zipDir = zipper.zip(file);
		File directory = new File(zipDir);

		Path tempDir = Files.createTempDirectory("gfs");

		String outputDir = tempDir.toString();

		int imgNum = 0;

		ArrayList<NewMediaItem> mediaItems = new ArrayList<>();

		Album album = photos.getAlbum(file.getName());

		File[] files = Objects.requireNonNull(directory.listFiles());

		for (File f : files) {
			if (f.isFile()) {

				FileInputStream fis = new FileInputStream(f);

				long size = f.length();

				byte[] checksum = Checksum.getFileChecksum(f);

				size += checksum.length;

				int closestSquare = (int) Math.ceil(Math.sqrt(Long.divideUnsigned(size, 3L)));

				ImageCreator ic = new ImageCreator(closestSquare, closestSquare);

				int pixelNum = 0;

				byte[] byteArray = new byte[4096];
				byte[] tempBytes = new byte[4096];
				byte[] tempStorage = new byte[0];
				int bytesCount = 0;
				int previousBytesCount = 0;
				long originalByteCount = 0;

				while ((bytesCount = fis.read(tempBytes)) != -1) {

					byteArray = tempBytes;

					previousBytesCount = bytesCount;
					originalByteCount += bytesCount;

					byteArray = Arrays.copyOfRange(byteArray, 0, bytesCount); // Artificially shrink array

					for (int i = 0; i < bytesCount; i += 3) {
						int row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
						int col = (pixelNum - closestSquare * row);

						try {
							if (tempStorage.length == 1) {
								ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF,
										byteArray[i] & 0xFF, byteArray[i + 1] & 0xFF));
								i--;
								tempStorage = new byte[0];
							} else if (tempStorage.length == 2) {
								ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF,
										tempStorage[1] & 0xFF, byteArray[i] & 0xFF));
								i -= 2;
								tempStorage = new byte[0];
							} else {
								ic.drawPixel(row, col, new Color(byteArray[i] & 0xFF,
										byteArray[i + 1] & 0xFF, byteArray[i + 2] & 0xFF));
							}

						} catch (ArrayIndexOutOfBoundsException e) {
							// This means that the number of bytes that was read is not divisible by 3, this will put it into multiple pixels

							if (tempStorage.length != 0) {
								System.out.println("Something broke");
							}

							tempStorage = Arrays.copyOfRange(byteArray, i, byteArray.length); // Copy up to the last two bytes
							pixelNum--; // Counteract the ++
						}
						pixelNum++;
					}

				}


				tempStorage = Arrays.copyOfRange(byteArray, Math.toIntExact(previousBytesCount - (originalByteCount % 3L)), previousBytesCount);

				if (tempStorage.length >= 3) {
					System.out.println("Something else broke");
				}

				for (int i = 0; i < checksum.length; i += 3) {
					int row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
					int col = (pixelNum - closestSquare * row);

					if (tempStorage.length == 1) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF, checksum[i] & 0xFF, checksum[i + 1] & 0xFF));
						tempStorage = new byte[0];
						i--;
					} else if (tempStorage.length == 2) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF, tempStorage[1] & 0xFF, checksum[i] & 0xFF));
						tempStorage = new byte[0];
						i -= 2;
					} else {
						try {
							ic.drawPixel(row, col, new Color(checksum[i] & 0xFF, checksum[i + 1] & 0xFF, checksum[i + 2] & 0xFF));
						} catch (ArrayIndexOutOfBoundsException e) {

							if (tempStorage.length != 0) {
								System.out.println("Something broke more");
							}

							tempStorage = Arrays.copyOfRange(checksum, i, checksum.length);
							pixelNum--;
						}
					}
					pixelNum++;
				}

				pixelNum++;
				int row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
				int col = (pixelNum - closestSquare * row);

				if (row < closestSquare && col < closestSquare) { // If there is still room left
					pixelNum--;

					row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
					col = (pixelNum - closestSquare * row);

					if (tempStorage.length == 1) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF, 255, 255));
						pixelNum++;
						row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
						col = (pixelNum - closestSquare * row);
						ic.drawPixel(row, col, new Color(255, 0, 0));
					} else if (tempStorage.length == 2) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF, tempStorage[1] & 0xFF, 255));
						pixelNum++;
						row = (pixelNum - (pixelNum % closestSquare)) / closestSquare;
						col = (pixelNum - closestSquare * row);
						ic.drawPixel(row, col, new Color(255, 255, 0));
					}
				}

				fis.close();

				File outputFile = new File(outputDir + File.separator + f.getName() + ".png");
				ImageIO.write(ic.getImage(), "png", outputFile);

				System.gc();

				NewMediaItem item = photos.uploadImage(outputFile);

				mediaItems.add(item);

				System.out.println();

				imgNum++;
			}

		}

		photos.processUploads(mediaItems, album);

		// Remove files

		/*File dir = new File(System.getProperty("user.dir")); // This is cheaty

		File[] delFiles = dir.listFiles((dir1, name) -> name.endsWith(".png") && name.startsWith(file.getName()));

		System.gc();
		assert delFiles != null;
		for (File f : delFiles) {

			forceDelete(f);
		}

		System.out.println();*/

		long endTime = System.nanoTime();

		double deltaTime = (double) (endTime - startTime) / 1000000000.0;

		System.out.println("Encoded in: " + deltaTime + " seconds");
	}

	/**
	 * Delete a file from google photos
	 * @param fileName The name of the file to delete
	 * @return Returns true when the deletion was successful
	 * @throws DuplicateNameException
	 */
	public boolean deleteFile(String fileName) throws DuplicateNameException {
		Album album = getAlbumFromInput(fileName);

		return photos.deleteAlbum(album);
	}

	private static void forceDelete(File f) throws InterruptedException {
		if (f.exists()) {
			boolean result = f.delete();

			int counter = 0;
			while (!result && counter < 20) { // Only do this max 20 times
				Thread.sleep(100);
				System.gc();
				result = f.delete();
				counter++;
			}

			if (counter >= 20) {
				System.out.println("Failed to delete: " + f.getName() + "#" + f.hashCode());
				System.out.println("Attempting to delete on exit");
				f.deleteOnExit();
			}
		}
	}
}
