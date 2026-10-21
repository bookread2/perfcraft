package net.minecraft.world.chunk.storage;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;
import net.optifine.reflect.Reflector;

public class ExtendedBlockStorage {
    private int yBase;
    private int blockRefCount;
    private int tickRefCount;
    private char[] data;
    private NibbleArray blocklightArray;
    private NibbleArray skylightArray;

    public ExtendedBlockStorage(int y, boolean storeSkylight) {
        this.yBase = y;
        this.data = new char[4096];
        this.blocklightArray = new NibbleArray();
        if (storeSkylight) {
            this.skylightArray = new NibbleArray();
        }
    }

    public IBlockState get(int x, int y, int z) {
        IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[y << 8 | z << 4 | x]);
        return iblockstate != null ? iblockstate : Blocks.air.getDefaultState();
    }

    public void set(int x, int y, int z, IBlockState state) {
        if (Reflector.IExtendedBlockState.isInstance(state)) {
            state = (IBlockState)Reflector.call(state, Reflector.IExtendedBlockState_getClean);
        }

        IBlockState iblockstate = this.get(x, y, z);
        Block block = iblockstate.getBlock();
        Block block1 = state.getBlock();
        if (block != Blocks.air) {
            this.blockRefCount--;
            if (block.getTickRandomly()) {
                this.tickRefCount--;
            }
        }

        if (block1 != Blocks.air) {
            this.blockRefCount++;
            if (block1.getTickRandomly()) {
                this.tickRefCount++;
            }
        }

        this.data[y << 8 | z << 4 | x] = (char)Block.BLOCK_STATE_IDS.get(state);
    }

    public Block getBlockByExtId(int x, int y, int z) {
        return this.get(x, y, z).getBlock();
    }

    public int getExtBlockMetadata(int x, int y, int z) {
        IBlockState iblockstate = this.get(x, y, z);
        return iblockstate.getBlock().getMetaFromState(iblockstate);
    }

    public boolean isEmpty() {
        return this.blockRefCount == 0;
    }

    public boolean getNeedsRandomTick() {
        return this.tickRefCount > 0;
    }

    public int getYLocation() {
        return this.yBase;
    }

    public void setExtSkylightValue(int x, int y, int z, int value) {
        this.skylightArray.set(x, y, z, value);
    }

    public int getExtSkylightValue(int x, int y, int z) {
        return this.skylightArray.get(x, y, z);
    }

    public void setExtBlocklightValue(int x, int y, int z, int value) {
        this.blocklightArray.set(x, y, z, value);
    }

    public int getExtBlocklightValue(int x, int y, int z) {
        return this.blocklightArray.get(x, y, z);
    }

    public void removeInvalidBlocks() {
        IBlockState STATE_AIR = Blocks.air.getDefaultState();
        int localBlockRefCount = 0;
        int localTickRefCount = 0;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Block block = this.getBlockByExtId(x, y, z);
                    if (block != Blocks.air) {
                        localBlockRefCount++;
                        if (block.getTickRandomly()) {
                            localTickRefCount++;
                        }
                    }
                }
            }
        }

        this.blockRefCount = localBlockRefCount;
        this.tickRefCount = localTickRefCount;
    }

    public char[] getData() {
        return this.data;
    }

    public void setData(char[] dataArray) {
        this.data = dataArray;
    }

    public NibbleArray getBlocklightArray() {
        return this.blocklightArray;
    }

    public NibbleArray getSkylightArray() {
        return this.skylightArray;
    }

    public void setBlocklightArray(NibbleArray newBlocklightArray) {
        this.blocklightArray = newBlocklightArray;
    }

    public void setSkylightArray(NibbleArray newSkylightArray) {
        this.skylightArray = newSkylightArray;
    }

    public int getBlockRefCount() {
        return this.blockRefCount;
    }
}
