package com.invasionmod;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static com.invasionmod.InvasionMod.MOD_ID;

public class DimensionManager {


    public static RuntimeWorldHandle getPlayerWorldHandle(Identifier id, MinecraftServer server) {
        Fantasy fantasy = Fantasy.get(server);
        ServerWorld overWorld = server.getOverworld();

        RuntimeWorldConfig worldConfig = getRuntimeWorldConfig(overWorld);

        return fantasy.getOrOpenPersistentWorld(
                id, worldConfig);
    }

    @NotNull
    private static RuntimeWorldConfig getRuntimeWorldConfig(ServerWorld overWorld) {
        return new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(overWorld.getDifficulty())
                .setGenerator(overWorld.getChunkManager().getChunkGenerator())
                .setSeed(overWorld.getSeed())
                .setShouldTickTime(true).setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 1);
    }

    public static RuntimeWorldHandle getPlayerWorldHandle(String playerUuid, MinecraftServer server) {
        Identifier worldId = new Identifier(MOD_ID, playerUuid + "-world");
        Fantasy fantasy = Fantasy.get(server);
        ServerWorld overWorld = server.getOverworld();

        RuntimeWorldConfig worldConfig = getRuntimeWorldConfig(overWorld);

        return fantasy.getOrOpenPersistentWorld(
                worldId, worldConfig);
    }

    public static RegistryKey<World> getPlayerWorldRegistry(String playerUuid) {
        Identifier worldId = new Identifier(MOD_ID, playerUuid + "-world");

        return RegistryKey.of(RegistryKeys.WORLD, worldId);
    }
}
