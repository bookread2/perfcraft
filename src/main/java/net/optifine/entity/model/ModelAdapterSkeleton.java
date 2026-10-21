package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSkeleton;
import net.minecraft.entity.monster.EntitySkeleton;

public class ModelAdapterSkeleton extends ModelAdapterBiped {
    public ModelAdapterSkeleton() {
        super(EntitySkeleton.class, "skeleton", 0.7F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelSkeleton();
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        RenderSkeleton render = new RenderSkeleton(renderManager);
        Render.setModelBipedMain(render, (ModelBiped)modelBase);
        render.mainModel = modelBase;
        render.shadowSize = shadowSize;
        return render;
    }
}
