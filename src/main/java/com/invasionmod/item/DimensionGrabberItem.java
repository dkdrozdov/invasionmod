package com.invasionmod.item;

import com.invasionmod.DimensionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.UUID;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.util.Nbt.getPlayerUuid;
import static com.invasionmod.util.Nbt.hasNbtPlayerUuid;

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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        if (world.isClient) return TypedActionResult.pass(playerEntity.getStackInHand(hand));

        MinecraftServer server = world.getServer();
        if (server == null) return TypedActionResult.pass(playerEntity.getStackInHand(hand));

        ItemStack itemStack = playerEntity.getStackInHand(hand);
        if (!hasNbtPlayerUuid(itemStack)) {
            LOGGER.info("Player " + playerEntity.getName().getString() + " with UUID " + playerEntity.getUuidAsString() +
                    " tried to teleport via DimensionGrabberItem, but the destination address is empty.");
            playerEntity.sendMessage(Text.of("You have to choose target player first!"), true);
            return TypedActionResult.fail(playerEntity.getStackInHand(hand));
        }


        String targetUuid = getPlayerUuid(itemStack);

        if (world.getServer().getPlayerManager().getPlayer(UUID.fromString(targetUuid)) == null){
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

        ((ServerPlayerEntity)playerEntity).teleport(destinationWorldHandle.asWorld(),
                playerEntity.getX(),
                playerEntity.getY(),
                playerEntity.getZ(),
                playerEntity.getYaw(),
                playerEntity.getPitch());

        playerEntity.getItemCooldownManager().set(this, 20);

        return TypedActionResult.success(playerEntity.getStackInHand(hand));
    }
}

