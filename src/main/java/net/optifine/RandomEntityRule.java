package net.optifine;

import java.util.Properties;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.config.ConnectedParser;
import net.optifine.config.Matches;
import net.optifine.config.NbtTagValue;
import net.optifine.config.RangeInt;
import net.optifine.config.RangeListInt;
import net.optifine.config.VillagerProfession;
import net.optifine.config.Weather;
import net.optifine.reflect.Reflector;
import net.optifine.util.ArrayUtils;
import net.optifine.util.MathUtils;

public class RandomEntityRule {
    private String pathProps = null;
    private ResourceLocation baseResLoc = null;
    private int index;
    private int[] textures = null;
    private ResourceLocation[] resourceLocations = null;
    private int[] weights = null;
    private BiomeGenBase[] biomes = null;
    private RangeListInt heights = null;
    private RangeListInt healthRange = null;
    private boolean healthPercent = false;
    private NbtTagValue nbtName = null;
    public int[] sumWeights = null;
    public int sumAllWeights = 1;
    private VillagerProfession[] professions = null;
    private EnumDyeColor[] collarColors = null;
    private Boolean baby = null;
    private RangeListInt moonPhases = null;
    private RangeListInt dayTimes = null;
    private Weather[] weatherList = null;

    public RandomEntityRule(Properties props, String pathProps, ResourceLocation baseResLoc, int index, String valTextures, ConnectedParser cp) {
        this.pathProps = pathProps;
        this.baseResLoc = baseResLoc;
        this.index = index;
        this.textures = cp.parseIntList(valTextures);
        this.weights = cp.parseIntList(props.getProperty("weights." + index));
        this.biomes = cp.parseBiomes(props.getProperty("biomes." + index));
        this.heights = cp.parseRangeListInt(props.getProperty("heights." + index));
        if (this.heights == null) {
            this.heights = this.parseMinMaxHeight(props, index);
        }

        String healthStr = props.getProperty("health." + index);
        if (healthStr != null) {
            this.healthPercent = healthStr.contains("%");
            healthStr = healthStr.replace("%", "");
            this.healthRange = cp.parseRangeListInt(healthStr);
        }

        this.nbtName = cp.parseNbtTagValue("name", props.getProperty("name." + index));
        this.professions = cp.parseProfessions(props.getProperty("professions." + index));
        this.collarColors = cp.parseDyeColors(props.getProperty("collarColors." + index), "collar color", ConnectedParser.DYE_COLORS_INVALID);
        this.baby = cp.parseBooleanObject(props.getProperty("baby." + index));
        this.moonPhases = cp.parseRangeListInt(props.getProperty("moonPhase." + index));
        this.dayTimes = cp.parseRangeListInt(props.getProperty("dayTime." + index));
        this.weatherList = cp.parseWeather(props.getProperty("weather." + index), "weather." + index, null);
    }

    private RangeListInt parseMinMaxHeight(Properties props, int index) {
        String minHeightStr = props.getProperty("minHeight." + index);
        String maxHeightStr = props.getProperty("maxHeight." + index);
        if (minHeightStr == null && maxHeightStr == null) {
            return null;
        } else {
            int minHeight = 0;
            if (minHeightStr != null) {
                minHeight = Config.parseInt(minHeightStr, -1);
                if (minHeight < 0) {
                    Config.warn("Invalid minHeight: " + minHeightStr);
                    return null;
                }
            }

            int maxHeight = 256;
            if (maxHeightStr != null) {
                maxHeight = Config.parseInt(maxHeightStr, -1);
                if (maxHeight < 0) {
                    Config.warn("Invalid maxHeight: " + maxHeightStr);
                    return null;
                }
            }

            if (maxHeight < 0) {
                Config.warn("Invalid minHeight, maxHeight: " + minHeightStr + ", " + maxHeightStr);
                return null;
            } else {
                RangeListInt list = new RangeListInt();
                list.addRange(new RangeInt(minHeight, maxHeight));
                return list;
            }
        }
    }

