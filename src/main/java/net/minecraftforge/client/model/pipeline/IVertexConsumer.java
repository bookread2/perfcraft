package net.minecraftforge.client.model.pipeline;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

public interface IVertexConsumer {
    VertexFormat getVertexFormat();

    void setQuadTint(int i);

    void setQuadOrientation(EnumFacing enumFacing);

    void setQuadColored();

    void put(int i, float... fs);
}
