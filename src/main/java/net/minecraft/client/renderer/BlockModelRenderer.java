package net.minecraft.client.renderer;

import java.util.BitSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Block.EnumOffsetType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.optifine.BetterSnow;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.model.BlockModelCustomizer;
import net.optifine.model.ListQuadsOverlay;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;

public class BlockModelRenderer {
    private static float aoLightValueOpaque = 0.2F;
    private static boolean separateAoLightValue = false;
    private static final EnumWorldBlockLayer[] OVERLAY_LAYERS = new EnumWorldBlockLayer[]{
        EnumWorldBlockLayer.CUTOUT, EnumWorldBlockLayer.CUTOUT_MIPPED, EnumWorldBlockLayer.TRANSLUCENT
    };

    public BlockModelRenderer() {
        if (Reflector.ForgeModContainer_forgeLightPipelineEnabled.exists()) {
            Reflector.setFieldValue(Reflector.ForgeModContainer_forgeLightPipelineEnabled, false);
        }
    }

    public boolean renderModel(IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, WorldRenderer worldRendererIn) {
        Block block = blockStateIn.getBlock();
        block.setBlockBoundsBasedOnState(blockAccessIn, blockPosIn);
        return this.renderModel(blockAccessIn, modelIn, blockStateIn, blockPosIn, worldRendererIn, true);
    }

    public boolean renderModel(
        IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides
    ) {
        boolean flag = Minecraft.isAmbientOcclusionEnabled() && blockStateIn.getBlock().getLightValue() == 0 && modelIn.isAmbientOcclusion();

        try {
            if (Config.isShaders()) {
                SVertexBuilder.pushEntity(blockStateIn, blockPosIn, blockAccessIn, worldRendererIn);
            }

            RenderEnv renderEnv = worldRendererIn.getRenderEnv(blockStateIn, blockPosIn);
            modelIn = BlockModelCustomizer.getRenderModel(modelIn, blockStateIn, renderEnv);
            boolean rendered = flag
                ? this.renderModelSmooth(blockAccessIn, modelIn, blockStateIn, blockPosIn, worldRendererIn, checkSides)
                : this.renderModelFlat(blockAccessIn, modelIn, blockStateIn, blockPosIn, worldRendererIn, checkSides);
            if (rendered) {
                this.renderOverlayModels(blockAccessIn, modelIn, blockStateIn, blockPosIn, worldRendererIn, checkSides, 0L, renderEnv, flag);
            }

            if (Config.isShaders()) {
                SVertexBuilder.popEntity(worldRendererIn);
            }

            return rendered;
        } catch (Throwable var13) {
            CrashReport crashreport = CrashReport.makeCrashReport(var13, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, blockPosIn, blockStateIn);
            crashreportcategory.addCrashSection("Using AO", flag);
            throw new ReportedException(crashreport);
        }
    }

    public boolean renderModelAmbientOcclusion(
        IBlockAccess blockAccessIn, IBakedModel modelIn, Block blockIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides
    ) {
        IBlockState stateIn = blockAccessIn.getBlockState(blockPosIn);
        return this.renderModelSmooth(blockAccessIn, modelIn, stateIn, blockPosIn, worldRendererIn, checkSides);
    }

