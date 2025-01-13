package com.invasionmod.entity.effect;

import com.invasionmod.DimensionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static com.invasionmod.InvasionMod.*;

public class RepellingStatusEffect extends StatusEffect {

    public RepellingStatusEffect() {
        super(StatusEffectCategory.NEUTRAL, 0xfff7af);
    }

    // Called every tick to check if the effect can be applied or not
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof PlayerEntity playerEntity) {
            LOGGER.info("Applied update repelling status effect: player %s".formatted(playerEntity.getName().getString()));

            MinecraftServer server = playerEntity.getServer();
            String playerUuid = playerEntity.getUuidAsString();
            ServerWorld world = DimensionManager.getPlayerWorldHandle(playerUuid, server).asWorld();

            List<ServerPlayerEntity> players = world.getPlayers((player -> player.hasStatusEffect(PHANTOM)));

            for(ServerPlayerEntity phantom : players){
                phantom.addStatusEffect(new StatusEffectInstance(STRANGER,
                        20*60*3, 0, true, true, true));
            }

            if(!players.isEmpty()){
                entity.removeStatusEffect(REPELLING);
                world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(),
                        SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.NEUTRAL, 1.0f, 0.5f);
            }
        }

        super.applyUpdateEffect(entity, amplifier);
    }
}
