package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWitch;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderWitch;
import net.minecraft.entity.monster.EntityWitch;
import net.optifine.reflect.Reflector;

public class ModelAdapterWitch extends ModelAdapter {
    public ModelAdapterWitch() {
        super(EntityWitch.class, "witch", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelWitch(0.0F);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelWitch)) {
            return null;
        } else {
            ModelWitch modelWitch = (ModelWitch)model;
            if (modelPart.equals("mole")) {
                return (ModelRenderer)Reflector.getFieldValue(modelWitch, Reflector.ModelWitch_mole);
            } else if (modelPart.equals("hat")) {
                return (ModelRenderer)Reflector.getFieldValue(modelWitch, Reflector.ModelWitch_hat);
            } else if (modelPart.equals("head")) {
                return modelWitch.villagerHead;
            } else if (modelPart.equals("body")) {
                return modelWitch.villagerBody;
            } else if (modelPart.equals("arms")) {
                return modelWitch.villagerArms;
            } else if (modelPart.equals("left_leg")) {
                return modelWitch.leftVillagerLeg;
            } else if (modelPart.equals("right_leg")) {
                return modelWitch.rightVillagerLeg;
            } else {
                return modelPart.equals("nose") ? modelWitch.villagerNose : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"mole", "head", "body", "arms", "right_leg", "left_leg", "nose"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderWitch render = new RenderWitch(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
