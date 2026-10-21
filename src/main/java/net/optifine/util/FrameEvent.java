package net.optifine.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

public class FrameEvent {
    private static Map<String, Integer> mapEventFrames = new HashMap<>();

    public static boolean isActive(String name, int frameInterval) {
        synchronized (mapEventFrames) {
            int frameCount = Minecraft.getMinecraft().entityRenderer.frameCount;
            Integer frameCountLastObj = mapEventFrames.get(name);
            if (frameCountLastObj == null) {
                frameCountLastObj = new Integer(frameCount);
                mapEventFrames.put(name, frameCountLastObj);
            }

            int frameCountLast = frameCountLastObj;
            if (frameCount > frameCountLast && frameCount < frameCountLast + frameInterval) {
                return false;
            } else {
                mapEventFrames.put(name, new Integer(frameCount));
                return true;
            }
        }
    }
}
