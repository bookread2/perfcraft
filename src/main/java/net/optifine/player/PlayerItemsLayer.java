package net.optifine.player;

import java.util.Map;
import java.util.Set;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.optifine.Config;

public class PlayerItemsLayer implements LayerRenderer {
    private RenderPlayer renderPlayer = null;

    public PlayerItemsLayer(RenderPlayer renderPlayer) {
        this.renderPlayer = renderPlayer;
    }

    @Override
    public void doRenderLayer(
        EntityLivingBase entitylivingbaseIn,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ticksExisted,
        float headYaw,
        float rotationPitch,
        float scale
    ) {
        this.renderEquippedItems(entitylivingbaseIn, scale, partialTicks);
    }

    protected void renderEquippedItems(EntityLivingBase entityLiving, float scale, float partialTicks) {
        if (Config.isShowCapes()) {
            if (entityLiving instanceof AbstractClientPlayer) {
                AbstractClientPlayer player = (AbstractClientPlayer)entityLiving;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.disableRescaleNormal();
                GlStateManager.enableCull();
                ModelBiped modelBipedMain = this.renderPlayer.getMainModel();
                PlayerConfigurations.renderPlayerItems(modelBipedMain, player, scale, partialTicks);
                GlStateManager.disableCull();
            }
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }

    public static void register(Map renderPlayerMap) {
        Set keys = renderPlayerMap.keySet();
        boolean registered = false;

        for (Object key : keys) {
            Object renderer = renderPlayerMap.get(key);
            if (renderer instanceof RenderPlayer) {
                RenderPlayer renderPlayer = (RenderPlayer)renderer;
                renderPlayer.addLayer(new PlayerItemsLayer(renderPlayer));
                registered = true;
            }
        }

        if (!registered) {
            Config.warn("PlayerItemsLayer not registered");
        }
    }
}
