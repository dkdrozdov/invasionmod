package com.invasionmod.item;

import com.invasionmod.DimensionManager;
import com.invasionmod.InvasionMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;
import static com.invasionmod.util.Nbt.getPlayerUuid;
import static com.invasionmod.util.Nbt.hasNbtPlayerUuid;
import static net.minecraft.block.Blocks.*;

public class DimensionGrabberItem extends Item {

    private final static int phantomDurationMinutes = 3;

    public DimensionGrabberItem(Settings settings) {
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

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();

        if (!world.isClient) {
            PlayerEntity player = context.getPlayer();

            if (player == null) return ActionResult.PASS;

            ItemStack itemStack = context.getStack();
            ServerWorld serverWorld = (ServerWorld) world;
            Hand hand = context.getHand();

            if (!hasNbtPlayerUuid(itemStack)) {
                LOGGER.info("Player %s with UUID %s tried to teleport via DimensionGrabberItem, but the destination address is empty."
                        .formatted(player.getName().getString(), player.getUuidAsString()));
                player.sendMessage(Text.of("You have to choose target player first!"), true);
                return ActionResult.FAIL;
            }

            BlockPattern.Result result = InvasionMod.portalPattern.searchAround(serverWorld, context.getBlockPos());

            if (result == null || result.getUp() != Direction.UP) {
                player.sendMessage(Text.of("You need a valid portal to teleport!"), true);

                return ActionResult.FAIL;
            }

            LOGGER.info(result.toString());

            return invade(world, player, hand, result).getResult();
        }
        return ActionResult.PASS;
    }


    public TypedActionResult<ItemStack> invade(World world, PlayerEntity invaderPlayer, Hand hand, BlockPattern.Result validPortalMatch) {
        if (world.isClient) return TypedActionResult.pass(invaderPlayer.getStackInHand(hand));

        MinecraftServer server = world.getServer();
        if (server == null) return TypedActionResult.pass(invaderPlayer.getStackInHand(hand));

        ItemStack itemStack = invaderPlayer.getStackInHand(hand);


        String targetUuid = getPlayerUuid(itemStack);

        if (Objects.equals(targetUuid, invaderPlayer.getUuidAsString())) {
            LOGGER.info("Player " + invaderPlayer.getName().getString() + " with UUID " + invaderPlayer.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is themselves.");
            invaderPlayer.sendMessage(Text.of("You are the target player!"), true);
            return TypedActionResult.fail(invaderPlayer.getStackInHand(hand));
        }

        ServerPlayerEntity targetPlayer = world.getServer().getPlayerManager().getPlayer(UUID.fromString(targetUuid));

        if (targetPlayer == null) {
            LOGGER.info("Player " + invaderPlayer.getName().getString() + " with UUID " + invaderPlayer.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is offline.");
            invaderPlayer.sendMessage(Text.of("Target player is offline."), true);
            return TypedActionResult.fail(invaderPlayer.getStackInHand(hand));
        }

        if (!targetPlayer.isAlive()) {
            LOGGER.info("Player " + invaderPlayer.getName().getString() + " with UUID " + invaderPlayer.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is dead.");
            invaderPlayer.sendMessage(Text.of("Target player is dead."), true);
            return TypedActionResult.fail(invaderPlayer.getStackInHand(hand));
        }

        if (world.getRegistryKey() != DimensionManager.getPlayerWorldRegistry(invaderPlayer.getUuidAsString())) {
            LOGGER.info("Player " + invaderPlayer.getName().getString() + " with UUID " + invaderPlayer.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but the dimension of departure is forbidden: " + world.getRegistryKey().toString());
            invaderPlayer.sendMessage(Text.of("You can teleport only from your own world!"), true);
            return TypedActionResult.fail(invaderPlayer.getStackInHand(hand));
        }

        RuntimeWorldHandle destinationWorldHandle = DimensionManager.getPlayerWorldHandle(targetUuid, server);
        ServerWorld destinationWorld = destinationWorldHandle.asWorld();
        playUseSound(world, invaderPlayer);
        playUseSound(destinationWorld, invaderPlayer);
        addUseParticles(world, invaderPlayer);
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

        return TypedActionResult.success(invaderPlayer.getStackInHand(hand));
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
}

