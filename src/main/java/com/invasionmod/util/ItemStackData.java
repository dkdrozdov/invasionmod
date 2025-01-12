package com.invasionmod.util;

import com.invasionmod.item.SoulGrabberItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import static com.invasionmod.InvasionMod.MOD_ID;

public class ItemStackData {
    private static final String PLAYER_UUID = "player_uuid";
    private static final String PLAYER_NAME = "player_name";
    private static final String IS_OWNED = "is_owned";

    static public NbtCompound createDataIfAbsent(ItemStack stack) {
        if (!stack.hasNbt()) {
            stack.setNbt(new NbtCompound());
        }
        return stack.getOrCreateSubNbt(MOD_ID);
    }

    static private int getDataInt(ItemStack stack, String nbtName) {
        int value = 0;
        NbtCompound nbt = createDataIfAbsent(stack);

        if (!nbt.contains(nbtName)) {
            nbt.putInt(nbtName, value);
        } else {
            value = nbt.getInt(nbtName);
        }

        return value;
    }

    static private String getDataString(ItemStack stack, String nbtName) {
        String value = "";
        NbtCompound nbt = createDataIfAbsent(stack);

        if (!nbt.contains(nbtName)) {
            nbt.putString(nbtName, value);
        } else {
            value = nbt.getString(nbtName);
        }

        return value;
    }

    static private void setData(ItemStack stack, String nbtName, int value) {
        stack.getOrCreateSubNbt(MOD_ID).putInt(nbtName, value);
    }

    static private void setData(ItemStack stack, String nbtName, String value) {
        stack.getOrCreateSubNbt(MOD_ID).putString(nbtName, value);
    }

    static public String getPlayerUuid(ItemStack stack) {
        return getDataString(stack, PLAYER_UUID);
    }

    public static void setPlayerUuid(ItemStack stack, String playerUuid) {
        setData(stack, PLAYER_UUID, playerUuid);
    }

    static public String getPlayerName(ItemStack stack) {
        return getDataString(stack, PLAYER_NAME);
    }

    static public void setPlayerName(ItemStack stack, String playerName) {
        setData(stack, PLAYER_NAME, playerName);
    }


    static public void setIsOwned(ItemStack stack, boolean isOwned) {
        if (isOwned)
            setData(stack, IS_OWNED, "true");
        else
            clearData(stack, IS_OWNED);
    }

    static private void clearData(ItemStack stack, String nbtName) {
        stack.getOrCreateSubNbt(MOD_ID).remove(nbtName);
        if (!(stack.getItem() instanceof SoulGrabberItem))
            stack.removeSubNbt(MOD_ID);
    }

    static public void clearOwned(ItemStack stack) {
        clearData(stack, IS_OWNED);
    }

    static public boolean getIsOwned(ItemStack stack) {
        NbtCompound nbt = createDataIfAbsent(stack);
        if (!nbt.contains(IS_OWNED)) {
            clearData(stack, IS_OWNED);

            return false;
        }

        return true;
    }

    static public boolean hasDataPlayerUuid(ItemStack stack) {
        NbtCompound nbt = createDataIfAbsent(stack);

        return nbt.contains(PLAYER_UUID);
    }

}
