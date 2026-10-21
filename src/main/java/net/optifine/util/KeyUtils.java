package net.optifine.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.settings.KeyBinding;

public class KeyUtils {
    public static void fixKeyConflicts(KeyBinding[] keys, KeyBinding[] keysPrio) {
        Set<Integer> keyPrioCodes = new HashSet<>();

        for (int i = 0; i < keysPrio.length; i++) {
            KeyBinding keyPrio = keysPrio[i];
            keyPrioCodes.add(keyPrio.getKeyCode());
        }

        Set<KeyBinding> setKeys = new HashSet<>(Arrays.asList(keys));
        setKeys.removeAll(Arrays.asList(keysPrio));

        for (KeyBinding key : setKeys) {
            Integer code = key.getKeyCode();
            if (keyPrioCodes.contains(code)) {
                key.setKeyCode(0);
            }
        }
    }
}
