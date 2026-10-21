package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EntityAuraFX.HappyVillagerFactory;
import net.minecraft.client.particle.EntityBreakingFX.SlimeFactory;
import net.minecraft.client.particle.EntityBreakingFX.SnowballFactory;
import net.minecraft.client.particle.EntityCrit2FX.MagicFactory;
import net.minecraft.client.particle.EntityDropParticleFX.LavaFactory;
import net.minecraft.client.particle.EntityDropParticleFX.WaterFactory;
import net.minecraft.client.particle.EntityEnchantmentTableParticleFX.EnchantmentTable;
import net.minecraft.client.particle.EntityExplodeFX.Factory;
import net.minecraft.client.particle.EntityFirework.SparkFX;
import net.minecraft.client.particle.EntityHeartFX.AngryVillagerFactory;
import net.minecraft.client.particle.EntitySpellParticleFX.AmbientMobFactory;
import net.minecraft.client.particle.EntitySpellParticleFX.InstantFactory;
import net.minecraft.client.particle.EntitySpellParticleFX.MobFactory;
import net.minecraft.client.particle.EntitySpellParticleFX.WitchFactory;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class EffectRenderer {
    private static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");
    protected World worldObj;
    private List<EntityFX>[][] fxLayers = new List[4][];
    private List<EntityParticleEmitter> particleEmitters = Lists.newArrayList();
    private TextureManager renderer;
    private Random rand = new Random();
    private Map<Integer, IParticleFactory> particleTypes = Maps.newHashMap();

    public EffectRenderer(World worldIn, TextureManager rendererIn) {
        this.worldObj = worldIn;
        this.renderer = rendererIn;

        for (int i = 0; i < 4; i++) {
            this.fxLayers[i] = new List[2];

            for (int j = 0; j < 2; j++) {
                this.fxLayers[i][j] = Lists.newArrayList();
            }
        }

        this.registerVanillaParticles();
    }

    private void registerVanillaParticles() {
        this.registerParticle(EnumParticleTypes.EXPLOSION_NORMAL.getParticleID(), new Factory());
        this.registerParticle(EnumParticleTypes.WATER_BUBBLE.getParticleID(), new net.minecraft.client.particle.EntityBubbleFX.Factory());
        this.registerParticle(EnumParticleTypes.WATER_SPLASH.getParticleID(), new net.minecraft.client.particle.EntitySplashFX.Factory());
        this.registerParticle(EnumParticleTypes.WATER_WAKE.getParticleID(), new net.minecraft.client.particle.EntityFishWakeFX.Factory());
        this.registerParticle(EnumParticleTypes.WATER_DROP.getParticleID(), new net.minecraft.client.particle.EntityRainFX.Factory());
        this.registerParticle(EnumParticleTypes.SUSPENDED.getParticleID(), new net.minecraft.client.particle.EntitySuspendFX.Factory());
        this.registerParticle(EnumParticleTypes.SUSPENDED_DEPTH.getParticleID(), new net.minecraft.client.particle.EntityAuraFX.Factory());
        this.registerParticle(EnumParticleTypes.CRIT.getParticleID(), new net.minecraft.client.particle.EntityCrit2FX.Factory());
        this.registerParticle(EnumParticleTypes.CRIT_MAGIC.getParticleID(), new MagicFactory());
        this.registerParticle(EnumParticleTypes.SMOKE_NORMAL.getParticleID(), new net.minecraft.client.particle.EntitySmokeFX.Factory());
        this.registerParticle(EnumParticleTypes.SMOKE_LARGE.getParticleID(), new net.minecraft.client.particle.EntityCritFX.Factory());
        this.registerParticle(EnumParticleTypes.SPELL.getParticleID(), new net.minecraft.client.particle.EntitySpellParticleFX.Factory());
        this.registerParticle(EnumParticleTypes.SPELL_INSTANT.getParticleID(), new InstantFactory());
        this.registerParticle(EnumParticleTypes.SPELL_MOB.getParticleID(), new MobFactory());
        this.registerParticle(EnumParticleTypes.SPELL_MOB_AMBIENT.getParticleID(), new AmbientMobFactory());
        this.registerParticle(EnumParticleTypes.SPELL_WITCH.getParticleID(), new WitchFactory());
        this.registerParticle(EnumParticleTypes.DRIP_WATER.getParticleID(), new WaterFactory());
        this.registerParticle(EnumParticleTypes.DRIP_LAVA.getParticleID(), new LavaFactory());
        this.registerParticle(EnumParticleTypes.VILLAGER_ANGRY.getParticleID(), new AngryVillagerFactory());
        this.registerParticle(EnumParticleTypes.VILLAGER_HAPPY.getParticleID(), new HappyVillagerFactory());
        this.registerParticle(EnumParticleTypes.TOWN_AURA.getParticleID(), new net.minecraft.client.particle.EntityAuraFX.Factory());
        this.registerParticle(EnumParticleTypes.NOTE.getParticleID(), new net.minecraft.client.particle.EntityNoteFX.Factory());
        this.registerParticle(EnumParticleTypes.PORTAL.getParticleID(), new net.minecraft.client.particle.EntityPortalFX.Factory());
        this.registerParticle(EnumParticleTypes.ENCHANTMENT_TABLE.getParticleID(), new EnchantmentTable());
        this.registerParticle(EnumParticleTypes.FLAME.getParticleID(), new net.minecraft.client.particle.EntityFlameFX.Factory());
        this.registerParticle(EnumParticleTypes.LAVA.getParticleID(), new net.minecraft.client.particle.EntityLavaFX.Factory());
        this.registerParticle(EnumParticleTypes.FOOTSTEP.getParticleID(), new net.minecraft.client.particle.EntityFootStepFX.Factory());
        this.registerParticle(EnumParticleTypes.CLOUD.getParticleID(), new net.minecraft.client.particle.EntityCloudFX.Factory());
        this.registerParticle(EnumParticleTypes.REDSTONE.getParticleID(), new net.minecraft.client.particle.EntityReddustFX.Factory());
        this.registerParticle(EnumParticleTypes.SNOWBALL.getParticleID(), new SnowballFactory());
        this.registerParticle(EnumParticleTypes.SNOW_SHOVEL.getParticleID(), new net.minecraft.client.particle.EntitySnowShovelFX.Factory());
        this.registerParticle(EnumParticleTypes.SLIME.getParticleID(), new SlimeFactory());
        this.registerParticle(EnumParticleTypes.HEART.getParticleID(), new net.minecraft.client.particle.EntityHeartFX.Factory());
        this.registerParticle(EnumParticleTypes.BARRIER.getParticleID(), new net.minecraft.client.particle.Barrier.Factory());
        this.registerParticle(EnumParticleTypes.ITEM_CRACK.getParticleID(), new net.minecraft.client.particle.EntityBreakingFX.Factory());
        this.registerParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), new net.minecraft.client.particle.EntityDiggingFX.Factory());
        this.registerParticle(EnumParticleTypes.BLOCK_DUST.getParticleID(), new net.minecraft.client.particle.EntityBlockDustFX.Factory());
        this.registerParticle(EnumParticleTypes.EXPLOSION_HUGE.getParticleID(), new net.minecraft.client.particle.EntityHugeExplodeFX.Factory());
        this.registerParticle(EnumParticleTypes.EXPLOSION_LARGE.getParticleID(), new net.minecraft.client.particle.EntityLargeExplodeFX.Factory());
        this.registerParticle(EnumParticleTypes.FIREWORKS_SPARK.getParticleID(), new net.minecraft.client.particle.EntityFirework.Factory());
        this.registerParticle(EnumParticleTypes.MOB_APPEARANCE.getParticleID(), new net.minecraft.client.particle.MobAppearance.Factory());
    }

    public void registerParticle(int id, IParticleFactory particleFactory) {
        this.particleTypes.put(id, particleFactory);
    }

    public void emitParticleAtEntity(Entity entityIn, EnumParticleTypes particleTypes) {
        this.particleEmitters.add(new EntityParticleEmitter(this.worldObj, entityIn, particleTypes));
    }

    public EntityFX spawnEffectParticle(
        int particleId, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters
    ) {
        IParticleFactory iparticlefactory = this.particleTypes.get(particleId);
        if (iparticlefactory != null) {
            EntityFX entityfx = iparticlefactory.getEntityFX(particleId, this.worldObj, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
            if (entityfx != null) {
                this.addEffect(entityfx);
                return entityfx;
            }
        }

        return null;
    }

    public void addEffect(EntityFX effect) {
        if (effect != null) {
            if (!(effect instanceof SparkFX) || Config.isFireworkParticles()) {
                int i = effect.getFXLayer();
                int j = effect.getAlpha() != 1.0F ? 0 : 1;
                if (this.fxLayers[i][j].size() >= 4000) {
                    this.fxLayers[i][j].remove(0);
                }

                this.fxLayers[i][j].add(effect);
            }
        }
    }

    public void updateEffects() {
        for (int i = 0; i < 4; i++) {
            this.updateEffectLayer(i);
        }

        List<EntityParticleEmitter> list = Lists.newArrayList();

        for (EntityParticleEmitter entityparticleemitter : this.particleEmitters) {
            entityparticleemitter.onUpdate();
            if (entityparticleemitter.isDead) {
                list.add(entityparticleemitter);
            }
        }

        this.particleEmitters.removeAll(list);
    }

    private void updateEffectLayer(int layer) {
        for (int i = 0; i < 2; i++) {
            this.updateEffectAlphaLayer(this.fxLayers[layer][i]);
        }
    }

    private void updateEffectAlphaLayer(List<EntityFX> entitiesFX) {
        List<EntityFX> list = Lists.newArrayList();
        long timeStartMs = System.currentTimeMillis();
        int countLeft = entitiesFX.size();

        for (int i = 0; i < entitiesFX.size(); i++) {
            EntityFX entityfx = entitiesFX.get(i);
            this.tickParticle(entityfx);
            if (entityfx.isDead) {
                list.add(entityfx);
            }

            countLeft--;
            if (System.currentTimeMillis() > timeStartMs + 20L) {
                break;
            }
        }

        if (countLeft > 0) {
            int countToRemove = countLeft;

            for (Iterator it = entitiesFX.iterator(); it.hasNext() && countToRemove > 0; countToRemove--) {
                EntityFX particle = (EntityFX)it.next();
                particle.setDead();
                it.remove();
            }
        }

        entitiesFX.removeAll(list);
    }

    private void tickParticle(EntityFX particle) {
        try {
            particle.onUpdate();
        } catch (Throwable var6) {
            CrashReport crashreport = CrashReport.makeCrashReport(var6, "Ticking Particle");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being ticked");
            final int i = particle.getFXLayer();
            crashreportcategory.addCrashSectionCallable("Particle", new Callable<String>() {
                public String call() throws Exception {
                    return particle.toString();
                }
            });
            crashreportcategory.addCrashSectionCallable("Particle Type", new Callable<String>() {
                public String call() throws Exception {
                    return i == 0 ? "MISC_TEXTURE" : (i == 1 ? "TERRAIN_TEXTURE" : (i == 3 ? "ENTITY_PARTICLE_TEXTURE" : "Unknown - " + i));
                }
            });
            throw new ReportedException(crashreport);
        }
    }

    public void renderParticles(Entity entityIn, float partialTicks) {
        float f = ActiveRenderInfo.getRotationX();
        float f1 = ActiveRenderInfo.getRotationZ();
        float f2 = ActiveRenderInfo.getRotationYZ();
        float f3 = ActiveRenderInfo.getRotationXY();
        float f4 = ActiveRenderInfo.getRotationXZ();
        EntityFX.interpPosX = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        EntityFX.interpPosY = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        EntityFX.interpPosZ = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.alphaFunc(516, 0.003921569F);
        Block cameraBlock = ActiveRenderInfo.getBlockAtEntityViewpoint(this.worldObj, entityIn, partialTicks);
        boolean isEyeInWater = cameraBlock.getMaterial() == Material.water;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                if (!this.fxLayers[i][j].isEmpty()) {
                    switch (j) {
                        case 0:
                            GlStateManager.depthMask(false);
                            break;
                        case 1:
                            GlStateManager.depthMask(true);
                    }

                    switch (i) {
                        case 0:
                        default:
                            this.renderer.bindTexture(particleTextures);
                            break;
                        case 1:
                            this.renderer.bindTexture(TextureMap.locationBlocksTexture);
                    }

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    Tessellator tessellator = Tessellator.getInstance();
                    WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
                    vertexbuffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

                    for (int k = 0; k < this.fxLayers[i][j].size(); k++) {
                        final int layerIndex = i;
                        final EntityFX entityfx = this.fxLayers[i][j].get(k);

                        try {
                            if (isEyeInWater || !(entityfx instanceof EntitySuspendFX)) {
                                entityfx.renderParticle(vertexbuffer, entityIn, partialTicks, f, f4, f1, f2, f3);
                            }
                        } catch (Throwable var20) {
                            CrashReport crashreport = CrashReport.makeCrashReport(var20, "Rendering Particle");
                            CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being rendered");
                            crashreportcategory.addCrashSectionCallable("Particle", new Callable<String>() {
                                public String call() throws Exception {
                                    return entityfx.toString();
                                }
                            });
                            crashreportcategory.addCrashSectionCallable("Particle Type", new Callable<String>() {
                                public String call() throws Exception {
                                    return layerIndex == 0 ? "MISC_TEXTURE" : (layerIndex == 1 ? "TERRAIN_TEXTURE" : (layerIndex == 3 ? "ENTITY_PARTICLE_TEXTURE" : "Unknown - " + layerIndex));
                                }
                            });
                            throw new ReportedException(crashreport);
                        }
                    }

                    tessellator.draw();
                }
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(516, 0.1F);
    }

    public void renderLitParticles(Entity entityIn, float partialTick) {
        float f = (float) (Math.PI / 180.0);
        float f1 = MathHelper.cos(entityIn.rotationYaw * (float) (Math.PI / 180.0));
        float f2 = MathHelper.sin(entityIn.rotationYaw * (float) (Math.PI / 180.0));
        float f3 = -f2 * MathHelper.sin(entityIn.rotationPitch * (float) (Math.PI / 180.0));
        float f4 = f1 * MathHelper.sin(entityIn.rotationPitch * (float) (Math.PI / 180.0));
        float f5 = MathHelper.cos(entityIn.rotationPitch * (float) (Math.PI / 180.0));

        for (int i = 0; i < 2; i++) {
            List<EntityFX> list = this.fxLayers[3][i];
            if (!list.isEmpty()) {
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer vertexbuffer = tessellator.getWorldRenderer();

                for (int j = 0; j < list.size(); j++) {
                    EntityFX entityfx = list.get(j);
                    entityfx.renderParticle(vertexbuffer, entityIn, partialTick, f1, f5, f2, f3, f4);
                }
            }
        }
    }

    public void clearEffects(World worldIn) {
        this.worldObj = worldIn;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                this.fxLayers[i][j].clear();
            }
        }

        this.particleEmitters.clear();
    }

    public void addBlockDestroyEffects(BlockPos pos, IBlockState state) {
        boolean notAir;
        if (Reflector.ForgeBlock_addDestroyEffects.exists() && Reflector.ForgeBlock_isAir.exists()) {
            Block block = state.getBlock();
            notAir = !Reflector.callBoolean(block, Reflector.ForgeBlock_isAir, this.worldObj, pos)
                && !Reflector.callBoolean(block, Reflector.ForgeBlock_addDestroyEffects, this.worldObj, pos, this);
        } else {
            notAir = state.getBlock().getMaterial() != Material.air;
        }

        if (notAir) {
            state = state.getBlock().getActualState(state, this.worldObj, pos);
            int i = 4;

            for (int j = 0; j < i; j++) {
                for (int k = 0; k < i; k++) {
                    for (int l = 0; l < i; l++) {
                        double d0 = pos.getX() + (j + 0.5) / i;
                        double d1 = pos.getY() + (k + 0.5) / i;
                        double d2 = pos.getZ() + (l + 0.5) / i;
                        this.addEffect(
                            new EntityDiggingFX(this.worldObj, d0, d1, d2, d0 - pos.getX() - 0.5, d1 - pos.getY() - 0.5, d2 - pos.getZ() - 0.5, state)
                                .setBlockPos(pos)
                        );
                    }
                }
            }
        }
    }

    public void addBlockHitEffects(BlockPos pos, EnumFacing side) {
        IBlockState iblockstate = this.worldObj.getBlockState(pos);
        Block block = iblockstate.getBlock();
        if (block.getRenderType() != -1) {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            float f = 0.1F;
            double d0 = i + this.rand.nextDouble() * (block.getBlockBoundsMaxX() - block.getBlockBoundsMinX() - f * 2.0F) + f + block.getBlockBoundsMinX();
            double d1 = j + this.rand.nextDouble() * (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() - f * 2.0F) + f + block.getBlockBoundsMinY();
            double d2 = k + this.rand.nextDouble() * (block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ() - f * 2.0F) + f + block.getBlockBoundsMinZ();
            if (side == EnumFacing.DOWN) {
                d1 = j + block.getBlockBoundsMinY() - f;
            }

            if (side == EnumFacing.UP) {
                d1 = j + block.getBlockBoundsMaxY() + f;
            }

            if (side == EnumFacing.NORTH) {
                d2 = k + block.getBlockBoundsMinZ() - f;
            }

            if (side == EnumFacing.SOUTH) {
                d2 = k + block.getBlockBoundsMaxZ() + f;
            }

            if (side == EnumFacing.WEST) {
                d0 = i + block.getBlockBoundsMinX() - f;
            }

            if (side == EnumFacing.EAST) {
                d0 = i + block.getBlockBoundsMaxX() + f;
            }

            this.addEffect(
                new EntityDiggingFX(this.worldObj, d0, d1, d2, 0.0, 0.0, 0.0, iblockstate)
                    .setBlockPos(pos)
                    .multiplyVelocity(0.2F)
                    .multipleParticleScaleBy(0.6F)
            );
        }
    }

    public void moveToAlphaLayer(EntityFX effect) {
        this.moveToLayer(effect, 1, 0);
    }

    public void moveToNoAlphaLayer(EntityFX effect) {
        this.moveToLayer(effect, 0, 1);
    }

    private void moveToLayer(EntityFX effect, int layerFrom, int layerTo) {
        for (int i = 0; i < 4; i++) {
            if (this.fxLayers[i][layerFrom].contains(effect)) {
                this.fxLayers[i][layerFrom].remove(effect);
                this.fxLayers[i][layerTo].add(effect);
            }
        }
    }

    public String getStatistics() {
        int i = 0;

        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 2; k++) {
                i += this.fxLayers[j][k].size();
            }
        }

        return "" + i;
    }

    public void addBlockHitEffects(BlockPos pos, MovingObjectPosition target) {
        IBlockState state = this.worldObj.getBlockState(pos);
        if (state != null) {
            boolean blockAddHitEffects = Reflector.callBoolean(state.getBlock(), Reflector.ForgeBlock_addHitEffects, this.worldObj, target, this);
            if (state != null && !blockAddHitEffects) {
                this.addBlockHitEffects(pos, target.sideHit);
            }
        }
    }
}
