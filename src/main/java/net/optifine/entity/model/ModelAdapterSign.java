package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterSign extends ModelAdapter {
    public ModelAdapterSign() {
        super(TileEntitySign.class, "sign", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSign();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelSign)) {
            return null;
        } else {
            ModelSign modelSign = (ModelSign)model;
            if (modelPart.equals("board")) {
                return modelSign.signBoard;
            } else {
                return modelPart.equals("stick") ? modelSign.signStick : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"board", "stick"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntitySign.class);
        if (!(renderer instanceof TileEntitySignRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntitySignRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntitySignRenderer_model.exists()) {
                Config.warn("Field not found: TileEntitySignRenderer.model");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntitySignRenderer_model, modelBase);
                return renderer;
            }
        }
    }
}
