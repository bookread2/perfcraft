package net.optifine;

import java.util.Comparator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;

public class ChunkPosComparator implements Comparator<ChunkCoordIntPair> {
    private int chunkPosX;
    private int chunkPosZ;
    private double yawRad;
    private double pitchNorm;

    public ChunkPosComparator(int chunkPosX, int chunkPosZ, double yawRad, double pitchRad) {
        this.chunkPosX = chunkPosX;
        this.chunkPosZ = chunkPosZ;
        this.yawRad = yawRad;
        this.pitchNorm = 1.0 - MathHelper.clamp_double(Math.abs(pitchRad) / (Math.PI / 2), 0.0, 1.0);
    }

    public int compare(ChunkCoordIntPair cp1, ChunkCoordIntPair cp2) {
        int distSq1 = this.getDistSq(cp1);
        int distSq2 = this.getDistSq(cp2);
        return distSq1 - distSq2;
    }

    private int getDistSq(ChunkCoordIntPair cp) {
        int dx = cp.chunkXPos - this.chunkPosX;
        int dz = cp.chunkZPos - this.chunkPosZ;
        int distSq = dx * dx + dz * dz;
        double yaw = MathHelper.atan2(dz, dx);
        double dYaw = Math.abs(yaw - this.yawRad);
        if (dYaw > Math.PI) {
            dYaw = (Math.PI * 2) - dYaw;
        }

        return (int)(distSq * (1000.0 * (this.pitchNorm * dYaw * dYaw)));
    }
}
