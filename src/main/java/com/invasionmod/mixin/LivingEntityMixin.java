package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.invasionmod.entity.effect.PhantomStatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static com.invasionmod.DimensionManager.getPlayerWorldHandle;
import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;

@Debug(export = true)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(at = @At(value = "TAIL"), method = "onStatusEffectRemoved")
    private void onStatusEffectRemovedInject(StatusEffectInstance effect, CallbackInfo ci) {
        LivingEntity livingEntity = ((LivingEntity) ((Object) this));

        if (livingEntity.getWorld().isClient || effect.getEffectType() != PHANTOM) return;
        MinecraftServer server = livingEntity.getServer();

        if (livingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
            World currentWorld = serverPlayerEntity.getWorld();
            String playerUUID = serverPlayerEntity.getUuidAsString();

            if (currentWorld.getDimensionKey() != DimensionTypes.OVERWORLD) {
                LOGGER.info("PhantomStatusEffect is removed from player %s with UUID %s, and since they appeared in neutral dimension their dimension is kept unchanged."
                        .formatted(serverPlayerEntity.getName().getString(), playerUUID));
                return;
            }
            RuntimeWorldHandle playerWorldHandle = getPlayerWorldHandle(playerUUID, server);
            ServerWorld playerWorld = playerWorldHandle.asWorld();

            double playerX = serverPlayerEntity.getX();
            double playerY = serverPlayerEntity.getY();
            double playerZ = serverPlayerEntity.getZ();

            // manage loot
            PhantomStatusEffect.manageLoot(serverPlayerEntity);

            serverPlayerEntity.teleport(playerWorld,
                    playerX,
                    playerY,
                    playerZ,
                    serverPlayerEntity.getYaw(),
                    serverPlayerEntity.getPitch());

            LOGGER.info("PhantomStatusEffect is removed from player %s with UUID %s, and they are teleported to their world %s at X:%s, Y:%s, Z:%s."
                    .formatted(serverPlayerEntity.getName().getString(),
                            playerUUID,
                            DimensionManager.getPlayerWorldRegistry(playerUUID).toString(),
                            playerX,
                            playerY,
                            playerZ));
        }
    }

    @Inject(at = @At(value = "RETURN", ordinal = 1), method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", cancellable = true)
    private void canTargetInject(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && !target.hasStatusEffect(PHANTOM));
    }
}
  