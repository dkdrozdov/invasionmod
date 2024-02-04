package com.invasionmod.item;

import com.invasionmod.DimensionManager;
import com.invasionmod.InvasionMod;
import com.invasionmod.util.Nbt;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;
import static com.invasionmod.util.Nbt.getPlayerUuid;
import static com.invasionmod.util.Nbt.hasNbtPlayerUuid;
import static net.minecraft.block.Blocks.*;

public class SoulGrabberItem extends Item {

    private final static int phantomDurationMinutes = 3;

    public SoulGrabberItem(Settings settings) {
        super(settings);
    }

    private void playUseSound(World world, PlayerEntity playerEntity) {
        world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(),
                SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS);
    }

    private void addUseParticles(World world, PlayerEntity playerEntity) {
        for (int i = 0; i < 32; ++i) {
            world.addParticle(ParticleTypes.PORTAL, playerEntity.getX(),
                    playerEntity.getY() + playerEntity.getRandom().nextDouble() * 2.0, playerEntity.getZ(),
                    playerEntity.getRandom().nextGaussian(), 0.0, playerEntity.getRandom().nextGaussian());
        }
    }

    @Nullable
    private String getTargetUUID(ItemStack itemStack, PlayerEntity player) {

        if (!hasNbtPlayerUuid(itemStack)) {
            LOGGER.info("Player %s with UUID %s tried to use DimensionGrabberItem, but the target uuid is empty."
                    .formatted(player.getName().getString(), player.getUuidAsString()));
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.should_choose_player"), true);
            return null;
        }

        return getPlayerUuid(itemStack);
    }

    private boolean canUse(PlayerEntity player, ServerWorld world, String targetUuid) {

        if (player == null) return false;
        if (world.isClient) return false;

        if (Objects.equals(targetUuid, player.getUuidAsString())) {
            LOGGER.info("Player " + player.getName().getString() + " with UUID " + player.getUuidAsString() +
                    " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is themselves.");
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.you_are_target"), true);
            return false;
        }

        if (world.getRegistryKey() != DimensionManager.getPlayerWorldRegistry(player.getUuidAsString())) {
            LOGGER.info("Player " + player.getName().getString() + " with UUID " + player.getUuidAsString() +
                    " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but the dimension of departure is forbidden: " + world.getRegistryKey().toString());
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.only_from_own_world"), true);
            return false;
        }

        ServerPlayerEntity targetPlayer = world.getServer().getPlayerManager().getPlayer(UUID.fromString(targetUuid));

        if (targetPlayer == null) {
            LOGGER.info("Player " + player.getName().getString() + " with UUID " + player.getUuidAsString() +
                    " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is offline.");
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.target_offline"), true);
            return false;
        }

        return true;
    }

    public boolean tryUseTeleport(World playerWorld, ItemStack soulGrabberStack, PlayerEntity player, BlockPos blockPos) {

        if (playerWorld.isClient)
            return false;

        ServerWorld serverWorld = (ServerWorld) playerWorld;

        String targetUUID = getTargetUUID(soulGrabberStack, player);
        if (targetUUID == null) return false;

        if (canUse(player, serverWorld, targetUUID))
            return invade(player, serverWorld, blockPos, targetUUID);
        return false;
    }


    public boolean invade(PlayerEntity invaderPlayer, ServerWorld serverWorld, BlockPos fromPos, String targetUUID) {

        MinecraftServer server = serverWorld.getServer();

        BlockPattern.Result validPortalMatch = InvasionMod.portalPattern.searchAround(serverWorld, fromPos);

        if (validPortalMatch == null || validPortalMatch.getUp() != Direction.UP) {
            invaderPlayer.sendMessage(Text.translatable("invasionmod.soul_grabber.you_need_portal"), true);

            return false;
        }

        ServerPlayerEntity targetPlayer = serverWorld.getServer().getPlayerManager().getPlayer(UUID.fromString(targetUUID));
        if (targetPlayer == null) return false;

        if (!targetPlayer.isAlive()) {
            LOGGER.info("Player " + invaderPlayer.getName().getString() + " with UUID " + invaderPlayer.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                    " via DimensionGrabberItem, but target player is dead.");
            invaderPlayer.sendMessage(Text.translatable("invasionmod.soul_grabber.target_dead"), true);
            return false;
        }

        RuntimeWorldHandle destinationWorldHandle = DimensionManager.getPlayerWorldHandle(targetUUID, server);
        ServerWorld destinationWorld = destinationWorldHandle.asWorld();
        playUseSound(serverWorld, invaderPlayer);
        playUseSound(destinationWorld, invaderPlayer);
        addUseParticles(serverWorld, invaderPlayer);
        setInvadingWeather(destinationWorld);

        Vec3d portalCenter = getOrCreatePortal(destinationWorld, validPortalMatch);

        ((ServerPlayerEntity) invaderPlayer).teleport(destinationWorld,
                portalCenter.getX(),
                portalCenter.getY(),
                portalCenter.getZ(),
                invaderPlayer.getYaw(),
                invaderPlayer.getPitch());

        addPhantomStatusEffects(invaderPlayer);
        invaderPlayer.getItemCooldownManager().set(this, 20);

        return true;
    }

    private void setInvadingWeather(ServerWorld destinationWorld) {
        int random = Random.create().nextBetween(0, 100);
        if (random <= 2)
            destinationWorld.setWeather(0, 60 * phantomDurationMinutes, false, true);
        else
            destinationWorld.setWeather(0, 60 * phantomDurationMinutes, true, false);

    }

    private Vec3d getOrCreatePortal(ServerWorld world, BlockPattern.Result portalMatch) {
        BlockPattern.Result targetWorldPortalMatch = InvasionMod.portalPattern.searchAround(world, portalMatch.getFrontTopLeft());

        Direction forwards = (targetWorldPortalMatch == null ? portalMatch : targetWorldPortalMatch).getForwards();
        Direction left = forwards.rotateYCounterclockwise();
        BlockPos portalCenter = (targetWorldPortalMatch == null ? portalMatch : targetWorldPortalMatch)
                .getFrontTopLeft()
                .offset(forwards, 1)
                .offset(left.getOpposite(), 1)
                .offset(Direction.DOWN, 4);

        if (targetWorldPortalMatch == null || targetWorldPortalMatch.getUp() != Direction.UP) {
            List<BlockPos> columnOrigins = List.of(
                    portalCenter.west().north(),
                    portalCenter.east().north(),
                    portalCenter.west().south(),
                    portalCenter.east().south());
            List<BlockState> columnBlockStates = List.of(
                    QUARTZ_BRICKS.getDefaultState(),
                    QUARTZ_BRICKS.getDefaultState(),
                    QUARTZ_BRICKS.getDefaultState(),
                    HAY_BLOCK.getDefaultState(),
                    SOUL_CAMPFIRE.getDefaultState());

            for (BlockPos columnOrigin : columnOrigins) {
                BlockPos blockPos = columnOrigin;

                for (BlockState blockState : columnBlockStates) {
                    if (canPlacePortalBlock.test(world.getBlockState(blockPos)))
                        world.setBlockState(blockPos, blockState);

                    blockPos = blockPos.up();
                }
            }
        }

        return portalCenter.toCenterPos();
    }

    private final Predicate<BlockState> canPlacePortalBlock = blockState -> blockState.isAir() || blockState.isOf(Blocks.WATER);

    private static void addPhantomStatusEffects(PlayerEntity playerEntity) {
        StatusEffectInstance statusEffectInstance = new StatusEffectInstance(PHANTOM,
                20 * 60 * phantomDurationMinutes,
                0,
                true,
                true,
                true);

        playerEntity.addStatusEffect(statusEffectInstance);

        playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                20 * 60 * phantomDurationMinutes,
                1,
                false,
                false,
                false));
    }

    public boolean tryUseChunkSwap(World world, ItemStack itemStack, PlayerEntity player, BlockPos blockPos) {
        if (world.isClient)
            return false;

        ServerWorld serverWorld = (ServerWorld) world;

        String targetUUID = getTargetUUID(itemStack, player);
        if (targetUUID == null) return false;

        if (canUse(player, serverWorld, targetUUID))
            return useChunkSwap(player, serverWorld, blockPos, targetUUID);
        return false;
    }

    private boolean useChunkSwap(PlayerEntity player, ServerWorld serverWorld, BlockPos blockPos, String targetUUID) {
        MinecraftServer server = serverWorld.getServer();
        RuntimeWorldHandle targetWorldHandle = DimensionManager.getPlayerWorldHandle(targetUUID, server);
        ServerWorld targetWorld = targetWorldHandle.asWorld();

        Chunk playerChunk = serverWorld.getChunk(blockPos);
        Chunk otherChunk = targetWorld.getChunk(blockPos);

        LOGGER.info("trying to replace chunks:");
        LOGGER.info("world " + serverWorld.getRegistryKey().getValue() + ", pos " + playerChunk.getPos());
        LOGGER.info("world " + targetWorld.getRegistryKey().getValue() + ", pos " + otherChunk.getPos());

        int sectionIndex = playerChunk.getSectionIndex(blockPos.getY());

        LOGGER.info("sectionIndex " + sectionIndex);
        LOGGER.info("sectionIndex " + sectionIndex);

        ChunkSection playerSection = playerChunk.getSection(sectionIndex);
        ChunkSection otherSection = otherChunk.getSection(sectionIndex);

        LOGGER.info("sections:");
        LOGGER.info("section 0 0 0:" + playerSection.getBlockState(0, 0, 0).getBlock().toString());
        LOGGER.info("section 0 0 0:" + otherSection.getBlockState(0, 0, 0).getBlock().toString());

        ShortSet updatedPositions = new ShortOpenHashSet();

        ChunkPos chunkPos = playerChunk.getPos();
        Vec3i chunkSectionOffset = new Vec3i(chunkPos.x * 16, sectionIndex * 8, chunkPos.z * 16);

        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 16; y++) {
                    BlockPos currentPosSection = new BlockPos(x, y, z);
                    BlockPos currentPosWorld = new BlockPos(x, y, z).add(chunkSectionOffset.getX(), chunkSectionOffset.getY(), chunkSectionOffset.getZ());

                    BlockState playerBlockState = playerSection.getBlockState(x, y, z);
                    BlockState otherBlockState = otherSection.getBlockState(x, y, z);

                    BlockEntity playerBlockEntity = playerChunk.getBlockEntity(currentPosWorld);
                    BlockEntity otherBlockEntity = otherChunk.getBlockEntity(currentPosWorld);
                    NbtCompound playerBlockEntityNbt = null;
                    NbtCompound otherBlockEntityNbt = null;
                    if (playerBlockEntity != null) {
                        playerBlockEntityNbt = playerBlockEntity.createNbtWithId();
                        Clearable.clear(playerBlockEntity);
                    }
                    if (otherBlockEntity != null) {
                        otherBlockEntityNbt = otherBlockEntity.createNbtWithId();
                        Clearable.clear(otherBlockEntity);
                    }

                    if (playerBlockState != otherBlockState) {
                        LOGGER.info("Swapping pos %s %s %s: %s with %s".formatted(x, y, z, playerBlockState.toString(), otherBlockState.toString()));
                        otherSection.setBlockState(x, y, z, playerBlockState);
                        playerSection.setBlockState(x, y, z, otherBlockState);
                        updatedPositions.add(ChunkSectionPos.packLocal(currentPosSection));
                    }

                    BlockEntity newPlayerBlockEntity = serverWorld.getBlockEntity(currentPosWorld);
                    if (otherBlockEntityNbt != null && newPlayerBlockEntity != null) {
                        newPlayerBlockEntity.readNbt(otherBlockEntityNbt);
                        newPlayerBlockEntity.markDirty();
                    }

                    BlockEntity newOtherBlockEntity = targetWorld.getBlockEntity(currentPosWorld);
                    if (playerBlockEntityNbt != null && newOtherBlockEntity != null) {
                        newOtherBlockEntity.readNbt(playerBlockEntityNbt);
                        newOtherBlockEntity.markDirty();
                    }

                    serverWorld.updateNeighbors(currentPosWorld, serverWorld.getBlockState(currentPosWorld).getBlock());
                    targetWorld.updateNeighbors(currentPosWorld, targetWorld.getBlockState(currentPosWorld).getBlock());
                }

        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(blockPos);

        Collection<ServerPlayerEntity> playerWorldTrackingPlayers = PlayerLookup.tracking(serverWorld, chunkPos);
        ChunkDeltaUpdateS2CPacket playerWorldPacket = new ChunkDeltaUpdateS2CPacket(chunkSectionPos, updatedPositions, playerSection);

        playerWorldTrackingPlayers.forEach(trackingPlayer -> {
            LOGGER.info("sending update packet to " + trackingPlayer.getName().getString());
            trackingPlayer.networkHandler.sendPacket(playerWorldPacket);
        });

        Collection<ServerPlayerEntity> otherWorldTrackingPlayers = PlayerLookup.tracking(targetWorld, chunkPos);
        ChunkDeltaUpdateS2CPacket otherWorldPacket = new ChunkDeltaUpdateS2CPacket(chunkSectionPos, updatedPositions, otherSection);

        otherWorldTrackingPlayers.forEach(trackingPlayer -> {
            LOGGER.info("sending update packet to " + trackingPlayer.getName().getString());
            trackingPlayer.networkHandler.sendPacket(otherWorldPacket);
        });

        return true;
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        String playerName = Nbt.getPlayerName(itemStack);
        if (!Objects.equals(playerName, "")) {
            tooltip.add(Text.of(playerName));
        }
    }
}

