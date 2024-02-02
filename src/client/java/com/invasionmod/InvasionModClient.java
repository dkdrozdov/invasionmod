package com.invasionmod;

import com.invasionmod.renderer.GhostEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import static com.invasionmod.InvasionMod.GHOST;

public class InvasionModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GHOST, (context) -> new GhostEntityRenderer(context, new PlayerEntityModel<>(
                context.getPart(EntityModelLayers.PLAYER_SLIM), false), 0.5f));
    }
}