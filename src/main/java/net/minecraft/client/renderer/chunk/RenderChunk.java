package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Sets;
import java.nio.FloatBuffer;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.Status;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.Type;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomBlockLayers;
import net.optifine.override.ChunkCacheOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.render.AabbFrame;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;

public class RenderChunk {
    private final World world;
    private final RenderGlobal renderGlobal;
    public static int renderChunksUpdated;
    private BlockPos position;
    public CompiledChunk compiledChunk = CompiledChunk.DUMMY;
    private final ReentrantLock lockCompileTask = new ReentrantLock();
    private final ReentrantLock lockCompiledChunk = new ReentrantLock();
    private ChunkCompileTaskGenerator compileTask = null;
    private final Set<TileEntity> setTileEntities = Sets.newHashSet();
    private final int index;
    private final FloatBuffer modelviewMatrix = GLAllocation.createDirectFloatBuffer(16);
    private final VertexBuffer[] vertexBuffers = new VertexBuffer[EnumWorldBlockLayer.values().length];
    public AxisAlignedBB boundingBox;
    private int frameIndex = -1;
    private boolean needsUpdate = true;
    private EnumMap<EnumFacing, BlockPos> mapEnumFacing = null;
    private BlockPos[] positionOffsets16 = new BlockPos[EnumFacing.VALUES.length];
    public static final EnumWorldBlockLayer[] ENUM_WORLD_BLOCK_LAYERS = EnumWorldBlockLayer.values();
    private final EnumWorldBlockLayer[] blockLayersSingle = new EnumWorldBlockLayer[1];
    private final boolean isMipmaps = Config.isMipmaps();
    private final boolean fixBlockLayer = !Reflector.BetterFoliageClient.exists();
    private boolean playerUpdate = false;
    public int regionX;
    public int regionZ;
    private final RenderChunk[] renderChunksOfset16 = new RenderChunk[6];
    private boolean renderChunksOffset16Updated = false;
    private Chunk chunk;
    private RenderChunk[] renderChunkNeighbours = new RenderChunk[EnumFacing.VALUES.length];
    private RenderChunk[] renderChunkNeighboursValid = new RenderChunk[EnumFacing.VALUES.length];
    private boolean renderChunkNeighboursUpated = false;
    private RenderGlobal.ContainerLocalRenderInformation renderInfo = new RenderGlobal.ContainerLocalRenderInformation(this, null, 0);
    public AabbFrame boundingBoxParent;

    public RenderChunk(World worldIn, RenderGlobal renderGlobalIn, BlockPos blockPosIn, int indexIn) {
        this.world = worldIn;
        this.renderGlobal = renderGlobalIn;
        this.index = indexIn;
        if (!blockPosIn.equals(this.getPosition())) {
            this.setPosition(blockPosIn);
        }

        if (OpenGlHelper.useVbo()) {
            for (int i = 0; i < EnumWorldBlockLayer.values().length; i++) {
                this.vertexBuffers[i] = new VertexBuffer(DefaultVertexFormats.BLOCK);
            }
        }
    }

    public boolean setFrameIndex(int frameIndexIn) {
        if (this.frameIndex == frameIndexIn) {
            return false;
        } else {
            this.frameIndex = frameIndexIn;
            return true;
        }
    }

    public VertexBuffer getVertexBufferByLayer(int layer) {
        return this.vertexBuffers[layer];
    }

    public void setPosition(BlockPos pos) {
        this.stopCompileTask();
        this.position = pos;
        int bits = 8;
        this.regionX = pos.getX() >> bits << bits;
        this.regionZ = pos.getZ() >> bits << bits;
        this.boundingBox = new AxisAlignedBB(pos, pos.add(16, 16, 16));
        this.initModelviewMatrix();

        for (int i = 0; i < this.positionOffsets16.length; i++) {
            this.positionOffsets16[i] = null;
        }

        this.renderChunksOffset16Updated = false;
        this.renderChunkNeighboursUpated = false;

        for (int i = 0; i < this.renderChunkNeighbours.length; i++) {
            RenderChunk neighbour = this.renderChunkNeighbours[i];
            if (neighbour != null) {
                neighbour.renderChunkNeighboursUpated = false;
            }
        }

        this.chunk = null;
        this.boundingBoxParent = null;
    }

