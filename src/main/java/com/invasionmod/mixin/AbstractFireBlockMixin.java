package com.invasionmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(AbstractFireBlock.class)
public abstract class AbstractFireBlockMixin {
    /*
    * By default, a nether portal lights up only in overworld (by registry key).
    * Worlds created by this mod are of overworld dimension type, but do not have
    * overworld registry key, so that they don't pass the default check.
    *
    * This injection replaces code in the AbstractFireBlock class'
    * IsOverworldOrNetherModify method that is called as a condition for portal lighting.
    * */
    @ModifyReturnValue(at = @At(value = "RETURN"), method = "isOverworldOrNether")
    private static boolean onIsOverworldOrNetherModify(boolean original, World world) {
        return original || (world.getDimensionKey() == DimensionTypes.OVERWORLD);
    }

}
  