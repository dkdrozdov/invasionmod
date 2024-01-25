package com.invasionmod.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import static com.invasionmod.InvasionMod.MOD_ID;

public class Nbt {
    private static final String PLAYER_UUID = "player_uuid";
    private static final String PLAYER_NAME = "player_name";

    static public NbtCompound createNbtIfAbsent(ItemStack stack) {
        if (!stack.hasNbt()) {
            stack.setNbt(new NbtCompound());
        }
        return stack.getOrCreateSubNbt(MOD_ID);
    }

    static private int getNbtInt(ItemStack stack, String nbtName) {
        int value = 0;
        NbtCompound nbt = createNbtIfAbsent(stack);

        if (!nbt.contains(nbtName)) {
            nbt.putInt(nbtName, value);
        } else {
            value = nbt.getInt(nbtName);
        }

        return value;
    }

    static private String getNbtString(ItemStack stack, String nbtName) {
        String value = "";
        NbtCompound nbt = createNbtIfAbsent(stack);

        if (!nbt.contains(nbtName)) {
            nbt.putString(nbtName, value);
        } else {
            value = nbt.getString(nbtName);
        }

        return value;
    }

    static private void setNbt(ItemStack stack, String nbtName, int value) {
        stack.getOrCreateSubNbt(MOD_ID).putInt(nbtName, value);
    }

    static private void setNbt(ItemStack stack, String nbtName, String value) {
        stack.getOrCreateSubNbt(MOD_ID).putString(nbtName, value);
    }

    static public String getPlayerUuid(ItemStack stack) {
        return getNbtString(stack, PLAYER_UUID);
    }

    public static void setPlayerUuid(ItemStack stack, String playerUuid) {
        setNbt(stack, PLAYER_UUID, playerUuid);
    }

    static public String getPlayerName(ItemStack stack) {
        return getNbtString(stack, PLAYER_NAME);
    }

    static public void setPlayerName(ItemStack stack, String playerName) {
        setNbt(stack, PLAYER_NAME, playerName);
    }

    static public boolean hasNbtPlayerUuid(ItemStack stack) {
        NbtCompound nbt = createNbtIfAbsent(stack);

        return nbt.contains(PLAYER_UUID);
    }
}