    public boolean isValid(String path) {
        if (this.textures == null || this.textures.length == 0) {
            Config.warn("Invalid skins for rule: " + this.index);
            return false;
        } else if (this.resourceLocations != null) {
            return true;
        } else {
            this.resourceLocations = new ResourceLocation[this.textures.length];
            boolean mcpatcher = this.pathProps.startsWith("mcpatcher/mob/");
            ResourceLocation locMcp = RandomEntities.getLocationRandom(this.baseResLoc, mcpatcher);
            if (locMcp == null) {
                Config.warn("Invalid path: " + this.baseResLoc.getResourcePath());
                return false;
            } else {
                for (int i = 0; i < this.resourceLocations.length; i++) {
                    int index = this.textures[i];
                    if (index <= 1) {
                        this.resourceLocations[i] = this.baseResLoc;
                    } else {
                        ResourceLocation locNew = RandomEntities.getLocationIndexed(locMcp, index);
                        if (locNew == null) {
                            Config.warn("Invalid path: " + this.baseResLoc.getResourcePath());
                            return false;
                        }

                        if (!Config.hasResource(locNew)) {
                            Config.warn("Texture not found: " + locNew.getResourcePath());
                            return false;
                        }

                        this.resourceLocations[i] = locNew;
                    }
                }

                if (this.weights != null) {
                    if (this.weights.length > this.resourceLocations.length) {
                        Config.warn("More weights defined than skins, trimming weights: " + path);
                        int[] weights2 = new int[this.resourceLocations.length];
                        System.arraycopy(this.weights, 0, weights2, 0, weights2.length);
                        this.weights = weights2;
                    }

                    if (this.weights.length < this.resourceLocations.length) {
                        Config.warn("Less weights defined than skins, expanding weights: " + path);
                        int[] weights2 = new int[this.resourceLocations.length];
                        System.arraycopy(this.weights, 0, weights2, 0, this.weights.length);
                        int avgWeight = MathUtils.getAverage(this.weights);

                        for (int ix = this.weights.length; ix < weights2.length; ix++) {
                            weights2[ix] = avgWeight;
                        }

                        this.weights = weights2;
                    }

                    this.sumWeights = new int[this.weights.length];
                    int sum = 0;

                    for (int ix = 0; ix < this.weights.length; ix++) {
                        if (this.weights[ix] < 0) {
                            Config.warn("Invalid weight: " + this.weights[ix]);
                            return false;
                        }

                        sum += this.weights[ix];
                        this.sumWeights[ix] = sum;
                    }

                    this.sumAllWeights = sum;
                    if (this.sumAllWeights <= 0) {
                        Config.warn("Invalid sum of all weights: " + sum);
                        this.sumAllWeights = 1;
                    }
                }

                if (this.professions == ConnectedParser.PROFESSIONS_INVALID) {
                    Config.warn("Invalid professions or careers: " + path);
                    return false;
                } else if (this.collarColors == ConnectedParser.DYE_COLORS_INVALID) {
                    Config.warn("Invalid collar colors: " + path);
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    public boolean matches(IRandomEntity randomEntity) {
        if (this.biomes != null && !Matches.biome(randomEntity.getSpawnBiome(), this.biomes)) {
            return false;
        } else {
            if (this.heights != null) {
                BlockPos pos = randomEntity.getSpawnPosition();
                if (pos != null && !this.heights.isInRange(pos.getY())) {
                    return false;
                }
            }

            if (this.healthRange != null) {
                int health = randomEntity.getHealth();
                if (this.healthPercent) {
                    int healthMax = randomEntity.getMaxHealth();
                    if (healthMax > 0) {
                        health = (int)((double)(health * 100) / healthMax);
                    }
                }

                if (!this.healthRange.isInRange(health)) {
                    return false;
                }
            }

            if (this.nbtName != null) {
                String name = randomEntity.getName();
                if (!this.nbtName.matchesValue(name)) {
                    return false;
                }
            }

            if (this.professions != null && randomEntity instanceof RandomEntity) {
                RandomEntity rme = (RandomEntity)randomEntity;
                Entity entity = rme.getEntity();
                if (entity instanceof EntityVillager) {
                    EntityVillager entityVillager = (EntityVillager)entity;
                    int profInt = entityVillager.getProfession();
                    int careerInt = Reflector.getFieldValueInt(entityVillager, Reflector.EntityVillager_careerId, -1);
                    if (profInt < 0 || careerInt < 0) {
                        return false;
                    }

                    boolean matchProfession = false;

                    for (int i = 0; i < this.professions.length; i++) {
                        VillagerProfession prof = this.professions[i];
                        if (prof.matches(profInt, careerInt)) {
                            matchProfession = true;
                            break;
                        }
                    }

                    if (!matchProfession) {
                        return false;
                    }
                }
            }

            if (this.collarColors != null && randomEntity instanceof RandomEntity) {
                RandomEntity rme = (RandomEntity)randomEntity;
                Entity entity = rme.getEntity();
                if (entity instanceof EntityWolf) {
                    EntityWolf entityWolf = (EntityWolf)entity;
                    if (!entityWolf.isTamed()) {
                        return false;
                    }

                    EnumDyeColor col = entityWolf.getCollarColor();
                    if (!Config.equalsOne(col, this.collarColors)) {
                        return false;
                    }
                }
            }

            if (this.baby != null && randomEntity instanceof RandomEntity) {
                RandomEntity rme = (RandomEntity)randomEntity;
                Entity entity = rme.getEntity();
                if (entity instanceof EntityLiving) {
                    EntityLiving livingEntity = (EntityLiving)entity;
                    if (livingEntity.isChild() != this.baby) {
                        return false;
                    }
                }
            }

            if (this.moonPhases != null) {
                World world = Config.getMinecraft().theWorld;
                if (world != null) {
                    int moonPhase = world.getMoonPhase();
                    if (!this.moonPhases.isInRange(moonPhase)) {
                        return false;
                    }
                }
            }

            if (this.dayTimes != null) {
                World world = Config.getMinecraft().theWorld;
                if (world != null) {
                    int dayTime = (int)world.getWorldInfo().getWorldTime();
                    if (!this.dayTimes.isInRange(dayTime)) {
                        return false;
                    }
                }
            }

            if (this.weatherList != null) {
                World world = Config.getMinecraft().theWorld;
                if (world != null) {
                    Weather weather = Weather.getWeather(world, 0.0F);
                    if (!ArrayUtils.contains(this.weatherList, weather)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public ResourceLocation getTextureLocation(ResourceLocation loc, int randomId) {
        if (this.resourceLocations != null && this.resourceLocations.length != 0) {
            int index = 0;
            if (this.weights == null) {
                index = randomId % this.resourceLocations.length;
            } else {
                int randWeight = randomId % this.sumAllWeights;

                for (int i = 0; i < this.sumWeights.length; i++) {
                    if (this.sumWeights[i] > randWeight) {
                        index = i;
                        break;
                    }
                }
            }

            return this.resourceLocations[index];
        } else {
            return loc;
        }
    }
}
