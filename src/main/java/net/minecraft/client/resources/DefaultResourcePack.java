package net.minecraft.client.resources;

import com.google.common.collect.ImmutableSet;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.optifine.reflect.ReflectorForge;

public class DefaultResourcePack implements IResourcePack {
    public static final Set<String> defaultResourceDomains = ImmutableSet.of("minecraft", "realms");
    private final Map<String, File> mapAssets;

    public DefaultResourcePack(Map<String, File> p_i46346_1_) {
        this.mapAssets = p_i46346_1_;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream inputstream = this.getResourceStream(location);
        if (inputstream != null) {
            return inputstream;
        } else {
            InputStream inputstream1 = this.getInputStreamAssets(location);
            if (inputstream1 != null) {
                return inputstream1;
            } else {
                throw new FileNotFoundException(location.getResourcePath());
            }
        }
    }

    public InputStream getInputStreamAssets(ResourceLocation location) throws IOException, FileNotFoundException {
        File file1 = this.mapAssets.get(location.toString());
        return file1 != null && file1.isFile() ? new FileInputStream(file1) : null;
    }

    private InputStream getResourceStream(ResourceLocation location) {
        String path = "/assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
        InputStream is = ReflectorForge.getOptiFineResourceStream(path);
        return is != null ? is : DefaultResourcePack.class.getResourceAsStream(path);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return this.getResourceStream(location) != null || this.mapAssets.containsKey(location.toString());
    }

    @Override
    public Set<String> getResourceDomains() {
        return defaultResourceDomains;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        try {
            InputStream inputstream = new FileInputStream(this.mapAssets.get("pack.mcmeta"));
            return AbstractResourcePack.readMetadata(metadataSerializer, inputstream, metadataSectionName);
        } catch (RuntimeException var4) {
            return null;
        } catch (FileNotFoundException var51) {
            return null;
        }
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return TextureUtil.readBufferedImage(DefaultResourcePack.class.getResourceAsStream("/" + new ResourceLocation("pack.png").getResourcePath()));
    }

    @Override
    public String getPackName() {
        return "Default";
    }
}
