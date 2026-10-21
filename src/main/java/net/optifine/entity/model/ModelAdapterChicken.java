package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderChicken;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.passive.EntityChicken;

public class ModelAdapterChicken extends ModelAdapter {
    public ModelAdapterChicken() {
        super(EntityChicken.class, "chicken", 0.3F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelChicken();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelChicken)) {
            return null;
        } else {
            ModelChicken modelChicken = (ModelChicken)model;
            if (modelPart.equals("head")) {
                return modelChicken.head;
            } else if (modelPart.equals("body")) {
                return modelChicken.body;
            } else if (modelPart.equals("right_leg")) {
                return modelChicken.rightLeg;
            } else if (modelPart.equals("left_leg")) {
                return modelChicken.leftLeg;
            } else if (modelPart.equals("right_wing")) {
                return modelChicken.rightWing;
            } else if (modelPart.equals("left_wing")) {
                return modelChicken.leftWing;
            } else if (modelPart.equals("bill")) {
                return modelChicken.bill;
            } else {
                return modelPart.equals("chin") ? modelChicken.chin : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "right_leg", "left_leg", "right_wing", "left_wing", "bill", "chin"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        return new RenderChicken(renderManager, modelBase, shadowSize);
    }
}
