package net.optifine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel.Builder;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.optifine.config.IParserInt;
import net.optifine.config.NbtTagValue;
import net.optifine.config.ParserEnchantmentId;
import net.optifine.config.RangeInt;
import net.optifine.config.RangeListInt;
import net.optifine.reflect.Reflector;
import net.optifine.render.Blender;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;
import org.lwjgl.opengl.GL11;

public class CustomItemProperties {
    public String name = null;
    public String basePath = null;
    public int type = 1;
    public int[] items = null;
    public String texture = null;
    public Map<String, String> mapTextures = null;
    public String model = null;
    public Map<String, String> mapModels = null;
    public RangeListInt damage = null;
    public boolean damagePercent = false;
    public int damageMask = 0;
    public RangeListInt stackSize = null;
    public RangeListInt enchantmentIds = null;
    public RangeListInt enchantmentLevels = null;
    public NbtTagValue[] nbtTagValues = null;
    public int hand = 0;
    public int blend = 1;
    public float speed = 0.0F;
    public float rotation = 0.0F;
    public int layer = 0;
    public float duration = 1.0F;
    public int weight = 0;
    public ResourceLocation textureLocation = null;
    public Map mapTextureLocations = null;
    public TextureAtlasSprite sprite = null;
    public Map mapSprites = null;
    public IBakedModel bakedModelTexture = null;
    public Map<String, IBakedModel> mapBakedModelsTexture = null;
    public IBakedModel bakedModelFull = null;
    public Map<String, IBakedModel> mapBakedModelsFull = null;
    private int textureWidth = 0;
    private int textureHeight = 0;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_ENCHANTMENT = 2;
    public static final int TYPE_ARMOR = 3;
    public static final int HAND_ANY = 0;
    public static final int HAND_MAIN = 1;
    public static final int HAND_OFF = 2;
    public static final String INVENTORY = "inventory";

    public CustomItemProperties(Properties props, String path) {
        this.name = parseName(path);
        this.basePath = parseBasePath(path);
        this.type = this.parseType(props.getProperty("type"));
        this.items = this.parseItems(props.getProperty("items"), props.getProperty("matchItems"));
        this.mapModels = parseModels(props, this.basePath);
        this.model = parseModel(props.getProperty("model"), path, this.basePath, this.type, this.mapModels);
        this.mapTextures = parseTextures(props, this.basePath);
        boolean textureFromPath = this.mapModels == null && this.model == null;
        this.texture = parseTexture(
            props.getProperty("texture"),
            props.getProperty("tile"),
            props.getProperty("source"),
            path,
            this.basePath,
            this.type,
            this.mapTextures,
            textureFromPath
        );
        String damageStr = props.getProperty("damage");
        if (damageStr != null) {
            this.damagePercent = damageStr.contains("%");
            damageStr = damageStr.replace("%", "");
            this.damage = this.parseRangeListInt(damageStr);
            this.damageMask = this.parseInt(props.getProperty("damageMask"), 0);
        }

        this.stackSize = this.parseRangeListInt(props.getProperty("stackSize"));
        this.enchantmentIds = this.parseRangeListInt(props.getProperty("enchantmentIDs"), new ParserEnchantmentId());
        this.enchantmentLevels = this.parseRangeListInt(props.getProperty("enchantmentLevels"));
        this.nbtTagValues = this.parseNbtTagValues(props);
        this.hand = this.parseHand(props.getProperty("hand"));
        this.blend = Blender.parseBlend(props.getProperty("blend"));
        this.speed = this.parseFloat(props.getProperty("speed"), 0.0F);
        this.rotation = this.parseFloat(props.getProperty("rotation"), 0.0F);
        this.layer = this.parseInt(props.getProperty("layer"), 0);
        this.weight = this.parseInt(props.getProperty("weight"), 0);
        this.duration = this.parseFloat(props.getProperty("duration"), 1.0F);
    }

    private static String parseName(String path) {
        String str = path;
        int pos = path.lastIndexOf(47);
        if (pos >= 0) {
            str = path.substring(pos + 1);
        }

        int pos2 = str.lastIndexOf(46);
        if (pos2 >= 0) {
            str = str.substring(0, pos2);
        }

        return str;
    }

