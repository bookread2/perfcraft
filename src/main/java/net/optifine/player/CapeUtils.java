package net.optifine.player;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.optifine.Config;

public class CapeUtils {
    private static final Pattern PATTERN_USERNAME = Pattern.compile("[a-zA-Z0-9_]+");

    public static void downloadCape(AbstractClientPlayer player) {
        String username = player.getNameClear();
        if (username != null && !username.isEmpty() && !username.contains("\u0000") && PATTERN_USERNAME.matcher(username).matches()) {
            String ofCapeUrl = "http://s.optifine.net/capes/" + username + ".png";
            ResourceLocation rl = new ResourceLocation("capeof/" + username);
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ITextureObject tex = textureManager.getTexture(rl);
            if (tex != null && tex instanceof ThreadDownloadImageData) {
                ThreadDownloadImageData tdid = (ThreadDownloadImageData)tex;
                if (tdid.imageFound != null) {
                    if (tdid.imageFound) {
                        player.setLocationOfCape(rl);
                        if (tdid.getImageBuffer() instanceof CapeImageBuffer) {
                            CapeImageBuffer cib = (CapeImageBuffer)tdid.getImageBuffer();
                            player.setElytraOfCape(cib.isElytraOfCape());
                        }
                    }

                    return;
                }
            }

            CapeImageBuffer cib = new CapeImageBuffer(player, rl);
            ThreadDownloadImageData textureCape = new ThreadDownloadImageData(null, ofCapeUrl, null, cib);
            textureCape.pipeline = true;
            textureManager.loadTexture(rl, textureCape);
        }
    }

    public static BufferedImage parseCape(BufferedImage img) {
        int imageWidth = 64;
        int imageHeight = 32;
        int srcWidth = img.getWidth();

        for (int srcHeight = img.getHeight(); imageWidth < srcWidth || imageHeight < srcHeight; imageHeight *= 2) {
            imageWidth *= 2;
        }

        BufferedImage imgNew = new BufferedImage(imageWidth, imageHeight, 2);
        Graphics g = imgNew.getGraphics();
        g.drawImage(img, 0, 0, (ImageObserver)null);
        g.dispose();
        return imgNew;
    }

    public static boolean isElytraCape(BufferedImage imageRaw, BufferedImage imageFixed) {
        return imageRaw.getWidth() > imageFixed.getHeight();
    }

    public static void reloadCape(AbstractClientPlayer player) {
        String nameClear = player.getNameClear();
        ResourceLocation rl = new ResourceLocation("capeof/" + nameClear);
        TextureManager textureManager = Config.getTextureManager();
        ITextureObject tex = textureManager.getTexture(rl);
        if (tex instanceof SimpleTexture) {
            SimpleTexture simpleTex = (SimpleTexture)tex;
            simpleTex.deleteGlTexture();
            textureManager.deleteTexture(rl);
        }

        player.setLocationOfCape(null);
        player.setElytraOfCape(false);
        downloadCape(player);
    }
}
