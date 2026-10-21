package net.minecraft.client.renderer;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MouseFilter;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.GlErrors;
import net.optifine.Lagometer;
import net.optifine.RandomEntities;
import net.optifine.gui.GuiChatOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.reflect.ReflectorResolver;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.TextureUtils;
import net.optifine.util.TimedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;

public class EntityRenderer implements IResourceManagerReloadListener {
    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation locationRainPng = new ResourceLocation("textures/environment/rain.png");
    private static final ResourceLocation locationSnowPng = new ResourceLocation("textures/environment/snow.png");
    public static boolean anaglyphEnable;
    public static int anaglyphField;
    private Minecraft mc;
    private final IResourceManager resourceManager;
    private Random random = new Random();
    private float farPlaneDistance;
    public ItemRenderer itemRenderer;
    private final MapItemRenderer theMapItemRenderer;
    private int rendererUpdateCount;
    private Entity pointedEntity;
    private MouseFilter mouseFilterXAxis = new MouseFilter();
    private MouseFilter mouseFilterYAxis = new MouseFilter();
    private float thirdPersonDistance = 4.0F;
    private float thirdPersonDistanceTemp = 4.0F;
    private float smoothCamYaw;
    private float smoothCamPitch;
    private float smoothCamFilterX;
    private float smoothCamFilterY;
    private float smoothCamPartialTicks;
    private float fovModifierHand;
    private float fovModifierHandPrev;
    private float bossColorModifier;
    private float bossColorModifierPrev;
    private boolean cloudFog;
    private boolean renderHand = true;
    private boolean drawBlockOutline = true;
    private long prevFrameTime = Minecraft.getSystemTime();
    private long renderEndNanoTime;
    private final DynamicTexture lightmapTexture;
    private final int[] lightmapColors;
    private final ResourceLocation locationLightMap;
    private boolean lightmapUpdateNeeded;
    private float torchFlickerX;
    private float torchFlickerDX;
    private int rainSoundCounter;
    private float[] rainXCoords = new float[1024];
    private float[] rainYCoords = new float[1024];
    private FloatBuffer fogColorBuffer = GLAllocation.createDirectFloatBuffer(16);
    public float fogColorRed;
    public float fogColorGreen;
    public float fogColorBlue;
    private float fogColor2;
    private float fogColor1;
    private int debugViewDirection = 0;
    private boolean debugView = false;
    private double cameraZoom = 1.0;
    private double cameraYaw;
    private double cameraPitch;
    private ShaderGroup theShaderGroup;
    private static final ResourceLocation[] shaderResourceLocations = new ResourceLocation[]{
        new ResourceLocation("shaders/post/notch.json"),
        new ResourceLocation("shaders/post/fxaa.json"),
        new ResourceLocation("shaders/post/art.json"),
        new ResourceLocation("shaders/post/bumpy.json"),
        new ResourceLocation("shaders/post/blobs2.json"),
        new ResourceLocation("shaders/post/pencil.json"),
        new ResourceLocation("shaders/post/color_convolve.json"),
        new ResourceLocation("shaders/post/deconverge.json"),
        new ResourceLocation("shaders/post/flip.json"),
        new ResourceLocation("shaders/post/invert.json"),
        new ResourceLocation("shaders/post/ntsc.json"),
        new ResourceLocation("shaders/post/outline.json"),
        new ResourceLocation("shaders/post/phosphor.json"),
        new ResourceLocation("shaders/post/scan_pincushion.json"),
        new ResourceLocation("shaders/post/sobel.json"),
        new ResourceLocation("shaders/post/bits.json"),
        new ResourceLocation("shaders/post/desaturate.json"),
        new ResourceLocation("shaders/post/green.json"),
        new ResourceLocation("shaders/post/blur.json"),
        new ResourceLocation("shaders/post/wobble.json"),
        new ResourceLocation("shaders/post/blobs.json"),
        new ResourceLocation("shaders/post/antialias.json"),
        new ResourceLocation("shaders/post/creeper.json"),
        new ResourceLocation("shaders/post/spider.json")
    };
    public static final int shaderCount = shaderResourceLocations.length;
    private int shaderIndex;
    private boolean useShader;
    public int frameCount;
    private boolean initialized = false;
    private World updatedWorld = null;
    private boolean showDebugInfo = false;
    public boolean fogStandard = false;
    private float clipDistance = 128.0F;
    private long lastServerTime = 0L;
    private int lastServerTicks = 0;
    private int serverWaitTime = 0;
    private int serverWaitTimeCurrent = 0;
    private float avgServerTimeDiff = 0.0F;
    private float avgServerTickDiff = 0.0F;
    private ShaderGroup[] fxaaShaders = new ShaderGroup[10];
    private boolean loadVisibleChunks = false;

