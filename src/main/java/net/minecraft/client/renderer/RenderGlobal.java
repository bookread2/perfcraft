package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.Block.EnumOffsetType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.ListChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VboChunkFactory;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemRecord;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vector3d;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.optifine.DynamicLights;
import net.optifine.Lagometer;
import net.optifine.RandomEntities;
import net.optifine.SmartAnimations;
import net.optifine.model.BlockModelUtils;
import net.optifine.reflect.Reflector;
import net.optifine.render.ChunkVisibility;
import net.optifine.render.CloudRenderer;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.shaders.ShadowUtils;
import net.optifine.shaders.gui.GuiShaderOptions;
import net.optifine.util.ChunkUtils;
import net.optifine.util.RenderChunkUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class RenderGlobal implements IWorldAccess, IResourceManagerReloadListener {
    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation locationMoonPhasesPng = new ResourceLocation("textures/environment/moon_phases.png");
    private static final ResourceLocation locationSunPng = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation locationCloudsPng = new ResourceLocation("textures/environment/clouds.png");
    private static final ResourceLocation locationEndSkyPng = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation locationForcefieldPng = new ResourceLocation("textures/misc/forcefield.png");
    public final Minecraft mc;
    private final TextureManager renderEngine;
    private final RenderManager renderManager;
    private WorldClient theWorld;
    private Set<RenderChunk> chunksToUpdate = Sets.newLinkedHashSet();
    private List<RenderGlobal.ContainerLocalRenderInformation> renderInfos = Lists.newArrayListWithCapacity(69696);
    private final Set<TileEntity> setTileEntities = Sets.newHashSet();
    private ViewFrustum viewFrustum;
    private int starGLCallList = -1;
    private int glSkyList = -1;
    private int glSkyList2 = -1;
    private VertexFormat vertexBufferFormat;
    private VertexBuffer starVBO;
    private VertexBuffer skyVBO;
    private VertexBuffer sky2VBO;
    private int cloudTickCounter;
    public final Map<Integer, DestroyBlockProgress> damagedBlocks = Maps.newHashMap();
    private final Map<BlockPos, ISound> mapSoundPositions = Maps.newHashMap();
    private final TextureAtlasSprite[] destroyBlockIcons = new TextureAtlasSprite[10];
    private Framebuffer entityOutlineFramebuffer;
    private ShaderGroup entityOutlineShader;
    private double frustumUpdatePosX = Double.MIN_VALUE;
    private double frustumUpdatePosY = Double.MIN_VALUE;
    private double frustumUpdatePosZ = Double.MIN_VALUE;
    private int frustumUpdatePosChunkX = Integer.MIN_VALUE;
    private int frustumUpdatePosChunkY = Integer.MIN_VALUE;
    private int frustumUpdatePosChunkZ = Integer.MIN_VALUE;
    private double lastViewEntityX = Double.MIN_VALUE;
    private double lastViewEntityY = Double.MIN_VALUE;
    private double lastViewEntityZ = Double.MIN_VALUE;
    private double lastViewEntityPitch = Double.MIN_VALUE;
    private double lastViewEntityYaw = Double.MIN_VALUE;
    private final ChunkRenderDispatcher renderDispatcher = new ChunkRenderDispatcher();
    private ChunkRenderContainer renderContainer;
    private int renderDistanceChunks = -1;
    private int renderEntitiesStartupCounter = 2;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;
    private boolean debugFixTerrainFrustum = false;
    private ClippingHelper debugFixedClippingHelper;
    private final Vector4f[] debugTerrainMatrix = new Vector4f[8];
    private final Vector3d debugTerrainFrustumPosition = new Vector3d();
    private boolean vboEnabled = false;
    IRenderChunkFactory renderChunkFactory;
    private double prevRenderSortX;
    private double prevRenderSortY;
    private double prevRenderSortZ;
    public boolean displayListEntitiesDirty = true;
    private CloudRenderer cloudRenderer;
    public Entity renderedEntity;
    public Set chunksToResortTransparency = new LinkedHashSet();
    public Set chunksToUpdateForced = new LinkedHashSet();
    private Deque visibilityDeque = new ArrayDeque();
    private List renderInfosEntities = new ArrayList(1024);
    private List renderInfosTileEntities = new ArrayList(1024);
    private List renderInfosNormal = new ArrayList(1024);
    private List renderInfosEntitiesNormal = new ArrayList(1024);
    private List renderInfosTileEntitiesNormal = new ArrayList(1024);
    private List renderInfosShadow = new ArrayList(1024);
    private List renderInfosEntitiesShadow = new ArrayList(1024);
    private List renderInfosTileEntitiesShadow = new ArrayList(1024);
    private int renderDistance = 0;
    private int renderDistanceSq = 0;
    private static final Set SET_ALL_FACINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EnumFacing.VALUES)));
    private int countTileEntitiesRendered;
    private IChunkProvider worldChunkProvider = null;
    private LongHashMap worldChunkProviderMap = null;
    private int countLoadedChunksPrev = 0;
    private RenderEnv renderEnv = new RenderEnv(Blocks.air.getDefaultState(), new BlockPos(0, 0, 0));
    public boolean renderOverlayDamaged = false;
    public boolean renderOverlayEyes = false;
    private boolean firstWorldLoad = false;
    private static int renderEntitiesCounter = 0;

    public RenderGlobal(Minecraft mcIn) {
        this.cloudRenderer = new CloudRenderer(mcIn);
        this.mc = mcIn;
        this.renderManager = mcIn.getRenderManager();
        this.renderEngine = mcIn.getTextureManager();
        this.renderEngine.bindTexture(locationForcefieldPng);
        GL11.glTexParameteri(3553, 10242, 10497);
        GL11.glTexParameteri(3553, 10243, 10497);
        GlStateManager.bindTexture(0);
        this.updateDestroyBlockIcons();
        this.vboEnabled = OpenGlHelper.useVbo();
        if (this.vboEnabled) {
            this.renderContainer = new VboRenderList();
            this.renderChunkFactory = new VboChunkFactory();
        } else {
            this.renderContainer = new RenderList();
            this.renderChunkFactory = new ListChunkFactory();
        }

        this.vertexBufferFormat = new VertexFormat();
        this.vertexBufferFormat.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
        this.generateStars();
        this.generateSky();
        this.generateSky2();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.updateDestroyBlockIcons();
    }

    private void updateDestroyBlockIcons() {
        TextureMap texturemap = this.mc.getTextureMapBlocks();

        for (int i = 0; i < this.destroyBlockIcons.length; i++) {
            this.destroyBlockIcons[i] = texturemap.getAtlasSprite("minecraft:blocks/destroy_stage_" + i);
        }
    }

    public void makeEntityOutlineShader() {
        if (OpenGlHelper.shadersSupported) {
            if (ShaderLinkHelper.getStaticShaderLinkHelper() == null) {
                ShaderLinkHelper.setNewStaticShaderLinkHelper();
            }

            ResourceLocation resourcelocation = new ResourceLocation("shaders/post/entity_outline.json");

            try {
                this.entityOutlineShader = new ShaderGroup(
                    this.mc.getTextureManager(), this.mc.getResourceManager(), this.mc.getFramebuffer(), resourcelocation
                );
                this.entityOutlineShader.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
                this.entityOutlineFramebuffer = this.entityOutlineShader.getFramebufferRaw("final");
            } catch (IOException var3) {
                logger.warn("Failed to load shader: " + resourcelocation, var3);
                this.entityOutlineShader = null;
                this.entityOutlineFramebuffer = null;
            } catch (JsonSyntaxException var4) {
                logger.warn("Failed to load shader: " + resourcelocation, var4);
                this.entityOutlineShader = null;
                this.entityOutlineFramebuffer = null;
            }
        } else {
            this.entityOutlineShader = null;
            this.entityOutlineFramebuffer = null;
        }
    }

    public void renderEntityOutlineFramebuffer() {
        if (this.isRenderEntityOutlines()) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
            this.entityOutlineFramebuffer.framebufferRenderExt(this.mc.displayWidth, this.mc.displayHeight, false);
            GlStateManager.disableBlend();
        }
    }

    protected boolean isRenderEntityOutlines() {
        return !Config.isFastRender() && !Config.isShaders() && !Config.isAntialiasing()
            ? this.entityOutlineFramebuffer != null
                && this.entityOutlineShader != null
                && this.mc.thePlayer != null
                && this.mc.thePlayer.isSpectator()
                && this.mc.gameSettings.keyBindSpectatorOutlines.isKeyDown()
            : false;
    }

    private void generateSky2() {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        if (this.sky2VBO != null) {
            this.sky2VBO.deleteGlBuffers();
        }

        if (this.glSkyList2 >= 0) {
            GLAllocation.deleteDisplayLists(this.glSkyList2);
            this.glSkyList2 = -1;
        }

        if (this.vboEnabled) {
            this.sky2VBO = new VertexBuffer(this.vertexBufferFormat);
            this.renderSky(vertexbuffer, -16.0F, true);
            vertexbuffer.finishDrawing();
            vertexbuffer.reset();
            this.sky2VBO.bufferData(vertexbuffer.getByteBuffer());
        } else {
            this.glSkyList2 = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(this.glSkyList2, 4864);
            this.renderSky(vertexbuffer, -16.0F, true);
            tessellator.draw();
            GL11.glEndList();
        }
    }

    private void generateSky() {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        if (this.skyVBO != null) {
            this.skyVBO.deleteGlBuffers();
        }

        if (this.glSkyList >= 0) {
            GLAllocation.deleteDisplayLists(this.glSkyList);
            this.glSkyList = -1;
        }

        if (this.vboEnabled) {
            this.skyVBO = new VertexBuffer(this.vertexBufferFormat);
            this.renderSky(vertexbuffer, 16.0F, false);
            vertexbuffer.finishDrawing();
            vertexbuffer.reset();
            this.skyVBO.bufferData(vertexbuffer.getByteBuffer());
        } else {
            this.glSkyList = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(this.glSkyList, 4864);
            this.renderSky(vertexbuffer, 16.0F, false);
            tessellator.draw();
            GL11.glEndList();
        }
    }

    private void renderSky(WorldRenderer worldRendererIn, float posY, boolean reverseX) {
        int i = 64;
        int j = 6;
        worldRendererIn.begin(7, DefaultVertexFormats.POSITION);
        int skyDist = (this.renderDistance / 64 + 1) * 64 + 64;

        for (int k = -skyDist; k <= skyDist; k += 64) {
            for (int l = -skyDist; l <= skyDist; l += 64) {
                float f = k;
                float f1 = k + 64;
                if (reverseX) {
                    f1 = k;
                    f = k + 64;
                }

                worldRendererIn.pos(f, posY, l).endVertex();
                worldRendererIn.pos(f1, posY, l).endVertex();
                worldRendererIn.pos(f1, posY, l + 64).endVertex();
                worldRendererIn.pos(f, posY, l + 64).endVertex();
            }
        }
    }

    private void generateStars() {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        if (this.starVBO != null) {
            this.starVBO.deleteGlBuffers();
        }

        if (this.starGLCallList >= 0) {
            GLAllocation.deleteDisplayLists(this.starGLCallList);
            this.starGLCallList = -1;
        }

        if (this.vboEnabled) {
            this.starVBO = new VertexBuffer(this.vertexBufferFormat);
            this.renderStars(vertexbuffer);
            vertexbuffer.finishDrawing();
            vertexbuffer.reset();
            this.starVBO.bufferData(vertexbuffer.getByteBuffer());
        } else {
            this.starGLCallList = GLAllocation.generateDisplayLists(1);
            GlStateManager.pushMatrix();
            GL11.glNewList(this.starGLCallList, 4864);
            this.renderStars(vertexbuffer);
            tessellator.draw();
            GL11.glEndList();
            GlStateManager.popMatrix();
        }
    }

    private void renderStars(WorldRenderer worldRendererIn) {
        Random random = new Random(10842L);
        worldRendererIn.begin(7, DefaultVertexFormats.POSITION);

        for (int i = 0; i < 1500; i++) {
            double d0 = random.nextFloat() * 2.0F - 1.0F;
            double d1 = random.nextFloat() * 2.0F - 1.0F;
            double d2 = random.nextFloat() * 2.0F - 1.0F;
            double d3 = 0.15F + random.nextFloat() * 0.1F;
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;
            if (d4 < 1.0 && d4 > 0.01) {
                d4 = 1.0 / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0;
                double d6 = d1 * 100.0;
                double d7 = d2 * 100.0;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = random.nextDouble() * Math.PI * 2.0;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                for (int j = 0; j < 4; j++) {
                    double d17 = 0.0;
                    double d18 = ((j & 2) - 1) * d3;
                    double d19 = ((j + 1 & 2) - 1) * d3;
                    double d20 = 0.0;
                    double d21 = d18 * d16 - d19 * d15;
                    double d22 = d19 * d16 + d18 * d15;
                    double d23 = d21 * d12 + 0.0 * d13;
                    double d24 = 0.0 * d12 - d21 * d13;
                    double d25 = d24 * d9 - d22 * d10;
                    double d26 = d22 * d9 + d24 * d10;
                    worldRendererIn.pos(d5 + d25, d6 + d23, d7 + d26).endVertex();
                }
            }
        }
    }

    public void setWorldAndLoadRenderers(WorldClient worldClientIn) {
        if (this.theWorld != null) {
            this.theWorld.removeWorldAccess(this);
        }

        this.frustumUpdatePosX = Double.MIN_VALUE;
        this.frustumUpdatePosY = Double.MIN_VALUE;
        this.frustumUpdatePosZ = Double.MIN_VALUE;
        this.frustumUpdatePosChunkX = Integer.MIN_VALUE;
        this.frustumUpdatePosChunkY = Integer.MIN_VALUE;
        this.frustumUpdatePosChunkZ = Integer.MIN_VALUE;
        this.renderManager.set(worldClientIn);
        this.theWorld = worldClientIn;
        if (Config.isDynamicLights()) {
            DynamicLights.clear();
        }

        ChunkVisibility.reset();
        this.worldChunkProvider = null;
        this.worldChunkProviderMap = null;
        this.renderEnv.reset(null, null);
        Shaders.checkWorldChanged(this.theWorld);
        if (worldClientIn != null) {
            worldClientIn.addWorldAccess(this);
            this.loadRenderers();
        } else {
            this.chunksToUpdate.clear();
            this.clearRenderInfos();
            if (this.viewFrustum != null) {
                this.viewFrustum.deleteGlResources();
            }

            this.viewFrustum = null;
        }
    }

    public void loadRenderers() {
        if (this.theWorld != null) {
            this.displayListEntitiesDirty = true;
            Blocks.leaves.setGraphicsLevel(Config.isTreesFancy());
            Blocks.leaves2.setGraphicsLevel(Config.isTreesFancy());
            BlockModelRenderer.updateAoLightValue();
            if (Config.isDynamicLights()) {
                DynamicLights.clear();
            }

            SmartAnimations.update();
            this.renderDistanceChunks = this.mc.gameSettings.renderDistanceChunks;
            this.renderDistance = this.renderDistanceChunks * 16;
            this.renderDistanceSq = this.renderDistance * this.renderDistance;
            boolean flag = this.vboEnabled;
            this.vboEnabled = OpenGlHelper.useVbo();
            if (flag && !this.vboEnabled) {
                this.renderContainer = new RenderList();
                this.renderChunkFactory = new ListChunkFactory();
            } else if (!flag && this.vboEnabled) {
                this.renderContainer = new VboRenderList();
                this.renderChunkFactory = new VboChunkFactory();
            }

            this.generateStars();
            this.generateSky();
            this.generateSky2();
            if (this.viewFrustum != null) {
                this.viewFrustum.deleteGlResources();
            }

            this.stopChunkUpdates();
            synchronized (this.setTileEntities) {
                this.setTileEntities.clear();
            }

            this.viewFrustum = new ViewFrustum(this.theWorld, this.mc.gameSettings.renderDistanceChunks, this, this.renderChunkFactory);
            if (this.theWorld != null) {
                Entity entity = this.mc.getRenderViewEntity();
                if (entity != null) {
                    this.viewFrustum.updateChunkPositions(entity.posX, entity.posZ);
                }
            }

            this.renderEntitiesStartupCounter = 2;
        }

        if (this.mc.thePlayer == null) {
            this.firstWorldLoad = true;
        }
    }

    protected void stopChunkUpdates() {
        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates();
    }

    public void createBindEntityOutlineFbs(int width, int height) {
        if (OpenGlHelper.shadersSupported && this.entityOutlineShader != null) {
            this.entityOutlineShader.createBindFramebuffers(width, height);
        }
    }

    public void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks) {
        int pass = 0;
        if (Reflector.MinecraftForgeClient_getRenderPass.exists()) {
            pass = Reflector.callInt(Reflector.MinecraftForgeClient_getRenderPass);
        }

        if (this.renderEntitiesStartupCounter > 0) {
            if (pass > 0) {
                return;
            }

            this.renderEntitiesStartupCounter--;
        } else {
            double d0 = renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * partialTicks;
            double d1 = renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * partialTicks;
            double d2 = renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * partialTicks;
            this.theWorld.theProfiler.startSection("prepare");
            TileEntityRendererDispatcher.instance
                .cacheActiveRenderInfo(this.theWorld, this.mc.getTextureManager(), this.mc.fontRendererObj, this.mc.getRenderViewEntity(), partialTicks);
            this.renderManager
                .cacheActiveRenderInfo(
                    this.theWorld, this.mc.fontRendererObj, this.mc.getRenderViewEntity(), this.mc.pointedEntity, this.mc.gameSettings, partialTicks
                );
            renderEntitiesCounter++;
            if (pass == 0) {
                this.countEntitiesTotal = 0;
                this.countEntitiesRendered = 0;
                this.countEntitiesHidden = 0;
                this.countTileEntitiesRendered = 0;
            }

            Entity entity = this.mc.getRenderViewEntity();
            double d3 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double d4 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double d5 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
            TileEntityRendererDispatcher.staticPlayerX = d3;
            TileEntityRendererDispatcher.staticPlayerY = d4;
            TileEntityRendererDispatcher.staticPlayerZ = d5;
            this.renderManager.setRenderPosition(d3, d4, d5);
            this.mc.entityRenderer.enableLightmap();
            this.theWorld.theProfiler.endStartSection("global");
            List<Entity> list = this.theWorld.getLoadedEntityList();
            if (pass == 0) {
                this.countEntitiesTotal = list.size();
            }

            if (Config.isFogOff() && this.mc.entityRenderer.fogStandard) {
                GlStateManager.disableFog();
            }

            boolean forgeEntityPass = Reflector.ForgeEntity_shouldRenderInPass.exists();
            boolean forgeTileEntityPass = Reflector.ForgeTileEntity_shouldRenderInPass.exists();

            for (int i = 0; i < this.theWorld.weatherEffects.size(); i++) {
                Entity entity1 = this.theWorld.weatherEffects.get(i);
                if (!forgeEntityPass || Reflector.callBoolean(entity1, Reflector.ForgeEntity_shouldRenderInPass, pass)) {
                    this.countEntitiesRendered++;
                    if (entity1.isInRangeToRender3d(d0, d1, d2)) {
                        this.renderManager.renderEntitySimple(entity1, partialTicks);
                    }
                }
            }

            if (this.isRenderEntityOutlines()) {
                GlStateManager.depthFunc(519);
                GlStateManager.disableFog();
                this.entityOutlineFramebuffer.framebufferClear();
                this.entityOutlineFramebuffer.bindFramebuffer(false);
                this.theWorld.theProfiler.endStartSection("entityOutlines");
                RenderHelper.disableStandardItemLighting();
                this.renderManager.setRenderOutlines(true);

                for (int j = 0; j < list.size(); j++) {
                    Entity entity3 = list.get(j);
                    boolean flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase
                        && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();
                    boolean flag1 = entity3.isInRangeToRender3d(d0, d1, d2)
                        && (
                            entity3.ignoreFrustumCheck
                                || camera.isBoundingBoxInFrustum(entity3.getEntityBoundingBox())
                                || entity3.riddenByEntity == this.mc.thePlayer
                        )
                        && entity3 instanceof EntityPlayer;
                    if ((entity3 != this.mc.getRenderViewEntity() || this.mc.gameSettings.thirdPersonView != 0 || flag) && flag1) {
                        this.renderManager.renderEntitySimple(entity3, partialTicks);
                    }
                }

                this.renderManager.setRenderOutlines(false);
                RenderHelper.enableStandardItemLighting();
                GlStateManager.depthMask(false);
                this.entityOutlineShader.loadShaderGroup(partialTicks);
                GlStateManager.enableLighting();
                GlStateManager.depthMask(true);
                this.mc.getFramebuffer().bindFramebuffer(false);
                GlStateManager.enableFog();
                GlStateManager.enableBlend();
                GlStateManager.enableColorMaterial();
                GlStateManager.depthFunc(515);
                GlStateManager.enableDepth();
                GlStateManager.enableAlpha();
            }

            this.theWorld.theProfiler.endStartSection("entities");
            boolean isShaders = Config.isShaders();
            if (isShaders) {
                Shaders.beginEntities();
            }

            RenderItemFrame.updateItemRenderDistance();
            boolean oldFancyGraphics = this.mc.gameSettings.fancyGraphics;
            this.mc.gameSettings.fancyGraphics = Config.isDroppedItemsFancy();
            boolean playerShadowPass = Shaders.isShadowPass && !this.mc.thePlayer.isSpectator();

            for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation : (Iterable<RenderGlobal.ContainerLocalRenderInformation>) (Iterable) this.renderInfosEntities) {
                Chunk chunk = renderglobal$containerlocalrenderinformation.renderChunk.getChunk();
                ClassInheritanceMultiMap<Entity> classinheritancemultimap = chunk.getEntityLists()[renderglobal$containerlocalrenderinformation.renderChunk
                        .getPosition()
                        .getY()
                    / 16];
                if (!classinheritancemultimap.isEmpty()) {
                    for (Entity entity2 : classinheritancemultimap) {
                        if (!forgeEntityPass || Reflector.callBoolean(entity2, Reflector.ForgeEntity_shouldRenderInPass, pass)) {
                            boolean flag2 = this.renderManager.shouldRender(entity2, camera, d0, d1, d2) || entity2.riddenByEntity == this.mc.thePlayer;
                            if (flag2) {
                                boolean flag3 = this.mc.getRenderViewEntity() instanceof EntityLivingBase
                                    ? ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping()
                                    : false;
                                if (entity2 == this.mc.getRenderViewEntity() && !playerShadowPass && this.mc.gameSettings.thirdPersonView == 0 && !flag3
                                    || !(entity2.posY < 0.0) && !(entity2.posY >= 256.0) && !this.theWorld.isBlockLoaded(new BlockPos(entity2))) {
                                    continue;
                                }

                                this.countEntitiesRendered++;
                                this.renderedEntity = entity2;
                                if (isShaders) {
                                    Shaders.nextEntity(entity2);
                                }

                                this.renderManager.renderEntitySimple(entity2, partialTicks);
                                this.renderedEntity = null;
                            }

                            if (!flag2
                                && entity2 instanceof EntityWitherSkull
                                && (!forgeEntityPass || Reflector.callBoolean(entity2, Reflector.ForgeEntity_shouldRenderInPass, pass))) {
                                this.renderedEntity = entity2;
                                if (isShaders) {
                                    Shaders.nextEntity(entity2);
                                }

                                this.mc.getRenderManager().renderWitherSkull(entity2, partialTicks);
                                this.renderedEntity = null;
                            }
                        }
                    }
                }
            }

            this.mc.gameSettings.fancyGraphics = oldFancyGraphics;
            if (isShaders) {
                Shaders.endEntities();
                Shaders.beginBlockEntities();
            }

            this.theWorld.theProfiler.endStartSection("blockentities");
            RenderHelper.enableStandardItemLighting();
            if (Reflector.ForgeTileEntity_hasFastRenderer.exists()) {
                TileEntityRendererDispatcher.instance.preDrawBatch();
            }

            TileEntitySignRenderer.updateTextRenderDistance();

            for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation1 : (Iterable<RenderGlobal.ContainerLocalRenderInformation>) (Iterable) this.renderInfosTileEntities) {
                List<TileEntity> list1 = renderglobal$containerlocalrenderinformation1.renderChunk.getCompiledChunk().getTileEntities();
                if (!list1.isEmpty()) {
                    for (TileEntity tileentity2 : list1) {
                        if (forgeTileEntityPass) {
                            if (!Reflector.callBoolean(tileentity2, Reflector.ForgeTileEntity_shouldRenderInPass, pass)) {
                                continue;
                            }

                            AxisAlignedBB aabb = (AxisAlignedBB)Reflector.call(tileentity2, Reflector.ForgeTileEntity_getRenderBoundingBox);
                            if (aabb != null && !camera.isBoundingBoxInFrustum(aabb)) {
                                continue;
                            }
                        }

                        if (isShaders) {
                            Shaders.nextBlockEntity(tileentity2);
                        }

                        TileEntityRendererDispatcher.instance.renderTileEntity(tileentity2, partialTicks, -1);
                        this.countTileEntitiesRendered++;
                    }
                }
            }

            synchronized (this.setTileEntities) {
                for (TileEntity tileentity : this.setTileEntities) {
                    if (!forgeTileEntityPass || Reflector.callBoolean(tileentity, Reflector.ForgeTileEntity_shouldRenderInPass, pass)) {
                        if (isShaders) {
                            Shaders.nextBlockEntity(tileentity);
                        }

                        TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, -1);
                    }
                }
            }

            if (Reflector.ForgeTileEntity_hasFastRenderer.exists()) {
                TileEntityRendererDispatcher.instance.drawBatch(pass);
            }

            this.renderOverlayDamaged = true;
            this.preRenderDamagedBlocks();

            for (DestroyBlockProgress destroyblockprogress : this.damagedBlocks.values()) {
                BlockPos blockpos = destroyblockprogress.getPosition();
                TileEntity tileentity1 = this.theWorld.getTileEntity(blockpos);
                if (tileentity1 instanceof TileEntityChest) {
                    TileEntityChest tileentitychest = (TileEntityChest)tileentity1;
                    if (tileentitychest.adjacentChestXNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.WEST);
                        tileentity1 = this.theWorld.getTileEntity(blockpos);
                    } else if (tileentitychest.adjacentChestZNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.NORTH);
                        tileentity1 = this.theWorld.getTileEntity(blockpos);
                    }
                }

                Block block = this.theWorld.getBlockState(blockpos).getBlock();
                boolean shouldRender;
                if (forgeTileEntityPass) {
                    shouldRender = false;
                    if (tileentity1 != null
                        && Reflector.callBoolean(tileentity1, Reflector.ForgeTileEntity_shouldRenderInPass, pass)
                        && Reflector.callBoolean(tileentity1, Reflector.ForgeTileEntity_canRenderBreaking)) {
                        AxisAlignedBB aabb = (AxisAlignedBB)Reflector.call(tileentity1, Reflector.ForgeTileEntity_getRenderBoundingBox);
                        if (aabb != null) {
                            shouldRender = camera.isBoundingBoxInFrustum(aabb);
                        }
                    }
                } else {
                    shouldRender = tileentity1 != null
                        && (block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockSign || block instanceof BlockSkull);
                }

                if (shouldRender) {
                    if (isShaders) {
                        Shaders.nextBlockEntity(tileentity1);
                    }

                    TileEntityRendererDispatcher.instance.renderTileEntity(tileentity1, partialTicks, destroyblockprogress.getPartialBlockDamage());
                }
            }

            this.postRenderDamagedBlocks();
            this.renderOverlayDamaged = false;
            if (isShaders) {
                Shaders.endBlockEntities();
            }

            renderEntitiesCounter--;
            this.mc.entityRenderer.disableLightmap();
            this.mc.mcProfiler.endSection();
        }
    }

    public String getDebugInfoRenders() {
        int i = this.viewFrustum.renderChunks.length;
        int j = 0;

        for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation : this.renderInfos) {
            CompiledChunk compiledchunk = renderglobal$containerlocalrenderinformation.renderChunk.compiledChunk;
            if (compiledchunk != CompiledChunk.DUMMY && !compiledchunk.isEmpty()) {
                j++;
            }
        }

        return String.format(
            "C: %d/%d %sD: %d, %s", j, i, this.mc.renderChunksMany ? "(s) " : "", this.renderDistanceChunks, this.renderDispatcher.getDebugInfo()
        );
    }

    public String getDebugInfoEntities() {
        return "E: "
            + this.countEntitiesRendered
            + "/"
            + this.countEntitiesTotal
            + ", B: "
            + this.countEntitiesHidden
            + ", I: "
            + (this.countEntitiesTotal - this.countEntitiesHidden - this.countEntitiesRendered)
            + ", "
            + Config.getVersionDebug();
    }

    public void setupTerrain(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator) {
        if (this.mc.gameSettings.renderDistanceChunks != this.renderDistanceChunks) {
            this.loadRenderers();
        }

        this.theWorld.theProfiler.startSection("camera");
        double d0 = viewEntity.posX - this.frustumUpdatePosX;
        double d1 = viewEntity.posY - this.frustumUpdatePosY;
        double d2 = viewEntity.posZ - this.frustumUpdatePosZ;
        if (this.frustumUpdatePosChunkX != viewEntity.chunkCoordX
            || this.frustumUpdatePosChunkY != viewEntity.chunkCoordY
            || this.frustumUpdatePosChunkZ != viewEntity.chunkCoordZ
            || d0 * d0 + d1 * d1 + d2 * d2 > 16.0) {
            this.frustumUpdatePosX = viewEntity.posX;
            this.frustumUpdatePosY = viewEntity.posY;
            this.frustumUpdatePosZ = viewEntity.posZ;
            this.frustumUpdatePosChunkX = viewEntity.chunkCoordX;
            this.frustumUpdatePosChunkY = viewEntity.chunkCoordY;
            this.frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;
            this.viewFrustum.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
        }

        if (Config.isDynamicLights()) {
            DynamicLights.update(this);
        }

        this.theWorld.theProfiler.endStartSection("renderlistcamera");
        double d3 = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double d4 = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double d5 = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
        this.renderContainer.initialize(d3, d4, d5);
        this.theWorld.theProfiler.endStartSection("cull");
        if (this.debugFixedClippingHelper != null) {
            Frustum frustum = new Frustum(this.debugFixedClippingHelper);
            frustum.setPosition(this.debugTerrainFrustumPosition.x, this.debugTerrainFrustumPosition.y, this.debugTerrainFrustumPosition.z);
            camera = frustum;
        }

        this.mc.mcProfiler.endStartSection("culling");
        BlockPos blockpos1 = new BlockPos(d3, d4 + viewEntity.getEyeHeight(), d5);
        RenderChunk renderchunk = this.viewFrustum.getRenderChunk(blockpos1);
        new BlockPos(MathHelper.floor_double(d3 / 16.0) * 16, MathHelper.floor_double(d4 / 16.0) * 16, MathHelper.floor_double(d5 / 16.0) * 16);
        this.displayListEntitiesDirty = this.displayListEntitiesDirty
            || !this.chunksToUpdate.isEmpty()
            || viewEntity.posX != this.lastViewEntityX
            || viewEntity.posY != this.lastViewEntityY
            || viewEntity.posZ != this.lastViewEntityZ
            || viewEntity.rotationPitch != this.lastViewEntityPitch
            || viewEntity.rotationYaw != this.lastViewEntityYaw;
        this.lastViewEntityX = viewEntity.posX;
        this.lastViewEntityY = viewEntity.posY;
        this.lastViewEntityZ = viewEntity.posZ;
        this.lastViewEntityPitch = viewEntity.rotationPitch;
        this.lastViewEntityYaw = viewEntity.rotationYaw;
        boolean flag = this.debugFixedClippingHelper != null;
        this.mc.mcProfiler.endStartSection("update");
        Lagometer.timerVisibility.start();
        int countLoadedChunks = this.getCountLoadedChunks();
        if (countLoadedChunks != this.countLoadedChunksPrev) {
            this.countLoadedChunksPrev = countLoadedChunks;
            this.displayListEntitiesDirty = true;
        }

        int maxChunkY = 256;
        if (!ChunkVisibility.isFinished()) {
            this.displayListEntitiesDirty = true;
        }

        if (!flag && this.displayListEntitiesDirty && Config.isIntegratedServerRunning()) {
            maxChunkY = ChunkVisibility.getMaxChunkY(this.theWorld, viewEntity, this.renderDistanceChunks);
        }

        RenderChunk renderChunkPlayer = this.viewFrustum.getRenderChunk(new BlockPos(viewEntity.posX, viewEntity.posY, viewEntity.posZ));
        if (Shaders.isShadowPass) {
            this.renderInfos = this.renderInfosShadow;
            this.renderInfosEntities = this.renderInfosEntitiesShadow;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesShadow;
            if (!flag && this.displayListEntitiesDirty) {
                this.clearRenderInfos();
                if (renderChunkPlayer != null && renderChunkPlayer.getPosition().getY() > maxChunkY) {
                    this.renderInfosEntities.add(renderChunkPlayer.getRenderInfo());
                }

                Iterator<RenderChunk> it = ShadowUtils.makeShadowChunkIterator(
                    this.theWorld, partialTicks, viewEntity, this.renderDistanceChunks, this.viewFrustum
                );

                while (it.hasNext()) {
                    RenderChunk chunk = it.next();
                    if (chunk != null && chunk.getPosition().getY() <= maxChunkY) {
                        RenderGlobal.ContainerLocalRenderInformation renderInfo = chunk.getRenderInfo();
                        if (!chunk.compiledChunk.isEmpty() || chunk.isNeedsUpdate()) {
                            this.renderInfos.add(renderInfo);
                        }

                        if (ChunkUtils.hasEntities(chunk.getChunk())) {
                            this.renderInfosEntities.add(renderInfo);
                        }

                        if (chunk.getCompiledChunk().getTileEntities().size() > 0) {
                            this.renderInfosTileEntities.add(renderInfo);
                        }
                    }
                }
            }
        } else {
            this.renderInfos = this.renderInfosNormal;
            this.renderInfosEntities = this.renderInfosEntitiesNormal;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesNormal;
        }

        if (!flag && this.displayListEntitiesDirty && !Shaders.isShadowPass) {
            this.displayListEntitiesDirty = false;
            this.clearRenderInfos();
            this.visibilityDeque.clear();
            Deque queue = this.visibilityDeque;
            boolean flag1 = this.mc.renderChunksMany;
            if (renderchunk != null && renderchunk.getPosition().getY() <= maxChunkY) {
                boolean flag2 = false;
                RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation3 = new RenderGlobal.ContainerLocalRenderInformation(
                    renderchunk, (EnumFacing)null, 0
                );
                Set set1 = SET_ALL_FACINGS;
                if (set1.size() == 1) {
                    Vector3f vector3f = this.getViewVector(viewEntity, partialTicks);
                    EnumFacing enumfacing = EnumFacing.getFacingFromVector(vector3f.x, vector3f.y, vector3f.z).getOpposite();
                    set1.remove(enumfacing);
                }

                if (set1.isEmpty()) {
                    flag2 = true;
                }

                if (flag2 && !playerSpectator) {
                    this.renderInfos.add(renderglobal$containerlocalrenderinformation3);
                } else {
                    if (playerSpectator && this.theWorld.getBlockState(blockpos1).getBlock().isOpaqueCube()) {
                        flag1 = false;
                    }

                    renderchunk.setFrameIndex(frameCount);
                    queue.add(renderglobal$containerlocalrenderinformation3);
                }
            } else {
                int i = blockpos1.getY() > 0 ? Math.min(maxChunkY, 248) : 8;
                if (renderChunkPlayer != null) {
                    this.renderInfosEntities.add(renderChunkPlayer.getRenderInfo());
                }

                for (int j = -this.renderDistanceChunks; j <= this.renderDistanceChunks; j++) {
                    for (int k = -this.renderDistanceChunks; k <= this.renderDistanceChunks; k++) {
                        RenderChunk renderchunk1 = this.viewFrustum.getRenderChunk(new BlockPos((j << 4) + 8, i, (k << 4) + 8));
                        if (renderchunk1 != null && renderchunk1.isBoundingBoxInFrustum(camera, frameCount)) {
                            renderchunk1.setFrameIndex(frameCount);
                            RenderGlobal.ContainerLocalRenderInformation info = renderchunk1.getRenderInfo();
                            info.initialize(null, 0);
                            queue.add(info);
                        }
                    }
                }
            }

            this.mc.mcProfiler.startSection("iteration");
            boolean fog = Config.isFogOn();

            while (!queue.isEmpty()) {
                RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation1 = (RenderGlobal.ContainerLocalRenderInformation)queue.poll();
                RenderChunk renderchunk3 = renderglobal$containerlocalrenderinformation1.renderChunk;
                EnumFacing enumfacing2 = renderglobal$containerlocalrenderinformation1.facing;
                CompiledChunk compiledChunk3 = renderchunk3.compiledChunk;
                if (!compiledChunk3.isEmpty() || renderchunk3.isNeedsUpdate()) {
                    this.renderInfos.add(renderglobal$containerlocalrenderinformation1);
                }

                if (ChunkUtils.hasEntities(renderchunk3.getChunk())) {
                    this.renderInfosEntities.add(renderglobal$containerlocalrenderinformation1);
                }

                if (compiledChunk3.getTileEntities().size() > 0) {
                    this.renderInfosTileEntities.add(renderglobal$containerlocalrenderinformation1);
                }

                for (EnumFacing enumfacing1 : flag1
                    ? ChunkVisibility.getFacingsNotOpposite(renderglobal$containerlocalrenderinformation1.c)
                    : EnumFacing.VALUES) {
                    if (!flag1 || enumfacing2 == null || compiledChunk3.isVisible(enumfacing2.getOpposite(), enumfacing1)) {
                        RenderChunk renderchunk2 = this.getRenderChunkOffset(blockpos1, renderchunk3, enumfacing1, fog, maxChunkY);
                        if (renderchunk2 != null && renderchunk2.setFrameIndex(frameCount) && renderchunk2.isBoundingBoxInFrustum(camera, frameCount)) {
                            int setFacing = renderglobal$containerlocalrenderinformation1.c | 1 << enumfacing1.ordinal();
                            RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation = renderchunk2.getRenderInfo();
                            renderglobal$containerlocalrenderinformation.initialize(enumfacing1, setFacing);
                            queue.add(renderglobal$containerlocalrenderinformation);
                        }
                    }
                }
            }

            this.mc.mcProfiler.endSection();
        }

        this.mc.mcProfiler.endStartSection("captureFrustum");
        if (this.debugFixTerrainFrustum) {
            this.fixTerrainFrustum(d3, d4, d5);
            this.debugFixTerrainFrustum = false;
        }

        Lagometer.timerVisibility.end();
        if (Shaders.isShadowPass) {
            Shaders.mcProfilerEndSection();
        } else {
            this.mc.mcProfiler.endStartSection("rebuildNear");
            this.renderDispatcher.clearChunkUpdates();
            Set<RenderChunk> set = this.chunksToUpdate;
            this.chunksToUpdate = Sets.newLinkedHashSet();
            Lagometer.timerChunkUpdate.start();

            for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation2 : this.renderInfos) {
                RenderChunk renderchunk4 = renderglobal$containerlocalrenderinformation2.renderChunk;
                if (renderchunk4.isNeedsUpdate() || set.contains(renderchunk4)) {
                    this.displayListEntitiesDirty = true;
                    BlockPos posChunk = renderchunk4.getPosition();
                    boolean flag3 = blockpos1.distanceSq(posChunk.getX() + 8, posChunk.getY() + 8, posChunk.getZ() + 8) < 768.0;
                    if (!flag3) {
                        this.chunksToUpdate.add(renderchunk4);
                    } else if (!renderchunk4.isPlayerUpdate()) {
                        this.chunksToUpdateForced.add(renderchunk4);
                    } else {
                        this.mc.mcProfiler.startSection("build near");
                        this.renderDispatcher.updateChunkNow(renderchunk4);
                        renderchunk4.setNeedsUpdate(false);
                        this.mc.mcProfiler.endSection();
                    }
                }
            }

            Lagometer.timerChunkUpdate.end();
            this.chunksToUpdate.addAll(set);
            this.mc.mcProfiler.endSection();
        }
    }

    private boolean isPositionInRenderChunk(BlockPos pos, RenderChunk renderChunkIn) {
        BlockPos blockpos = renderChunkIn.getPosition();
        return MathHelper.abs_int(pos.getX() - blockpos.getX()) > 16
            ? false
            : (MathHelper.abs_int(pos.getY() - blockpos.getY()) > 16 ? false : MathHelper.abs_int(pos.getZ() - blockpos.getZ()) <= 16);
    }

    private Set<EnumFacing> getVisibleFacings(BlockPos pos) {
        VisGraph visgraph = new VisGraph();
        BlockPos blockpos = new BlockPos(pos.getX() >> 4 << 4, pos.getY() >> 4 << 4, pos.getZ() >> 4 << 4);
        Chunk chunk = this.theWorld.getChunkFromBlockCoords(blockpos);

        for (MutableBlockPos blockpos$mutableblockpos : BlockPos.getAllInBoxMutable(blockpos, blockpos.add(15, 15, 15))) {
            if (chunk.getBlock(blockpos$mutableblockpos).isOpaqueCube()) {
                visgraph.func_178606_a(blockpos$mutableblockpos);
            }
        }

        return visgraph.func_178609_b(pos);
    }

    private RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing, boolean fog, int yMax) {
        RenderChunk neighbour = renderChunkBase.getRenderChunkNeighbour(facing);
        if (neighbour == null) {
            return null;
        } else if (neighbour.getPosition().getY() > yMax) {
            return null;
        } else {
            if (fog) {
                BlockPos var4 = neighbour.getPosition();
                int dxs = playerPos.getX() - var4.getX();
                int dzs = playerPos.getZ() - var4.getZ();
                int distSq = dxs * dxs + dzs * dzs;
                if (distSq > this.renderDistanceSq) {
                    return null;
                }
            }

            return neighbour;
        }
    }

    private void fixTerrainFrustum(double x, double y, double z) {
        this.debugFixedClippingHelper = new ClippingHelperImpl();
        ((ClippingHelperImpl)this.debugFixedClippingHelper).init();
        Matrix4f matrix4f = new Matrix4f(this.debugFixedClippingHelper.modelviewMatrix);
        matrix4f.transpose();
        Matrix4f matrix4f1 = new Matrix4f(this.debugFixedClippingHelper.projectionMatrix);
        matrix4f1.transpose();
        Matrix4f matrix4f2 = new Matrix4f();
        Matrix4f.mul(matrix4f1, matrix4f, matrix4f2);
        matrix4f2.invert();
        this.debugTerrainFrustumPosition.x = x;
        this.debugTerrainFrustumPosition.y = y;
        this.debugTerrainFrustumPosition.z = z;
        this.debugTerrainMatrix[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
        this.debugTerrainMatrix[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
        this.debugTerrainMatrix[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
        this.debugTerrainMatrix[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
        this.debugTerrainMatrix[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
        this.debugTerrainMatrix[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
        this.debugTerrainMatrix[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.debugTerrainMatrix[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 8; i++) {
            Matrix4f.transform(matrix4f2, this.debugTerrainMatrix[i], this.debugTerrainMatrix[i]);
            this.debugTerrainMatrix[i].x = this.debugTerrainMatrix[i].x / this.debugTerrainMatrix[i].w;
            this.debugTerrainMatrix[i].y = this.debugTerrainMatrix[i].y / this.debugTerrainMatrix[i].w;
            this.debugTerrainMatrix[i].z = this.debugTerrainMatrix[i].z / this.debugTerrainMatrix[i].w;
            this.debugTerrainMatrix[i].w = 1.0F;
        }
    }

    protected Vector3f getViewVector(Entity entityIn, double partialTicks) {
        float f = (float)(entityIn.prevRotationPitch + (entityIn.rotationPitch - entityIn.prevRotationPitch) * partialTicks);
        float f1 = (float)(entityIn.prevRotationYaw + (entityIn.rotationYaw - entityIn.prevRotationYaw) * partialTicks);
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            f += 180.0F;
        }

        float f2 = MathHelper.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * (float) (Math.PI / 180.0));
        float f5 = MathHelper.sin(-f * (float) (Math.PI / 180.0));
        return new Vector3f(f3 * f4, f5, f2 * f4);
    }

    public int renderBlockLayer(EnumWorldBlockLayer blockLayerIn, double partialTicks, int pass, Entity entityIn) {
        RenderHelper.disableStandardItemLighting();
        if (blockLayerIn == EnumWorldBlockLayer.TRANSLUCENT && !Shaders.isShadowPass) {
            this.mc.mcProfiler.startSection("translucent_sort");
            double d0 = entityIn.posX - this.prevRenderSortX;
            double d1 = entityIn.posY - this.prevRenderSortY;
            double d2 = entityIn.posZ - this.prevRenderSortZ;
            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0) {
                this.prevRenderSortX = entityIn.posX;
                this.prevRenderSortY = entityIn.posY;
                this.prevRenderSortZ = entityIn.posZ;
                int k = 0;
                this.chunksToResortTransparency.clear();

                for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation : this.renderInfos) {
                    if (renderglobal$containerlocalrenderinformation.renderChunk.compiledChunk.isLayerStarted(blockLayerIn) && k++ < 15) {
                        this.chunksToResortTransparency.add(renderglobal$containerlocalrenderinformation.renderChunk);
                    }
                }
            }

            this.mc.mcProfiler.endSection();
        }

        this.mc.mcProfiler.startSection("filterempty");
        int l = 0;
        boolean flag = blockLayerIn == EnumWorldBlockLayer.TRANSLUCENT;
        int i1 = flag ? this.renderInfos.size() - 1 : 0;
        int i = flag ? -1 : this.renderInfos.size();
        int j1 = flag ? -1 : 1;

        for (int j = i1; j != i; j += j1) {
            RenderChunk renderchunk = this.renderInfos.get(j).renderChunk;
            if (!renderchunk.getCompiledChunk().isLayerEmpty(blockLayerIn)) {
                l++;
                this.renderContainer.addRenderChunk(renderchunk, blockLayerIn);
            }
        }

        if (l == 0) {
            this.mc.mcProfiler.endSection();
            return l;
        } else {
            if (Config.isFogOff() && this.mc.entityRenderer.fogStandard) {
                GlStateManager.disableFog();
            }

            this.mc.mcProfiler.endStartSection("render_" + blockLayerIn);
            this.renderBlockLayer(blockLayerIn);
            this.mc.mcProfiler.endSection();
            return l;
        }
    }

    private void renderBlockLayer(EnumWorldBlockLayer blockLayerIn) {
        this.mc.entityRenderer.enableLightmap();
        if (OpenGlHelper.useVbo()) {
            GL11.glEnableClientState(32884);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnableClientState(32888);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(32886);
        }

        if (Config.isShaders()) {
            ShadersRender.preRenderChunkLayer(blockLayerIn);
        }

        this.renderContainer.renderChunkLayer(blockLayerIn);
        if (Config.isShaders()) {
            ShadersRender.postRenderChunkLayer(blockLayerIn);
        }

        if (OpenGlHelper.useVbo()) {
            for (VertexFormatElement vertexformatelement : DefaultVertexFormats.BLOCK.getElements()) {
                EnumUsage vertexformatelement$enumusage = vertexformatelement.getUsage();
                int i = vertexformatelement.getIndex();
                switch (vertexformatelement$enumusage) {
                    case POSITION:
                        GL11.glDisableClientState(32884);
                        break;
                    case UV:
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + i);
                        GL11.glDisableClientState(32888);
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                        break;
                    case COLOR:
                        GL11.glDisableClientState(32886);
                        GlStateManager.resetColor();
                }
            }
        }

        this.mc.entityRenderer.disableLightmap();
    }

    private void cleanupDamagedBlocks(Iterator<DestroyBlockProgress> iteratorIn) {
        while (iteratorIn.hasNext()) {
            DestroyBlockProgress destroyblockprogress = iteratorIn.next();
            int i = destroyblockprogress.getCreationCloudUpdateTick();
            if (this.cloudTickCounter - i > 400) {
                iteratorIn.remove();
            }
        }
    }

    public void updateClouds() {
        if (Config.isShaders()) {
            if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(24)) {
                GuiShaderOptions gui = new GuiShaderOptions(null, Config.getGameSettings());
                Config.getMinecraft().displayGuiScreen(gui);
            }

            if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(19)) {
                Shaders.uninit();
                Shaders.loadShaderPack();
            }
        }

        this.cloudTickCounter++;
        if (this.cloudTickCounter % 20 == 0) {
            this.cleanupDamagedBlocks(this.damagedBlocks.values().iterator());
        }
    }

    private void renderSkyEnd() {
        if (Config.isSkyEnabled()) {
            GlStateManager.disableFog();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.depthMask(false);
            this.renderEngine.bindTexture(locationEndSkyPng);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer vertexbuffer = tessellator.getWorldRenderer();

            for (int i = 0; i < 6; i++) {
                GlStateManager.pushMatrix();
                if (i == 1) {
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 2) {
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 3) {
                    GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                }

                if (i == 4) {
                    GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                }

                if (i == 5) {
                    GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                }

                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int r = 40;
                int g = 40;
                int b = 40;
                if (Config.isCustomColors()) {
                    Vec3 vec3d = new Vec3(r / 255.0, g / 255.0, b / 255.0);
                    vec3d = CustomColors.getWorldSkyColor(vec3d, this.theWorld, this.mc.getRenderViewEntity(), 0.0F);
                    r = (int)(vec3d.xCoord * 255.0);
                    g = (int)(vec3d.yCoord * 255.0);
                    b = (int)(vec3d.zCoord * 255.0);
                }

                vertexbuffer.pos(-100.0, -100.0, -100.0).tex(0.0, 0.0).color(r, g, b, 255).endVertex();
                vertexbuffer.pos(-100.0, -100.0, 100.0).tex(0.0, 16.0).color(r, g, b, 255).endVertex();
                vertexbuffer.pos(100.0, -100.0, 100.0).tex(16.0, 16.0).color(r, g, b, 255).endVertex();
                vertexbuffer.pos(100.0, -100.0, -100.0).tex(16.0, 0.0).color(r, g, b, 255).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    }

    public void renderSky(float partialTicks, int pass) {
        if (Reflector.ForgeWorldProvider_getSkyRenderer.exists()) {
            WorldProvider wp = this.mc.theWorld.provider;
            Object skyRenderer = Reflector.call(wp, Reflector.ForgeWorldProvider_getSkyRenderer);
            if (skyRenderer != null) {
                Reflector.callVoid(skyRenderer, Reflector.IRenderHandler_render, partialTicks, this.theWorld, this.mc);
                return;
            }
        }

        if (this.mc.theWorld.provider.getDimensionId() == 1) {
            this.renderSkyEnd();
        } else if (this.mc.theWorld.provider.isSurfaceWorld()) {
            GlStateManager.disableTexture2D();
            boolean isShaders = Config.isShaders();
            if (isShaders) {
                Shaders.disableTexture2D();
            }

            Vec3 vec3 = this.theWorld.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
            vec3 = CustomColors.getSkyColor(
                vec3, this.mc.theWorld, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0, this.mc.getRenderViewEntity().posZ
            );
            if (isShaders) {
                Shaders.setSkyColor(vec3);
            }

            float f = (float)vec3.xCoord;
            float f1 = (float)vec3.yCoord;
            float f2 = (float)vec3.zCoord;
            if (pass != 2) {
                float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
                float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
                float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
                f = f3;
                f1 = f4;
                f2 = f5;
            }

            GlStateManager.color(f, f1, f2);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
            GlStateManager.depthMask(false);
            GlStateManager.enableFog();
            if (isShaders) {
                Shaders.enableFog();
            }

            GlStateManager.color(f, f1, f2);
            if (isShaders) {
                Shaders.preSkyList();
            }

            if (Config.isSkyEnabled()) {
                if (this.vboEnabled) {
                    this.skyVBO.bindBuffer();
                    GL11.glEnableClientState(32884);
                    GL11.glVertexPointer(3, 5126, 12, 0L);
                    this.skyVBO.drawArrays(7);
                    this.skyVBO.unbindBuffer();
                    GL11.glDisableClientState(32884);
                } else {
                    GlStateManager.callList(this.glSkyList);
                }
            }

            GlStateManager.disableFog();
            if (isShaders) {
                Shaders.disableFog();
            }

            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.disableStandardItemLighting();
            float[] afloat = this.theWorld.provider.calcSunriseSunsetColors(this.theWorld.getCelestialAngle(partialTicks), partialTicks);
            if (afloat != null && Config.isSunMoonEnabled()) {
                GlStateManager.disableTexture2D();
                if (isShaders) {
                    Shaders.disableTexture2D();
                }

                GlStateManager.shadeModel(7425);
                GlStateManager.pushMatrix();
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(MathHelper.sin(this.theWorld.getCelestialAngleRadians(partialTicks)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                float f6 = afloat[0];
                float f7 = afloat[1];
                float f8 = afloat[2];
                if (pass != 2) {
                    float f9 = (f6 * 30.0F + f7 * 59.0F + f8 * 11.0F) / 100.0F;
                    float f10 = (f6 * 30.0F + f7 * 70.0F) / 100.0F;
                    float f11 = (f6 * 30.0F + f8 * 70.0F) / 100.0F;
                    f6 = f9;
                    f7 = f10;
                    f8 = f11;
                }

                vertexbuffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
                vertexbuffer.pos(0.0, 100.0, 0.0).color(f6, f7, f8, afloat[3]).endVertex();
                int j = 16;

                for (int l = 0; l <= 16; l++) {
                    float f21 = l * (float) Math.PI * 2.0F / 16.0F;
                    float f12 = MathHelper.sin(f21);
                    float f13 = MathHelper.cos(f21);
                    vertexbuffer.pos(f12 * 120.0F, f13 * 120.0F, -f13 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
                }

                tessellator.draw();
                GlStateManager.popMatrix();
                GlStateManager.shadeModel(7424);
            }

            GlStateManager.enableTexture2D();
            if (isShaders) {
                Shaders.enableTexture2D();
            }

            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
            GlStateManager.pushMatrix();
            float f16 = 1.0F - this.theWorld.getRainStrength(partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, f16);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            CustomSky.renderSky(this.theWorld, this.renderEngine, partialTicks);
            if (isShaders) {
                Shaders.preCelestialRotate();
            }

            GlStateManager.rotate(this.theWorld.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);
            if (isShaders) {
                Shaders.postCelestialRotate();
            }

            float f17 = 30.0F;
            if (Config.isSunTexture()) {
                this.renderEngine.bindTexture(locationSunPng);
                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
                vertexbuffer.pos(-f17, 100.0, -f17).tex(0.0, 0.0).endVertex();
                vertexbuffer.pos(f17, 100.0, -f17).tex(1.0, 0.0).endVertex();
                vertexbuffer.pos(f17, 100.0, f17).tex(1.0, 1.0).endVertex();
                vertexbuffer.pos(-f17, 100.0, f17).tex(0.0, 1.0).endVertex();
                tessellator.draw();
            }

            f17 = 20.0F;
            if (Config.isMoonTexture()) {
                this.renderEngine.bindTexture(locationMoonPhasesPng);
                int i = this.theWorld.getMoonPhase();
                int k = i % 4;
                int i1 = i / 4 % 2;
                float f22 = (k + 0) / 4.0F;
                float f23 = (i1 + 0) / 2.0F;
                float f24 = (k + 1) / 4.0F;
                float f14 = (i1 + 1) / 2.0F;
                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
                vertexbuffer.pos(-f17, -100.0, f17).tex(f24, f14).endVertex();
                vertexbuffer.pos(f17, -100.0, f17).tex(f22, f14).endVertex();
                vertexbuffer.pos(f17, -100.0, -f17).tex(f22, f23).endVertex();
                vertexbuffer.pos(-f17, -100.0, -f17).tex(f24, f23).endVertex();
                tessellator.draw();
            }

            GlStateManager.disableTexture2D();
            if (isShaders) {
                Shaders.disableTexture2D();
            }

            float f15 = this.theWorld.getStarBrightness(partialTicks) * f16;
            if (f15 > 0.0F && Config.isStarsEnabled() && !CustomSky.hasSkyLayers(this.theWorld)) {
                GlStateManager.color(f15, f15, f15, f15);
                if (this.vboEnabled) {
                    this.starVBO.bindBuffer();
                    GL11.glEnableClientState(32884);
                    GL11.glVertexPointer(3, 5126, 12, 0L);
                    this.starVBO.drawArrays(7);
                    this.starVBO.unbindBuffer();
                    GL11.glDisableClientState(32884);
                } else {
                    GlStateManager.callList(this.starGLCallList);
                }
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableFog();
            if (isShaders) {
                Shaders.enableFog();
            }

            GlStateManager.popMatrix();
            GlStateManager.disableTexture2D();
            if (isShaders) {
                Shaders.disableTexture2D();
            }

            GlStateManager.color(0.0F, 0.0F, 0.0F);
            double d0 = this.mc.thePlayer.getPositionEyes(partialTicks).yCoord - this.theWorld.getHorizon();
            if (d0 < 0.0) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 12.0F, 0.0F);
                if (this.vboEnabled) {
                    this.sky2VBO.bindBuffer();
                    GL11.glEnableClientState(32884);
                    GL11.glVertexPointer(3, 5126, 12, 0L);
                    this.sky2VBO.drawArrays(7);
                    this.sky2VBO.unbindBuffer();
                    GL11.glDisableClientState(32884);
                } else {
                    GlStateManager.callList(this.glSkyList2);
                }

                GlStateManager.popMatrix();
                float f18 = 1.0F;
                float f19 = -((float)(d0 + 65.0));
                float f20 = -1.0F;
                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                vertexbuffer.pos(-1.0, f19, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, f19, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, f19, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, f19, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, f19, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, f19, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, f19, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, f19, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(-1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, 1.0).color(0, 0, 0, 255).endVertex();
                vertexbuffer.pos(1.0, -1.0, -1.0).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
            }

            if (this.theWorld.provider.isSkyColored()) {
                GlStateManager.color(f * 0.2F + 0.04F, f1 * 0.2F + 0.04F, f2 * 0.6F + 0.1F);
            } else {
                GlStateManager.color(f, f1, f2);
            }

            if (this.mc.gameSettings.renderDistanceChunks <= 4) {
                GlStateManager.color(this.mc.entityRenderer.fogColorRed, this.mc.entityRenderer.fogColorGreen, this.mc.entityRenderer.fogColorBlue);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -((float)(d0 - 16.0)), 0.0F);
            if (Config.isSkyEnabled()) {
                if (this.vboEnabled) {
                    this.sky2VBO.bindBuffer();
                    GlStateManager.glEnableClientState(32884);
                    GlStateManager.glVertexPointer(3, 5126, 12, 0);
                    this.sky2VBO.drawArrays(7);
                    this.sky2VBO.unbindBuffer();
                    GlStateManager.glDisableClientState(32884);
                } else {
                    GlStateManager.callList(this.glSkyList2);
                }
            }

            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            if (isShaders) {
                Shaders.enableTexture2D();
            }

            GlStateManager.depthMask(true);
        }
    }

    public void renderClouds(float partialTicks, int pass) {
        if (!Config.isCloudsOff()) {
            if (Reflector.ForgeWorldProvider_getCloudRenderer.exists()) {
                WorldProvider wp = this.mc.theWorld.provider;
                Object cloudRenderer = Reflector.call(wp, Reflector.ForgeWorldProvider_getCloudRenderer);
                if (cloudRenderer != null) {
                    Reflector.callVoid(cloudRenderer, Reflector.IRenderHandler_render, partialTicks, this.theWorld, this.mc);
                    return;
                }
            }

            if (this.mc.theWorld.provider.isSurfaceWorld()) {
                if (Config.isShaders()) {
                    Shaders.beginClouds();
                }

                if (Config.isCloudsFancy()) {
                    this.renderCloudsFancy(partialTicks, pass);
                } else {
                    float var27 = 0.0F;
                    GlStateManager.disableCull();
                    float f = (float)(
                        this.mc.getRenderViewEntity().lastTickPosY + (this.mc.getRenderViewEntity().posY - this.mc.getRenderViewEntity().lastTickPosY) * var27
                    );
                    int i = 32;
                    int j = 8;
                    Tessellator tessellator = Tessellator.getInstance();
                    WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
                    this.renderEngine.bindTexture(locationCloudsPng);
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    Vec3 vec3 = this.theWorld.getCloudColour(var27);
                    float f1 = (float)vec3.xCoord;
                    float f2 = (float)vec3.yCoord;
                    float f3 = (float)vec3.zCoord;
                    this.cloudRenderer.prepareToRender(false, this.cloudTickCounter, partialTicks, vec3);
                    if (this.cloudRenderer.shouldUpdateGlList()) {
                        this.cloudRenderer.startUpdateGlList();
                        if (pass != 2) {
                            float f4 = (f1 * 30.0F + f2 * 59.0F + f3 * 11.0F) / 100.0F;
                            float f5 = (f1 * 30.0F + f2 * 70.0F) / 100.0F;
                            float f6 = (f1 * 30.0F + f3 * 70.0F) / 100.0F;
                            f1 = f4;
                            f2 = f5;
                            f3 = f6;
                        }

                        float f10 = 4.8828125E-4F;
                        double d2 = this.cloudTickCounter + var27;
                        double d0 = this.mc.getRenderViewEntity().prevPosX
                            + (this.mc.getRenderViewEntity().posX - this.mc.getRenderViewEntity().prevPosX) * var27
                            + d2 * 0.03F;
                        double d1 = this.mc.getRenderViewEntity().prevPosZ
                            + (this.mc.getRenderViewEntity().posZ - this.mc.getRenderViewEntity().prevPosZ) * var27;
                        int k = MathHelper.floor_double(d0 / 2048.0);
                        int l = MathHelper.floor_double(d1 / 2048.0);
                        d0 -= k * 2048;
                        d1 -= l * 2048;
                        float f7 = this.theWorld.provider.getCloudHeight() - f + 0.33F;
                        f7 += this.mc.gameSettings.ofCloudsHeight * 128.0F;
                        float f8 = (float)(d0 * 4.8828125E-4);
                        float f9 = (float)(d1 * 4.8828125E-4);
                        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

                        for (int i1 = -256; i1 < 256; i1 += 32) {
                            for (int j1 = -256; j1 < 256; j1 += 32) {
                                vertexbuffer.pos(i1 + 0, f7, j1 + 32)
                                    .tex((i1 + 0) * 4.8828125E-4F + f8, (j1 + 32) * 4.8828125E-4F + f9)
                                    .color(f1, f2, f3, 0.8F)
                                    .endVertex();
                                vertexbuffer.pos(i1 + 32, f7, j1 + 32)
                                    .tex((i1 + 32) * 4.8828125E-4F + f8, (j1 + 32) * 4.8828125E-4F + f9)
                                    .color(f1, f2, f3, 0.8F)
                                    .endVertex();
                                vertexbuffer.pos(i1 + 32, f7, j1 + 0)
                                    .tex((i1 + 32) * 4.8828125E-4F + f8, (j1 + 0) * 4.8828125E-4F + f9)
                                    .color(f1, f2, f3, 0.8F)
                                    .endVertex();
                                vertexbuffer.pos(i1 + 0, f7, j1 + 0)
                                    .tex((i1 + 0) * 4.8828125E-4F + f8, (j1 + 0) * 4.8828125E-4F + f9)
                                    .color(f1, f2, f3, 0.8F)
                                    .endVertex();
                            }
                        }

                        tessellator.draw();
                        this.cloudRenderer.endUpdateGlList();
                    }

                    this.cloudRenderer.renderGlList();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.disableBlend();
                    GlStateManager.enableCull();
                }

                if (Config.isShaders()) {
                    Shaders.endClouds();
                }
            }
        }
    }

    public boolean hasCloudFog(double x, double y, double z, float partialTicks) {
        return false;
    }

    private void renderCloudsFancy(float partialTicks, int pass) {
        float var46 = 0.0F;
        GlStateManager.disableCull();
        float f = (float)(
            this.mc.getRenderViewEntity().lastTickPosY + (this.mc.getRenderViewEntity().posY - this.mc.getRenderViewEntity().lastTickPosY) * var46
        );
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        float f1 = 12.0F;
        float f2 = 4.0F;
        double d0 = this.cloudTickCounter + var46;
        double d1 = (
                this.mc.getRenderViewEntity().prevPosX + (this.mc.getRenderViewEntity().posX - this.mc.getRenderViewEntity().prevPosX) * var46 + d0 * 0.03F
            )
            / 12.0;
        double d2 = (this.mc.getRenderViewEntity().prevPosZ + (this.mc.getRenderViewEntity().posZ - this.mc.getRenderViewEntity().prevPosZ) * var46) / 12.0
            + 0.33F;
        float f3 = this.theWorld.provider.getCloudHeight() - f + 0.33F;
        f3 += this.mc.gameSettings.ofCloudsHeight * 128.0F;
        int i = MathHelper.floor_double(d1 / 2048.0);
        int j = MathHelper.floor_double(d2 / 2048.0);
        d1 -= i * 2048;
        d2 -= j * 2048;
        this.renderEngine.bindTexture(locationCloudsPng);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Vec3 vec3 = this.theWorld.getCloudColour(var46);
        float f4 = (float)vec3.xCoord;
        float f5 = (float)vec3.yCoord;
        float f6 = (float)vec3.zCoord;
        this.cloudRenderer.prepareToRender(true, this.cloudTickCounter, partialTicks, vec3);
        if (pass != 2) {
            float f7 = (f4 * 30.0F + f5 * 59.0F + f6 * 11.0F) / 100.0F;
            float f8 = (f4 * 30.0F + f5 * 70.0F) / 100.0F;
            float f9 = (f4 * 30.0F + f6 * 70.0F) / 100.0F;
            f4 = f7;
            f5 = f8;
            f6 = f9;
        }

        float f26 = f4 * 0.9F;
        float f27 = f5 * 0.9F;
        float f28 = f6 * 0.9F;
        float f10 = f4 * 0.7F;
        float f11 = f5 * 0.7F;
        float f12 = f6 * 0.7F;
        float f13 = f4 * 0.8F;
        float f14 = f5 * 0.8F;
        float f15 = f6 * 0.8F;
        float f16 = 0.00390625F;
        float f17 = MathHelper.floor_double(d1) * 0.00390625F;
        float f18 = MathHelper.floor_double(d2) * 0.00390625F;
        float f19 = (float)(d1 - MathHelper.floor_double(d1));
        float f20 = (float)(d2 - MathHelper.floor_double(d2));
        int k = 8;
        int l = 4;
        float f21 = 9.765625E-4F;
        GlStateManager.scale(12.0F, 1.0F, 12.0F);

        for (int i1 = 0; i1 < 2; i1++) {
            if (i1 == 0) {
                GlStateManager.colorMask(false, false, false, false);
            } else {
                switch (pass) {
                    case 0:
                        GlStateManager.colorMask(false, true, true, true);
                        break;
                    case 1:
                        GlStateManager.colorMask(true, false, false, true);
                        break;
                    case 2:
                        GlStateManager.colorMask(true, true, true, true);
                }
            }

            this.cloudRenderer.renderGlList();
        }

        if (this.cloudRenderer.shouldUpdateGlList()) {
            this.cloudRenderer.startUpdateGlList();

            for (int j1 = -3; j1 <= 4; j1++) {
                for (int k1 = -3; k1 <= 4; k1++) {
                    vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
                    float f22 = j1 * 8;
                    float f23 = k1 * 8;
                    float f24 = f22 - f19;
                    float f25 = f23 - f20;
                    if (f3 > -5.0F) {
                        vertexbuffer.pos(f24 + 0.0F, f3 + 0.0F, f25 + 8.0F)
                            .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                            .color(f10, f11, f12, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 8.0F, f3 + 0.0F, f25 + 8.0F)
                            .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                            .color(f10, f11, f12, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 8.0F, f3 + 0.0F, f25 + 0.0F)
                            .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                            .color(f10, f11, f12, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 0.0F, f3 + 0.0F, f25 + 0.0F)
                            .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                            .color(f10, f11, f12, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                    }

                    if (f3 <= 5.0F) {
                        vertexbuffer.pos(f24 + 0.0F, f3 + 4.0F - 9.765625E-4F, f25 + 8.0F)
                            .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                            .color(f4, f5, f6, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 8.0F, f3 + 4.0F - 9.765625E-4F, f25 + 8.0F)
                            .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                            .color(f4, f5, f6, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 8.0F, f3 + 4.0F - 9.765625E-4F, f25 + 0.0F)
                            .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                            .color(f4, f5, f6, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                        vertexbuffer.pos(f24 + 0.0F, f3 + 4.0F - 9.765625E-4F, f25 + 0.0F)
                            .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                            .color(f4, f5, f6, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                    }

                    if (j1 > -1) {
                        for (int l1 = 0; l1 < 8; l1++) {
                            vertexbuffer.pos(f24 + l1 + 0.0F, f3 + 0.0F, f25 + 8.0F)
                                .tex((f22 + l1 + 0.5F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(-1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + l1 + 0.0F, f3 + 4.0F, f25 + 8.0F)
                                .tex((f22 + l1 + 0.5F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(-1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + l1 + 0.0F, f3 + 4.0F, f25 + 0.0F)
                                .tex((f22 + l1 + 0.5F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(-1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + l1 + 0.0F, f3 + 0.0F, f25 + 0.0F)
                                .tex((f22 + l1 + 0.5F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(-1.0F, 0.0F, 0.0F)
                                .endVertex();
                        }
                    }

                    if (j1 <= 1) {
                        for (int i2 = 0; i2 < 8; i2++) {
                            vertexbuffer.pos(f24 + i2 + 1.0F - 9.765625E-4F, f3 + 0.0F, f25 + 8.0F)
                                .tex((f22 + i2 + 0.5F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + i2 + 1.0F - 9.765625E-4F, f3 + 4.0F, f25 + 8.0F)
                                .tex((f22 + i2 + 0.5F) * 0.00390625F + f17, (f23 + 8.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + i2 + 1.0F - 9.765625E-4F, f3 + 4.0F, f25 + 0.0F)
                                .tex((f22 + i2 + 0.5F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(1.0F, 0.0F, 0.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + i2 + 1.0F - 9.765625E-4F, f3 + 0.0F, f25 + 0.0F)
                                .tex((f22 + i2 + 0.5F) * 0.00390625F + f17, (f23 + 0.0F) * 0.00390625F + f18)
                                .color(f26, f27, f28, 0.8F)
                                .normal(1.0F, 0.0F, 0.0F)
                                .endVertex();
                        }
                    }

                    if (k1 > -1) {
                        for (int j2 = 0; j2 < 8; j2++) {
                            vertexbuffer.pos(f24 + 0.0F, f3 + 4.0F, f25 + j2 + 0.0F)
                                .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + j2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, -1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 8.0F, f3 + 4.0F, f25 + j2 + 0.0F)
                                .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + j2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, -1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 8.0F, f3 + 0.0F, f25 + j2 + 0.0F)
                                .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + j2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, -1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 0.0F, f3 + 0.0F, f25 + j2 + 0.0F)
                                .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + j2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, -1.0F)
                                .endVertex();
                        }
                    }

                    if (k1 <= 1) {
                        for (int k2 = 0; k2 < 8; k2++) {
                            vertexbuffer.pos(f24 + 0.0F, f3 + 4.0F, f25 + k2 + 1.0F - 9.765625E-4F)
                                .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + k2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, 1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 8.0F, f3 + 4.0F, f25 + k2 + 1.0F - 9.765625E-4F)
                                .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + k2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, 1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 8.0F, f3 + 0.0F, f25 + k2 + 1.0F - 9.765625E-4F)
                                .tex((f22 + 8.0F) * 0.00390625F + f17, (f23 + k2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, 1.0F)
                                .endVertex();
                            vertexbuffer.pos(f24 + 0.0F, f3 + 0.0F, f25 + k2 + 1.0F - 9.765625E-4F)
                                .tex((f22 + 0.0F) * 0.00390625F + f17, (f23 + k2 + 0.5F) * 0.00390625F + f18)
                                .color(f13, f14, f15, 0.8F)
                                .normal(0.0F, 0.0F, 1.0F)
                                .endVertex();
                        }
                    }

                    tessellator.draw();
                }
            }

            this.cloudRenderer.endUpdateGlList();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
    }

    public void updateChunks(long finishTimeNano) {
        finishTimeNano = (long)(finishTimeNano + 1.0E8);
        this.displayListEntitiesDirty = this.displayListEntitiesDirty | this.renderDispatcher.runChunkUploads(finishTimeNano);
        if (this.chunksToUpdateForced.size() > 0) {
            Iterator itForced = this.chunksToUpdateForced.iterator();

            while (itForced.hasNext()) {
                RenderChunk rc = (RenderChunk)itForced.next();
                if (!this.renderDispatcher.updateChunkLater(rc)) {
                    break;
                }

                rc.setNeedsUpdate(false);
                itForced.remove();
                this.chunksToUpdate.remove(rc);
                this.chunksToResortTransparency.remove(rc);
            }
        }

        if (this.chunksToResortTransparency.size() > 0) {
            Iterator itTransparency = this.chunksToResortTransparency.iterator();
            if (itTransparency.hasNext()) {
                RenderChunk renderChunk = (RenderChunk)itTransparency.next();
                if (this.renderDispatcher.updateTransparencyLater(renderChunk)) {
                    itTransparency.remove();
                }
            }
        }

        double weightTotal = 0.0;
        int updatesPerFrame = Config.getUpdatesPerFrame();
        if (!this.chunksToUpdate.isEmpty()) {
            Iterator<RenderChunk> iterator = this.chunksToUpdate.iterator();

            while (iterator.hasNext()) {
                RenderChunk renderchunk = iterator.next();
                boolean empty = renderchunk.isChunkRegionEmpty();
                boolean flag;
                if (empty) {
                    flag = this.renderDispatcher.updateChunkNow(renderchunk);
                } else {
                    flag = this.renderDispatcher.updateChunkLater(renderchunk);
                }

                if (!flag) {
                    break;
                }

                renderchunk.setNeedsUpdate(false);
                iterator.remove();
                if (!empty) {
                    double weight = 2.0 * RenderChunkUtils.getRelativeBufferSize(renderchunk);
                    weightTotal += weight;
                    if (weightTotal > updatesPerFrame) {
                        break;
                    }
                }
            }
        }
    }

    public void renderWorldBorder(Entity entityIn, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        WorldBorder worldborder = this.theWorld.getWorldBorder();
        double d0 = this.mc.gameSettings.renderDistanceChunks * 16;
        if (entityIn.posX >= worldborder.maxX() - d0
            || entityIn.posX <= worldborder.minX() + d0
            || entityIn.posZ >= worldborder.maxZ() - d0
            || entityIn.posZ <= worldborder.minZ() + d0) {
            if (Config.isShaders()) {
                Shaders.pushProgram();
                Shaders.useProgram(Shaders.ProgramTexturedLit);
            }

            double d1 = 1.0 - worldborder.getClosestDistance(entityIn) / d0;
            d1 = Math.pow(d1, 4.0);
            double d2 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
            double d3 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
            double d4 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
            this.renderEngine.bindTexture(locationForcefieldPng);
            GlStateManager.depthMask(false);
            GlStateManager.pushMatrix();
            int i = worldborder.getStatus().getID();
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            GlStateManager.color(f, f1, f2, (float)d1);
            GlStateManager.doPolygonOffset(-3.0F, -3.0F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableAlpha();
            GlStateManager.disableCull();
            float f3 = (float)(Minecraft.getSystemTime() % 3000L) / 3000.0F;
            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 128.0F;
            vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            vertexbuffer.setTranslation(-d2, -d3, -d4);
            double d5 = Math.max((double)MathHelper.floor_double(d4 - d0), worldborder.minZ());
            double d6 = Math.min((double)MathHelper.ceiling_double_int(d4 + d0), worldborder.maxZ());
            if (d2 > worldborder.maxX() - d0) {
                float f7 = 0.0F;

                for (double d7 = d5; d7 < d6; f7 += 0.5F) {
                    double d8 = Math.min(1.0, d6 - d7);
                    float f8 = (float)d8 * 0.5F;
                    vertexbuffer.pos(worldborder.maxX(), 256.0, d7).tex(f3 + f7, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(worldborder.maxX(), 256.0, d7 + d8).tex(f3 + f8 + f7, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(worldborder.maxX(), 0.0, d7 + d8).tex(f3 + f8 + f7, f3 + 128.0F).endVertex();
                    vertexbuffer.pos(worldborder.maxX(), 0.0, d7).tex(f3 + f7, f3 + 128.0F).endVertex();
                    d7++;
                }
            }

            if (d2 < worldborder.minX() + d0) {
                float f9 = 0.0F;

                for (double d9 = d5; d9 < d6; f9 += 0.5F) {
                    double d12 = Math.min(1.0, d6 - d9);
                    float f12 = (float)d12 * 0.5F;
                    vertexbuffer.pos(worldborder.minX(), 256.0, d9).tex(f3 + f9, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(worldborder.minX(), 256.0, d9 + d12).tex(f3 + f12 + f9, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(worldborder.minX(), 0.0, d9 + d12).tex(f3 + f12 + f9, f3 + 128.0F).endVertex();
                    vertexbuffer.pos(worldborder.minX(), 0.0, d9).tex(f3 + f9, f3 + 128.0F).endVertex();
                    d9++;
                }
            }

            d5 = Math.max((double)MathHelper.floor_double(d2 - d0), worldborder.minX());
            d6 = Math.min((double)MathHelper.ceiling_double_int(d2 + d0), worldborder.maxX());
            if (d4 > worldborder.maxZ() - d0) {
                float f10 = 0.0F;

                for (double d10 = d5; d10 < d6; f10 += 0.5F) {
                    double d13 = Math.min(1.0, d6 - d10);
                    float f13 = (float)d13 * 0.5F;
                    vertexbuffer.pos(d10, 256.0, worldborder.maxZ()).tex(f3 + f10, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(d10 + d13, 256.0, worldborder.maxZ()).tex(f3 + f13 + f10, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(d10 + d13, 0.0, worldborder.maxZ()).tex(f3 + f13 + f10, f3 + 128.0F).endVertex();
                    vertexbuffer.pos(d10, 0.0, worldborder.maxZ()).tex(f3 + f10, f3 + 128.0F).endVertex();
                    d10++;
                }
            }

            if (d4 < worldborder.minZ() + d0) {
                float f11 = 0.0F;

                for (double d11 = d5; d11 < d6; f11 += 0.5F) {
                    double d14 = Math.min(1.0, d6 - d11);
                    float f14 = (float)d14 * 0.5F;
                    vertexbuffer.pos(d11, 256.0, worldborder.minZ()).tex(f3 + f11, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(d11 + d14, 256.0, worldborder.minZ()).tex(f3 + f14 + f11, f3 + 0.0F).endVertex();
                    vertexbuffer.pos(d11 + d14, 0.0, worldborder.minZ()).tex(f3 + f14 + f11, f3 + 128.0F).endVertex();
                    vertexbuffer.pos(d11, 0.0, worldborder.minZ()).tex(f3 + f11, f3 + 128.0F).endVertex();
                    d11++;
                }
            }

            tessellator.draw();
            vertexbuffer.setTranslation(0.0, 0.0, 0.0);
            GlStateManager.enableCull();
            GlStateManager.disableAlpha();
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
            if (Config.isShaders()) {
                Shaders.popProgram();
            }
        }
    }

    private void preRenderDamagedBlocks() {
        GlStateManager.tryBlendFuncSeparate(774, 768, 1, 0);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
        GlStateManager.doPolygonOffset(-1.0F, -10.0F);
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();
        if (Config.isShaders()) {
            ShadersRender.beginBlockDamage();
        }
    }

    private void postRenderDamagedBlocks() {
        GlStateManager.disableAlpha();
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
        if (Config.isShaders()) {
            ShadersRender.endBlockDamage();
        }
    }

    public void drawBlockDamageTexture(Tessellator tessellatorIn, WorldRenderer worldRendererIn, Entity entityIn, float partialTicks) {
        double d0 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        double d1 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        double d2 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
        if (!this.damagedBlocks.isEmpty()) {
            this.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            this.preRenderDamagedBlocks();
            worldRendererIn.begin(7, DefaultVertexFormats.BLOCK);
            worldRendererIn.setTranslation(-d0, -d1, -d2);
            worldRendererIn.noColor();
            Iterator<DestroyBlockProgress> iterator = this.damagedBlocks.values().iterator();

            while (iterator.hasNext()) {
                DestroyBlockProgress destroyblockprogress = iterator.next();
                BlockPos blockpos = destroyblockprogress.getPosition();
                double d3 = blockpos.getX() - d0;
                double d4 = blockpos.getY() - d1;
                double d5 = blockpos.getZ() - d2;
                Block block = this.theWorld.getBlockState(blockpos).getBlock();
                boolean renderBreaking;
                if (Reflector.ForgeTileEntity_canRenderBreaking.exists()) {
                    boolean tileEntityRenderBreaking = block instanceof BlockChest
                        || block instanceof BlockEnderChest
                        || block instanceof BlockSign
                        || block instanceof BlockSkull;
                    if (!tileEntityRenderBreaking) {
                        TileEntity te = this.theWorld.getTileEntity(blockpos);
                        if (te != null) {
                            tileEntityRenderBreaking = Reflector.callBoolean(te, Reflector.ForgeTileEntity_canRenderBreaking);
                        }
                    }

                    renderBreaking = !tileEntityRenderBreaking;
                } else {
                    renderBreaking = !(block instanceof BlockChest)
                        && !(block instanceof BlockEnderChest)
                        && !(block instanceof BlockSign)
                        && !(block instanceof BlockSkull);
                }

                if (renderBreaking) {
                    if (d3 * d3 + d4 * d4 + d5 * d5 > 1024.0) {
                        iterator.remove();
                    } else {
                        IBlockState iblockstate = this.theWorld.getBlockState(blockpos);
                        if (iblockstate.getBlock().getMaterial() != Material.air) {
                            int i = destroyblockprogress.getPartialBlockDamage();
                            TextureAtlasSprite textureatlassprite = this.destroyBlockIcons[i];
                            BlockRendererDispatcher blockrendererdispatcher = this.mc.getBlockRendererDispatcher();
                            blockrendererdispatcher.renderBlockDamage(iblockstate, blockpos, textureatlassprite, this.theWorld);
                        }
                    }
                }
            }

            tessellatorIn.draw();
            worldRendererIn.setTranslation(0.0, 0.0, 0.0);
            this.postRenderDamagedBlocks();
        }
    }

    public void drawSelectionBox(EntityPlayer player, MovingObjectPosition movingObjectPositionIn, int execute, float partialTicks) {
        if (execute == 0 && movingObjectPositionIn.typeOfHit == MovingObjectType.BLOCK) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
            GL11.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            if (Config.isShaders()) {
                Shaders.disableTexture2D();
            }

            GlStateManager.depthMask(false);
            float f = 0.002F;
            BlockPos blockpos = movingObjectPositionIn.getBlockPos();
            Block block = this.theWorld.getBlockState(blockpos).getBlock();
            if (block.getMaterial() != Material.air && this.theWorld.getWorldBorder().contains(blockpos)) {
                block.setBlockBoundsBasedOnState(this.theWorld, blockpos);
                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
                AxisAlignedBB aabb = block.getSelectedBoundingBox(this.theWorld, blockpos);
                EnumOffsetType offsetType = block.getOffsetType();
                if (offsetType != EnumOffsetType.NONE) {
                    aabb = BlockModelUtils.getOffsetBoundingBox(aabb, offsetType, blockpos);
                }

                drawSelectionBoundingBox(aabb.expand(0.002F, 0.002F, 0.002F).offset(-d0, -d1, -d2));
            }

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            if (Config.isShaders()) {
                Shaders.enableTexture2D();
            }

            GlStateManager.disableBlend();
        }
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();
        vertexbuffer.begin(1, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawOutlinedBoundingBox(AxisAlignedBB boundingBox, int red, int green, int blue, int alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
    }

    private void markBlocksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.viewFrustum.markBlocksForUpdate(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public void markBlockForUpdate(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        this.markBlocksForUpdate(i - 1, j - 1, k - 1, i + 1, j + 1, k + 1);
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        this.markBlocksForUpdate(i - 1, j - 1, k - 1, i + 1, j + 1, k + 1);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.markBlocksForUpdate(x1 - 1, y1 - 1, z1 - 1, x2 + 1, y2 + 1, z2 + 1);
    }

    @Override
    public void playRecord(String recordName, BlockPos blockPosIn) {
        ISound isound = this.mapSoundPositions.get(blockPosIn);
        if (isound != null) {
            this.mc.getSoundHandler().stopSound(isound);
            this.mapSoundPositions.remove(blockPosIn);
        }

        if (recordName != null) {
            ItemRecord itemrecord = ItemRecord.getRecord(recordName);
            if (itemrecord != null) {
                this.mc.ingameGUI.setRecordPlayingMessage(itemrecord.getRecordNameLocal());
            }

            PositionedSoundRecord positionedsoundrecord = PositionedSoundRecord.create(
                new ResourceLocation(recordName), blockPosIn.getX(), blockPosIn.getY(), blockPosIn.getZ()
            );
            this.mapSoundPositions.put(blockPosIn, positionedsoundrecord);
            this.mc.getSoundHandler().playSound(positionedsoundrecord);
        }
    }

    @Override
    public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer except, String soundName, double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void spawnParticle(
        int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... parameters
    ) {
        try {
            this.spawnEntityFX(particleID, ignoreRange, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, parameters);
        } catch (Throwable var19) {
            CrashReport crashreport = CrashReport.makeCrashReport(var19, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being added");
            crashreportcategory.addCrashSection("ID", particleID);
            if (parameters != null) {
                crashreportcategory.addCrashSection("Parameters", parameters);
            }

            crashreportcategory.addCrashSectionCallable("Position", new Callable<String>() {
                public String call() throws Exception {
                    return CrashReportCategory.getCoordinateInfo(xCoord, yCoord, zCoord);
                }
            });
            throw new ReportedException(crashreport);
        }
    }

    private void spawnParticle(
        EnumParticleTypes particleIn, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... parameters
    ) {
        this.spawnParticle(particleIn.getParticleID(), particleIn.getShouldIgnoreRange(), xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, parameters);
    }

    private EntityFX spawnEntityFX(
        int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... parameters
    ) {
        if (this.mc != null && this.mc.getRenderViewEntity() != null && this.mc.effectRenderer != null) {
            int i = this.mc.gameSettings.particleSetting;
            if (i == 1 && this.theWorld.rand.nextInt(3) == 0) {
                i = 2;
            }

            double d0 = this.mc.getRenderViewEntity().posX - xCoord;
            double d1 = this.mc.getRenderViewEntity().posY - yCoord;
            double d2 = this.mc.getRenderViewEntity().posZ - zCoord;
            if (particleID == EnumParticleTypes.EXPLOSION_HUGE.getParticleID() && !Config.isAnimatedExplosion()) {
                return null;
            } else if (particleID == EnumParticleTypes.EXPLOSION_LARGE.getParticleID() && !Config.isAnimatedExplosion()) {
                return null;
            } else if (particleID == EnumParticleTypes.EXPLOSION_NORMAL.getParticleID() && !Config.isAnimatedExplosion()) {
                return null;
            } else if (particleID == EnumParticleTypes.SUSPENDED.getParticleID() && !Config.isWaterParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SUSPENDED_DEPTH.getParticleID() && !Config.isVoidParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SMOKE_NORMAL.getParticleID() && !Config.isAnimatedSmoke()) {
                return null;
            } else if (particleID == EnumParticleTypes.SMOKE_LARGE.getParticleID() && !Config.isAnimatedSmoke()) {
                return null;
            } else if (particleID == EnumParticleTypes.SPELL_MOB.getParticleID() && !Config.isPotionParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SPELL_MOB_AMBIENT.getParticleID() && !Config.isPotionParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SPELL.getParticleID() && !Config.isPotionParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SPELL_INSTANT.getParticleID() && !Config.isPotionParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.SPELL_WITCH.getParticleID() && !Config.isPotionParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.PORTAL.getParticleID() && !Config.isPortalParticles()) {
                return null;
            } else if (particleID == EnumParticleTypes.FLAME.getParticleID() && !Config.isAnimatedFlame()) {
                return null;
            } else if (particleID == EnumParticleTypes.REDSTONE.getParticleID() && !Config.isAnimatedRedstone()) {
                return null;
            } else if (particleID == EnumParticleTypes.DRIP_WATER.getParticleID() && !Config.isDrippingWaterLava()) {
                return null;
            } else if (particleID == EnumParticleTypes.DRIP_LAVA.getParticleID() && !Config.isDrippingWaterLava()) {
                return null;
            } else if (particleID == EnumParticleTypes.FIREWORKS_SPARK.getParticleID() && !Config.isFireworkParticles()) {
                return null;
            } else {
                if (!ignoreRange) {
                    double maxDistSq = 256.0;
                    if (particleID == EnumParticleTypes.CRIT.getParticleID()) {
                        maxDistSq = 38416.0;
                    }

                    if (d0 * d0 + d1 * d1 + d2 * d2 > maxDistSq) {
                        return null;
                    }

                    if (i > 1) {
                        return null;
                    }
                }

                EntityFX entityFx = this.mc.effectRenderer.spawnEffectParticle(particleID, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, parameters);
                if (particleID == EnumParticleTypes.WATER_BUBBLE.getParticleID()) {
                    CustomColors.updateWaterFX(entityFx, this.theWorld, xCoord, yCoord, zCoord, this.renderEnv);
                }

                if (particleID == EnumParticleTypes.WATER_SPLASH.getParticleID()) {
                    CustomColors.updateWaterFX(entityFx, this.theWorld, xCoord, yCoord, zCoord, this.renderEnv);
                }

                if (particleID == EnumParticleTypes.WATER_DROP.getParticleID()) {
                    CustomColors.updateWaterFX(entityFx, this.theWorld, xCoord, yCoord, zCoord, this.renderEnv);
                }

                if (particleID == EnumParticleTypes.TOWN_AURA.getParticleID()) {
                    CustomColors.updateMyceliumFX(entityFx);
                }

                if (particleID == EnumParticleTypes.PORTAL.getParticleID()) {
                    CustomColors.updatePortalFX(entityFx);
                }

                if (particleID == EnumParticleTypes.REDSTONE.getParticleID()) {
                    CustomColors.updateReddustFX(entityFx, this.theWorld, xCoord, yCoord, zCoord);
                }

                return entityFx;
            }
        } else {
            return null;
        }
    }

    @Override
    public void onEntityAdded(Entity entityIn) {
        RandomEntities.entityLoaded(entityIn, this.theWorld);
        if (Config.isDynamicLights()) {
            DynamicLights.entityAdded(entityIn, this);
        }
    }

    @Override
    public void onEntityRemoved(Entity entityIn) {
        RandomEntities.entityUnloaded(entityIn, this.theWorld);
        if (Config.isDynamicLights()) {
            DynamicLights.entityRemoved(entityIn, this);
        }
    }

    public void deleteAllDisplayLists() {
    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {
        switch (soundID) {
            case 1013:
            case 1018:
                if (this.mc.getRenderViewEntity() != null) {
                    double d0 = pos.getX() - this.mc.getRenderViewEntity().posX;
                    double d1 = pos.getY() - this.mc.getRenderViewEntity().posY;
                    double d2 = pos.getZ() - this.mc.getRenderViewEntity().posZ;
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    double d4 = this.mc.getRenderViewEntity().posX;
                    double d5 = this.mc.getRenderViewEntity().posY;
                    double d6 = this.mc.getRenderViewEntity().posZ;
                    if (d3 > 0.0) {
                        d4 += d0 / d3 * 2.0;
                        d5 += d1 / d3 * 2.0;
                        d6 += d2 / d3 * 2.0;
                    }

                    if (soundID == 1013) {
                        this.theWorld.playSound(d4, d5, d6, "mob.wither.spawn", 1.0F, 1.0F, false);
                    } else {
                        this.theWorld.playSound(d4, d5, d6, "mob.enderdragon.end", 5.0F, 1.0F, false);
                    }
                }
        }
    }

    @Override
    public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int data) {
        Random random = this.theWorld.rand;
        switch (sfxType) {
            case 1000:
                this.theWorld.playSoundAtPos(blockPosIn, "random.click", 1.0F, 1.0F, false);
                break;
            case 1001:
                this.theWorld.playSoundAtPos(blockPosIn, "random.click", 1.0F, 1.2F, false);
                break;
            case 1002:
                this.theWorld.playSoundAtPos(blockPosIn, "random.bow", 1.0F, 1.2F, false);
                break;
            case 1003:
                this.theWorld.playSoundAtPos(blockPosIn, "random.door_open", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1004:
                this.theWorld.playSoundAtPos(blockPosIn, "random.fizz", 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);
                break;
            case 1005:
                if (Item.getItemById(data) instanceof ItemRecord) {
                    this.theWorld.playRecord(blockPosIn, "records." + ((ItemRecord)Item.getItemById(data)).recordName);
                } else {
                    this.theWorld.playRecord(blockPosIn, (String)null);
                }
                break;
            case 1006:
                this.theWorld.playSoundAtPos(blockPosIn, "random.door_close", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1007:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.ghast.charge", 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1008:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.ghast.fireball", 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1009:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.ghast.fireball", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1010:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.zombie.wood", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1011:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.zombie.metal", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1012:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.zombie.woodbreak", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1014:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.wither.shoot", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1015:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.bat.takeoff", 0.05F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1016:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.zombie.infect", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1017:
                this.theWorld.playSoundAtPos(blockPosIn, "mob.zombie.unfect", 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1020:
                this.theWorld.playSoundAtPos(blockPosIn, "random.anvil_break", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1021:
                this.theWorld.playSoundAtPos(blockPosIn, "random.anvil_use", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1022:
                this.theWorld.playSoundAtPos(blockPosIn, "random.anvil_land", 0.3F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2000:
                int l = data % 3 - 1;
                int i = data / 3 % 3 - 1;
                double d15 = blockPosIn.getX() + l * 0.6 + 0.5;
                double d17 = blockPosIn.getY() + 0.5;
                double d19 = blockPosIn.getZ() + i * 0.6 + 0.5;

                for (int k1 = 0; k1 < 10; k1++) {
                    double d20 = random.nextDouble() * 0.2 + 0.01;
                    double d21 = d15 + l * 0.01 + (random.nextDouble() - 0.5) * i * 0.5;
                    double d4 = d17 + (random.nextDouble() - 0.5) * 0.5;
                    double d6 = d19 + i * 0.01 + (random.nextDouble() - 0.5) * l * 0.5;
                    double d8 = l * d20 + random.nextGaussian() * 0.01;
                    double d10 = -0.03 + random.nextGaussian() * 0.01;
                    double d12 = i * d20 + random.nextGaussian() * 0.01;
                    this.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d21, d4, d6, d8, d10, d12);
                }

                return;
            case 2001:
                Block block = Block.getBlockById(data & 4095);
                if (block.getMaterial() != Material.air) {
                    this.mc
                        .getSoundHandler()
                        .playSound(
                            new PositionedSoundRecord(
                                new ResourceLocation(block.stepSound.getBreakSound()),
                                (block.stepSound.getVolume() + 1.0F) / 2.0F,
                                block.stepSound.getFrequency() * 0.8F,
                                blockPosIn.getX() + 0.5F,
                                blockPosIn.getY() + 0.5F,
                                blockPosIn.getZ() + 0.5F
                            )
                        );
                }

                this.mc.effectRenderer.addBlockDestroyEffects(blockPosIn, block.getStateFromMeta(data >> 12 & 0xFF));
                break;
            case 2002:
                double d13 = blockPosIn.getX();
                double d14 = blockPosIn.getY();
                double d16 = blockPosIn.getZ();

                for (int i1 = 0; i1 < 8; i1++) {
                    this.spawnParticle(
                        EnumParticleTypes.ITEM_CRACK,
                        d13,
                        d14,
                        d16,
                        random.nextGaussian() * 0.15,
                        random.nextDouble() * 0.2,
                        random.nextGaussian() * 0.15,
                        Item.getIdFromItem(Items.potionitem),
                        data
                    );
                }

                int j1 = Items.potionitem.getColorFromDamage(data);
                float f = (j1 >> 16 & 0xFF) / 255.0F;
                float f1 = (j1 >> 8 & 0xFF) / 255.0F;
                float f2 = (j1 >> 0 & 0xFF) / 255.0F;
                EnumParticleTypes enumparticletypes = EnumParticleTypes.SPELL;
                if (Items.potionitem.isEffectInstant(data)) {
                    enumparticletypes = EnumParticleTypes.SPELL_INSTANT;
                }

                for (int l1 = 0; l1 < 100; l1++) {
                    double d22 = random.nextDouble() * 4.0;
                    double d23 = random.nextDouble() * Math.PI * 2.0;
                    double d24 = Math.cos(d23) * d22;
                    double d9 = 0.01 + random.nextDouble() * 0.5;
                    double d11 = Math.sin(d23) * d22;
                    EntityFX entityfx = this.spawnEntityFX(
                        enumparticletypes.getParticleID(), enumparticletypes.getShouldIgnoreRange(), d13 + d24 * 0.1, d14 + 0.3, d16 + d11 * 0.1, d24, d9, d11
                    );
                    if (entityfx != null) {
                        float f3 = 0.75F + random.nextFloat() * 0.25F;
                        entityfx.setRBGColorF(f * f3, f1 * f3, f2 * f3);
                        entityfx.multiplyVelocity((float)d22);
                    }
                }

                this.theWorld.playSoundAtPos(blockPosIn, "game.potion.smash", 1.0F, this.theWorld.rand.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2003:
                double d0 = blockPosIn.getX() + 0.5;
                double d1 = blockPosIn.getY();
                double d2 = blockPosIn.getZ() + 0.5;

                for (int j = 0; j < 8; j++) {
                    this.spawnParticle(
                        EnumParticleTypes.ITEM_CRACK,
                        d0,
                        d1,
                        d2,
                        random.nextGaussian() * 0.15,
                        random.nextDouble() * 0.2,
                        random.nextGaussian() * 0.15,
                        Item.getIdFromItem(Items.ender_eye)
                    );
                }

                for (double d18 = 0.0; d18 < Math.PI * 2; d18 += Math.PI / 20) {
                    this.spawnParticle(
                        EnumParticleTypes.PORTAL, d0 + Math.cos(d18) * 5.0, d1 - 0.4, d2 + Math.sin(d18) * 5.0, Math.cos(d18) * -5.0, 0.0, Math.sin(d18) * -5.0
                    );
                    this.spawnParticle(
                        EnumParticleTypes.PORTAL, d0 + Math.cos(d18) * 5.0, d1 - 0.4, d2 + Math.sin(d18) * 5.0, Math.cos(d18) * -7.0, 0.0, Math.sin(d18) * -7.0
                    );
                }

                return;
            case 2004:
                for (int k = 0; k < 20; k++) {
                    double d3 = blockPosIn.getX() + 0.5 + (this.theWorld.rand.nextFloat() - 0.5) * 2.0;
                    double d5 = blockPosIn.getY() + 0.5 + (this.theWorld.rand.nextFloat() - 0.5) * 2.0;
                    double d7 = blockPosIn.getZ() + 0.5 + (this.theWorld.rand.nextFloat() - 0.5) * 2.0;
                    this.theWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d3, d5, d7, 0.0, 0.0, 0.0, new int[0]);
                    this.theWorld.spawnParticle(EnumParticleTypes.FLAME, d3, d5, d7, 0.0, 0.0, 0.0, new int[0]);
                }

                return;
            case 2005:
                ItemDye.spawnBonemealParticles(this.theWorld, blockPosIn, data);
        }
    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        if (progress >= 0 && progress < 10) {
            DestroyBlockProgress destroyblockprogress = this.damagedBlocks.get(breakerId);
            if (destroyblockprogress == null
                || destroyblockprogress.getPosition().getX() != pos.getX()
                || destroyblockprogress.getPosition().getY() != pos.getY()
                || destroyblockprogress.getPosition().getZ() != pos.getZ()) {
                destroyblockprogress = new DestroyBlockProgress(breakerId, pos);
                this.damagedBlocks.put(breakerId, destroyblockprogress);
            }

            destroyblockprogress.setPartialBlockDamage(progress);
            destroyblockprogress.setCloudUpdateTick(this.cloudTickCounter);
        } else {
            this.damagedBlocks.remove(breakerId);
        }
    }

    public void setDisplayListEntitiesDirty() {
        this.displayListEntitiesDirty = true;
    }

    public boolean hasNoChunkUpdates() {
        return this.chunksToUpdate.isEmpty() && this.renderDispatcher.hasChunkUpdates();
    }

    public void resetClouds() {
        this.cloudRenderer.reset();
    }

    public int getCountRenderers() {
        return this.viewFrustum.renderChunks.length;
    }

    public int getCountActiveRenderers() {
        return this.renderInfos.size();
    }

    public int getCountEntitiesRendered() {
        return this.countEntitiesRendered;
    }

    public int getCountTileEntitiesRendered() {
        return this.countTileEntitiesRendered;
    }

    public int getCountLoadedChunks() {
        if (this.theWorld == null) {
            return 0;
        } else {
            IChunkProvider chunkProvider = this.theWorld.getChunkProvider();
            if (chunkProvider == null) {
                return 0;
            } else {
                if (chunkProvider != this.worldChunkProvider) {
                    this.worldChunkProvider = chunkProvider;
                    this.worldChunkProviderMap = (LongHashMap)Reflector.getFieldValue(chunkProvider, Reflector.ChunkProviderClient_chunkMapping);
                }

                return this.worldChunkProviderMap == null ? 0 : this.worldChunkProviderMap.getNumHashElements();
            }
        }
    }

    public int getCountChunksToUpdate() {
        return this.chunksToUpdate.size();
    }

    public RenderChunk getRenderChunk(BlockPos pos) {
        return this.viewFrustum.getRenderChunk(pos);
    }

    public WorldClient getWorld() {
        return this.theWorld;
    }

    private void clearRenderInfos() {
        if (renderEntitiesCounter > 0) {
            this.renderInfos = new ArrayList<>(this.renderInfos.size() + 16);
            this.renderInfosEntities = new ArrayList(this.renderInfosEntities.size() + 16);
            this.renderInfosTileEntities = new ArrayList(this.renderInfosTileEntities.size() + 16);
        } else {
            this.renderInfos.clear();
            this.renderInfosEntities.clear();
            this.renderInfosTileEntities.clear();
        }
    }

    public void onPlayerPositionSet() {
        if (this.firstWorldLoad) {
            this.loadRenderers();
            this.firstWorldLoad = false;
        }
    }

    public void pauseChunkUpdates() {
        if (this.renderDispatcher != null) {
            this.renderDispatcher.pauseChunkUpdates();
        }
    }

    public void resumeChunkUpdates() {
        if (this.renderDispatcher != null) {
            this.renderDispatcher.resumeChunkUpdates();
        }
    }

    public void updateTileEntities(Collection<TileEntity> tileEntitiesToRemove, Collection<TileEntity> tileEntitiesToAdd) {
        synchronized (this.setTileEntities) {
            this.setTileEntities.removeAll(tileEntitiesToRemove);
            this.setTileEntities.addAll(tileEntitiesToAdd);
        }
    }

    public static class ContainerLocalRenderInformation {
        final RenderChunk renderChunk;
        EnumFacing facing;
        int c;

        public ContainerLocalRenderInformation(RenderChunk renderChunkIn, EnumFacing facingIn, int setFacingIn) {
            this.renderChunk = renderChunkIn;
            this.facing = facingIn;
            this.c = setFacingIn;
        }

        public void setFacingBit(byte p_189561_1_, EnumFacing p_189561_2_) {
            this.c = this.c | p_189561_1_ | 1 << p_189561_2_.ordinal();
        }

        public boolean isFacingBit(EnumFacing p_500049_1_) {
            return (this.c & 1 << p_500049_1_.ordinal()) > 0;
        }

        private void initialize(EnumFacing facingIn, int setFacingIn) {
            this.facing = facingIn;
            this.c = setFacingIn;
        }
    }
}
