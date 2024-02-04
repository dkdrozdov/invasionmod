package com.invasionmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import static com.invasionmod.util.Util.getOpposite;
import static net.minecraft.util.ActionResult.PASS;

public class ChunkSwapperItem extends Item {

    public ChunkSwapperItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Hand hand = context.getHand();
        ItemStack chunkSwapperStack = context.getStack();
        PlayerEntity player = context.getPlayer();

        if (player == null) return ActionResult.PASS;

        ItemStack otherStack = player.getStackInHand(getOpposite(hand));
        if (otherStack.getItem() instanceof SoulGrabberItem soulGrabberItem) {
            if (soulGrabberItem.tryUseChunkSwap(context.getWorld(), otherStack, player, context.getBlockPos())) {
                player.getItemCooldownManager().set(chunkSwapperStack.getItem(), 40);
                chunkSwapperStack.decrement(1);

                return ActionResult.success(player.getWorld().isClient);
            }
            return ActionResult.FAIL;
        }
        return PASS;
    }
}

