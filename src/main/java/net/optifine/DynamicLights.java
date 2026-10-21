package net.optifine;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.optifine.config.ConnectedParser;
import net.optifine.config.EntityClassLocator;
import net.optifine.config.IObjectLocator;
import net.optifine.config.ItemLocator;
import net.optifine.reflect.ReflectorForge;
import net.optifine.util.PropertiesOrdered;

public class DynamicLights {
    private static DynamicLightsMap mapDynamicLights = new DynamicLightsMap();
    private static Map<Class, Integer> mapEntityLightLevels = new HashMap<>();
    private static Map<Item, Integer> mapItemLightLevels = new HashMap<>();
    private static long timeUpdateMs = 0L;
    private static final double MAX_DIST = 7.5;
    private static final double MAX_DIST_SQ = 56.25;
    private static final int LIGHT_LEVEL_MAX = 15;
    private static final int LIGHT_LEVEL_FIRE = 15;
    private static final int LIGHT_LEVEL_BLAZE = 10;
    private static final int LIGHT_LEVEL_MAGMA_CUBE = 8;
    private static final int LIGHT_LEVEL_MAGMA_CUBE_CORE = 13;
    private static final int LIGHT_LEVEL_GLOWSTONE_DUST = 8;
    private static final int LIGHT_LEVEL_PRISMARINE_CRYSTALS = 8;
    private static boolean initialized;

    public static void entityAdded(Entity entityIn, RenderGlobal renderGlobal) {
    }

    public static void entityRemoved(Entity entityIn, RenderGlobal renderGlobal) {
        synchronized (mapDynamicLights) {
            DynamicLight dynamicLight = mapDynamicLights.remove(entityIn.getEntityId());
            if (dynamicLight != null) {
                dynamicLight.updateLitChunks(renderGlobal);
            }
        }
    }

    public static void update(RenderGlobal renderGlobal) {
        long timeNowMs = System.currentTimeMillis();
        if (timeNowMs >= timeUpdateMs + 50L) {
            timeUpdateMs = timeNowMs;
            if (!initialized) {
                initialize();
            }

            synchronized (mapDynamicLights) {
                updateMapDynamicLights(renderGlobal);
                if (mapDynamicLights.size() > 0) {
                    List<DynamicLight> dynamicLights = mapDynamicLights.valueList();

                    for (int i = 0; i < dynamicLights.size(); i++) {
                        DynamicLight dynamicLight = dynamicLights.get(i);
                        dynamicLight.update(renderGlobal);
                    }
                }
            }
        }
    }

    private static void initialize() {
        initialized = true;
        mapEntityLightLevels.clear();
        mapItemLightLevels.clear();
        String[] modIds = ReflectorForge.getForgeModIds();

        for (int i = 0; i < modIds.length; i++) {
            String modId = modIds[i];

            try {
                ResourceLocation loc = new ResourceLocation(modId, "optifine/dynamic_lights.properties");
                InputStream in = Config.getResourceStream(loc);
                loadModConfiguration(in, loc.toString(), modId);
            } catch (IOException var5) {
            }
        }

        if (mapEntityLightLevels.size() > 0) {
            Config.dbg("DynamicLights entities: " + mapEntityLightLevels.size());
        }

        if (mapItemLightLevels.size() > 0) {
            Config.dbg("DynamicLights items: " + mapItemLightLevels.size());
        }
    }

    private static void loadModConfiguration(InputStream in, String path, String modId) {
        if (in != null) {
            try {
                Properties props = new PropertiesOrdered();
                props.load(in);
                in.close();
                Config.dbg("DynamicLights: Parsing " + path);
                ConnectedParser cp = new ConnectedParser("DynamicLights");
                loadModLightLevels(props.getProperty("entities"), mapEntityLightLevels, new EntityClassLocator(), cp, path, modId);
                loadModLightLevels(props.getProperty("items"), mapItemLightLevels, new ItemLocator(), cp, path, modId);
            } catch (IOException var5) {
                Config.warn("DynamicLights: Error reading " + path);
            }
        }
    }

