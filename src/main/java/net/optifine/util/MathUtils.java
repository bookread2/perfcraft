package net.optifine.util;

import net.minecraft.util.MathHelper;

public class MathUtils {
    public static final float PI = (float) Math.PI;
    public static final float PI2 = (float) (Math.PI * 2);
    public static final float PId2 = (float) (Math.PI / 2);
    private static final float[] ASIN_TABLE = new float[65536];

    public static float asin(float value) {
        return ASIN_TABLE[(int)((value + 1.0F) * 32767.5) & 65535];
    }

    public static float acos(float value) {
        return (float) (Math.PI / 2) - ASIN_TABLE[(int)((value + 1.0F) * 32767.5) & 65535];
    }

    public static int getAverage(int[] vals) {
        if (vals.length <= 0) {
            return 0;
        } else {
            int sum = getSum(vals);
            return sum / vals.length;
        }
    }

    public static int getSum(int[] vals) {
        if (vals.length <= 0) {
            return 0;
        } else {
            int sum = 0;

            for (int i = 0; i < vals.length; i++) {
                int val = vals[i];
                sum += val;
            }

            return sum;
        }
    }

    public static int roundDownToPowerOfTwo(int val) {
        int po2 = MathHelper.roundUpToPowerOfTwo(val);
        return val == po2 ? po2 : po2 / 2;
    }

    public static boolean equalsDelta(float f1, float f2, float delta) {
        return Math.abs(f1 - f2) <= delta;
    }

    public static float toDeg(float angle) {
        return angle * 180.0F / MathHelper.PI;
    }

    public static float toRad(float angle) {
        return angle / 180.0F * MathHelper.PI;
    }

    public static float roundToFloat(double d) {
        return (float)(Math.round(d * 1.0E8) / 1.0E8);
    }

    static {
        for (int i = 0; i < 65536; i++) {
            ASIN_TABLE[i] = (float)Math.asin(i / 32767.5 - 1.0);
        }

        for (int i = -1; i < 2; i++) {
            ASIN_TABLE[(int)((i + 1.0) * 32767.5) & 65535] = (float)Math.asin(i);
        }
    }
}
