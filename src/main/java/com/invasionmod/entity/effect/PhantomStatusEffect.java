package com.invasionmod.entity.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class PhantomStatusEffect extends StatusEffect {

    public PhantomStatusEffect() {
        super(StatusEffectCategory.NEUTRAL, 0xad1917);
    }
    
    // This method is called every tick to check whether it should apply the status effect or not
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

}
