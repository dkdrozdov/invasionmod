package com.invasionmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public class DimensionManager {
    public static RuntimeWorldHandle getPlayerWorldHandle(String playerUuid, MinecraftServer server)
    {
        Identifier worldId = new Identifier("invasionmod", playerUuid + "-world");
        Fantasy fantasy = Fantasy.get(server);
        ServerWorld overWorld = server.getOverworld();

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(overWorld.getDimensionKey())
                .setDifficulty(overWorld.getDifficulty())
                .setGenerator(overWorld.getChunkManager().getChunkGenerator())
                .setSeed(overWorld.getSeed())
                .setShouldTickTime(true);

        return fantasy.getOrOpenPersistentWorld(
                worldId, worldConfig);
    }
}
