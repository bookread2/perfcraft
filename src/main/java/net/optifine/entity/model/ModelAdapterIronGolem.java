package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderIronGolem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.monster.EntityIronGolem;

public class ModelAdapterIronGolem extends ModelAdapter {
    public ModelAdapterIronGolem() {
        super(EntityIronGolem.class, "iron_golem", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelIronGolem();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelIronGolem)) {
            return null;
        } else {
            ModelIronGolem modelIronGolem = (ModelIronGolem)model;
            if (modelPart.equals("head")) {
                return modelIronGolem.ironGolemHead;
            } else if (modelPart.equals("body")) {
                return modelIronGolem.ironGolemBody;
            } else if (modelPart.equals("left_arm")) {
                return modelIronGolem.ironGolemLeftArm;
            } else if (modelPart.equals("right_arm")) {
                return modelIronGolem.ironGolemRightArm;
            } else if (modelPart.equals("left_leg")) {
                return modelIronGolem.ironGolemLeftLeg;
            } else {
                return modelPart.equals("right_leg") ? modelIronGolem.ironGolemRightLeg : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "right_arm", "left_arm", "left_leg", "right_leg"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderIronGolem render = new RenderIronGolem(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
