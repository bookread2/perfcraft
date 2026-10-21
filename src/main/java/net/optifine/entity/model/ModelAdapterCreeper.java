package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelCreeper;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderCreeper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.monster.EntityCreeper;

public class ModelAdapterCreeper extends ModelAdapter {
    public ModelAdapterCreeper() {
        super(EntityCreeper.class, "creeper", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelCreeper();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelCreeper)) {
            return null;
        } else {
            ModelCreeper modelCreeper = (ModelCreeper)model;
            if (modelPart.equals("head")) {
                return modelCreeper.head;
            } else if (modelPart.equals("armor")) {
                return modelCreeper.creeperArmor;
            } else if (modelPart.equals("body")) {
                return modelCreeper.body;
            } else if (modelPart.equals("leg1")) {
                return modelCreeper.leg1;
            } else if (modelPart.equals("leg2")) {
                return modelCreeper.leg2;
            } else if (modelPart.equals("leg3")) {
                return modelCreeper.leg3;
            } else {
                return modelPart.equals("leg4") ? modelCreeper.leg4 : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "armor", "body", "leg1", "leg2", "leg3", "leg4"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderCreeper render = new RenderCreeper(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
