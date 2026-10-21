package net.minecraft.client.renderer;

import com.google.common.primitives.Floats;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.util.TextureUtils;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

public class WorldRenderer {
    private ByteBuffer byteBuffer;
    public IntBuffer rawIntBuffer;
    private ShortBuffer rawShortBuffer;
    public FloatBuffer rawFloatBuffer;
    public int vertexCount;
    private VertexFormatElement vertexFormatElement;
    private int vertexFormatIndex;
    private boolean noColor;
    public int drawMode;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private VertexFormat vertexFormat;
    private boolean isDrawing;
    private EnumWorldBlockLayer blockLayer = null;
    private boolean[] drawnIcons = new boolean[256];
    private TextureAtlasSprite[] quadSprites = null;
    private TextureAtlasSprite[] quadSpritesPrev = null;
    private TextureAtlasSprite quadSprite = null;
    public SVertexBuilder sVertexBuilder;
    public RenderEnv renderEnv = null;
    public BitSet animatedSprites = null;
    public BitSet animatedSpritesCached = new BitSet();
    private boolean modeTriangles = false;
    private ByteBuffer byteBufferTriangles;

    public WorldRenderer(int p_i2_1_) {
        this.byteBuffer = GLAllocation.createDirectByteBuffer(p_i2_1_ * 4);
        this.rawIntBuffer = this.byteBuffer.asIntBuffer();
        this.rawShortBuffer = this.byteBuffer.asShortBuffer();
        this.rawFloatBuffer = this.byteBuffer.asFloatBuffer();
        SVertexBuilder.initVertexBuilder(this);
    }

    private void growBuffer(int p_181670_1_) {
        if (p_181670_1_ > this.rawIntBuffer.remaining()) {
            int i = this.byteBuffer.capacity();
            int j = i % 2097152;
            int k = j + (((this.rawIntBuffer.position() + p_181670_1_) * 4 - j) / 2097152 + 1) * 2097152;
            LogManager.getLogger().warn("Needed to grow BufferBuilder buffer: Old size " + i + " bytes, new size " + k + " bytes.");
            int l = this.rawIntBuffer.position();
            ByteBuffer bytebuffer = GLAllocation.createDirectByteBuffer(k);
            ((Buffer)this.byteBuffer).position(0);
            bytebuffer.put(this.byteBuffer);
            ((Buffer)bytebuffer).rewind();
            this.byteBuffer = bytebuffer;
            this.rawFloatBuffer = this.byteBuffer.asFloatBuffer();
            this.rawIntBuffer = this.byteBuffer.asIntBuffer();
            ((Buffer)this.rawIntBuffer).position(l);
            this.rawShortBuffer = this.byteBuffer.asShortBuffer();
            ((Buffer)this.rawShortBuffer).position(l << 1);
            if (this.quadSprites != null) {
                TextureAtlasSprite[] sprites = this.quadSprites;
                int quadSize = this.getBufferQuadSize();
                this.quadSprites = new TextureAtlasSprite[quadSize];
                System.arraycopy(sprites, 0, this.quadSprites, 0, Math.min(sprites.length, this.quadSprites.length));
                this.quadSpritesPrev = null;
            }
        }
    }

