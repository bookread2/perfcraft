package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.passive.EntityWolf;
import net.optifine.reflect.Reflector;

public class ModelAdapterWolf extends ModelAdapter {
    public ModelAdapterWolf() {
        super(EntityWolf.class, "wolf", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelWolf();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelWolf)) {
            return null;
        } else {
            ModelWolf modelWolf = (ModelWolf)model;
            if (modelPart.equals("head")) {
                return modelWolf.wolfHeadMain;
            } else if (modelPart.equals("body")) {
                return modelWolf.wolfBody;
            } else if (modelPart.equals("leg1")) {
                return modelWolf.wolfLeg1;
            } else if (modelPart.equals("leg2")) {
                return modelWolf.wolfLeg2;
            } else if (modelPart.equals("leg3")) {
                return modelWolf.wolfLeg3;
            } else if (modelPart.equals("leg4")) {
                return modelWolf.wolfLeg4;
            } else if (modelPart.equals("tail")) {
                return (ModelRenderer)Reflector.getFieldValue(modelWolf, Reflector.ModelWolf_tail);
            } else {
                return modelPart.equals("mane") ? (ModelRenderer)Reflector.getFieldValue(modelWolf, Reflector.ModelWolf_mane) : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "leg1", "leg2", "leg3", "leg4", "tail", "mane"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        return new RenderWolf(renderManager, modelBase, shadowSize);
    }
}
