package net.optifine.entity.model;

import net.minecraft.client.model.ModelBanner;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityBannerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityBanner;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBanner extends ModelAdapter {
    public ModelAdapterBanner() {
        super(TileEntityBanner.class, "banner", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBanner();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelBanner)) {
            return null;
        } else {
            ModelBanner modelBanner = (ModelBanner)model;
            if (modelPart.equals("slate")) {
                return modelBanner.bannerSlate;
            } else if (modelPart.equals("stand")) {
                return modelBanner.bannerStand;
            } else {
                return modelPart.equals("top") ? modelBanner.bannerTop : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"slate", "stand", "top"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntityBanner.class);
        if (!(renderer instanceof TileEntityBannerRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntityBannerRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntityBannerRenderer_bannerModel.exists()) {
                Config.warn("Field not found: TileEntityBannerRenderer.bannerModel");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntityBannerRenderer_bannerModel, modelBase);
                return renderer;
            }
        }
    }
}