    public void sortVertexData(float p_181674_1_, float p_181674_2_, float p_181674_3_) {
        int i = this.vertexCount / 4;
        final float[] afloat = new float[i];

        for (int j = 0; j < i; j++) {
            afloat[j] = getDistanceSq(
                this.rawFloatBuffer,
                (float)(p_181674_1_ + this.xOffset),
                (float)(p_181674_2_ + this.yOffset),
                (float)(p_181674_3_ + this.zOffset),
                this.vertexFormat.getIntegerSize(),
                j * this.vertexFormat.getNextOffset()
            );
        }

        Integer[] ainteger = new Integer[i];

        for (int k = 0; k < ainteger.length; k++) {
            ainteger[k] = k;
        }

        Arrays.sort(ainteger, new Comparator<Integer>() {
            public int compare(Integer p_compare_1_, Integer p_compare_2_) {
                return Floats.compare(afloat[p_compare_2_], afloat[p_compare_1_]);
            }
        });
        BitSet bitset = new BitSet();
        int l = this.vertexFormat.getNextOffset();
        int[] aint = new int[l];

        for (int l1 = 0; (l1 = bitset.nextClearBit(l1)) < ainteger.length; l1++) {
            int i1 = ainteger[l1];
            if (i1 != l1) {
                ((Buffer)this.rawIntBuffer).limit(i1 * l + l);
                ((Buffer)this.rawIntBuffer).position(i1 * l);
                this.rawIntBuffer.get(aint);
                int j1 = i1;

                for (int k1 = ainteger[i1]; j1 != l1; k1 = ainteger[k1]) {
                    ((Buffer)this.rawIntBuffer).limit(k1 * l + l);
                    ((Buffer)this.rawIntBuffer).position(k1 * l);
                    IntBuffer intbuffer = this.rawIntBuffer.slice();
                    ((Buffer)this.rawIntBuffer).limit(j1 * l + l);
                    ((Buffer)this.rawIntBuffer).position(j1 * l);
                    this.rawIntBuffer.put(intbuffer);
                    bitset.set(j1);
                    j1 = k1;
                }

                ((Buffer)this.rawIntBuffer).limit(l1 * l + l);
                ((Buffer)this.rawIntBuffer).position(l1 * l);
                this.rawIntBuffer.put(aint);
            }

            bitset.set(l1);
        }

        ((Buffer)this.rawIntBuffer).limit(this.rawIntBuffer.capacity());
        ((Buffer)this.rawIntBuffer).position(this.getBufferSize());
        if (this.quadSprites != null) {
            TextureAtlasSprite[] quadSpritesSorted = new TextureAtlasSprite[this.vertexCount / 4];
            int quadStep = this.vertexFormat.getNextOffset() / 4 * 4;

            for (int ix = 0; ix < ainteger.length; ix++) {
                int indexQuad = ainteger[ix];
                quadSpritesSorted[ix] = this.quadSprites[indexQuad];
            }

            System.arraycopy(quadSpritesSorted, 0, this.quadSprites, 0, quadSpritesSorted.length);
        }
    }

    public WorldRenderer.State getVertexState() {
        ((Buffer)this.rawIntBuffer).rewind();
        int i = this.getBufferSize();
        ((Buffer)this.rawIntBuffer).limit(i);
        int[] aint = new int[i];
        this.rawIntBuffer.get(aint);
        ((Buffer)this.rawIntBuffer).limit(this.rawIntBuffer.capacity());
        ((Buffer)this.rawIntBuffer).position(i);
        TextureAtlasSprite[] quadSpritesCopy = null;
        if (this.quadSprites != null) {
            int countQuads = this.vertexCount / 4;
            quadSpritesCopy = new TextureAtlasSprite[countQuads];
            System.arraycopy(this.quadSprites, 0, quadSpritesCopy, 0, countQuads);
        }

        return new WorldRenderer.State(aint, new VertexFormat(this.vertexFormat), quadSpritesCopy);
    }

    public int getBufferSize() {
        return this.vertexCount * this.vertexFormat.getIntegerSize();
    }

