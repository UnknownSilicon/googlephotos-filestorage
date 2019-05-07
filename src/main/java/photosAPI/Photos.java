package photosAPI;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.*;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.*;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.protobuf.CodedOutputStream;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Photos {

	private PhotosLibraryClient photosLibraryClient;

	private static final String CLIENT_ID = "***REMOVED***";

	private static final String CLIENT_SECRET = "***REMOVED***";

	private static final String REQUEST_SCOPE = "https://www.googleapis.com/auth/photoslibrary";

	private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".store/gpfs");

	public Photos() throws IOException {
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

	private static Credential authorize() throws IOException {
		AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
				new NetHttpTransport(),
				JacksonFactory.getDefaultInstance(),
				new GenericUrl("https://oauth2.googleapis.com/token"),
				new ClientParametersAuthentication(CLIENT_ID, CLIENT_SECRET),
				CLIENT_ID,
				"https://accounts.google.com/o/oauth2/auth").setScopes(Arrays.asList(REQUEST_SCOPE))
				.setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR)).build();

		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost("localhost").setPort(80).build();

		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public Album getAlbum(String name) {
		return photosLibraryClient.createAlbum(name);
	}

	public ArrayList<String> getFileNames() {
		InternalPhotosLibraryClient.ListAlbumsPagedResponse response = photosLibraryClient.listAlbums();

		ArrayList<String> fileNames = new ArrayList<>();

		for (Album album : response.iterateAll()) {
			String title = album.getTitle();
			String id = album.getId();
			String fullName = title + "#" + id;
			fileNames.add(fullName);
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

	public Album getExistingAlbum(String fileName) {
		InternalPhotosLibraryClient.ListAlbumsPagedResponse response = photosLibraryClient.listAlbums();

		for (Album album : response.iterateAll()) {
			if (album.getTitle().equals(fileName)) {
				return album;
			}
		}
		return null;
	}

	public ArrayList<File> downloadFiles(Album album) throws IOException {
		String albumId = album.getId();

		InternalPhotosLibraryClient.SearchMediaItemsPagedResponse response = photosLibraryClient.searchMediaItems(albumId);

		ArrayList<String> mediaItemIds = new ArrayList<>();
		for (MediaItem item : response.iterateAll()) {
			mediaItemIds.add(item.getId());
		}

		BatchGetMediaItemsResponse mediaItemsResponse = photosLibraryClient.batchGetMediaItems(mediaItemIds);

		ArrayList<File> files = new ArrayList<>();
		for (MediaItemResult result : mediaItemsResponse.getMediaItemResultsList()) {
			if (result.hasMediaItem()) {
				MediaItem mediaItem = result.getMediaItem();

				File outputFile = new File(mediaItem.getFilename());
				files.add(outputFile);

				String baseUrl = mediaItem.getBaseUrl();

				String fullDl = baseUrl + "=d";

				FileUtils.copyURLToFile(new URL(fullDl), outputFile);
			}
		}

		return files;
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
