package com.invasionmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.TickCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

import static com.invasionmod.InvasionMod.MOD_ID;
import static com.invasionmod.InvasionMod.TRAVEL_STONE;

public class InvasionModAdvancementProvider extends FabricAdvancementProvider {

    protected InvasionModAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<AdvancementEntry> consumer) {
        // Root
        AdvancementEntry root = Advancement.Builder.create()
                .display(
                        Blocks.SOUL_LANTERN,
                        Text.translatable("advancement.invasionmod.root.title"),
                        Text.translatable("advancement.invasionmod.root.description"),
                        new Identifier("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.TASK,
                        false,
                        false,
                        false)
                .criterion(
                        "entered_anything",
                        TickCriterion.Conditions.createLocation(LocationPredicate.Builder.create()))
                .build(consumer, MOD_ID + "root");


        // Getting a Travel Stone
        AdvancementEntry getTravelStone = Advancement.Builder.create()
                .parent(root)
                .display(
                        TRAVEL_STONE,
                        Text.translatable("advancement.invasionmod.get_travel_stone.title"),
                        Text.translatable("advancement.invasionmod.get_travel_stone.description"),
                        null,
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("got_dirt", InventoryChangedCriterion.Conditions.items(TRAVEL_STONE))
                .build(consumer, MOD_ID + "/get_travel_stone");
    }
}
