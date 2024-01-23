package com.invasionmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
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
        ServerWorld overWorld = server.getOverworld();
        Identifier worldId = new Identifier("invasionmod", player.getUuidAsString() + "-world");
        Fantasy fantasy = Fantasy.get(server);

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(overWorld.getDimensionKey())
                .setDifficulty(overWorld.getDifficulty())
                .setGenerator(overWorld.getChunkManager().getChunkGenerator())
                .setSeed(overWorld.getSeed());
        RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(
                worldId, worldConfig);

        RegistryKey<World> registryKey = playerNBT != null ? DimensionType.worldFromDimensionNbt(new Dynamic<NbtElement>(NbtOps.INSTANCE, playerNBT.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;
        if (registryKey == World.OVERWORLD) {
            player.setSpawnPoint(worldHandle.getRegistryKey(), player.getSpawnPointPosition(), player.getSpawnAngle(), player.isSpawnForced(), false);
            return worldHandle.asWorld();
        } else
            return server.getWorld(registryKey);
    }

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "respawnPlayer")
    private ServerWorld onRespawnPlayerInject(ServerWorld original, ServerPlayerEntity player, boolean alive) {
        ServerWorld overWorld = server.getOverworld();
        Identifier worldId = new Identifier("invasionmod", player.getUuidAsString() + "-world");
        Fantasy fantasy = Fantasy.get(server);

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(overWorld.getDimensionKey())
                .setDifficulty(overWorld.getDifficulty())
                .setGenerator(overWorld.getChunkManager().getChunkGenerator())
                .setSeed(overWorld.getSeed());
        RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(
                worldId, worldConfig);

        return worldHandle.asWorld();
    }
}
