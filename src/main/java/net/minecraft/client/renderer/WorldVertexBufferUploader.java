package net.minecraft.client.renderer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.SVertexBuilder;
import org.lwjgl.opengl.GL11;

public class WorldVertexBufferUploader {
    public void draw(WorldRenderer vertexBufferIn) {
        if (vertexBufferIn.getVertexCount() > 0) {
            if (vertexBufferIn.getDrawMode() == 7 && Config.isQuadsToTriangles()) {
                vertexBufferIn.quadsToTriangles();
            }

            VertexFormat vertexformat = vertexBufferIn.getVertexFormat();
            int i = vertexformat.getNextOffset();
            ByteBuffer bytebuffer = vertexBufferIn.getByteBuffer();
            List<VertexFormatElement> list = vertexformat.getElements();
            boolean forgePreDrawExists = Reflector.ForgeVertexFormatElementEnumUseage_preDraw.exists();
            boolean forgePostDrawExists = Reflector.ForgeVertexFormatElementEnumUseage_postDraw.exists();

            for (int j = 0; j < list.size(); j++) {
                VertexFormatElement vertexformatelement = list.get(j);
                EnumUsage vertexformatelement$enumusage = vertexformatelement.getUsage();
                if (forgePreDrawExists) {
                    Reflector.callVoid(vertexformatelement$enumusage, Reflector.ForgeVertexFormatElementEnumUseage_preDraw, vertexformat, j, i, bytebuffer);
                } else {
                    int k = vertexformatelement.getType().getGlConstant();
                    int l = vertexformatelement.getIndex();
                    ((Buffer)bytebuffer).position(vertexformat.getOffset(j));
                    switch (vertexformatelement$enumusage) {
                        case POSITION:
                            GL11.glVertexPointer(vertexformatelement.getElementCount(), k, i, bytebuffer);
                            GL11.glEnableClientState(32884);
                            break;
                        case UV:
                            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + l);
                            GL11.glTexCoordPointer(vertexformatelement.getElementCount(), k, i, bytebuffer);
                            GL11.glEnableClientState(32888);
                            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                            break;
                        case COLOR:
                            GL11.glColorPointer(vertexformatelement.getElementCount(), k, i, bytebuffer);
                            GL11.glEnableClientState(32886);
                            break;
                        case NORMAL:
                            GL11.glNormalPointer(k, i, bytebuffer);
                            GL11.glEnableClientState(32885);
                    }
                }
            }

            if (vertexBufferIn.isMultiTexture()) {
                vertexBufferIn.drawMultiTexture();
            } else if (Config.isShaders()) {
                SVertexBuilder.drawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount(), vertexBufferIn);
            } else {
                GL11.glDrawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount());
            }

            int i1 = 0;

            for (int j1 = list.size(); i1 < j1; i1++) {
                VertexFormatElement vertexformatelement1 = list.get(i1);
                EnumUsage vertexformatelement$enumusage1 = vertexformatelement1.getUsage();
                if (forgePostDrawExists) {
                    Reflector.callVoid(vertexformatelement$enumusage1, Reflector.ForgeVertexFormatElementEnumUseage_postDraw, vertexformat, i1, i, bytebuffer);
                } else {
                    int k1 = vertexformatelement1.getIndex();
                    switch (vertexformatelement$enumusage1) {
                        case POSITION:
                            GL11.glDisableClientState(32884);
                            break;
                        case UV:
                            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + k1);
                            GL11.glDisableClientState(32888);
                            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                            break;
                        case COLOR:
                            GL11.glDisableClientState(32886);
                            GlStateManager.resetColor();
                            break;
                        case NORMAL:
                            GL11.glDisableClientState(32885);
                    }
                }
            }
        }

        vertexBufferIn.reset();
    }
}
