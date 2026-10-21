package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderVillager;
import net.minecraft.entity.passive.EntityVillager;

public class ModelAdapterVillager extends ModelAdapter {
    public ModelAdapterVillager() {
        super(EntityVillager.class, "villager", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelVillager(0.0F);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelVillager)) {
            return null;
        } else {
            ModelVillager modelVillager = (ModelVillager)model;
            if (modelPart.equals("head")) {
                return modelVillager.villagerHead;
            } else if (modelPart.equals("body")) {
                return modelVillager.villagerBody;
            } else if (modelPart.equals("arms")) {
                return modelVillager.villagerArms;
            } else if (modelPart.equals("left_leg")) {
                return modelVillager.leftVillagerLeg;
            } else if (modelPart.equals("right_leg")) {
                return modelVillager.rightVillagerLeg;
            } else {
                return modelPart.equals("nose") ? modelVillager.villagerNose : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "arms", "right_leg", "left_leg", "nose"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderVillager render = new RenderVillager(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
