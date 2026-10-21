package net.optifine.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorClass;
import net.optifine.reflect.ReflectorField;

public class ChunkUtils {
    private static ReflectorClass chunkClass = new ReflectorClass(Chunk.class);
    private static ReflectorField fieldHasEntities = findFieldHasEntities();
    private static ReflectorField fieldPrecipitationHeightMap = new ReflectorField(chunkClass, int[].class, 0);

    public static boolean hasEntities(Chunk chunk) {
        return Reflector.getFieldValueBoolean(chunk, fieldHasEntities, true);
    }

    public static int getPrecipitationHeight(Chunk chunk, BlockPos pos) {
        int[] precipitationHeightMap = (int[])Reflector.getFieldValue(chunk, fieldPrecipitationHeightMap);
        if (precipitationHeightMap != null && precipitationHeightMap.length == 256) {
            int cx = pos.getX() & 15;
            int cz = pos.getZ() & 15;
            int ix = cx | cz << 4;
            int y = precipitationHeightMap[ix];
            if (y >= 0) {
                return y;
            } else {
                BlockPos posPrep = chunk.getPrecipitationHeight(pos);
                return posPrep.getY();
            }
        } else {
            return -1;
        }
    }

    private static ReflectorField findFieldHasEntities() {
        try {
            Chunk chunk = new Chunk(null, 0, 0);
            List listBoolFields = new ArrayList();
            List listBoolValuesPre = new ArrayList();
            Field[] fields = Chunk.class.getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (field.getType() == boolean.class) {
                    field.setAccessible(true);
                    listBoolFields.add(field);
                    listBoolValuesPre.add(field.get(chunk));
                }
            }

            chunk.setHasEntities(false);
            List listBoolValuesFalse = new ArrayList();

            for (Field field : (Iterable<Field>) (Iterable) listBoolFields) {
                listBoolValuesFalse.add(field.get(chunk));
            }

            chunk.setHasEntities(true);
            List listBoolValuesTrue = new ArrayList();

            for (Field field : (Iterable<Field>) (Iterable) listBoolFields) {
                listBoolValuesTrue.add(field.get(chunk));
            }

            List listMatchingFields = new ArrayList();

            for (int ix = 0; ix < listBoolFields.size(); ix++) {
                Field field = (Field)listBoolFields.get(ix);
                Boolean valFalse = (Boolean)listBoolValuesFalse.get(ix);
                Boolean valTrue = (Boolean)listBoolValuesTrue.get(ix);
                if (!valFalse && valTrue) {
                    listMatchingFields.add(field);
                    Boolean valPre = (Boolean)listBoolValuesPre.get(ix);
                    field.set(chunk, valPre);
                }
            }

            if (listMatchingFields.size() == 1) {
                Field field = (Field)listMatchingFields.get(0);
                return new ReflectorField(field);
            }
        } catch (Exception var12) {
            Config.warn(var12.getClass().getName() + " " + var12.getMessage());
        }

        Config.warn("Error finding Chunk.hasEntities");
        return new ReflectorField(new ReflectorClass(Chunk.class), "hasEntities");
    }
}
