package net.optifine.entity.model.anim;

import net.minecraft.client.model.ModelRenderer;
import net.optifine.expr.IExpressionResolver;

public interface IModelResolver extends IExpressionResolver {
    ModelRenderer getModelRenderer(String string);

    ModelVariableFloat getModelVariable(String string);
}
