package com.invasionmod;

import com.invasionmod.entity.GhostEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.*;

import static com.invasionmod.InvasionMod.GHOST;
import static com.invasionmod.InvasionMod.LOGGER;

public class GhostManager {

    /**
     * @return Set of worlds where the {@code player}'s ghost can be tracked.
     */
    public static Set<ServerWorld> getTrackingWorlds(MinecraftServer server, ServerPlayerEntity player) {
        // not to be confused with "player's own world".
        ServerWorld playerWorld = player.getServerWorld();

        if (playerWorld.getDimensionKey() != DimensionTypes.OVERWORLD) return Collections.emptySet();

        BlockPos playerPos = player.getBlockPos();

        // get all overworlds that are not playerWorld where the playerPos is tracked
        Collection<ServerPlayerEntity> players = PlayerLookup.all(server);
        Set<ServerWorld> trackingWorlds = new HashSet<>();

        for (ServerPlayerEntity serverPlayer : players) {
            if (serverPlayer == player) continue;

            ServerWorld serverWorld = serverPlayer.getServerWorld();
            Collection<ServerPlayerEntity> tracking = PlayerLookup.tracking(serverWorld, playerPos);
            if (serverWorld != playerWorld && !tracking.isEmpty())
                trackingWorlds.add(serverWorld);
        }

        return trackingWorlds;
    }

    /**
     * @return Set of players whose ghost can be tracked by {@code player}.
     */
    public static Set<ServerPlayerEntity> getTrackedPlayers(MinecraftServer server, ServerPlayerEntity player) {
        // not to be confused with "player's own world".
        ServerWorld playerWorld = player.getServerWorld();

        if (playerWorld.getDimensionKey() != DimensionTypes.OVERWORLD) return Collections.emptySet();

        // get all players which are tracked by player and are not the same player
        Collection<ServerPlayerEntity> players = PlayerLookup.all(server);
        Set<ServerPlayerEntity> trackedPlayers = new HashSet<>();

        for (ServerPlayerEntity serverPlayer : players) {
            if (serverPlayer == player) continue;

            BlockPos serverPlayerPos = serverPlayer.getBlockPos();

            if (PlayerLookup.tracking(playerWorld, serverPlayerPos).contains(player)) {
                trackedPlayers.add(serverPlayer);
            }

        }

        return trackedPlayers;
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        updateGhosts(player);
    }


    /**
     * Spawns ghosts of a player in other dimensions and ghosts of other players in player's current dimension if needed.
     * Also, despawns expired or unavailable ghosts.
     */
    public static void updateGhosts(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();

//        LOGGER.info("Player %s changed chunk: world %s pos %s".formatted(player.getName().getString(), player.getWorld(), player.getPos()));
        Set<ServerWorld> trackingWorlds = getTrackingWorlds(server, player);

        if (trackingWorlds.isEmpty()) return;

        for (ServerWorld world : trackingWorlds) {
            trySpawnGhost(player, world);
        }

        // spawn ghosts player might discover

        ServerWorld world = player.getServerWorld();
        Set<ServerPlayerEntity> trackedPlayers = getTrackedPlayers(server, player);

        for (ServerPlayerEntity trackedPlayer : trackedPlayers) {
            trySpawnGhost(trackedPlayer, world);
        }
    }

    private static void trySpawnGhost(ServerPlayerEntity player, ServerWorld world) {
        List<? extends GhostEntity> ghostEntities = world.getEntitiesByType(GHOST, (GhostEntity ghostEntity) -> ghostEntity.getPlayer() == player);

        if (ghostEntities.isEmpty()) {
            BlockPos playerPos = player.getBlockPos();
            LOGGER.info("Spawned ghost: player %s pos %s world %s".formatted(player.getName().getString(), playerPos, world));
            GhostEntity ghostEntity = GHOST.spawn(world, playerPos, SpawnReason.EVENT);
            if (ghostEntity == null)
                LOGGER.info("couldn't spawn ghost!");
            else
                ghostEntity.setPlayer(player);
        } else {
            for (GhostEntity entity : ghostEntities) {
                if (PlayerLookup.tracking(entity).isEmpty())
                    entity.discard();   // remove ghosts nobody tracks
//                else
//                    LOGGER.info("tried to spawn a ghost, but it already exists: client " + world.isClient + ", player " + entity.getPlayer().getName().getString() + ", ghost id: " +
//                            entity.getId() + ", location: " + entity.getBlockPos().toString() + ", inRange 32: " + entity.isInRange(player, 32));
            }
            long count = 0L;
            for (GhostEntity ghostEntity : ghostEntities) {
                count++;
                if (count > 1)
                    ghostEntity.discard();
            }
        }
    }

    public static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        Collection<ServerPlayerEntity> trackingPlayers = PlayerLookup.tracking(world, chunk.getPos());

        for (ServerPlayerEntity trackingPlayer : trackingPlayers) {
            updateGhosts(trackingPlayer);
        }
    }

    public static void onChunkUnload() {
    }
}
