package net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.StitcherException;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.optifine.BetterGrass;
import net.optifine.Config;
import net.optifine.ConnectedTextures;
import net.optifine.CustomItems;
import net.optifine.EmissiveTextures;
import net.optifine.SmartAnimations;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.shaders.ShadersTex;
import net.optifine.util.CounterInt;
import net.optifine.util.TextureUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextureMap extends AbstractTexture implements ITickableTextureObject {
    private static final boolean ENABLE_SKIP = Boolean.parseBoolean(System.getProperty("fml.skipFirstTextureLoad", "true"));
    private static final Logger logger = LogManager.getLogger();
    public static final ResourceLocation LOCATION_MISSING_TEXTURE = new ResourceLocation("missingno");
    public static final ResourceLocation locationBlocksTexture = new ResourceLocation("textures/atlas/blocks.png");
    private final List<TextureAtlasSprite> listAnimatedSprites;
    private final Map<String, TextureAtlasSprite> mapRegisteredSprites;
    private final Map<String, TextureAtlasSprite> mapUploadedSprites;
    private final String basePath;
    private final IIconCreator iconCreator;
    private int mipmapLevels;
    private final TextureAtlasSprite missingImage;
    private boolean skipFirst = false;
    private TextureAtlasSprite[] iconGrid = null;
    private int iconGridSize = -1;
    private int iconGridCountX = -1;
    private int iconGridCountY = -1;
    private double iconGridSizeU = -1.0;
    private double iconGridSizeV = -1.0;
    private CounterInt counterIndexInMap = new CounterInt(0);
    public int atlasWidth = 0;
    public int atlasHeight = 0;
    private int countAnimationsActive;
    private int frameCountAnimations;

    public TextureMap(String basePathIn) {
        this(basePathIn, (IIconCreator)null);
    }

    public TextureMap(String basePathIn, boolean skipFirst) {
        this(basePathIn, (IIconCreator)null, skipFirst);
    }

    public TextureMap(String basePathIn, IIconCreator iconCreatorIn) {
        this(basePathIn, iconCreatorIn, false);
    }

    public TextureMap(String basePathIn, IIconCreator iconCreatorIn, boolean skipFirst) {
        this.listAnimatedSprites = Lists.newArrayList();
        this.mapRegisteredSprites = Maps.newHashMap();
        this.mapUploadedSprites = Maps.newHashMap();
        this.missingImage = new TextureAtlasSprite("missingno");
        this.basePath = basePathIn;
        this.iconCreator = iconCreatorIn;
        this.skipFirst = skipFirst && ENABLE_SKIP;
    }

    private void initMissingImage() {
        int size = this.getMinSpriteSize();
        int[] aint = this.getMissingImageData(size);
        this.missingImage.setIconWidth(size);
        this.missingImage.setIconHeight(size);
        int[][] aint1 = new int[this.mipmapLevels + 1][];
        aint1[0] = aint;
        this.missingImage.setFramesTextureData(Lists.newArrayList(new int[][][]{aint1}));
        this.missingImage.setIndexInMap(this.counterIndexInMap.nextValue());
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        if (this.iconCreator != null) {
            this.loadSprites(resourceManager, this.iconCreator);
        }
    }

    public void loadSprites(IResourceManager resourceManager, IIconCreator p_174943_2_) {
        this.mapRegisteredSprites.clear();
        this.counterIndexInMap.reset();
        p_174943_2_.registerSprites(this);
        if (this.mipmapLevels >= 4) {
            this.mipmapLevels = this.detectMaxMipmapLevel(this.mapRegisteredSprites, resourceManager);
            Config.log("Mipmap levels: " + this.mipmapLevels);
        }

        this.initMissingImage();
        this.deleteGlTexture();
        this.loadTextureAtlas(resourceManager);
    }

    public void loadTextureAtlas(IResourceManager resourceManager) {
        Config.dbg("Multitexture: " + Config.isMultiTexture());
        if (Config.isMultiTexture()) {
            for (TextureAtlasSprite ts : this.mapUploadedSprites.values()) {
                ts.deleteSpriteTexture();
            }
        }

        ConnectedTextures.updateIcons(this);
        CustomItems.updateIcons(this);
        BetterGrass.updateIcons(this);
        int i = TextureUtils.getGLMaximumTextureSize();
        Stitcher stitcher = new Stitcher(i, i, true, 0, this.mipmapLevels);
        this.mapUploadedSprites.clear();
        this.listAnimatedSprites.clear();
        int j = Integer.MAX_VALUE;
        Reflector.callVoid(Reflector.ForgeHooksClient_onTextureStitchedPre, this);
        int minSpriteSize = this.getMinSpriteSize();
        this.iconGridSize = minSpriteSize;
        int k = 1 << this.mipmapLevels;
        int countCustomLoader = 0;
        int countCustomLoaderSkipped = 0;

        for (Entry<String, TextureAtlasSprite> entry : this.mapRegisteredSprites.entrySet()) {
            if (!this.skipFirst) {
                TextureAtlasSprite textureatlassprite = entry.getValue();
                ResourceLocation resourcelocation = new ResourceLocation(textureatlassprite.getIconName());
                ResourceLocation resourcelocation1 = this.completeResourceLocation(resourcelocation, 0);
                textureatlassprite.updateIndexInMap(this.counterIndexInMap);
                if (textureatlassprite.hasCustomLoader(resourceManager, resourcelocation)) {
                    if (!textureatlassprite.load(resourceManager, resourcelocation)) {
                        j = Math.min(j, Math.min(textureatlassprite.getIconWidth(), textureatlassprite.getIconHeight()));
                        stitcher.addSprite(textureatlassprite);
                        Config.detail("Custom loader (skipped): " + textureatlassprite);
                        countCustomLoaderSkipped++;
                    }

                    Config.detail("Custom loader: " + textureatlassprite);
                    countCustomLoader++;
                } else {
                    try {
                        IResource iresource = resourceManager.getResource(resourcelocation1);
                        BufferedImage[] abufferedimage = new BufferedImage[1 + this.mipmapLevels];
                        abufferedimage[0] = TextureUtil.readBufferedImage(iresource.getInputStream());
                        int ws = abufferedimage[0].getWidth();
                        int hs = abufferedimage[0].getHeight();
                        if (ws < 1 || hs < 1) {
                            Config.warn("Invalid sprite size: " + textureatlassprite);
                            continue;
                        }

                        if (ws < minSpriteSize || this.mipmapLevels > 0) {
                            int ws2 = this.mipmapLevels > 0 ? TextureUtils.scaleToGrid(ws, minSpriteSize) : TextureUtils.scaleToMin(ws, minSpriteSize);
                            if (ws2 != ws) {
                                if (!TextureUtils.isPowerOfTwo(ws)) {
                                    Config.log("Scaled non power of 2: " + textureatlassprite.getIconName() + ", " + ws + " -> " + ws2);
                                } else {
                                    Config.log("Scaled too small texture: " + textureatlassprite.getIconName() + ", " + ws + " -> " + ws2);
                                }

                                int hs2 = hs * ws2 / ws;
                                abufferedimage[0] = TextureUtils.scaleImage(abufferedimage[0], ws2);
                            }
                        }

                        TextureMetadataSection texturemetadatasection = iresource.getMetadata("texture");
                        if (texturemetadatasection != null) {
                            List<Integer> list = texturemetadatasection.getListMipmaps();
                            if (!list.isEmpty()) {
                                int l = abufferedimage[0].getWidth();
                                int i1 = abufferedimage[0].getHeight();
                                if (MathHelper.roundUpToPowerOfTwo(l) != l || MathHelper.roundUpToPowerOfTwo(i1) != i1) {
                                    throw new RuntimeException("Unable to load extra miplevels, source-texture is not power of two");
                                }
                            }

                            for (int i2 : list) {
                                if (i2 > 0 && i2 < abufferedimage.length - 1 && abufferedimage[i2] == null) {
                                    ResourceLocation resourcelocation2 = this.completeResourceLocation(resourcelocation, i2);

                                    try {
                                        abufferedimage[i2] = TextureUtil.readBufferedImage(resourceManager.getResource(resourcelocation2).getInputStream());
                                    } catch (IOException var29) {
                                        logger.error("Unable to load miplevel {} from: {}", i2, resourcelocation2, var29);
                                    }
                                }
                            }
                        }

                        AnimationMetadataSection animationmetadatasection = iresource.getMetadata("animation");
                        textureatlassprite.loadSprite(abufferedimage, animationmetadatasection);
                    } catch (RuntimeException var30) {
                        logger.error("Unable to parse metadata from " + resourcelocation1, var30);
                        ReflectorForge.FMLClientHandler_trackBrokenTexture(resourcelocation1, var30.getMessage());
                        continue;
                    } catch (IOException var31) {
                        logger.error("Using missing texture, unable to load " + resourcelocation1 + ", " + var31.getClass().getName());
                        ReflectorForge.FMLClientHandler_trackMissingTexture(resourcelocation1);
                        continue;
                    }

                    j = Math.min(j, Math.min(textureatlassprite.getIconWidth(), textureatlassprite.getIconHeight()));
                    int l1 = Math.min(Integer.lowestOneBit(textureatlassprite.getIconWidth()), Integer.lowestOneBit(textureatlassprite.getIconHeight()));
                    if (l1 < k) {
                        logger.warn(
                            "Texture {} with size {}x{} limits mip level from {} to {}",
                            resourcelocation1,
                            textureatlassprite.getIconWidth(),
                            textureatlassprite.getIconHeight(),
                            MathHelper.calculateLogBaseTwo(k),
                            MathHelper.calculateLogBaseTwo(l1)
                        );
                        k = l1;
                    }

                    stitcher.addSprite(textureatlassprite);
                }
                continue;
            }
            break;
        }

        if (countCustomLoader > 0) {
            Config.dbg("Custom loader sprites: " + countCustomLoader);
        }

        if (countCustomLoaderSkipped > 0) {
            Config.dbg("Custom loader sprites (skipped): " + countCustomLoaderSkipped);
        }

        int j1 = Math.min(j, k);
        int k1 = MathHelper.calculateLogBaseTwo(j1);
        if (k1 < 0) {
            k1 = 0;
        }

        if (k1 < this.mipmapLevels) {
            logger.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.basePath, this.mipmapLevels, k1, j1);
            this.mipmapLevels = k1;
        }

        for (final TextureAtlasSprite textureatlassprite1 : this.mapRegisteredSprites.values()) {
            if (this.skipFirst) {
                break;
            }

            try {
                textureatlassprite1.generateMipmaps(this.mipmapLevels);
            } catch (Throwable var28) {
                CrashReport crashreport = CrashReport.makeCrashReport(var28, "Applying mipmap");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Sprite being mipmapped");
                crashreportcategory.addCrashSectionCallable("Sprite name", new Callable<String>() {
                    public String call() throws Exception {
                        return textureatlassprite1.getIconName();
                    }
                });
                crashreportcategory.addCrashSectionCallable("Sprite size", new Callable<String>() {
                    public String call() throws Exception {
                        return textureatlassprite1.getIconWidth() + " x " + textureatlassprite1.getIconHeight();
                    }
                });
                crashreportcategory.addCrashSectionCallable("Sprite frames", new Callable<String>() {
                    public String call() throws Exception {
                        return textureatlassprite1.getFrameCount() + " frames";
                    }
                });
                crashreportcategory.addCrashSection("Mipmap levels", this.mipmapLevels);
                throw new ReportedException(crashreport);
            }
        }

        this.missingImage.generateMipmaps(this.mipmapLevels);
        stitcher.addSprite(this.missingImage);
        this.skipFirst = false;

        try {
            stitcher.doStitch();
        } catch (StitcherException var27) {
            throw var27;
        }

        logger.info("Created: {}x{} {}-atlas", stitcher.getCurrentWidth(), stitcher.getCurrentHeight(), this.basePath);
        if (Config.isShaders()) {
            ShadersTex.allocateTextureMap(this.getGlTextureId(), this.mipmapLevels, stitcher.getCurrentWidth(), stitcher.getCurrentHeight(), stitcher, this);
        } else {
            TextureUtil.allocateTextureImpl(this.getGlTextureId(), this.mipmapLevels, stitcher.getCurrentWidth(), stitcher.getCurrentHeight());
        }

        Map<String, TextureAtlasSprite> map = Maps.newHashMap(this.mapRegisteredSprites);

        for (TextureAtlasSprite textureatlassprite2 : stitcher.getStichSlots()) {
            String s = textureatlassprite2.getIconName();
            map.remove(s);
            this.mapUploadedSprites.put(s, textureatlassprite2);

            try {
                if (Config.isShaders()) {
                    ShadersTex.uploadTexSubForLoadAtlas(
                        this,
                        textureatlassprite2.getIconName(),
                        textureatlassprite2.getFrameTextureData(0),
                        textureatlassprite2.getIconWidth(),
                        textureatlassprite2.getIconHeight(),
                        textureatlassprite2.getOriginX(),
                        textureatlassprite2.getOriginY(),
                        false,
                        false
                    );
                } else {
                    TextureUtil.uploadTextureMipmap(
                        textureatlassprite2.getFrameTextureData(0),
                        textureatlassprite2.getIconWidth(),
                        textureatlassprite2.getIconHeight(),
                        textureatlassprite2.getOriginX(),
                        textureatlassprite2.getOriginY(),
                        false,
                        false
                    );
                }
            } catch (Throwable var26) {
                CrashReport crashreport1 = CrashReport.makeCrashReport(var26, "Stitching texture atlas");
                CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Texture being stitched together");
                crashreportcategory1.addCrashSection("Atlas path", this.basePath);
                crashreportcategory1.addCrashSection("Sprite", textureatlassprite2);
                throw new ReportedException(crashreport1);
            }

            if (textureatlassprite2.hasAnimationMetadata()) {
                textureatlassprite2.setAnimationIndex(this.listAnimatedSprites.size());
                this.listAnimatedSprites.add(textureatlassprite2);
            }
        }

        for (TextureAtlasSprite textureatlassprite3 : map.values()) {
            textureatlassprite3.copyFrom(this.missingImage);
        }

        Config.log("Animated sprites: " + this.listAnimatedSprites.size());
        if (Config.isMultiTexture()) {
            int sheetWidth = stitcher.getCurrentWidth();
            int sheetHeight = stitcher.getCurrentHeight();

            for (TextureAtlasSprite tas : stitcher.getStichSlots()) {
                tas.sheetWidth = sheetWidth;
                tas.sheetHeight = sheetHeight;
                tas.mipmapLevels = this.mipmapLevels;
                TextureAtlasSprite ss = tas.spriteSingle;
                if (ss != null) {
                    if (ss.getIconWidth() <= 0) {
                        ss.setIconWidth(tas.getIconWidth());
                        ss.setIconHeight(tas.getIconHeight());
                        ss.initSprite(tas.getIconWidth(), tas.getIconHeight(), 0, 0, false);
                        ss.clearFramesTextureData();
                        List<int[][]> frameDatas = tas.getFramesTextureData();
                        ss.setFramesTextureData(frameDatas);
                        ss.setAnimationMetadata(tas.getAnimationMetadata());
                    }

                    ss.sheetWidth = sheetWidth;
                    ss.sheetHeight = sheetHeight;
                    ss.mipmapLevels = this.mipmapLevels;
                    ss.setAnimationIndex(tas.getAnimationIndex());
                    tas.bindSpriteTexture();
                    boolean texBlur = false;
                    boolean texClamp = true;

                    try {
                        TextureUtil.uploadTextureMipmap(
                            ss.getFrameTextureData(0), ss.getIconWidth(), ss.getIconHeight(), ss.getOriginX(), ss.getOriginY(), texBlur, texClamp
                        );
                    } catch (Exception var25) {
                        Config.dbg("Error uploading sprite single: " + ss + ", parent: " + tas);
                        var25.printStackTrace();
                    }
                }
            }

            Config.getMinecraft().getTextureManager().bindTexture(locationBlocksTexture);
        }

        Reflector.callVoid(Reflector.ForgeHooksClient_onTextureStitchedPost, this);
        this.updateIconGrid(stitcher.getCurrentWidth(), stitcher.getCurrentHeight());
        if (Config.equals(System.getProperty("saveTextureMap"), "true")) {
            Config.dbg("Exporting texture map: " + this.basePath);
            TextureUtils.saveGlTexture(
                "debug/" + this.basePath.replaceAll("/", "_"),
                this.getGlTextureId(),
                this.mipmapLevels,
                stitcher.getCurrentWidth(),
                stitcher.getCurrentHeight()
            );
        }
    }

    public ResourceLocation completeResourceLocation(ResourceLocation p_147634_1_) {
        return this.completeResourceLocation(p_147634_1_, 0);
    }

    public ResourceLocation completeResourceLocation(ResourceLocation location, int p_147634_2_) {
        if (this.isAbsoluteLocation(location)) {
            return new ResourceLocation(location.getResourceDomain(), location.getResourcePath() + ".png");
        } else {
            return p_147634_2_ == 0
                ? new ResourceLocation(location.getResourceDomain(), String.format("%s/%s%s", this.basePath, location.getResourcePath(), ".png"))
                : new ResourceLocation(
                    location.getResourceDomain(), String.format("%s/mipmaps/%s.%d%s", this.basePath, location.getResourcePath(), p_147634_2_, ".png")
                );
        }
    }

    public TextureAtlasSprite getAtlasSprite(String iconName) {
        TextureAtlasSprite textureatlassprite = this.mapUploadedSprites.get(iconName);
        if (textureatlassprite == null) {
            textureatlassprite = this.missingImage;
        }

        return textureatlassprite;
    }

    public void updateAnimations() {
        boolean hasNormal = false;
        boolean hasSpecular = false;
        TextureUtil.bindTexture(this.getGlTextureId());
        int countActive = 0;

        for (TextureAtlasSprite textureatlassprite : this.listAnimatedSprites) {
            if (this.isTerrainAnimationActive(textureatlassprite)) {
                textureatlassprite.updateAnimation();
                if (textureatlassprite.isAnimationActive()) {
                    countActive++;
                }

                if (textureatlassprite.spriteNormal != null) {
                    hasNormal = true;
                }

                if (textureatlassprite.spriteSpecular != null) {
                    hasSpecular = true;
                }
            }
        }

        if (Config.isMultiTexture()) {
            for (TextureAtlasSprite ts : this.listAnimatedSprites) {
                if (this.isTerrainAnimationActive(ts)) {
                    TextureAtlasSprite spriteSingle = ts.spriteSingle;
                    if (spriteSingle != null) {
                        if (ts == TextureUtils.iconClock || ts == TextureUtils.iconCompass) {
                            spriteSingle.frameCounter = ts.frameCounter;
                        }

                        ts.bindSpriteTexture();
                        spriteSingle.updateAnimation();
                        if (spriteSingle.isAnimationActive()) {
                            countActive++;
                        }
                    }
                }
            }

            TextureUtil.bindTexture(this.getGlTextureId());
        }

        if (Config.isShaders()) {
            if (hasNormal) {
                TextureUtil.bindTexture(this.getMultiTexID().norm);

                for (TextureAtlasSprite textureatlasspritex : this.listAnimatedSprites) {
                    if (textureatlasspritex.spriteNormal != null && this.isTerrainAnimationActive(textureatlasspritex)) {
                        if (textureatlasspritex == TextureUtils.iconClock || textureatlasspritex == TextureUtils.iconCompass) {
                            textureatlasspritex.spriteNormal.frameCounter = textureatlasspritex.frameCounter;
                        }

                        textureatlasspritex.spriteNormal.updateAnimation();
                        if (textureatlasspritex.spriteNormal.isAnimationActive()) {
                            countActive++;
                        }
                    }
                }
            }

            if (hasSpecular) {
                TextureUtil.bindTexture(this.getMultiTexID().spec);

                for (TextureAtlasSprite textureatlasspritexx : this.listAnimatedSprites) {
                    if (textureatlasspritexx.spriteSpecular != null && this.isTerrainAnimationActive(textureatlasspritexx)) {
                        if (textureatlasspritexx == TextureUtils.iconClock || textureatlasspritexx == TextureUtils.iconCompass) {
                            textureatlasspritexx.spriteNormal.frameCounter = textureatlasspritexx.frameCounter;
                        }

                        textureatlasspritexx.spriteSpecular.updateAnimation();
                        if (textureatlasspritexx.spriteSpecular.isAnimationActive()) {
                            countActive++;
                        }
                    }
                }
            }

            if (hasNormal || hasSpecular) {
                TextureUtil.bindTexture(this.getGlTextureId());
            }
        }

        int frameCount = Config.getMinecraft().entityRenderer.frameCount;
        if (frameCount != this.frameCountAnimations) {
            this.countAnimationsActive = countActive;
            this.frameCountAnimations = frameCount;
        }

        if (SmartAnimations.isActive()) {
            SmartAnimations.resetSpritesRendered();
        }
    }

    public TextureAtlasSprite registerSprite(ResourceLocation location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null!");
        } else {
            TextureAtlasSprite textureatlassprite = this.mapRegisteredSprites.get(location.toString());
            if (textureatlassprite == null) {
                textureatlassprite = TextureAtlasSprite.makeAtlasSprite(location);
                this.mapRegisteredSprites.put(location.toString(), textureatlassprite);
                textureatlassprite.updateIndexInMap(this.counterIndexInMap);
                if (Config.isEmissiveTextures()) {
                    this.checkEmissive(location, textureatlassprite);
                }
            }

            return textureatlassprite;
        }
    }

    @Override
    public void tick() {
        this.updateAnimations();
    }

    public void setMipmapLevels(int mipmapLevelsIn) {
        this.mipmapLevels = mipmapLevelsIn;
    }

    public TextureAtlasSprite getMissingSprite() {
        return this.missingImage;
    }

    public TextureAtlasSprite getTextureExtry(String name) {
        return this.mapRegisteredSprites.get(name);
    }

    public boolean setTextureEntry(String name, TextureAtlasSprite entry) {
        if (!this.mapRegisteredSprites.containsKey(name)) {
            this.mapRegisteredSprites.put(name, entry);
            entry.updateIndexInMap(this.counterIndexInMap);
            return true;
        } else {
            return false;
        }
    }

    public boolean setTextureEntry(TextureAtlasSprite entry) {
        return this.setTextureEntry(entry.getIconName(), entry);
    }

    public String getBasePath() {
        return this.basePath;
    }

    public int getMipmapLevels() {
        return this.mipmapLevels;
    }

    private boolean isAbsoluteLocation(ResourceLocation loc) {
        String path = loc.getResourcePath();
        return this.isAbsoluteLocationPath(path);
    }

    private boolean isAbsoluteLocationPath(String resPath) {
        String path = resPath.toLowerCase();
        return path.startsWith("mcpatcher/") || path.startsWith("optifine/");
    }

    public TextureAtlasSprite getSpriteSafe(String name) {
        ResourceLocation loc = new ResourceLocation(name);
        return this.mapRegisteredSprites.get(loc.toString());
    }

    public TextureAtlasSprite getRegisteredSprite(ResourceLocation loc) {
        return this.mapRegisteredSprites.get(loc.toString());
    }

    private boolean isTerrainAnimationActive(TextureAtlasSprite ts) {
        if (ts == TextureUtils.iconWaterStill || ts == TextureUtils.iconWaterFlow) {
            return Config.isAnimatedWater();
        } else if (ts == TextureUtils.iconLavaStill || ts == TextureUtils.iconLavaFlow) {
            return Config.isAnimatedLava();
        } else if (ts == TextureUtils.iconFireLayer0 || ts == TextureUtils.iconFireLayer1) {
            return Config.isAnimatedFire();
        } else if (ts == TextureUtils.iconPortal) {
            return Config.isAnimatedPortal();
        } else {
            return ts != TextureUtils.iconClock && ts != TextureUtils.iconCompass ? Config.isAnimatedTerrain() : true;
        }
    }

    public int getCountRegisteredSprites() {
        return this.counterIndexInMap.getValue();
    }

    private int detectMaxMipmapLevel(Map mapSprites, IResourceManager rm) {
        int minSize = this.detectMinimumSpriteSize(mapSprites, rm, 20);
        if (minSize < 16) {
            minSize = 16;
        }

        minSize = MathHelper.roundUpToPowerOfTwo(minSize);
        if (minSize > 16) {
            Config.log("Sprite size: " + minSize);
        }

        int minLevel = MathHelper.calculateLogBaseTwo(minSize);
        if (minLevel < 4) {
            minLevel = 4;
        }

        return minLevel;
    }

    private int detectMinimumSpriteSize(Map mapSprites, IResourceManager rm, int percentScale) {
        Map mapSizeCounts = new HashMap();

        for (Entry entry : (Iterable<Entry>) (Iterable) mapSprites.entrySet()) {
            TextureAtlasSprite sprite = (TextureAtlasSprite)entry.getValue();
            ResourceLocation loc = new ResourceLocation(sprite.getIconName());
            ResourceLocation locComplete = this.completeResourceLocation(loc);
            if (!sprite.hasCustomLoader(rm, loc)) {
                try {
                    IResource res = rm.getResource(locComplete);
                    if (res != null) {
                        InputStream in = res.getInputStream();
                        if (in != null) {
                            Dimension dim = TextureUtils.getImageSize(in, "png");
                            in.close();
                            if (dim != null) {
                                int width = dim.width;
                                int width2 = MathHelper.roundUpToPowerOfTwo(width);
                                if (!mapSizeCounts.containsKey(width2)) {
                                    mapSizeCounts.put(width2, 1);
                                } else {
                                    int count = (Integer)mapSizeCounts.get(width2);
                                    mapSizeCounts.put(width2, count + 1);
                                }
                            }
                        }
                    }
                } catch (Exception var17) {
                }
            }
        }

        int countSprites = 0;
        Set setSizes = mapSizeCounts.keySet();
        Set setSizesSorted = new TreeSet(setSizes);

        for (int size : (Iterable<Integer>) (Iterable) setSizesSorted) {
            int count = (Integer)mapSizeCounts.get(size);
            countSprites += count;
        }

        int minSize = 16;
        int countScale = 0;
        int countScaleMax = countSprites * percentScale / 100;

        for (int size : (Iterable<Integer>) (Iterable) setSizesSorted) {
            int count = (Integer)mapSizeCounts.get(size);
            countScale += count;
            if (size > minSize) {
                minSize = size;
            }

            if (countScale > countScaleMax) {
                return minSize;
            }
        }

        return minSize;
    }

    private int getMinSpriteSize() {
        int minSize = 1 << this.mipmapLevels;
        if (minSize < 8) {
            minSize = 8;
        }

        return minSize;
    }

    private int[] getMissingImageData(int size) {
        BufferedImage bi = new BufferedImage(16, 16, 2);
        bi.setRGB(0, 0, 16, 16, TextureUtil.missingTextureData, 0, 16);
        BufferedImage bi2 = TextureUtils.scaleImage(bi, size);
        int[] data = new int[size * size];
        bi2.getRGB(0, 0, size, size, data, 0, size);
        return data;
    }

    public boolean isTextureBound() {
        int boundTexId = GlStateManager.getBoundTexture();
        int texId = this.getGlTextureId();
        return boundTexId == texId;
    }

    private void updateIconGrid(int sheetWidth, int sheetHeight) {
        this.iconGridCountX = -1;
        this.iconGridCountY = -1;
        this.iconGrid = null;
        if (this.iconGridSize > 0) {
            this.iconGridCountX = sheetWidth / this.iconGridSize;
            this.iconGridCountY = sheetHeight / this.iconGridSize;
            this.iconGrid = new TextureAtlasSprite[this.iconGridCountX * this.iconGridCountY];
            this.iconGridSizeU = 1.0 / this.iconGridCountX;
            this.iconGridSizeV = 1.0 / this.iconGridCountY;

            for (TextureAtlasSprite ts : this.mapUploadedSprites.values()) {
                double deltaU = 0.5 / sheetWidth;
                double deltaV = 0.5 / sheetHeight;
                double uMin = Math.min(ts.getMinU(), ts.getMaxU()) + deltaU;
                double vMin = Math.min(ts.getMinV(), ts.getMaxV()) + deltaV;
                double uMax = Math.max(ts.getMinU(), ts.getMaxU()) - deltaU;
                double vMax = Math.max(ts.getMinV(), ts.getMaxV()) - deltaV;
                int iuMin = (int)(uMin / this.iconGridSizeU);
                int ivMin = (int)(vMin / this.iconGridSizeV);
                int iuMax = (int)(uMax / this.iconGridSizeU);
                int ivMax = (int)(vMax / this.iconGridSizeV);

                for (int iu = iuMin; iu <= iuMax; iu++) {
                    if (iu >= 0 && iu < this.iconGridCountX) {
                        for (int iv = ivMin; iv <= ivMax; iv++) {
                            if (iv >= 0 && iv < this.iconGridCountX) {
                                int index = iv * this.iconGridCountX + iu;
                                this.iconGrid[index] = ts;
                            } else {
                                Config.warn("Invalid grid V: " + iv + ", icon: " + ts.getIconName());
                            }
                        }
                    } else {
                        Config.warn("Invalid grid U: " + iu + ", icon: " + ts.getIconName());
                    }
                }
            }
        }
    }

    public TextureAtlasSprite getIconByUV(double u, double v) {
        if (this.iconGrid == null) {
            return null;
        } else {
            int iu = (int)(u / this.iconGridSizeU);
            int iv = (int)(v / this.iconGridSizeV);
            int index = iv * this.iconGridCountX + iu;
            return index >= 0 && index <= this.iconGrid.length ? this.iconGrid[index] : null;
        }
    }

    private void checkEmissive(ResourceLocation locSprite, TextureAtlasSprite sprite) {
        String suffixEm = EmissiveTextures.getSuffixEmissive();
        if (suffixEm != null) {
            if (!locSprite.getResourcePath().endsWith(suffixEm)) {
                ResourceLocation locSpriteEm = new ResourceLocation(locSprite.getResourceDomain(), locSprite.getResourcePath() + suffixEm);
                ResourceLocation locPngEm = this.completeResourceLocation(locSpriteEm);
                if (Config.hasResource(locPngEm)) {
                    TextureAtlasSprite spriteEmissive = this.registerSprite(locSpriteEm);
                    spriteEmissive.isEmissive = true;
                    sprite.spriteEmissive = spriteEmissive;
                }
            }
        }
    }

    public int getCountAnimations() {
        return this.listAnimatedSprites.size();
    }

    public int getCountAnimationsActive() {
        return this.countAnimationsActive;
    }
}
