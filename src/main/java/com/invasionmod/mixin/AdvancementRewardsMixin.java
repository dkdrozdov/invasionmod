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

    /*
     * This variable foreshadows the experience method parameter, which defines
     * amount of experience player gets.
     *
     * If experience is not 0, a Chunk Swapper is given.
     */
    @Shadow
    public abstract int experience();

    /*
     * By design, players should get Chunk Swapper when they achieve a difficult advancement.
     * It was decided to define difficult advancements as advancements that give experience.
     *
     * This injection creates and gives a Chunk Swapper to a player who is being rewarded
     * with experience for an advancement.
     *
     */
    @Inject(at = @At(value = "HEAD"), method = "apply")
    private void onPlayerGetAdvancementReward(ServerPlayerEntity player, CallbackInfo ci) {
        if (experience() > 0) {
            ItemEntity itemEntity = player.dropItem(new ItemStack(CHUNK_SWAPPER, 1), false);
            if (itemEntity != null)
                itemEntity.setOwner(player.getUuid());
        }
    }
}
