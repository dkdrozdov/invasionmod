package com.invasionmod.mixin;

import com.google.common.collect.ImmutableList;
import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.callback.ServerPlayerEntityCallback;
import com.invasionmod.util.ItemStackData;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.invasionmod.InvasionMod.LOGGER;
import static com.invasionmod.InvasionMod.PHANTOM;

@Debug(export = true)
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityAccess {
    @Shadow
    @Final
    public MinecraftServer server;

    /**
     * Whether the player should receive Travel Stone upon respawning.
     * Used when the player is killed by a phantom.
     */
    @Unique
    boolean invasionmod$shouldGetStone = false;

    /**
     * Whether the player (being phantom) should drop all stolen items back to the invaded world.
     * Used when phantom joins after server restart before invaded world is loaded.
     */
    @Unique
    boolean invasionmod$needReturnLoot;
    @Unique
    ChunkPos invasionmod$lastChunkPos = null;

    /**
     * The world where the player's stolen loot (while being phantom) should be returned to.
     *
     * @see #invasionmod$needReturnLoot
     */
    @Unique
    Identifier invasionmod$returnLootWorld;

    /**
     * How many times player's world can be invaded for free.
     */
    @Unique
    int invasionmod$sinnerCounter = 0;

    @Unique
    public int invasionmod$getSinnerCounter() {
        return invasionmod$sinnerCounter;
    }

    @Unique
    public void invasionmod$setSinnerCounter(int sinnerCounter) {
        invasionmod$sinnerCounter = sinnerCounter;
    }

    public boolean invasionmod$getNeedReturnLoot() {
        return invasionmod$needReturnLoot;
    }

    public boolean invasionmod$getShouldGetStone() {
        return invasionmod$shouldGetStone;
    }

    public void invasionmod$setNeedReturnLoot(boolean _needReturnLoot) {
        invasionmod$needReturnLoot = _needReturnLoot;
    }

    public void invasionmod$setShouldGetStone(boolean _shouldGetStone) {
        invasionmod$shouldGetStone = _shouldGetStone;
    }

    public Identifier invasionmod$getReturnLootWorld() {
        return invasionmod$returnLootWorld;
    }

    public void invasionmod$setReturnLootWorld(Identifier _needReturnLoot) {
        invasionmod$returnLootWorld = _needReturnLoot;
    }

    /*
     * Reads player's custom data to serverplayer properties (called on join).
     * */
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void readReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        invasionmod$needReturnLoot = nbt.getBoolean("needReturnLoot");
        invasionmod$shouldGetStone = nbt.getBoolean("shouldGetStone");
        invasionmod$sinnerCounter = nbt.getInt("sinnerCounter");
    }

    /*
     * Writes player's custom data from serverplayer properties (called on leave).
     * */
    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void writeReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("needReturnLoot", invasionmod$needReturnLoot);
        nbt.putBoolean("shouldGetStone", invasionmod$shouldGetStone);
        nbt.putInt("sinnerCounter", invasionmod$sinnerCounter);
    }

    /*
     * This injection modifies on death method so that dying phantoms get their loot cleared of
     * 'owned by' tag.
     * */
    @Inject(at = @At(value = "HEAD"), method = "onDeath")
    private void onDeathInject(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity playerEntity = ((PlayerEntity) ((Object) this));
        ServerPlayerEntityCallback.ON_PLAYER_DEATH.invoker().notify(((ServerPlayerEntity) ((Object) this)));

        LOGGER.info("Player " + playerEntity.getName() + " died.");
        LOGGER.info("status: " + playerEntity.getStatusEffects().toString());

        if (!playerEntity.hasStatusEffect(PHANTOM) || playerEntity.getWorld().isClient) return;

        LOGGER.info("They were a phantom");

        PlayerInventory inventory = playerEntity.getInventory();
        List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(inventory.main, inventory.armor, inventory.offHand);

        for (List<ItemStack> list : combinedInventory) {
            for (ItemStack stack : list) {
                if (stack.isEmpty()) continue;
                ItemStackData.clearOwned(stack);
                LOGGER.info("Cleared tag from ItemStack: " + stack);
            }
        }
    }

    /*
     * Overwrites general overworld with player's overworld when reading custom data (on join).
     * */
    @ModifyExpressionValue(at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;OVERWORLD:Lnet/minecraft/registry/RegistryKey;"), method = "readCustomDataFromNbt")
    private RegistryKey<World> readCustomDataFromNbtInject(RegistryKey<World> original) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        return DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server).getRegistryKey();
    }

    /*
     * This injection in serverplayer tick adds lastchankpos tracking.
     * */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInject(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        ChunkPos currentChunkPos = player.getWorld().getChunk(new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ())).getPos();

        if (invasionmod$lastChunkPos != null && invasionmod$lastChunkPos != currentChunkPos)
            ServerPlayerEntityCallback.ON_ENTER_CHUNK.invoker().notify(player);

        invasionmod$lastChunkPos = currentChunkPos;
    }

    /*
     * This injection raises onwakeup event when player wakes up.
     * */
    @Inject(method = "wakeUp", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;sendToNearbyPlayers(Lnet/minecraft/entity/Entity;Lnet/minecraft/network/packet/Packet;)V"))
    private void wakeUpInject(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        ServerPlayerEntityCallback.ON_WAKE_UP.invoker().notify(player);
    }

    /*
     * This injection raises onplayerswinghand event when player swings hand.
     * */
    @Inject(at = @At(value = "TAIL"), method = "swingHand")
    private void swingHandInject(Hand hand, CallbackInfo ci) {
        ServerPlayerEntityCallback.ON_PLAYER_SWING_HAND.invoker().notify(hand, ((ServerPlayerEntity) ((Object) this)));
    }


}
