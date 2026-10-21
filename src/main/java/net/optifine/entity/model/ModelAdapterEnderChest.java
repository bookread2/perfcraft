package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterEnderChest extends ModelAdapter {
    public ModelAdapterEnderChest() {
        super(TileEntityEnderChest.class, "ender_chest", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelChest();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelChest)) {
            return null;
        } else {
            ModelChest modelChest = (ModelChest)model;
            if (modelPart.equals("lid")) {
                return modelChest.chestLid;
            } else if (modelPart.equals("base")) {
                return modelChest.chestBelow;
            } else {
                return modelPart.equals("knob") ? modelChest.chestKnob : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"lid", "base", "knob"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntityEnderChest.class);
        if (!(renderer instanceof TileEntityEnderChestRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntityEnderChestRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntityEnderChestRenderer_modelChest.exists()) {
                Config.warn("Field not found: TileEntityEnderChestRenderer.modelChest");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntityEnderChestRenderer_modelChest, modelBase);
                return renderer;
            }
        }
    }
}
