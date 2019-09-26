package org.jakebacker.gpfs.photosAPI;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.*;
import com.google.common.io.Files;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.*;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Photos {

	private PhotosLibraryClient photosLibraryClient;

	private static final String CREDENIALS_FILE_PATH = "/credentials.json";

	private static final String REQUEST_SCOPE = "https://www.googleapis.com/auth/photoslibrary";

	private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".store/gpfs");

	public Photos() throws IOException, GeneralSecurityException {

		Credential authCred = authorize();

		authCred.refreshToken();

		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime()+authCred.getExpirationTimeMilliseconds());

		GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(authCred.getAccessToken(),expireTime));

		PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
				.build();

		photosLibraryClient = PhotosLibraryClient.initialize(settings);
	}

	private static Credential authorize() throws IOException, GeneralSecurityException {
		InputStream in = Photos.class.getResourceAsStream(CREDENIALS_FILE_PATH);
		GoogleClientSecrets secrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), secrets, Arrays.asList(REQUEST_SCOPE))
				.setDataStoreFactory(new PhotosDataStoreFactory(DATA_STORE_DIR))
				.setAccessType("offline")
				.build();

		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost("localhost").setPort(1337).build();

		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public Album getAlbum(String name) {
		return photosLibraryClient.createAlbum(name);
	}

	public ArrayList<String> getFileNames() {
		InternalPhotosLibraryClient.ListAlbumsPagedResponse response = photosLibraryClient.listAlbums();

		ArrayList<String> fileNames = new ArrayList<>();

		for (Album album : response.iterateAll()) {

			if(album.getMediaItemsCount() != 0) {

				String title = album.getTitle();
				String id = album.getId();
				String fullName = title + "*" + id;
				fileNames.add(fullName);
			}
		}

		return fileNames;
	}

	public NewMediaItem uploadImage(File f) {
		UploadMediaItemRequest uploadRequest = null;
		try {
			uploadRequest = UploadMediaItemRequest.newBuilder()
					.setFileName(f.getName())
					.setDataFile(new RandomAccessFile(f, "r"))
					.build();

			UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);

			if (uploadResponse.getError().isPresent()) {
				UploadMediaItemResponse.Error error = uploadResponse.getError().get();

				System.out.println(error.getCause());
			} else {
				String uploadToken = uploadResponse.getUploadToken().get();

				return NewMediaItemFactory.createNewMediaItem(uploadToken);
			}
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found!");
			e.printStackTrace();
		}

		return null;
	}

	public Album getExistingAlbum(String fileName) throws DuplicateNameException{
		InternalPhotosLibraryClient.ListAlbumsPagedResponse response = photosLibraryClient.listAlbums();

		Album foundAlbum = null;

		for (Album album : response.iterateAll()) {
			if (album.getTitle().equals(fileName) && album.getMediaItemsCount() !=0) {
				if (foundAlbum != null) {
					System.out.println("There is more than one file with that name! Please use id");
					throw new DuplicateNameException();
				}
				foundAlbum = album;
			}
		}
		return foundAlbum;
	}

	public Album getExistingAlbumFromId(String id) {
		InternalPhotosLibraryClient.ListAlbumsPagedResponse response = photosLibraryClient.listAlbums();

		for (Album album : response.iterateAll()) {
			if (album.getId().equals(id)) {
				return album;
			}
		}
		return null;
	}

	/**
	 * Delete an album from google photos
	 * @param album The album to delete
	 * @return Whether or not the deletion was successful. CURRENTLY ALWAYS RETURNS TRUE
	 */
	public boolean deleteAlbum(Album album) {

		String albumId = album.getId();

		ArrayList<String> mediaItemIds = getMediaItemsFromAlbum(albumId);


		BatchRemoveMediaItemsFromAlbumResponse removeResponse = photosLibraryClient.batchRemoveMediaItemsFromAlbum(albumId, mediaItemIds);

		// TODO: Test if removeResponse is successful
		return true;
	}

	private ArrayList<String> getMediaItemsFromAlbum(String albumId) {
		InternalPhotosLibraryClient.SearchMediaItemsPagedResponse response = photosLibraryClient.searchMediaItems(albumId);

		ArrayList<String> mediaItemIds = new ArrayList<>();
		for (MediaItem item : response.iterateAll()) {
			mediaItemIds.add(item.getId());

		}

		return mediaItemIds;
	}

	/**
	 *
	 * @param album
	 * @return The path to the file directory
	 * @throws IOException
	 */
	public File downloadFiles(Album album) throws IOException {
		String albumId = album.getId();

		ArrayList<String> mediaItemIds = getMediaItemsFromAlbum(albumId);

		BatchGetMediaItemsResponse mediaItemsResponse = photosLibraryClient.batchGetMediaItems(mediaItemIds);

		File tempDir = Files.createTempDir();

		//ArrayList<File> files = new ArrayList<>();
		for (MediaItemResult result : mediaItemsResponse.getMediaItemResultsList()) {
			if (result.hasMediaItem()) {
				MediaItem mediaItem = result.getMediaItem();

				File outputFile = new File(tempDir.getAbsolutePath() + File.separator + mediaItem.getFilename());
				//files.add(outputFile);

				String baseUrl = mediaItem.getBaseUrl();

				String fullDl = baseUrl + "=d";

				FileUtils.copyURLToFile(new URL(fullDl), outputFile);

				outputFile = null;
			}
		}

		return tempDir;
	}

	public void processUploads(List<NewMediaItem> newItems, Album album) {
		if (newItems.size() <= 0) {
			return;
		}

		BatchCreateMediaItemsResponse response;
		if (album != null) {
			response = photosLibraryClient.batchCreateMediaItems(album.getId(), newItems);
		} else {
			response = photosLibraryClient.batchCreateMediaItems(newItems);
		}


		for (NewMediaItemResult itemResponse : response.getNewMediaItemResultsList()) {
			Status status = itemResponse.getStatus();
			if (status.getCode() == Code.OK_VALUE) {
				MediaItem createdItem = itemResponse.getMediaItem();

				System.out.println("Successfully processed: " + createdItem.getFilename());

			} else {
				System.out.println("Error!");
			}
		}
	}
	public void processUploads(List<NewMediaItem> newItems) {
		processUploads(newItems, null);
	}
}

