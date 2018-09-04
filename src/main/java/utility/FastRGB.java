package utility;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class FastRGB
{

    private int width;
    private int height;
    private boolean hasAlphaChannel;
    private int pixelLength;
    private byte[] pixels;

    public FastRGB(BufferedImage image)
    {

        pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        width = image.getWidth();
        height = image.getHeight();
        hasAlphaChannel = image.getAlphaRaster() != null;
        pixelLength = 3;
        if (hasAlphaChannel)
        {
            pixelLength = 4;
        }

    }

    public byte[] getRGB(int x, int y)
    {
        int pos = (y * pixelLength * width) + (x * pixelLength);

        byte[] data = new byte[3];
        data[2] = pixels[pos++]; // blue
        data[1] = pixels[pos++]; // green
        data[0] = pixels[pos++]; // red
        return data;
    }
}