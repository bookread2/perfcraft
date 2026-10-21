package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelHumanoidHead;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterHeadHumanoid extends ModelAdapter {
    public ModelAdapterHeadHumanoid() {
        super(TileEntitySkull.class, "head_humanoid", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelHumanoidHead();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelHumanoidHead)) {
            return null;
        } else {
            ModelHumanoidHead modelHumanoidHead = (ModelHumanoidHead)model;
            if (modelPart.equals("head")) {
                return modelHumanoidHead.skeletonHead;
            } else if (modelPart.equals("head2")) {
                return !Reflector.ModelHumanoidHead_head.exists()
                    ? null
                    : (ModelRenderer)Reflector.getFieldValue(modelHumanoidHead, Reflector.ModelHumanoidHead_head);
            } else {
                return null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"head"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntitySkull.class);
        if (!(renderer instanceof TileEntitySkullRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntitySkullRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntitySkullRenderer_humanoidHead.exists()) {
                Config.warn("Field not found: TileEntitySkullRenderer.humanoidHead");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntitySkullRenderer_humanoidHead, modelBase);
                return renderer;
            }
        }
    }
}
