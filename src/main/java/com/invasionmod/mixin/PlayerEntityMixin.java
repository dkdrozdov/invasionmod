package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
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
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.invasionmod.InvasionMod.DIMENSION_GRABBER;

@Debug(export = true)
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @param targetEntity The entity that PlayerEntity interacts with.
     * @param hand         The hand that holds the item that PlayerEntity has used on @entity
     */
    @Inject(at = @At(value = "HEAD"), method = "interact", cancellable = true)
    private void interactInject(Entity targetEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity playerEntity = ((PlayerEntity) ((Object) this));
        ItemStack itemStack = playerEntity.getStackInHand(hand);

        LOGGER.info(" player " + playerEntity.getUuidAsString() + " clicked with " +
                playerEntity.getStackInHand(hand).getItem().getName().getString() + " on " +
                targetEntity.getName().getString() + " of type " + getClass() + ".");

        if (targetEntity instanceof PlayerEntity targetPlayer && itemStack.isOf(DIMENSION_GRABBER)) {
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

            Nbt.setPlayerName(itemStack, targetPlayer.getName().getString());
            Nbt.setPlayerUuid(itemStack, targetPlayer.getUuidAsString());

            LOGGER.info("grabbed uuid: " + targetPlayer.getUuidAsString());

            playerEntity.getItemCooldownManager().set(itemStack.getItem(), 20);

            World targetEntityWorld = targetEntity.getWorld();
            World playerEntityWorld = playerEntity.getWorld();

            playerEntityWorld.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                    SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS);

            if(targetEntityWorld != playerEntityWorld)
                targetEntityWorld.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                        SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }

}
  