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


    @ModifyReturnValue(at = @At(value = "RETURN"), method = "isOverworldOrNether")
    private static boolean onIsOverworldOrNetherModify(boolean original, World world) {
        return original || (world.getDimensionKey() == DimensionTypes.OVERWORLD);
    }

}
  