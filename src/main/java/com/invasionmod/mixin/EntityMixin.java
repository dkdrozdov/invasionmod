package com.invasionmod.mixin;

import com.invasionmod.DimensionManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyExpressionValue(at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;OVERWORLD:Lnet/minecraft/registry/RegistryKey;"), method = "tickPortal")
    private RegistryKey<World> tickPortalModifyOverworld(RegistryKey<World> original) {
        Entity entity = ((Entity) ((Object) this));

        if (entity.getWorld().isClient) return original;

        if (entity instanceof ServerPlayerEntity serverPlayerEntity)
            return DimensionManager.getPlayerWorldRegistry(serverPlayerEntity.getUuidAsString());

        return original;
    }

}
  