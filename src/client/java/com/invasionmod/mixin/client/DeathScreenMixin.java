package com.invasionmod.mixin.client;

import com.invasionmod.accessor.DeathScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

import static com.invasionmod.InvasionMod.IS_RESPAWN_ALLOWED_REQUEST_PACKET_ID;

@Mixin(DeathScreen.class)
public class DeathScreenMixin implements DeathScreenAccessor {
    @Shadow
    @Final
    private List<ButtonWidget> buttons;

    @Unique
    int invasionmod$ticksWithoutAskingForRespawn = 0;
    @Unique
    boolean invasionmod$respawnAllowed = false;

    @Inject(at = @At("TAIL"), method = "setButtonsActive")
    private void setButtonsActiveInject(boolean active, CallbackInfo ci) {
        invasionmod$respawnAllowed = false;
        if (active) {
            for (ButtonWidget button : buttons) {
                if (button.getMessage() == Text.of("deathScreen.respawn")) {
                    button.active = false;
                    sendIsRespawnAllowedRequest();
                }
            }
        }
    }

    @Unique
    private void setRespawnButtonActive(boolean active) {
        for (ButtonWidget button : buttons) {
            if (Objects.equals(Text.of(button.getMessage()), Text.translatable("deathScreen.respawn"))) {
                button.active = active;
            }
        }
    }

    @Unique
    private void sendIsRespawnAllowedRequest() {
        ClientPlayNetworking.send(IS_RESPAWN_ALLOWED_REQUEST_PACKET_ID, PacketByteBufs.empty());
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void tickInject(CallbackInfo ci) {
        setRespawnButtonActive(invasionmod$respawnAllowed);
        if (!invasionmod$respawnAllowed) {
            invasionmod$ticksWithoutAskingForRespawn++;
            if (invasionmod$ticksWithoutAskingForRespawn >= 20) {
                sendIsRespawnAllowedRequest();
                invasionmod$ticksWithoutAskingForRespawn = 0;
            }
        }
    }

    @Override
    public void invasionmod$setRespawnAllowed(boolean allowed) {
        invasionmod$respawnAllowed = allowed;
    }
}