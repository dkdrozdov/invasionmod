package com.invasionmod;

import com.invasionmod.entity.effect.PhantomStatusEffect;
import com.invasionmod.item.DimensionGrabberItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvasionMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "invasionmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item TRAVEL_STONE =
            Registry.register(Registries.ITEM, new Identifier(MOD_ID, "travel_stone"),
                    new Item(new FabricItemSettings()));

    public static final Item DIMENSION_GRABBER =
            Registry.register(Registries.ITEM, new Identifier(MOD_ID, "dimension_grabber"),
                    new DimensionGrabberItem(new FabricItemSettings()));
    public static final Item CHUNK_SWITCHER =
            Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chunk_switcher"),
                    new Item(new FabricItemSettings()));

    public static final StatusEffect PHANTOM =
            Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "phantom"),
                    new PhantomStatusEffect());

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(content -> content.add(TRAVEL_STONE));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(content -> content.add(DIMENSION_GRABBER));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(content -> content.add(CHUNK_SWITCHER));


        // block right-clicking restrictions
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            if (player.hasStatusEffect(PHANTOM)) {
                Item usedItem = player.getStackInHand(hand).getItem();

                if (usedItem == Items.FLINT_AND_STEEL || usedItem == Items.FIRE_CHARGE) {
                    player.sendMessage(Text.of("You can't place fire while invading other worlds!"), true);

                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });

        // block breaking restrictions
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
        {
            if (player.hasStatusEffect(PHANTOM) &&
                    (state.isIn(BlockTags.CROPS) || state.isIn(BlockTags.FLOWERS) || state.isIn(BlockTags.TALL_FLOWERS))) {
                player.sendMessage(Text.of("You can't break this block (directly) while invading other world!"), true);

                return false;
            }

            return true;
        });

        // entity use restrictions
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack itemStack = player.getStackInHand(hand);
            Item usedItem = itemStack.getItem();

            if (player.hasStatusEffect(PHANTOM)
                    && (entity instanceof CreeperEntity)
                    && (usedItem == Items.FLINT_AND_STEEL
                    || usedItem == Items.FIRE_CHARGE)) {
                player.sendMessage(Text.of("You can't light creepers while invading other world!"), true);

                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        //  sleep restrictions
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, sleepingPos) ->
        {
            if (player.hasStatusEffect(PHANTOM)) {
                player.sendMessage(Text.of("You can't change spawn point while invading other world!"), true);

                return false;
            }
            return true;
        });

        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) ->
                player.hasStatusEffect(PHANTOM) ? PlayerEntity.SleepFailureReason.NOT_POSSIBLE_HERE : null);

        // entity attacking restrictions
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            if (player.hasStatusEffect(PHANTOM) && !(entity instanceof PlayerEntity)) {
                player.sendMessage(Text.of("You can't attack mobs while invading other world!"), true);

                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        // remove phantom status when player joins and spawns at their world
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player.hasStatusEffect(PHANTOM)
                    && DimensionManager.getPlayerWorldHandle(player.getUuidAsString(), player.server).asWorld()
                    == player.getWorld()) {
                player.removeStatusEffect(PHANTOM);
                player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            }
        });
    }
}