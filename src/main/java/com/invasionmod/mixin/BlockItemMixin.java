package com.invasionmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static com.invasionmod.InvasionMod.PHANTOM;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    /*
     * This injection modifies check for player's ability to place blocks, so that
     * phantoms can only place falling blocks.
     */
    @ModifyExpressionValue(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemPlacementContext;canPlace()Z"))
    private boolean preventPlacingByPhantom(boolean original, ItemPlacementContext context) {
        PlayerEntity playerEntity = context.getPlayer();

        if (playerEntity != null) {
            Item usedItem = context.getStack().getItem();
            boolean blockIsFallingBlock = (usedItem instanceof BlockItem usedBlockItem &&
                    usedBlockItem.getBlock() instanceof FallingBlock);

            if (playerEntity.hasStatusEffect(PHANTOM) && !blockIsFallingBlock) {
                if (!playerEntity.getWorld().isClient)
                    playerEntity.sendMessage(Text.translatable("invasionmod.phantom.cant_place_non_falling"), true);

                return false;
            }
        }

        return original;
    }
}
