package net.minecraft.client.renderer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.Shaders;
import net.optifine.util.LockCounter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class GlStateManager {
    private static GlStateManager.AlphaState alphaState = new GlStateManager.AlphaState();
    private static GlStateManager.BooleanState lightingState = new GlStateManager.BooleanState(2896);
    private static GlStateManager.BooleanState[] lightState = new GlStateManager.BooleanState[8];
    private static GlStateManager.ColorMaterialState colorMaterialState = new GlStateManager.ColorMaterialState();
    private static GlStateManager.BlendState blendState = new GlStateManager.BlendState();
    private static GlStateManager.DepthState depthState = new GlStateManager.DepthState();
    private static GlStateManager.FogState fogState = new GlStateManager.FogState();
    private static GlStateManager.CullState cullState = new GlStateManager.CullState();
    private static GlStateManager.PolygonOffsetState polygonOffsetState = new GlStateManager.PolygonOffsetState();
    private static GlStateManager.ColorLogicState colorLogicState = new GlStateManager.ColorLogicState();
    private static GlStateManager.TexGenState texGenState = new GlStateManager.TexGenState();
    private static GlStateManager.ClearState clearState = new GlStateManager.ClearState();
    private static GlStateManager.StencilState stencilState = new GlStateManager.StencilState();
    private static GlStateManager.BooleanState normalizeState = new GlStateManager.BooleanState(2977);
    private static int activeTextureUnit = 0;
    private static GlStateManager.TextureState[] textureState = new GlStateManager.TextureState[32];
    private static int activeShadeModel = 7425;
    private static GlStateManager.BooleanState rescaleNormalState = new GlStateManager.BooleanState(32826);
    private static GlStateManager.ColorMask colorMaskState = new GlStateManager.ColorMask();
    private static GlStateManager.Color colorState = new GlStateManager.Color();
    public static boolean clearEnabled = true;
    private static LockCounter alphaLock = new LockCounter();
    private static GlAlphaState alphaLockState = new GlAlphaState();
    private static LockCounter blendLock = new LockCounter();
    private static GlBlendState blendLockState = new GlBlendState();
    private static boolean creatingDisplayList = false;

    public static void pushAttrib() {
        GL11.glPushAttrib(8256);
    }

    public static void popAttrib() {
        GL11.glPopAttrib();
    }

    public static void disableAlpha() {
        if (alphaLock.isLocked()) {
            alphaLockState.setDisabled();
        } else {
            alphaState.alphaTest.setDisabled();
        }
    }

    public static void enableAlpha() {
        if (alphaLock.isLocked()) {
            alphaLockState.setEnabled();
        } else {
            alphaState.alphaTest.setEnabled();
        }
    }

    public static void alphaFunc(int func, float ref) {
        if (alphaLock.isLocked()) {
            alphaLockState.setFuncRef(func, ref);
        } else {
            if (func != alphaState.func || ref != alphaState.ref) {
                alphaState.func = func;
                alphaState.ref = ref;
                GL11.glAlphaFunc(func, ref);
            }
        }
    }

    public static void enableLighting() {
        lightingState.setEnabled();
    }

    public static void disableLighting() {
        lightingState.setDisabled();
    }

    public static void enableLight(int light) {
        lightState[light].setEnabled();
    }

    public static void disableLight(int light) {
        lightState[light].setDisabled();
    }

    public static void enableColorMaterial() {
        colorMaterialState.colorMaterial.setEnabled();
    }

    public static void disableColorMaterial() {
        colorMaterialState.colorMaterial.setDisabled();
    }

    public static void colorMaterial(int face, int mode) {
        if (face != colorMaterialState.face || mode != colorMaterialState.mode) {
            colorMaterialState.face = face;
            colorMaterialState.mode = mode;
            GL11.glColorMaterial(face, mode);
        }
    }

    public static void disableDepth() {
        depthState.depthTest.setDisabled();
    }

    public static void enableDepth() {
        depthState.depthTest.setEnabled();
    }

    public static void depthFunc(int depthFunc) {
        if (depthFunc != depthState.depthFunc) {
            depthState.depthFunc = depthFunc;
            GL11.glDepthFunc(depthFunc);
        }
    }

    public static void depthMask(boolean flagIn) {
        if (flagIn != depthState.maskEnabled) {
            depthState.maskEnabled = flagIn;
            GL11.glDepthMask(flagIn);
        }
    }

    public static void disableBlend() {
        if (blendLock.isLocked()) {
            blendLockState.setDisabled();
        } else {
            blendState.blend.setDisabled();
        }
    }

    public static void enableBlend() {
        if (blendLock.isLocked()) {
            blendLockState.setEnabled();
        } else {
            blendState.blend.setEnabled();
        }
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        if (blendLock.isLocked()) {
            blendLockState.setFactors(srcFactor, dstFactor);
        } else {
            if (srcFactor != blendState.srcFactor
                || dstFactor != blendState.dstFactor
                || srcFactor != blendState.srcFactorAlpha
                || dstFactor != blendState.dstFactorAlpha) {
                blendState.srcFactor = srcFactor;
                blendState.dstFactor = dstFactor;
                blendState.srcFactorAlpha = srcFactor;
                blendState.dstFactorAlpha = dstFactor;
                if (Config.isShaders()) {
                    Shaders.uniform_blendFunc.setValue(srcFactor, dstFactor, srcFactor, dstFactor);
                }

                GL11.glBlendFunc(srcFactor, dstFactor);
            }
        }
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        if (blendLock.isLocked()) {
            blendLockState.setFactors(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        } else {
            if (srcFactor != blendState.srcFactor
                || dstFactor != blendState.dstFactor
                || srcFactorAlpha != blendState.srcFactorAlpha
                || dstFactorAlpha != blendState.dstFactorAlpha) {
                blendState.srcFactor = srcFactor;
                blendState.dstFactor = dstFactor;
                blendState.srcFactorAlpha = srcFactorAlpha;
                blendState.dstFactorAlpha = dstFactorAlpha;
                if (Config.isShaders()) {
                    Shaders.uniform_blendFunc.setValue(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
                }

                OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
            }
        }
    }

    public static void enableFog() {
        fogState.fog.setEnabled();
    }

    public static void disableFog() {
        fogState.fog.setDisabled();
    }

    public static void setFog(int param) {
        if (param != fogState.mode) {
            fogState.mode = param;
            GL11.glFogi(2917, param);
            if (Config.isShaders()) {
                Shaders.setFogMode(param);
            }
        }
    }

    public static void setFogDensity(float param) {
        if (param < 0.0F) {
            param = 0.0F;
        }

        if (param != fogState.density) {
            fogState.density = param;
            GL11.glFogf(2914, param);
            if (Config.isShaders()) {
                Shaders.setFogDensity(param);
            }
        }
    }

    public static void setFogStart(float param) {
        if (param != fogState.start) {
            fogState.start = param;
            GL11.glFogf(2915, param);
        }
    }

    public static void setFogEnd(float param) {
        if (param != fogState.end) {
            fogState.end = param;
            GL11.glFogf(2916, param);
        }
    }

    public static void glFog(int pname, FloatBuffer param) {
        GL11.glFog(pname, param);
    }

    public static void glFogi(int pname, int param) {
        GL11.glFogi(pname, param);
    }

    public static void enableCull() {
        cullState.cullFace.setEnabled();
    }

    public static void disableCull() {
        cullState.cullFace.setDisabled();
    }

    public static void cullFace(int mode) {
        if (mode != cullState.mode) {
            cullState.mode = mode;
            GL11.glCullFace(mode);
        }
    }

    public static void enablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setEnabled();
    }

    public static void disablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setDisabled();
    }

    public static void doPolygonOffset(float factor, float units) {
        if (factor != polygonOffsetState.factor || units != polygonOffsetState.units) {
            polygonOffsetState.factor = factor;
            polygonOffsetState.units = units;
            GL11.glPolygonOffset(factor, units);
        }
    }

    public static void enableColorLogic() {
        colorLogicState.colorLogicOp.setEnabled();
    }

    public static void disableColorLogic() {
        colorLogicState.colorLogicOp.setDisabled();
    }

    public static void colorLogicOp(int opcode) {
        if (opcode != colorLogicState.opcode) {
            colorLogicState.opcode = opcode;
            GL11.glLogicOp(opcode);
        }
    }

    public static void enableTexGenCoord(GlStateManager.TexGen p_179087_0_) {
        texGenCoord(p_179087_0_).textureGen.setEnabled();
    }

    public static void disableTexGenCoord(GlStateManager.TexGen p_179100_0_) {
        texGenCoord(p_179100_0_).textureGen.setDisabled();
    }

    public static void texGen(GlStateManager.TexGen texGen, int param) {
        GlStateManager.TexGenCoord glstatemanager$texgencoord = texGenCoord(texGen);
        if (param != glstatemanager$texgencoord.param) {
            glstatemanager$texgencoord.param = param;
            GL11.glTexGeni(glstatemanager$texgencoord.coord, 9472, param);
        }
    }

    public static void texGen(GlStateManager.TexGen p_179105_0_, int pname, FloatBuffer params) {
        GL11.glTexGen(texGenCoord(p_179105_0_).coord, pname, params);
    }

    private static GlStateManager.TexGenCoord texGenCoord(GlStateManager.TexGen p_179125_0_) {
        switch (p_179125_0_) {
            case S:
                return texGenState.s;
            case T:
                return texGenState.t;
            case R:
                return texGenState.r;
            case Q:
                return texGenState.q;
            default:
                return texGenState.s;
        }
    }

    public static void setActiveTexture(int texture) {
        if (activeTextureUnit != texture - OpenGlHelper.defaultTexUnit) {
            activeTextureUnit = texture - OpenGlHelper.defaultTexUnit;
            OpenGlHelper.setActiveTexture(texture);
        }
    }

    public static void enableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setEnabled();
    }

    public static void disableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setDisabled();
    }

    public static int generateTexture() {
        return GL11.glGenTextures();
    }

    public static void deleteTexture(int texture) {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);

            for (GlStateManager.TextureState glstatemanager$texturestate : textureState) {
                if (glstatemanager$texturestate.textureName == texture) {
                    glstatemanager$texturestate.textureName = 0;
                }
            }
        }
    }

    public static void bindTexture(int texture) {
        if (texture != textureState[activeTextureUnit].textureName) {
            textureState[activeTextureUnit].textureName = texture;
            GL11.glBindTexture(3553, texture);
            if (SmartAnimations.isActive()) {
                SmartAnimations.textureRendered(texture);
            }
        }
    }

    public static void enableNormalize() {
        normalizeState.setEnabled();
    }

    public static void disableNormalize() {
        normalizeState.setDisabled();
    }

    public static void shadeModel(int mode) {
        if (mode != activeShadeModel) {
            activeShadeModel = mode;
            GL11.glShadeModel(mode);
        }
    }

    public static void enableRescaleNormal() {
        rescaleNormalState.setEnabled();
    }

    public static void disableRescaleNormal() {
        rescaleNormalState.setDisabled();
    }

    public static void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        if (red != colorMaskState.red || green != colorMaskState.green || blue != colorMaskState.blue || alpha != colorMaskState.alpha) {
            colorMaskState.red = red;
            colorMaskState.green = green;
            colorMaskState.blue = blue;
            colorMaskState.alpha = alpha;
            GL11.glColorMask(red, green, blue, alpha);
        }
    }

    public static void clearDepth(double depth) {
        if (depth != clearState.depth) {
            clearState.depth = depth;
            GL11.glClearDepth(depth);
        }
    }

    public static void clearColor(float red, float green, float blue, float alpha) {
        if (red != clearState.color.red || green != clearState.color.green || blue != clearState.color.blue || alpha != clearState.color.alpha) {
            clearState.color.red = red;
            clearState.color.green = green;
            clearState.color.blue = blue;
            clearState.color.alpha = alpha;
            GL11.glClearColor(red, green, blue, alpha);
        }
    }

    public static void clear(int mask) {
        if (clearEnabled) {
            GL11.glClear(mask);
        }
    }

    public static void matrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    public static void loadIdentity() {
        GL11.glLoadIdentity();
    }

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void getFloat(int pname, FloatBuffer params) {
        GL11.glGetFloat(pname, params);
    }

    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void scale(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void multMatrix(FloatBuffer matrix) {
        GL11.glMultMatrix(matrix);
    }

    public static void color(float colorRed, float colorGreen, float colorBlue, float colorAlpha) {
        if (colorRed != colorState.red || colorGreen != colorState.green || colorBlue != colorState.blue || colorAlpha != colorState.alpha) {
            colorState.red = colorRed;
            colorState.green = colorGreen;
            colorState.blue = colorBlue;
            colorState.alpha = colorAlpha;
            GL11.glColor4f(colorRed, colorGreen, colorBlue, colorAlpha);
        }
    }

    public static void color(float colorRed, float colorGreen, float colorBlue) {
        color(colorRed, colorGreen, colorBlue, 1.0F);
    }

    public static void resetColor() {
        colorState.red = colorState.green = colorState.blue = colorState.alpha = -1.0F;
    }

    public static void glNormalPointer(int p_187446_0_, int p_187446_1_, ByteBuffer p_187446_2_) {
        GL11.glNormalPointer(p_187446_0_, p_187446_1_, p_187446_2_);
    }

    public static void glTexCoordPointer(int p_187405_0_, int p_187405_1_, int p_187405_2_, int p_187405_3_) {
        GL11.glTexCoordPointer(p_187405_0_, p_187405_1_, p_187405_2_, p_187405_3_);
    }

    public static void glTexCoordPointer(int p_187404_0_, int p_187404_1_, int p_187404_2_, ByteBuffer p_187404_3_) {
        GL11.glTexCoordPointer(p_187404_0_, p_187404_1_, p_187404_2_, p_187404_3_);
    }

    public static void glVertexPointer(int p_187420_0_, int p_187420_1_, int p_187420_2_, int p_187420_3_) {
        GL11.glVertexPointer(p_187420_0_, p_187420_1_, p_187420_2_, p_187420_3_);
    }

    public static void glVertexPointer(int p_187427_0_, int p_187427_1_, int p_187427_2_, ByteBuffer p_187427_3_) {
        GL11.glVertexPointer(p_187427_0_, p_187427_1_, p_187427_2_, p_187427_3_);
    }

    public static void glColorPointer(int p_187406_0_, int p_187406_1_, int p_187406_2_, int p_187406_3_) {
        GL11.glColorPointer(p_187406_0_, p_187406_1_, p_187406_2_, p_187406_3_);
    }

    public static void glColorPointer(int p_187400_0_, int p_187400_1_, int p_187400_2_, ByteBuffer p_187400_3_) {
        GL11.glColorPointer(p_187400_0_, p_187400_1_, p_187400_2_, p_187400_3_);
    }

    public static void glDisableClientState(int p_187429_0_) {
        GL11.glDisableClientState(p_187429_0_);
    }

    public static void glEnableClientState(int p_187410_0_) {
        GL11.glEnableClientState(p_187410_0_);
    }

    public static void glBegin(int p_187447_0_) {
        GL11.glBegin(p_187447_0_);
    }

    public static void glEnd() {
        GL11.glEnd();
    }

    public static void glDrawArrays(int p_187439_0_, int p_187439_1_, int p_187439_2_) {
        GL11.glDrawArrays(p_187439_0_, p_187439_1_, p_187439_2_);
        if (Config.isShaders() && !creatingDisplayList) {
            int countInstances = Shaders.activeProgram.getCountInstances();
            if (countInstances > 1) {
                for (int i = 1; i < countInstances; i++) {
                    Shaders.uniform_instanceId.setValue(i);
                    GL11.glDrawArrays(p_187439_0_, p_187439_1_, p_187439_2_);
                }

                Shaders.uniform_instanceId.setValue(0);
            }
        }
    }

    public static void callList(int list) {
        GL11.glCallList(list);
        if (Config.isShaders() && !creatingDisplayList) {
            int countInstances = Shaders.activeProgram.getCountInstances();
            if (countInstances > 1) {
                for (int i = 1; i < countInstances; i++) {
                    Shaders.uniform_instanceId.setValue(i);
                    GL11.glCallList(list);
                }

                Shaders.uniform_instanceId.setValue(0);
            }
        }
    }

    public static void callLists(IntBuffer lists) {
        GL11.glCallLists(lists);
        if (Config.isShaders() && !creatingDisplayList) {
            int countInstances = Shaders.activeProgram.getCountInstances();
            if (countInstances > 1) {
                for (int i = 1; i < countInstances; i++) {
                    Shaders.uniform_instanceId.setValue(i);
                    GL11.glCallLists(lists);
                }

                Shaders.uniform_instanceId.setValue(0);
            }
        }
    }

    public static void glDeleteLists(int p_187449_0_, int p_187449_1_) {
        GL11.glDeleteLists(p_187449_0_, p_187449_1_);
    }

    public static void glNewList(int p_187423_0_, int p_187423_1_) {
        GL11.glNewList(p_187423_0_, p_187423_1_);
        creatingDisplayList = true;
    }

    public static void glEndList() {
        GL11.glEndList();
        creatingDisplayList = false;
    }

    public static int glGetError() {
        return GL11.glGetError();
    }

    public static void glTexImage2D(
        int p_187419_0_,
        int p_187419_1_,
        int p_187419_2_,
        int p_187419_3_,
        int p_187419_4_,
        int p_187419_5_,
        int p_187419_6_,
        int p_187419_7_,
        IntBuffer p_187419_8_
    ) {
        GL11.glTexImage2D(p_187419_0_, p_187419_1_, p_187419_2_, p_187419_3_, p_187419_4_, p_187419_5_, p_187419_6_, p_187419_7_, p_187419_8_);
    }

    public static void glTexSubImage2D(
        int p_187414_0_,
        int p_187414_1_,
        int p_187414_2_,
        int p_187414_3_,
        int p_187414_4_,
        int p_187414_5_,
        int p_187414_6_,
        int p_187414_7_,
        IntBuffer p_187414_8_
    ) {
        GL11.glTexSubImage2D(p_187414_0_, p_187414_1_, p_187414_2_, p_187414_3_, p_187414_4_, p_187414_5_, p_187414_6_, p_187414_7_, p_187414_8_);
    }

    public static void glCopyTexSubImage2D(
        int p_187443_0_, int p_187443_1_, int p_187443_2_, int p_187443_3_, int p_187443_4_, int p_187443_5_, int p_187443_6_, int p_187443_7_
    ) {
        GL11.glCopyTexSubImage2D(p_187443_0_, p_187443_1_, p_187443_2_, p_187443_3_, p_187443_4_, p_187443_5_, p_187443_6_, p_187443_7_);
    }

    public static void glGetTexImage(int p_187433_0_, int p_187433_1_, int p_187433_2_, int p_187433_3_, IntBuffer p_187433_4_) {
        GL11.glGetTexImage(p_187433_0_, p_187433_1_, p_187433_2_, p_187433_3_, p_187433_4_);
    }

    public static void glTexParameterf(int p_187403_0_, int p_187403_1_, float p_187403_2_) {
        GL11.glTexParameterf(p_187403_0_, p_187403_1_, p_187403_2_);
    }

    public static void glTexParameteri(int p_187421_0_, int p_187421_1_, int p_187421_2_) {
        GL11.glTexParameteri(p_187421_0_, p_187421_1_, p_187421_2_);
    }

    public static int glGetTexLevelParameteri(int p_187411_0_, int p_187411_1_, int p_187411_2_) {
        return GL11.glGetTexLevelParameteri(p_187411_0_, p_187411_1_, p_187411_2_);
    }

    public static int getActiveTextureUnit() {
        return OpenGlHelper.defaultTexUnit + activeTextureUnit;
    }

    public static void bindCurrentTexture() {
        GL11.glBindTexture(3553, textureState[activeTextureUnit].textureName);
    }

    public static int getBoundTexture() {
        return textureState[activeTextureUnit].textureName;
    }

    public static void checkBoundTexture() {
        if (Config.isMinecraftThread()) {
            int glAct = GL11.glGetInteger(34016);
            int glTex = GL11.glGetInteger(32873);
            int act = getActiveTextureUnit();
            int tex = getBoundTexture();
            if (tex > 0) {
                if (glAct != act || glTex != tex) {
                    Config.dbg("checkTexture: act: " + act + ", glAct: " + glAct + ", tex: " + tex + ", glTex: " + glTex);
                }
            }
        }
    }

    public static void deleteTextures(IntBuffer buf) {
        ((Buffer)buf).rewind();

        while (buf.position() < buf.limit()) {
            int texId = buf.get();
            deleteTexture(texId);
        }

        ((Buffer)buf).rewind();
    }

    public static boolean isFogEnabled() {
        return fogState.fog.currentState;
    }

    public static void setFogEnabled(boolean state) {
        fogState.fog.setState(state);
    }

    public static void lockAlpha(GlAlphaState stateNew) {
        if (!alphaLock.isLocked()) {
            getAlphaState(alphaLockState);
            setAlphaState(stateNew);
            alphaLock.lock();
        }
    }

    public static void unlockAlpha() {
        if (alphaLock.unlock()) {
            setAlphaState(alphaLockState);
        }
    }

    public static void getAlphaState(GlAlphaState state) {
        if (alphaLock.isLocked()) {
            state.setState(alphaLockState);
        } else {
            state.setState(alphaState.alphaTest.currentState, alphaState.func, alphaState.ref);
        }
    }

    public static void setAlphaState(GlAlphaState state) {
        if (alphaLock.isLocked()) {
            alphaLockState.setState(state);
        } else {
            alphaState.alphaTest.setState(state.isEnabled());
            alphaFunc(state.getFunc(), state.getRef());
        }
    }

    public static void lockBlend(GlBlendState stateNew) {
        if (!blendLock.isLocked()) {
            getBlendState(blendLockState);
            setBlendState(stateNew);
            blendLock.lock();
        }
    }

    public static void unlockBlend() {
        if (blendLock.unlock()) {
            setBlendState(blendLockState);
        }
    }

    public static void getBlendState(GlBlendState gbs) {
        if (blendLock.isLocked()) {
            gbs.setState(blendLockState);
        } else {
            gbs.setState(blendState.blend.currentState, blendState.srcFactor, blendState.dstFactor, blendState.srcFactorAlpha, blendState.dstFactorAlpha);
        }
    }

    public static void setBlendState(GlBlendState gbs) {
        if (blendLock.isLocked()) {
            blendLockState.setState(gbs);
        } else {
            blendState.blend.setState(gbs.isEnabled());
            if (!gbs.isSeparate()) {
                blendFunc(gbs.getSrcFactor(), gbs.getDstFactor());
            } else {
                tryBlendFuncSeparate(gbs.getSrcFactor(), gbs.getDstFactor(), gbs.getSrcFactorAlpha(), gbs.getDstFactorAlpha());
            }
        }
    }

    public static void glMultiDrawArrays(int mode, IntBuffer bFirst, IntBuffer bCount) {
        GL14.glMultiDrawArrays(mode, bFirst, bCount);
        if (Config.isShaders() && !creatingDisplayList) {
            int countInstances = Shaders.activeProgram.getCountInstances();
            if (countInstances > 1) {
                for (int i = 1; i < countInstances; i++) {
                    Shaders.uniform_instanceId.setValue(i);
                    GL14.glMultiDrawArrays(mode, bFirst, bCount);
                }

                Shaders.uniform_instanceId.setValue(0);
            }
        }
    }

    static {
        for (int i = 0; i < 8; i++) {
            lightState[i] = new GlStateManager.BooleanState(16384 + i);
        }

        for (int j = 0; j < textureState.length; j++) {
            textureState[j] = new GlStateManager.TextureState();
        }
    }

    static class AlphaState {
        public GlStateManager.BooleanState alphaTest = new GlStateManager.BooleanState(3008);
        public int func = 519;
        public float ref = -1.0F;

        private AlphaState() {
        }
    }

    static class BlendState {
        public GlStateManager.BooleanState blend = new GlStateManager.BooleanState(3042);
        public int srcFactor = 1;
        public int dstFactor = 0;
        public int srcFactorAlpha = 1;
        public int dstFactorAlpha = 0;

        private BlendState() {
        }
    }

    static class BooleanState {
        private final int capability;
        private boolean currentState = false;

        public BooleanState(int capabilityIn) {
            this.capability = capabilityIn;
        }

        public void setDisabled() {
            this.setState(false);
        }

        public void setEnabled() {
            this.setState(true);
        }

        public void setState(boolean state) {
            if (state != this.currentState) {
                this.currentState = state;
                if (state) {
                    GL11.glEnable(this.capability);
                } else {
                    GL11.glDisable(this.capability);
                }
            }
        }
    }

    static class ClearState {
        public double depth = 1.0;
        public GlStateManager.Color color = new GlStateManager.Color(0.0F, 0.0F, 0.0F, 0.0F);
        public int field_179204_c = 0;

        private ClearState() {
        }
    }

    static class Color {
        public float red = 1.0F;
        public float green = 1.0F;
        public float blue = 1.0F;
        public float alpha = 1.0F;

        public Color() {
        }

        public Color(float redIn, float greenIn, float blueIn, float alphaIn) {
            this.red = redIn;
            this.green = greenIn;
            this.blue = blueIn;
            this.alpha = alphaIn;
        }
    }

    static class ColorLogicState {
        public GlStateManager.BooleanState colorLogicOp = new GlStateManager.BooleanState(3058);
        public int opcode = 5379;

        private ColorLogicState() {
        }
    }

    static class ColorMask {
        public boolean red = true;
        public boolean green = true;
        public boolean blue = true;
        public boolean alpha = true;

        private ColorMask() {
        }
    }

    static class ColorMaterialState {
        public GlStateManager.BooleanState colorMaterial = new GlStateManager.BooleanState(2903);
        public int face = 1032;
        public int mode = 5634;

        private ColorMaterialState() {
        }
    }

    static class CullState {
        public GlStateManager.BooleanState cullFace = new GlStateManager.BooleanState(2884);
        public int mode = 1029;

        private CullState() {
        }
    }

    static class DepthState {
        public GlStateManager.BooleanState depthTest = new GlStateManager.BooleanState(2929);
        public boolean maskEnabled = true;
        public int depthFunc = 513;

        private DepthState() {
        }
    }

    static class FogState {
        public GlStateManager.BooleanState fog = new GlStateManager.BooleanState(2912);
        public int mode = 2048;
        public float density = 1.0F;
        public float start = 0.0F;
        public float end = 1.0F;

        private FogState() {
        }
    }

    static class PolygonOffsetState {
        public GlStateManager.BooleanState polygonOffsetFill = new GlStateManager.BooleanState(32823);
        public GlStateManager.BooleanState polygonOffsetLine = new GlStateManager.BooleanState(10754);
        public float factor = 0.0F;
        public float units = 0.0F;

        private PolygonOffsetState() {
        }
    }

    static class StencilFunc {
        public int field_179081_a = 519;
        public int field_179079_b = 0;
        public int field_179080_c = -1;

        private StencilFunc() {
        }
    }

    static class StencilState {
        public GlStateManager.StencilFunc field_179078_a = new GlStateManager.StencilFunc();
        public int field_179076_b = -1;
        public int field_179077_c = 7680;
        public int field_179074_d = 7680;
        public int field_179075_e = 7680;

        private StencilState() {
        }
    }

    public static enum TexGen {
        S,
        T,
        R,
        Q;
    }

    static class TexGenCoord {
        public GlStateManager.BooleanState textureGen;
        public int coord;
        public int param = -1;

        public TexGenCoord(int coordIn, int capabilityIn) {
            this.coord = coordIn;
            this.textureGen = new GlStateManager.BooleanState(capabilityIn);
        }
    }

    static class TexGenState {
        public GlStateManager.TexGenCoord s = new GlStateManager.TexGenCoord(8192, 3168);
        public GlStateManager.TexGenCoord t = new GlStateManager.TexGenCoord(8193, 3169);
        public GlStateManager.TexGenCoord r = new GlStateManager.TexGenCoord(8194, 3170);
        public GlStateManager.TexGenCoord q = new GlStateManager.TexGenCoord(8195, 3171);

        private TexGenState() {
        }
    }

    static class TextureState {
        public GlStateManager.BooleanState texture2DState = new GlStateManager.BooleanState(3553);
        public int textureName = 0;

        private TextureState() {
        }
    }
}
