package net.minecraft.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.optifine.BlockPosM;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;

public final class SpawnerAnimals {
    private static final int MOB_COUNT_DIV = (int)Math.pow(17.0, 2.0);
    private final Set<ChunkCoordIntPair> eligibleChunksForSpawning = Sets.newHashSet();
    private Map<Class, EntityLiving> mapSampleEntitiesByClass = new HashMap<>();
    private int lastPlayerChunkX = Integer.MAX_VALUE;
    private int lastPlayerChunkZ = Integer.MAX_VALUE;
    private int countChunkPos;

    public int findChunksForSpawning(WorldServer worldServerIn, boolean spawnHostileMobs, boolean spawnPeacefulMobs, boolean spawnOnSetTickRate) {
        if (!spawnHostileMobs && !spawnPeacefulMobs) {
            return 0;
        } else {
            boolean updateEligibleChunks = true;
            EntityPlayer player = null;
            if (worldServerIn.playerEntities.size() == 1) {
                player = worldServerIn.playerEntities.get(0);
                if (this.eligibleChunksForSpawning.size() > 0
                    && player != null
                    && player.chunkCoordX == this.lastPlayerChunkX
                    && player.chunkCoordZ == this.lastPlayerChunkZ) {
                    updateEligibleChunks = false;
                }
            }

            if (updateEligibleChunks) {
                this.eligibleChunksForSpawning.clear();
                int i = 0;

                for (EntityPlayer entityplayer : worldServerIn.playerEntities) {
                    if (!entityplayer.isSpectator()) {
                        int j = MathHelper.floor_double(entityplayer.posX / 16.0);
                        int k = MathHelper.floor_double(entityplayer.posZ / 16.0);
                        int l = 8;

                        for (int i1 = -l; i1 <= l; i1++) {
                            for (int j1 = -l; j1 <= l; j1++) {
                                boolean flag = i1 == -l || i1 == l || j1 == -l || j1 == l;
                                ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i1 + j, j1 + k);
                                if (!this.eligibleChunksForSpawning.contains(chunkcoordintpair)) {
                                    i++;
                                    if (!flag && worldServerIn.getWorldBorder().contains(chunkcoordintpair)) {
                                        this.eligibleChunksForSpawning.add(chunkcoordintpair);
                                    }
                                }
                            }
                        }
                    }
                }

                this.countChunkPos = i;
                if (player != null) {
                    this.lastPlayerChunkX = player.chunkCoordX;
                    this.lastPlayerChunkZ = player.chunkCoordZ;
                }
            }

            int i4 = 0;
            BlockPos blockpos2 = worldServerIn.getSpawnPoint();
            BlockPosM blockPosM = new BlockPosM(0, 0, 0);
            new MutableBlockPos();

            for (EnumCreatureType enumcreaturetype : EnumCreatureType.values()) {
                if ((!enumcreaturetype.getPeacefulCreature() || spawnPeacefulMobs)
                    && (enumcreaturetype.getPeacefulCreature() || spawnHostileMobs)
                    && (!enumcreaturetype.getAnimal() || spawnOnSetTickRate)) {
                    int j4 = Reflector.ForgeWorld_countEntities.exists()
                        ? Reflector.callInt(worldServerIn, Reflector.ForgeWorld_countEntities, enumcreaturetype, true)
                        : worldServerIn.countEntities(enumcreaturetype.getCreatureClass());
                    int k4 = enumcreaturetype.getMaxNumberOfCreature() * this.countChunkPos / MOB_COUNT_DIV;
                    if (j4 <= k4) {
                        Collection<ChunkCoordIntPair> chunksForSpawning = this.eligibleChunksForSpawning;
                        if (Reflector.ForgeHooksClient.exists()) {
                            ArrayList<ChunkCoordIntPair> shuffled = Lists.newArrayList(chunksForSpawning);
                            Collections.shuffle(shuffled);
                            chunksForSpawning = shuffled;
                        }

                        label166:
                        for (ChunkCoordIntPair chunkcoordintpair1 : chunksForSpawning) {
                            BlockPos blockpos = getRandomChunkPosition(worldServerIn, chunkcoordintpair1.chunkXPos, chunkcoordintpair1.chunkZPos, blockPosM);
                            int k1 = blockpos.getX();
                            int l1 = blockpos.getY();
                            int i2 = blockpos.getZ();
                            Block block = worldServerIn.getBlockState(blockpos).getBlock();
                            if (!block.isNormalCube()) {
                                int j2 = 0;

                                for (int k2 = 0; k2 < 3; k2++) {
                                    int l2 = k1;
                                    int i3 = l1;
                                    int j3 = i2;
                                    int k3 = 6;
                                    SpawnListEntry biomegenbase$spawnlistentry = null;
                                    IEntityLivingData ientitylivingdata = null;

                                    for (int l3 = 0; l3 < 4; l3++) {
                                        l2 += worldServerIn.rand.nextInt(k3) - worldServerIn.rand.nextInt(k3);
                                        i3 += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
                                        j3 += worldServerIn.rand.nextInt(k3) - worldServerIn.rand.nextInt(k3);
                                        BlockPos blockpos1 = new BlockPos(l2, i3, j3);
                                        float f = l2 + 0.5F;
                                        float f1 = j3 + 0.5F;
                                        if (!worldServerIn.isAnyPlayerWithinRangeAt(f, i3, f1, 24.0) && blockpos2.distanceSq(f, i3, f1) >= 576.0) {
                                            if (biomegenbase$spawnlistentry == null) {
                                                biomegenbase$spawnlistentry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos1);
                                                if (biomegenbase$spawnlistentry == null) {
                                                    break;
                                                }
                                            }

                                            if (worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, biomegenbase$spawnlistentry, blockpos1)
                                                && canCreatureTypeSpawnAtLocation(
                                                    EntitySpawnPlacementRegistry.getPlacementForEntity(biomegenbase$spawnlistentry.entityClass),
                                                    worldServerIn,
                                                    blockpos1
                                                )) {
                                                EntityLiving entityliving;
                                                try {
                                                    entityliving = this.mapSampleEntitiesByClass.get(biomegenbase$spawnlistentry.entityClass);
                                                    if (entityliving == null) {
                                                        entityliving = biomegenbase$spawnlistentry.entityClass
                                                            .getConstructor(World.class)
                                                            .newInstance(worldServerIn);
                                                        this.mapSampleEntitiesByClass.put(biomegenbase$spawnlistentry.entityClass, entityliving);
                                                    }
                                                } catch (Exception var40) {
                                                    var40.printStackTrace();
                                                    return i4;
                                                }

                                                entityliving.setLocationAndAngles(f, i3, f1, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);
                                                boolean canSpawn = Reflector.ForgeEventFactory_canEntitySpawn.exists()
                                                    ? ReflectorForge.canEntitySpawn(entityliving, worldServerIn, f, i3, f1)
                                                    : entityliving.getCanSpawnHere() && entityliving.isNotColliding();
                                                if (canSpawn) {
                                                    this.mapSampleEntitiesByClass.remove(biomegenbase$spawnlistentry.entityClass);
                                                    if (!ReflectorForge.doSpecialSpawn(entityliving, worldServerIn, f, i3, f1)) {
                                                        ientitylivingdata = entityliving.onInitialSpawn(
                                                            worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata
                                                        );
                                                    }

                                                    if (entityliving.isNotColliding()) {
                                                        j2++;
                                                        worldServerIn.spawnEntityInWorld(entityliving);
                                                    }

                                                    int maxSpawnedInChunk = Reflector.ForgeEventFactory_getMaxSpawnPackSize.exists()
                                                        ? Reflector.callInt(Reflector.ForgeEventFactory_getMaxSpawnPackSize, entityliving)
                                                        : entityliving.getMaxSpawnedInChunk();
                                                    if (j2 >= maxSpawnedInChunk) {
                                                        continue label166;
                                                    }
                                                }

                                                i4 += j2;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return i4;
        }
    }

    protected static BlockPos getRandomChunkPosition(World worldIn, int x, int z) {
        Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
        int i = x * 16 + worldIn.rand.nextInt(16);
        int j = z * 16 + worldIn.rand.nextInt(16);
        int k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
        int l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(i, l, j);
    }

    private static BlockPosM getRandomChunkPosition(World worldIn, int x, int z, BlockPosM blockPosM) {
        Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
        int px = x * 16 + worldIn.rand.nextInt(16);
        int pz = z * 16 + worldIn.rand.nextInt(16);
        int k = MathHelper.roundUp(chunk.getHeightValue(px & 15, pz & 15) + 1, 16);
        int py = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
        blockPosM.setXyz(px, py, pz);
        return blockPosM;
    }

    public static boolean canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos) {
        if (!worldIn.getWorldBorder().contains(pos)) {
            return false;
        } else if (spawnPlacementTypeIn == null) {
            return false;
        } else {
            Block block = worldIn.getBlockState(pos).getBlock();
            if (spawnPlacementTypeIn == EntityLiving.SpawnPlacementType.IN_WATER) {
                return block.getMaterial().isLiquid()
                    && worldIn.getBlockState(pos.down()).getBlock().getMaterial().isLiquid()
                    && !worldIn.getBlockState(pos.up()).getBlock().isNormalCube();
            } else {
                BlockPos blockpos = pos.down();
                IBlockState state = worldIn.getBlockState(blockpos);
                boolean canSpawn = Reflector.ForgeBlock_canCreatureSpawn.exists()
                    ? Reflector.callBoolean(state.getBlock(), Reflector.ForgeBlock_canCreatureSpawn, worldIn, blockpos, spawnPlacementTypeIn)
                    : World.doesBlockHaveSolidTopSurface(worldIn, blockpos);
                if (!canSpawn) {
                    return false;
                } else {
                    Block block1 = worldIn.getBlockState(blockpos).getBlock();
                    boolean flag = block1 != Blocks.bedrock && block1 != Blocks.barrier;
                    return flag && !block.isNormalCube() && !block.getMaterial().isLiquid() && !worldIn.getBlockState(pos.up()).getBlock().isNormalCube();
                }
            }
        }
    }

    public static void performWorldGenSpawning(
        World worldIn, BiomeGenBase biomeIn, int p_77191_2_, int p_77191_3_, int p_77191_4_, int p_77191_5_, Random randomIn
    ) {
        List<SpawnListEntry> list = biomeIn.getSpawnableList(EnumCreatureType.CREATURE);
        if (!list.isEmpty()) {
            while (randomIn.nextFloat() < biomeIn.getSpawningChance()) {
                SpawnListEntry biomegenbase$spawnlistentry = WeightedRandom.getRandomItem(worldIn.rand, list);
                int i = biomegenbase$spawnlistentry.minGroupCount
                    + randomIn.nextInt(1 + biomegenbase$spawnlistentry.maxGroupCount - biomegenbase$spawnlistentry.minGroupCount);
                IEntityLivingData ientitylivingdata = null;
                int j = p_77191_2_ + randomIn.nextInt(p_77191_4_);
                int k = p_77191_3_ + randomIn.nextInt(p_77191_5_);
                int l = j;
                int i1 = k;

                for (int j1 = 0; j1 < i; j1++) {
                    boolean flag = false;

                    for (int k1 = 0; !flag && k1 < 4; k1++) {
                        BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j, 0, k));
                        if (canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, worldIn, blockpos)) {
                            EntityLiving entityliving;
                            try {
                                entityliving = biomegenbase$spawnlistentry.entityClass.getConstructor(World.class).newInstance(worldIn);
                            } catch (Exception var21) {
                                var21.printStackTrace();
                                continue;
                            }

                            if (Reflector.ForgeEventFactory_canEntitySpawn.exists()) {
                                Object canSpawn = Reflector.call(
                                    Reflector.ForgeEventFactory_canEntitySpawn, entityliving, worldIn, j + 0.5F, blockpos.getY(), k + 0.5F
                                );
                                if (canSpawn == ReflectorForge.EVENT_RESULT_DENY) {
                                    continue;
                                }
                            }

                            entityliving.setLocationAndAngles(j + 0.5F, blockpos.getY(), k + 0.5F, randomIn.nextFloat() * 360.0F, 0.0F);
                            worldIn.spawnEntityInWorld(entityliving);
                            ientitylivingdata = entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
                            flag = true;
                        }

                        j += randomIn.nextInt(5) - randomIn.nextInt(5);

                        for (k += randomIn.nextInt(5) - randomIn.nextInt(5);
                            j < p_77191_2_ || j >= p_77191_2_ + p_77191_4_ || k < p_77191_3_ || k >= p_77191_3_ + p_77191_4_;
                            k = i1 + randomIn.nextInt(5) - randomIn.nextInt(5)
                        ) {
                            j = l + randomIn.nextInt(5) - randomIn.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}
