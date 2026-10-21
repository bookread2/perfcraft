package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityBoat;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBoat extends ModelAdapter {
    public ModelAdapterBoat() {
        super(EntityBoat.class, "boat", 0.5F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBoat();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelBoat)) {
            return null;
        } else {
            ModelBoat modelBoat = (ModelBoat)model;
            if (modelPart.equals("bottom")) {
                return modelBoat.boatSides[0];
            } else if (modelPart.equals("back")) {
                return modelBoat.boatSides[1];
            } else if (modelPart.equals("front")) {
                return modelBoat.boatSides[2];
            } else if (modelPart.equals("right")) {
                return modelBoat.boatSides[3];
            } else {
                return modelPart.equals("left") ? modelBoat.boatSides[4] : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"bottom", "back", "front", "right", "left"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderBoat render = new RenderBoat(renderManager);
        if (!Reflector.RenderBoat_modelBoat.exists()) {
            Config.warn("Field not found: RenderBoat.modelBoat");
            return null;
        } else {
            Reflector.setFieldValue(render, Reflector.RenderBoat_modelBoat, modelBase);
            render.shadowSize = shadowSize;
            return render;
        }
    }
}
