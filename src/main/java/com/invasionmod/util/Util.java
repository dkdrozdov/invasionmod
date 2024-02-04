package com.invasionmod.util;

import net.minecraft.util.Hand;

public class Util {
    public static Hand getOpposite(Hand hand) {
        if (hand == Hand.MAIN_HAND) return Hand.OFF_HAND;
        else return Hand.MAIN_HAND;
    }
}
