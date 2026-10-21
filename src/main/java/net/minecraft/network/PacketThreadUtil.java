package net.minecraft.network;

import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.IThreadListener;
import net.optifine.Config;

public class PacketThreadUtil {
    public static int lastDimensionId = Integer.MIN_VALUE;

    public static <T extends INetHandler> void checkThreadAndEnqueue(Packet<T> packetIn, T processor, IThreadListener scheduler) throws ThreadQuickExitException {
        if (!scheduler.isCallingFromMinecraftThread()) {
            scheduler.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    PacketThreadUtil.clientPreProcessPacket(packetIn);
                    packetIn.processPacket(processor);
                }
            });
            throw ThreadQuickExitException.INSTANCE;
        } else {
            clientPreProcessPacket(packetIn);
        }
    }

    protected static void clientPreProcessPacket(Packet packetIn) {
        if (packetIn instanceof S08PacketPlayerPosLook) {
            Config.getRenderGlobal().onPlayerPositionSet();
        }

        if (packetIn instanceof S07PacketRespawn) {
            S07PacketRespawn respawn = (S07PacketRespawn)packetIn;
            lastDimensionId = respawn.getDimensionID();
        } else if (packetIn instanceof S01PacketJoinGame) {
            S01PacketJoinGame joinGame = (S01PacketJoinGame)packetIn;
            lastDimensionId = joinGame.getDimension();
        } else {
            lastDimensionId = Integer.MIN_VALUE;
        }
    }
}
