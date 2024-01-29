package com.invasionmod.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import static com.invasionmod.InvasionMod.MOD_ID;

public class Nbt {
    private static final String PLAYER_UUID = "player_uuid";
    private static final String PLAYER_NAME = "player_name";
    private static final String IS_OWNED = "is_owned";

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


    static public void setIsOwned(ItemStack stack, boolean isOwned) {
        if (isOwned)
            setNbt(stack, IS_OWNED, "true");
        else
            clearNbt(stack, IS_OWNED);
    }

    static private void clearNbt(ItemStack stack, String nbtName) {
        stack.getOrCreateSubNbt(MOD_ID).remove(nbtName);
    }

    static public void clearOwned(ItemStack stack) {
        clearNbt(stack, IS_OWNED);
    }

    static public boolean getIsOwned(ItemStack stack) {
        NbtCompound nbt = createNbtIfAbsent(stack);

        return nbt.contains(IS_OWNED);
    }

    static public boolean hasNbtPlayerUuid(ItemStack stack) {
        NbtCompound nbt = createNbtIfAbsent(stack);

        return nbt.contains(PLAYER_UUID);
    }

}
