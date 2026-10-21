package net.optifine.entity.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import net.optifine.Config;
import net.optifine.config.ConnectedParser;
import net.optifine.entity.model.anim.ModelUpdater;
import net.optifine.entity.model.anim.ModelVariableUpdater;
import net.optifine.player.PlayerItemParser;
import net.optifine.util.Json;

public class CustomEntityModelParser {
    public static final String ENTITY = "entity";
    public static final String TEXTURE = "texture";
    public static final String SHADOW_SIZE = "shadowSize";
    public static final String ITEM_TYPE = "type";
    public static final String ITEM_TEXTURE_SIZE = "textureSize";
    public static final String ITEM_USE_PLAYER_TEXTURE = "usePlayerTexture";
    public static final String ITEM_MODELS = "models";
    public static final String ITEM_ANIMATIONS = "animations";
    public static final String MODEL_ID = "id";
    public static final String MODEL_BASE_ID = "baseId";
    public static final String MODEL_MODEL = "model";
    public static final String MODEL_TYPE = "type";
    public static final String MODEL_PART = "part";
    public static final String MODEL_ATTACH = "attach";
    public static final String MODEL_INVERT_AXIS = "invertAxis";
    public static final String MODEL_MIRROR_TEXTURE = "mirrorTexture";
    public static final String MODEL_TRANSLATE = "translate";
    public static final String MODEL_ROTATE = "rotate";
    public static final String MODEL_SCALE = "scale";
    public static final String MODEL_BOXES = "boxes";
    public static final String MODEL_SPRITES = "sprites";
    public static final String MODEL_SUBMODEL = "submodel";
    public static final String MODEL_SUBMODELS = "submodels";
    public static final String BOX_TEXTURE_OFFSET = "textureOffset";
    public static final String BOX_COORDINATES = "coordinates";
    public static final String BOX_SIZE_ADD = "sizeAdd";
    public static final String ENTITY_MODEL = "EntityModel";
    public static final String ENTITY_MODEL_PART = "EntityModelPart";

    public static CustomEntityRenderer parseEntityRender(JsonObject obj, String path) {
        ConnectedParser cp = new ConnectedParser("CustomEntityModels");
        String name = cp.parseName(path);
        String basePath = cp.parseBasePath(path);
        String texture = Json.getString(obj, "texture");
        int[] textureSize = Json.parseIntArray(obj.get("textureSize"), 2);
        float shadowSize = Json.getFloat(obj, "shadowSize", -1.0F);
        JsonArray models = (JsonArray)obj.get("models");
        checkNull(models, "Missing models");
        Map mapModelJsons = new HashMap();
        List listModels = new ArrayList();

        for (int i = 0; i < models.size(); i++) {
            JsonObject elem = (JsonObject)models.get(i);
            processBaseId(elem, mapModelJsons);
            processExternalModel(elem, mapModelJsons, basePath);
            processId(elem, mapModelJsons);
            CustomModelRenderer mr = parseCustomModelRenderer(elem, textureSize, basePath);
            if (mr != null) {
                listModels.add(mr);
            }
        }

        CustomModelRenderer[] modelRenderers = (CustomModelRenderer[]) listModels.toArray(new CustomModelRenderer[listModels.size()]);
        ResourceLocation textureLocation = null;
        if (texture != null) {
            textureLocation = getResourceLocation(basePath, texture, ".png");
        }

        return new CustomEntityRenderer(name, basePath, textureLocation, modelRenderers, shadowSize);
    }

    private static void processBaseId(JsonObject elem, Map mapModelJsons) {
        String baseId = Json.getString(elem, "baseId");
        if (baseId != null) {
            JsonObject baseObj = (JsonObject)mapModelJsons.get(baseId);
            if (baseObj == null) {
                Config.warn("BaseID not found: " + baseId);
            } else {
                copyJsonElements(baseObj, elem);
            }
        }
    }

