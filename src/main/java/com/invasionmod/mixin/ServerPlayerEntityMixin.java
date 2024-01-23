package com.invasionmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

@Debug(export = true)
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    public MinecraftServer server;

@Shadow private RegistryKey<World> spawnPointDimension;

    @ModifyExpressionValue(at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;OVERWORLD:Lnet/minecraft/registry/RegistryKey;"), method = "readCustomDataFromNbt")
    private  RegistryKey<World> readCustomDataFromNbtInject( RegistryKey<World> original) {
        ServerPlayerEntity player = (ServerPlayerEntity)((Object) this);
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

        return worldHandle.getRegistryKey();
    }
}
