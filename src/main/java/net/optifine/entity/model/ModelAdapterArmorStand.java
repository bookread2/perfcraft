package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelArmorStand;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityArmorStand;
import net.optifine.Config;

public class ModelAdapterArmorStand extends ModelAdapterBiped {
    public ModelAdapterArmorStand() {
        super(EntityArmorStand.class, "armor_stand", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelArmorStand();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelArmorStand)) {
            return null;
        } else {
            ModelArmorStand modelArmorStand = (ModelArmorStand)model;
            if (modelPart.equals("right")) {
                return modelArmorStand.standRightSide;
            } else if (modelPart.equals("left")) {
                return modelArmorStand.standLeftSide;
            } else if (modelPart.equals("waist")) {
                return modelArmorStand.standWaist;
            } else {
                return modelPart.equals("base") ? modelArmorStand.standBase : super.getModelRenderer(modelArmorStand, modelPart);
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        String[] names = super.getModelRendererNames();
        return (String[])Config.addObjectsToArray(names, new String[]{"right", "left", "waist", "base"});
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        ArmorStandRenderer render = new ArmorStandRenderer(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
