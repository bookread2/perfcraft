package net.optifine;

import java.util.Properties;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class CustomLoadingScreen {
    private ResourceLocation locationTexture;
    private int scaleMode = 0;
    private int scale = 2;
    private boolean center;
    private static final int SCALE_DEFAULT = 2;
    private static final int SCALE_MODE_FIXED = 0;
    private static final int SCALE_MODE_FULL = 1;
    private static final int SCALE_MODE_STRETCH = 2;

    public CustomLoadingScreen(ResourceLocation locationTexture, int scaleMode, int scale, boolean center) {
        this.locationTexture = locationTexture;
        this.scaleMode = scaleMode;
        this.scale = scale;
        this.center = center;
    }

    public static CustomLoadingScreen parseScreen(String path, int dimId, Properties props) {
        ResourceLocation loc = new ResourceLocation(path);
        int scaleMode = parseScaleMode(getProperty("scaleMode", dimId, props));
        int scaleDef = scaleMode == 0 ? 2 : 1;
        int scale = parseScale(getProperty("scale", dimId, props), scaleDef);
        boolean center = Config.parseBoolean(getProperty("center", dimId, props), false);
        return new CustomLoadingScreen(loc, scaleMode, scale, center);
    }

    private static String getProperty(String key, int dim, Properties props) {
        if (props == null) {
            return null;
        } else {
            String val = props.getProperty("dim" + dim + "." + key);
            return val != null ? val : props.getProperty(key);
        }
    }

    private static int parseScaleMode(String str) {
        if (str == null) {
            return 0;
        } else {
            str = str.toLowerCase().trim();
            if (str.equals("fixed")) {
                return 0;
            } else if (str.equals("full")) {
                return 1;
            } else if (str.equals("stretch")) {
                return 2;
            } else {
                CustomLoadingScreens.warn("Invalid scale mode: " + str);
                return 0;
            }
        }
    }

    private static int parseScale(String str, int def) {
        if (str == null) {
            return def;
        } else {
            str = str.trim();
            int val = Config.parseInt(str, -1);
            if (val < 1) {
                CustomLoadingScreens.warn("Invalid scale: " + str);
                return def;
            } else {
                return val;
            }
        }
    }

    public void drawBackground(int width, int height) {
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
        Config.getTextureManager().bindTexture(this.locationTexture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        double div = 16 * this.scale;
        double uMax = width / div;
        double vMax = height / div;
        double du = 0.0;
        double dv = 0.0;
        if (this.center) {
            du = (div - width) / (div * 2.0);
            dv = (div - height) / (div * 2.0);
        }

        switch (this.scaleMode) {
            case 1:
                div = Math.max(width, height);
                uMax = this.scale * width / div;
                vMax = this.scale * height / div;
                if (this.center) {
                    du = this.scale * (div - width) / (div * 2.0);
                    dv = this.scale * (div - height) / (div * 2.0);
                }
                break;
            case 2:
                uMax = this.scale;
                vMax = this.scale;
                du = 0.0;
                dv = 0.0;
        }

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(0.0, height, 0.0).tex(du, dv + vMax).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(width, height, 0.0).tex(du + uMax, dv + vMax).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(width, 0.0, 0.0).tex(du + uMax, dv).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(0.0, 0.0, 0.0).tex(du, dv).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
    }
}
