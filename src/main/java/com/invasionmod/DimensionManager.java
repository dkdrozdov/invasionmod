package com.invasionmod;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static com.invasionmod.InvasionMod.MOD_ID;

public class DimensionManager {
    public static RuntimeWorldHandle getPlayerWorldHandle(String playerUuid, MinecraftServer server) {
        Identifier worldId = new Identifier(MOD_ID, playerUuid + "-world");
        Fantasy fantasy = Fantasy.get(server);
        ServerWorld overWorld = server.getOverworld();

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(overWorld.getDifficulty())
                .setGenerator(overWorld.getChunkManager().getChunkGenerator())
                .setSeed(overWorld.getSeed())
                .setShouldTickTime(true);

        return fantasy.getOrOpenPersistentWorld(
                worldId, worldConfig);
    }

    public static RegistryKey<World> getPlayerWorldRegistry(String playerUuid) {
        Identifier worldId = new Identifier(MOD_ID, playerUuid + "-world");

        return RegistryKey.of(RegistryKeys.WORLD, worldId);
    }

    public static boolean isPlayerWorldLoaded(String playerUuid, MinecraftServer server) {
        Identifier worldId = new Identifier(MOD_ID, playerUuid + "-world");
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);

        return server.getWorld(worldKey) != null;
    }
}