    private static String parseBasePath(String path) {
        int pos = path.lastIndexOf(47);
        return pos < 0 ? "" : path.substring(0, pos);
    }

    private int parseType(String str) {
        if (str == null) {
            return 1;
        } else if (str.equals("item")) {
            return 1;
        } else if (str.equals("enchantment")) {
            return 2;
        } else if (str.equals("armor")) {
            return 3;
        } else {
            Config.warn("Unknown method: " + str);
            return 0;
        }
    }

    private int[] parseItems(String str, String str2) {
        if (str == null) {
            str = str2;
        }

        if (str == null) {
            return null;
        } else {
            str = str.trim();
            Set setItemIds = new TreeSet();
            String[] tokens = Config.tokenize(str, " ");

            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                int val = Config.parseInt(token, -1);
                if (val >= 0) {
                    setItemIds.add(new Integer(val));
                } else {
                    if (token.contains("-")) {
                        String[] parts = Config.tokenize(token, "-");
                        if (parts.length == 2) {
                            int val1 = Config.parseInt(parts[0], -1);
                            int val2 = Config.parseInt(parts[1], -1);
                            if (val1 >= 0 && val2 >= 0) {
                                int min = Math.min(val1, val2);
                                int max = Math.max(val1, val2);

                                for (int x = min; x <= max; x++) {
                                    setItemIds.add(new Integer(x));
                                }
                                continue;
                            }
                        }
                    }

                    Item item = Item.getByNameOrId(token);
                    if (item == null) {
                        Config.warn("Item not found: " + token);
                    } else {
                        int id = Item.getIdFromItem(item);
                        if (id <= 0) {
                            Config.warn("Item not found: " + token);
                        } else {
                            setItemIds.add(new Integer(id));
                        }
                    }
                }
            }

            Integer[] integers = (Integer[]) setItemIds.toArray(new Integer[setItemIds.size()]);
            int[] ints = new int[integers.length];

            for (int ix = 0; ix < ints.length; ix++) {
                ints[ix] = integers[ix];
            }

            return ints;
        }
    }

    private static String parseTexture(
        String texStr, String texStr2, String texStr3, String path, String basePath, int type, Map<String, String> mapTexs, boolean textureFromPath
    ) {
        if (texStr == null) {
            texStr = texStr2;
        }

        if (texStr == null) {
            texStr = texStr3;
        }

        if (texStr != null) {
            String png = ".png";
            if (texStr.endsWith(png)) {
                texStr = texStr.substring(0, texStr.length() - png.length());
            }

            return fixTextureName(texStr, basePath);
        } else if (type == 3) {
            return null;
        } else {
            if (mapTexs != null) {
                String bowStandbyTex = mapTexs.get("texture.bow_standby");
                if (bowStandbyTex != null) {
                    return bowStandbyTex;
                }
            }

            if (!textureFromPath) {
                return null;
            } else {
                String str = path;
                int pos = path.lastIndexOf(47);
                if (pos >= 0) {
                    str = path.substring(pos + 1);
                }

                int pos2 = str.lastIndexOf(46);
                if (pos2 >= 0) {
                    str = str.substring(0, pos2);
                }

                return fixTextureName(str, basePath);
            }
        }
    }

    private static Map parseTextures(Properties props, String basePath) {
        String prefix = "texture.";
        Map mapProps = getMatchingProperties(props, prefix);
        if (mapProps.size() <= 0) {
            return null;
        } else {
            Set keySet = mapProps.keySet();
            Map mapTex = new LinkedHashMap();

            for (String key : (Iterable<String>) (Iterable) keySet) {
                String val = (String)mapProps.get(key);
                val = fixTextureName(val, basePath);
                mapTex.put(key, val);
            }

            return mapTex;
        }
    }

    private static String fixTextureName(String iconName, String basePath) {
        iconName = TextureUtils.fixResourcePath(iconName, basePath);
        if (!iconName.startsWith(basePath) && !iconName.startsWith("textures/") && !iconName.startsWith("mcpatcher/")) {
            iconName = basePath + "/" + iconName;
        }

        if (iconName.endsWith(".png")) {
            iconName = iconName.substring(0, iconName.length() - 4);
        }

        if (iconName.startsWith("/")) {
            iconName = iconName.substring(1);
        }

        return iconName;
    }

    private static String parseModel(String modelStr, String path, String basePath, int type, Map<String, String> mapModelNames) {
        if (modelStr != null) {
            String json = ".json";
            if (modelStr.endsWith(json)) {
                modelStr = modelStr.substring(0, modelStr.length() - json.length());
            }

            return fixModelName(modelStr, basePath);
        } else if (type == 3) {
            return null;
        } else {
            if (mapModelNames != null) {
                String bowStandbyModel = mapModelNames.get("model.bow_standby");
                if (bowStandbyModel != null) {
                    return bowStandbyModel;
                }
            }

            return modelStr;
        }
    }

    private static Map parseModels(Properties props, String basePath) {
        String prefix = "model.";
        Map mapProps = getMatchingProperties(props, prefix);
        if (mapProps.size() <= 0) {
            return null;
        } else {
            Set keySet = mapProps.keySet();
            Map mapTex = new LinkedHashMap();

            for (String key : (Iterable<String>) (Iterable) keySet) {
                String val = (String)mapProps.get(key);
                val = fixModelName(val, basePath);
                mapTex.put(key, val);
            }

            return mapTex;
        }
    }

    private static String fixModelName(String modelName, String basePath) {
        modelName = TextureUtils.fixResourcePath(modelName, basePath);
        boolean isVanilla = modelName.startsWith("block/") || modelName.startsWith("item/");
        if (!modelName.startsWith(basePath) && !isVanilla && !modelName.startsWith("mcpatcher/")) {
            modelName = basePath + "/" + modelName;
        }

        String json = ".json";
        if (modelName.endsWith(json)) {
            modelName = modelName.substring(0, modelName.length() - json.length());
        }

        if (modelName.startsWith("/")) {
            modelName = modelName.substring(1);
        }

        return modelName;
    }

    private int parseInt(String str, int defVal) {
        if (str == null) {
            return defVal;
        } else {
            str = str.trim();
            int val = Config.parseInt(str, Integer.MIN_VALUE);
            if (val == Integer.MIN_VALUE) {
                Config.warn("Invalid integer: " + str);
                return defVal;
            } else {
                return val;
            }
        }
    }

    private float parseFloat(String str, float defVal) {
        if (str == null) {
            return defVal;
        } else {
            str = str.trim();
            float val = Config.parseFloat(str, Float.MIN_VALUE);
            if (val == Float.MIN_VALUE) {
                Config.warn("Invalid float: " + str);
                return defVal;
            } else {
                return val;
            }
        }
    }

    private RangeListInt parseRangeListInt(String str) {
        return this.parseRangeListInt(str, null);
    }

    private RangeListInt parseRangeListInt(String str, IParserInt parser) {
        if (str == null) {
            return null;
        } else {
            String[] tokens = Config.tokenize(str, " ");
            RangeListInt rangeList = new RangeListInt();

            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (parser != null) {
                    int val = parser.parse(token, Integer.MIN_VALUE);
                    if (val != Integer.MIN_VALUE) {
                        rangeList.addRange(new RangeInt(val, val));
                        continue;
                    }
                }

                RangeInt range = this.parseRangeInt(token);
                if (range == null) {
                    Config.warn("Invalid range list: " + str);
                    return null;
                }

                rangeList.addRange(range);
            }

            return rangeList;
        }
    }

    private RangeInt parseRangeInt(String str) {
        if (str == null) {
            return null;
        } else {
            str = str.trim();
            int countMinus = str.length() - str.replace("-", "").length();
            if (countMinus > 1) {
                Config.warn("Invalid range: " + str);
                return null;
            } else {
                String[] tokens = Config.tokenize(str, "- ");
                int[] vals = new int[tokens.length];

                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    int val = Config.parseInt(token, -1);
                    if (val < 0) {
                        Config.warn("Invalid range: " + str);
                        return null;
                    }

                    vals[i] = val;
                }

                if (vals.length == 1) {
                    int val = vals[0];
                    if (str.startsWith("-")) {
                        return new RangeInt(0, val);
                    } else {
                        return str.endsWith("-") ? new RangeInt(val, 65535) : new RangeInt(val, val);
                    }
                } else if (vals.length == 2) {
                    int min = Math.min(vals[0], vals[1]);
                    int max = Math.max(vals[0], vals[1]);
                    return new RangeInt(min, max);
                } else {
                    Config.warn("Invalid range: " + str);
                    return null;
                }
            }
        }
    }

    private NbtTagValue[] parseNbtTagValues(Properties props) {
        String PREFIX_NBT = "nbt.";
        Map mapNbt = getMatchingProperties(props, PREFIX_NBT);
        if (mapNbt.size() <= 0) {
            return null;
        } else {
            List listNbts = new ArrayList();

            for (String key : (Iterable<String>) (Iterable) mapNbt.keySet()) {
                String val = (String)mapNbt.get(key);
                String id = key.substring(PREFIX_NBT.length());
                NbtTagValue nbt = new NbtTagValue(id, val);
                listNbts.add(nbt);
            }

            NbtTagValue[] nbts = (NbtTagValue[]) listNbts.toArray(new NbtTagValue[listNbts.size()]);
            return nbts;
        }
    }

    private static Map getMatchingProperties(Properties props, String keyPrefix) {
        Map map = new LinkedHashMap();

        for (String key : (Iterable<String>) (Iterable) props.keySet()) {
            String val = props.getProperty(key);
            if (key.startsWith(keyPrefix)) {
                map.put(key, val);
            }
        }

        return map;
    }

    private int parseHand(String str) {
        if (str == null) {
            return 0;
        } else {
            str = str.toLowerCase();
            if (str.equals("any")) {
                return 0;
            } else if (str.equals("main")) {
                return 1;
            } else if (str.equals("off")) {
                return 2;
            } else {
                Config.warn("Invalid hand: " + str);
                return 0;
            }
        }
    }

    public boolean isValid(String path) {
        if (this.name == null || this.name.length() <= 0) {
            Config.warn("No name found: " + path);
            return false;
        } else if (this.basePath == null) {
            Config.warn("No base path found: " + path);
            return false;
        } else if (this.type == 0) {
            Config.warn("No type defined: " + path);
            return false;
        } else {
            if (this.type == 1 || this.type == 3) {
                if (this.items == null) {
                    this.items = this.detectItems();
                }

                if (this.items == null) {
                    Config.warn("No items defined: " + path);
                    return false;
                }
            }

            if (this.texture == null && this.mapTextures == null && this.model == null && this.mapModels == null) {
                Config.warn("No texture or model specified: " + path);
                return false;
            } else if (this.type == 2 && this.enchantmentIds == null) {
                Config.warn("No enchantmentIDs specified: " + path);
                return false;
            } else {
                return true;
            }
        }
    }

    private int[] detectItems() {
        Item item = Item.getByNameOrId(this.name);
        if (item == null) {
            return null;
        } else {
            int id = Item.getIdFromItem(item);
            return id <= 0 ? null : new int[]{id};
        }
    }

    public void updateIcons(TextureMap textureMap) {
        if (this.texture != null) {
            this.textureLocation = this.getTextureLocation(this.texture);
            if (this.type == 1) {
                ResourceLocation spriteLocation = this.getSpriteLocation(this.textureLocation);
                this.sprite = textureMap.registerSprite(spriteLocation);
            }
        }

        if (this.mapTextures != null) {
            this.mapTextureLocations = new HashMap();
            this.mapSprites = new HashMap();

            for (String key : this.mapTextures.keySet()) {
                String val = this.mapTextures.get(key);
                ResourceLocation locTex = this.getTextureLocation(val);
                this.mapTextureLocations.put(key, locTex);
                if (this.type == 1) {
                    ResourceLocation locSprite = this.getSpriteLocation(locTex);
                    TextureAtlasSprite icon = textureMap.registerSprite(locSprite);
                    this.mapSprites.put(key, icon);
                }
            }
        }
    }

    private ResourceLocation getTextureLocation(String texName) {
        if (texName == null) {
            return null;
        } else {
            ResourceLocation resLoc = new ResourceLocation(texName);
            String domain = resLoc.getResourceDomain();
            String path = resLoc.getResourcePath();
            if (!path.contains("/")) {
                path = "textures/items/" + path;
            }

            String filePath = path + ".png";
            ResourceLocation locFile = new ResourceLocation(domain, filePath);
            boolean exists = Config.hasResource(locFile);
            if (!exists) {
                Config.warn("File not found: " + filePath);
            }

            return locFile;
        }
    }

    private ResourceLocation getSpriteLocation(ResourceLocation resLoc) {
        String pathTex = resLoc.getResourcePath();
        pathTex = StrUtils.removePrefix(pathTex, "textures/");
        pathTex = StrUtils.removeSuffix(pathTex, ".png");
        return new ResourceLocation(resLoc.getResourceDomain(), pathTex);
    }

    public void updateModelTexture(TextureMap textureMap, ItemModelGenerator itemModelGenerator) {
        if (this.texture != null || this.mapTextures != null) {
            String[] textures = this.getModelTextures();
            boolean useTint = this.isUseTint();
            this.bakedModelTexture = makeBakedModel(textureMap, itemModelGenerator, textures, useTint);
            if (this.type == 1 && this.mapTextures != null) {
                for (String key : this.mapTextures.keySet()) {
                    String tex = this.mapTextures.get(key);
                    String path = StrUtils.removePrefix(key, "texture.");
                    if (path.startsWith("bow") || path.startsWith("fishing_rod") || path.startsWith("shield")) {
                        String[] texNames = new String[]{tex};
                        IBakedModel modelTex = makeBakedModel(textureMap, itemModelGenerator, texNames, useTint);
                        if (this.mapBakedModelsTexture == null) {
                            this.mapBakedModelsTexture = new HashMap<>();
                        }

                        this.mapBakedModelsTexture.put(path, modelTex);
                    }
                }
            }
        }
    }

    private boolean isUseTint() {
        return true;
    }

    private static IBakedModel makeBakedModel(TextureMap textureMap, ItemModelGenerator itemModelGenerator, String[] textures, boolean useTint) {
        String[] spriteNames = new String[textures.length];

        for (int i = 0; i < spriteNames.length; i++) {
            String texture = textures[i];
            spriteNames[i] = StrUtils.removePrefix(texture, "textures/");
        }

        ModelBlock modelBlockBase = makeModelBlock(spriteNames);
        ModelBlock modelBlock = itemModelGenerator.makeItemModel(textureMap, modelBlockBase);
        return bakeModel(textureMap, modelBlock, useTint);
    }

    private String[] getModelTextures() {
        if (this.type == 1 && this.items.length == 1) {
            Item item = Item.getItemById(this.items[0]);
            if (item == Items.potionitem && this.damage != null && this.damage.getCountRanges() > 0) {
                RangeInt range = this.damage.getRange(0);
                int valDamage = range.getMin();
                boolean splash = (valDamage & 16384) != 0;
                String texOverlay = this.getMapTexture(this.mapTextures, "texture.potion_overlay", "items/potion_overlay");
                String texMain = null;
                if (splash) {
                    texMain = this.getMapTexture(this.mapTextures, "texture.potion_bottle_splash", "items/potion_bottle_splash");
                } else {
                    texMain = this.getMapTexture(this.mapTextures, "texture.potion_bottle_drinkable", "items/potion_bottle_drinkable");
                }

                return new String[]{texOverlay, texMain};
            }

            if (item instanceof ItemArmor) {
                ItemArmor itemArmor = (ItemArmor)item;
                if (itemArmor.getArmorMaterial() == ArmorMaterial.LEATHER) {
                    String material = "leather";
                    String type = "helmet";
                    if (itemArmor.armorType == 0) {
                        type = "helmet";
                    }

                    if (itemArmor.armorType == 1) {
                        type = "chestplate";
                    }

                    if (itemArmor.armorType == 2) {
                        type = "leggings";
                    }

                    if (itemArmor.armorType == 3) {
                        type = "boots";
                    }

                    String key = material + "_" + type;
                    String texMain = this.getMapTexture(this.mapTextures, "texture." + key, "items/" + key);
                    String texOverlay = this.getMapTexture(this.mapTextures, "texture." + key + "_overlay", "items/" + key + "_overlay");
                    return new String[]{texMain, texOverlay};
                }
            }
        }

        return new String[]{this.texture};
    }

    private String getMapTexture(Map<String, String> map, String key, String def) {
        if (map == null) {
            return def;
        } else {
            String str = map.get(key);
            return str == null ? def : str;
        }
    }

    private static ModelBlock makeModelBlock(String[] modelTextures) {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"parent\": \"builtin/generated\",\"textures\": {");

        for (int i = 0; i < modelTextures.length; i++) {
            String modelTexture = modelTextures[i];
            if (i > 0) {
                sb.append(", ");
            }

            sb.append("\"layer" + i + "\": \"" + modelTexture + "\"");
        }

        sb.append("}}");
        String modelStr = sb.toString();
        return ModelBlock.deserialize(modelStr);
    }

    private static IBakedModel bakeModel(TextureMap textureMap, ModelBlock modelBlockIn, boolean useTint) {
        ModelRotation modelRotationIn = ModelRotation.X0_Y0;
        boolean uvLocked = false;
        String spriteParticleName = modelBlockIn.resolveTextureName("particle");
        TextureAtlasSprite var4 = textureMap.getAtlasSprite(new ResourceLocation(spriteParticleName).toString());
        Builder var5 = new Builder(modelBlockIn).setTexture(var4);

        for (BlockPart var7 : modelBlockIn.getElements()) {
            for (EnumFacing var9 : var7.mapFaces.keySet()) {
                BlockPartFace var10 = var7.mapFaces.get(var9);
                if (!useTint) {
                    var10 = new BlockPartFace(var10.cullFace, -1, var10.texture, var10.blockFaceUV);
                }

                String spriteName = modelBlockIn.resolveTextureName(var10.texture);
                TextureAtlasSprite var11 = textureMap.getAtlasSprite(new ResourceLocation(spriteName).toString());
                BakedQuad quad = makeBakedQuad(var7, var10, var11, var9, modelRotationIn, uvLocked);
                if (var10.cullFace == null) {
                    var5.addGeneralQuad(quad);
                } else {
                    var5.addFaceQuad(modelRotationIn.rotateFace(var10.cullFace), quad);
                }
            }
        }

        return var5.makeBakedModel();
    }

    private static BakedQuad makeBakedQuad(
        BlockPart blockPart,
        BlockPartFace blockPartFace,
        TextureAtlasSprite textureAtlasSprite,
        EnumFacing enumFacing,
        ModelRotation modelRotation,
        boolean uvLocked
    ) {
        FaceBakery faceBakery = new FaceBakery();
        return faceBakery.makeBakedQuad(
            blockPart.positionFrom,
            blockPart.positionTo,
            blockPartFace,
            textureAtlasSprite,
            enumFacing,
            modelRotation,
            blockPart.partRotation,
            uvLocked,
            blockPart.shade
        );
    }

    @Override
    public String toString() {
        return "" + this.basePath + "/" + this.name + ", type: " + this.type + ", items: [" + Config.arrayToString(this.items) + "], textture: " + this.texture;
    }

    public float getTextureWidth(TextureManager textureManager) {
        if (this.textureWidth <= 0) {
            if (this.textureLocation != null) {
                ITextureObject tex = textureManager.getTexture(this.textureLocation);
                int texId = tex.getGlTextureId();
                int prevTexId = GlStateManager.getBoundTexture();
                GlStateManager.bindTexture(texId);
                this.textureWidth = GL11.glGetTexLevelParameteri(3553, 0, 4096);
                GlStateManager.bindTexture(prevTexId);
            }

            if (this.textureWidth <= 0) {
                this.textureWidth = 16;
            }
        }

        return this.textureWidth;
    }

    public float getTextureHeight(TextureManager textureManager) {
        if (this.textureHeight <= 0) {
            if (this.textureLocation != null) {
                ITextureObject tex = textureManager.getTexture(this.textureLocation);
                int texId = tex.getGlTextureId();
                int prevTexId = GlStateManager.getBoundTexture();
                GlStateManager.bindTexture(texId);
                this.textureHeight = GL11.glGetTexLevelParameteri(3553, 0, 4097);
                GlStateManager.bindTexture(prevTexId);
            }

            if (this.textureHeight <= 0) {
                this.textureHeight = 16;
            }
        }

        return this.textureHeight;
    }

    public IBakedModel getBakedModel(ResourceLocation modelLocation, boolean fullModel) {
        IBakedModel bakedModel;
        Map<String, IBakedModel> mapBakedModels;
        if (fullModel) {
            bakedModel = this.bakedModelFull;
            mapBakedModels = this.mapBakedModelsFull;
        } else {
            bakedModel = this.bakedModelTexture;
            mapBakedModels = this.mapBakedModelsTexture;
        }

        if (modelLocation != null && mapBakedModels != null) {
            String modelPath = modelLocation.getResourcePath();
            IBakedModel customModel = mapBakedModels.get(modelPath);
            if (customModel != null) {
                return customModel;
            }
        }

        return bakedModel;
    }

    public void loadModels(ModelBakery modelBakery) {
        if (this.model != null) {
            loadItemModel(modelBakery, this.model);
        }

        if (this.type == 1 && this.mapModels != null) {
            for (String key : this.mapModels.keySet()) {
                String mod = this.mapModels.get(key);
                String path = StrUtils.removePrefix(key, "model.");
                if (path.startsWith("bow") || path.startsWith("fishing_rod") || path.startsWith("shield")) {
                    loadItemModel(modelBakery, mod);
                }
            }
        }
    }

    public void updateModelsFull() {
        ModelManager modelManager = Config.getModelManager();
        IBakedModel missingModel = modelManager.getMissingModel();
        if (this.model != null) {
            ResourceLocation locItem = getModelLocation(this.model);
            ModelResourceLocation mrl = new ModelResourceLocation(locItem, "inventory");
            this.bakedModelFull = modelManager.getModel(mrl);
            if (this.bakedModelFull == missingModel) {
                Config.warn("Custom Items: Model not found " + mrl.getResourcePath());
                this.bakedModelFull = null;
            }
        }

        if (this.type == 1 && this.mapModels != null) {
            for (String key : this.mapModels.keySet()) {
                String mod = this.mapModels.get(key);
                String path = StrUtils.removePrefix(key, "model.");
                if (path.startsWith("bow") || path.startsWith("fishing_rod") || path.startsWith("shield")) {
                    ResourceLocation locItem = getModelLocation(mod);
                    ModelResourceLocation mrl = new ModelResourceLocation(locItem, "inventory");
                    IBakedModel bm = modelManager.getModel(mrl);
                    if (bm == missingModel) {
                        Config.warn("Custom Items: Model not found " + mrl.getResourcePath());
                    } else {
                        if (this.mapBakedModelsFull == null) {
                            this.mapBakedModelsFull = new HashMap<>();
                        }

                        this.mapBakedModelsFull.put(path, bm);
                    }
                }
            }
        }
    }

    private static void loadItemModel(ModelBakery modelBakery, String model) {
        ResourceLocation locItem = getModelLocation(model);
        ModelResourceLocation mrl = new ModelResourceLocation(locItem, "inventory");
        if (Reflector.ModelLoader.exists()) {
            try {
                Object vanillaLoader = Reflector.ModelLoader_VanillaLoader_INSTANCE.getValue();
                checkNull(vanillaLoader, "vanillaLoader is null");
                Object iModel = Reflector.call(vanillaLoader, Reflector.ModelLoader_VanillaLoader_loadModel, mrl);
                checkNull(iModel, "iModel is null");
                Map stateModels = (Map)Reflector.getFieldValue(modelBakery, Reflector.ModelLoader_stateModels);
                checkNull(stateModels, "stateModels is null");
                stateModels.put(mrl, iModel);
                Set registryTextures = (Set)Reflector.getFieldValue(modelBakery, Reflector.ModelLoader_textures);
                checkNull(registryTextures, "registryTextures is null");
                Collection modelTextures = (Collection)Reflector.call(iModel, Reflector.IModel_getTextures);
                checkNull(modelTextures, "modelTextures is null");
                registryTextures.addAll(modelTextures);
            } catch (Exception var9) {
                Config.warn("Error registering model with ModelLoader: " + mrl + ", " + var9.getClass().getName() + ": " + var9.getMessage());
            }
        } else {
            modelBakery.loadItemModel(locItem.toString(), mrl, locItem);
        }
    }

    private static void checkNull(Object obj, String msg) throws NullPointerException {
        if (obj == null) {
            throw new NullPointerException(msg);
        }
    }

    private static ResourceLocation getModelLocation(String modelName) {
        return Reflector.ModelLoader.exists() && !modelName.startsWith("mcpatcher/") && !modelName.startsWith("optifine/")
            ? new ResourceLocation("models/" + modelName)
            : new ResourceLocation(modelName);
    }
}