    public EntityRenderer(Minecraft mcIn, IResourceManager resourceManagerIn) {
        this.shaderIndex = shaderCount;
        this.useShader = false;
        this.frameCount = 0;
        this.mc = mcIn;
        this.resourceManager = resourceManagerIn;
        this.itemRenderer = mcIn.getItemRenderer();
        this.theMapItemRenderer = new MapItemRenderer(mcIn.getTextureManager());
        this.lightmapTexture = new DynamicTexture(16, 16);
        this.locationLightMap = mcIn.getTextureManager().getDynamicTextureLocation("lightMap", this.lightmapTexture);
        this.lightmapColors = this.lightmapTexture.getTextureData();
        this.theShaderGroup = null;

        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f = j - 16;
                float f1 = i - 16;
                float f2 = MathHelper.sqrt_float(f * f + f1 * f1);
                this.rainXCoords[i << 5 | j] = -f1 / f2;
                this.rainYCoords[i << 5 | j] = f / f2;
            }
        }
    }

    public boolean isShaderActive() {
        return OpenGlHelper.shadersSupported && this.theShaderGroup != null;
    }

    public void stopUseShader() {
        if (this.theShaderGroup != null) {
            this.theShaderGroup.deleteShaderGroup();
        }

        this.theShaderGroup = null;
        this.shaderIndex = shaderCount;
    }

    public void switchUseShader() {
        this.useShader = !this.useShader;
    }

    public void loadEntityShader(Entity entityIn) {
        if (OpenGlHelper.shadersSupported) {
            if (this.theShaderGroup != null) {
                this.theShaderGroup.deleteShaderGroup();
            }

            this.theShaderGroup = null;
            if (entityIn instanceof EntityCreeper) {
                this.loadShader(new ResourceLocation("shaders/post/creeper.json"));
            } else if (entityIn instanceof EntitySpider) {
                this.loadShader(new ResourceLocation("shaders/post/spider.json"));
            } else if (entityIn instanceof EntityEnderman) {
                this.loadShader(new ResourceLocation("shaders/post/invert.json"));
            } else if (Reflector.ForgeHooksClient_loadEntityShader.exists()) {
                Reflector.call(Reflector.ForgeHooksClient_loadEntityShader, entityIn, this);
            }
        }
    }

    public void activateNextShader() {
        if (OpenGlHelper.shadersSupported && this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            if (this.theShaderGroup != null) {
                this.theShaderGroup.deleteShaderGroup();
            }

            this.shaderIndex = (this.shaderIndex + 1) % (shaderResourceLocations.length + 1);
            if (this.shaderIndex != shaderCount) {
                this.loadShader(shaderResourceLocations[this.shaderIndex]);
            } else {
                this.theShaderGroup = null;
            }
        }
    }

    private void loadShader(ResourceLocation resourceLocationIn) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            try {
                this.theShaderGroup = new ShaderGroup(this.mc.getTextureManager(), this.resourceManager, this.mc.getFramebuffer(), resourceLocationIn);
                this.theShaderGroup.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
                this.useShader = true;
            } catch (IOException var3) {
                logger.warn("Failed to load shader: " + resourceLocationIn, var3);
                this.shaderIndex = shaderCount;
                this.useShader = false;
            } catch (JsonSyntaxException var4) {
                logger.warn("Failed to load shader: " + resourceLocationIn, var4);
                this.shaderIndex = shaderCount;
                this.useShader = false;
            }
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        if (this.theShaderGroup != null) {
            this.theShaderGroup.deleteShaderGroup();
        }

        this.theShaderGroup = null;
        if (this.shaderIndex != shaderCount) {
            this.loadShader(shaderResourceLocations[this.shaderIndex]);
        } else {
            this.loadEntityShader(this.mc.getRenderViewEntity());
        }
    }

    public void updateRenderer() {
        if (OpenGlHelper.shadersSupported && ShaderLinkHelper.getStaticShaderLinkHelper() == null) {
            ShaderLinkHelper.setNewStaticShaderLinkHelper();
        }

        this.updateFovModifierHand();
        this.updateTorchFlicker();
        this.fogColor2 = this.fogColor1;
        this.thirdPersonDistanceTemp = this.thirdPersonDistance;
        if (this.mc.gameSettings.smoothCamera) {
            float f = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;
            this.smoothCamFilterX = this.mouseFilterXAxis.smooth(this.smoothCamYaw, 0.05F * f1);
            this.smoothCamFilterY = this.mouseFilterYAxis.smooth(this.smoothCamPitch, 0.05F * f1);
            this.smoothCamPartialTicks = 0.0F;
            this.smoothCamYaw = 0.0F;
            this.smoothCamPitch = 0.0F;
        } else {
            this.smoothCamFilterX = 0.0F;
            this.smoothCamFilterY = 0.0F;
            this.mouseFilterXAxis.reset();
            this.mouseFilterYAxis.reset();
        }

        if (this.mc.getRenderViewEntity() == null) {
            this.mc.setRenderViewEntity(this.mc.thePlayer);
        }

        Entity viewEntity = this.mc.getRenderViewEntity();
        double vx = viewEntity.posX;
        double vy = viewEntity.posY + viewEntity.getEyeHeight();
        double vz = viewEntity.posZ;
        float f3 = this.mc.theWorld.getLightBrightness(new BlockPos(vx, vy, vz));
        float f4 = this.mc.gameSettings.renderDistanceChunks / 16.0F;
        f4 = MathHelper.clamp_float(f4, 0.0F, 1.0F);
        float f2 = f3 * (1.0F - f4) + f4;
        this.fogColor1 = this.fogColor1 + (f2 - this.fogColor1) * 0.1F;
        this.rendererUpdateCount++;
        this.itemRenderer.updateEquippedItem();
        this.addRainParticles();
        this.bossColorModifierPrev = this.bossColorModifier;
        if (BossStatus.hasColorModifier) {
            this.bossColorModifier += 0.05F;
            if (this.bossColorModifier > 1.0F) {
                this.bossColorModifier = 1.0F;
            }

            BossStatus.hasColorModifier = false;
        } else if (this.bossColorModifier > 0.0F) {
            this.bossColorModifier -= 0.0125F;
        }
    }

    public ShaderGroup getShaderGroup() {
        return this.theShaderGroup;
    }

    public void updateShaderGroupSize(int width, int height) {
        if (OpenGlHelper.shadersSupported) {
            if (this.theShaderGroup != null) {
                this.theShaderGroup.createBindFramebuffers(width, height);
            }

            this.mc.renderGlobal.createBindEntityOutlineFbs(width, height);
        }
    }

    public void getMouseOver(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        if (entity != null && this.mc.theWorld != null) {
            this.mc.mcProfiler.startSection("pick");
            this.mc.pointedEntity = null;
            double d0 = this.mc.playerController.getBlockReachDistance();
            this.mc.objectMouseOver = entity.rayTrace(d0, partialTicks);
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(partialTicks);
            boolean flag = false;
            int i = 3;
            if (this.mc.playerController.extendedReach()) {
                d0 = 6.0;
                d1 = 6.0;
            } else if (d0 > 3.0) {
                flag = true;
            }

            if (this.mc.objectMouseOver != null) {
                d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            Vec3 vec31 = entity.getLook(partialTicks);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            this.pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = this.mc
                .theWorld
                .getEntitiesInAABBexcluding(
                    entity,
                    entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f),
                    Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>() {
                        public boolean apply(Entity p_apply_1_) {
                            return p_apply_1_.canBeCollidedWith();
                        }
                    })
                );
            double d2 = d1;

            for (int j = 0; j < list.size(); j++) {
                Entity entity1 = list.get(j);
                float f1 = entity1.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0) {
                        this.pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d3 < d2 || d2 == 0.0) {
                        boolean canRiderInteract = false;
                        if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                            canRiderInteract = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                        }

                        if (canRiderInteract || entity1 != entity.ridingEntity) {
                            this.pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        } else if (d2 == 0.0) {
                            this.pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                        }
                    }
                }
            }

            if (this.pointedEntity != null && flag && vec3.distanceTo(vec33) > 3.0) {
                this.pointedEntity = null;
                this.mc.objectMouseOver = new MovingObjectPosition(MovingObjectType.MISS, vec33, (EnumFacing)null, new BlockPos(vec33));
            }

            if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
                this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec33);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    this.mc.pointedEntity = this.pointedEntity;
                }
            }

            this.mc.mcProfiler.endSection();
        }
    }

    private void updateFovModifierHand() {
        float f = 1.0F;
        if (this.mc.getRenderViewEntity() instanceof AbstractClientPlayer) {
            AbstractClientPlayer abstractclientplayer = (AbstractClientPlayer)this.mc.getRenderViewEntity();
            f = abstractclientplayer.getFovModifier();
        }

        this.fovModifierHandPrev = this.fovModifierHand;
        this.fovModifierHand = this.fovModifierHand + (f - this.fovModifierHand) * 0.5F;
        if (this.fovModifierHand > 1.5F) {
            this.fovModifierHand = 1.5F;
        }

        if (this.fovModifierHand < 0.1F) {
            this.fovModifierHand = 0.1F;
        }
    }

    private float getFOVModifier(float partialTicks, boolean useFOVSetting) {
        if (this.debugView) {
            return 90.0F;
        } else {
            Entity entity = this.mc.getRenderViewEntity();
            float f = 70.0F;
            if (useFOVSetting) {
                f = this.mc.gameSettings.fovSetting;
                if (Config.isDynamicFov()) {
                    f *= this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * partialTicks;
                }
            }

            boolean zoomActive = false;
            if (this.mc.currentScreen == null) {
                zoomActive = GameSettings.isKeyDown(this.mc.gameSettings.ofKeyBindZoom);
            }

            if (zoomActive) {
                if (!Config.zoomMode) {
                    Config.zoomMode = true;
                    Config.zoomSmoothCamera = this.mc.gameSettings.smoothCamera;
                    this.mc.gameSettings.smoothCamera = true;
                    this.mc.renderGlobal.displayListEntitiesDirty = true;
                }

                if (Config.zoomMode) {
                    f /= 4.0F;
                }
            } else if (Config.zoomMode) {
                Config.zoomMode = false;
                this.mc.gameSettings.smoothCamera = Config.zoomSmoothCamera;
                this.mouseFilterXAxis = new MouseFilter();
                this.mouseFilterYAxis = new MouseFilter();
                this.mc.renderGlobal.displayListEntitiesDirty = true;
            }

            if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getHealth() <= 0.0F) {
                float f1 = ((EntityLivingBase)entity).deathTime + partialTicks;
                f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
            }

            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
            if (block.getMaterial() == Material.water) {
                f = f * 60.0F / 70.0F;
            }

            return Reflector.ForgeHooksClient_getFOVModifier.exists()
                ? Reflector.callFloat(Reflector.ForgeHooksClient_getFOVModifier, this, entity, block, partialTicks, f)
                : f;
        }
    }

    private void hurtCameraEffect(float partialTicks) {
        if (this.mc.getRenderViewEntity() instanceof EntityLivingBase) {
            EntityLivingBase entitylivingbase = (EntityLivingBase)this.mc.getRenderViewEntity();
            float f = entitylivingbase.hurtTime - partialTicks;
            if (entitylivingbase.getHealth() <= 0.0F) {
                float f1 = entitylivingbase.deathTime + partialTicks;
                GlStateManager.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
            }

            if (f < 0.0F) {
                return;
            }

            f /= entitylivingbase.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * (float) Math.PI);
            float f2 = entitylivingbase.attackedAtYaw;
            GlStateManager.rotate(-f2, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-f * 14.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f2, 0.0F, 1.0F, 0.0F);
        }
    }

    private void setupViewBobbing(float partialTicks) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer)this.mc.getRenderViewEntity();
            float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
            float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
            float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
            GlStateManager.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
        }
    }

    private void orientCamera(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPlayerSleeping()) {
            f = (float)(f + 1.0);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);
            if (!this.mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                if (Reflector.ForgeHooksClient_orientBedCamera.exists()) {
                    Reflector.callVoid(Reflector.ForgeHooksClient_orientBedCamera, this.mc.theWorld, blockpos, iblockstate, entity);
                } else if (block == Blocks.bed) {
                    int j = iblockstate.getValue(BlockBed.FACING).getHorizontalIndex();
                    GlStateManager.rotate(j * 90, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
            }
        } else if (this.mc.gameSettings.thirdPersonView > 0) {
            double d3 = this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks;
            if (this.mc.gameSettings.debugCamEnable) {
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
            } else {
                float f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;
                if (this.mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = -MathHelper.sin(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI) * d3;
                double d5 = MathHelper.cos(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI) * d3;
                double d6 = -MathHelper.sin(f2 / 180.0F * (float) Math.PI) * d3;

                for (int i = 0; i < 8; i++) {
                    float f3 = (i & 1) * 2 - 1;
                    float f4 = (i >> 1 & 1) * 2 - 1;
                    float f5 = (i >> 2 & 1) * 2 - 1;
                    f3 *= 0.1F;
                    f4 *= 0.1F;
                    f5 *= 0.1F;
                    MovingObjectPosition movingobjectposition = this.mc
                        .theWorld
                        .rayTraceBlocks(new Vec3(d0 + f3, d1 + f4, d2 + f5), new Vec3(d0 - d4 + f3 + f5, d1 - d6 + f4, d2 - d5 + f5));
                    if (movingobjectposition != null) {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));
                        if (d7 < d3) {
                            d3 = d7;
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
                GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        } else {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }

        if (Reflector.EntityViewRenderEvent_CameraSetup_Constructor.exists()) {
            if (!this.mc.gameSettings.debugCamEnable) {
                float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
                float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                float roll = 0.0F;
                if (entity instanceof EntityAnimal) {
                    EntityAnimal entityanimal = (EntityAnimal)entity;
                    yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
                }

                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
                Object event = Reflector.newInstance(
                    Reflector.EntityViewRenderEvent_CameraSetup_Constructor, this, entity, block, partialTicks, yaw, pitch, roll
                );
                Reflector.postForgeBusEvent(event);
                roll = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_CameraSetup_roll, roll);
                pitch = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_CameraSetup_pitch, pitch);
                yaw = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_CameraSetup_yaw, yaw);
                GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            }
        } else if (!this.mc.gameSettings.debugCamEnable) {
            GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);
            if (entity instanceof EntityAnimal) {
                EntityAnimal entityanimal = (EntityAnimal)entity;
                GlStateManager.rotate(
                    entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F,
                    0.0F,
                    1.0F,
                    0.0F
                );
            } else {
                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            }
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
    }

    public void setupCameraTransform(float partialTicks, int pass) {
        this.farPlaneDistance = this.mc.gameSettings.renderDistanceChunks * 16;
        if (Config.isFogFancy()) {
            this.farPlaneDistance *= 0.95F;
        }

        if (Config.isFogFast()) {
            this.farPlaneDistance *= 0.83F;
        }

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f = 0.07F;
        if (this.mc.gameSettings.anaglyph) {
            GlStateManager.translate(-(pass * 2 - 1) * f, 0.0F, 0.0F);
        }

        this.clipDistance = this.farPlaneDistance * 2.0F;
        if (this.clipDistance < 173.0F) {
            this.clipDistance = 173.0F;
        }

        if (this.cameraZoom != 1.0) {
            GlStateManager.translate((float)this.cameraYaw, (float)(-this.cameraPitch), 0.0F);
            GlStateManager.scale(this.cameraZoom, this.cameraZoom, 1.0);
        }

        Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.clipDistance);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        if (this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((pass * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }

        this.hurtCameraEffect(partialTicks);
        if (this.mc.gameSettings.viewBobbing) {
            this.setupViewBobbing(partialTicks);
        }

        float f1 = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * partialTicks;
        if (f1 > 0.0F) {
            int i = 20;
            if (this.mc.thePlayer.isPotionActive(Potion.confusion)) {
                i = 7;
            }

            float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
            f2 *= f2;
            GlStateManager.rotate((this.rendererUpdateCount + partialTicks) * i, 0.0F, 1.0F, 1.0F);
            GlStateManager.scale(1.0F / f2, 1.0F, 1.0F);
            GlStateManager.rotate(-(this.rendererUpdateCount + partialTicks) * i, 0.0F, 1.0F, 1.0F);
        }

        this.orientCamera(partialTicks);
        if (this.debugView) {
            switch (this.debugViewDirection) {
                case 0:
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 1:
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 2:
                    GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                    break;
                case 3:
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 4:
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    private void renderHand(float partialTicks, int xOffset) {
        this.renderHand(partialTicks, xOffset, true, true, false);
    }

    public void renderHand(float partialTicks, int pass, boolean renderItem, boolean renderOverlay, boolean renderTranslucent) {
        if (!this.debugView) {
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            float f = 0.07F;
            if (this.mc.gameSettings.anaglyph) {
                GlStateManager.translate(-(pass * 2 - 1) * f, 0.0F, 0.0F);
            }

            if (Config.isShaders()) {
                Shaders.applyHandDepth();
            }

            Project.gluPerspective(
                this.getFOVModifier(partialTicks, false), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F
            );
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            if (this.mc.gameSettings.anaglyph) {
                GlStateManager.translate((pass * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }

            boolean flag = false;
            if (renderItem) {
                GlStateManager.pushMatrix();
                this.hurtCameraEffect(partialTicks);
                if (this.mc.gameSettings.viewBobbing) {
                    this.setupViewBobbing(partialTicks);
                }

                flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();
                boolean shouldRenderHand = !ReflectorForge.renderFirstPersonHand(this.mc.renderGlobal, partialTicks, pass);
                if (shouldRenderHand
                    && this.mc.gameSettings.thirdPersonView == 0
                    && !flag
                    && !this.mc.gameSettings.hideGUI
                    && !this.mc.playerController.isSpectator()) {
                    this.enableLightmap();
                    if (Config.isShaders()) {
                        ShadersRender.renderItemFP(this.itemRenderer, partialTicks, renderTranslucent);
                    } else {
                        this.itemRenderer.renderItemInFirstPerson(partialTicks);
                    }

                    this.disableLightmap();
                }

                GlStateManager.popMatrix();
            }

            if (!renderOverlay) {
                return;
            }

            this.disableLightmap();
            if (this.mc.gameSettings.thirdPersonView == 0 && !flag) {
                this.itemRenderer.renderOverlays(partialTicks);
                this.hurtCameraEffect(partialTicks);
            }

            if (this.mc.gameSettings.viewBobbing) {
                this.setupViewBobbing(partialTicks);
            }
        }
    }

    public void disableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if (Config.isShaders()) {
            Shaders.disableLightmap();
        }
    }

    public void enableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        float f = 0.00390625F;
        GlStateManager.scale(f, f, f);
        GlStateManager.translate(8.0F, 8.0F, 8.0F);
        GlStateManager.matrixMode(5888);
        this.mc.getTextureManager().bindTexture(this.locationLightMap);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if (Config.isShaders()) {
            Shaders.enableLightmap();
        }
    }

    private void updateTorchFlicker() {
        this.torchFlickerDX = (float)(this.torchFlickerDX + (Math.random() - Math.random()) * Math.random() * Math.random());
        this.torchFlickerDX = (float)(this.torchFlickerDX * 0.9);
        this.torchFlickerX = this.torchFlickerX + (this.torchFlickerDX - this.torchFlickerX) * 1.0F;
        this.lightmapUpdateNeeded = true;
    }

    private void updateLightmap(float partialTicks) {
        if (this.lightmapUpdateNeeded) {
            this.mc.mcProfiler.startSection("lightTex");
            World world = this.mc.theWorld;
            if (world != null) {
                if (Config.isCustomColors()
                    && CustomColors.updateLightmap(
                        world, this.torchFlickerX, this.lightmapColors, this.mc.thePlayer.isPotionActive(Potion.nightVision), partialTicks
                    )) {
                    this.lightmapTexture.updateDynamicTexture();
                    this.lightmapUpdateNeeded = false;
                    this.mc.mcProfiler.endSection();
                    return;
                }

                float f = world.getSunBrightness(1.0F);
                float f1 = f * 0.95F + 0.05F;

                for (int i = 0; i < 256; i++) {
                    float f2 = world.provider.getLightBrightnessTable()[i / 16] * f1;
                    float f3 = world.provider.getLightBrightnessTable()[i % 16] * (this.torchFlickerX * 0.1F + 1.5F);
                    if (world.getLastLightningBolt() > 0) {
                        f2 = world.provider.getLightBrightnessTable()[i / 16];
                    }

                    float f4 = f2 * (f * 0.65F + 0.35F);
                    float f5 = f2 * (f * 0.65F + 0.35F);
                    float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
                    float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
                    float f8 = f4 + f3;
                    float f9 = f5 + f6;
                    float f10 = f2 + f7;
                    f8 = f8 * 0.96F + 0.03F;
                    f9 = f9 * 0.96F + 0.03F;
                    f10 = f10 * 0.96F + 0.03F;
                    if (this.bossColorModifier > 0.0F) {
                        float f11 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
                        f8 = f8 * (1.0F - f11) + f8 * 0.7F * f11;
                        f9 = f9 * (1.0F - f11) + f9 * 0.6F * f11;
                        f10 = f10 * (1.0F - f11) + f10 * 0.6F * f11;
                    }

                    if (world.provider.getDimensionId() == 1) {
                        f8 = 0.22F + f3 * 0.75F;
                        f9 = 0.28F + f6 * 0.75F;
                        f10 = 0.25F + f7 * 0.75F;
                    }

                    if (this.mc.thePlayer.isPotionActive(Potion.nightVision)) {
                        float f15 = this.getNightVisionBrightness(this.mc.thePlayer, partialTicks);
                        float f12 = 1.0F / f8;
                        if (f12 > 1.0F / f9) {
                            f12 = 1.0F / f9;
                        }

                        if (f12 > 1.0F / f10) {
                            f12 = 1.0F / f10;
                        }

                        f8 = f8 * (1.0F - f15) + f8 * f12 * f15;
                        f9 = f9 * (1.0F - f15) + f9 * f12 * f15;
                        f10 = f10 * (1.0F - f15) + f10 * f12 * f15;
                    }

                    if (f8 > 1.0F) {
                        f8 = 1.0F;
                    }

                    if (f9 > 1.0F) {
                        f9 = 1.0F;
                    }

                    if (f10 > 1.0F) {
                        f10 = 1.0F;
                    }

                    float f16 = this.mc.gameSettings.gammaSetting;
                    float f17 = 1.0F - f8;
                    float f13 = 1.0F - f9;
                    float f14 = 1.0F - f10;
                    f17 = 1.0F - f17 * f17 * f17 * f17;
                    f13 = 1.0F - f13 * f13 * f13 * f13;
                    f14 = 1.0F - f14 * f14 * f14 * f14;
                    f8 = f8 * (1.0F - f16) + f17 * f16;
                    f9 = f9 * (1.0F - f16) + f13 * f16;
                    f10 = f10 * (1.0F - f16) + f14 * f16;
                    f8 = f8 * 0.96F + 0.03F;
                    f9 = f9 * 0.96F + 0.03F;
                    f10 = f10 * 0.96F + 0.03F;
                    if (f8 > 1.0F) {
                        f8 = 1.0F;
                    }

                    if (f9 > 1.0F) {
                        f9 = 1.0F;
                    }

                    if (f10 > 1.0F) {
                        f10 = 1.0F;
                    }

                    if (f8 < 0.0F) {
                        f8 = 0.0F;
                    }

                    if (f9 < 0.0F) {
                        f9 = 0.0F;
                    }

                    if (f10 < 0.0F) {
                        f10 = 0.0F;
                    }

                    int j = 255;
                    int k = (int)(f8 * 255.0F);
                    int l = (int)(f9 * 255.0F);
                    int i1 = (int)(f10 * 255.0F);
                    this.lightmapColors[i] = j << 24 | k << 16 | l << 8 | i1;
                }

                this.lightmapTexture.updateDynamicTexture();
                this.lightmapUpdateNeeded = false;
                this.mc.mcProfiler.endSection();
            }
        }
    }

    public float getNightVisionBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks) {
        int i = entitylivingbaseIn.getActivePotionEffect(Potion.nightVision).getDuration();
        return i > 200 ? 1.0F : 0.7F + MathHelper.sin((i - partialTicks) * (float) Math.PI * 0.2F) * 0.3F;
    }

    public void updateCameraAndRender(float partialTicks, long nanoTime) {
        Config.renderPartialTicks = partialTicks;
        this.frameInit();
        boolean flag = Display.isActive();
        if (!flag && this.mc.gameSettings.pauseOnLostFocus && (!this.mc.gameSettings.touchscreen || !Mouse.isButtonDown(1))) {
            if (Minecraft.getSystemTime() - this.prevFrameTime > 500L) {
                this.mc.displayInGameMenu();
            }
        } else {
            this.prevFrameTime = Minecraft.getSystemTime();
        }

        this.mc.mcProfiler.startSection("mouse");
        if (flag && Minecraft.isRunningOnMac && this.mc.inGameHasFocus && !Mouse.isInsideWindow()) {
            Mouse.setGrabbed(false);
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
            Mouse.setGrabbed(true);
        }

        if (this.mc.inGameHasFocus && flag) {
            this.mc.mouseHelper.mouseXYChange();
            float f = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;
            float f2 = this.mc.mouseHelper.deltaX * f1;
            float f3 = this.mc.mouseHelper.deltaY * f1;
            int i = 1;
            if (this.mc.gameSettings.invertMouse) {
                i = -1;
            }

            if (this.mc.gameSettings.smoothCamera) {
                this.smoothCamYaw += f2;
                this.smoothCamPitch += f3;
                float f4 = partialTicks - this.smoothCamPartialTicks;
                this.smoothCamPartialTicks = partialTicks;
                f2 = this.smoothCamFilterX * f4;
                f3 = this.smoothCamFilterY * f4;
                this.mc.thePlayer.setAngles(f2, f3 * i);
            } else {
                this.smoothCamYaw = 0.0F;
                this.smoothCamPitch = 0.0F;
                this.mc.thePlayer.setAngles(f2, f3 * i);
            }
        }

        this.mc.mcProfiler.endSection();
        if (!this.mc.skipRenderWorld) {
            anaglyphEnable = this.mc.gameSettings.anaglyph;
            final ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i1 = scaledresolution.getScaledWidth();
            int j1 = scaledresolution.getScaledHeight();
            final int k1 = Mouse.getX() * i1 / this.mc.displayWidth;
            final int l1 = j1 - Mouse.getY() * j1 / this.mc.displayHeight - 1;
            int i2 = this.mc.gameSettings.limitFramerate;
            if (this.mc.theWorld != null) {
                this.mc.mcProfiler.startSection("level");
                int j = Math.min(Minecraft.getDebugFPS(), i2);
                j = Math.max(j, 60);
                long k = System.nanoTime() - nanoTime;
                long l = Math.max(1000000000 / j / 4 - k, 0L);
                this.renderWorld(partialTicks, System.nanoTime() + l);
                if (OpenGlHelper.shadersSupported) {
                    this.mc.renderGlobal.renderEntityOutlineFramebuffer();
                    if (this.theShaderGroup != null && this.useShader) {
                        GlStateManager.matrixMode(5890);
                        GlStateManager.pushMatrix();
                        GlStateManager.loadIdentity();
                        this.theShaderGroup.loadShaderGroup(partialTicks);
                        GlStateManager.popMatrix();
                    }

                    this.mc.getFramebuffer().bindFramebuffer(true);
                }

                this.renderEndNanoTime = System.nanoTime();
                this.mc.mcProfiler.endStartSection("gui");
                if (!this.mc.gameSettings.hideGUI || this.mc.currentScreen != null) {
                    GlStateManager.alphaFunc(516, 0.1F);
                    this.mc.ingameGUI.renderGameOverlay(partialTicks);
                    if (this.mc.gameSettings.ofShowFps && !this.mc.gameSettings.showDebugInfo) {
                        Config.drawFps();
                    }

                    if (this.mc.gameSettings.showDebugInfo) {
                        Lagometer.showLagometer(scaledresolution);
                    }
                }

                this.mc.mcProfiler.endSection();
            } else {
                GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                this.setupOverlayRendering();
                this.renderEndNanoTime = System.nanoTime();
                TileEntityRendererDispatcher.instance.renderEngine = this.mc.getTextureManager();
                TileEntityRendererDispatcher.instance.fontRenderer = this.mc.fontRendererObj;
            }

            if (this.mc.currentScreen != null) {
                GlStateManager.clear(256);

                try {
                    if (Reflector.ForgeHooksClient_drawScreen.exists()) {
                        Reflector.callVoid(Reflector.ForgeHooksClient_drawScreen, this.mc.currentScreen, k1, l1, partialTicks);
                    } else {
                        this.mc.currentScreen.drawScreen(k1, l1, partialTicks);
                    }
                } catch (Throwable var16) {
                    CrashReport crashreport = CrashReport.makeCrashReport(var16, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
                    crashreportcategory.addCrashSectionCallable("Screen name", new Callable<String>() {
                        public String call() throws Exception {
                            return EntityRenderer.this.mc.currentScreen.getClass().getCanonicalName();
                        }
                    });
                    crashreportcategory.addCrashSectionCallable("Mouse location", new Callable<String>() {
                        public String call() throws Exception {
                            return String.format("Scaled: (%d, %d). Absolute: (%d, %d)", k1, l1, Mouse.getX(), Mouse.getY());
                        }
                    });
                    crashreportcategory.addCrashSectionCallable(
                        "Screen size",
                        new Callable<String>() {
                            public String call() throws Exception {
                                return String.format(
                                    "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d",
                                    scaledresolution.getScaledWidth(),
                                    scaledresolution.getScaledHeight(),
                                    EntityRenderer.this.mc.displayWidth,
                                    EntityRenderer.this.mc.displayHeight,
                                    scaledresolution.getScaleFactor()
                                );
                            }
                        }
                    );
                    throw new ReportedException(crashreport);
                }
            }
        }

        this.frameFinish();
        this.waitForServerThread();
        MemoryMonitor.update();
        Lagometer.updateLagometer();
        if (this.mc.gameSettings.ofProfiler) {
            this.mc.gameSettings.showDebugProfilerChart = true;
        }
    }

    public void renderStreamIndicator(float partialTicks) {
        this.setupOverlayRendering();
        this.mc.ingameGUI.renderStreamIndicator(new ScaledResolution(this.mc));
    }

    private boolean isDrawBlockOutline() {
        if (!this.drawBlockOutline) {
            return false;
        } else {
            Entity entity = this.mc.getRenderViewEntity();
            boolean flag = entity instanceof EntityPlayer && !this.mc.gameSettings.hideGUI;
            if (flag && !((EntityPlayer)entity).capabilities.allowEdit) {
                ItemStack itemstack = ((EntityPlayer)entity).getCurrentEquippedItem();
                if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                    BlockPos blockpos = this.mc.objectMouseOver.getBlockPos();
                    IBlockState state = this.mc.theWorld.getBlockState(blockpos);
                    Block block = state.getBlock();
                    if (this.mc.playerController.getCurrentGameType() == GameType.SPECTATOR) {
                        flag = ReflectorForge.blockHasTileEntity(state) && this.mc.theWorld.getTileEntity(blockpos) instanceof IInventory;
                    } else {
                        flag = itemstack != null && (itemstack.canDestroy(block) || itemstack.canPlaceOn(block));
                    }
                }
            }

            return flag;
        }
    }

    private void renderWorldDirections(float partialTicks) {
        if (this.mc.gameSettings.showDebugInfo
            && !this.mc.gameSettings.hideGUI
            && !this.mc.thePlayer.hasReducedDebug()
            && !this.mc.gameSettings.reducedDebugInfo) {
            Entity entity = this.mc.getRenderViewEntity();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GL11.glLineWidth(1.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            this.orientCamera(partialTicks);
            GlStateManager.translate(0.0F, entity.getEyeHeight(), 0.0F);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0, 0.0, 0.0, 0.005, 1.0E-4, 1.0E-4), 255, 0, 0, 255);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0, 0.0, 0.0, 1.0E-4, 1.0E-4, 0.005), 0, 0, 255, 255);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0, 0.0, 0.0, 1.0E-4, 0.0033, 1.0E-4), 0, 255, 0, 255);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        this.updateLightmap(partialTicks);
        if (this.mc.getRenderViewEntity() == null) {
            this.mc.setRenderViewEntity(this.mc.thePlayer);
        }

        this.getMouseOver(partialTicks);
        if (Config.isShaders()) {
            Shaders.beginRender(this.mc, partialTicks, finishTimeNano);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        this.mc.mcProfiler.startSection("center");
        if (this.mc.gameSettings.anaglyph) {
            anaglyphField = 0;
            GlStateManager.colorMask(false, true, true, false);
            this.renderWorldPass(0, partialTicks, finishTimeNano);
            anaglyphField = 1;
            GlStateManager.colorMask(true, false, false, false);
            this.renderWorldPass(1, partialTicks, finishTimeNano);
            GlStateManager.colorMask(true, true, true, false);
        } else {
            this.renderWorldPass(2, partialTicks, finishTimeNano);
        }

        this.mc.mcProfiler.endSection();
    }

    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
        boolean isShaders = Config.isShaders();
        if (isShaders) {
            Shaders.beginRenderPass(pass, partialTicks, finishTimeNano);
        }

        RenderGlobal renderglobal = this.mc.renderGlobal;
        EffectRenderer effectrenderer = this.mc.effectRenderer;
        boolean flag = this.isDrawBlockOutline();
        GlStateManager.enableCull();
        this.mc.mcProfiler.endStartSection("clear");
        if (isShaders) {
            Shaders.setViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        } else {
            GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        }

        this.updateFogColor(partialTicks);
        GlStateManager.clear(16640);
        if (isShaders) {
            Shaders.clearRenderBuffer();
        }

        this.mc.mcProfiler.endStartSection("camera");
        this.setupCameraTransform(partialTicks, pass);
        if (isShaders) {
            Shaders.setCamera(partialTicks);
        }

        ActiveRenderInfo.updateRenderInfo(this.mc.thePlayer, this.mc.gameSettings.thirdPersonView == 2);
        this.mc.mcProfiler.endStartSection("frustum");
        ClippingHelper clippingHelper = ClippingHelperImpl.getInstance();
        this.mc.mcProfiler.endStartSection("culling");
        clippingHelper.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
        ICamera icamera = new Frustum(clippingHelper);
        Entity entity = this.mc.getRenderViewEntity();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        if (isShaders) {
            ShadersRender.setFrustrumPosition(icamera, d0, d1, d2);
        } else {
            icamera.setPosition(d0, d1, d2);
        }

        if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
            this.setupFog(-1, partialTicks);
            this.mc.mcProfiler.endStartSection("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.clipDistance);
            GlStateManager.matrixMode(5888);
            if (isShaders) {
                Shaders.beginSky();
            }

            renderglobal.renderSky(partialTicks, pass);
            if (isShaders) {
                Shaders.endSky();
            }

            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.clipDistance);
            GlStateManager.matrixMode(5888);
        } else {
            GlStateManager.disableBlend();
        }

        this.setupFog(0, partialTicks);
        GlStateManager.shadeModel(7425);
        if (entity.posY + entity.getEyeHeight() < 128.0 + this.mc.gameSettings.ofCloudsHeight * 128.0F) {
            this.renderCloudsCheck(renderglobal, partialTicks, pass);
        }

        this.mc.mcProfiler.endStartSection("prepareterrain");
        this.setupFog(0, partialTicks);
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        this.mc.mcProfiler.endStartSection("terrain_setup");
        this.checkLoadVisibleChunks(entity, partialTicks, icamera, this.mc.thePlayer.isSpectator());
        if (isShaders) {
            ShadersRender.setupTerrain(renderglobal, entity, partialTicks, icamera, this.frameCount++, this.mc.thePlayer.isSpectator());
        } else {
            renderglobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.thePlayer.isSpectator());
        }

        if (pass == 0 || pass == 2) {
            this.mc.mcProfiler.endStartSection("updatechunks");
            Lagometer.timerChunkUpload.start();
            this.mc.renderGlobal.updateChunks(finishTimeNano);
            Lagometer.timerChunkUpload.end();
        }

        this.mc.mcProfiler.endStartSection("terrain");
        Lagometer.timerTerrain.start();
        if (this.mc.gameSettings.ofSmoothFps && pass > 0) {
            this.mc.mcProfiler.endStartSection("finish");
            GL11.glFinish();
            this.mc.mcProfiler.endStartSection("terrain");
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();
        if (isShaders) {
            ShadersRender.beginTerrainSolid();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.SOLID, partialTicks, pass, entity);
        GlStateManager.enableAlpha();
        if (isShaders) {
            ShadersRender.beginTerrainCutoutMipped();
        }

        this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, this.mc.gameSettings.mipmapLevels > 0);
        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, partialTicks, pass, entity);
        this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        if (isShaders) {
            ShadersRender.beginTerrainCutout();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT, partialTicks, pass, entity);
        this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        if (isShaders) {
            ShadersRender.endTerrain();
        }

        Lagometer.timerTerrain.end();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);
        if (!this.debugView) {
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            RenderHelper.enableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("entities");
            if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 0);
            }

            renderglobal.renderEntities(entity, icamera, partialTicks);
            if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
            }

            RenderHelper.disableStandardItemLighting();
            this.disableLightmap();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            if (this.mc.objectMouseOver != null && entity.isInsideOfMaterial(Material.water) && flag) {
                EntityPlayer entityplayer = (EntityPlayer)entity;
                GlStateManager.disableAlpha();
                this.mc.mcProfiler.endStartSection("outline");
                renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);
                GlStateManager.enableAlpha();
            }
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        if (flag && this.mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.water)) {
            EntityPlayer entityplayer1 = (EntityPlayer)entity;
            GlStateManager.disableAlpha();
            this.mc.mcProfiler.endStartSection("outline");
            if ((
                    !Reflector.ForgeHooksClient_onDrawBlockHighlight.exists()
                        || !Reflector.callBoolean(
                            Reflector.ForgeHooksClient_onDrawBlockHighlight,
                            renderglobal,
                            entityplayer1,
                            this.mc.objectMouseOver,
                            0,
                            entityplayer1.getHeldItem(),
                            partialTicks
                        )
                )
                && !this.mc.gameSettings.hideGUI) {
                renderglobal.drawSelectionBox(entityplayer1, this.mc.objectMouseOver, 0, partialTicks);
            }

            GlStateManager.enableAlpha();
        }

        if (!renderglobal.damagedBlocks.isEmpty()) {
            this.mc.mcProfiler.endStartSection("destroyProgress");
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getWorldRenderer(), entity, partialTicks);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            GlStateManager.disableBlend();
        }

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableBlend();
        if (!this.debugView) {
            this.enableLightmap();
            this.mc.mcProfiler.endStartSection("litParticles");
            if (isShaders) {
                Shaders.beginLitParticles();
            }

            effectrenderer.renderLitParticles(entity, partialTicks);
            RenderHelper.disableStandardItemLighting();
            this.setupFog(0, partialTicks);
            this.mc.mcProfiler.endStartSection("particles");
            if (isShaders) {
                Shaders.beginParticles();
            }

            effectrenderer.renderParticles(entity, partialTicks);
            if (isShaders) {
                Shaders.endParticles();
            }

            this.disableLightmap();
        }

        GlStateManager.depthMask(false);
        if (Config.isShaders()) {
            GlStateManager.depthMask(Shaders.isRainDepth());
        }

        GlStateManager.enableCull();
        this.mc.mcProfiler.endStartSection("weather");
        if (isShaders) {
            Shaders.beginWeather();
        }

        this.renderRainSnow(partialTicks);
        if (isShaders) {
            Shaders.endWeather();
        }

        GlStateManager.depthMask(true);
        renderglobal.renderWorldBorder(entity, partialTicks);
        if (isShaders) {
            ShadersRender.renderHand0(this, partialTicks, pass);
            Shaders.preWater();
        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.alphaFunc(516, 0.1F);
        this.setupFog(0, partialTicks);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.shadeModel(7425);
        this.mc.mcProfiler.endStartSection("translucent");
        if (isShaders) {
            Shaders.beginWater();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partialTicks, pass, entity);
        if (isShaders) {
            Shaders.endWater();
        }

        if (Reflector.ForgeHooksClient_setRenderPass.exists() && !this.debugView) {
            RenderHelper.enableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("entities");
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 1);
            this.mc.renderGlobal.renderEntities(entity, icamera, partialTicks);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        if (entity.posY + entity.getEyeHeight() >= 128.0 + this.mc.gameSettings.ofCloudsHeight * 128.0F) {
            this.mc.mcProfiler.endStartSection("aboveClouds");
            this.renderCloudsCheck(renderglobal, partialTicks, pass);
        }

        if (Reflector.ForgeHooksClient_dispatchRenderLast.exists()) {
            this.mc.mcProfiler.endStartSection("forge_render_last");
            Reflector.callVoid(Reflector.ForgeHooksClient_dispatchRenderLast, renderglobal, partialTicks);
        }

        this.mc.mcProfiler.endStartSection("hand");
        if (this.renderHand && !Shaders.isShadowPass) {
            if (isShaders) {
                ShadersRender.renderHand1(this, partialTicks, pass);
                Shaders.renderCompositeFinal();
            }

            GlStateManager.clear(256);
            if (isShaders) {
                ShadersRender.renderFPOverlay(this, partialTicks, pass);
            } else {
                this.renderHand(partialTicks, pass);
            }

            this.renderWorldDirections(partialTicks);
        }

        if (isShaders) {
            Shaders.endRender();
        }
    }

    private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass) {
        if (this.mc.gameSettings.renderDistanceChunks >= 4 && !Config.isCloudsOff() && Shaders.shouldRenderClouds(this.mc.gameSettings)) {
            this.mc.mcProfiler.endStartSection("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(
                this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.clipDistance * 4.0F
            );
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.setupFog(0, partialTicks);
            renderGlobalIn.renderClouds(partialTicks, pass);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / this.mc.displayHeight, 0.05F, this.clipDistance);
            GlStateManager.matrixMode(5888);
        }
    }

    private void addRainParticles() {
        float f = this.mc.theWorld.getRainStrength(1.0F);
        if (!Config.isRainFancy()) {
            f /= 2.0F;
        }

        if (f != 0.0F && Config.isRainSplash()) {
            this.random.setSeed(this.rendererUpdateCount * 312987231L);
            Entity entity = this.mc.getRenderViewEntity();
            World world = this.mc.theWorld;
            BlockPos blockpos = new BlockPos(entity);
            int i = 10;
            double d0 = 0.0;
            double d1 = 0.0;
            double d2 = 0.0;
            int j = 0;
            int k = (int)(100.0F * f * f);
            if (this.mc.gameSettings.particleSetting == 1) {
                k >>= 1;
            } else if (this.mc.gameSettings.particleSetting == 2) {
                k = 0;
            }

            for (int l = 0; l < k; l++) {
                BlockPos blockpos1 = world.getPrecipitationHeight(
                    blockpos.add(this.random.nextInt(i) - this.random.nextInt(i), 0, this.random.nextInt(i) - this.random.nextInt(i))
                );
                BiomeGenBase biomegenbase = world.getBiomeGenForCoords(blockpos1);
                BlockPos blockpos2 = blockpos1.down();
                Block block = world.getBlockState(blockpos2).getBlock();
                if (blockpos1.getY() <= blockpos.getY() + i
                    && blockpos1.getY() >= blockpos.getY() - i
                    && biomegenbase.canRain()
                    && biomegenbase.getFloatTemperature(blockpos1) >= 0.15F) {
                    double d3 = this.random.nextDouble();
                    double d4 = this.random.nextDouble();
                    if (block.getMaterial() == Material.lava) {
                        this.mc
                            .theWorld
                            .spawnParticle(
                                EnumParticleTypes.SMOKE_NORMAL,
                                blockpos1.getX() + d3,
                                blockpos1.getY() + 0.1F - block.getBlockBoundsMinY(),
                                blockpos1.getZ() + d4,
                                0.0,
                                0.0,
                                0.0,
                                new int[0]
                            );
                    } else if (block.getMaterial() != Material.air) {
                        block.setBlockBoundsBasedOnState(world, blockpos2);
                        if (this.random.nextInt(++j) == 0) {
                            d0 = blockpos2.getX() + d3;
                            d1 = blockpos2.getY() + 0.1F + block.getBlockBoundsMaxY() - 1.0;
                            d2 = blockpos2.getZ() + d4;
                        }

                        this.mc
                            .theWorld
                            .spawnParticle(
                                EnumParticleTypes.WATER_DROP,
                                blockpos2.getX() + d3,
                                blockpos2.getY() + 0.1F + block.getBlockBoundsMaxY(),
                                blockpos2.getZ() + d4,
                                0.0,
                                0.0,
                                0.0,
                                new int[0]
                            );
                    }
                }
            }

            if (j > 0 && this.random.nextInt(3) < this.rainSoundCounter++) {
                this.rainSoundCounter = 0;
                if (d1 > blockpos.getY() + 1 && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor_float(blockpos.getY())) {
                    this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.1F, 0.5F, false);
                } else {
                    this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.2F, 1.0F, false);
                }
            }
        }
    }

    protected void renderRainSnow(float partialTicks) {
        if (Reflector.ForgeWorldProvider_getWeatherRenderer.exists()) {
            WorldProvider worldProvider = this.mc.theWorld.provider;
            Object weatherRenderer = Reflector.call(worldProvider, Reflector.ForgeWorldProvider_getWeatherRenderer);
            if (weatherRenderer != null) {
                Reflector.callVoid(weatherRenderer, Reflector.IRenderHandler_render, partialTicks, this.mc.theWorld, this.mc);
                return;
            }
        }

        float f = this.mc.theWorld.getRainStrength(partialTicks);
        if (f > 0.0F) {
            if (Config.isRainOff()) {
                return;
            }

            this.enableLightmap();
            Entity entity = this.mc.getRenderViewEntity();
            World world = this.mc.theWorld;
            int i = MathHelper.floor_double(entity.posX);
            int j = MathHelper.floor_double(entity.posY);
            int k = MathHelper.floor_double(entity.posZ);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
            GlStateManager.disableCull();
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.alphaFunc(516, 0.1F);
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
            int l = MathHelper.floor_double(d1);
            int i1 = 5;
            if (Config.isRainFancy()) {
                i1 = 10;
            }

            int j1 = -1;
            float f1 = this.rendererUpdateCount + partialTicks;
            vertexbuffer.setTranslation(-d0, -d1, -d2);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            MutableBlockPos blockpos$mutableblockpos = new MutableBlockPos();

            for (int k1 = k - i1; k1 <= k + i1; k1++) {
                for (int l1 = i - i1; l1 <= i + i1; l1++) {
                    int i2 = (k1 - k + 16) * 32 + l1 - i + 16;
                    double d3 = this.rainXCoords[i2] * 0.5;
                    double d4 = this.rainYCoords[i2] * 0.5;
                    blockpos$mutableblockpos.set(l1, 0, k1);
                    BiomeGenBase biomegenbase = world.getBiomeGenForCoords(blockpos$mutableblockpos);
                    if (biomegenbase.canRain() || biomegenbase.getEnableSnow()) {
                        int j2 = world.getPrecipitationHeight(blockpos$mutableblockpos).getY();
                        int k2 = j - i1;
                        int l2 = j + i1;
                        if (k2 < j2) {
                            k2 = j2;
                        }

                        if (l2 < j2) {
                            l2 = j2;
                        }

                        int i3 = j2;
                        if (j2 < l) {
                            i3 = l;
                        }

                        if (k2 != l2) {
                            this.random.setSeed(l1 * l1 * 3121 + l1 * 45238971 ^ k1 * k1 * 418711 + k1 * 13761);
                            blockpos$mutableblockpos.set(l1, k2, k1);
                            float f2 = biomegenbase.getFloatTemperature(blockpos$mutableblockpos);
                            if (world.getWorldChunkManager().getTemperatureAtHeight(f2, j2) >= 0.15F) {
                                if (j1 != 0) {
                                    if (j1 >= 0) {
                                        tessellator.draw();
                                    }

                                    j1 = 0;
                                    this.mc.getTextureManager().bindTexture(locationRainPng);
                                    vertexbuffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d5 = (
                                        (double)(this.rendererUpdateCount + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + partialTicks
                                    )
                                    / 32.0
                                    * (3.0 + this.random.nextDouble());
                                double d6 = l1 + 0.5F - entity.posX;
                                double d7 = k1 + 0.5F - entity.posZ;
                                float f3 = MathHelper.sqrt_double(d6 * d6 + d7 * d7) / i1;
                                float f4 = ((1.0F - f3 * f3) * 0.5F + 0.5F) * f;
                                blockpos$mutableblockpos.set(l1, i3, k1);
                                int j3 = world.getCombinedLight(blockpos$mutableblockpos, 0);
                                int k3 = j3 >> 16 & 65535;
                                int l3 = j3 & 65535;
                                vertexbuffer.pos(l1 - d3 + 0.5, k2, k1 - d4 + 0.5)
                                    .tex(0.0, k2 * 0.25 + d5)
                                    .color(1.0F, 1.0F, 1.0F, f4)
                                    .lightmap(k3, l3)
                                    .endVertex();
                                vertexbuffer.pos(l1 + d3 + 0.5, k2, k1 + d4 + 0.5)
                                    .tex(1.0, k2 * 0.25 + d5)
                                    .color(1.0F, 1.0F, 1.0F, f4)
                                    .lightmap(k3, l3)
                                    .endVertex();
                                vertexbuffer.pos(l1 + d3 + 0.5, l2, k1 + d4 + 0.5)
                                    .tex(1.0, l2 * 0.25 + d5)
                                    .color(1.0F, 1.0F, 1.0F, f4)
                                    .lightmap(k3, l3)
                                    .endVertex();
                                vertexbuffer.pos(l1 - d3 + 0.5, l2, k1 - d4 + 0.5)
                                    .tex(0.0, l2 * 0.25 + d5)
                                    .color(1.0F, 1.0F, 1.0F, f4)
                                    .lightmap(k3, l3)
                                    .endVertex();
                            } else {
                                if (j1 != 1) {
                                    if (j1 >= 0) {
                                        tessellator.draw();
                                    }

                                    j1 = 1;
                                    this.mc.getTextureManager().bindTexture(locationSnowPng);
                                    vertexbuffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d8 = ((this.rendererUpdateCount & 511) + partialTicks) / 512.0F;
                                double d9 = this.random.nextDouble() + f1 * 0.01 * (float)this.random.nextGaussian();
                                double d10 = this.random.nextDouble() + f1 * (float)this.random.nextGaussian() * 0.001;
                                double d11 = l1 + 0.5F - entity.posX;
                                double d12 = k1 + 0.5F - entity.posZ;
                                float f6 = MathHelper.sqrt_double(d11 * d11 + d12 * d12) / i1;
                                float f5 = ((1.0F - f6 * f6) * 0.3F + 0.5F) * f;
                                blockpos$mutableblockpos.set(l1, i3, k1);
                                int i4 = (world.getCombinedLight(blockpos$mutableblockpos, 0) * 3 + 15728880) / 4;
                                int j4 = i4 >> 16 & 65535;
                                int k4 = i4 & 65535;
                                vertexbuffer.pos(l1 - d3 + 0.5, k2, k1 - d4 + 0.5)
                                    .tex(0.0 + d9, k2 * 0.25 + d8 + d10)
                                    .color(1.0F, 1.0F, 1.0F, f5)
                                    .lightmap(j4, k4)
                                    .endVertex();
                                vertexbuffer.pos(l1 + d3 + 0.5, k2, k1 + d4 + 0.5)
                                    .tex(1.0 + d9, k2 * 0.25 + d8 + d10)
                                    .color(1.0F, 1.0F, 1.0F, f5)
                                    .lightmap(j4, k4)
                                    .endVertex();
                                vertexbuffer.pos(l1 + d3 + 0.5, l2, k1 + d4 + 0.5)
                                    .tex(1.0 + d9, l2 * 0.25 + d8 + d10)
                                    .color(1.0F, 1.0F, 1.0F, f5)
                                    .lightmap(j4, k4)
                                    .endVertex();
                                vertexbuffer.pos(l1 - d3 + 0.5, l2, k1 - d4 + 0.5)
                                    .tex(0.0 + d9, l2 * 0.25 + d8 + d10)
                                    .color(1.0F, 1.0F, 1.0F, f5)
                                    .lightmap(j4, k4)
                                    .endVertex();
                            }
                        }
                    }
                }
            }

            if (j1 >= 0) {
                tessellator.draw();
            }

            vertexbuffer.setTranslation(0.0, 0.0, 0.0);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            this.disableLightmap();
        }
    }

    public void setupOverlayRendering() {
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        GlStateManager.clear(256);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0, 1000.0, 3000.0);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
    }

    private void updateFogColor(float partialTicks) {
        World world = this.mc.theWorld;
        Entity entity = this.mc.getRenderViewEntity();
        float f = 0.25F + 0.75F * this.mc.gameSettings.renderDistanceChunks / 32.0F;
        f = 1.0F - (float)Math.pow(f, 0.25);
        Vec3 vec3 = world.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
        vec3 = CustomColors.getWorldSkyColor(vec3, world, this.mc.getRenderViewEntity(), partialTicks);
        float f1 = (float)vec3.xCoord;
        float f2 = (float)vec3.yCoord;
        float f3 = (float)vec3.zCoord;
        Vec3 vec31 = world.getFogColor(partialTicks);
        vec31 = CustomColors.getWorldFogColor(vec31, world, this.mc.getRenderViewEntity(), partialTicks);
        this.fogColorRed = (float)vec31.xCoord;
        this.fogColorGreen = (float)vec31.yCoord;
        this.fogColorBlue = (float)vec31.zCoord;
        if (this.mc.gameSettings.renderDistanceChunks >= 4) {
            double d0 = -1.0;
            Vec3 vec32 = MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) > 0.0F ? new Vec3(d0, 0.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            float f5 = (float)entity.getLook(partialTicks).dotProduct(vec32);
            if (f5 < 0.0F) {
                f5 = 0.0F;
            }

            if (f5 > 0.0F) {
                float[] afloat = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);
                if (afloat != null) {
                    f5 *= afloat[3];
                    this.fogColorRed = this.fogColorRed * (1.0F - f5) + afloat[0] * f5;
                    this.fogColorGreen = this.fogColorGreen * (1.0F - f5) + afloat[1] * f5;
                    this.fogColorBlue = this.fogColorBlue * (1.0F - f5) + afloat[2] * f5;
                }
            }
        }

        this.fogColorRed = this.fogColorRed + (f1 - this.fogColorRed) * f;
        this.fogColorGreen = this.fogColorGreen + (f2 - this.fogColorGreen) * f;
        this.fogColorBlue = this.fogColorBlue + (f3 - this.fogColorBlue) * f;
        float f8 = world.getRainStrength(partialTicks);
        if (f8 > 0.0F) {
            float f4 = 1.0F - f8 * 0.5F;
            float f10 = 1.0F - f8 * 0.4F;
            this.fogColorRed *= f4;
            this.fogColorGreen *= f4;
            this.fogColorBlue *= f10;
        }

        float f9 = world.getThunderStrength(partialTicks);
        if (f9 > 0.0F) {
            float f11 = 1.0F - f9 * 0.5F;
            this.fogColorRed *= f11;
            this.fogColorGreen *= f11;
            this.fogColorBlue *= f11;
        }

        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
        if (this.cloudFog) {
            Vec3 vec33 = world.getCloudColour(partialTicks);
            this.fogColorRed = (float)vec33.xCoord;
            this.fogColorGreen = (float)vec33.yCoord;
            this.fogColorBlue = (float)vec33.zCoord;
        } else if (block.getMaterial() == Material.water) {
            float f12 = EnchantmentHelper.getRespiration(entity) * 0.2F;
            f12 = Config.limit(f12, 0.0F, 0.6F);
            if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(Potion.waterBreathing)) {
                f12 = f12 * 0.3F + 0.6F;
            }

            this.fogColorRed = 0.02F + f12;
            this.fogColorGreen = 0.02F + f12;
            this.fogColorBlue = 0.2F + f12;
            Vec3 colUnderwater = CustomColors.getUnderwaterColor(
                this.mc.theWorld, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0, this.mc.getRenderViewEntity().posZ
            );
            if (colUnderwater != null) {
                this.fogColorRed = (float)colUnderwater.xCoord;
                this.fogColorGreen = (float)colUnderwater.yCoord;
                this.fogColorBlue = (float)colUnderwater.zCoord;
            }
        } else if (block.getMaterial() == Material.lava) {
            this.fogColorRed = 0.6F;
            this.fogColorGreen = 0.1F;
            this.fogColorBlue = 0.0F;
            Vec3 colUnderlava = CustomColors.getUnderlavaColor(
                this.mc.theWorld, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0, this.mc.getRenderViewEntity().posZ
            );
            if (colUnderlava != null) {
                this.fogColorRed = (float)colUnderlava.xCoord;
                this.fogColorGreen = (float)colUnderlava.yCoord;
                this.fogColorBlue = (float)colUnderlava.zCoord;
            }
        }

        float f13 = this.fogColor2 + (this.fogColor1 - this.fogColor2) * partialTicks;
        this.fogColorRed *= f13;
        this.fogColorGreen *= f13;
        this.fogColorBlue *= f13;
        double d1 = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks) * world.provider.getVoidFogYFactor();
        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(Potion.blindness)) {
            int i = ((EntityLivingBase)entity).getActivePotionEffect(Potion.blindness).getDuration();
            if (i < 20) {
                d1 *= 1.0F - i / 20.0F;
            } else {
                d1 = 0.0;
            }
        }

        if (d1 < 1.0) {
            if (d1 < 0.0) {
                d1 = 0.0;
            }

            d1 *= d1;
            this.fogColorRed = (float)(this.fogColorRed * d1);
            this.fogColorGreen = (float)(this.fogColorGreen * d1);
            this.fogColorBlue = (float)(this.fogColorBlue * d1);
        }

        if (this.bossColorModifier > 0.0F) {
            float f14 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
            this.fogColorRed = this.fogColorRed * (1.0F - f14) + this.fogColorRed * 0.7F * f14;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f14) + this.fogColorGreen * 0.6F * f14;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f14) + this.fogColorBlue * 0.6F * f14;
        }

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(Potion.nightVision)) {
            float f15 = this.getNightVisionBrightness((EntityLivingBase)entity, partialTicks);
            float f6 = 1.0F / this.fogColorRed;
            if (f6 > 1.0F / this.fogColorGreen) {
                f6 = 1.0F / this.fogColorGreen;
            }

            if (f6 > 1.0F / this.fogColorBlue) {
                f6 = 1.0F / this.fogColorBlue;
            }

            if (Float.isInfinite(f6)) {
                f6 = Math.nextAfter(f6, 0.0);
            }

            this.fogColorRed = this.fogColorRed * (1.0F - f15) + this.fogColorRed * f6 * f15;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f15) + this.fogColorGreen * f6 * f15;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f15) + this.fogColorBlue * f6 * f15;
        }

        if (this.mc.gameSettings.anaglyph) {
            float f16 = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
            float f17 = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
            float f7 = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
            this.fogColorRed = f16;
            this.fogColorGreen = f17;
            this.fogColorBlue = f7;
        }

        if (Reflector.EntityViewRenderEvent_FogColors_Constructor.exists()) {
            Object event = Reflector.newInstance(
                Reflector.EntityViewRenderEvent_FogColors_Constructor,
                this,
                entity,
                block,
                partialTicks,
                this.fogColorRed,
                this.fogColorGreen,
                this.fogColorBlue
            );
            Reflector.postForgeBusEvent(event);
            this.fogColorRed = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_FogColors_red, this.fogColorRed);
            this.fogColorGreen = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_FogColors_green, this.fogColorGreen);
            this.fogColorBlue = Reflector.getFieldValueFloat(event, Reflector.EntityViewRenderEvent_FogColors_blue, this.fogColorBlue);
        }

        Shaders.setClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
    }

    private void setupFog(int startCoords, float partialTicks) {
        this.fogStandard = false;
        Entity entity = this.mc.getRenderViewEntity();
        boolean flag = false;
        if (entity instanceof EntityPlayer) {
            flag = ((EntityPlayer)entity).capabilities.isCreativeMode;
        }

        GL11.glFog(2918, this.setFogColorBuffer(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 1.0F));
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
        float forgeFogDensity = -1.0F;
        if (Reflector.ForgeHooksClient_getFogDensity.exists()) {
            forgeFogDensity = Reflector.callFloat(Reflector.ForgeHooksClient_getFogDensity, this, entity, block, partialTicks, 0.1F);
        }

        if (forgeFogDensity >= 0.0F) {
            GlStateManager.setFogDensity(forgeFogDensity);
        } else if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(Potion.blindness)) {
            float f1 = 5.0F;
            int i = ((EntityLivingBase)entity).getActivePotionEffect(Potion.blindness).getDuration();
            if (i < 20) {
                f1 = 5.0F + (this.farPlaneDistance - 5.0F) * (1.0F - i / 20.0F);
            }

            GlStateManager.setFog(9729);
            if (startCoords == -1) {
                GlStateManager.setFogStart(0.0F);
                GlStateManager.setFogEnd(f1 * 0.8F);
            } else {
                GlStateManager.setFogStart(f1 * 0.25F);
                GlStateManager.setFogEnd(f1);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance && Config.isFogFancy()) {
                GL11.glFogi(34138, 34139);
            }
        } else if (this.cloudFog) {
            GlStateManager.setFog(2048);
            GlStateManager.setFogDensity(0.1F);
        } else if (block.getMaterial() == Material.water) {
            GlStateManager.setFog(2048);
            float waterFogDensityMax = Config.isClearWater() ? 0.02F : 0.1F;
            if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(Potion.waterBreathing)) {
                GlStateManager.setFogDensity(0.01F);
            } else {
                float waterFogDensity = 0.1F - EnchantmentHelper.getRespiration(entity) * 0.03F;
                GlStateManager.setFogDensity(Config.limit(waterFogDensity, 0.0F, waterFogDensityMax));
            }
        } else if (block.getMaterial() == Material.lava) {
            GlStateManager.setFog(2048);
            GlStateManager.setFogDensity(2.0F);
        } else {
            float f = this.farPlaneDistance;
            this.fogStandard = true;
            GlStateManager.setFog(9729);
            if (startCoords == -1) {
                GlStateManager.setFogStart(0.0F);
                GlStateManager.setFogEnd(f);
            } else {
                GlStateManager.setFogStart(f * Config.getFogStart());
                GlStateManager.setFogEnd(f);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                if (Config.isFogFancy()) {
                    GL11.glFogi(34138, 34139);
                }

                if (Config.isFogFast()) {
                    GL11.glFogi(34138, 34140);
                }
            }

            if (this.mc.theWorld.provider.doesXZShowFog((int)entity.posX, (int)entity.posZ)) {
                GlStateManager.setFogStart(f * 0.05F);
                GlStateManager.setFogEnd(f);
            }

            if (Reflector.ForgeHooksClient_onFogRender.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_onFogRender, this, entity, block, partialTicks, startCoords, f);
            }
        }

        GlStateManager.enableColorMaterial();
        GlStateManager.enableFog();
        GlStateManager.colorMaterial(1028, 4608);
    }

    private FloatBuffer setFogColorBuffer(float red, float green, float blue, float alpha) {
        if (Config.isShaders()) {
            Shaders.setFogColor(red, green, blue);
        }

        ((Buffer)this.fogColorBuffer).clear();
        this.fogColorBuffer.put(red).put(green).put(blue).put(alpha);
        ((Buffer)this.fogColorBuffer).flip();
        return this.fogColorBuffer;
    }

    public MapItemRenderer getMapItemRenderer() {
        return this.theMapItemRenderer;
    }

    private void waitForServerThread() {
        this.serverWaitTimeCurrent = 0;
        if (!Config.isSmoothWorld() || !Config.isSingleProcessor()) {
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
        } else if (this.mc.isIntegratedServerRunning()) {
            IntegratedServer srv = this.mc.getIntegratedServer();
            if (srv != null) {
                boolean paused = this.mc.isGamePaused();
                if (!paused && !(this.mc.currentScreen instanceof GuiDownloadTerrain)) {
                    if (this.serverWaitTime > 0) {
                        Lagometer.timerServer.start();
                        Config.sleep(this.serverWaitTime);
                        Lagometer.timerServer.end();
                        this.serverWaitTimeCurrent = this.serverWaitTime;
                    }

                    long timeNow = System.nanoTime() / 1000000L;
                    if (this.lastServerTime != 0L && this.lastServerTicks != 0) {
                        long timeDiff = timeNow - this.lastServerTime;
                        if (timeDiff < 0L) {
                            this.lastServerTime = timeNow;
                            timeDiff = 0L;
                        }

                        if (timeDiff >= 50L) {
                            this.lastServerTime = timeNow;
                            int ticks = srv.getTickCounter();
                            int tickDiff = ticks - this.lastServerTicks;
                            if (tickDiff < 0) {
                                this.lastServerTicks = ticks;
                                tickDiff = 0;
                            }

                            if (tickDiff < 1 && this.serverWaitTime < 100) {
                                this.serverWaitTime += 2;
                            }

                            if (tickDiff > 1 && this.serverWaitTime > 0) {
                                this.serverWaitTime--;
                            }

                            this.lastServerTicks = ticks;
                        }
                    } else {
                        this.lastServerTime = timeNow;
                        this.lastServerTicks = srv.getTickCounter();
                        this.avgServerTickDiff = 1.0F;
                        this.avgServerTimeDiff = 50.0F;
                    }
                } else {
                    if (this.mc.currentScreen instanceof GuiDownloadTerrain) {
                        Config.sleep(20L);
                    }

                    this.lastServerTime = 0L;
                    this.lastServerTicks = 0;
                }
            }
        }
    }

    private void frameInit() {
        GlErrors.frameStart();
        if (!this.initialized) {
            ReflectorResolver.resolve();
            TextureUtils.registerResourceListener();
            if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32) {
                Config.setNotify64BitJava(true);
            }

            this.initialized = true;
        }

        Config.checkDisplayMode();
        World world = this.mc.theWorld;
        if (world != null) {
            if (Config.getNewRelease() != null) {
                String userEdition = "HD_U".replace("HD_U", "HD Ultra").replace("L", "Light");
                String fullNewVer = userEdition + " " + Config.getNewRelease();
                ChatComponentText msg = new ChatComponentText(I18n.format("of.message.newVersion", "§n" + fullNewVer + "§r"));
                msg.setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(Action.OPEN_URL, "https://optifine.net/downloads")));
                this.mc.ingameGUI.getChatGUI().printChatMessage(msg);
                Config.setNewRelease(null);
            }

            if (Config.isNotify64BitJava()) {
                Config.setNotify64BitJava(false);
                ChatComponentText msg = new ChatComponentText(I18n.format("of.message.java64Bit"));
                this.mc.ingameGUI.getChatGUI().printChatMessage(msg);
            }
        }

        if (this.mc.currentScreen instanceof GuiMainMenu) {
            this.updateMainMenu((GuiMainMenu)this.mc.currentScreen);
        }

        if (this.updatedWorld != world) {
            RandomEntities.worldChanged(this.updatedWorld, world);
            Config.updateThreadPriorities();
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
            this.updatedWorld = world;
        }

        if (!this.setFxaaShader(Shaders.configAntialiasingLevel)) {
            Shaders.configAntialiasingLevel = 0;
        }

        if (this.mc.currentScreen != null && this.mc.currentScreen.getClass() == GuiChat.class) {
            this.mc.displayGuiScreen(new GuiChatOF((GuiChat)this.mc.currentScreen));
        }
    }

    private void frameFinish() {
        if (this.mc.theWorld != null && Config.isShowGlErrors() && TimedEvent.isActive("CheckGlErrorFrameFinish", 10000L)) {
            int err = GlStateManager.glGetError();
            if (err != 0 && GlErrors.isEnabled(err)) {
                String text = Config.getGlErrorString(err);
                ChatComponentText msg = new ChatComponentText(I18n.format("of.message.openglError", err, text));
                this.mc.ingameGUI.getChatGUI().printChatMessage(msg);
            }
        }
    }

    private void updateMainMenu(GuiMainMenu mainGui) {
        try {
            String str = null;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int day = calendar.get(5);
            int month = calendar.get(2) + 1;
            if (day == 8 && month == 4) {
                str = "Happy birthday, OptiFine!";
            }

            if (day == 14 && month == 8) {
                str = "Happy birthday, sp614x!";
            }

            if (str == null) {
                return;
            }

            Reflector.setFieldValue(mainGui, Reflector.GuiMainMenu_splashText, str);
        } catch (Throwable var6) {
        }
    }

    public boolean setFxaaShader(int fxaaLevel) {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            return false;
        } else if (this.theShaderGroup != null && this.theShaderGroup != this.fxaaShaders[2] && this.theShaderGroup != this.fxaaShaders[4]) {
            return true;
        } else if (fxaaLevel != 2 && fxaaLevel != 4) {
            if (this.theShaderGroup == null) {
                return true;
            } else {
                this.theShaderGroup.deleteShaderGroup();
                this.theShaderGroup = null;
                return true;
            }
        } else if (this.theShaderGroup != null && this.theShaderGroup == this.fxaaShaders[fxaaLevel]) {
            return true;
        } else if (this.mc.theWorld == null) {
            return true;
        } else {
            this.loadShader(new ResourceLocation("shaders/post/fxaa_of_" + fxaaLevel + "x.json"));
            this.fxaaShaders[fxaaLevel] = this.theShaderGroup;
            return this.useShader;
        }
    }

    private void checkLoadVisibleChunks(Entity entity, float partialTicks, ICamera icamera, boolean spectator) {
        int messageId = 201435902;
        if (this.loadVisibleChunks) {
            this.loadVisibleChunks = false;
            this.loadAllVisibleChunks(entity, partialTicks, icamera, spectator);
            this.mc.ingameGUI.getChatGUI().deleteChatLine(messageId);
        }

        if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(38)) {
            if (this.mc.currentScreen != null) {
                return;
            }

            this.loadVisibleChunks = true;
            ChatComponentText msg = new ChatComponentText(I18n.format("of.message.loadingVisibleChunks"));
            this.mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(msg, messageId);
        }
    }

    private void loadAllVisibleChunks(Entity entity, double partialTicks, ICamera icamera, boolean spectator) {
        int chunkUpdatesConfig = this.mc.gameSettings.ofChunkUpdates;
        boolean lazyChunkLoadingConfig = this.mc.gameSettings.ofLazyChunkLoading;

        try {
            this.mc.gameSettings.ofChunkUpdates = 1000;
            this.mc.gameSettings.ofLazyChunkLoading = false;
            RenderGlobal renderGlobal = Config.getRenderGlobal();
            int countLoadedChunks = renderGlobal.getCountLoadedChunks();
            long timeStart = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long timeLog = System.currentTimeMillis() + 5000L;
            int chunksUpdated = 0;
            boolean hasUpdates = false;

            do {
                hasUpdates = false;

                for (int i = 0; i < 100; i++) {
                    renderGlobal.displayListEntitiesDirty = true;
                    renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, spectator);
                    if (!renderGlobal.hasNoChunkUpdates()) {
                        hasUpdates = true;
                    }

                    chunksUpdated += renderGlobal.getCountChunksToUpdate();

                    while (!renderGlobal.hasNoChunkUpdates()) {
                        renderGlobal.updateChunks(System.nanoTime() + 1000000000L);
                    }

                    chunksUpdated -= renderGlobal.getCountChunksToUpdate();
                    if (!hasUpdates) {
                        break;
                    }
                }

                if (renderGlobal.getCountLoadedChunks() != countLoadedChunks) {
                    hasUpdates = true;
                    countLoadedChunks = renderGlobal.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > timeLog) {
                    Config.log("Chunks loaded: " + chunksUpdated);
                    timeLog = System.currentTimeMillis() + 5000L;
                }
            } while (hasUpdates);

            Config.log("Chunks loaded: " + chunksUpdated);
            Config.log("Finished loading visible chunks");
            RenderChunk.renderChunksUpdated = 0;
        } finally {
            this.mc.gameSettings.ofChunkUpdates = chunkUpdatesConfig;
            this.mc.gameSettings.ofLazyChunkLoading = lazyChunkLoadingConfig;
        }
    }
}
