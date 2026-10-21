package net.optifine.config;

import net.minecraft.enchantment.Enchantment;

public class ParserEnchantmentId implements IParserInt {
    @Override
    public int parse(String str, int defVal) {
        Enchantment en = Enchantment.getEnchantmentByLocation(str);
        return en == null ? defVal : en.effectId;
    }
}
