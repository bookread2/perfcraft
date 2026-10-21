package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelLargeChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityChest;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterChestLarge extends ModelAdapter {
    public ModelAdapterChestLarge() {
        super(TileEntityChest.class, "chest_large", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelLargeChest();
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
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntityChest.class);
        if (!(renderer instanceof TileEntityChestRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntityChestRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntityChestRenderer_largeChest.exists()) {
                Config.warn("Field not found: TileEntityChestRenderer.largeChest");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntityChestRenderer_largeChest, modelBase);
                return renderer;
            }
        }
    }
}
