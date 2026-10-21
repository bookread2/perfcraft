package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.entity.monster.EntitySnowman;

public class ModelAdapterSnowman extends ModelAdapter {
    public ModelAdapterSnowman() {
        super(EntitySnowman.class, "snow_golem", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSnowMan();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelSnowMan)) {
            return null;
        } else {
            ModelSnowMan modelSnowman = (ModelSnowMan)model;
            if (modelPart.equals("body")) {
                return modelSnowman.body;
            } else if (modelPart.equals("body_bottom")) {
                return modelSnowman.bottomBody;
            } else if (modelPart.equals("head")) {
                return modelSnowman.head;
            } else if (modelPart.equals("left_hand")) {
                return modelSnowman.leftHand;
            } else {
                return modelPart.equals("right_hand") ? modelSnowman.rightHand : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"body", "body_bottom", "head", "right_hand", "left_hand"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderSnowMan render = new RenderSnowMan(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
