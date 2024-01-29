package com.invasionmod.mixin;

import com.google.common.collect.ImmutableList;
import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
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
    boolean invasionmod$NeedReturnLoot;

    @Unique
    Identifier invasionmod$ReturnLootWorld;

    public boolean invasionmod$getNeedReturnLoot() {
        return invasionmod$NeedReturnLoot;
    }

    public void invasionmod$setNeedReturnLoot(boolean _needReturnLoot) {
        invasionmod$NeedReturnLoot = _needReturnLoot;
    }

    public Identifier invasionmod$getReturnLootWorld() {
        return invasionmod$ReturnLootWorld;
    }

    public void invasionmod$setReturnLootWorld(Identifier _needReturnLoot) {
        invasionmod$ReturnLootWorld = _needReturnLoot;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void readReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        invasionmod$NeedReturnLoot = nbt.getBoolean("needReturnLoot");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void writeReturnLoot(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("needReturnLoot", invasionmod$NeedReturnLoot);
    }

    @Inject(at = @At(value = "HEAD"), method = "onDeath")
    private void onDeathInject(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity playerEntity = ((PlayerEntity) ((Object) this));

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
}
