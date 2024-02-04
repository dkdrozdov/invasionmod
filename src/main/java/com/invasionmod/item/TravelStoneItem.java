package com.invasionmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import static com.invasionmod.util.Util.getOpposite;
import static net.minecraft.util.ActionResult.PASS;

public class TravelStoneItem extends Item {

    public TravelStoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Hand hand = context.getHand();
        ItemStack travelStoneStack = context.getStack();
        PlayerEntity player = context.getPlayer();

        if (player == null) return ActionResult.PASS;

        ItemStack otherStack = player.getStackInHand(getOpposite(hand));
        if (otherStack.getItem() instanceof SoulGrabberItem soulGrabberItem) {
            if (soulGrabberItem.tryUseTeleport(context.getWorld(), otherStack, player, context.getBlockPos())) {
                player.getItemCooldownManager().set(travelStoneStack.getItem(), 40);
                travelStoneStack.decrement(1);

                return ActionResult.success(player.getWorld().isClient);
            }
            return ActionResult.FAIL;
        }
        return PASS;
    }
}

