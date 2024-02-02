package com.invasionmod.callback;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public interface ServerPlayerEntityCallback {
    Event<ServerPlayerEntityCallback.onPlayer> ON_ENTER_CHUNK = EventFactory.createArrayBacked(ServerPlayerEntityCallback.onPlayer.class,
            (listeners) -> (ServerPlayerEntity player) -> {
                for (ServerPlayerEntityCallback.onPlayer listener : listeners) {
                    listener.notify(player);
                }
            });

    Event<ServerPlayerEntityCallback.onPlayer> ON_WAKE_UP = EventFactory.createArrayBacked(ServerPlayerEntityCallback.onPlayer.class,
            (listeners) -> (ServerPlayerEntity player) -> {
                for (ServerPlayerEntityCallback.onPlayer listener : listeners) {
                    listener.notify(player);
                }
            });

    Event<ServerPlayerEntityCallback.onSwingHand> ON_PLAYER_SWING_HAND = EventFactory.createArrayBacked(ServerPlayerEntityCallback.onSwingHand.class,
            (listeners) -> (Hand hand, ServerPlayerEntity player) -> {
                for (ServerPlayerEntityCallback.onSwingHand listener : listeners) {
                    listener.notify(hand, player);
                }
            });
    Event<ServerPlayerEntityCallback.onPlayer> ON_PLAYER_DEATH = EventFactory.createArrayBacked(ServerPlayerEntityCallback.onPlayer.class,
            (listeners) -> (ServerPlayerEntity player) -> {
                for (ServerPlayerEntityCallback.onPlayer listener : listeners) {
                    listener.notify(player);
                }
            });
    @FunctionalInterface
    interface onSwingHand {
        void notify(Hand hand, ServerPlayerEntity player);
    }

    @FunctionalInterface
    interface onPlayer {
        void notify(ServerPlayerEntity player);
    }

}