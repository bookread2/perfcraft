package net.optifine.shaders.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.optifine.Config;
import net.optifine.Lang;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Shaders;
import net.optifine.util.ResUtils;

class GuiSlotShaders extends GuiSlot {
    private ArrayList shaderslist;
    private int selectedIndex;
    private long lastClickedCached = 0L;
    final GuiShaders shadersGui;

    public GuiSlotShaders(GuiShaders par1GuiShaders, int width, int height, int top, int bottom, int slotHeight) {
        super(par1GuiShaders.getMc(), width, height, top, bottom, slotHeight);
        this.shadersGui = par1GuiShaders;
        this.updateList();
        this.amountScrolled = 0.0F;
        int posYSelected = this.selectedIndex * slotHeight;
        int wMid = (bottom - top) / 2;
        if (posYSelected > wMid) {
            this.scrollBy(posYSelected - wMid);
        }
    }

    @Override
    public int getListWidth() {
        return this.width - 20;
    }

    public void updateList() {
        this.shaderslist = Shaders.listOfShaders();
        this.selectedIndex = 0;
        int i = 0;

        for (int n = this.shaderslist.size(); i < n; i++) {
            if (((String)this.shaderslist.get(i)).equals(Shaders.currentShaderName)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    @Override
    protected int getSize() {
        return this.shaderslist.size();
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        if (slotIndex != this.selectedIndex || this.lastClicked != this.lastClickedCached) {
            String name = (String)this.shaderslist.get(slotIndex);
            IShaderPack sp = Shaders.getShaderPack(name);
            if (this.checkCompatible(sp, slotIndex)) {
                this.selectIndex(slotIndex);
            }
        }
    }

    private void selectIndex(int index) {
        this.selectedIndex = index;
        this.lastClickedCached = this.lastClicked;
        Shaders.setShaderPack((String)this.shaderslist.get(index));
        Shaders.uninit();
        this.shadersGui.updateButtons();
    }

    private boolean checkCompatible(IShaderPack sp, int index) {
        if (sp == null) {
            return true;
        } else {
            InputStream in = sp.getResourceAsStream("/shaders/shaders.properties");
            Properties props = ResUtils.readProperties(in, "Shaders");
            if (props == null) {
                return true;
            } else {
                String keyVer = "version.1.8.9";
                String relMin = props.getProperty(keyVer);
                if (relMin == null) {
                    return true;
                } else {
                    relMin = relMin.trim();
                    String rel = "M6_pre1";
                    int res = Config.compareRelease(rel, relMin);
                    if (res >= 0) {
                        return true;
                    } else {
                        String verMin = ("HD_U_" + relMin).replace('_', ' ');
                        String msg1 = I18n.format("of.message.shaders.nv1", verMin);
                        String msg2 = I18n.format("of.message.shaders.nv2");
                        final int theIndex = index;
                        GuiYesNoCallback callback = new GuiYesNoCallback() {
                            @Override
                            public void confirmClicked(boolean result, int id) {
                                if (result) {
                                    GuiSlotShaders.this.selectIndex(theIndex);
                                }

                                GuiSlotShaders.this.mc.displayGuiScreen(GuiSlotShaders.this.shadersGui);
                            }
                        };
                        GuiYesNo guiYesNo = new GuiYesNo(callback, msg1, msg2, 0);
                        this.mc.displayGuiScreen(guiYesNo);
                        return false;
                    }
                }
            }
        }
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return slotIndex == this.selectedIndex;
    }

    @Override
    protected int getScrollBarX() {
        return this.width - 6;
    }

    @Override
    protected int getContentHeight() {
        return this.getSize() * 18;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void drawSlot(int entryID, int posX, int posY, int contentY, int mouseXIn, int mouseYIn) {
        String label = (String)this.shaderslist.get(entryID);
        if (label.equals("OFF")) {
            label = Lang.get("of.options.shaders.packNone");
        } else if (label.equals("(internal)")) {
            label = Lang.get("of.options.shaders.packDefault");
        }

        this.shadersGui.drawCenteredString(label, this.width / 2, posY + 1, 14737632);
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }
}
