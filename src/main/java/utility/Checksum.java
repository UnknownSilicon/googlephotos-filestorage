package utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

    public static byte[] getFileChecksum(File file) throws IOException, NoSuchAlgorithmException {

        FileInputStream fis = new FileInputStream(file);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        return digest.digest();

    }
}