    private static void processExternalModel(JsonObject elem, Map mapModelJsons, String basePath) {
        String modelPath = Json.getString(elem, "model");
        if (modelPath != null) {
            ResourceLocation locJson = getResourceLocation(basePath, modelPath, ".jpm");

            try {
                JsonObject modelObj = loadJson(locJson);
                if (modelObj == null) {
                    Config.warn("Model not found: " + locJson);
                    return;
                }

                copyJsonElements(modelObj, elem);
            } catch (IOException var6) {
                Config.error("" + var6.getClass().getName() + ": " + var6.getMessage());
            } catch (JsonParseException var7) {
                Config.error("" + var7.getClass().getName() + ": " + var7.getMessage());
            } catch (Exception var8) {
                var8.printStackTrace();
            }
        }
    }

    private static void copyJsonElements(JsonObject objFrom, JsonObject objTo) {
        for (Entry<String, JsonElement> entry : objFrom.entrySet()) {
            if (!entry.getKey().equals("id") && !objTo.has(entry.getKey())) {
                objTo.add(entry.getKey(), entry.getValue());
            }
        }
    }

    public static ResourceLocation getResourceLocation(String basePath, String path, String extension) {
        if (!path.endsWith(extension)) {
            path = path + extension;
        }

        if (!path.contains("/")) {
            path = basePath + "/" + path;
        } else if (path.startsWith("./")) {
            path = basePath + "/" + path.substring(2);
        } else if (path.startsWith("~/")) {
            path = "optifine/" + path.substring(2);
        }

        return new ResourceLocation(path);
    }

    private static void processId(JsonObject elem, Map mapModelJsons) {
        String id = Json.getString(elem, "id");
        if (id != null) {
            if (id.length() < 1) {
                Config.warn("Empty model ID: " + id);
            } else if (mapModelJsons.containsKey(id)) {
                Config.warn("Duplicate model ID: " + id);
            } else {
                mapModelJsons.put(id, elem);
            }
        }
    }

    public static CustomModelRenderer parseCustomModelRenderer(JsonObject elem, int[] textureSize, String basePath) {
        String modelPart = Json.getString(elem, "part");
        checkNull(modelPart, "Model part not specified, missing \"replace\" or \"attachTo\".");
        boolean attach = Json.getBoolean(elem, "attach", false);
        ModelBase modelBase = new CustomEntityModel();
        if (textureSize != null) {
            modelBase.textureWidth = textureSize[0];
            modelBase.textureHeight = textureSize[1];
        }

        ModelUpdater mu = null;
        JsonArray animations = (JsonArray)elem.get("animations");
        if (animations != null) {
            List<ModelVariableUpdater> listModelVariableUpdaters = new ArrayList<>();

            for (int i = 0; i < animations.size(); i++) {
                JsonObject anim = (JsonObject)animations.get(i);

                for (Entry<String, JsonElement> entry : anim.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue().getAsString();
                    ModelVariableUpdater mvu = new ModelVariableUpdater(key, val);
                    listModelVariableUpdaters.add(mvu);
                }
            }

            if (listModelVariableUpdaters.size() > 0) {
                ModelVariableUpdater[] mvus = listModelVariableUpdaters.toArray(new ModelVariableUpdater[listModelVariableUpdaters.size()]);
                mu = new ModelUpdater(mvus);
            }
        }

        ModelRenderer mr = PlayerItemParser.parseModelRenderer(elem, modelBase, textureSize, basePath);
        return new CustomModelRenderer(modelPart, attach, mr, mu);
    }

    private static void checkNull(Object obj, String msg) {
        if (obj == null) {
            throw new JsonParseException(msg);
        }
    }

    public static JsonObject loadJson(ResourceLocation location) throws IOException, JsonParseException {
        InputStream in = Config.getResourceStream(location);
        if (in == null) {
            return null;
        } else {
            String jsonStr = Config.readInputStream(in, "ASCII");
            in.close();
            JsonParser jp = new JsonParser();
            return (JsonObject)jp.parse(jsonStr);
        }
    }
}
