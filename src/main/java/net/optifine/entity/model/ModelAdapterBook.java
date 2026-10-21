package net.optifine.entity.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnchantmentTableRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBook extends ModelAdapter {
    public ModelAdapterBook() {
        super(TileEntityEnchantmentTable.class, "book", 0.0F);
    }

    @Override
    public ModelBase makeModel() {
        return new ModelBook();
    }

    @Override
    public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
        if (!(model instanceof ModelBook)) {
            return null;
        } else {
            ModelBook modelBook = (ModelBook)model;
            if (modelPart.equals("cover_right")) {
                return modelBook.coverRight;
            } else if (modelPart.equals("cover_left")) {
                return modelBook.coverLeft;
            } else if (modelPart.equals("pages_right")) {
                return modelBook.pagesRight;
            } else if (modelPart.equals("pages_left")) {
                return modelBook.pagesLeft;
            } else if (modelPart.equals("flipping_page_right")) {
                return modelBook.flippingPageRight;
            } else if (modelPart.equals("flipping_page_left")) {
                return modelBook.flippingPageLeft;
            } else {
                return modelPart.equals("book_spine") ? modelBook.bookSpine : null;
            }
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"cover_right", "cover_left", "pages_right", "pages_left", "flipping_page_right", "flipping_page_left", "book_spine"};
    }

    @Override
    public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRendererByClass(TileEntityEnchantmentTable.class);
        if (!(renderer instanceof TileEntityEnchantmentTableRenderer)) {
            return null;
        } else {
            if (renderer.getEntityClass() == null) {
                renderer = new TileEntityEnchantmentTableRenderer();
                renderer.setRendererDispatcher(dispatcher);
            }

            if (!Reflector.TileEntityEnchantmentTableRenderer_modelBook.exists()) {
                Config.warn("Field not found: TileEntityEnchantmentTableRenderer.modelBook");
                return null;
            } else {
                Reflector.setFieldValue(renderer, Reflector.TileEntityEnchantmentTableRenderer_modelBook, modelBase);
                return renderer;
            }
        }
    }
}
