package net.optifine;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class DynamicLight {
    private Entity entity = null;
    private double offsetY = 0.0;
    private double lastPosX = -2.1474836E9F;
    private double lastPosY = -2.1474836E9F;
    private double lastPosZ = -2.1474836E9F;
    private int lastLightLevel = 0;
    private boolean underwater = false;
    private long timeCheckMs = 0L;
    private Set<BlockPos> setLitChunkPos = new HashSet<>();
    private MutableBlockPos blockPosMutable = new MutableBlockPos();

    public DynamicLight(Entity entity) {
        this.entity = entity;
        this.offsetY = entity.getEyeHeight();
    }

    public void update(RenderGlobal renderGlobal) {
        if (Config.isDynamicLightsFast()) {
            long timeNowMs = System.currentTimeMillis();
            if (timeNowMs < this.timeCheckMs + 500L) {
                return;
            }

            this.timeCheckMs = timeNowMs;
        }

        double posX = this.entity.posX - 0.5;
        double posY = this.entity.posY - 0.5 + this.offsetY;
        double posZ = this.entity.posZ - 0.5;
        int lightLevel = DynamicLights.getLightLevel(this.entity);
        double dx = posX - this.lastPosX;
        double dy = posY - this.lastPosY;
        double dz = posZ - this.lastPosZ;
        double delta = 0.1;
        if (!(Math.abs(dx) <= delta) || !(Math.abs(dy) <= delta) || !(Math.abs(dz) <= delta) || this.lastLightLevel != lightLevel) {
            this.lastPosX = posX;
            this.lastPosY = posY;
            this.lastPosZ = posZ;
            this.lastLightLevel = lightLevel;
            this.underwater = false;
            World world = renderGlobal.getWorld();
            if (world != null) {
                this.blockPosMutable.set(MathHelper.floor_double(posX), MathHelper.floor_double(posY), MathHelper.floor_double(posZ));
                IBlockState state = world.getBlockState(this.blockPosMutable);
                Block block = state.getBlock();
                this.underwater = block == Blocks.water;
            }

            Set<BlockPos> setNewPos = new HashSet<>();
            if (lightLevel > 0) {
                EnumFacing dirX = (MathHelper.floor_double(posX) & 15) >= 8 ? EnumFacing.EAST : EnumFacing.WEST;
                EnumFacing dirY = (MathHelper.floor_double(posY) & 15) >= 8 ? EnumFacing.UP : EnumFacing.DOWN;
                EnumFacing dirZ = (MathHelper.floor_double(posZ) & 15) >= 8 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                BlockPos chunkPos = new BlockPos(posX, posY, posZ);
                RenderChunk chunk = renderGlobal.getRenderChunk(chunkPos);
                BlockPos chunkPosX = this.getChunkPos(chunk, chunkPos, dirX);
                RenderChunk chunkX = renderGlobal.getRenderChunk(chunkPosX);
                BlockPos chunkPosZ = this.getChunkPos(chunk, chunkPos, dirZ);
                RenderChunk chunkZ = renderGlobal.getRenderChunk(chunkPosZ);
                BlockPos chunkPosXZ = this.getChunkPos(chunkX, chunkPosX, dirZ);
                RenderChunk chunkXZ = renderGlobal.getRenderChunk(chunkPosXZ);
                BlockPos chunkPosY = this.getChunkPos(chunk, chunkPos, dirY);
                RenderChunk chunkY = renderGlobal.getRenderChunk(chunkPosY);
                BlockPos chunkPosYX = this.getChunkPos(chunkY, chunkPosY, dirX);
                RenderChunk chunkYX = renderGlobal.getRenderChunk(chunkPosYX);
                BlockPos chunkPosYZ = this.getChunkPos(chunkY, chunkPosY, dirZ);
                RenderChunk chunkYZ = renderGlobal.getRenderChunk(chunkPosYZ);
                BlockPos chunkPosYXZ = this.getChunkPos(chunkYX, chunkPosYX, dirZ);
                RenderChunk chunkYXZ = renderGlobal.getRenderChunk(chunkPosYXZ);
                this.updateChunkLight(chunk, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkX, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkZ, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkXZ, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkY, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkYX, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkYZ, this.setLitChunkPos, setNewPos);
                this.updateChunkLight(chunkYXZ, this.setLitChunkPos, setNewPos);
            }

            this.updateLitChunks(renderGlobal);
            this.setLitChunkPos = setNewPos;
        }
    }

    private BlockPos getChunkPos(RenderChunk renderChunk, BlockPos pos, EnumFacing facing) {
        return renderChunk != null ? renderChunk.getBlockPosOffset16(facing) : pos.offset(facing, 16);
    }

    private void updateChunkLight(RenderChunk renderChunk, Set<BlockPos> setPrevPos, Set<BlockPos> setNewPos) {
        if (renderChunk != null) {
            CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
            if (compiledChunk != null && !compiledChunk.isEmpty()) {
                renderChunk.setNeedsUpdate(true);
            }

            BlockPos pos = renderChunk.getPosition();
            if (setPrevPos != null) {
                setPrevPos.remove(pos);
            }

            if (setNewPos != null) {
                setNewPos.add(pos);
            }
        }
    }

    public void updateLitChunks(RenderGlobal renderGlobal) {
        for (BlockPos posOld : this.setLitChunkPos) {
            RenderChunk chunkOld = renderGlobal.getRenderChunk(posOld);
            this.updateChunkLight(chunkOld, null, null);
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public double getLastPosX() {
        return this.lastPosX;
    }

    public double getLastPosY() {
        return this.lastPosY;
    }

    public double getLastPosZ() {
        return this.lastPosZ;
    }

    public int getLastLightLevel() {
        return this.lastLightLevel;
    }

    public boolean isUnderwater() {
        return this.underwater;
    }

    public double getOffsetY() {
        return this.offsetY;
    }

    @Override
    public String toString() {
        return "Entity: " + this.entity + ", offsetY: " + this.offsetY;
    }
}
