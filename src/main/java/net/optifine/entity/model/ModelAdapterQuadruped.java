package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;

public abstract class ModelAdapterQuadruped extends ModelAdapter {
    public ModelAdapterQuadruped(Class entityClass, String name, float shadowSize) {
        super(entityClass, name, shadowSize);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelQuadruped)) {
            return null;
        } else {
            ModelQuadruped modelQuadruped = (ModelQuadruped)model;
            if (modelPart.equals("head")) {
                return modelQuadruped.head;
            } else if (modelPart.equals("body")) {
                return modelQuadruped.body;
            } else if (modelPart.equals("leg1")) {
                return modelQuadruped.leg1;
            } else if (modelPart.equals("leg2")) {
                return modelQuadruped.leg2;
            } else if (modelPart.equals("leg3")) {
                return modelQuadruped.leg3;
            } else {
                return modelPart.equals("leg4") ? modelQuadruped.leg4 : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head", "body", "leg1", "leg2", "leg3", "leg4"};
    }
}
