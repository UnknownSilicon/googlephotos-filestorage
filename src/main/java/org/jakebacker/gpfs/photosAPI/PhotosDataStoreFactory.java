package org.jakebacker.gpfs.photosAPI;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Maps;
import com.google.api.client.util.store.*;

import java.io.*;
import java.util.logging.Logger;

/*
This is mostly a copy and paste of PhotosDataStoreFactory
 */
public class PhotosDataStoreFactory extends AbstractDataStoreFactory{

	private static final Logger LOGGER = Logger.getLogger(PhotosDataStoreFactory.class.getName());

	/** Directory to store data. */
	private final File dataDirectory;

	public PhotosDataStoreFactory(File dataDirectory) throws IOException {
		dataDirectory = dataDirectory.getCanonicalFile();
		this.dataDirectory = dataDirectory;
		// error if it is a symbolic link
		if (IOUtils.isSymbolicLink(dataDirectory)) {
			throw new IOException("unable to use a symbolic link: " + dataDirectory);
		}
		// create parent directory (if necessary)
		if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
			throw new IOException("unable to create directory: " + dataDirectory);
		}
		setPermissionsToOwnerOnly(dataDirectory);
	}

	/** Returns the data directory. */
	public final File getDataDirectory() {
		return dataDirectory;
	}

	@Override
	protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
		return new PhotosDataStoreFactory.FileDataStore<V>(this, dataDirectory, id);
	}

	/**
	 * File data store that inherits from the abstract memory data store because the key-value pairs
	 * are stored in a memory cache, and saved in the file (see {@link #save()} when changing values.
	 *
	 * @param <V> serializable type of the mapped value
	 */
	static class FileDataStore<V extends Serializable> extends AbstractMemoryDataStore<V> {

		/** File to store data. */
		private final File dataFile;

		FileDataStore(PhotosDataStoreFactory dataStore, File dataDirectory, String id)
				throws IOException {
			super(dataStore, id);
			this.dataFile = new File(dataDirectory, id);
			// error if it is a symbolic link
			if (IOUtils.isSymbolicLink(dataFile)) {
				throw new IOException("unable to use a symbolic link: " + dataFile);
			}
			// create new file (if necessary)
			if (dataFile.createNewFile()) {
				keyValueMap = Maps.newHashMap();
				// save the credentials to create a new file
				save();
			} else {
				// load credentials from existing file
				keyValueMap = IOUtils.deserialize(new FileInputStream(dataFile));
			}
		}

		@Override
		public void save() throws IOException {
			IOUtils.serialize(keyValueMap, new FileOutputStream(dataFile));
		}

		@Override
		public PhotosDataStoreFactory getDataStoreFactory() {
			return (PhotosDataStoreFactory) super.getDataStoreFactory();
		}
	}

	/**
	 * Attempts to set the given file's permissions such that it can only be read, written, and
	 * executed by the file's owner.
	 *
	 * @param file the file's permissions to modify
	 * @throws IOException
	 */
	private static void setPermissionsToOwnerOnly(File file) {
		// Disable access by other users if O/S allows it and set file permissions to readable and
		// writable by user. Use reflection since JDK 1.5 will not have these methods
		
		try {
			boolean everybodyReadable = file.setReadable(false, false); 
			boolean everybodyWritable = file.setWritable(false, false);
			boolean everybodyExecutable = file.setExecutable(false, false);

			// These statements are redundant for now unless a fix is found
			if (!everybodyReadable
					|| !everybodyWritable
					|| !everybodyExecutable) {
				//LOGGER.warning("unable to change permissions for everybody: " + file);
			}
			boolean ownerReadable = file.setReadable(true, true);
			boolean ownerWritable = file.setWritable(true, true);
			boolean ownerExecutable = file.setExecutable(true, true);

			if (!ownerReadable
					|| !ownerWritable
					|| !ownerExecutable) {
				//LOGGER.warning("unable to change permissions for owner: " + file);
			}
		} catch (SecurityException | IllegalArgumentException exception) {
			// ignored
		}
	}
}
