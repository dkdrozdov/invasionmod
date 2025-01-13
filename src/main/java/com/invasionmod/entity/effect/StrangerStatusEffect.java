package com.invasionmod.entity.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class StrangerStatusEffect extends StatusEffect {

    public StrangerStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0xbf1736);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
