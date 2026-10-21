package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.passive.EntityBat;
import net.optifine.reflect.Reflector;

public class ModelAdapterBat extends ModelAdapter {
    public ModelAdapterBat() {
        super(EntityBat.class, "bat", 0.25F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBat();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelBat)) {
            return null;
        } else {
            ModelBat modelBat = (ModelBat)model;
            if (modelPart.equals("head")) {
                return (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 0);
            } else if (modelPart.equals("body")) {
                return (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 1);
            } else if (modelPart.equals("right_wing")) {
                return (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 2);
            } else if (modelPart.equals("left_wing")) {
                return (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 3);
            } else if (modelPart.equals("outer_right_wing")) {
                return (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 4);
            } else {
                return modelPart.equals("outer_left_wing") ? (ModelRenderer)Reflector.getFieldValue(modelBat, Reflector.ModelBat_ModelRenderers, 5) : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "right_wing", "left_wing", "outer_right_wing", "outer_left_wing"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderBat render = new RenderBat(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
