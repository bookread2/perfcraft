package net.optifine.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class ModelSprite {
    private ModelRenderer modelRenderer = null;
    private int textureOffsetX = 0;
    private int textureOffsetY = 0;
    private float posX = 0.0F;
    private float posY = 0.0F;
    private float posZ = 0.0F;
    private int sizeX = 0;
    private int sizeY = 0;
    private int sizeZ = 0;
    private float sizeAdd = 0.0F;
    private float minU = 0.0F;
    private float minV = 0.0F;
    private float maxU = 0.0F;
    private float maxV = 0.0F;

    public ModelSprite(
        ModelRenderer modelRenderer, int textureOffsetX, int textureOffsetY, float posX, float posY, float posZ, int sizeX, int sizeY, int sizeZ, float sizeAdd
    ) {
        this.modelRenderer = modelRenderer;
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeAdd = sizeAdd;
        this.minU = textureOffsetX / modelRenderer.textureWidth;
        this.minV = textureOffsetY / modelRenderer.textureHeight;
        this.maxU = (textureOffsetX + sizeX) / modelRenderer.textureWidth;
        this.maxV = (textureOffsetY + sizeY) / modelRenderer.textureHeight;
    }

    public void render(Tessellator tessellator, float scale) {
        GlStateManager.translate(this.posX * scale, this.posY * scale, this.posZ * scale);
        float rMinU = this.minU;
        float rMaxU = this.maxU;
        float rMinV = this.minV;
        float rMaxV = this.maxV;
        if (this.modelRenderer.mirror) {
            rMinU = this.maxU;
            rMaxU = this.minU;
        }

        if (this.modelRenderer.mirrorV) {
            rMinV = this.maxV;
            rMaxV = this.minV;
        }

        renderItemIn2D(
            tessellator,
            rMinU,
            rMinV,
            rMaxU,
            rMaxV,
            this.sizeX,
            this.sizeY,
            scale * this.sizeZ,
            this.modelRenderer.textureWidth,
            this.modelRenderer.textureHeight
        );
        GlStateManager.translate(-this.posX * scale, -this.posY * scale, -this.posZ * scale);
    }

    public static void renderItemIn2D(
        Tessellator tess, float minU, float minV, float maxU, float maxV, int sizeX, int sizeY, float width, float texWidth, float texHeight
    ) {
        if (width < 6.25E-4F) {
            width = 6.25E-4F;
        }

        float dU = maxU - minU;
        float dV = maxV - minV;
        double dimX = MathHelper.abs(dU) * (texWidth / 16.0F);
        double dimY = MathHelper.abs(dV) * (texHeight / 16.0F);
        WorldRenderer tessellator = tess.getWorldRenderer();
        GL11.glNormal3f(0.0F, 0.0F, -1.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);
        tessellator.pos(0.0, dimY, 0.0).tex(minU, maxV).endVertex();
        tessellator.pos(dimX, dimY, 0.0).tex(maxU, maxV).endVertex();
        tessellator.pos(dimX, 0.0, 0.0).tex(maxU, minV).endVertex();
        tessellator.pos(0.0, 0.0, 0.0).tex(minU, minV).endVertex();
        tess.draw();
        GL11.glNormal3f(0.0F, 0.0F, 1.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);
        tessellator.pos(0.0, 0.0, width).tex(minU, minV).endVertex();
        tessellator.pos(dimX, 0.0, width).tex(maxU, minV).endVertex();
        tessellator.pos(dimX, dimY, width).tex(maxU, maxV).endVertex();
        tessellator.pos(0.0, dimY, width).tex(minU, maxV).endVertex();
        tess.draw();
        float var8 = 0.5F * dU / sizeX;
        float var9 = 0.5F * dV / sizeY;
        GL11.glNormal3f(-1.0F, 0.0F, 0.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);

        for (int var10 = 0; var10 < sizeX; var10++) {
            float var11 = (float)var10 / sizeX;
            float var12 = minU + dU * var11 + var8;
            tessellator.pos(var11 * dimX, dimY, width).tex(var12, maxV).endVertex();
            tessellator.pos(var11 * dimX, dimY, 0.0).tex(var12, maxV).endVertex();
            tessellator.pos(var11 * dimX, 0.0, 0.0).tex(var12, minV).endVertex();
            tessellator.pos(var11 * dimX, 0.0, width).tex(var12, minV).endVertex();
        }

        tess.draw();
        GL11.glNormal3f(1.0F, 0.0F, 0.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);

        for (int var23 = 0; var23 < sizeX; var23++) {
            float var11 = (float)var23 / sizeX;
            float var12 = minU + dU * var11 + var8;
            float var13 = var11 + 1.0F / sizeX;
            tessellator.pos(var13 * dimX, 0.0, width).tex(var12, minV).endVertex();
            tessellator.pos(var13 * dimX, 0.0, 0.0).tex(var12, minV).endVertex();
            tessellator.pos(var13 * dimX, dimY, 0.0).tex(var12, maxV).endVertex();
            tessellator.pos(var13 * dimX, dimY, width).tex(var12, maxV).endVertex();
        }

        tess.draw();
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);

        for (int var24 = 0; var24 < sizeY; var24++) {
            float var11 = (float)var24 / sizeY;
            float var12 = minV + dV * var11 + var9;
            float var13 = var11 + 1.0F / sizeY;
            tessellator.pos(0.0, var13 * dimY, width).tex(minU, var12).endVertex();
            tessellator.pos(dimX, var13 * dimY, width).tex(maxU, var12).endVertex();
            tessellator.pos(dimX, var13 * dimY, 0.0).tex(maxU, var12).endVertex();
            tessellator.pos(0.0, var13 * dimY, 0.0).tex(minU, var12).endVertex();
        }

        tess.draw();
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);

        for (int var25 = 0; var25 < sizeY; var25++) {
            float var11 = (float)var25 / sizeY;
            float var12 = minV + dV * var11 + var9;
            tessellator.pos(dimX, var11 * dimY, width).tex(maxU, var12).endVertex();
            tessellator.pos(0.0, var11 * dimY, width).tex(minU, var12).endVertex();
            tessellator.pos(0.0, var11 * dimY, 0.0).tex(minU, var12).endVertex();
            tessellator.pos(dimX, var11 * dimY, 0.0).tex(maxU, var12).endVertex();
        }

        tess.draw();
    }
}
