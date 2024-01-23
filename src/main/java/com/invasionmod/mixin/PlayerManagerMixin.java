package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

@Debug(export = true)
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private MinecraftServer server;

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"), method = "onPlayerConnect")
    private ServerWorld onPlayerConnectInject(ServerWorld original, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        LOGGER.info(player.getName() + " just connected!");

        NbtCompound playerNBT = ((PlayerManager) ((Object) this)).loadPlayerData(player);
        @SuppressWarnings("deprecation") RegistryKey<World> registryKey = playerNBT != null ? DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, playerNBT.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;

        RuntimeWorldHandle worldHandle = DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server);

        if (registryKey == World.OVERWORLD) {
            player.setSpawnPoint(worldHandle.getRegistryKey(), player.getSpawnPointPosition(), player.getSpawnAngle(), player.isSpawnForced(), false);
            return worldHandle.asWorld();
        } else
            return server.getWorld(registryKey);
    }

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "respawnPlayer")
    private ServerWorld onRespawnPlayerInject(ServerWorld original, ServerPlayerEntity player, boolean alive) {
        return DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server).asWorld();
    }
}
