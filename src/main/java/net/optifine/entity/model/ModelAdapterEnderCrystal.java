package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterEnderCrystal extends ModelAdapter {
    public ModelAdapterEnderCrystal() {
        this("end_crystal");
    }

    protected ModelAdapterEnderCrystal(String name) {
        super(EntityEnderCrystal.class, name, 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelEnderCrystal(0.0F, true);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelEnderCrystal)) {
            return null;
        } else {
            ModelEnderCrystal modelEnderCrystal = (ModelEnderCrystal)model;
            if (modelPart.equals("cube")) {
                return (ModelRenderer)Reflector.getFieldValue(modelEnderCrystal, Reflector.ModelEnderCrystal_ModelRenderers, 0);
            } else if (modelPart.equals("glass")) {
                return (ModelRenderer)Reflector.getFieldValue(modelEnderCrystal, Reflector.ModelEnderCrystal_ModelRenderers, 1);
            } else {
                return modelPart.equals("base")
                    ? (ModelRenderer)Reflector.getFieldValue(modelEnderCrystal, Reflector.ModelEnderCrystal_ModelRenderers, 2)
                    : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"cube", "glass", "base"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        Render renderObj = renderManager.getEntityRenderMap().get(EntityEnderCrystal.class);
        if (!(renderObj instanceof RenderEnderCrystal)) {
            Config.warn("Not an instance of RenderEnderCrystal: " + renderObj);
            return null;
        } else {
            RenderEnderCrystal render = (RenderEnderCrystal)renderObj;
            if (!Reflector.RenderEnderCrystal_modelEnderCrystal.exists()) {
                Config.warn("Field not found: RenderEnderCrystal.modelEnderCrystal");
                return null;
            } else {
                Reflector.setFieldValue(render, Reflector.RenderEnderCrystal_modelEnderCrystal, modelBase);
                render.shadowSize = shadowSize;
                return render;
            }
        }
    }
}
