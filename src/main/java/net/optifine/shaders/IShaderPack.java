package net.optifine.shaders;

import java.io.InputStream;

public interface IShaderPack {
    String getName();

    InputStream getResourceAsStream(String string);

    boolean hasDirectory(String string);

    void close();
}
