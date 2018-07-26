import utility.Checksum;
import utility.FastRGB;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);

        String s = "";

        while (!s.equals("e") && !s.equals("d")) {
            System.out.println("Encode (e) or Decode (d)?");
            s = scan.nextLine();
        }

        if (s.equals("e")) {
            String fileStr = "";
            File file = null;
            boolean isFileValid = false;

            while(!isFileValid) {
                System.out.println("Input file: ");
                fileStr = scan.nextLine();
                file = new File(fileStr);

                isFileValid = file.exists();
            }

            fileStr = "";
            File outputFile = null;

            System.out.println("Output File: ");
            fileStr = scan.nextLine();
            outputFile = new File(fileStr);

            encode(file);
        } else if (s.equals("d")) {

        } else {
            System.out.println("Something Broke");
        }

    }

    public static void decode(BufferedImage image) throws IOException {
        long startTime = System.nanoTime();
        FastRGB fastRGB = new FastRGB(image);

        int size = image.getHeight()*image.getWidth();
        byte[] data = new byte[size*3];

        int dataNum = 0;

        for (int y=0; y<image.getHeight(); y++) {
            for (int x=0; x<image.getWidth(); x++) {
                byte[] pixel = fastRGB.getRGB(x, y);

                data[dataNum] = pixel[0];
                data[dataNum + 1] = pixel[1];
                data[dataNum + 2] = pixel[2];

                dataNum += 3;
            }
        }

        // File should be encoded in data

        FileOutputStream fos = new FileOutputStream(new File("output.apk"));

        fos.write(data);
        fos.close();

        long endTime = System.nanoTime();

        double deltaTime = (double)(endTime - startTime)/1000000000.0;

        System.out.println("Decoded in: " + deltaTime + " seconds");
    }

    public static void encode(File file) throws IOException {
        long startTime = System.nanoTime();
        if (!file.exists()) {
            System.out.println("File does not exist!");
            System.exit(1);
        }

        FileInputStream fis = new FileInputStream(file);

        double size = (double)Math.toIntExact(file.length());

        //byte[] checksum = Checksum.getFileChecksum(file);

        //size += checksum.length;

        size /= 3;

        int closestSquare = (int)Math.ceil(Math.sqrt(size));

        ImageCreator ic = new ImageCreator(closestSquare, closestSquare);

        int pixleNum = 0;

        byte[] byteArray = new byte[4096];
        int bytesCount = 0;
        int previousBytes = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            for (int i=previousBytes; i<bytesCount+previousBytes; i+=3) {
                int row = (pixleNum - (pixleNum%closestSquare))/closestSquare;
                int col = (pixleNum-closestSquare*row);

                try {
                    ic.drawPixel(row, col, new Color(byteArray[i- previousBytes] & 0xFF,
                            byteArray[i - previousBytes + 1] & 0xFF, byteArray[i - previousBytes + 2] & 0xFF));
                } catch (ArrayIndexOutOfBoundsException e) {
                    try {
                        ic.drawPixel(row, col, new Color(byteArray[i - previousBytes] & 0xFF,
                                byteArray[i - previousBytes + 1] & 0xFF, 0));
                    } catch (ArrayIndexOutOfBoundsException ee) {
                        try {
                            ic.drawPixel(row, col, new Color(byteArray[i - previousBytes] & 0xFF, 0, 0));
                        } catch (ArrayIndexOutOfBoundsException eee) {

                        }
                    }
                }
                pixleNum++;
            }
            previousBytes = bytesCount;
        }

        /*for (int i=0; i<checksum.length; i++) {
            int row = pixleNum % closestSquare;
            int col = pixleNum - (row * closestSquare);

            try {
                ic.drawPixel(row-1, col-1, new Color(checksum[i], checksum[i + 1], checksum[i + 2]));
            } catch (ArrayIndexOutOfBoundsException e) {
                try {
                    ic.drawPixel(row-1, col-1, new Color(checksum[i], checksum[i + 1], 0));
                } catch (ArrayIndexOutOfBoundsException ee) {
                    ic.drawPixel(row-1, col-1, new Color(checksum[i], 0, 0));
                }
            }
            pixleNum++;
        }*/

        BufferedImage image = ic.getImage();

        File output = new File("output.png");
        ImageIO.write(image, "png", output);

        long endTime = System.nanoTime();

        double deltaTime = (double)(endTime - startTime)/1000000000.0;

        System.out.println("Encoded in: " + deltaTime + " seconds");
    }

}