    public void resortTransparency(float x, float y, float z, ChunkCompileTaskGenerator generator) {
        CompiledChunk compiledchunk = generator.getCompiledChunk();
        if (compiledchunk.getState() != null && !compiledchunk.isLayerEmpty(EnumWorldBlockLayer.TRANSLUCENT)) {
            WorldRenderer bufferTranslucent = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(EnumWorldBlockLayer.TRANSLUCENT);
            this.preRenderBlocks(bufferTranslucent, this.position);
            bufferTranslucent.setVertexState(compiledchunk.getState());
            this.postRenderBlocks(EnumWorldBlockLayer.TRANSLUCENT, x, y, z, bufferTranslucent, compiledchunk);
        }
    }

    public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGenerator generator) {
        CompiledChunk compiledchunk = new CompiledChunk();
        int i = 1;
        BlockPos blockpos = new BlockPos(this.position);
        BlockPos blockpos1 = blockpos.add(15, 15, 15);
        generator.getLock().lock();

        try {
            if (generator.getStatus() != Status.COMPILING) {
                return;
            }

            generator.setCompiledChunk(compiledchunk);
        } finally {
            generator.getLock().unlock();
        }

        VisGraph lvt_10_1_ = new VisGraph();
        HashSet lvt_11_1_ = Sets.newHashSet();
        if (!this.isChunkRegionEmpty(blockpos)) {
            renderChunksUpdated++;
            ChunkCacheOF blockAccess = this.makeChunkCacheOF(blockpos);
            blockAccess.renderStart();
            boolean[] aboolean = new boolean[ENUM_WORLD_BLOCK_LAYERS.length];
            BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            boolean forgeBlockCanRenderInLayerExists = Reflector.ForgeBlock_canRenderInLayer.exists();
            boolean forgeHooksSetRenderLayerExists = Reflector.ForgeHooksClient_setRenderLayer.exists();

            for (BlockPosM blockpos$mutableblockpos : (Iterable<BlockPosM>) (Iterable) BlockPosM.getAllInBoxMutable(blockpos, blockpos1)) {
                IBlockState iblockstate = blockAccess.getBlockState(blockpos$mutableblockpos);
                Block block = iblockstate.getBlock();
                if (block.isOpaqueCube()) {
                    lvt_10_1_.func_178606_a(blockpos$mutableblockpos);
                }

                if (ReflectorForge.blockHasTileEntity(iblockstate)) {
                    TileEntity tileentity = blockAccess.getTileEntity(new BlockPos(blockpos$mutableblockpos));
                    TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(tileentity);
                    if (tileentity != null && tileentityspecialrenderer != null) {
                        compiledchunk.addTileEntity(tileentity);
                        if (tileentityspecialrenderer.forceTileEntityRender()) {
                            lvt_11_1_.add(tileentity);
                        }
                    }
                }

                EnumWorldBlockLayer[] blockLayers;
                if (forgeBlockCanRenderInLayerExists) {
                    blockLayers = ENUM_WORLD_BLOCK_LAYERS;
                } else {
                    blockLayers = this.blockLayersSingle;
                    blockLayers[0] = block.getBlockLayer();
                }

                for (int ix = 0; ix < blockLayers.length; ix++) {
                    EnumWorldBlockLayer enumworldblocklayer1 = blockLayers[ix];
                    if (forgeBlockCanRenderInLayerExists) {
                        boolean canRenderInLayer = Reflector.callBoolean(block, Reflector.ForgeBlock_canRenderInLayer, enumworldblocklayer1);
                        if (!canRenderInLayer) {
                            continue;
                        }
                    }

                    if (forgeHooksSetRenderLayerExists) {
                        Reflector.callVoid(Reflector.ForgeHooksClient_setRenderLayer, enumworldblocklayer1);
                    }

                    enumworldblocklayer1 = this.fixBlockLayer(iblockstate, enumworldblocklayer1);
                    int j = enumworldblocklayer1.ordinal();
                    if (block.getRenderType() != -1) {
                        WorldRenderer vertexbuffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayerId(j);
                        vertexbuffer.setBlockLayer(enumworldblocklayer1);
                        RenderEnv renderEnv = vertexbuffer.getRenderEnv(iblockstate, blockpos$mutableblockpos);
                        renderEnv.setRegionRenderCacheBuilder(generator.getRegionRenderCacheBuilder());
                        if (!compiledchunk.isLayerStarted(enumworldblocklayer1)) {
                            compiledchunk.setLayerStarted(enumworldblocklayer1);
                            this.preRenderBlocks(vertexbuffer, blockpos);
                        }

                        aboolean[j] |= blockrendererdispatcher.renderBlock(iblockstate, blockpos$mutableblockpos, blockAccess, vertexbuffer);
                        if (renderEnv.isOverlaysRendered()) {
                            this.postRenderOverlays(generator.getRegionRenderCacheBuilder(), compiledchunk, aboolean);
                            renderEnv.setOverlaysRendered(false);
                        }
                    }
                }

                if (forgeHooksSetRenderLayerExists) {
                    Reflector.callVoid(Reflector.ForgeHooksClient_setRenderLayer, (Object)null);
                }
            }

            for (EnumWorldBlockLayer enumworldblocklayer : ENUM_WORLD_BLOCK_LAYERS) {
                if (aboolean[enumworldblocklayer.ordinal()]) {
                    compiledchunk.setLayerUsed(enumworldblocklayer);
                }

                if (compiledchunk.isLayerStarted(enumworldblocklayer)) {
                    if (Config.isShaders()) {
                        SVertexBuilder.calcNormalChunkLayer(generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer));
                    }

                    WorldRenderer bufferBuilder = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer);
                    this.postRenderBlocks(enumworldblocklayer, x, y, z, bufferBuilder, compiledchunk);
                    if (bufferBuilder.animatedSprites != null) {
                        compiledchunk.setAnimatedSprites(enumworldblocklayer, (BitSet)bufferBuilder.animatedSprites.clone());
                    }
                } else {
                    compiledchunk.setAnimatedSprites(enumworldblocklayer, null);
                }
            }

            blockAccess.renderFinish();
        }

        compiledchunk.setVisibility(lvt_10_1_.computeVisibility());
        this.lockCompileTask.lock();

        try {
            Set<TileEntity> set = Sets.newHashSet(lvt_11_1_);
            Set<TileEntity> set1 = Sets.newHashSet(this.setTileEntities);
            set.removeAll(this.setTileEntities);
            set1.removeAll(lvt_11_1_);
            this.setTileEntities.clear();
            this.setTileEntities.addAll(lvt_11_1_);
            this.renderGlobal.updateTileEntities(set1, set);
        } finally {
            this.lockCompileTask.unlock();
        }
    }

    protected void finishCompileTask() {
        this.lockCompileTask.lock();

        try {
            if (this.compileTask != null && this.compileTask.getStatus() != Status.DONE) {
                this.compileTask.finish();
                this.compileTask = null;
            }
        } finally {
            this.lockCompileTask.unlock();
        }
    }

    public ReentrantLock getLockCompileTask() {
        return this.lockCompileTask;
    }

    public ChunkCompileTaskGenerator makeCompileTaskChunk() {
        this.lockCompileTask.lock();

        ChunkCompileTaskGenerator chunkcompiletaskgenerator;
        try {
            this.finishCompileTask();
            this.compileTask = new ChunkCompileTaskGenerator(this, Type.REBUILD_CHUNK);
            chunkcompiletaskgenerator = this.compileTask;
        } finally {
            this.lockCompileTask.unlock();
        }

        return chunkcompiletaskgenerator;
    }

    public ChunkCompileTaskGenerator makeCompileTaskTransparency() {
        this.lockCompileTask.lock();

        ChunkCompileTaskGenerator var2;
        try {
            if (this.compileTask != null && this.compileTask.getStatus() == Status.PENDING) {
                return null;
            }

            if (this.compileTask != null && this.compileTask.getStatus() != Status.DONE) {
                this.compileTask.finish();
                this.compileTask = null;
            }

            this.compileTask = new ChunkCompileTaskGenerator(this, Type.RESORT_TRANSPARENCY);
            this.compileTask.setCompiledChunk(this.compiledChunk);
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = this.compileTask;
            var2 = chunkcompiletaskgenerator;
        } finally {
            this.lockCompileTask.unlock();
        }

        return var2;
    }

    private void preRenderBlocks(WorldRenderer worldRendererIn, BlockPos pos) {
        worldRendererIn.begin(7, DefaultVertexFormats.BLOCK);
        if (Config.isRenderRegions()) {
            int bits = 8;
            int dx = pos.getX() >> bits << bits;
            int dy = pos.getY() >> bits << bits;
            int dz = pos.getZ() >> bits << bits;
            dx = this.regionX;
            dz = this.regionZ;
            worldRendererIn.setTranslation(-dx, -dy, -dz);
        } else {
            worldRendererIn.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
        }
    }

    private void postRenderBlocks(EnumWorldBlockLayer layer, float x, float y, float z, WorldRenderer worldRendererIn, CompiledChunk compiledChunkIn) {
        if (layer == EnumWorldBlockLayer.TRANSLUCENT && !compiledChunkIn.isLayerEmpty(layer)) {
            worldRendererIn.sortVertexData(x, y, z);
            compiledChunkIn.setState(worldRendererIn.getVertexState());
        }

        worldRendererIn.finishDrawing();
    }

    private void initModelviewMatrix() {
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        float f = 1.000001F;
        GlStateManager.translate(-8.0F, -8.0F, -8.0F);
        GlStateManager.scale(f, f, f);
        GlStateManager.translate(8.0F, 8.0F, 8.0F);
        GlStateManager.getFloat(2982, this.modelviewMatrix);
        GlStateManager.popMatrix();
    }

    public void multModelviewMatrix() {
        GlStateManager.multMatrix(this.modelviewMatrix);
    }

    public CompiledChunk getCompiledChunk() {
        return this.compiledChunk;
    }

    public void setCompiledChunk(CompiledChunk compiledChunkIn) {
        this.lockCompiledChunk.lock();

        try {
            this.compiledChunk = compiledChunkIn;
        } finally {
            this.lockCompiledChunk.unlock();
        }
    }

    public void stopCompileTask() {
        this.finishCompileTask();
        this.compiledChunk = CompiledChunk.DUMMY;
    }

    public void deleteGlResources() {
        this.stopCompileTask();

        for (int i = 0; i < EnumWorldBlockLayer.values().length; i++) {
            if (this.vertexBuffers[i] != null) {
                this.vertexBuffers[i].deleteGlBuffers();
            }
        }
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public void setNeedsUpdate(boolean needsUpdateIn) {
        this.needsUpdate = needsUpdateIn;
        if (needsUpdateIn) {
            if (this.isWorldPlayerUpdate()) {
                this.playerUpdate = true;
            }
        } else {
            this.playerUpdate = false;
        }
    }

    public boolean isNeedsUpdate() {
        return this.needsUpdate;
    }

    public BlockPos getBlockPosOffset16(EnumFacing p_181701_1_) {
        return this.getPositionOffset16(p_181701_1_);
    }

    public BlockPos getPositionOffset16(EnumFacing facing) {
        int index = facing.getIndex();
        BlockPos posOffset = this.positionOffsets16[index];
        if (posOffset == null) {
            posOffset = this.getPosition().offset(facing, 16);
            this.positionOffsets16[index] = posOffset;
        }

        return posOffset;
    }

    private boolean isWorldPlayerUpdate() {
        if (this.world instanceof WorldClient) {
            WorldClient worldClient = (WorldClient)this.world;
            return worldClient.isPlayerUpdate();
        } else {
            return false;
        }
    }

    public boolean isPlayerUpdate() {
        return this.playerUpdate;
    }

    protected RegionRenderCache createRegionRenderCache(World world, BlockPos from, BlockPos to, int subtract) {
        return new RegionRenderCache(world, from, to, subtract);
    }

    private EnumWorldBlockLayer fixBlockLayer(IBlockState blockState, EnumWorldBlockLayer layer) {
        if (CustomBlockLayers.isActive()) {
            EnumWorldBlockLayer layerCustom = CustomBlockLayers.getRenderLayer(blockState);
            if (layerCustom != null) {
                return layerCustom;
            }
        }

        if (!this.fixBlockLayer) {
            return layer;
        } else {
            if (this.isMipmaps) {
                if (layer == EnumWorldBlockLayer.CUTOUT) {
                    Block block = blockState.getBlock();
                    if (block instanceof BlockRedstoneWire) {
                        return layer;
                    }

                    if (block instanceof BlockCactus) {
                        return layer;
                    }

                    return EnumWorldBlockLayer.CUTOUT_MIPPED;
                }
            } else if (layer == EnumWorldBlockLayer.CUTOUT_MIPPED) {
                return EnumWorldBlockLayer.CUTOUT;
            }

            return layer;
        }
    }

    private void postRenderOverlays(RegionRenderCacheBuilder regionRenderCacheBuilder, CompiledChunk compiledChunk, boolean[] layerFlags) {
        this.postRenderOverlay(EnumWorldBlockLayer.CUTOUT, regionRenderCacheBuilder, compiledChunk, layerFlags);
        this.postRenderOverlay(EnumWorldBlockLayer.CUTOUT_MIPPED, regionRenderCacheBuilder, compiledChunk, layerFlags);
        this.postRenderOverlay(EnumWorldBlockLayer.TRANSLUCENT, regionRenderCacheBuilder, compiledChunk, layerFlags);
    }

    private void postRenderOverlay(
        EnumWorldBlockLayer layer, RegionRenderCacheBuilder regionRenderCacheBuilder, CompiledChunk compiledchunk, boolean[] layerFlags
    ) {
        WorldRenderer bufferOverlay = regionRenderCacheBuilder.getWorldRendererByLayer(layer);
        if (bufferOverlay.isDrawing()) {
            compiledchunk.setLayerStarted(layer);
            layerFlags[layer.ordinal()] = true;
        }
    }

    private ChunkCacheOF makeChunkCacheOF(BlockPos posIn) {
        BlockPos posFrom = posIn.add(-1, -1, -1);
        BlockPos posTo = posIn.add(16, 16, 16);
        ChunkCache chunkCache = this.createRegionRenderCache(this.world, posFrom, posTo, 1);
        if (Reflector.MinecraftForgeClient_onRebuildChunk.exists()) {
            Reflector.call(Reflector.MinecraftForgeClient_onRebuildChunk, this.world, posIn, chunkCache);
        }

        return new ChunkCacheOF(chunkCache, posFrom, posTo, 1);
    }

    public RenderChunk getRenderChunkOffset16(ViewFrustum viewFrustum, EnumFacing facing) {
        if (!this.renderChunksOffset16Updated) {
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                EnumFacing ef = EnumFacing.VALUES[i];
                BlockPos posOffset16 = this.getBlockPosOffset16(ef);
                this.renderChunksOfset16[i] = viewFrustum.getRenderChunk(posOffset16);
            }

            this.renderChunksOffset16Updated = true;
        }

        return this.renderChunksOfset16[facing.ordinal()];
    }

    public Chunk getChunk() {
        return this.getChunk(this.position);
    }

    private Chunk getChunk(BlockPos posIn) {
        Chunk chunkLocal = this.chunk;
        if (chunkLocal != null && chunkLocal.isLoaded()) {
            return chunkLocal;
        } else {
            chunkLocal = this.world.getChunkFromBlockCoords(posIn);
            this.chunk = chunkLocal;
            return chunkLocal;
        }
    }

    public boolean isChunkRegionEmpty() {
        return this.isChunkRegionEmpty(this.position);
    }

    private boolean isChunkRegionEmpty(BlockPos posIn) {
        int yStart = posIn.getY();
        int yEnd = yStart + 15;
        return this.getChunk(posIn).getAreLevelsEmpty(yStart, yEnd);
    }

    public void setRenderChunkNeighbour(EnumFacing facing, RenderChunk neighbour) {
        this.renderChunkNeighbours[facing.ordinal()] = neighbour;
        this.renderChunkNeighboursValid[facing.ordinal()] = neighbour;
    }

    public RenderChunk getRenderChunkNeighbour(EnumFacing facing) {
        if (!this.renderChunkNeighboursUpated) {
            this.updateRenderChunkNeighboursValid();
        }

        return this.renderChunkNeighboursValid[facing.ordinal()];
    }

    public RenderGlobal.ContainerLocalRenderInformation getRenderInfo() {
        return this.renderInfo;
    }

    private void updateRenderChunkNeighboursValid() {
        int x = this.getPosition().getX();
        int z = this.getPosition().getZ();
        int north = EnumFacing.NORTH.ordinal();
        int south = EnumFacing.SOUTH.ordinal();
        int west = EnumFacing.WEST.ordinal();
        int east = EnumFacing.EAST.ordinal();
        this.renderChunkNeighboursValid[north] = this.renderChunkNeighbours[north].getPosition().getZ() == z - 16 ? this.renderChunkNeighbours[north] : null;
        this.renderChunkNeighboursValid[south] = this.renderChunkNeighbours[south].getPosition().getZ() == z + 16 ? this.renderChunkNeighbours[south] : null;
        this.renderChunkNeighboursValid[west] = this.renderChunkNeighbours[west].getPosition().getX() == x - 16 ? this.renderChunkNeighbours[west] : null;
        this.renderChunkNeighboursValid[east] = this.renderChunkNeighbours[east].getPosition().getX() == x + 16 ? this.renderChunkNeighbours[east] : null;
        this.renderChunkNeighboursUpated = true;
    }

    public boolean isBoundingBoxInFrustum(ICamera camera, int frameCount) {
        return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(camera, frameCount) ? true : camera.isBoundingBoxInFrustum(this.boundingBox);
    }

    public AabbFrame getBoundingBoxParent() {
        if (this.boundingBoxParent == null) {
            BlockPos pos = this.getPosition();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            int bits = 5;
            int xp = x >> bits << bits;
            int yp = y >> bits << bits;
            int zp = z >> bits << bits;
            if (xp != x || yp != y || zp != z) {
                AabbFrame bbp = this.renderGlobal.getRenderChunk(new BlockPos(xp, yp, zp)).getBoundingBoxParent();
                if (bbp != null && bbp.minX == xp && bbp.minY == yp && bbp.minZ == zp) {
                    this.boundingBoxParent = bbp;
                }
            }

            if (this.boundingBoxParent == null) {
                int delta = 1 << bits;
                this.boundingBoxParent = new AabbFrame(xp, yp, zp, xp + delta, yp + delta, zp + delta);
            }
        }

        return this.boundingBoxParent;
    }

    @Override
    public String toString() {
        return "pos: " + this.getPosition() + ", frameIndex: " + this.frameIndex;
    }
}
