package net.optifine.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldNameable;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class TileEntityUtils {
    public static String getTileEntityName(IBlockAccess blockAccess, BlockPos blockPos) {
        TileEntity te = blockAccess.getTileEntity(blockPos);
        return getTileEntityName(te);
    }

    public static String getTileEntityName(TileEntity te) {
        if (!(te instanceof IWorldNameable)) {
            return null;
        } else {
            IWorldNameable iwn = (IWorldNameable)te;
            updateTileEntityName(te);
            return !iwn.hasCustomName() ? null : iwn.getName();
        }
    }

    public static void updateTileEntityName(TileEntity te) {
        BlockPos pos = te.getPos();
        String name = getTileEntityRawName(te);
        if (name == null) {
            String nameServer = getServerTileEntityRawName(pos);
            nameServer = Config.normalize(nameServer);
            setTileEntityRawName(te, nameServer);
        }
    }

    public static String getServerTileEntityRawName(BlockPos blockPos) {
        TileEntity tes = IntegratedServerUtils.getTileEntity(blockPos);
        return tes == null ? null : getTileEntityRawName(tes);
    }

    public static String getTileEntityRawName(TileEntity te) {
        if (te instanceof TileEntityBeacon) {
            return (String)Reflector.getFieldValue(te, Reflector.TileEntityBeacon_customName);
        } else if (te instanceof TileEntityBrewingStand) {
            return (String)Reflector.getFieldValue(te, Reflector.TileEntityBrewingStand_customName);
        } else if (te instanceof TileEntityEnchantmentTable) {
            return (String)Reflector.getFieldValue(te, Reflector.TileEntityEnchantmentTable_customName);
        } else if (te instanceof TileEntityFurnace) {
            return (String)Reflector.getFieldValue(te, Reflector.TileEntityFurnace_customName);
        } else {
            if (te instanceof IWorldNameable) {
                IWorldNameable iwn = (IWorldNameable)te;
                if (iwn.hasCustomName()) {
                    return iwn.getName();
                }
            }

            return null;
        }
    }

    public static boolean setTileEntityRawName(TileEntity te, String name) {
        if (te instanceof TileEntityBeacon) {
            return Reflector.setFieldValue(te, Reflector.TileEntityBeacon_customName, name);
        } else if (te instanceof TileEntityBrewingStand) {
            return Reflector.setFieldValue(te, Reflector.TileEntityBrewingStand_customName, name);
        } else if (te instanceof TileEntityEnchantmentTable) {
            return Reflector.setFieldValue(te, Reflector.TileEntityEnchantmentTable_customName, name);
        } else if (te instanceof TileEntityFurnace) {
            return Reflector.setFieldValue(te, Reflector.TileEntityFurnace_customName, name);
        } else if (te instanceof TileEntityChest) {
            ((TileEntityChest)te).setCustomName(name);
            return true;
        } else if (te instanceof TileEntityDispenser) {
            ((TileEntityDispenser)te).setCustomName(name);
            return true;
        } else if (te instanceof TileEntityHopper) {
            ((TileEntityHopper)te).setCustomName(name);
            return true;
        } else {
            return false;
        }
    }
}
