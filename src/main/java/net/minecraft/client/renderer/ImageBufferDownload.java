package net.minecraft.client.renderer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;

public class ImageBufferDownload implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        if (image == null) {
            return null;
        } else {
            this.imageWidth = 64;
            this.imageHeight = 64;
            int srcWidth = image.getWidth();
            int srcHeight = image.getHeight();

            int k;
            for (k = 1; this.imageWidth < srcWidth || this.imageHeight < srcHeight; k *= 2) {
                this.imageWidth *= 2;
                this.imageHeight *= 2;
            }

            BufferedImage bufferedimage = new BufferedImage(this.imageWidth, this.imageHeight, 2);
            Graphics graphics = bufferedimage.getGraphics();
            graphics.drawImage(image, 0, 0, (ImageObserver)null);
            if (image.getHeight() == 32 * k) {
                graphics.drawImage(bufferedimage, 24 * k, 48 * k, 20 * k, 52 * k, 4 * k, 16 * k, 8 * k, 20 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 28 * k, 48 * k, 24 * k, 52 * k, 8 * k, 16 * k, 12 * k, 20 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 20 * k, 52 * k, 16 * k, 64 * k, 8 * k, 20 * k, 12 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 24 * k, 52 * k, 20 * k, 64 * k, 4 * k, 20 * k, 8 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 28 * k, 52 * k, 24 * k, 64 * k, 0 * k, 20 * k, 4 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 32 * k, 52 * k, 28 * k, 64 * k, 12 * k, 20 * k, 16 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 40 * k, 48 * k, 36 * k, 52 * k, 44 * k, 16 * k, 48 * k, 20 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 44 * k, 48 * k, 40 * k, 52 * k, 48 * k, 16 * k, 52 * k, 20 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 36 * k, 52 * k, 32 * k, 64 * k, 48 * k, 20 * k, 52 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 40 * k, 52 * k, 36 * k, 64 * k, 44 * k, 20 * k, 48 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 44 * k, 52 * k, 40 * k, 64 * k, 40 * k, 20 * k, 44 * k, 32 * k, (ImageObserver)null);
                graphics.drawImage(bufferedimage, 48 * k, 52 * k, 44 * k, 64 * k, 52 * k, 20 * k, 56 * k, 32 * k, (ImageObserver)null);
            }

            graphics.dispose();
            this.imageData = ((DataBufferInt)bufferedimage.getRaster().getDataBuffer()).getData();
            this.setAreaOpaque(0 * k, 0 * k, 32 * k, 16 * k);
            this.setAreaTransparent(32 * k, 0 * k, 64 * k, 32 * k);
            this.setAreaOpaque(0 * k, 16 * k, 64 * k, 32 * k);
            this.setAreaTransparent(0 * k, 32 * k, 16 * k, 48 * k);
            this.setAreaTransparent(16 * k, 32 * k, 40 * k, 48 * k);
            this.setAreaTransparent(40 * k, 32 * k, 56 * k, 48 * k);
            this.setAreaTransparent(0 * k, 48 * k, 16 * k, 64 * k);
            this.setAreaOpaque(16 * k, 48 * k, 48 * k, 64 * k);
            this.setAreaTransparent(48 * k, 48 * k, 64 * k, 64 * k);
            return bufferedimage;
        }
    }

    @Override
    public void skinAvailable() {
    }

    private void setAreaTransparent(int x, int y, int width, int height) {
        if (!this.hasTransparency(x, y, width, height)) {
            for (int i = x; i < width; i++) {
                for (int j = y; j < height; j++) {
                    this.imageData[i + j * this.imageWidth] = this.imageData[i + j * this.imageWidth] & 16777215;
                }
            }
        }
    }

    private void setAreaOpaque(int x, int y, int width, int height) {
        for (int i = x; i < width; i++) {
            for (int j = y; j < height; j++) {
                this.imageData[i + j * this.imageWidth] = this.imageData[i + j * this.imageWidth] | 0xFF000000;
            }
        }
    }

    private boolean hasTransparency(int x, int y, int width, int height) {
        for (int i = x; i < width; i++) {
            for (int j = y; j < height; j++) {
                int k = this.imageData[i + j * this.imageWidth];
                if ((k >> 24 & 0xFF) < 128) {
                    return true;
                }
            }
        }

        return false;
    }
}
