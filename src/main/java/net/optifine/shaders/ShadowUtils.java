package net.optifine.shaders;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class ShadowUtils {
    public static Iterator<RenderChunk> makeShadowChunkIterator(
        WorldClient world, double partialTicks, Entity viewEntity, int renderDistanceChunks, ViewFrustum viewFrustum
    ) {
        float shadowRenderDistance = Shaders.getShadowRenderDistance();
        if (!(shadowRenderDistance <= 0.0F) && !(shadowRenderDistance >= (renderDistanceChunks - 1) * 16)) {
            int shadowDistanceChunks = MathHelper.ceiling_float_int(shadowRenderDistance / 16.0F) + 1;
            float car = world.getCelestialAngleRadians((float)partialTicks);
            float sunTiltRad = Shaders.sunPathRotation * MathHelper.deg2Rad;
            float sar = car > MathHelper.PId2 && car < 3.0F * MathHelper.PId2 ? car + MathHelper.PI : car;
            float dx = -MathHelper.sin(sar);
            float dy = MathHelper.cos(sar) * MathHelper.cos(sunTiltRad);
            float dz = -MathHelper.cos(sar) * MathHelper.sin(sunTiltRad);
            BlockPos posEntity = new BlockPos(
                MathHelper.floor_double(viewEntity.posX) >> 4, MathHelper.floor_double(viewEntity.posY) >> 4, MathHelper.floor_double(viewEntity.posZ) >> 4
            );
            BlockPos posStart = posEntity.add((double)(-dx * shadowDistanceChunks), (double)(-dy * shadowDistanceChunks), (double)(-dz * shadowDistanceChunks));
            BlockPos posEnd = posEntity.add((double)(dx * renderDistanceChunks), (double)(dy * renderDistanceChunks), (double)(dz * renderDistanceChunks));
            return new IteratorRenderChunks(viewFrustum, posStart, posEnd, shadowDistanceChunks, shadowDistanceChunks);
        } else {
            List<RenderChunk> listChunks = Arrays.asList(viewFrustum.renderChunks);
            return listChunks.iterator();
        }
    }
}
