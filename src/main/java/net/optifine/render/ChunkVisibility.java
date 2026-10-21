package net.optifine.render;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkVisibility {
    public static final int MASK_FACINGS = 63;
    public static final EnumFacing[][] enumFacingArrays = makeEnumFacingArrays(false);
    public static final EnumFacing[][] enumFacingOppositeArrays = makeEnumFacingArrays(true);
    private static int counter = 0;
    private static int iMaxStatic = -1;
    private static int iMaxStaticFinal = 16;
    private static World worldLast = null;
    private static int pcxLast = Integer.MIN_VALUE;
    private static int pczLast = Integer.MIN_VALUE;

    public static int getMaxChunkY(World world, Entity viewEntity, int renderDistanceChunks) {
        int pcx = MathHelper.floor_double(viewEntity.posX) >> 4;
        int pcy = MathHelper.floor_double(viewEntity.posY) >> 4;
        int pcz = MathHelper.floor_double(viewEntity.posZ) >> 4;
        Chunk playerChunk = world.getChunkFromChunkCoords(pcx, pcz);
        int cxStart = pcx - renderDistanceChunks;
        int cxEnd = pcx + renderDistanceChunks;
        int czStart = pcz - renderDistanceChunks;
        int czEnd = pcz + renderDistanceChunks;
        if (world != worldLast || pcx != pcxLast || pcz != pczLast) {
            counter = 0;
            iMaxStaticFinal = 16;
            worldLast = world;
            pcxLast = pcx;
            pczLast = pcz;
        }

        if (counter == 0) {
            iMaxStatic = -1;
        }

        int iMax = iMaxStatic;
        switch (counter) {
            case 0:
                cxEnd = pcx;
                czEnd = pcz;
                break;
            case 1:
                cxStart = pcx;
                czEnd = pcz;
                break;
            case 2:
                cxEnd = pcx;
                czStart = pcz;
                break;
            case 3:
                cxStart = pcx;
                czStart = pcz;
        }

        for (int cx = cxStart; cx < cxEnd; cx++) {
            for (int cz = czStart; cz < czEnd; cz++) {
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                if (!chunk.isEmpty()) {
                    ExtendedBlockStorage[] ebss = chunk.getBlockStorageArray();

                    for (int i = ebss.length - 1; i > iMax; i--) {
                        ExtendedBlockStorage ebs = ebss[i];
                        if (ebs != null && !ebs.isEmpty()) {
                            if (i > iMax) {
                                iMax = i;
                            }
                            break;
                        }
                    }

                    try {
                        Map<BlockPos, TileEntity> mapTileEntities = chunk.getTileEntityMap();
                        if (!mapTileEntities.isEmpty()) {
                            for (BlockPos pos : mapTileEntities.keySet()) {
                                int ix = pos.getY() >> 4;
                                if (ix > iMax) {
                                    iMax = ix;
                                }
                            }
                        }
                    } catch (ConcurrentModificationException var21) {
                    }

                    ClassInheritanceMultiMap<Entity>[] cimms = chunk.getEntityLists();

                    for (int ix = cimms.length - 1; ix > iMax; ix--) {
                        ClassInheritanceMultiMap<Entity> cimm = cimms[ix];
                        if (!cimm.isEmpty() && (chunk != playerChunk || ix != pcy || cimm.size() != 1)) {
                            if (ix > iMax) {
                                iMax = ix;
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (counter < 3) {
            iMaxStatic = iMax;
            iMax = iMaxStaticFinal;
        } else {
            iMaxStaticFinal = iMax;
            iMaxStatic = -1;
        }

        counter = (counter + 1) % 4;
        return iMax << 4;
    }

    public static boolean isFinished() {
        return counter == 0;
    }

    private static EnumFacing[][] makeEnumFacingArrays(boolean opposite) {
        int count = 64;
        EnumFacing[][] arrs = new EnumFacing[count][];

        for (int i = 0; i < count; i++) {
            List<EnumFacing> list = new ArrayList<>();

            for (int ix = 0; ix < EnumFacing.VALUES.length; ix++) {
                EnumFacing facing = EnumFacing.VALUES[ix];
                EnumFacing facingMask = opposite ? facing.getOpposite() : facing;
                int mask = 1 << facingMask.ordinal();
                if ((i & mask) != 0) {
                    list.add(facing);
                }
            }

            EnumFacing[] fs = list.toArray(new EnumFacing[list.size()]);
            arrs[i] = fs;
        }

        return arrs;
    }

    public static EnumFacing[] getFacingsNotOpposite(int setDisabled) {
        int index = ~setDisabled & 63;
        return enumFacingOppositeArrays[index];
    }

    public static void reset() {
        worldLast = null;
    }
}
