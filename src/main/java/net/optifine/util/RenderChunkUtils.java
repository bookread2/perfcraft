package net.optifine.util;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class RenderChunkUtils {
    public static int getCountBlocks(RenderChunk renderChunk) {
        ExtendedBlockStorage[] ebss = renderChunk.getChunk().getBlockStorageArray();
        if (ebss == null) {
            return 0;
        } else {
            int indexEbs = renderChunk.getPosition().getY() >> 4;
            ExtendedBlockStorage ebs = ebss[indexEbs];
            return ebs == null ? 0 : ebs.getBlockRefCount();
        }
    }

    public static double getRelativeBufferSize(RenderChunk renderChunk) {
        int blockCount = getCountBlocks(renderChunk);
        return getRelativeBufferSize(blockCount);
    }

    public static double getRelativeBufferSize(int blockCount) {
        double countRel = blockCount / 4096.0;
        countRel *= 0.995;
        double weight = countRel * 2.0 - 1.0;
        weight = MathHelper.clamp_double(weight, -1.0, 1.0);
        return MathHelper.sqrt_double(1.0 - weight * weight);
    }
}
