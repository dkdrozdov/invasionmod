package com.invasionmod.mixin.client;

import com.invasionmod.renderer.GhostEntityRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @ModifyExpressionValue(at = @At(value = "CONSTANT", args = "floatValue=0.15"), method = "render")
    private float modifyTeamInvisibleAlphaValue(float original, LivingEntity livingEntity) {
        //noinspection unchecked
        LivingEntityRenderer<T, M> livingEntityRenderer = (LivingEntityRenderer<T, M>) ((Object) this);
        if (livingEntityRenderer instanceof GhostEntityRenderer ghostEntityRenderer)
            return ghostEntityRenderer.getAlphaValue();
        else return original;
    }
}