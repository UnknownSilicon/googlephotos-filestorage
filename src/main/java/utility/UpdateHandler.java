package utility;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class UpdateHandler {

	private static final String GITHUB_RELEASE_URL = "https://api.github.com/repos/jakebacker/googlephotos-filestorage/releases/latest";

	private static final String BACKUP_VERSION = "v3.0.0";

	public UpdateHandler() {

	}

	// Returns true if up to date
	public boolean checkVersion() {
		Class clazz = UpdateHandler.class;
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();
		String version = null;
		if (!classPath.startsWith("jar")) {
			// Class not from JAR
			System.out.println("Class not from JAR. Using backup version");
			version = BACKUP_VERSION;
		} else {
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
					"/META-INF/MANIFEST.MF";
			Manifest manifest = null;
			try {
				manifest = new Manifest(new URL(manifestPath).openStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			Attributes attr = null;
			if (manifest != null) {
				attr = manifest.getMainAttributes();
			}
			if (attr != null) {
				version = attr.getValue("Manifest-Version");
			}

			if (version == null) {
				System.out.println("Unable to retrieve current version");
				return true;
			}
		}

		System.out.println("You are using version " + version);

		String latestVersion = getLatestVersion();

		if (latestVersion == null) {
			System.out.println("Unable to retrieve latest version");
		} else {
			if (latestVersion.compareTo(version) > 0) {
				System.out.println("Your version is out of date, please visit https://github.com/jakebacker/googlephotos-filestorage/releases/latest to download the latest update.");
			} else {
				System.out.println("Your version is up to date");
			}
		}

		return true;
	}

	private static String getLatestVersion() {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.url(GITHUB_RELEASE_URL)
				.build();

		try (Response response = client.newCall(request).execute()) {
			String responseBody = response.body().string();

			JSONObject jsonObject = new JSONObject(responseBody);

			return jsonObject.getString("tag_name");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
