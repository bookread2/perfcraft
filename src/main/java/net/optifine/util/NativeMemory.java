package net.optifine.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.optifine.Config;

public class NativeMemory {
    private static LongSupplier bufferAllocatedSupplier = makeLongSupplier(
        new String[][]{
            {"sun.misc.SharedSecrets", "getJavaNioAccess", "getDirectBufferPool", "getMemoryUsed"},
            {"jdk.internal.misc.SharedSecrets", "getJavaNioAccess", "getDirectBufferPool", "getMemoryUsed"}
        }
    );
    private static LongSupplier bufferMaximumSupplier = makeLongSupplier(
        new String[][]{{"sun.misc.VM", "maxDirectMemory"}, {"jdk.internal.misc.VM", "maxDirectMemory"}}
    );

    public static long getBufferAllocated() {
        return bufferAllocatedSupplier == null ? -1L : bufferAllocatedSupplier.getAsLong();
    }

    public static long getBufferMaximum() {
        return bufferMaximumSupplier == null ? -1L : bufferMaximumSupplier.getAsLong();
    }

    private static LongSupplier makeLongSupplier(String[][] paths) {
        List<Throwable> exceptions = new ArrayList<>();

        for (int i = 0; i < paths.length; i++) {
            String[] path = paths[i];

            try {
                return makeLongSupplier(path);
            } catch (Throwable var5) {
                exceptions.add(var5);
            }
        }

        for (Throwable t : exceptions) {
            Config.warn("" + t.getClass().getName() + ": " + t.getMessage());
        }

        return null;
    }

    private static LongSupplier makeLongSupplier(String[] path) throws Exception {
        if (path.length < 2) {
            return null;
        } else {
            Class cls = Class.forName(path[0]);
            Method method = cls.getMethod(path[1]);
            method.setAccessible(true);
            Object object = null;

            for (int i = 2; i < path.length; i++) {
                String name = path[i];
                object = method.invoke(object);
                method = object.getClass().getMethod(name);
                method.setAccessible(true);
            }

            final Method methodF = method;
            final Object objectF = object;
            return new LongSupplier() {
                private boolean disabled = false;

                @Override
                public long getAsLong() {
                    if (this.disabled) {
                        return -1L;
                    } else {
                        try {
                            return (Long)methodF.invoke(objectF);
                        } catch (Throwable var2) {
                            Config.warn("" + var2.getClass().getName() + ": " + var2.getMessage());
                            this.disabled = true;
                            return -1L;
                        }
                    }
                }
            };
        }
    }
}
