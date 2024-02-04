package com.invasionmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class InvasionModLootTableProvider extends FabricBlockLootTableProvider {
    public InvasionModLootTableProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        // ...
    }
}