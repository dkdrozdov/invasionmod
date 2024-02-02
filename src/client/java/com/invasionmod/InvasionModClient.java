package com.invasionmod;

import com.invasionmod.accessor.DeathScreenAccessor;
import com.invasionmod.renderer.GhostEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import static com.invasionmod.InvasionMod.ALLOW_RESPAWN_PACKET_ID;
import static com.invasionmod.InvasionMod.GHOST;

public class InvasionModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GHOST, (context) -> new GhostEntityRenderer(context, new PlayerEntityModel<>(
                context.getPart(EntityModelLayers.PLAYER_SLIM), false), 0.5f));

        ClientPlayNetworking.registerGlobalReceiver(ALLOW_RESPAWN_PACKET_ID, (client, handler, buf, responseSender) -> {
            Screen currentScreen = client.currentScreen;
            if (currentScreen instanceof DeathScreen deathScreen) {
                ((DeathScreenAccessor) deathScreen).invasionmod$setRespawnAllowed(true);
            }
        });
    }
}