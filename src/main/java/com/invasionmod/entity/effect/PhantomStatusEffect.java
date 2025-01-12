package com.invasionmod.entity.effect;

import com.google.common.collect.ImmutableList;
import com.invasionmod.DimensionManager;
import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.util.ItemStackData;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

import static com.invasionmod.InvasionMod.LOGGER;

public class PhantomStatusEffect extends StatusEffect {

    public PhantomStatusEffect() {
        super(StatusEffectCategory.NEUTRAL, 0xad1917);
    }

    // This method is called every tick to check whether it should apply the status effect or not
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof PlayerEntity player) {
            PlayerInventory inventory = player.getInventory();
            List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(inventory.main, inventory.armor, inventory.offHand);

            for (List<ItemStack> list : combinedInventory) {
                for (ItemStack stack : list) {
                    if (stack.isEmpty()) continue;
                    ItemStackData.setIsOwned(stack, true);
                }
            }
        }
    }

    public static void manageLoot(ServerPlayerEntity serverPlayerEntity) {
        PlayerInventory playerInventory = serverPlayerEntity.getInventory();
        List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(playerInventory.main, playerInventory.armor, playerInventory.offHand);
        boolean returnLoot = false;
        ServerWorld returnWorld = null;
        ServerPlayerEntityAccess serverPlayerEntityAccess = (ServerPlayerEntityAccess) serverPlayerEntity;

        if (serverPlayerEntityAccess.invasionmod$getNeedReturnLoot()) {
            Identifier returnWorldId = serverPlayerEntityAccess.invasionmod$getReturnLootWorld();
            returnWorld = DimensionManager.getPlayerWorldHandle(returnWorldId, serverPlayerEntity.getServer()).asWorld();
            returnLoot = true;
            serverPlayerEntityAccess.invasionmod$setNeedReturnLoot(false);

            LOGGER.info(("Player %s is tagged with needReturnLoot. Returning stolen items to world %s.")
                    .formatted(serverPlayerEntity.getName().getString(), returnWorldId));
        }

        int allowedStacks = returnLoot ? 0 : 2;
        int allowedStackSize = 16;


        for (List<ItemStack> list : combinedInventory) {
            for (int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = list.get(i);

                if (itemStack.isEmpty()) continue;
                if (ItemStackData.getIsOwned(itemStack)) {
                    ItemStackData.clearOwned(itemStack);
                    continue;
                }

                if (allowedStacks > 0) {
                    if (itemStack.getCount() > allowedStackSize) {
                        int originalCount = itemStack.getCount();
                        ItemStack droppedStack = itemStack.copy();

                        droppedStack.setCount(originalCount - allowedStackSize);

                        ItemEntity itemEntity = serverPlayerEntity.dropItem(droppedStack, true, false);
                        if (itemEntity != null)
                            itemEntity.setPortalCooldown(300);

                        itemStack.setCount(allowedStackSize);
                    }
                    allowedStacks--;

                } else {
                    if (returnLoot) {
                        LOGGER.info("Returning stack: %s".formatted(itemStack.toString()));

                        ItemEntity itemEntity = new ItemEntity(returnWorld,
                                serverPlayerEntity.getX(),
                                serverPlayerEntity.getEyeY() - (double) 0.3f,
                                serverPlayerEntity.getZ(),
                                itemStack);

                        itemEntity.setPickupDelay(40);
                        returnWorld.spawnEntity(itemEntity);
                    } else {
                        ItemEntity itemEntity = serverPlayerEntity.dropItem(itemStack, true, false);
                        if (itemEntity != null)
                            itemEntity.setPortalCooldown(300);
                    }
                    list.set(i, ItemStack.EMPTY);

                }
            }
        }
    }
}
