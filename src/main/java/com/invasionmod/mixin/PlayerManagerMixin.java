package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;

@Debug(export = true)
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    /*
    * This injection adds logic to connecting players so that their world is loaded and or created and
    * they are spawned in their world.
    * */
    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"), method = "onPlayerConnect")
    private ServerWorld onPlayerConnectInject(ServerWorld original, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        LOGGER.info("Connected player%s with UUID %s."
                .formatted(player.getName().getString(), player.getUuidAsString()));

        NbtCompound playerNBT = ((PlayerManager) ((Object) this)).loadPlayerData(player);
        @SuppressWarnings("deprecation") RegistryKey<World> registryKey = playerNBT != null ? DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, playerNBT.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;

        RuntimeWorldHandle worldHandle = DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server);

        if (registryKey == World.OVERWORLD) {
            LOGGER.info("Detected %s first join on this server. Generating new world %s."
                    .formatted(player.getName().getString(), worldHandle.getRegistryKey().getValue()));

            player.setSpawnPoint(worldHandle.getRegistryKey(), player.getSpawnPointPosition(), player.getSpawnAngle(), player.isSpawnForced(), false);
            return worldHandle.asWorld();
        } else
            return server.getWorld(registryKey);
    }

    /*
    * This injection modifies the code of players joining while being in unloaded world, so that
    * the world they're in is loaded (if they are a phantom) and they are teleported back to their world.
    * */
    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "onPlayerConnect")
    private ServerWorld onPlayerConnectUnknownDimensionInject(ServerWorld original, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, @Local RegistryKey<World> registryKey) {
        LOGGER.info("Detected " + player.getName().getString() + " tries to spawn on unloaded world.");

        RuntimeWorldHandle worldHandle = DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server);
        LOGGER.info("Changing spawn to player's own world: " + worldHandle.getRegistryKey().toString());

        if (player.hasStatusEffect(PHANTOM)) {
            Identifier unloadedWorldId = registryKey.getValue();

            LOGGER.info("Player " + player.getName().getString() + " is a phantom! Tagging them with needReturnLoot.");

            ((ServerPlayerEntityAccess) player).invasionmod$setNeedReturnLoot(true);
            ((ServerPlayerEntityAccess) player).invasionmod$setReturnLootWorld(unloadedWorldId);
        }

        return worldHandle.asWorld();
    }

    /*
    * This injection replaces general overworld with player's overworld in player respawn code.
    * */
    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "respawnPlayer")
    private ServerWorld onRespawnPlayerInject(ServerWorld original, ServerPlayerEntity player, boolean alive) {
        return DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server).asWorld();
    }
}
