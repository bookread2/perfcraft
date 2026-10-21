package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.monster.EntitySpider;

public class ModelAdapterSpider extends ModelAdapter {
    public ModelAdapterSpider() {
        super(EntitySpider.class, "spider", 1.0F);
    }

    protected ModelAdapterSpider(Class entityClass, String name, float shadowSize) {
        super(entityClass, name, shadowSize);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSpider();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelSpider)) {
            return null;
        } else {
            ModelSpider modelSpider = (ModelSpider)model;
            if (modelPart.equals("head")) {
                return modelSpider.spiderHead;
            } else if (modelPart.equals("neck")) {
                return modelSpider.spiderNeck;
            } else if (modelPart.equals("body")) {
                return modelSpider.spiderBody;
            } else if (modelPart.equals("leg1")) {
                return modelSpider.spiderLeg1;
            } else if (modelPart.equals("leg2")) {
                return modelSpider.spiderLeg2;
            } else if (modelPart.equals("leg3")) {
                return modelSpider.spiderLeg3;
            } else if (modelPart.equals("leg4")) {
                return modelSpider.spiderLeg4;
            } else if (modelPart.equals("leg5")) {
                return modelSpider.spiderLeg5;
            } else if (modelPart.equals("leg6")) {
                return modelSpider.spiderLeg6;
            } else if (modelPart.equals("leg7")) {
                return modelSpider.spiderLeg7;
            } else {
                return modelPart.equals("leg8") ? modelSpider.spiderLeg8 : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "neck", "body", "leg1", "leg2", "leg3", "leg4", "leg5", "leg6", "leg7", "leg8"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderSpider render = new RenderSpider(renderManager);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
