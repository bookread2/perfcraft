package net.optifine.shaders.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import net.optifine.Config;
import net.optifine.Lang;
import net.optifine.gui.GuiScreenOF;
import net.optifine.gui.TooltipManager;
import net.optifine.gui.TooltipProviderShaderOptions;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderOptionProfile;
import net.optifine.shaders.config.ShaderOptionScreen;

public class GuiShaderOptions extends GuiScreenOF {
    private GuiScreen prevScreen;
    protected String title;
    private GameSettings settings;
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderShaderOptions());
    private String screenName = null;
    private String screenText = null;
    private boolean changed = false;
    public static final String OPTION_PROFILE = "<profile>";
    public static final String OPTION_EMPTY = "<empty>";
    public static final String OPTION_REST = "*";

    public GuiShaderOptions(GuiScreen guiscreen, GameSettings gamesettings) {
        this.title = "Shader Options";
        this.prevScreen = guiscreen;
        this.settings = gamesettings;
    }

    public GuiShaderOptions(GuiScreen guiscreen, GameSettings gamesettings, String screenName) {
        this(guiscreen, gamesettings);
        this.screenName = screenName;
        if (screenName != null) {
            this.screenText = Shaders.translate("screen." + screenName, screenName);
        }
    }

    @Override
    public void initGui() {
        this.title = I18n.format("of.options.shaderOptionsTitle");
        int baseId = 100;
        int baseX = 0;
        int baseY = 30;
        int stepY = 20;
        int btnWidth = 120;
        int btnHeight = 20;
        int columns = Shaders.getShaderPackColumns(this.screenName, 2);
        ShaderOption[] ops = Shaders.getShaderPackOptions(this.screenName);
        if (ops != null) {
            int colsMin = MathHelper.ceiling_double_int(ops.length / 9.0);
            if (columns < colsMin) {
                columns = colsMin;
            }

            for (int i = 0; i < ops.length; i++) {
                ShaderOption so = ops[i];
                if (so != null && so.isVisible()) {
                    int col = i % columns;
                    int row = i / columns;
                    int colWidth = Math.min(this.width / columns, 200);
                    baseX = (this.width - colWidth * columns) / 2;
                    int x = col * colWidth + 5 + baseX;
                    int y = baseY + row * stepY;
                    int w = colWidth - 10;
                    String text = getButtonText(so, w);
                    GuiButtonShaderOption btn;
                    if (Shaders.isShaderPackOptionSlider(so.getName())) {
                        btn = new GuiSliderShaderOption(baseId + i, x, y, w, btnHeight, so, text);
                    } else {
                        btn = new GuiButtonShaderOption(baseId + i, x, y, w, btnHeight, so, text);
                    }

                    btn.enabled = so.isEnabled();
                    this.buttonList.add(btn);
                }
            }
        }

        this.buttonList.add(new GuiButton(201, this.width / 2 - btnWidth - 20, this.height / 6 + 168 + 11, btnWidth, btnHeight, I18n.format("controls.reset")));
        this.buttonList.add(new GuiButton(200, this.width / 2 + 20, this.height / 6 + 168 + 11, btnWidth, btnHeight, I18n.format("gui.done")));
    }

    public static String getButtonText(ShaderOption so, int btnWidth) {
        String labelName = so.getNameText();
        if (so instanceof ShaderOptionScreen) {
            ShaderOptionScreen soScr = (ShaderOptionScreen)so;
            return labelName + "...";
        } else {
            FontRenderer fr = Config.getMinecraft().fontRendererObj;
            int lenSuffix = fr.getStringWidth(": " + Lang.getOff()) + 5;

            while (fr.getStringWidth(labelName) + lenSuffix >= btnWidth && labelName.length() > 0) {
                labelName = labelName.substring(0, labelName.length() - 1);
            }

            String col = so.isChanged() ? so.getValueColor(so.getValue()) : "";
            String labelValue = so.getValueText(so.getValue());
            return labelName + ": " + col + labelValue;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id < 200 && button instanceof GuiButtonShaderOption) {
                GuiButtonShaderOption btnSo = (GuiButtonShaderOption)button;
                ShaderOption so = btnSo.getShaderOption();
                if (so instanceof ShaderOptionScreen) {
                    String screenName = so.getName();
                    GuiShaderOptions scr = new GuiShaderOptions(this, this.settings, screenName);
                    this.mc.displayGuiScreen(scr);
                    return;
                }

                if (isShiftKeyDown()) {
                    so.resetValue();
                } else if (btnSo.isSwitchable()) {
                    so.nextValue();
                }

                this.updateAllButtons();
                this.changed = true;
            }

            if (button.id == 201) {
                ShaderOption[] opts = Shaders.getChangedOptions(Shaders.getShaderPackOptions());

                for (int i = 0; i < opts.length; i++) {
                    ShaderOption opt = opts[i];
                    opt.resetValue();
                    this.changed = true;
                }

                this.updateAllButtons();
            }

            if (button.id == 200) {
                if (this.changed) {
                    Shaders.saveShaderPackOptions();
                    this.changed = false;
                    Shaders.uninit();
                }

                this.mc.displayGuiScreen(this.prevScreen);
            }
        }
    }

    @Override
    protected void actionPerformedRightClick(GuiButton btn) {
        if (btn instanceof GuiButtonShaderOption) {
            GuiButtonShaderOption btnSo = (GuiButtonShaderOption)btn;
            ShaderOption so = btnSo.getShaderOption();
            if (isShiftKeyDown()) {
                so.resetValue();
            } else if (btnSo.isSwitchable()) {
                so.prevValue();
            }

            this.updateAllButtons();
            this.changed = true;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (this.changed) {
            Shaders.saveShaderPackOptions();
            this.changed = false;
            Shaders.uninit();
        }
    }

    private void updateAllButtons() {
        for (GuiButton btn : this.buttonList) {
            if (btn instanceof GuiButtonShaderOption) {
                GuiButtonShaderOption gbso = (GuiButtonShaderOption)btn;
                ShaderOption opt = gbso.getShaderOption();
                if (opt instanceof ShaderOptionProfile) {
                    ShaderOptionProfile optProf = (ShaderOptionProfile)opt;
                    optProf.updateProfile();
                }

                gbso.displayString = getButtonText(opt, gbso.getButtonWidth());
                gbso.valueChanged();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        if (this.screenText != null) {
            this.drawCenteredString(this.fontRendererObj, this.screenText, this.width / 2, 15, 16777215);
        } else {
            this.drawCenteredString(this.fontRendererObj, this.title, this.width / 2, 15, 16777215);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);
    }
}
