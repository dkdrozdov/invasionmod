package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.entity.effect.PhantomStatusEffect;
import com.invasionmod.item.SoulGrabberItem;
import com.invasionmod.util.ItemStackData;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import static com.invasionmod.InvasionMod.*;

@Debug(export = true)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /*
    * This injection modifies living entity removing status effect behaviour so that
    * expiring phantoms are teleported back to their world and their loot is managed.
    * */
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

    // makes aggressive mobs ignore phantoms
    @Inject(at = @At(value = "RETURN", ordinal = 1), method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", cancellable = true)
    private void canTargetInject(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && !target.hasStatusEffect(PHANTOM));
    }

    // awards travel stones and updates sinner counter when player is killed
    @Inject(at = @At(value = "HEAD"), method = "onKilledBy")
    private void onKilledByInject(LivingEntity killer, CallbackInfo ci) {
        if (killer == null || killer.getWorld().isClient) return;

        LivingEntity livingEntity = ((LivingEntity) (Object) this);

        if (livingEntity instanceof ServerPlayerEntity killedPlayer
                && killer instanceof ServerPlayerEntity killerPlayer) {

            // Award Travel Stones

            Item weaponItem = killer.getMainHandStack().getItem();
            ItemStack stoneStack = new ItemStack(TRAVEL_STONE, weaponItem instanceof SoulGrabberItem ? 2 : 1);

            if (!killedPlayer.hasStatusEffect(PHANTOM))
                ((ServerPlayerEntityAccess) killedPlayer).invasionmod$setShouldGetStone(true);

            if (!killerPlayer.giveItemStack(stoneStack)) {
                if (killerPlayer.hasStatusEffect(PHANTOM)) ItemStackData.setIsOwned(stoneStack, true);

                ItemEntity itemEntity = killerPlayer.dropItem(stoneStack, true, false);
                killer.getWorld().spawnEntity(itemEntity);
            }

            // Update Sinner Counter

            int killedPlayerSinnerCounter = ((ServerPlayerEntityAccess) killedPlayer).invasionmod$getSinnerCounter();

            if (killerPlayer.hasStatusEffect(PHANTOM) && killedPlayerSinnerCounter <= 0) {
                int sinnerCounter = ((ServerPlayerEntityAccess) killerPlayer).invasionmod$getSinnerCounter();

                ((ServerPlayerEntityAccess) killerPlayer).invasionmod$setSinnerCounter(sinnerCounter + 1);
                LOGGER.info("Increased player " + killerPlayer.getName().getString()
                        + "'s Sinner Counter from " + sinnerCounter
                        + " to " + ((ServerPlayerEntityAccess) killerPlayer).invasionmod$getSinnerCounter() + ".");
            }

            if (killedPlayerSinnerCounter > 0 && !killedPlayer.hasStatusEffect(PHANTOM)) {
                ((ServerPlayerEntityAccess) killedPlayer).invasionmod$setSinnerCounter(killedPlayerSinnerCounter - 1);
                LOGGER.info("Decreased player " + killedPlayer.getName().getString()
                        + "'s Sinner Counter from " + killedPlayerSinnerCounter
                        + " to " + ((ServerPlayerEntityAccess) killedPlayer).invasionmod$getSinnerCounter() + ".");
            }

        }
    }
}
  