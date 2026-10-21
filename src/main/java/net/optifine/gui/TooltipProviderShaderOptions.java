package net.optifine.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.GameSettings;
import net.optifine.Config;
import net.optifine.Lang;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.gui.GuiButtonShaderOption;
import net.optifine.util.StrUtils;

public class TooltipProviderShaderOptions extends TooltipProviderOptions {
    @Override
    public String[] getTooltipLines(GuiButton btn, int width) {
        if (!(btn instanceof GuiButtonShaderOption)) {
            return null;
        } else {
            GuiButtonShaderOption btnSo = (GuiButtonShaderOption)btn;
            ShaderOption so = btnSo.getShaderOption();
            return this.makeTooltipLines(so, width);
        }
    }

    private String[] makeTooltipLines(ShaderOption so, int width) {
        String name = so.getNameText();
        String desc = Config.normalize(so.getDescriptionText()).trim();
        String[] descs = this.splitDescription(desc);
        GameSettings settings = Config.getGameSettings();
        String id = null;
        if (!name.equals(so.getName()) && settings.advancedItemTooltips) {
            id = "§8" + Lang.get("of.general.id") + ": " + so.getName();
        }

        String source = null;
        if (so.getPaths() != null && settings.advancedItemTooltips) {
            source = "§8" + Lang.get("of.general.from") + ": " + Config.arrayToString((Object[])so.getPaths());
        }

        String def = null;
        if (so.getValueDefault() != null && settings.advancedItemTooltips) {
            String defVal = so.isEnabled() ? so.getValueText(so.getValueDefault()) : Lang.get("of.general.ambiguous");
            def = "§8" + Lang.getDefault() + ": " + defVal;
        }

        List<String> list = new ArrayList<>();
        list.add(name);
        list.addAll(Arrays.asList(descs));
        if (id != null) {
            list.add(id);
        }

        if (source != null) {
            list.add(source);
        }

        if (def != null) {
            list.add(def);
        }

        return this.makeTooltipLines(width, list);
    }

    private String[] splitDescription(String desc) {
        if (desc.length() <= 0) {
            return new String[0];
        } else {
            desc = StrUtils.removePrefix(desc, "//");
            String[] descs = desc.split("\\. ");

            for (int i = 0; i < descs.length; i++) {
                descs[i] = "- " + descs[i].trim();
                descs[i] = StrUtils.removeSuffix(descs[i], ".");
            }

            return descs;
        }
    }

    private String[] makeTooltipLines(int width, List<String> args) {
        FontRenderer fr = Config.getMinecraft().fontRendererObj;
        List<String> list = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg != null && arg.length() > 0) {
                for (String part : fr.listFormattedStringToWidth(arg, width)) {
                    list.add(part);
                }
            }
        }

        return list.toArray(new String[list.size()]);
    }
}
