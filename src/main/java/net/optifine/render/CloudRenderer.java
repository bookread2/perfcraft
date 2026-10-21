package net.optifine.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class CloudRenderer {
    private Minecraft mc;
    private boolean updated = false;
    private boolean renderFancy = false;
    int cloudTickCounter;
    private Vec3 cloudColor;
    float partialTicks;
    private boolean updateRenderFancy = false;
    private int updateCloudTickCounter = 0;
    private Vec3 updateCloudColor = new Vec3(-1.0, -1.0, -1.0);
    private double updatePlayerX = 0.0;
    private double updatePlayerY = 0.0;
    private double updatePlayerZ = 0.0;
    private int glListClouds = -1;

    public CloudRenderer(Minecraft mc) {
        this.mc = mc;
        this.glListClouds = GLAllocation.generateDisplayLists(1);
    }

    public void prepareToRender(boolean renderFancy, int cloudTickCounter, float partialTicks, Vec3 cloudColor) {
        this.renderFancy = renderFancy;
        this.cloudTickCounter = cloudTickCounter;
        this.partialTicks = partialTicks;
        this.cloudColor = cloudColor;
    }

    public boolean shouldUpdateGlList() {
        if (!this.updated) {
            return true;
        } else if (this.renderFancy != this.updateRenderFancy) {
            return true;
        } else if (this.cloudTickCounter >= this.updateCloudTickCounter + 20) {
            return true;
        } else if (Math.abs(this.cloudColor.xCoord - this.updateCloudColor.xCoord) > 0.003) {
            return true;
        } else if (Math.abs(this.cloudColor.yCoord - this.updateCloudColor.yCoord) > 0.003) {
            return true;
        } else if (Math.abs(this.cloudColor.zCoord - this.updateCloudColor.zCoord) > 0.003) {
            return true;
        } else {
            Entity rve = this.mc.getRenderViewEntity();
            boolean belowCloudsPrev = this.updatePlayerY + rve.getEyeHeight() < 128.0 + this.mc.gameSettings.ofCloudsHeight * 128.0F;
            boolean belowClouds = rve.prevPosY + rve.getEyeHeight() < 128.0 + this.mc.gameSettings.ofCloudsHeight * 128.0F;
            return belowClouds != belowCloudsPrev;
        }
    }

    public void startUpdateGlList() {
        GL11.glNewList(this.glListClouds, 4864);
    }

    public void endUpdateGlList() {
        GL11.glEndList();
        this.updateRenderFancy = this.renderFancy;
        this.updateCloudTickCounter = this.cloudTickCounter;
        this.updateCloudColor = this.cloudColor;
        this.updatePlayerX = this.mc.getRenderViewEntity().prevPosX;
        this.updatePlayerY = this.mc.getRenderViewEntity().prevPosY;
        this.updatePlayerZ = this.mc.getRenderViewEntity().prevPosZ;
        this.updated = true;
        GlStateManager.resetColor();
    }

    public void renderGlList() {
        Entity entityliving = this.mc.getRenderViewEntity();
        double exactPlayerX = entityliving.prevPosX + (entityliving.posX - entityliving.prevPosX) * this.partialTicks;
        double exactPlayerY = entityliving.prevPosY + (entityliving.posY - entityliving.prevPosY) * this.partialTicks;
        double exactPlayerZ = entityliving.prevPosZ + (entityliving.posZ - entityliving.prevPosZ) * this.partialTicks;
        double dc = this.cloudTickCounter - this.updateCloudTickCounter + this.partialTicks;
        float cdx = (float)(exactPlayerX - this.updatePlayerX + dc * 0.03);
        float cdy = (float)(exactPlayerY - this.updatePlayerY);
        float cdz = (float)(exactPlayerZ - this.updatePlayerZ);
        GlStateManager.pushMatrix();
        if (this.renderFancy) {
            GlStateManager.translate(-cdx / 12.0F, -cdy, -cdz / 12.0F);
        } else {
            GlStateManager.translate(-cdx, -cdy, -cdz);
        }

        GlStateManager.callList(this.glListClouds);
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }

    public void reset() {
        this.updated = false;
    }
}
