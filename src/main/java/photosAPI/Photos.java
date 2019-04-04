package photosAPI;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.MediaItem;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.rpc.Code;
import com.google.rpc.Status;

import java.io.*;
import java.util.List;

public class Photos {

	private PhotosLibraryClient photosLibraryClient;

	public Photos() throws IOException {
		//InputStream in = Photos.class.getResourceAsStream("/GPFS-c29aa6fb95a3.json");
		InputStream in = Photos.class.getResourceAsStream("/client_id.json");

		GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(PhotosLibrarySettings.getDefaultServiceScopes());
		PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
				.build();

		photosLibraryClient = PhotosLibraryClient.initialize(settings);
	}

	/**
	 *
	 * @param f The image to upload
	 * @return The media item
	 */
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

				System.out.println(error.toString());
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

	public void processUploads(List<NewMediaItem> newItems) {
		if (newItems.size() <= 0) {
			return;
		}
		BatchCreateMediaItemsResponse response = photosLibraryClient.batchCreateMediaItems(newItems);

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
}
