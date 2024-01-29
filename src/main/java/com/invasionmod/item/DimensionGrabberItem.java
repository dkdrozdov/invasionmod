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

            return teleport(world, player, hand, result).getResult();
        }
        return ActionResult.PASS;
    }


    public TypedActionResult<ItemStack> teleport(World world, PlayerEntity playerEntity, Hand hand, BlockPattern.Result validPortalMatch) {
        if (world.isClient) return TypedActionResult.pass(playerEntity.getStackInHand(hand));

        MinecraftServer server = world.getServer();
        if (server == null) return TypedActionResult.pass(playerEntity.getStackInHand(hand));

        ItemStack itemStack = playerEntity.getStackInHand(hand);


        String targetUuid = getPlayerUuid(itemStack);

        if (Objects.equals(targetUuid, playerEntity.getUuidAsString())) {
            LOGGER.info("Player " + playerEntity.getName().getString() + " with UUID " + playerEntity.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is themselves.");
            playerEntity.sendMessage(Text.of("You are the target player!"), true);
            return TypedActionResult.fail(playerEntity.getStackInHand(hand));
        }

        if (world.getServer().getPlayerManager().getPlayer(UUID.fromString(targetUuid)) == null) {
            LOGGER.info("Player " + playerEntity.getName().getString() + " with UUID " + playerEntity.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but target player is offline.");
            playerEntity.sendMessage(Text.of("Target player is offline."), true);
            return TypedActionResult.fail(playerEntity.getStackInHand(hand));
        }

        if (world.getRegistryKey() != DimensionManager.getPlayerWorldRegistry(playerEntity.getUuidAsString())) {
            LOGGER.info("Player " + playerEntity.getName().getString() + " with UUID " + playerEntity.getUuidAsString() +
                    " tried to teleport to world " + DimensionManager.getPlayerWorldRegistry(targetUuid).toString() +
                    " via DimensionGrabberItem, but the dimension of departure is forbidden: " + world.getRegistryKey().toString());
            playerEntity.sendMessage(Text.of("You can teleport only from your own world!"), true);
            return TypedActionResult.fail(playerEntity.getStackInHand(hand));
        }

        RuntimeWorldHandle destinationWorldHandle = DimensionManager.getPlayerWorldHandle(targetUuid, server);
        playUseSound(world, playerEntity);
        addUseParticles(world, playerEntity);

        Vec3d portalCenter = getOrCreatePortal(destinationWorldHandle.asWorld(), validPortalMatch);

        ((ServerPlayerEntity) playerEntity).teleport(destinationWorldHandle.asWorld(),
                portalCenter.getX(),
                portalCenter.getY(),
                portalCenter.getZ(),
                playerEntity.getYaw(),
                playerEntity.getPitch());

        addPhantomStatusEffects(playerEntity);
        playerEntity.getItemCooldownManager().set(this, 20);

        return TypedActionResult.success(playerEntity.getStackInHand(hand));
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
                20 * 60 * 5,
                0,
                true,
                true,
                true);

        playerEntity.addStatusEffect(statusEffectInstance);

        playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                20 * 60 * 10,
                1,
                false,
                false,
                false));
    }
}

