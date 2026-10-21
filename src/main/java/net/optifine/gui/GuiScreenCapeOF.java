package net.optifine.gui;

import com.mojang.authlib.exceptions.InvalidCredentialsException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Random;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.optifine.Config;
import net.optifine.Lang;

public class GuiScreenCapeOF extends GuiScreenOF {
    private final GuiScreen parentScreen;
    private String title;
    private String message;
    private long messageHideTimeMs;
    private String linkUrl;
    private GuiButtonOF buttonCopyLink;
    private FontRenderer fontRenderer = Config.getMinecraft().fontRendererObj;

    public GuiScreenCapeOF(GuiScreen parentScreenIn) {
        this.parentScreen = parentScreenIn;
    }

    @Override
    public void initGui() {
        int i = 0;
        this.title = I18n.format("of.options.capeOF.title");
        i += 2;
        this.buttonList.add(new GuiButtonOF(210, this.width / 2 - 155, this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("of.options.capeOF.openEditor")));
        this.buttonList
            .add(new GuiButtonOF(220, this.width / 2 - 155 + 160, this.height / 6 + 24 * (i >> 1), 150, 20, I18n.format("of.options.capeOF.reloadCape")));
        i += 6;
        this.buttonCopyLink = new GuiButtonOF(
            230, this.width / 2 - 100, this.height / 6 + 24 * (i >> 1), 200, 20, I18n.format("of.options.capeOF.copyEditorLink")
        );
        this.buttonCopyLink.visible = this.linkUrl != null;
        this.buttonList.add(this.buttonCopyLink);
        i += 4;
        this.buttonList.add(new GuiButtonOF(200, this.width / 2 - 100, this.height / 6 + 24 * (i >> 1), I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 200) {
                this.mc.displayGuiScreen(this.parentScreen);
            }

            if (button.id == 210) {
                try {
                    String userName = this.mc.getSession().getProfile().getName();
                    String userId = this.mc.getSession().getProfile().getId().toString().replace("-", "");
                    String accessToken = this.mc.getSession().getToken();
                    Random r1 = new Random();
                    Random r2 = new Random(System.identityHashCode(new Object()));
                    BigInteger random1Bi = new BigInteger(128, r1);
                    BigInteger random2Bi = new BigInteger(128, r2);
                    BigInteger serverBi = random1Bi.xor(random2Bi);
                    String serverId = serverBi.toString(16);
                    this.mc.getSessionService().joinServer(this.mc.getSession().getProfile(), accessToken, serverId);
                    String urlStr = "https://optifine.net/capeChange?u=" + userId + "&n=" + userName + "&s=" + serverId;
                    boolean opened = Config.openWebLink(new URI(urlStr));
                    if (opened) {
                        this.showMessage(Lang.get("of.message.capeOF.openEditor"), 10000L);
                    } else {
                        this.showMessage(Lang.get("of.message.capeOF.openEditorError"), 10000L);
                        this.setLinkUrl(urlStr);
                    }
                } catch (InvalidCredentialsException var13) {
                    Config.showGuiMessage(I18n.format("of.message.capeOF.error1"), I18n.format("of.message.capeOF.error2", var13.getMessage()));
                    Config.warn("Mojang authentication failed");
                    Config.warn(var13.getClass().getName() + ": " + var13.getMessage());
                } catch (Exception var14) {
                    Config.warn("Error opening OptiFine cape link");
                    Config.warn(var14.getClass().getName() + ": " + var14.getMessage());
                }
            }

            if (button.id == 220) {
                this.showMessage(Lang.get("of.message.capeOF.reloadCape"), 15000L);
                if (this.mc.thePlayer != null) {
                    long delayMs = 15000L;
                    long reloadTimeMs = System.currentTimeMillis() + delayMs;
                    this.mc.thePlayer.setReloadCapeTimeMs(reloadTimeMs);
                }
            }

            if (button.id == 230 && this.linkUrl != null) {
                setClipboardString(this.linkUrl);
            }
        }
    }

    private void showMessage(String msg, long timeMs) {
        this.message = msg;
        this.messageHideTimeMs = System.currentTimeMillis() + timeMs;
        this.setLinkUrl(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 20, 16777215);
        if (this.message != null) {
            this.drawCenteredString(this.fontRenderer, this.message, this.width / 2, this.height / 6 + 60, 16777215);
            if (System.currentTimeMillis() > this.messageHideTimeMs) {
                this.message = null;
                this.setLinkUrl(null);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
        this.buttonCopyLink.visible = linkUrl != null;
    }
}