    private static float getDistanceSq(FloatBuffer p_181665_0_, float p_181665_1_, float p_181665_2_, float p_181665_3_, int p_181665_4_, int p_181665_5_) {
        float f = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 0 + 0);
        float f1 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 0 + 1);
        float f2 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 0 + 2);
        float f3 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 1 + 0);
        float f4 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 1 + 1);
        float f5 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 1 + 2);
        float f6 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 2 + 0);
        float f7 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 2 + 1);
        float f8 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 2 + 2);
        float f9 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 3 + 0);
        float f10 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 3 + 1);
        float f11 = p_181665_0_.get(p_181665_5_ + p_181665_4_ * 3 + 2);
        float f12 = (f + f3 + f6 + f9) * 0.25F - p_181665_1_;
        float f13 = (f1 + f4 + f7 + f10) * 0.25F - p_181665_2_;
        float f14 = (f2 + f5 + f8 + f11) * 0.25F - p_181665_3_;
        return f12 * f12 + f13 * f13 + f14 * f14;
    }

    public void setVertexState(WorldRenderer.State state) {
        ((Buffer)this.rawIntBuffer).clear();
        this.growBuffer(state.getRawBuffer().length);
        this.rawIntBuffer.put(state.getRawBuffer());
        this.vertexCount = state.getVertexCount();
        this.vertexFormat = new VertexFormat(state.getVertexFormat());
        if (state.stateQuadSprites != null) {
            if (this.quadSprites == null) {
                this.quadSprites = this.quadSpritesPrev;
            }

            if (this.quadSprites == null || this.quadSprites.length < this.getBufferQuadSize()) {
                this.quadSprites = new TextureAtlasSprite[this.getBufferQuadSize()];
            }

            TextureAtlasSprite[] src = state.stateQuadSprites;
            System.arraycopy(src, 0, this.quadSprites, 0, src.length);
        } else {
            if (this.quadSprites != null) {
                this.quadSpritesPrev = this.quadSprites;
            }

            this.quadSprites = null;
        }
    }

    public void reset() {
        this.vertexCount = 0;
        this.vertexFormatElement = null;
        this.vertexFormatIndex = 0;
        this.quadSprite = null;
        if (SmartAnimations.isActive()) {
            if (this.animatedSprites == null) {
                this.animatedSprites = this.animatedSpritesCached;
            }

            this.animatedSprites.clear();
        } else if (this.animatedSprites != null) {
            this.animatedSprites = null;
        }

        this.modeTriangles = false;
    }

    public void begin(int glMode, VertexFormat format) {
        if (this.isDrawing) {
            throw new IllegalStateException("Already building!");
        } else {
            this.isDrawing = true;
            this.reset();
            this.drawMode = glMode;
            this.vertexFormat = format;
            this.vertexFormatElement = format.getElement(this.vertexFormatIndex);
            this.noColor = false;
            ((Buffer)this.byteBuffer).limit(this.byteBuffer.capacity());
            if (Config.isShaders()) {
                SVertexBuilder.endSetVertexFormat(this);
            }

            if (Config.isMultiTexture()) {
                if (this.blockLayer != null) {
                    if (this.quadSprites == null) {
                        this.quadSprites = this.quadSpritesPrev;
                    }

                    if (this.quadSprites == null || this.quadSprites.length < this.getBufferQuadSize()) {
                        this.quadSprites = new TextureAtlasSprite[this.getBufferQuadSize()];
                    }
                }
            } else {
                if (this.quadSprites != null) {
                    this.quadSpritesPrev = this.quadSprites;
                }

                this.quadSprites = null;
            }
        }
    }

    public WorldRenderer tex(double u, double v) {
        if (this.quadSprite != null && this.quadSprites != null) {
            u = this.quadSprite.toSingleU((float)u);
            v = this.quadSprite.toSingleV((float)v);
            this.quadSprites[this.vertexCount / 4] = this.quadSprite;
        }

        int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);
        switch (this.vertexFormatElement.getType()) {
            case FLOAT:
                this.byteBuffer.putFloat(i, (float)u);
                this.byteBuffer.putFloat(i + 4, (float)v);
                break;
            case UINT:
            case INT:
                this.byteBuffer.putInt(i, (int)u);
                this.byteBuffer.putInt(i + 4, (int)v);
                break;
            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)v);
                this.byteBuffer.putShort(i + 2, (short)u);
                break;
            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)v);
                this.byteBuffer.put(i + 1, (byte)u);
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public WorldRenderer lightmap(int p_181671_1_, int p_181671_2_) {
        int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);
        switch (this.vertexFormatElement.getType()) {
            case FLOAT:
                this.byteBuffer.putFloat(i, p_181671_1_);
                this.byteBuffer.putFloat(i + 4, p_181671_2_);
                break;
            case UINT:
            case INT:
                this.byteBuffer.putInt(i, p_181671_1_);
                this.byteBuffer.putInt(i + 4, p_181671_2_);
                break;
            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)p_181671_2_);
                this.byteBuffer.putShort(i + 2, (short)p_181671_1_);
                break;
            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)p_181671_2_);
                this.byteBuffer.put(i + 1, (byte)p_181671_1_);
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public void putBrightness4(int p_178962_1_, int p_178962_2_, int p_178962_3_, int p_178962_4_) {
        int i = (this.vertexCount - 4) * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(1) / 4;
        int j = this.vertexFormat.getNextOffset() >> 2;
        this.rawIntBuffer.put(i, p_178962_1_);
        this.rawIntBuffer.put(i + j, p_178962_2_);
        this.rawIntBuffer.put(i + j * 2, p_178962_3_);
        this.rawIntBuffer.put(i + j * 3, p_178962_4_);
    }

    public void putPosition(double x, double y, double z) {
        int i = this.vertexFormat.getIntegerSize();
        int j = (this.vertexCount - 4) * i;

        for (int k = 0; k < 4; k++) {
            int l = j + k * i;
            int i1 = l + 1;
            int j1 = i1 + 1;
            this.rawIntBuffer.put(l, Float.floatToRawIntBits((float)(x + this.xOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(l))));
            this.rawIntBuffer.put(i1, Float.floatToRawIntBits((float)(y + this.yOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(i1))));
            this.rawIntBuffer.put(j1, Float.floatToRawIntBits((float)(z + this.zOffset) + Float.intBitsToFloat(this.rawIntBuffer.get(j1))));
        }
    }

    public int getColorIndex(int p_78909_1_) {
        return ((this.vertexCount - p_78909_1_) * this.vertexFormat.getNextOffset() + this.vertexFormat.getColorOffset()) / 4;
    }

    public void putColorMultiplier(float red, float green, float blue, int p_178978_4_) {
        int i = this.getColorIndex(p_178978_4_);
        int j = -1;
        if (!this.noColor) {
            j = this.rawIntBuffer.get(i);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                int k = (int)((j & 0xFF) * red);
                int l = (int)((j >> 8 & 0xFF) * green);
                int i1 = (int)((j >> 16 & 0xFF) * blue);
                j &= -16777216;
                j = j | i1 << 16 | l << 8 | k;
            } else {
                int j1 = (int)((j >> 24 & 0xFF) * red);
                int k1 = (int)((j >> 16 & 0xFF) * green);
                int l1 = (int)((j >> 8 & 0xFF) * blue);
                j &= 255;
                j = j | j1 << 24 | k1 << 16 | l1 << 8;
            }
        }

        this.rawIntBuffer.put(i, j);
    }

    private void putColor(int argb, int p_178988_2_) {
        int i = this.getColorIndex(p_178988_2_);
        int j = argb >> 16 & 0xFF;
        int k = argb >> 8 & 0xFF;
        int l = argb & 0xFF;
        int i1 = argb >> 24 & 0xFF;
        this.putColorRGBA(i, j, k, l, i1);
    }

    public void putColorRGB_F(float red, float green, float blue, int p_178994_4_) {
        int i = this.getColorIndex(p_178994_4_);
        int j = MathHelper.clamp_int((int)(red * 255.0F), 0, 255);
        int k = MathHelper.clamp_int((int)(green * 255.0F), 0, 255);
        int l = MathHelper.clamp_int((int)(blue * 255.0F), 0, 255);
        this.putColorRGBA(i, j, k, l, 255);
    }

    public void putColorRGBA(int index, int red, int p_178972_3_, int p_178972_4_, int p_178972_5_) {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            this.rawIntBuffer.put(index, p_178972_5_ << 24 | p_178972_4_ << 16 | p_178972_3_ << 8 | red);
        } else {
            this.rawIntBuffer.put(index, red << 24 | p_178972_3_ << 16 | p_178972_4_ << 8 | p_178972_5_);
        }
    }

    public void noColor() {
        this.noColor = true;
    }

    public WorldRenderer color(float red, float green, float blue, float alpha) {
        return this.color((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
    }

    public WorldRenderer color(int red, int green, int blue, int alpha) {
        if (this.noColor) {
            return this;
        } else {
            int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);
            switch (this.vertexFormatElement.getType()) {
                case FLOAT:
                    this.byteBuffer.putFloat(i, red / 255.0F);
                    this.byteBuffer.putFloat(i + 4, green / 255.0F);
                    this.byteBuffer.putFloat(i + 8, blue / 255.0F);
                    this.byteBuffer.putFloat(i + 12, alpha / 255.0F);
                    break;
                case UINT:
                case INT:
                    this.byteBuffer.putFloat(i, red);
                    this.byteBuffer.putFloat(i + 4, green);
                    this.byteBuffer.putFloat(i + 8, blue);
                    this.byteBuffer.putFloat(i + 12, alpha);
                    break;
                case USHORT:
                case SHORT:
                    this.byteBuffer.putShort(i, (short)red);
                    this.byteBuffer.putShort(i + 2, (short)green);
                    this.byteBuffer.putShort(i + 4, (short)blue);
                    this.byteBuffer.putShort(i + 6, (short)alpha);
                    break;
                case UBYTE:
                case BYTE:
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                        this.byteBuffer.put(i, (byte)red);
                        this.byteBuffer.put(i + 1, (byte)green);
                        this.byteBuffer.put(i + 2, (byte)blue);
                        this.byteBuffer.put(i + 3, (byte)alpha);
                    } else {
                        this.byteBuffer.put(i, (byte)alpha);
                        this.byteBuffer.put(i + 1, (byte)blue);
                        this.byteBuffer.put(i + 2, (byte)green);
                        this.byteBuffer.put(i + 3, (byte)red);
                    }
            }

            this.nextVertexFormatIndex();
            return this;
        }
    }

    public void addVertexData(int[] vertexData) {
        if (Config.isShaders()) {
            SVertexBuilder.beginAddVertexData(this, vertexData);
        }

        this.growBuffer(vertexData.length);
        ((Buffer)this.rawIntBuffer).position(this.getBufferSize());
        this.rawIntBuffer.put(vertexData);
        this.vertexCount = this.vertexCount + vertexData.length / this.vertexFormat.getIntegerSize();
        if (Config.isShaders()) {
            SVertexBuilder.endAddVertexData(this);
        }
    }

    public void endVertex() {
        this.vertexCount++;
        this.growBuffer(this.vertexFormat.getIntegerSize());
        this.vertexFormatIndex = 0;
        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);
        if (Config.isShaders()) {
            SVertexBuilder.endAddVertex(this);
        }
    }

    public WorldRenderer pos(double x, double y, double z) {
        if (Config.isShaders()) {
            SVertexBuilder.beginAddVertex(this);
        }

        int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);
        switch (this.vertexFormatElement.getType()) {
            case FLOAT:
                this.byteBuffer.putFloat(i, (float)(x + this.xOffset));
                this.byteBuffer.putFloat(i + 4, (float)(y + this.yOffset));
                this.byteBuffer.putFloat(i + 8, (float)(z + this.zOffset));
                break;
            case UINT:
            case INT:
                this.byteBuffer.putInt(i, Float.floatToRawIntBits((float)(x + this.xOffset)));
                this.byteBuffer.putInt(i + 4, Float.floatToRawIntBits((float)(y + this.yOffset)));
                this.byteBuffer.putInt(i + 8, Float.floatToRawIntBits((float)(z + this.zOffset)));
                break;
            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)(x + this.xOffset));
                this.byteBuffer.putShort(i + 2, (short)(y + this.yOffset));
                this.byteBuffer.putShort(i + 4, (short)(z + this.zOffset));
                break;
            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)(x + this.xOffset));
                this.byteBuffer.put(i + 1, (byte)(y + this.yOffset));
                this.byteBuffer.put(i + 2, (byte)(z + this.zOffset));
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public void putNormal(float x, float y, float z) {
        int i = (byte)(x * 127.0F) & 255;
        int j = (byte)(y * 127.0F) & 255;
        int k = (byte)(z * 127.0F) & 255;
        int l = i | j << 8 | k << 16;
        int i1 = this.vertexFormat.getNextOffset() >> 2;
        int j1 = (this.vertexCount - 4) * i1 + this.vertexFormat.getNormalOffset() / 4;
        this.rawIntBuffer.put(j1, l);
        this.rawIntBuffer.put(j1 + i1, l);
        this.rawIntBuffer.put(j1 + i1 * 2, l);
        this.rawIntBuffer.put(j1 + i1 * 3, l);
    }

    private void nextVertexFormatIndex() {
        this.vertexFormatIndex++;
        this.vertexFormatIndex = this.vertexFormatIndex % this.vertexFormat.getElementCount();
        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);
        if (this.vertexFormatElement.getUsage() == EnumUsage.PADDING) {
            this.nextVertexFormatIndex();
        }
    }

    public WorldRenderer normal(float p_181663_1_, float p_181663_2_, float p_181663_3_) {
        int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);
        switch (this.vertexFormatElement.getType()) {
            case FLOAT:
                this.byteBuffer.putFloat(i, p_181663_1_);
                this.byteBuffer.putFloat(i + 4, p_181663_2_);
                this.byteBuffer.putFloat(i + 8, p_181663_3_);
                break;
            case UINT:
            case INT:
                this.byteBuffer.putInt(i, (int)p_181663_1_);
                this.byteBuffer.putInt(i + 4, (int)p_181663_2_);
                this.byteBuffer.putInt(i + 8, (int)p_181663_3_);
                break;
            case USHORT:
            case SHORT:
                this.byteBuffer.putShort(i, (short)((int)(p_181663_1_ * 32767.0F) & 65535));
                this.byteBuffer.putShort(i + 2, (short)((int)(p_181663_2_ * 32767.0F) & 65535));
                this.byteBuffer.putShort(i + 4, (short)((int)(p_181663_3_ * 32767.0F) & 65535));
                break;
            case UBYTE:
            case BYTE:
                this.byteBuffer.put(i, (byte)((int)(p_181663_1_ * 127.0F) & 0xFF));
                this.byteBuffer.put(i + 1, (byte)((int)(p_181663_2_ * 127.0F) & 0xFF));
                this.byteBuffer.put(i + 2, (byte)((int)(p_181663_3_ * 127.0F) & 0xFF));
        }

        this.nextVertexFormatIndex();
        return this;
    }

    public void setTranslation(double x, double y, double z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void finishDrawing() {
        if (!this.isDrawing) {
            throw new IllegalStateException("Not building!");
        } else {
            this.isDrawing = false;
            ((Buffer)this.byteBuffer).position(0);
            ((Buffer)this.byteBuffer).limit(this.getBufferSize() * 4);
        }
    }

    public ByteBuffer getByteBuffer() {
        return this.modeTriangles ? this.byteBufferTriangles : this.byteBuffer;
    }

    public VertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public int getVertexCount() {
        return this.modeTriangles ? this.vertexCount / 4 * 6 : this.vertexCount;
    }

    public int getDrawMode() {
        return this.modeTriangles ? 4 : this.drawMode;
    }

    public void putColor4(int argb) {
        for (int i = 0; i < 4; i++) {
            this.putColor(argb, i + 1);
        }
    }

    public void putColorRGB_F4(float red, float green, float blue) {
        for (int i = 0; i < 4; i++) {
            this.putColorRGB_F(red, green, blue, i + 1);
        }
    }

    public void putSprite(TextureAtlasSprite sprite) {
        if (this.animatedSprites != null && sprite != null && sprite.getAnimationIndex() >= 0) {
            this.animatedSprites.set(sprite.getAnimationIndex());
        }

        if (this.quadSprites != null) {
            int countQuads = this.vertexCount / 4;
            this.quadSprites[countQuads - 1] = sprite;
        }
    }

    public void setSprite(TextureAtlasSprite sprite) {
        if (this.animatedSprites != null && sprite != null && sprite.getAnimationIndex() >= 0) {
            this.animatedSprites.set(sprite.getAnimationIndex());
        }

        if (this.quadSprites != null) {
            this.quadSprite = sprite;
        }
    }

    public boolean isMultiTexture() {
        return this.quadSprites != null;
    }

    public void drawMultiTexture() {
        if (this.quadSprites != null) {
            int maxTextureIndex = Config.getMinecraft().getTextureMapBlocks().getCountRegisteredSprites();
            if (this.drawnIcons.length <= maxTextureIndex) {
                this.drawnIcons = new boolean[maxTextureIndex + 1];
            }

            Arrays.fill(this.drawnIcons, false);
            int texSwitch = 0;
            int grassOverlayIndex = -1;
            int countQuads = this.vertexCount / 4;

            for (int i = 0; i < countQuads; i++) {
                TextureAtlasSprite icon = this.quadSprites[i];
                if (icon != null) {
                    int iconIndex = icon.getIndexInMap();
                    if (!this.drawnIcons[iconIndex]) {
                        if (icon == TextureUtils.iconGrassSideOverlay) {
                            if (grassOverlayIndex < 0) {
                                grassOverlayIndex = i;
                            }
                        } else {
                            i = this.drawForIcon(icon, i) - 1;
                            texSwitch++;
                            if (this.blockLayer != EnumWorldBlockLayer.TRANSLUCENT) {
                                this.drawnIcons[iconIndex] = true;
                            }
                        }
                    }
                }
            }

            if (grassOverlayIndex >= 0) {
                this.drawForIcon(TextureUtils.iconGrassSideOverlay, grassOverlayIndex);
                texSwitch++;
            }

            if (texSwitch > 0) {
            }
        }
    }

    private int drawForIcon(TextureAtlasSprite sprite, int startQuadPos) {
        GL11.glBindTexture(3553, sprite.glSpriteTextureId);
        int firstRegionEnd = -1;
        int lastPos = -1;
        int countQuads = this.vertexCount / 4;

        for (int i = startQuadPos; i < countQuads; i++) {
            TextureAtlasSprite ts = this.quadSprites[i];
            if (ts == sprite) {
                if (lastPos < 0) {
                    lastPos = i;
                }
            } else if (lastPos >= 0) {
                this.draw(lastPos, i);
                if (this.blockLayer == EnumWorldBlockLayer.TRANSLUCENT) {
                    return i;
                }

                lastPos = -1;
                if (firstRegionEnd < 0) {
                    firstRegionEnd = i;
                }
            }
        }

        if (lastPos >= 0) {
            this.draw(lastPos, countQuads);
        }

        if (firstRegionEnd < 0) {
            firstRegionEnd = countQuads;
        }

        return firstRegionEnd;
    }

    private void draw(int startQuadVertex, int endQuadVertex) {
        int vxQuadCount = endQuadVertex - startQuadVertex;
        if (vxQuadCount > 0) {
            int startVertex = startQuadVertex * 4;
            int vxCount = vxQuadCount * 4;
            GL11.glDrawArrays(this.drawMode, startVertex, vxCount);
        }
    }

    public void setBlockLayer(EnumWorldBlockLayer blockLayer) {
        this.blockLayer = blockLayer;
        if (blockLayer == null) {
            if (this.quadSprites != null) {
                this.quadSpritesPrev = this.quadSprites;
            }

            this.quadSprites = null;
            this.quadSprite = null;
        }
    }

    private int getBufferQuadSize() {
        return this.rawIntBuffer.capacity() * 4 / (this.vertexFormat.getIntegerSize() * 4);
    }

    public RenderEnv getRenderEnv(IBlockState blockStateIn, BlockPos blockPosIn) {
        if (this.renderEnv == null) {
            this.renderEnv = new RenderEnv(blockStateIn, blockPosIn);
            return this.renderEnv;
        } else {
            this.renderEnv.reset(blockStateIn, blockPosIn);
            return this.renderEnv;
        }
    }

    public boolean isDrawing() {
        return this.isDrawing;
    }

    public double getXOffset() {
        return this.xOffset;
    }

    public double getYOffset() {
        return this.yOffset;
    }

    public double getZOffset() {
        return this.zOffset;
    }

    public EnumWorldBlockLayer getBlockLayer() {
        return this.blockLayer;
    }

    public void putColorMultiplierRgba(float red, float green, float blue, float alpha, int vertexIndex) {
        int index = this.getColorIndex(vertexIndex);
        int col = -1;
        if (!this.noColor) {
            col = this.rawIntBuffer.get(index);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                int r = (int)((col & 0xFF) * red);
                int g = (int)((col >> 8 & 0xFF) * green);
                int b = (int)((col >> 16 & 0xFF) * blue);
                int a = (int)((col >> 24 & 0xFF) * alpha);
                col = a << 24 | b << 16 | g << 8 | r;
            } else {
                int r = (int)((col >> 24 & 0xFF) * red);
                int g = (int)((col >> 16 & 0xFF) * green);
                int b = (int)((col >> 8 & 0xFF) * blue);
                int a = (int)((col & 0xFF) * alpha);
                col = r << 24 | g << 16 | b << 8 | a;
            }
        }

        this.rawIntBuffer.put(index, col);
    }

    public void quadsToTriangles() {
        if (this.drawMode == 7) {
            if (this.byteBufferTriangles == null) {
                this.byteBufferTriangles = GLAllocation.createDirectByteBuffer(this.byteBuffer.capacity() * 2);
            }

            if (this.byteBufferTriangles.capacity() < this.byteBuffer.capacity() * 2) {
                this.byteBufferTriangles = GLAllocation.createDirectByteBuffer(this.byteBuffer.capacity() * 2);
            }

            int vertexSize = this.vertexFormat.getNextOffset();
            int limit = this.byteBuffer.limit();
            ((Buffer)this.byteBuffer).rewind();
            ((Buffer)this.byteBufferTriangles).clear();

            for (int v = 0; v < this.vertexCount; v += 4) {
                ((Buffer)this.byteBuffer).limit((v + 3) * vertexSize);
                ((Buffer)this.byteBuffer).position(v * vertexSize);
                this.byteBufferTriangles.put(this.byteBuffer);
                ((Buffer)this.byteBuffer).limit((v + 1) * vertexSize);
                ((Buffer)this.byteBuffer).position(v * vertexSize);
                this.byteBufferTriangles.put(this.byteBuffer);
                ((Buffer)this.byteBuffer).limit((v + 2 + 2) * vertexSize);
                ((Buffer)this.byteBuffer).position((v + 2) * vertexSize);
                this.byteBufferTriangles.put(this.byteBuffer);
            }

            ((Buffer)this.byteBuffer).limit(limit);
            ((Buffer)this.byteBuffer).rewind();
            ((Buffer)this.byteBufferTriangles).flip();
            this.modeTriangles = true;
        }
    }

    public boolean isColorDisabled() {
        return this.noColor;
    }

    public class State {
        private final int[] stateRawBuffer;
        private final VertexFormat stateVertexFormat;
        private TextureAtlasSprite[] stateQuadSprites;

        public State(int[] buffer, VertexFormat format, TextureAtlasSprite[] quadSprites) {
            this.stateRawBuffer = buffer;
            this.stateVertexFormat = format;
            this.stateQuadSprites = quadSprites;
        }

        public State(int[] p_i1_2_, VertexFormat p_i1_3_) {
            this.stateRawBuffer = p_i1_2_;
            this.stateVertexFormat = p_i1_3_;
        }

        public int[] getRawBuffer() {
            return this.stateRawBuffer;
        }

        public int getVertexCount() {
            return this.stateRawBuffer.length / this.stateVertexFormat.getIntegerSize();
        }

        public VertexFormat getVertexFormat() {
            return this.stateVertexFormat;
        }
    }
}
