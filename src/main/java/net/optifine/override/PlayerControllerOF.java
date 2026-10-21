package net.optifine.override;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PlayerControllerOF extends PlayerControllerMP {
    private boolean acting = false;
    private BlockPos lastClickBlockPos = null;
    private Entity lastClickEntity = null;

    public PlayerControllerOF(Minecraft mcIn, NetHandlerPlayClient netHandler) {
        super(mcIn, netHandler);
    }

    @Override
    public boolean clickBlock(BlockPos loc, EnumFacing face) {
        this.acting = true;
        this.lastClickBlockPos = loc;
        boolean res = super.clickBlock(loc, face);
        this.acting = false;
        return res;
    }

    @Override
    public boolean onPlayerDamageBlock(BlockPos posBlock, EnumFacing directionFacing) {
        this.acting = true;
        this.lastClickBlockPos = posBlock;
        boolean res = super.onPlayerDamageBlock(posBlock, directionFacing);
        this.acting = false;
        return res;
    }

    @Override
    public boolean sendUseItem(EntityPlayer playerIn, World worldIn, ItemStack itemStackIn) {
        this.acting = true;
        boolean res = super.sendUseItem(playerIn, worldIn, itemStackIn);
        this.acting = false;
        return res;
    }

    @Override
    public boolean onPlayerRightClick(EntityPlayerSP player, WorldClient worldIn, ItemStack heldStack, BlockPos hitPos, EnumFacing side, Vec3 hitVec) {
        this.acting = true;
        this.lastClickBlockPos = hitPos;
        boolean res = super.onPlayerRightClick(player, worldIn, heldStack, hitPos, side, hitVec);
        this.acting = false;
        return res;
    }

    @Override
    public boolean interactWithEntitySendPacket(EntityPlayer playerIn, Entity targetEntity) {
        this.lastClickEntity = targetEntity;
        return super.interactWithEntitySendPacket(playerIn, targetEntity);
    }

    @Override
    public boolean isPlayerRightClickingOnEntity(EntityPlayer player, Entity entityIn, MovingObjectPosition movingObject) {
        this.lastClickEntity = entityIn;
        return super.isPlayerRightClickingOnEntity(player, entityIn, movingObject);
    }

    public boolean isActing() {
        return this.acting;
    }

    public BlockPos getLastClickBlockPos() {
        return this.lastClickBlockPos;
    }

    public Entity getLastClickEntity() {
        return this.lastClickEntity;
    }
}
