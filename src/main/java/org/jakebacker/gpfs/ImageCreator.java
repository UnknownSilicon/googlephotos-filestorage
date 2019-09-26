package org.jakebacker.gpfs;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageCreator {

    private BufferedImage image;

    public ImageCreator(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void drawPixel(int y, int x, Color c) {
        image.setRGB(x, y, c.getRGB());
    }

    public BufferedImage getImage() {
        return image;
    }
}
