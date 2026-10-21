package net.optifine.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.util.EnumFacing;
import net.optifine.Config;

public class ModelUtils {
    public static void dbgModel(IBakedModel model) {
        if (model != null) {
            Config.dbg(
                "Model: "
                    + model
                    + ", ao: "
                    + model.isAmbientOcclusion()
                    + ", gui3d: "
                    + model.isGui3d()
                    + ", builtIn: "
                    + model.isBuiltInRenderer()
                    + ", particle: "
                    + model.getParticleTexture()
            );
            EnumFacing[] faces = EnumFacing.VALUES;

            for (int i = 0; i < faces.length; i++) {
                EnumFacing face = faces[i];
                List faceQuads = model.getFaceQuads(face);
                dbgQuads(face.getName(), faceQuads, "  ");
            }

            List generalQuads = model.getGeneralQuads();
            dbgQuads("General", generalQuads, "  ");
        }
    }

    private static void dbgQuads(String name, List quads, String prefix) {
        for (BakedQuad quad : (Iterable<BakedQuad>) (Iterable) quads) {
            dbgQuad(name, quad, prefix);
        }
    }

    public static void dbgQuad(String name, BakedQuad quad, String prefix) {
        Config.dbg(
            prefix
                + "Quad: "
                + quad.getClass().getName()
                + ", type: "
                + name
                + ", face: "
                + quad.getFace()
                + ", tint: "
                + quad.getTintIndex()
                + ", sprite: "
                + quad.getSprite()
        );
        dbgVertexData(quad.getVertexData(), "  " + prefix);
    }

    public static void dbgVertexData(int[] vd, String prefix) {
        int step = vd.length / 4;
        Config.dbg(prefix + "Length: " + vd.length + ", step: " + step);

        for (int i = 0; i < 4; i++) {
            int pos = i * step;
            float x = Float.intBitsToFloat(vd[pos + 0]);
            float y = Float.intBitsToFloat(vd[pos + 1]);
            float z = Float.intBitsToFloat(vd[pos + 2]);
            int col = vd[pos + 3];
            float u = Float.intBitsToFloat(vd[pos + 4]);
            float v = Float.intBitsToFloat(vd[pos + 5]);
            Config.dbg(prefix + i + " xyz: " + x + "," + y + "," + z + " col: " + col + " u,v: " + u + "," + v);
        }
    }

    public static IBakedModel duplicateModel(IBakedModel model) {
        List generalQuads2 = duplicateQuadList(model.getGeneralQuads());
        EnumFacing[] faces = EnumFacing.VALUES;
        List faceQuads2 = new ArrayList();

        for (int i = 0; i < faces.length; i++) {
            EnumFacing face = faces[i];
            List quads = model.getFaceQuads(face);
            List quads2 = duplicateQuadList(quads);
            faceQuads2.add(quads2);
        }

        return new SimpleBakedModel(
            generalQuads2, faceQuads2, model.isAmbientOcclusion(), model.isGui3d(), model.getParticleTexture(), model.getItemCameraTransforms()
        );
    }

    public static List duplicateQuadList(List list) {
        List list2 = new ArrayList();

        for (BakedQuad quad : (Iterable<BakedQuad>) (Iterable) list) {
            BakedQuad quad2 = duplicateQuad(quad);
            list2.add(quad2);
        }

        return list2;
    }

    public static BakedQuad duplicateQuad(BakedQuad quad) {
        return new BakedQuad((int[])quad.getVertexData().clone(), quad.getTintIndex(), quad.getFace(), quad.getSprite());
    }
}
