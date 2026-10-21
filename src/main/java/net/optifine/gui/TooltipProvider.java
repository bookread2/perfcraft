package net.optifine.gui;

import java.awt.Rectangle;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public interface TooltipProvider {
    Rectangle getTooltipBounds(GuiScreen guiScreen, int i, int j);

    String[] getTooltipLines(GuiButton guiButton, int i);

    boolean isRenderBorder();
}
