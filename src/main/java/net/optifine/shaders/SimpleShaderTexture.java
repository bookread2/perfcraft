package net.optifine.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.FontMetadataSection;
import net.minecraft.client.resources.data.FontMetadataSectionSerializer;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.client.resources.data.LanguageMetadataSectionSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSectionSerializer;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.client.resources.data.TextureMetadataSectionSerializer;
import org.apache.commons.io.IOUtils;

public class SimpleShaderTexture extends AbstractTexture {
    private String texturePath;
    private static final IMetadataSerializer METADATA_SERIALIZER = makeMetadataSerializer();

    public SimpleShaderTexture(String texturePath) {
        this.texturePath = texturePath;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        InputStream inputStream = Shaders.getShaderPackResourceStream(this.texturePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Shader texture not found: " + this.texturePath);
        } else {
            try {
                BufferedImage bufferedimage = TextureUtil.readBufferedImage(inputStream);
                TextureMetadataSection tms = loadTextureMetadataSection(this.texturePath, new TextureMetadataSection(false, false, new ArrayList<>()));
                TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, tms.getTextureBlur(), tms.getTextureClamp());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    public static TextureMetadataSection loadTextureMetadataSection(String texturePath, TextureMetadataSection def) {
        String pathMeta = texturePath + ".mcmeta";
        String sectionName = "texture";
        InputStream inMeta = Shaders.getShaderPackResourceStream(pathMeta);
        if (inMeta != null) {
            IMetadataSerializer ms = METADATA_SERIALIZER;
            BufferedReader brMeta = new BufferedReader(new InputStreamReader(inMeta));

            TextureMetadataSection var9;
            try {
                JsonObject jsonMeta = new JsonParser().parse(brMeta).getAsJsonObject();
                TextureMetadataSection meta = ms.parseMetadataSection(sectionName, jsonMeta);
                if (meta == null) {
                    return def;
                }

                var9 = meta;
            } catch (RuntimeException var13) {
                SMCLog.warning("Error reading metadata: " + pathMeta);
                SMCLog.warning("" + var13.getClass().getName() + ": " + var13.getMessage());
                return def;
            } finally {
                IOUtils.closeQuietly((Reader)brMeta);
                IOUtils.closeQuietly(inMeta);
            }

            return var9;
        } else {
            return def;
        }
    }

    private static IMetadataSerializer makeMetadataSerializer() {
        IMetadataSerializer ms = new IMetadataSerializer();
        ms.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
        ms.registerMetadataSectionType(new FontMetadataSectionSerializer(), FontMetadataSection.class);
        ms.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
        ms.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
        ms.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
        return ms;
    }
}