    private boolean renderModelSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, WorldRenderer buffer, boolean checkSides) {
        boolean flag = false;
        Block blockIn = stateIn.getBlock();
        RenderEnv renderEnv = buffer.getRenderEnv(stateIn, posIn);
        EnumWorldBlockLayer layer = buffer.getBlockLayer();

        for (EnumFacing enumfacing : EnumFacing.VALUES) {
            List<BakedQuad> list = modelIn.getFaceQuads(enumfacing);
            if (!list.isEmpty()) {
                BlockPos blockpos = posIn.offset(enumfacing);
                if (!checkSides || blockIn.shouldSideBeRendered(worldIn, blockpos, enumfacing)) {
                    list = BlockModelCustomizer.getRenderQuads(list, worldIn, stateIn, posIn, enumfacing, layer, 0L, renderEnv);
                    this.renderQuadsSmooth(worldIn, stateIn, posIn, buffer, list, renderEnv);
                    flag = true;
                }
            }
        }

        List<BakedQuad> list1 = modelIn.getGeneralQuads();
        if (list1.size() > 0) {
            list1 = BlockModelCustomizer.getRenderQuads(list1, worldIn, stateIn, posIn, null, layer, 0L, renderEnv);
            this.renderQuadsSmooth(worldIn, stateIn, posIn, buffer, list1, renderEnv);
            flag = true;
        }

        return flag;
    }

    public boolean renderModelStandard(
        IBlockAccess blockAccessIn, IBakedModel modelIn, Block blockIn, BlockPos blockPosIn, WorldRenderer worldRendererIn, boolean checkSides
    ) {
        IBlockState stateIn = blockAccessIn.getBlockState(blockPosIn);
        return this.renderModelFlat(blockAccessIn, modelIn, stateIn, blockPosIn, worldRendererIn, checkSides);
    }

    public boolean renderModelFlat(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, WorldRenderer buffer, boolean checkSides) {
        boolean flag = false;
        Block blockIn = stateIn.getBlock();
        RenderEnv renderEnv = buffer.getRenderEnv(stateIn, posIn);
        EnumWorldBlockLayer layer = buffer.getBlockLayer();

        for (EnumFacing enumfacing : EnumFacing.VALUES) {
            List<BakedQuad> list = modelIn.getFaceQuads(enumfacing);
            if (!list.isEmpty()) {
                BlockPos blockpos = posIn.offset(enumfacing);
                if (!checkSides || blockIn.shouldSideBeRendered(worldIn, blockpos, enumfacing)) {
                    int i = blockIn.getMixedBrightnessForBlock(worldIn, blockpos);
                    list = BlockModelCustomizer.getRenderQuads(list, worldIn, stateIn, posIn, enumfacing, layer, 0L, renderEnv);
                    this.renderQuadsFlat(worldIn, stateIn, posIn, enumfacing, i, false, buffer, list, renderEnv);
                    flag = true;
                }
            }
        }

        List<BakedQuad> list1 = modelIn.getGeneralQuads();
        if (list1.size() > 0) {
            list1 = BlockModelCustomizer.getRenderQuads(list1, worldIn, stateIn, posIn, null, layer, 0L, renderEnv);
            this.renderQuadsFlat(worldIn, stateIn, posIn, (EnumFacing)null, -1, true, buffer, list1, renderEnv);
            flag = true;
        }

        return flag;
    }

    private void renderQuadsSmooth(IBlockAccess worldIn, IBlockState stateIn, BlockPos posIn, WorldRenderer buffer, List<BakedQuad> list, RenderEnv renderEnv) {
        Block blockIn = stateIn.getBlock();
        float[] quadBounds = renderEnv.getQuadBounds();
        BitSet bitSet = renderEnv.getBoundsFlags();
        BlockModelRenderer.AmbientOcclusionFace aoFace = renderEnv.getAoFace();
        double d0 = posIn.getX();
        double d1 = posIn.getY();
        double d2 = posIn.getZ();
        EnumOffsetType block$enumoffsettype = blockIn.getOffsetType();
        if (block$enumoffsettype != EnumOffsetType.NONE) {
            long i = MathHelper.getPositionRandom(posIn);
            d0 += ((float)(i >> 16 & 15L) / 15.0F - 0.5) * 0.5;
            d2 += ((float)(i >> 24 & 15L) / 15.0F - 0.5) * 0.5;
            if (block$enumoffsettype == EnumOffsetType.XYZ) {
                d1 += ((float)(i >> 20 & 15L) / 15.0F - 1.0) * 0.2;
            }
        }

        for (BakedQuad bakedquad : list) {
            this.fillQuadBounds(blockIn, bakedquad.getVertexData(), bakedquad.getFace(), quadBounds, bitSet);
            aoFace.updateVertexBrightness(worldIn, blockIn, posIn, bakedquad.getFace(), quadBounds, bitSet);
            if (bakedquad.getSprite().isEmissive) {
                aoFace.setMaxBlockLight();
            }

            if (buffer.isMultiTexture()) {
                buffer.addVertexData(bakedquad.getVertexDataSingle());
            } else {
                buffer.addVertexData(bakedquad.getVertexData());
            }

            buffer.putSprite(bakedquad.getSprite());
            buffer.putBrightness4(aoFace.vertexBrightness[0], aoFace.vertexBrightness[1], aoFace.vertexBrightness[2], aoFace.vertexBrightness[3]);
            int colorMultiplier = CustomColors.getColorMultiplier(bakedquad, stateIn, worldIn, posIn, renderEnv);
            if (!bakedquad.hasTintIndex() && colorMultiplier == -1) {
                if (separateAoLightValue) {
                    buffer.putColorMultiplierRgba(1.0F, 1.0F, 1.0F, aoFace.vertexColorMultiplier[0], 4);
                    buffer.putColorMultiplierRgba(1.0F, 1.0F, 1.0F, aoFace.vertexColorMultiplier[1], 3);
                    buffer.putColorMultiplierRgba(1.0F, 1.0F, 1.0F, aoFace.vertexColorMultiplier[2], 2);
                    buffer.putColorMultiplierRgba(1.0F, 1.0F, 1.0F, aoFace.vertexColorMultiplier[3], 1);
                } else {
                    buffer.putColorMultiplier(aoFace.vertexColorMultiplier[0], aoFace.vertexColorMultiplier[0], aoFace.vertexColorMultiplier[0], 4);
                    buffer.putColorMultiplier(aoFace.vertexColorMultiplier[1], aoFace.vertexColorMultiplier[1], aoFace.vertexColorMultiplier[1], 3);
                    buffer.putColorMultiplier(aoFace.vertexColorMultiplier[2], aoFace.vertexColorMultiplier[2], aoFace.vertexColorMultiplier[2], 2);
                    buffer.putColorMultiplier(aoFace.vertexColorMultiplier[3], aoFace.vertexColorMultiplier[3], aoFace.vertexColorMultiplier[3], 1);
                }
            } else {
                int j;
                if (colorMultiplier != -1) {
                    j = colorMultiplier;
                } else {
                    j = blockIn.colorMultiplier(worldIn, posIn, bakedquad.getTintIndex());
                }

                if (EntityRenderer.anaglyphEnable) {
                    j = TextureUtil.anaglyphColor(j);
                }

                float f = (j >> 16 & 0xFF) / 255.0F;
                float f1 = (j >> 8 & 0xFF) / 255.0F;
                float f2 = (j & 0xFF) / 255.0F;
                if (separateAoLightValue) {
                    buffer.putColorMultiplierRgba(f, f1, f2, aoFace.vertexColorMultiplier[0], 4);
                    buffer.putColorMultiplierRgba(f, f1, f2, aoFace.vertexColorMultiplier[1], 3);
                    buffer.putColorMultiplierRgba(f, f1, f2, aoFace.vertexColorMultiplier[2], 2);
                    buffer.putColorMultiplierRgba(f, f1, f2, aoFace.vertexColorMultiplier[3], 1);
                } else {
                    buffer.putColorMultiplier(
                        aoFace.vertexColorMultiplier[0] * f, aoFace.vertexColorMultiplier[0] * f1, aoFace.vertexColorMultiplier[0] * f2, 4
                    );
                    buffer.putColorMultiplier(
                        aoFace.vertexColorMultiplier[1] * f, aoFace.vertexColorMultiplier[1] * f1, aoFace.vertexColorMultiplier[1] * f2, 3
                    );
                    buffer.putColorMultiplier(
                        aoFace.vertexColorMultiplier[2] * f, aoFace.vertexColorMultiplier[2] * f1, aoFace.vertexColorMultiplier[2] * f2, 2
                    );
                    buffer.putColorMultiplier(
                        aoFace.vertexColorMultiplier[3] * f, aoFace.vertexColorMultiplier[3] * f1, aoFace.vertexColorMultiplier[3] * f2, 1
                    );
                }
            }

            buffer.putPosition(d0, d1, d2);
        }
    }

    private void fillQuadBounds(Block blockIn, int[] vertexData, EnumFacing facingIn, float[] quadBounds, BitSet boundsFlags) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;
        int step = vertexData.length / 4;

        for (int i = 0; i < 4; i++) {
            float f6 = Float.intBitsToFloat(vertexData[i * step]);
            float f7 = Float.intBitsToFloat(vertexData[i * step + 1]);
            float f8 = Float.intBitsToFloat(vertexData[i * step + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (quadBounds != null) {
            quadBounds[EnumFacing.WEST.getIndex()] = f;
            quadBounds[EnumFacing.EAST.getIndex()] = f3;
            quadBounds[EnumFacing.DOWN.getIndex()] = f1;
            quadBounds[EnumFacing.UP.getIndex()] = f4;
            quadBounds[EnumFacing.NORTH.getIndex()] = f2;
            quadBounds[EnumFacing.SOUTH.getIndex()] = f5;
            int facingValuesLength = EnumFacing.VALUES.length;
            quadBounds[EnumFacing.WEST.getIndex() + facingValuesLength] = 1.0F - f;
            quadBounds[EnumFacing.EAST.getIndex() + facingValuesLength] = 1.0F - f3;
            quadBounds[EnumFacing.DOWN.getIndex() + facingValuesLength] = 1.0F - f1;
            quadBounds[EnumFacing.UP.getIndex() + facingValuesLength] = 1.0F - f4;
            quadBounds[EnumFacing.NORTH.getIndex() + facingValuesLength] = 1.0F - f2;
            quadBounds[EnumFacing.SOUTH.getIndex() + facingValuesLength] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;
        switch (facingIn) {
            case DOWN:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f1 < 1.0E-4F || blockIn.isFullCube()) && f1 == f4);
                break;
            case UP:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f4 > 0.9999F || blockIn.isFullCube()) && f1 == f4);
                break;
            case NORTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f2 < 1.0E-4F || blockIn.isFullCube()) && f2 == f5);
                break;
            case SOUTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f5 > 0.9999F || blockIn.isFullCube()) && f2 == f5);
                break;
            case WEST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f < 1.0E-4F || blockIn.isFullCube()) && f == f3);
                break;
            case EAST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f3 > 0.9999F || blockIn.isFullCube()) && f == f3);
        }
    }

    private void renderQuadsFlat(
        IBlockAccess worldIn,
        IBlockState stateIn,
        BlockPos posIn,
        EnumFacing face,
        int brightnessIn,
        boolean ownBrightness,
        WorldRenderer buffer,
        List<BakedQuad> list,
        RenderEnv renderEnv
    ) {
        Block blockIn = stateIn.getBlock();
        BitSet bitSet = renderEnv.getBoundsFlags();
        double d0 = posIn.getX();
        double d1 = posIn.getY();
        double d2 = posIn.getZ();
        EnumOffsetType block$enumoffsettype = blockIn.getOffsetType();
        if (block$enumoffsettype != EnumOffsetType.NONE) {
            int i = posIn.getX();
            int j = posIn.getZ();
            long k = i * 3129871 ^ j * 116129781L;
            k = k * k * 42317861L + k * 11L;
            d0 += ((float)(k >> 16 & 15L) / 15.0F - 0.5) * 0.5;
            d2 += ((float)(k >> 24 & 15L) / 15.0F - 0.5) * 0.5;
            if (block$enumoffsettype == EnumOffsetType.XYZ) {
                d1 += ((float)(k >> 20 & 15L) / 15.0F - 1.0) * 0.2;
            }
        }

        for (BakedQuad bakedquad : list) {
            if (ownBrightness) {
                this.fillQuadBounds(blockIn, bakedquad.getVertexData(), bakedquad.getFace(), (float[])null, bitSet);
                brightnessIn = bitSet.get(0)
                    ? blockIn.getMixedBrightnessForBlock(worldIn, posIn.offset(bakedquad.getFace()))
                    : blockIn.getMixedBrightnessForBlock(worldIn, posIn);
            }

            if (bakedquad.getSprite().isEmissive) {
                brightnessIn |= 240;
            }

            if (buffer.isMultiTexture()) {
                buffer.addVertexData(bakedquad.getVertexDataSingle());
            } else {
                buffer.addVertexData(bakedquad.getVertexData());
            }

            buffer.putSprite(bakedquad.getSprite());
            buffer.putBrightness4(brightnessIn, brightnessIn, brightnessIn, brightnessIn);
            int colorMultiplier = CustomColors.getColorMultiplier(bakedquad, stateIn, worldIn, posIn, renderEnv);
            if (bakedquad.hasTintIndex() || colorMultiplier != -1) {
                int l;
                if (colorMultiplier != -1) {
                    l = colorMultiplier;
                } else {
                    l = blockIn.colorMultiplier(worldIn, posIn, bakedquad.getTintIndex());
                }

                if (EntityRenderer.anaglyphEnable) {
                    l = TextureUtil.anaglyphColor(l);
                }

                float f = (l >> 16 & 0xFF) / 255.0F;
                float f1 = (l >> 8 & 0xFF) / 255.0F;
                float f2 = (l & 0xFF) / 255.0F;
                buffer.putColorMultiplier(f, f1, f2, 4);
                buffer.putColorMultiplier(f, f1, f2, 3);
                buffer.putColorMultiplier(f, f1, f2, 2);
                buffer.putColorMultiplier(f, f1, f2, 1);
            }

            buffer.putPosition(d0, d1, d2);
        }
    }

    public void renderModelBrightnessColor(IBakedModel bakedModel, float brightness, float red, float green, float blue) {
        for (EnumFacing enumfacing : EnumFacing.VALUES) {
            this.renderModelBrightnessColorQuads(brightness, red, green, blue, bakedModel.getFaceQuads(enumfacing));
        }

        this.renderModelBrightnessColorQuads(brightness, red, green, blue, bakedModel.getGeneralQuads());
    }

    public void renderModelBrightness(IBakedModel model, IBlockState stateIn, float brightness, boolean p_178266_4_) {
        Block block = stateIn.getBlock();
        block.setBlockBoundsForItemRender();
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        int i = block.getRenderColor(block.getStateForEntityRender(stateIn));
        if (EntityRenderer.anaglyphEnable) {
            i = TextureUtil.anaglyphColor(i);
        }

        float f = (i >> 16 & 0xFF) / 255.0F;
        float f1 = (i >> 8 & 0xFF) / 255.0F;
        float f2 = (i & 0xFF) / 255.0F;
        if (!p_178266_4_) {
            GlStateManager.color(brightness, brightness, brightness, 1.0F);
        }

        this.renderModelBrightnessColor(model, brightness, f, f1, f2);
    }

    private void renderModelBrightnessColorQuads(float brightness, float red, float green, float blue, List<BakedQuad> listQuads) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();

        for (BakedQuad bakedquad : listQuads) {
            vertexbuffer.begin(7, DefaultVertexFormats.ITEM);
            vertexbuffer.addVertexData(bakedquad.getVertexData());
            vertexbuffer.putSprite(bakedquad.getSprite());
            if (bakedquad.hasTintIndex()) {
                vertexbuffer.putColorRGB_F4(red * brightness, green * brightness, blue * brightness);
            } else {
                vertexbuffer.putColorRGB_F4(brightness, brightness, brightness);
            }

            Vec3i vec3i = bakedquad.getFace().getDirectionVec();
            vertexbuffer.putNormal(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            tessellator.draw();
        }
    }

    public static float fixAoLightValue(float val) {
        return val == 0.2F ? aoLightValueOpaque : val;
    }

    public static void updateAoLightValue() {
        aoLightValueOpaque = 1.0F - Config.getAmbientOcclusionLevel() * 0.8F;
        separateAoLightValue = Config.isShaders() && Shaders.isSeparateAo();
    }

    private void renderOverlayModels(
        IBlockAccess worldIn,
        IBakedModel modelIn,
        IBlockState stateIn,
        BlockPos posIn,
        WorldRenderer buffer,
        boolean checkSides,
        long rand,
        RenderEnv renderEnv,
        boolean smooth
    ) {
        if (renderEnv.isOverlaysRendered()) {
            for (int l = 0; l < OVERLAY_LAYERS.length; l++) {
                EnumWorldBlockLayer layer = OVERLAY_LAYERS[l];
                ListQuadsOverlay listQuadsOverlay = renderEnv.getListQuadsOverlay(layer);
                if (listQuadsOverlay.size() > 0) {
                    RegionRenderCacheBuilder rrcb = renderEnv.getRegionRenderCacheBuilder();
                    if (rrcb != null) {
                        WorldRenderer overlayBuffer = rrcb.getWorldRendererByLayer(layer);
                        if (!overlayBuffer.isDrawing()) {
                            overlayBuffer.begin(7, DefaultVertexFormats.BLOCK);
                            overlayBuffer.setTranslation(buffer.getXOffset(), buffer.getYOffset(), buffer.getZOffset());
                        }

                        for (int q = 0; q < listQuadsOverlay.size(); q++) {
                            BakedQuad quad = listQuadsOverlay.getQuad(q);
                            List<BakedQuad> listQuadSingle = listQuadsOverlay.getListQuadsSingle(quad);
                            IBlockState quadBlockState = listQuadsOverlay.getBlockState(q);
                            if (quad.getQuadEmissive() != null) {
                                listQuadsOverlay.addQuad(quad.getQuadEmissive(), quadBlockState);
                            }

                            renderEnv.reset(quadBlockState, posIn);
                            if (smooth) {
                                this.renderQuadsSmooth(worldIn, quadBlockState, posIn, overlayBuffer, listQuadSingle, renderEnv);
                            } else {
                                int col = quadBlockState.getBlock().getMixedBrightnessForBlock(worldIn, posIn.offset(quad.getFace()));
                                this.renderQuadsFlat(worldIn, quadBlockState, posIn, quad.getFace(), col, false, overlayBuffer, listQuadSingle, renderEnv);
                            }
                        }
                    }

                    listQuadsOverlay.clear();
                }
            }
        }

        if (Config.isBetterSnow() && !renderEnv.isBreakingAnimation() && BetterSnow.shouldRender(worldIn, stateIn, posIn)) {
            IBakedModel modelSnow = BetterSnow.getModelSnowLayer();
            IBlockState stateSnow = BetterSnow.getStateSnowLayer();
            this.renderModel(worldIn, modelSnow, stateSnow, posIn, buffer, checkSides);
        }
    }

    public static class AmbientOcclusionFace {
        private final float[] vertexColorMultiplier = new float[4];
        private final int[] vertexBrightness = new int[4];

        public AmbientOcclusionFace() {
            this(null);
        }

        public AmbientOcclusionFace(BlockModelRenderer bmr) {
        }

        public void setMaxBlockLight() {
            int maxBlockLight = 240;
            this.vertexBrightness[0] = this.vertexBrightness[0] | maxBlockLight;
            this.vertexBrightness[1] = this.vertexBrightness[1] | maxBlockLight;
            this.vertexBrightness[2] = this.vertexBrightness[2] | maxBlockLight;
            this.vertexBrightness[3] = this.vertexBrightness[3] | maxBlockLight;
            this.vertexColorMultiplier[0] = 1.0F;
            this.vertexColorMultiplier[1] = 1.0F;
            this.vertexColorMultiplier[2] = 1.0F;
            this.vertexColorMultiplier[3] = 1.0F;
        }

        public void updateVertexBrightness(
            IBlockAccess blockAccessIn, Block blockIn, BlockPos blockPosIn, EnumFacing facingIn, float[] quadBounds, BitSet boundsFlags
        ) {
            BlockPos blockpos = boundsFlags.get(0) ? blockPosIn.offset(facingIn) : blockPosIn;
            BlockModelRenderer.EnumNeighborInfo blockmodelrenderer$enumneighborinfo = BlockModelRenderer.EnumNeighborInfo.getNeighbourInfo(facingIn);
            BlockPos blockpos1 = blockpos.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[0]);
            BlockPos blockpos2 = blockpos.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[1]);
            BlockPos blockpos3 = blockpos.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[2]);
            BlockPos blockpos4 = blockpos.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[3]);
            int i = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos1);
            int j = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos2);
            int k = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos3);
            int l = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos4);
            float f = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos1).getBlock().getAmbientOcclusionLightValue());
            float f1 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos2).getBlock().getAmbientOcclusionLightValue());
            float f2 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos3).getBlock().getAmbientOcclusionLightValue());
            float f3 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos4).getBlock().getAmbientOcclusionLightValue());
            boolean flag = blockAccessIn.getBlockState(blockpos1.offset(facingIn)).getBlock().isTranslucent();
            boolean flag1 = blockAccessIn.getBlockState(blockpos2.offset(facingIn)).getBlock().isTranslucent();
            boolean flag2 = blockAccessIn.getBlockState(blockpos3.offset(facingIn)).getBlock().isTranslucent();
            boolean flag3 = blockAccessIn.getBlockState(blockpos4.offset(facingIn)).getBlock().isTranslucent();
            float f4;
            int i1;
            if (!flag2 && !flag) {
                f4 = f;
                i1 = i;
            } else {
                BlockPos blockpos5 = blockpos1.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[2]);
                f4 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos5).getBlock().getAmbientOcclusionLightValue());
                i1 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos5);
            }

            int j1;
            float f5;
            if (!flag3 && !flag) {
                f5 = f;
                j1 = i;
            } else {
                BlockPos blockpos6 = blockpos1.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[3]);
                f5 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos6).getBlock().getAmbientOcclusionLightValue());
                j1 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos6);
            }

            int k1;
            float f6;
            if (!flag2 && !flag1) {
                f6 = f1;
                k1 = j;
            } else {
                BlockPos blockpos7 = blockpos2.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[2]);
                f6 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos7).getBlock().getAmbientOcclusionLightValue());
                k1 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos7);
            }

            int l1;
            float f7;
            if (!flag3 && !flag1) {
                f7 = f1;
                l1 = j;
            } else {
                BlockPos blockpos8 = blockpos2.offset(blockmodelrenderer$enumneighborinfo.field_178276_g[3]);
                f7 = BlockModelRenderer.fixAoLightValue(blockAccessIn.getBlockState(blockpos8).getBlock().getAmbientOcclusionLightValue());
                l1 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockpos8);
            }

            int i3 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn);
            if (boundsFlags.get(0) || !blockAccessIn.getBlockState(blockPosIn.offset(facingIn)).getBlock().isOpaqueCube()) {
                i3 = blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn.offset(facingIn));
            }

            float f8 = boundsFlags.get(0)
                ? blockAccessIn.getBlockState(blockpos).getBlock().getAmbientOcclusionLightValue()
                : blockAccessIn.getBlockState(blockPosIn).getBlock().getAmbientOcclusionLightValue();
            f8 = BlockModelRenderer.fixAoLightValue(f8);
            BlockModelRenderer.VertexTranslations blockmodelrenderer$vertextranslations = BlockModelRenderer.VertexTranslations.getVertexTranslations(facingIn);
            if (boundsFlags.get(1) && blockmodelrenderer$enumneighborinfo.field_178289_i) {
                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f30 = (f2 + f + f4 + f8) * 0.25F;
                float f31 = (f2 + f1 + f6 + f8) * 0.25F;
                float f32 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[0].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[1].field_178229_m];
                float f14 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[2].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[3].field_178229_m];
                float f15 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[4].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[5].field_178229_m];
                float f16 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[6].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178286_j[7].field_178229_m];
                float f17 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[0].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[1].field_178229_m];
                float f18 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[2].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[3].field_178229_m];
                float f19 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[4].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[5].field_178229_m];
                float f20 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[6].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178287_k[7].field_178229_m];
                float f21 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[0].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[1].field_178229_m];
                float f22 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[2].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[3].field_178229_m];
                float f23 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[4].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[5].field_178229_m];
                float f24 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[6].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178284_l[7].field_178229_m];
                float f25 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[0].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[1].field_178229_m];
                float f26 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[2].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[3].field_178229_m];
                float f27 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[4].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[5].field_178229_m];
                float f28 = quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[6].field_178229_m]
                    * quadBounds[blockmodelrenderer$enumneighborinfo.field_178285_m[7].field_178229_m];
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178191_g] = f29 * f13 + f30 * f14 + f31 * f15 + f32 * f16;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178200_h] = f29 * f17 + f30 * f18 + f31 * f19 + f32 * f20;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178201_i] = f29 * f21 + f30 * f22 + f31 * f23 + f32 * f24;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178198_j] = f29 * f25 + f30 * f26 + f31 * f27 + f32 * f28;
                int i2 = this.getAoBrightness(l, i, j1, i3);
                int j2 = this.getAoBrightness(k, i, i1, i3);
                int k2 = this.getAoBrightness(k, j, k1, i3);
                int l2 = this.getAoBrightness(l, j, l1, i3);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178191_g] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178200_h] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178201_i] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178198_j] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
            } else {
                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178191_g] = this.getAoBrightness(l, i, j1, i3);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178200_h] = this.getAoBrightness(k, i, i1, i3);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178201_i] = this.getAoBrightness(k, j, k1, i3);
                this.vertexBrightness[blockmodelrenderer$vertextranslations.field_178198_j] = this.getAoBrightness(l, j, l1, i3);
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178191_g] = f9;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178200_h] = f10;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178201_i] = f11;
                this.vertexColorMultiplier[blockmodelrenderer$vertextranslations.field_178198_j] = f12;
            }
        }

        private int getAoBrightness(int br1, int br2, int br3, int br4) {
            if (br1 == 0) {
                br1 = br4;
            }

            if (br2 == 0) {
                br2 = br4;
            }

            if (br3 == 0) {
                br3 = br4;
            }

            return br1 + br2 + br3 + br4 >> 2 & 16711935;
        }

        private int getVertexBrightness(
            int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_, float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_
        ) {
            int i = (int)(
                    (p_178203_1_ >> 16 & 0xFF) * p_178203_5_
                        + (p_178203_2_ >> 16 & 0xFF) * p_178203_6_
                        + (p_178203_3_ >> 16 & 0xFF) * p_178203_7_
                        + (p_178203_4_ >> 16 & 0xFF) * p_178203_8_
                )
                & 0xFF;
            int j = (int)(
                    (p_178203_1_ & 0xFF) * p_178203_5_
                        + (p_178203_2_ & 0xFF) * p_178203_6_
                        + (p_178203_3_ & 0xFF) * p_178203_7_
                        + (p_178203_4_ & 0xFF) * p_178203_8_
                )
                & 0xFF;
            return i << 16 | j;
        }
    }

    public static enum EnumNeighborInfo {
        DOWN(
            new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH},
            0.5F,
            false,
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0]
        ),
        UP(
            new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH},
            1.0F,
            false,
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0],
            new BlockModelRenderer.Orientation[0]
        ),
        NORTH(
            new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST},
            0.8F,
            true,
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_WEST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_EAST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_EAST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_WEST
            }
        ),
        SOUTH(
            new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP},
            0.8F,
            true,
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.WEST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_WEST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.WEST,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.WEST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.EAST
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_EAST,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.EAST,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.EAST
            }
        ),
        WEST(
            new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH},
            0.6F,
            true,
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.SOUTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.SOUTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.NORTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.NORTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.NORTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.NORTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.SOUTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.SOUTH
            }
        ),
        EAST(
            new EnumFacing[]{EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH},
            0.6F,
            true,
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.SOUTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.SOUTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.NORTH,
                BlockModelRenderer.Orientation.FLIP_DOWN,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.DOWN,
                BlockModelRenderer.Orientation.NORTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.NORTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_NORTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.NORTH
            },
            new BlockModelRenderer.Orientation[]{
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.SOUTH,
                BlockModelRenderer.Orientation.FLIP_UP,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.FLIP_SOUTH,
                BlockModelRenderer.Orientation.UP,
                BlockModelRenderer.Orientation.SOUTH
            }
        );

        protected final EnumFacing[] field_178276_g;
        protected final float field_178288_h;
        protected final boolean field_178289_i;
        protected final BlockModelRenderer.Orientation[] field_178286_j;
        protected final BlockModelRenderer.Orientation[] field_178287_k;
        protected final BlockModelRenderer.Orientation[] field_178284_l;
        protected final BlockModelRenderer.Orientation[] field_178285_m;
        private static final BlockModelRenderer.EnumNeighborInfo[] VALUES = new BlockModelRenderer.EnumNeighborInfo[6];

        private EnumNeighborInfo(
            EnumFacing[] p_i46236_3_,
            float p_i46236_4_,
            boolean p_i46236_5_,
            BlockModelRenderer.Orientation[] p_i46236_6_,
            BlockModelRenderer.Orientation[] p_i46236_7_,
            BlockModelRenderer.Orientation[] p_i46236_8_,
            BlockModelRenderer.Orientation[] p_i46236_9_
        ) {
            this.field_178276_g = p_i46236_3_;
            this.field_178288_h = p_i46236_4_;
            this.field_178289_i = p_i46236_5_;
            this.field_178286_j = p_i46236_6_;
            this.field_178287_k = p_i46236_7_;
            this.field_178284_l = p_i46236_8_;
            this.field_178285_m = p_i46236_9_;
        }

        public static BlockModelRenderer.EnumNeighborInfo getNeighbourInfo(EnumFacing p_178273_0_) {
            return VALUES[p_178273_0_.getIndex()];
        }

        static {
            VALUES[EnumFacing.DOWN.getIndex()] = DOWN;
            VALUES[EnumFacing.UP.getIndex()] = UP;
            VALUES[EnumFacing.NORTH.getIndex()] = NORTH;
            VALUES[EnumFacing.SOUTH.getIndex()] = SOUTH;
            VALUES[EnumFacing.WEST.getIndex()] = WEST;
            VALUES[EnumFacing.EAST.getIndex()] = EAST;
        }
    }

    public static enum Orientation {
        DOWN(EnumFacing.DOWN, false),
        UP(EnumFacing.UP, false),
        NORTH(EnumFacing.NORTH, false),
        SOUTH(EnumFacing.SOUTH, false),
        WEST(EnumFacing.WEST, false),
        EAST(EnumFacing.EAST, false),
        FLIP_DOWN(EnumFacing.DOWN, true),
        FLIP_UP(EnumFacing.UP, true),
        FLIP_NORTH(EnumFacing.NORTH, true),
        FLIP_SOUTH(EnumFacing.SOUTH, true),
        FLIP_WEST(EnumFacing.WEST, true),
        FLIP_EAST(EnumFacing.EAST, true);

        protected final int field_178229_m;

        private Orientation(EnumFacing p_i46233_3_, boolean p_i46233_4_) {
            this.field_178229_m = p_i46233_3_.getIndex() + (p_i46233_4_ ? EnumFacing.values().length : 0);
        }
    }

    static enum VertexTranslations {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        private final int field_178191_g;
        private final int field_178200_h;
        private final int field_178201_i;
        private final int field_178198_j;
        private static final BlockModelRenderer.VertexTranslations[] VALUES = new BlockModelRenderer.VertexTranslations[6];

        private VertexTranslations(int p_i46234_3_, int p_i46234_4_, int p_i46234_5_, int p_i46234_6_) {
            this.field_178191_g = p_i46234_3_;
            this.field_178200_h = p_i46234_4_;
            this.field_178201_i = p_i46234_5_;
            this.field_178198_j = p_i46234_6_;
        }

        public static BlockModelRenderer.VertexTranslations getVertexTranslations(EnumFacing p_178184_0_) {
            return VALUES[p_178184_0_.getIndex()];
        }

        static {
            VALUES[EnumFacing.DOWN.getIndex()] = DOWN;
            VALUES[EnumFacing.UP.getIndex()] = UP;
            VALUES[EnumFacing.NORTH.getIndex()] = NORTH;
            VALUES[EnumFacing.SOUTH.getIndex()] = SOUTH;
            VALUES[EnumFacing.WEST.getIndex()] = WEST;
            VALUES[EnumFacing.EAST.getIndex()] = EAST;
        }
    }
}
