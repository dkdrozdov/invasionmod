package com.invasionmod.mixin;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;

@Debug(export = true)
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(at = @At(value = "TAIL"), method = "onPlayerTeleport")
    private void onPlayerTeleportInject(ServerPlayerEntity player, CallbackInfo ci) {
        ServerWorld serverWorld = (ServerWorld) ((Object) this);
        LOGGER.info("Player %s with UUID %s teleported to world %s."
                .formatted(player.getName().toString(),
                        player.getUuidAsString(),
                        serverWorld.getRegistryKey().getValue()));
    }

    @Inject(at = @At(value = "TAIL"), method = "onPlayerChangeDimension")
    private void onPlayerChangeDimensionInject(ServerPlayerEntity player, CallbackInfo ci) {
        ServerWorld serverWorld = (ServerWorld) ((Object) this);
        LOGGER.info("Player %s with UUID %s changed dimension to world %s."
                .formatted(player.getName().toString(),
                        player.getUuidAsString(),
                        serverWorld.getRegistryKey().getValue()));

        player.removeStatusEffect(PHANTOM);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
    }
}
