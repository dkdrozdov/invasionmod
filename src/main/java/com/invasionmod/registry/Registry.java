package com.invasionmod.registry;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import static com.invasionmod.InvasionMod.MOD_ID;

public class Registry {
    public static Item register(Item item, String id) {
        Identifier itemID = new Identifier(MOD_ID, id);
        return net.minecraft.registry.Registry.register(Registries.ITEM, itemID, item);
    }
    public static Block register(Block block, String id) {
        Identifier blockID = new Identifier(MOD_ID, id);
        return net.minecraft.registry.Registry.register(Registries.BLOCK, blockID, block);
    }
    public static BlockItem register(BlockItem blockItem, String id) {
        Identifier blockItemID = new Identifier(MOD_ID, id);
        return net.minecraft.registry.Registry.register(Registries.ITEM, blockItemID, blockItem);
    }
    public static StatusEffect register(StatusEffect statusEffect, String id) {
        Identifier itemID = new Identifier(MOD_ID, id);
        return net.minecraft.registry.Registry.register(Registries.STATUS_EFFECT, itemID, statusEffect);
    }
    public static <T extends Entity> EntityType<T> register(EntityType<T> statusEffect, String id) {
        Identifier entityTypeID = new Identifier(MOD_ID, id);
        return net.minecraft.registry.Registry.register(Registries.ENTITY_TYPE, entityTypeID, statusEffect);
    }

}
