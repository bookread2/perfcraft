package net.optifine.util;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.optifine.Config;

public class IntegratedServerUtils {
    public static WorldServer getWorldServer() {
        Minecraft mc = Config.getMinecraft();
        World world = mc.theWorld;
        if (world == null) {
            return null;
        } else if (!mc.isIntegratedServerRunning()) {
            return null;
        } else {
            IntegratedServer is = mc.getIntegratedServer();
            if (is == null) {
                return null;
            } else {
                WorldProvider wp = world.provider;
                if (wp == null) {
                    return null;
                } else {
                    int wd = wp.getDimensionId();

                    try {
                        return is.worldServerForDimension(wd);
                    } catch (NullPointerException var6) {
                        return null;
                    }
                }
            }
        }
    }

    public static Entity getEntity(UUID uuid) {
        WorldServer ws = getWorldServer();
        return ws == null ? null : ws.getEntityFromUuid(uuid);
    }

    public static TileEntity getTileEntity(BlockPos pos) {
        WorldServer ws = getWorldServer();
        if (ws == null) {
            return null;
        } else {
            Chunk chunk = ws.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getTileEntity(pos, EnumCreateEntityType.CHECK);
        }
    }
}
