package com.invasionmod.mixin;

import com.google.common.collect.ImmutableList;
import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.callback.ServerPlayerEntityCallback;
import com.invasionmod.util.Nbt;
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

    @Unique
    boolean invasionmod$shouldGetStone = false;

    @Unique
    boolean invasionmod$needReturnLoot;
    @Unique
    ChunkPos invasionmod$lastChunkPos = null;

    @Unique
    Identifier invasionmod$ReturnLootWorld;

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
        return invasionmod$ReturnLootWorld;
    }

    public void invasionmod$setReturnLootWorld(Identifier _needReturnLoot) {
        invasionmod$ReturnLootWorld = _needReturnLoot;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void readReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        invasionmod$needReturnLoot = nbt.getBoolean("needReturnLoot");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void writeReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("needReturnLoot", invasionmod$needReturnLoot);
    }

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
                Nbt.clearOwned(stack);
                LOGGER.info("Cleared tag from ItemStack: " + stack);
            }
        }
    }

    @ModifyExpressionValue(at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;OVERWORLD:Lnet/minecraft/registry/RegistryKey;"), method = "readCustomDataFromNbt")
    private RegistryKey<World> readCustomDataFromNbtInject(RegistryKey<World> original) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        return DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), server).getRegistryKey();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInject(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        ChunkPos currentChunkPos = player.getWorld().getChunk(new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ())).getPos();

//        LOGGER.info("prev %s cur %s ".formatted(((invasionmod$lastChunkPos == null) ? "nol" : invasionmod$lastChunkPos.toString()), currentChunkPos.toString()));
        if (invasionmod$lastChunkPos != null && invasionmod$lastChunkPos != currentChunkPos)
            ServerPlayerEntityCallback.ON_ENTER_CHUNK.invoker().notify(player);

        invasionmod$lastChunkPos = currentChunkPos;
    }

    @Inject(method = "wakeUp", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;sendToNearbyPlayers(Lnet/minecraft/entity/Entity;Lnet/minecraft/network/packet/Packet;)V"))
    private void wakeUpInject(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) ((Object) this);

        ServerPlayerEntityCallback.ON_WAKE_UP.invoker().notify(player);
    }

    @Inject(at = @At(value = "TAIL"), method = "swingHand")
    private void swingHandInject(Hand hand, CallbackInfo ci) {
        ServerPlayerEntityCallback.ON_PLAYER_SWING_HAND.invoker().notify(hand, ((ServerPlayerEntity) ((Object) this)));
    }


}
