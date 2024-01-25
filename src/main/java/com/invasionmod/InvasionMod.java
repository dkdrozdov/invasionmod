package com.invasionmod;

import com.invasionmod.item.DimensionGrabberItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
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
    }
}