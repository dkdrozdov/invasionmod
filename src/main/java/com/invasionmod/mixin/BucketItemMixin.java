package com.invasionmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.invasionmod.InvasionMod.PHANTOM;

@Debug(export = true)
@Mixin(BucketItem.class)

public abstract class BucketItemMixin {
    /*
    * This injection modifies check for player's ability to use buckets, so that
     * phantoms can not use them.
    * */
    @Inject(method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/BlockHitResult;getBlockPos()Lnet/minecraft/util/math/BlockPos;"), cancellable = true)
    private void preventPlacingByPhantom(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (user.hasStatusEffect(PHANTOM)) {
            if(!user.getWorld().isClient)
                user.sendMessage(Text.translatable("invasionmod.phantom.cant_use_bucket"), true);

            cir.setReturnValue(TypedActionResult.fail(itemStack));
            cir.cancel();
        }
    }
}
