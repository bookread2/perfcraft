package net.optifine.reflect;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.optifine.Config;

public class FieldLocatorActionKeyF3 implements IFieldLocator {
    @Override
    public Field getField() {
        Class mcClass = Minecraft.class;
        Field fieldRenderChunksMany = this.getFieldRenderChunksMany();
        if (fieldRenderChunksMany == null) {
            Config.log("(Reflector) Field not present: " + mcClass.getName() + ".actionKeyF3 (field renderChunksMany not found)");
            return null;
        } else {
            Field fieldActionkeyF3 = ReflectorRaw.getFieldAfter(Minecraft.class, fieldRenderChunksMany, boolean.class, 0);
            if (fieldActionkeyF3 == null) {
                Config.log("(Reflector) Field not present: " + mcClass.getName() + ".actionKeyF3");
                return null;
            } else {
                return fieldActionkeyF3;
            }
        }
    }

    private Field getFieldRenderChunksMany() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean oldRenderChunksMany = mc.renderChunksMany;
        Field[] fields = Minecraft.class.getDeclaredFields();
        mc.renderChunksMany = true;
        Field[] fieldsTrue = ReflectorRaw.getFields(mc, fields, boolean.class, Boolean.TRUE);
        mc.renderChunksMany = false;
        Field[] fieldsFalse = ReflectorRaw.getFields(mc, fields, boolean.class, Boolean.FALSE);
        mc.renderChunksMany = oldRenderChunksMany;
        Set<Field> setTrue = new HashSet<>(Arrays.asList(fieldsTrue));
        Set<Field> setFalse = new HashSet<>(Arrays.asList(fieldsFalse));
        Set<Field> setFields = new HashSet<>(setTrue);
        setFields.retainAll(setFalse);
        Field[] fs = setFields.toArray(new Field[setFields.size()]);
        return fs.length != 1 ? null : fs[0];
    }
}
