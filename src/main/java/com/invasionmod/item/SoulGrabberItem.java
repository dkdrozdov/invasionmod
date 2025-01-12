package com.invasionmod.item;

import com.invasionmod.DimensionManager;
import com.invasionmod.InvasionMod;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.util.ItemStackData;
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
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Clearable;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.*;
import java.util.function.Predicate;

import static com.invasionmod.InvasionMod.*;
import static com.invasionmod.util.ItemStackData.getPlayerUuid;
import static com.invasionmod.util.ItemStackData.hasDataPlayerUuid;
import static com.invasionmod.util.Util.getOpposite;
import static net.minecraft.block.Blocks.*;
import static net.minecraft.util.ActionResult.PASS;

public class SoulGrabberItem extends Item {

    private enum UseFailReason {
        TARGET_UUID_EMPTY,
        SELF_TARGET,
        DIMENSION_FORBIDDEN,
        TARGET_OFFLINE,
        SUPPRESSOR_DENIED,
        TARGET_DEAD;

        public void log(String playerName, String playerUUID, @Nullable String targetUUID, @Nullable World world) {
            switch (this) {
                case TARGET_UUID_EMPTY ->
                        LOGGER.info("Player %s with UUID %s tried to use DimensionGrabberItem, but the target uuid is empty."
                                .formatted(playerName, playerUUID));
                case SELF_TARGET -> {
                    assert targetUUID != null;

                    LOGGER.info("Player " + playerName + " with UUID " + playerUUID +
                            " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                            " via DimensionGrabberItem, but target player is themselves.");
                }
                case DIMENSION_FORBIDDEN -> {
                    assert targetUUID != null;
                    assert world != null;

                    LOGGER.info("Player " + playerName + " with UUID " + playerUUID +
                            " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                            " via DimensionGrabberItem, but the dimension of departure is forbidden: " + world.getRegistryKey().toString());
                }
                case TARGET_OFFLINE -> {
                    assert targetUUID != null;

                    LOGGER.info("Player " + playerName + " with UUID " + playerUUID +
                            " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                            " via DimensionGrabberItem, but target player is offline.");
                }
                case SUPPRESSOR_DENIED -> {
                    assert targetUUID != null;

                    LOGGER.info("Player " + playerName + " with UUID " + playerUUID +
                            " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                            " via DimensionGrabberItem, but the action was denied by nearby suppressor.");
                }
                case TARGET_DEAD -> {
                    assert targetUUID != null;

                    LOGGER.info("Player " + playerName + " with UUID " + playerUUID +
                            " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUUID).toString() +
                            " via DimensionGrabberItem, but target player is dead.");
                }
            }
        }

        public void sendMessage(PlayerEntity player) {
            switch (this) {
                case TARGET_UUID_EMPTY ->
                        player.sendMessage(Text.translatable("invasionmod.soul_grabber.should_choose_player"), true);
                case SELF_TARGET ->
                        player.sendMessage(Text.translatable("invasionmod.soul_grabber.you_are_target"), true);
                case DIMENSION_FORBIDDEN ->
                        player.sendMessage(Text.translatable("invasionmod.soul_grabber.only_from_own_world"), true);
                case TARGET_OFFLINE ->
                        player.sendMessage(Text.translatable("invasionmod.soul_grabber.target_offline"), true);
                case SUPPRESSOR_DENIED ->
                        player.sendMessage(Text.translatable("invasionmod.soul_grabber.suppressor_denied"), true);
                case TARGET_DEAD -> player.sendMessage(Text.translatable("invasionmod.soul_grabber.target_dead"), true);
            }
        }
    }

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

    private String getTargetUUID(ItemStack itemStack, PlayerEntity player) {

        if (!hasDataPlayerUuid(itemStack)) {
            LOGGER.info("Player %s with UUID %s tried to use DimensionGrabberItem, but the target uuid is empty."
                    .formatted(player.getName().getString(), player.getUuidAsString()));
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.should_choose_player"), true);
            return null;
        }

        return getPlayerUuid(itemStack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Hand hand = context.getHand();
        ItemStack soulGrabberStack = context.getStack();
        PlayerEntity player = context.getPlayer();

        if (player == null) return ActionResult.PASS;

        ItemStack otherStack = player.getStackInHand(getOpposite(hand));
        if (!((otherStack.getItem() instanceof TravelStoneItem) ||
                (otherStack.getItem() instanceof ChunkSwapperItem))) {
            if (tryUseTeleportRandom(context.getWorld(), soulGrabberStack, player, context.getBlockPos())) {
                player.getItemCooldownManager().set(soulGrabberStack.getItem(), 40);

                return ActionResult.success(player.getWorld().isClient);
            }
            return ActionResult.PASS;
        }
        return PASS;
    }

    private boolean canUse(PlayerEntity player, ServerWorld world, String targetUuid, BlockPos blockPos) {

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

        World targetWorld = DimensionManager.getPlayerWorldHandle(targetUuid, world.getServer()).asWorld();
        Optional<BlockPos> closestSuppressor = BlockPos.findClosest(blockPos, 21, 21, blockPos1 -> targetWorld.getBlockState(blockPos1).isOf(SUPPRESSOR));

        if (closestSuppressor.isPresent()) {
            LOGGER.info("Player " + player.getName().getString() + " with UUID " + player.getUuidAsString() +
                    " tried to interact with world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but the action was denied by nearby suppressor.");
            player.sendMessage(Text.translatable("invasionmod.soul_grabber.suppressor_denied"), true);
            return false;
        }

        return true;
    }

    private boolean tryUseTeleportRandom(World playerWorld, ItemStack soulGrabberStack, PlayerEntity player, BlockPos blockPos) {
        if (playerWorld.isClient)
            return false;

        ServerWorld serverWorld = (ServerWorld) playerWorld;

        if (hasDataPlayerUuid(soulGrabberStack)) return false;

        MinecraftServer server = serverWorld.getServer();

        List<ServerPlayerEntity> sinners = PlayerLookup
                .all(server)
                .stream()
                .filter(serverPlayerEntity -> ((ServerPlayerEntityAccess) serverPlayerEntity).invasionmod$getSinnerCounter() > 0)
                .toList();

        if (sinners.size() == 0) return false;
        Optional<ServerPlayerEntity> sinner = sinners.stream().skip(serverWorld.getRandom().nextInt(sinners.size())).findFirst();

        if (sinner.isEmpty()) {
            return false;
        }
        String sinnerUUID = sinner.get().getUuidAsString();

        if (canUse(player, serverWorld, sinnerUUID, blockPos)) {
            return invade(player, serverWorld, blockPos, sinnerUUID);
        }

        return false;
    }

    public boolean tryUseTeleport(World playerWorld, ItemStack soulGrabberStack, PlayerEntity player, BlockPos blockPos) {

        if (playerWorld.isClient)
            return false;

        ServerWorld serverWorld = (ServerWorld) playerWorld;

        String targetUUID = getTargetUUID(soulGrabberStack, player);

        if (targetUUID == null) return false;

        if (canUse(player, serverWorld, targetUUID, blockPos))
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
        destinationWorld.setWeather(0, 20 * 60 * phantomDurationMinutes, true, random <= 2);
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

        if (canUse(player, serverWorld, targetUUID, blockPos))
            return useChunkSwap(serverWorld, blockPos, targetUUID);
        return false;
    }

    private boolean useChunkSwap(ServerWorld serverWorld, BlockPos blockPos, String targetUUID) {
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
        String playerName = ItemStackData.getPlayerName(itemStack);

        if (!Objects.equals(playerName, "")) {
            tooltip.add(Text.literal(playerName).formatted(Formatting.BOLD, Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("invasionmod.soul_grabber.empty").formatted(Formatting.ITALIC, Formatting.GRAY));
        }

    }
}

