package com.invasionmod.mixin;

import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.invasionmod.InvasionMod.CHUNK_SWAPPER;

@Debug(export = true)
@Mixin(AdvancementRewards.class)
public abstract class AdvancementRewardsMixin {

    @Shadow
    public abstract int experience();

    @Inject(at = @At(value = "HEAD"), method = "apply")
    private void onPlayerTeleportInject(ServerPlayerEntity player, CallbackInfo ci) {
        if (experience() > 0) {
            ItemEntity itemEntity = player.dropItem(new ItemStack(CHUNK_SWAPPER, 1), false);
            if (itemEntity != null)
                itemEntity.setOwner(player.getUuid());
        }
    }
}
