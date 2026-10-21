package net.optifine.override;

import java.util.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.Config;
import net.optifine.DynamicLights;
import net.optifine.reflect.Reflector;
import net.optifine.util.ArrayCache;

public class ChunkCacheOF implements IBlockAccess {
    private final ChunkCache chunkCache;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int sizeXY;
    private int[] combinedLights;
    private IBlockState[] blockStates;
    private final int arraySize;
    private final boolean dynamicLights = Config.isDynamicLights();
    private static final ArrayCache cacheCombinedLights = new ArrayCache(int.class, 16);
    private static final ArrayCache cacheBlockStates = new ArrayCache(IBlockState.class, 16);

    public ChunkCacheOF(ChunkCache chunkCache, BlockPos posFromIn, BlockPos posToIn, int subIn) {
        this.chunkCache = chunkCache;
        int minChunkX = posFromIn.getX() - subIn >> 4;
        int minChunkY = posFromIn.getY() - subIn >> 4;
        int minChunkZ = posFromIn.getZ() - subIn >> 4;
        int maxChunkX = posToIn.getX() + subIn >> 4;
        int maxChunkY = posToIn.getY() + subIn >> 4;
        int maxChunkZ = posToIn.getZ() + subIn >> 4;
        this.sizeX = maxChunkX - minChunkX + 1 << 4;
        this.sizeY = maxChunkY - minChunkY + 1 << 4;
        this.sizeZ = maxChunkZ - minChunkZ + 1 << 4;
        this.sizeXY = this.sizeX * this.sizeY;
        this.arraySize = this.sizeX * this.sizeY * this.sizeZ;
        this.posX = minChunkX << 4;
        this.posY = minChunkY << 4;
        this.posZ = minChunkZ << 4;
    }

    private int getPositionIndex(BlockPos pos) {
        int dx = pos.getX() - this.posX;
        if (dx >= 0 && dx < this.sizeX) {
            int dy = pos.getY() - this.posY;
            if (dy >= 0 && dy < this.sizeY) {
                int dz = pos.getZ() - this.posZ;
                return dz >= 0 && dz < this.sizeZ ? dz * this.sizeXY + dy * this.sizeX + dx : -1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int index = this.getPositionIndex(pos);
        if (index >= 0 && index < this.arraySize && this.combinedLights != null) {
            int light = this.combinedLights[index];
            if (light == -1) {
                light = this.getCombinedLightRaw(pos, lightValue);
                this.combinedLights[index] = light;
            }

            return light;
        } else {
            return this.getCombinedLightRaw(pos, lightValue);
        }
    }

    private int getCombinedLightRaw(BlockPos pos, int lightValue) {
        int light = this.chunkCache.getCombinedLight(pos, lightValue);
        if (this.dynamicLights && !this.getBlockState(pos).getBlock().isOpaqueCube()) {
            light = DynamicLights.getCombinedLight(pos, light);
        }

        return light;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        int index = this.getPositionIndex(pos);
        if (index >= 0 && index < this.arraySize && this.blockStates != null) {
            IBlockState iblockstate = this.blockStates[index];
            if (iblockstate == null) {
                iblockstate = this.chunkCache.getBlockState(pos);
                this.blockStates[index] = iblockstate;
            }

            return iblockstate;
        } else {
            return this.chunkCache.getBlockState(pos);
        }
    }

    public void renderStart() {
        if (this.combinedLights == null) {
            this.combinedLights = (int[])cacheCombinedLights.allocate(this.arraySize);
        }

        Arrays.fill(this.combinedLights, -1);
        if (this.blockStates == null) {
            this.blockStates = (IBlockState[])cacheBlockStates.allocate(this.arraySize);
        }

        Arrays.fill(this.blockStates, null);
    }

    public void renderFinish() {
        cacheCombinedLights.free(this.combinedLights);
        this.combinedLights = null;
        cacheBlockStates.free(this.blockStates);
        this.blockStates = null;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return this.chunkCache.extendedLevelsInChunkCache();
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
        return this.chunkCache.getBiomeGenForCoords(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.chunkCache.getStrongPower(pos, direction);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return this.chunkCache.getTileEntity(pos);
    }

    @Override
    public WorldType getWorldType() {
        return this.chunkCache.getWorldType();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return this.chunkCache.isAirBlock(pos);
    }

    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return Reflector.callBoolean(this.chunkCache, Reflector.ForgeChunkCache_isSideSolid, pos, side, _default);
    }
}
