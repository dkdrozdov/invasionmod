package com.invasionmod.entity;

import com.invasionmod.callback.ServerPlayerEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.List;
import java.util.Objects;

import static com.invasionmod.InvasionMod.LOGGER;

public class GhostEntity extends LivingEntity {
    PlayerEntity player;
    protected Vec3d lastVelocity;

    public GhostEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
        noClip = true;
        this.lastVelocity = Vec3d.ZERO;
        setNoGravity(true);
        ServerPlayerEntityCallback.ON_PLAYER_SWING_HAND.register(this::onPlayerSwingHand);
        ServerPlayerEntityCallback.ON_PLAYER_DEATH.register(this::onPlayerDeath);
        setInvisible(true);
    }

    @Override
    protected void tickStatusEffects() {
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return false;
    }

    private void onPlayerDeath(ServerPlayerEntity serverPlayerEntity) {
        if (player != null && player == serverPlayerEntity) {
            onDeath(getDamageSources().generic());
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return List.of();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
    }

    private void despawnIfPlayerIsInvalid() {
        World world = getWorld();

        if ((!world.isClient)) {
            if (player == null || player.isRemoved()) {
                LOGGER.info("A ghost is discarded in dimension " + world.getRegistryKey().getValue() + " because of not having a player.");
                discard();
            } else {
                if (Objects.requireNonNull(world.getServer()).getPlayerManager().getPlayer(player.getUuid()) == null ||
                        player.getWorld().getDimensionKey() != DimensionTypes.OVERWORLD ||
                        player.getWorld() == world) {
                    LOGGER.info("Player " + player.getName().getString() + "'s ghost discarded in dimension " + world.getRegistryKey().getValue());
                    player = null;
                    discard();
                }
            }
        }
    }

    @Override
    public void tick() {
        // despawn ghost if the player in invalid state

        despawnIfPlayerIsInvalid();

        if (!getEntityWorld().isClient && player != null) {
            this.lastVelocity = player.getVelocity();
            copyPositionAndRotation(player);
            setHeadYaw(player.getHeadYaw());
            setPose(player.getPose());
        }

        super.tick();
    }


    @Override
    public void tickMovement() {
        this.tickHandSwing();
        super.tickMovement();
    }

    public Vec3d lerpVelocity(float tickDelta) {
        return this.lastVelocity.lerp(this.getVelocity(), tickDelta);
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    public void setPlayer(PlayerEntity _player) {
        player = _player;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public void onPlayerSwingHand(Hand hand, ServerPlayerEntity serverPlayerEntity) {
        if (player != null && serverPlayerEntity == player) {
//            LOGGER.info("Player" + player.getName().getString() + " swing hand. Swinging ghost's hand.");

            swingHand(hand);
        }
    }
}
