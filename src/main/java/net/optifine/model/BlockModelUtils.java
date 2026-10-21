package net.optifine.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block.EnumOffsetType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.BreakingFour;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.optifine.Config;
import org.lwjgl.util.vector.Vector3f;

public class BlockModelUtils {
    private static final float VERTEX_COORD_ACCURACY = 1.0E-6F;

    public static IBakedModel makeModelCube(String spriteName, int tintIndex) {
        TextureAtlasSprite sprite = Config.getMinecraft().getTextureMapBlocks().getAtlasSprite(spriteName);
        return makeModelCube(sprite, tintIndex);
    }

    public static IBakedModel makeModelCube(TextureAtlasSprite sprite, int tintIndex) {
        List generalQuads = new ArrayList();
        EnumFacing[] facings = EnumFacing.VALUES;
        List<List<BakedQuad>> faceQuads = new ArrayList<>();

        for (int i = 0; i < facings.length; i++) {
            EnumFacing facing = facings[i];
            List quads = new ArrayList();
            quads.add(makeBakedQuad(facing, sprite, tintIndex));
            faceQuads.add(quads);
        }

        IBakedModel bakedModel = new SimpleBakedModel(generalQuads, faceQuads, true, true, sprite, ItemCameraTransforms.DEFAULT);
        return bakedModel;
    }

    public static IBakedModel joinModelsCube(IBakedModel modelBase, IBakedModel modelAdd) {
        List<BakedQuad> generalQuads = new ArrayList<>();
        generalQuads.addAll(modelBase.getGeneralQuads());
        generalQuads.addAll(modelAdd.getGeneralQuads());
        EnumFacing[] facings = EnumFacing.VALUES;
        List faceQuads = new ArrayList();

        for (int i = 0; i < facings.length; i++) {
            EnumFacing facing = facings[i];
            List quads = new ArrayList();
            quads.addAll(modelBase.getFaceQuads(facing));
            quads.addAll(modelAdd.getFaceQuads(facing));
            faceQuads.add(quads);
        }

        boolean ao = modelBase.isAmbientOcclusion();
        boolean builtIn = modelBase.isBuiltInRenderer();
        TextureAtlasSprite sprite = modelBase.getParticleTexture();
        ItemCameraTransforms transforms = modelBase.getItemCameraTransforms();
        IBakedModel bakedModel = new SimpleBakedModel(generalQuads, faceQuads, ao, builtIn, sprite, transforms);
        return bakedModel;
    }

    public static BakedQuad makeBakedQuad(EnumFacing facing, TextureAtlasSprite sprite, int tintIndex) {
        Vector3f posFrom = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f posTo = new Vector3f(16.0F, 16.0F, 16.0F);
        BlockFaceUV uv = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);
        BlockPartFace face = new BlockPartFace(facing, tintIndex, "#" + facing.getName(), uv);
        ModelRotation modelRotation = ModelRotation.X0_Y0;
        BlockPartRotation partRotation = null;
        boolean uvLocked = false;
        boolean shade = true;
        FaceBakery faceBakery = new FaceBakery();
        return faceBakery.makeBakedQuad(posFrom, posTo, face, sprite, facing, modelRotation, partRotation, uvLocked, shade);
    }

    public static IBakedModel makeModel(String modelName, String spriteOldName, String spriteNewName) {
        TextureMap textureMap = Config.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite spriteOld = textureMap.getSpriteSafe(spriteOldName);
        TextureAtlasSprite spriteNew = textureMap.getSpriteSafe(spriteNewName);
        return makeModel(modelName, spriteOld, spriteNew);
    }

    public static IBakedModel makeModel(String modelName, TextureAtlasSprite spriteOld, TextureAtlasSprite spriteNew) {
        if (spriteOld != null && spriteNew != null) {
            ModelManager modelManager = Config.getModelManager();
            if (modelManager == null) {
                return null;
            } else {
                ModelResourceLocation mrl = new ModelResourceLocation(modelName, "normal");
                IBakedModel model = modelManager.getModel(mrl);
                if (model != null && model != modelManager.getMissingModel()) {
                    IBakedModel modelNew = ModelUtils.duplicateModel(model);
                    EnumFacing[] faces = EnumFacing.VALUES;

                    for (int i = 0; i < faces.length; i++) {
                        EnumFacing face = faces[i];
                        List<BakedQuad> quads = modelNew.getFaceQuads(face);
                        replaceTexture(quads, spriteOld, spriteNew);
                    }

                    List<BakedQuad> quadsGeneral = modelNew.getGeneralQuads();
                    replaceTexture(quadsGeneral, spriteOld, spriteNew);
                    return modelNew;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static void replaceTexture(List<BakedQuad> quads, TextureAtlasSprite spriteOld, TextureAtlasSprite spriteNew) {
        List<BakedQuad> quadsNew = new ArrayList<>();

        for (BakedQuad quad : quads) {
            if (quad.getSprite() == spriteOld) {
                quad = new BreakingFour(quad, spriteNew);
            }

            quadsNew.add(quad);
        }

        quads.clear();
        quads.addAll(quadsNew);
    }

    public static void snapVertexPosition(Vector3f pos) {
        pos.setX(snapVertexCoord(pos.getX()));
        pos.setY(snapVertexCoord(pos.getY()));
        pos.setZ(snapVertexCoord(pos.getZ()));
    }

    private static float snapVertexCoord(float x) {
        if (x > -1.0E-6F && x < 1.0E-6F) {
            return 0.0F;
        } else {
            return x > 0.999999F && x < 1.000001F ? 1.0F : x;
        }
    }

    public static AxisAlignedBB getOffsetBoundingBox(AxisAlignedBB aabb, EnumOffsetType offsetType, BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        long k = x * 3129871 ^ z * 116129781L;
        k = k * k * 42317861L + k * 11L;
        double dx = ((float)(k >> 16 & 15L) / 15.0F - 0.5) * 0.5;
        double dz = ((float)(k >> 24 & 15L) / 15.0F - 0.5) * 0.5;
        double dy = 0.0;
        if (offsetType == EnumOffsetType.XYZ) {
            dy = ((float)(k >> 20 & 15L) / 15.0F - 1.0) * 0.2;
        }

        return aabb.offset(dx, dy, dz);
    }
}
