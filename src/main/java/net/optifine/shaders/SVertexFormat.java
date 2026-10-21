package net.optifine.shaders;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;

public class SVertexFormat {
    public static final int vertexSizeBlock = 14;
    public static final int offsetMidTexCoord = 8;
    public static final int offsetTangent = 10;
    public static final int offsetEntity = 12;
    public static final VertexFormat defVertexFormatTextured = makeDefVertexFormatTextured();

    public static VertexFormat makeDefVertexFormatBlock() {
        VertexFormat vf = new VertexFormat();
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.UBYTE, EnumUsage.COLOR, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.UV, 2));
        vf.addElement(new VertexFormatElement(1, EnumType.SHORT, EnumUsage.UV, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.NORMAL, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 1));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        return vf;
    }

    public static VertexFormat makeDefVertexFormatItem() {
        VertexFormat vf = new VertexFormat();
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.UBYTE, EnumUsage.COLOR, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.UV, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.NORMAL, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 1));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        return vf;
    }

    public static VertexFormat makeDefVertexFormatTextured() {
        VertexFormat vf = new VertexFormat();
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.UBYTE, EnumUsage.PADDING, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.UV, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.NORMAL, 3));
        vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 1));
        vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 2));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        return vf;
    }

    public static void setDefBakedFormat(VertexFormat vf) {
        if (vf != null) {
            vf.clear();
            vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
            vf.addElement(new VertexFormatElement(0, EnumType.UBYTE, EnumUsage.COLOR, 4));
            vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.UV, 2));
            vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 2));
            vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.NORMAL, 3));
            vf.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 1));
            vf.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 2));
            vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
            vf.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
        }
    }

    public static VertexFormat duplicate(VertexFormat src) {
        if (src == null) {
            return null;
        } else {
            VertexFormat dst = new VertexFormat();
            copy(src, dst);
            return dst;
        }
    }

    public static void copy(VertexFormat src, VertexFormat dst) {
        if (src != null && dst != null) {
            dst.clear();

            for (int i = 0; i < src.getElementCount(); i++) {
                dst.addElement(src.getElement(i));
            }
        }
    }
}
