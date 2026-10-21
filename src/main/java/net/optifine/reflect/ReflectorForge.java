package net.optifine.reflect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

public class ReflectorForge {
    public static Object EVENT_RESULT_ALLOW = Reflector.getFieldValue(Reflector.Event_Result_ALLOW);
    public static Object EVENT_RESULT_DENY = Reflector.getFieldValue(Reflector.Event_Result_DENY);
    public static Object EVENT_RESULT_DEFAULT = Reflector.getFieldValue(Reflector.Event_Result_DEFAULT);

    public static void FMLClientHandler_trackBrokenTexture(ResourceLocation loc, String message) {
        if (!Reflector.FMLClientHandler_trackBrokenTexture.exists()) {
            Object instance = Reflector.call(Reflector.FMLClientHandler_instance);
            Reflector.call(instance, Reflector.FMLClientHandler_trackBrokenTexture, loc, message);
        }
    }

    public static void FMLClientHandler_trackMissingTexture(ResourceLocation loc) {
        if (!Reflector.FMLClientHandler_trackMissingTexture.exists()) {
            Object instance = Reflector.call(Reflector.FMLClientHandler_instance);
            Reflector.call(instance, Reflector.FMLClientHandler_trackMissingTexture, loc);
        }
    }

    public static void putLaunchBlackboard(String key, Object value) {
        Map blackboard = (Map)Reflector.getFieldValue(Reflector.Launch_blackboard);
        if (blackboard != null) {
            blackboard.put(key, value);
        }
    }

    public static boolean renderFirstPersonHand(RenderGlobal renderGlobal, float partialTicks, int pass) {
        return !Reflector.ForgeHooksClient_renderFirstPersonHand.exists()
            ? false
            : Reflector.callBoolean(Reflector.ForgeHooksClient_renderFirstPersonHand, renderGlobal, partialTicks, pass);
    }

    public static InputStream getOptiFineResourceStream(String path) {
        if (!Reflector.OptiFineClassTransformer_instance.exists()) {
            return null;
        } else {
            Object instance = Reflector.getFieldValue(Reflector.OptiFineClassTransformer_instance);
            if (instance == null) {
                return null;
            } else {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                byte[] bytes = (byte[])Reflector.call(instance, Reflector.OptiFineClassTransformer_getOptiFineResource, path);
                if (bytes == null) {
                    return null;
                } else {
                    InputStream in = new ByteArrayInputStream(bytes);
                    return in;
                }
            }
        }
    }

    public static boolean blockHasTileEntity(IBlockState state) {
        Block block = state.getBlock();
        return !Reflector.ForgeBlock_hasTileEntity.exists() ? block.hasTileEntity() : Reflector.callBoolean(block, Reflector.ForgeBlock_hasTileEntity, state);
    }

    public static boolean isItemDamaged(ItemStack stack) {
        return !Reflector.ForgeItem_showDurabilityBar.exists()
            ? stack.isItemDamaged()
            : Reflector.callBoolean(stack.getItem(), Reflector.ForgeItem_showDurabilityBar, stack);
    }

    public static boolean armorHasOverlay(ItemArmor itemArmor, ItemStack itemStack) {
        int i = itemArmor.getColor(itemStack);
        return i != -1;
    }

    public static MapData getMapData(ItemMap itemMap, ItemStack stack, World world) {
        return Reflector.ForgeHooksClient.exists() ? ((ItemMap)stack.getItem()).getMapData(stack, world) : itemMap.getMapData(stack, world);
    }

    public static String[] getForgeModIds() {
        if (!Reflector.Loader.exists()) {
            return new String[0];
        } else {
            Object loader = Reflector.call(Reflector.Loader_instance);
            List listActiveMods = (List)Reflector.call(loader, Reflector.Loader_getActiveModList);
            if (listActiveMods == null) {
                return new String[0];
            } else {
                List<String> listModIds = new ArrayList<>();

                for (Object modContainer : listActiveMods) {
                    if (Reflector.ModContainer.isInstance(modContainer)) {
                        String modId = Reflector.callString(modContainer, Reflector.ModContainer_getModId);
                        if (modId != null) {
                            listModIds.add(modId);
                        }
                    }
                }

                return listModIds.toArray(new String[listModIds.size()]);
            }
        }
    }

    public static boolean canEntitySpawn(EntityLiving entityliving, World world, float x, float y, float z) {
        Object canSpawn = Reflector.call(Reflector.ForgeEventFactory_canEntitySpawn, entityliving, world, x, y, z);
        return canSpawn == EVENT_RESULT_ALLOW || canSpawn == EVENT_RESULT_DEFAULT && entityliving.getCanSpawnHere() && entityliving.isNotColliding();
    }

    public static boolean doSpecialSpawn(EntityLiving entityliving, World world, float x, int y, float z) {
        return Reflector.ForgeEventFactory_doSpecialSpawn.exists()
            ? Reflector.callBoolean(Reflector.ForgeEventFactory_doSpecialSpawn, entityliving, world, x, y, z)
            : false;
    }
}
