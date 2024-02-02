package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.invasionmod.entity.GhostEntity;
import com.invasionmod.util.Nbt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.invasionmod.InvasionMod.DIMENSION_GRABBER;
import static com.invasionmod.InvasionMod.LOGGER;

@Debug(export = true)
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    /**
     * @param targetEntity The entity that PlayerEntity interacts with.
     * @param hand         The hand that holds the item that PlayerEntity has used on @entity
     */
    @Inject(at = @At(value = "HEAD"), method = "interact", cancellable = true)
    private void interactInject(Entity targetEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (targetEntity.getWorld().isClient) return;
        PlayerEntity playerEntity = ((PlayerEntity) ((Object) this));
        ItemStack itemStack = playerEntity.getStackInHand(hand);

        LOGGER.info(" player " + playerEntity.getUuidAsString() + " clicked with " +
                itemStack.getItem().getName().getString() + " on " +
                targetEntity.getName().getString() + " of type " + targetEntity.getClass() + ".");

        if (((targetEntity instanceof PlayerEntity) || (targetEntity instanceof GhostEntity)) && itemStack.isOf(DIMENSION_GRABBER)) {
            if (Nbt.hasNbtPlayerUuid(itemStack)) {
                LOGGER.info(("Player %s with UUID %s tried to grab dimension of player %s " +
                        "with UUID %s via DimensionGrabberItem, but the itemStack already has destination: %s")
                        .formatted(playerEntity.getName().getString(),
                                playerEntity.getUuidAsString(),
                                targetEntity.getName().toString(),
                                targetEntity.getUuidAsString(),
                                DimensionManager.getPlayerWorldRegistry(Nbt.getPlayerUuid(itemStack)).toString()));
                playerEntity.sendMessage(Text.of("Dimension grabber already has destination!"), true);
                return;
            }

            String playerName;
            String playerUuid;
            PlayerEntity targetPlayer = null;

            if (targetEntity instanceof PlayerEntity targetPlayerEntity) targetPlayer = targetPlayerEntity;

            if (targetEntity instanceof GhostEntity ghostEntity) targetPlayer = ghostEntity.getPlayer();

            if (targetPlayer == null) {
                LOGGER.info("Target player is null! That shouldn't happen.");
                return;
            }

            playerName = targetPlayer.getName().getString();
            playerUuid = targetPlayer.getUuidAsString();

            Nbt.setPlayerName(itemStack, playerName);
            Nbt.setPlayerUuid(itemStack, playerUuid);

            LOGGER.info("grabbed uuid: " + playerUuid);

            playerEntity.getItemCooldownManager().set(itemStack.getItem(), 20);

            World targetEntityWorld = targetPlayer.getWorld();
            World playerEntityWorld = playerEntity.getWorld();

            playerEntityWorld.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                    SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS);

            if (targetEntityWorld != playerEntityWorld)
                targetEntityWorld.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                        SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }
}
  