    private static void loadModLightLevels(String prop, Map mapLightLevels, IObjectLocator ol, ConnectedParser cp, String path, String modId) {
        if (prop != null) {
            String[] parts = Config.tokenize(prop, " ");

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String[] tokens = Config.tokenize(part, ":");
                if (tokens.length != 2) {
                    cp.warn("Invalid entry: " + part + ", in:" + path);
                } else {
                    String name = tokens[0];
                    String light = tokens[1];
                    String nameFull = modId + ":" + name;
                    ResourceLocation loc = new ResourceLocation(nameFull);
                    Object obj = ol.getObject(loc);
                    if (obj == null) {
                        cp.warn("Object not found: " + nameFull);
                    } else {
                        int lightLevel = cp.parseInt(light, -1);
                        if (lightLevel >= 0 && lightLevel <= 15) {
                            mapLightLevels.put(obj, new Integer(lightLevel));
                        } else {
                            cp.warn("Invalid light level: " + part);
                        }
                    }
                }
            }
        }
    }

    private static void updateMapDynamicLights(RenderGlobal renderGlobal) {
        World world = renderGlobal.getWorld();
        if (world != null) {
            for (Entity entity : world.getLoadedEntityList()) {
                int lightLevel = getLightLevel(entity);
                if (lightLevel > 0) {
                    int key = entity.getEntityId();
                    DynamicLight dynamicLight = mapDynamicLights.get(key);
                    if (dynamicLight == null) {
                        dynamicLight = new DynamicLight(entity);
                        mapDynamicLights.put(key, dynamicLight);
                    }
                } else {
                    int key = entity.getEntityId();
                    DynamicLight dynamicLight = mapDynamicLights.remove(key);
                    if (dynamicLight != null) {
                        dynamicLight.updateLitChunks(renderGlobal);
                    }
                }
            }
        }
    }

    public static int getCombinedLight(BlockPos pos, int combinedLight) {
        double lightPlayer = getLightLevel(pos);
        return getCombinedLight(lightPlayer, combinedLight);
    }

    public static int getCombinedLight(Entity entity, int combinedLight) {
        double lightPlayer = getLightLevel(entity);
        return getCombinedLight(lightPlayer, combinedLight);
    }

    public static int getCombinedLight(double lightPlayer, int combinedLight) {
        if (lightPlayer > 0.0) {
            int lightPlayerFF = (int)(lightPlayer * 16.0);
            int lightBlockFF = combinedLight & 0xFF;
            if (lightPlayerFF > lightBlockFF) {
                combinedLight &= -256;
                combinedLight |= lightPlayerFF;
            }
        }

        return combinedLight;
    }

    public static double getLightLevel(BlockPos pos) {
        double lightLevelMax = 0.0;
        synchronized (mapDynamicLights) {
            List<DynamicLight> dynamicLights = mapDynamicLights.valueList();
            int dynamicLightsSize = dynamicLights.size();

            for (int i = 0; i < dynamicLightsSize; i++) {
                DynamicLight dynamicLight = dynamicLights.get(i);
                int dynamicLightLevel = dynamicLight.getLastLightLevel();
                if (dynamicLightLevel > 0) {
                    double px = dynamicLight.getLastPosX();
                    double py = dynamicLight.getLastPosY();
                    double pz = dynamicLight.getLastPosZ();
                    double dx = pos.getX() - px;
                    double dy = pos.getY() - py;
                    double dz = pos.getZ() - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (dynamicLight.isUnderwater() && !Config.isClearWater()) {
                        dynamicLightLevel = Config.limit(dynamicLightLevel - 2, 0, 15);
                        distSq *= 2.0;
                    }

                    if (!(distSq > 56.25)) {
                        double dist = Math.sqrt(distSq);
                        double light = 1.0 - dist / 7.5;
                        double lightLevel = light * dynamicLightLevel;
                        if (lightLevel > lightLevelMax) {
                            lightLevelMax = lightLevel;
                        }
                    }
                }
            }
        }

        return Config.limit(lightLevelMax, 0.0, 15.0);
    }

    public static int getLightLevel(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        } else {
            Item item = itemStack.getItem();
            if (item instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock)item;
                Block block = itemBlock.getBlock();
                if (block != null) {
                    return block.getLightValue();
                }
            }

            if (item == Items.lava_bucket) {
                return Blocks.lava.getLightValue();
            } else if (item == Items.blaze_rod || item == Items.blaze_powder) {
                return 10;
            } else if (item == Items.glowstone_dust) {
                return 8;
            } else if (item == Items.prismarine_crystals) {
                return 8;
            } else if (item == Items.magma_cream) {
                return 8;
            } else if (item == Items.nether_star) {
                return Blocks.beacon.getLightValue() / 2;
            } else {
                if (!mapItemLightLevels.isEmpty()) {
                    Integer level = mapItemLightLevels.get(item);
                    if (level != null) {
                        return level;
                    }
                }

                return 0;
            }
        }
    }

    public static int getLightLevel(Entity entity) {
        if (entity == Config.getMinecraft().getRenderViewEntity() && !Config.isDynamicHandLight()) {
            return 0;
        } else {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)entity;
                if (player.isSpectator()) {
                    return 0;
                }
            }

            if (entity.isBurning()) {
                return 15;
            } else {
                if (!mapEntityLightLevels.isEmpty()) {
                    Integer level = mapEntityLightLevels.get(entity.getClass());
                    if (level != null) {
                        return level;
                    }
                }

                if (entity instanceof EntityFireball) {
                    return 15;
                } else if (entity instanceof EntityTNTPrimed) {
                    return 15;
                } else if (entity instanceof EntityBlaze) {
                    EntityBlaze entityBlaze = (EntityBlaze)entity;
                    return entityBlaze.func_70845_n() ? 15 : 10;
                } else if (entity instanceof EntityMagmaCube) {
                    EntityMagmaCube emc = (EntityMagmaCube)entity;
                    return emc.squishFactor > 0.6 ? 13 : 8;
                } else {
                    if (entity instanceof EntityCreeper) {
                        EntityCreeper entityCreeper = (EntityCreeper)entity;
                        if (entityCreeper.getCreeperFlashIntensity(0.0F) > 0.001) {
                            return 15;
                        }
                    }

                    if (entity instanceof EntityLivingBase) {
                        EntityLivingBase player = (EntityLivingBase)entity;
                        ItemStack stackMain = player.getHeldItem();
                        int levelMain = getLightLevel(stackMain);
                        ItemStack stackHead = player.getEquipmentInSlot(4);
                        int levelHead = getLightLevel(stackHead);
                        return Math.max(levelMain, levelHead);
                    } else if (entity instanceof EntityItem) {
                        EntityItem entityItem = (EntityItem)entity;
                        ItemStack itemStack = getItemStack(entityItem);
                        return getLightLevel(itemStack);
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    public static void removeLights(RenderGlobal renderGlobal) {
        synchronized (mapDynamicLights) {
            List<DynamicLight> dynamicLights = mapDynamicLights.valueList();

            for (int i = 0; i < dynamicLights.size(); i++) {
                DynamicLight dynamicLight = dynamicLights.get(i);
                dynamicLight.updateLitChunks(renderGlobal);
            }

            mapDynamicLights.clear();
        }
    }

    public static void clear() {
        synchronized (mapDynamicLights) {
            mapDynamicLights.clear();
        }
    }

    public static int getCount() {
        synchronized (mapDynamicLights) {
            return mapDynamicLights.size();
        }
    }

    public static ItemStack getItemStack(EntityItem entityItem) {
        return entityItem.getDataWatcher().getWatchableObjectItemStack(10);
    }
}
