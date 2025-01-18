package com.invasionmod;

import com.invasionmod.access.ServerPlayerEntityAccess;
import com.invasionmod.callback.ServerPlayerEntityCallback;
import com.invasionmod.entity.GhostEntity;
import com.invasionmod.entity.effect.PhantomStatusEffect;
import com.invasionmod.entity.effect.RepellingStatusEffect;
import com.invasionmod.entity.effect.StrangerStatusEffect;
import com.invasionmod.item.ChunkSwapperItem;
import com.invasionmod.item.SoulGrabberItem;
import com.invasionmod.item.TravelStoneItem;
import com.invasionmod.registry.Registry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class InvasionMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final String MOD_ID = "invasionmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier IS_RESPAWN_ALLOWED_REQUEST_PACKET_ID = new Identifier(MOD_ID, "is_respawn_allowed_request");
    public static final Identifier ALLOW_RESPAWN_PACKET_ID = new Identifier(MOD_ID, "is_respawn_allowed_answer");
    public static final Item TRAVEL_STONE = Registry.register(new TravelStoneItem(new FabricItemSettings()), "travel_stone");
    public static final StatusEffect PHANTOM = Registry.register(new PhantomStatusEffect(), "phantom");
    public static final StatusEffect REPELLING = Registry.register(new RepellingStatusEffect(), "repelling");
    public static final StatusEffect STRANGER = Registry.register(new StrangerStatusEffect(), "stranger");
    public static final FoodComponent REPELLING_FRUIT_FOOD_COMPONENT = new FoodComponent.Builder()
            .alwaysEdible().snack().statusEffect(new StatusEffectInstance(REPELLING, 2 * 60 * 20, 0), 1.0f).build();
    public static final Item REPELLING_FRUIT = Registry.register(new Item(new FabricItemSettings().food(REPELLING_FRUIT_FOOD_COMPONENT)), "repelling_fruit");
    public static final Item SOUL_GRABBER = Registry.register(new SoulGrabberItem(new FabricItemSettings().maxCount(1)), "soul_grabber");
    public static final Item CHUNK_SWAPPER = Registry.register(new ChunkSwapperItem(new FabricItemSettings()), "chunk_swapper");
    public static final Block SUPPRESSOR = Registry.register(new Block(FabricBlockSettings.create().strength(3f).requiresTool()), "suppressor");
    public static final BlockItem SUPPRESSOR_ITEM = Registry.register(new BlockItem(SUPPRESSOR, new FabricItemSettings()), "suppressor");
    public static BlockPattern portalPattern = null;
    private static final Predicate<BlockState> IS_LIT_SOUL_CAMPFIRE = state -> state != null && state.isOf(Blocks.SOUL_CAMPFIRE) && state.get(CampfireBlock.LIT);
    public static final EntityType<GhostEntity> GHOST = Registry.register(FabricEntityTypeBuilder.create(SpawnGroup.MISC, GhostEntity::new).disableSaving().dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(32).trackedUpdateRate(2).build(), "ghost");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(content -> content.add(TRAVEL_STONE));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(content -> content.add(SOUL_GRABBER));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(content -> content.add(CHUNK_SWAPPER));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
                .register(content -> content.add(SUPPRESSOR_ITEM));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
                .register(content -> content.add(REPELLING_FRUIT));

        FabricDefaultAttributeRegistry.register(GHOST, GhostEntity.createLivingAttributes());

        if (portalPattern == null) {
            portalPattern = BlockPatternBuilder
                    .start()
                    .aisle("AAAAA", "AAAAA", "AAAAA")
                    .aisle("AAAAA", "ACACA", "AAAAA")
                    .aisle("AAAAA", "AAAAA", "AAAAA")
                    .aisle("AAAAA", "ACACA", "AAAAA")
                    .aisle("AAAAA", "AAAAA", "AAAAA")
                    .where('C', CachedBlockPosition.matchesBlockState(IS_LIT_SOUL_CAMPFIRE))
                    .where('A', CachedBlockPosition.matchesBlockState(BlockStatePredicate.ANY))
                    .build();
        }

        // block right-clicking restrictions
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            if (player.hasStatusEffect(PHANTOM)) {
                Item usedItem = player.getStackInHand(hand).getItem();

                if (usedItem == Items.FLINT_AND_STEEL || usedItem == Items.FIRE_CHARGE) {
                    player.sendMessage(Text.translatable("invasionmod.phantom.cant_place_fire"), true);

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
                player.sendMessage(Text.translatable("invasionmod.phantom.cant_break_block"), true);

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
                player.sendMessage(Text.translatable("invasionmod.phantom.cant_light_creepers"), true);

                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        //  sleep restrictions
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, sleepingPos) ->
        {
            if (player.hasStatusEffect(PHANTOM)) {
                player.sendMessage(Text.translatable("invasionmod.phantom.cant_change_spawn"), true);

                return false;
            }
            return true;
        });

        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) ->
                player.hasStatusEffect(PHANTOM) ? PlayerEntity.SleepFailureReason.NOT_POSSIBLE_HERE : null);

        // entity attacking restrictions
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            if ((player.hasStatusEffect(PHANTOM) && !player.hasStatusEffect(STRANGER)) && !(entity instanceof PlayerEntity)) {
                player.sendMessage(Text.translatable("invasionmod.phantom.cant_attack_mobs"), true);

                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        // remove phantom status when player joins and spawns at their world
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerPlayerEntityAccess serverPlayerEntityAccess = (ServerPlayerEntityAccess) player;

            if (player.hasStatusEffect(PHANTOM) && serverPlayerEntityAccess.invasionmod$getNeedReturnLoot()) {
                player.removeStatusEffect(PHANTOM);
                player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            }

            GhostManager.onPlayerJoin(player);
        });

        //
        ServerChunkEvents.CHUNK_LOAD.register(GhostManager::onChunkLoad);

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> GhostManager.onChunkUnload());

        ServerPlayerEntityCallback.ON_ENTER_CHUNK.register(GhostManager::updateGhosts);

        ServerPlayNetworking.registerGlobalReceiver(IS_RESPAWN_ALLOWED_REQUEST_PACKET_ID, (server, player, handler, buf, responseSender) -> server.execute(() -> {
            if (player.getWorld().getPlayers().stream().noneMatch(playerEntity -> playerEntity.hasStatusEffect(PHANTOM)))
                ServerPlayNetworking.send(player, ALLOW_RESPAWN_PACKET_ID, PacketByteBufs.empty());
        }));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {

            // Transfer data

            ((ServerPlayerEntityAccess) newPlayer).invasionmod$setSinnerCounter(((ServerPlayerEntityAccess) oldPlayer).invasionmod$getSinnerCounter());

            // Award Travel Stone

            if (((ServerPlayerEntityAccess) oldPlayer).invasionmod$getShouldGetStone()) {
                ItemStack stoneStackForKilled = new ItemStack(TRAVEL_STONE, 1);
                newPlayer.giveItemStack(stoneStackForKilled);
                ((ServerPlayerEntityAccess) newPlayer).invasionmod$setShouldGetStone(false);
            }
        });
    }
}