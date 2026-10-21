package net.minecraft.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.optifine.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ScreenShotHelper {
    private static final Logger logger = LogManager.getLogger();
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    public static IChatComponent saveScreenshot(File gameDirectory, int width, int height, Framebuffer buffer) {
        return saveScreenshot(gameDirectory, (String)null, width, height, buffer);
    }

    public static IChatComponent saveScreenshot(File gameDirectory, String screenshotName, int width, int height, Framebuffer buffer) {
        try {
            File file1 = new File(gameDirectory, "screenshots");
            file1.mkdir();
            Minecraft mc = Minecraft.getMinecraft();
            int guiScaleOld = Config.getGameSettings().guiScale;
            ScaledResolution sr = new ScaledResolution(mc);
            int guiScale = sr.getScaleFactor();
            int mul = Config.getScreenshotSize();
            boolean resize = OpenGlHelper.isFramebufferEnabled() && mul > 1;
            if (resize) {
                Config.getGameSettings().guiScale = guiScale * mul;
                resize(width * mul, height * mul);
                GlStateManager.pushMatrix();
                GlStateManager.clear(16640);
                mc.getFramebuffer().bindFramebuffer(true);
                mc.entityRenderer.updateCameraAndRender(Config.renderPartialTicks, System.nanoTime());
            }

            if (OpenGlHelper.isFramebufferEnabled()) {
                width = buffer.framebufferTextureWidth;
                height = buffer.framebufferTextureHeight;
            }

            int i = width * height;
            if (pixelBuffer == null || pixelBuffer.capacity() < i) {
                pixelBuffer = BufferUtils.createIntBuffer(i);
                pixelValues = new int[i];
            }

            GL11.glPixelStorei(3333, 1);
            GL11.glPixelStorei(3317, 1);
            ((Buffer)pixelBuffer).clear();
            if (OpenGlHelper.isFramebufferEnabled()) {
                GlStateManager.bindTexture(buffer.framebufferTexture);
                GL11.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
            } else {
                GL11.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
            TextureUtil.processPixelValues(pixelValues, width, height);
            BufferedImage bufferedimage = null;
            if (OpenGlHelper.isFramebufferEnabled()) {
                bufferedimage = new BufferedImage(buffer.framebufferWidth, buffer.framebufferHeight, 1);
                int j = buffer.framebufferTextureHeight - buffer.framebufferHeight;

                for (int k = j; k < buffer.framebufferTextureHeight; k++) {
                    for (int l = 0; l < buffer.framebufferWidth; l++) {
                        bufferedimage.setRGB(l, k - j, pixelValues[k * buffer.framebufferTextureWidth + l]);
                    }
                }
            } else {
                bufferedimage = new BufferedImage(width, height, 1);
                bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);
            }

            if (resize) {
                mc.getFramebuffer().unbindFramebuffer();
                GlStateManager.popMatrix();
                Config.getGameSettings().guiScale = guiScaleOld;
                resize(width, height);
            }

            File file2;
            if (screenshotName == null) {
                file2 = getTimestampedPNGFileForDirectory(file1);
            } else {
                file2 = new File(file1, screenshotName);
            }

            file2 = file2.getCanonicalFile();
            ImageIO.write(bufferedimage, "png", file2);
            IChatComponent ichatcomponent = new ChatComponentText(file2.getName());
            ichatcomponent.getChatStyle().setChatClickEvent(new ClickEvent(Action.OPEN_FILE, file2.getAbsolutePath()));
            ichatcomponent.getChatStyle().setUnderlined(true);
            return new ChatComponentTranslation("screenshot.success", ichatcomponent);
        } catch (Exception var17) {
            logger.warn("Couldn't save screenshot", var17);
            return new ChatComponentTranslation("screenshot.failure", var17.getMessage());
        }
    }

    private static File getTimestampedPNGFileForDirectory(File gameDirectory) {
        String s = dateFormat.format(new Date()).toString();
        int i = 1;

        while (true) {
            File file1 = new File(gameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            i++;
        }
    }

    private static void resize(int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayWidth = Math.max(1, width);
        mc.displayHeight = Math.max(1, height);
        if (mc.currentScreen != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            mc.currentScreen.onResize(mc, sr.getScaledWidth(), sr.getScaledHeight());
        }

        updateFramebufferSize();
    }

    private static void updateFramebufferSize() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().createBindFramebuffer(mc.displayWidth, mc.displayHeight);
        if (mc.entityRenderer != null) {
            mc.entityRenderer.updateShaderGroupSize(mc.displayWidth, mc.displayHeight);
        }
    }
}
