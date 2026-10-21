package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.EnumFaceDirection;
import net.minecraft.client.renderer.EnumFaceDirection.Constants;
import net.minecraft.client.renderer.EnumFaceDirection.VertexInformation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;
import net.minecraftforge.client.model.ITransformation;
import net.optifine.Config;
import net.optifine.model.BlockModelUtils;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class FaceBakery {
    private static final float SCALE_ROTATION_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
    private static final float SCALE_ROTATION_GENERAL = 1.0F / (float)Math.cos(Math.PI / 4) - 1.0F;

    public BakedQuad makeBakedQuad(
        Vector3f posFrom,
        Vector3f posTo,
        BlockPartFace face,
        TextureAtlasSprite sprite,
        EnumFacing facing,
        ModelRotation modelRotationIn,
        BlockPartRotation partRotation,
        boolean uvLocked,
        boolean shade
    ) {
        return this.makeBakedQuad(posFrom, posTo, face, sprite, facing, (ITransformation)modelRotationIn, partRotation, uvLocked, shade);
    }

    public BakedQuad makeBakedQuad(
        Vector3f posFrom,
        Vector3f posTo,
        BlockPartFace face,
        TextureAtlasSprite sprite,
        EnumFacing facing,
        ITransformation modelRotationIn,
        BlockPartRotation partRotation,
        boolean uvLocked,
        boolean shade
    ) {
        int[] aint = this.makeQuadVertexData(face, sprite, facing, this.getPositionsDiv16(posFrom, posTo), modelRotationIn, partRotation, uvLocked, shade);
        EnumFacing enumfacing = getFacingFromVertexData(aint);
        if (uvLocked) {
            this.lockUv(aint, enumfacing, face.blockFaceUV, sprite);
        }

        if (partRotation == null) {
            this.applyFacing(aint, enumfacing);
        }

        if (Reflector.ForgeHooksClient_fillNormal.exists()) {
            Reflector.call(Reflector.ForgeHooksClient_fillNormal, aint, enumfacing);
        }

        return new BakedQuad(aint, face.tintIndex, enumfacing);
    }

    private int[] makeQuadVertexData(
        BlockPartFace partIn,
        TextureAtlasSprite sprite,
        EnumFacing orientation,
        float[] p_178405_4_,
        ITransformation rotationIn,
        BlockPartRotation partRotation,
        boolean uvLocked,
        boolean shade
    ) {
        int vertexSize = 28;
        if (Config.isShaders()) {
            vertexSize = 56;
        }

        int[] aint = new int[vertexSize];

        for (int i = 0; i < 4; i++) {
            this.fillVertexData(aint, i, orientation, partIn, p_178405_4_, sprite, rotationIn, partRotation, uvLocked, shade);
        }

        return aint;
    }

    private int getFaceShadeColor(EnumFacing facing) {
        float f = getFaceBrightness(facing);
        int i = MathHelper.clamp_int((int)(f * 255.0F), 0, 255);
        return 0xFF000000 | i << 16 | i << 8 | i;
    }

    public static float getFaceBrightness(EnumFacing facing) {
        switch (facing) {
            case DOWN:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel05;
                }

                return 0.5F;
            case UP:
                return 1.0F;
            case NORTH:
            case SOUTH:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel08;
                }

                return 0.8F;
            case WEST:
            case EAST:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel06;
                }

                return 0.6F;
            default:
                return 1.0F;
        }
    }

    private float[] getPositionsDiv16(Vector3f pos1, Vector3f pos2) {
        float[] afloat = new float[EnumFacing.values().length];
        afloat[Constants.WEST_INDEX] = pos1.x / 16.0F;
        afloat[Constants.DOWN_INDEX] = pos1.y / 16.0F;
        afloat[Constants.NORTH_INDEX] = pos1.z / 16.0F;
        afloat[Constants.EAST_INDEX] = pos2.x / 16.0F;
        afloat[Constants.UP_INDEX] = pos2.y / 16.0F;
        afloat[Constants.SOUTH_INDEX] = pos2.z / 16.0F;
        return afloat;
    }

    private void fillVertexData(
        int[] faceData,
        int vertexIndex,
        EnumFacing facing,
        BlockPartFace partFace,
        float[] p_178402_5_,
        TextureAtlasSprite sprite,
        ITransformation modelRotationIn,
        BlockPartRotation partRotation,
        boolean uvLocked,
        boolean shade
    ) {
        EnumFacing enumfacing = modelRotationIn.rotate(facing);
        int i = shade ? this.getFaceShadeColor(enumfacing) : -1;
        VertexInformation enumfacedirection$vertexinformation = EnumFaceDirection.getFacing(facing).getVertexInformation(vertexIndex);
        Vector3f vector3f = new Vector3f(
            p_178402_5_[enumfacedirection$vertexinformation.xIndex],
            p_178402_5_[enumfacedirection$vertexinformation.yIndex],
            p_178402_5_[enumfacedirection$vertexinformation.zIndex]
        );
        this.rotatePart(vector3f, partRotation);
        int j = this.rotateVertex(vector3f, facing, vertexIndex, modelRotationIn, uvLocked);
        BlockModelUtils.snapVertexPosition(vector3f);
        this.storeVertexData(faceData, j, vertexIndex, vector3f, i, sprite, partFace.blockFaceUV);
    }

    private void storeVertexData(
        int[] faceData, int storeIndex, int vertexIndex, Vector3f position, int shadeColor, TextureAtlasSprite sprite, BlockFaceUV faceUV
    ) {
        int step = faceData.length / 4;
        int i = storeIndex * step;
        faceData[i] = Float.floatToRawIntBits(position.x);
        faceData[i + 1] = Float.floatToRawIntBits(position.y);
        faceData[i + 2] = Float.floatToRawIntBits(position.z);
        faceData[i + 3] = shadeColor;
        faceData[i + 4] = Float.floatToRawIntBits(
            sprite.getInterpolatedU(faceUV.func_178348_a(vertexIndex) * 0.999 + faceUV.func_178348_a((vertexIndex + 2) % 4) * 0.001)
        );
        faceData[i + 4 + 1] = Float.floatToRawIntBits(
            sprite.getInterpolatedV(faceUV.func_178346_b(vertexIndex) * 0.999 + faceUV.func_178346_b((vertexIndex + 2) % 4) * 0.001)
        );
    }

    private void rotatePart(Vector3f p_178407_1_, BlockPartRotation partRotation) {
        if (partRotation != null) {
            Matrix4f matrix4f = this.getMatrixIdentity();
            Vector3f vector3f = new Vector3f(0.0F, 0.0F, 0.0F);
            switch (partRotation.axis) {
                case X:
                    Matrix4f.rotate(partRotation.angle * (float) (Math.PI / 180.0), new Vector3f(1.0F, 0.0F, 0.0F), matrix4f, matrix4f);
                    vector3f.set(0.0F, 1.0F, 1.0F);
                    break;
                case Y:
                    Matrix4f.rotate(partRotation.angle * (float) (Math.PI / 180.0), new Vector3f(0.0F, 1.0F, 0.0F), matrix4f, matrix4f);
                    vector3f.set(1.0F, 0.0F, 1.0F);
                    break;
                case Z:
                    Matrix4f.rotate(partRotation.angle * (float) (Math.PI / 180.0), new Vector3f(0.0F, 0.0F, 1.0F), matrix4f, matrix4f);
                    vector3f.set(1.0F, 1.0F, 0.0F);
            }

            if (partRotation.rescale) {
                if (Math.abs(partRotation.angle) == 22.5F) {
                    vector3f.scale(SCALE_ROTATION_22_5);
                } else {
                    vector3f.scale(SCALE_ROTATION_GENERAL);
                }

                Vector3f.add(vector3f, new Vector3f(1.0F, 1.0F, 1.0F), vector3f);
            } else {
                vector3f.set(1.0F, 1.0F, 1.0F);
            }

            this.rotateScale(p_178407_1_, new Vector3f(partRotation.origin), matrix4f, vector3f);
        }
    }

    public int rotateVertex(Vector3f position, EnumFacing facing, int vertexIndex, ModelRotation modelRotationIn, boolean uvLocked) {
        return this.rotateVertex(position, facing, vertexIndex, (ITransformation)modelRotationIn, uvLocked);
    }

    public int rotateVertex(Vector3f position, EnumFacing facing, int vertexIndex, ITransformation modelRotationIn, boolean uvLocked) {
        if (modelRotationIn == ModelRotation.X0_Y0) {
            return vertexIndex;
        } else {
            if (Reflector.ForgeHooksClient_transform.exists()) {
                Reflector.call(Reflector.ForgeHooksClient_transform, position, modelRotationIn.getMatrix());
            } else {
                this.rotateScale(position, new Vector3f(0.5F, 0.5F, 0.5F), ((ModelRotation)modelRotationIn).getMatrix4d(), new Vector3f(1.0F, 1.0F, 1.0F));
            }

            return modelRotationIn.rotate(facing, vertexIndex);
        }
    }

    private void rotateScale(Vector3f position, Vector3f rotationOrigin, Matrix4f rotationMatrix, Vector3f scale) {
        Vector4f vector4f = new Vector4f(position.x - rotationOrigin.x, position.y - rotationOrigin.y, position.z - rotationOrigin.z, 1.0F);
        Matrix4f.transform(rotationMatrix, vector4f, vector4f);
        vector4f.x = vector4f.x * scale.x;
        vector4f.y = vector4f.y * scale.y;
        vector4f.z = vector4f.z * scale.z;
        position.set(vector4f.x + rotationOrigin.x, vector4f.y + rotationOrigin.y, vector4f.z + rotationOrigin.z);
    }

    private Matrix4f getMatrixIdentity() {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        return matrix4f;
    }

    public static EnumFacing getFacingFromVertexData(int[] faceData) {
        int step = faceData.length / 4;
        int step2 = step * 2;
        int step3 = step * 3;
        Vector3f vector3f = new Vector3f(Float.intBitsToFloat(faceData[0]), Float.intBitsToFloat(faceData[1]), Float.intBitsToFloat(faceData[2]));
        Vector3f vector3f1 = new Vector3f(
            Float.intBitsToFloat(faceData[step]), Float.intBitsToFloat(faceData[step + 1]), Float.intBitsToFloat(faceData[step + 2])
        );
        Vector3f vector3f2 = new Vector3f(
            Float.intBitsToFloat(faceData[step2]), Float.intBitsToFloat(faceData[step2 + 1]), Float.intBitsToFloat(faceData[step2 + 2])
        );
        Vector3f vector3f3 = new Vector3f();
        Vector3f vector3f4 = new Vector3f();
        Vector3f vector3f5 = new Vector3f();
        Vector3f.sub(vector3f, vector3f1, vector3f3);
        Vector3f.sub(vector3f2, vector3f1, vector3f4);
        Vector3f.cross(vector3f4, vector3f3, vector3f5);
        float f = (float)Math.sqrt(vector3f5.x * vector3f5.x + vector3f5.y * vector3f5.y + vector3f5.z * vector3f5.z);
        vector3f5.x /= f;
        vector3f5.y /= f;
        vector3f5.z /= f;
        EnumFacing enumfacing = null;
        float f1 = 0.0F;

        for (EnumFacing enumfacing1 : EnumFacing.values()) {
            Vec3i vec3i = enumfacing1.getDirectionVec();
            Vector3f vector3f6 = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            float f2 = Vector3f.dot(vector3f5, vector3f6);
            if (f2 >= 0.0F && f2 > f1) {
                f1 = f2;
                enumfacing = enumfacing1;
            }
        }

        return enumfacing == null ? EnumFacing.UP : enumfacing;
    }

    public void lockUv(int[] p_178409_1_, EnumFacing facing, BlockFaceUV p_178409_3_, TextureAtlasSprite p_178409_4_) {
        for (int i = 0; i < 4; i++) {
            this.lockVertexUv(i, p_178409_1_, facing, p_178409_3_, p_178409_4_);
        }
    }

    private void applyFacing(int[] p_178408_1_, EnumFacing p_178408_2_) {
        int[] aint = new int[p_178408_1_.length];
        System.arraycopy(p_178408_1_, 0, aint, 0, p_178408_1_.length);
        float[] afloat = new float[EnumFacing.values().length];
        afloat[Constants.WEST_INDEX] = 999.0F;
        afloat[Constants.DOWN_INDEX] = 999.0F;
        afloat[Constants.NORTH_INDEX] = 999.0F;
        afloat[Constants.EAST_INDEX] = -999.0F;
        afloat[Constants.UP_INDEX] = -999.0F;
        afloat[Constants.SOUTH_INDEX] = -999.0F;
        int step = p_178408_1_.length / 4;

        for (int i = 0; i < 4; i++) {
            int j = step * i;
            float f = Float.intBitsToFloat(aint[j]);
            float f1 = Float.intBitsToFloat(aint[j + 1]);
            float f2 = Float.intBitsToFloat(aint[j + 2]);
            if (f < afloat[Constants.WEST_INDEX]) {
                afloat[Constants.WEST_INDEX] = f;
            }

            if (f1 < afloat[Constants.DOWN_INDEX]) {
                afloat[Constants.DOWN_INDEX] = f1;
            }

            if (f2 < afloat[Constants.NORTH_INDEX]) {
                afloat[Constants.NORTH_INDEX] = f2;
            }

            if (f > afloat[Constants.EAST_INDEX]) {
                afloat[Constants.EAST_INDEX] = f;
            }

            if (f1 > afloat[Constants.UP_INDEX]) {
                afloat[Constants.UP_INDEX] = f1;
            }

            if (f2 > afloat[Constants.SOUTH_INDEX]) {
                afloat[Constants.SOUTH_INDEX] = f2;
            }
        }

        EnumFaceDirection enumfacedirection = EnumFaceDirection.getFacing(p_178408_2_);

        for (int i1 = 0; i1 < 4; i1++) {
            int j1 = step * i1;
            VertexInformation enumfacedirection$vertexinformation = enumfacedirection.getVertexInformation(i1);
            float f8 = afloat[enumfacedirection$vertexinformation.xIndex];
            float f3 = afloat[enumfacedirection$vertexinformation.yIndex];
            float f4 = afloat[enumfacedirection$vertexinformation.zIndex];
            p_178408_1_[j1] = Float.floatToRawIntBits(f8);
            p_178408_1_[j1 + 1] = Float.floatToRawIntBits(f3);
            p_178408_1_[j1 + 2] = Float.floatToRawIntBits(f4);

            for (int k = 0; k < 4; k++) {
                int l = step * k;
                float f5 = Float.intBitsToFloat(aint[l]);
                float f6 = Float.intBitsToFloat(aint[l + 1]);
                float f7 = Float.intBitsToFloat(aint[l + 2]);
                if (MathHelper.epsilonEquals(f8, f5) && MathHelper.epsilonEquals(f3, f6) && MathHelper.epsilonEquals(f4, f7)) {
                    p_178408_1_[j1 + 4] = aint[l + 4];
                    p_178408_1_[j1 + 4 + 1] = aint[l + 4 + 1];
                }
            }
        }
    }

    private void lockVertexUv(int p_178401_1_, int[] p_178401_2_, EnumFacing facing, BlockFaceUV p_178401_4_, TextureAtlasSprite p_178401_5_) {
        int step = p_178401_2_.length / 4;
        int i = step * p_178401_1_;
        float f = Float.intBitsToFloat(p_178401_2_[i]);
        float f1 = Float.intBitsToFloat(p_178401_2_[i + 1]);
        float f2 = Float.intBitsToFloat(p_178401_2_[i + 2]);
        if (f < -0.1F || f >= 1.1F) {
            f -= MathHelper.floor_float(f);
        }

        if (f1 < -0.1F || f1 >= 1.1F) {
            f1 -= MathHelper.floor_float(f1);
        }

        if (f2 < -0.1F || f2 >= 1.1F) {
            f2 -= MathHelper.floor_float(f2);
        }

        float f3 = 0.0F;
        float f4 = 0.0F;
        switch (facing) {
            case DOWN:
                f3 = f * 16.0F;
                f4 = (1.0F - f2) * 16.0F;
                break;
            case UP:
                f3 = f * 16.0F;
                f4 = f2 * 16.0F;
                break;
            case NORTH:
                f3 = (1.0F - f) * 16.0F;
                f4 = (1.0F - f1) * 16.0F;
                break;
            case SOUTH:
                f3 = f * 16.0F;
                f4 = (1.0F - f1) * 16.0F;
                break;
            case WEST:
                f3 = f2 * 16.0F;
                f4 = (1.0F - f1) * 16.0F;
                break;
            case EAST:
                f3 = (1.0F - f2) * 16.0F;
                f4 = (1.0F - f1) * 16.0F;
        }

        int j = p_178401_4_.func_178345_c(p_178401_1_) * step;
        p_178401_2_[j + 4] = Float.floatToRawIntBits(p_178401_5_.getInterpolatedU(f3));
        p_178401_2_[j + 4 + 1] = Float.floatToRawIntBits(p_178401_5_.getInterpolatedV(f4));
    }
}
