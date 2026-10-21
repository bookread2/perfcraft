package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterHeadSkeleton extends ModelAdapter {
    public ModelAdapterHeadSkeleton() {
        super(TileEntitySkull.class, "head_skeleton", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSkeletonHead(0, 0, 64, 32);
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelSkeletonHead)) {
            return null;
        } else {
            ModelSkeletonHead modelSkeletonHead = (ModelSkeletonHead)model;
            return modelPart.equals("head") ? modelSkeletonHead.skeletonHead : null;
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
