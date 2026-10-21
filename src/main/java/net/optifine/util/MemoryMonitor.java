package net.optifine.util;

public class MemoryMonitor {
    private static long startTimeMs = System.currentTimeMillis();
    private static long startMemory = getMemoryUsed();
    private static long lastTimeMs = startTimeMs;
    private static long lastMemory = startMemory;
    private static boolean gcEvent = false;
    private static int memBytesSec = 0;
    private static long MB = 1048576L;

    public static void update() {
        long timeMs = System.currentTimeMillis();
        long memory = getMemoryUsed();
        gcEvent = memory < lastMemory;
        if (gcEvent) {
            long timeDiffMs = lastTimeMs - startTimeMs;
            long memoryDiff = lastMemory - startMemory;
            double timeDiffSec = timeDiffMs / 1000.0;
            int bytesPerSec = (int)(memoryDiff / timeDiffSec);
            if (bytesPerSec > 0) {
                memBytesSec = bytesPerSec;
            }

            startTimeMs = timeMs;
            startMemory = memory;
        }

        lastTimeMs = timeMs;
        lastMemory = memory;
    }

    private static long getMemoryUsed() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    public static long getStartTimeMs() {
        return startTimeMs;
    }

    public static long getStartMemoryMb() {
        return startMemory / MB;
    }

    public static boolean isGcEvent() {
        return gcEvent;
    }

    public static long getAllocationRateMb() {
        return memBytesSec / MB;
    }
}
