package com.invasionmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import static com.invasionmod.InvasionMod.*;
import static net.minecraft.item.Items.*;

public class InvasionModRecipeProvider  extends FabricRecipeProvider {

    public InvasionModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(RecipeExporter exporter) {

        // Travel Stone

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TRAVEL_STONE)
                .input(ENDER_PEARL)
                .input(SOUL_SAND)
                .criterion(FabricRecipeProvider.hasItem(ENDER_PEARL), FabricRecipeProvider.conditionsFromItem(ENDER_PEARL))
                .criterion(FabricRecipeProvider.hasItem(SOUL_SAND), FabricRecipeProvider.conditionsFromItem(SOUL_SAND))
                .offerTo(exporter, Identifier.of(MOD_ID, getRecipeName(TRAVEL_STONE)));

        // Travel Stone from Suppressor

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TRAVEL_STONE)
                .input(SUPPRESSOR_ITEM)
                .criterion(FabricRecipeProvider.hasItem(SUPPRESSOR_ITEM), FabricRecipeProvider.conditionsFromItem(SUPPRESSOR_ITEM))
                .offerTo(exporter, Identifier.of(MOD_ID, getRecipeName(TRAVEL_STONE)+"_from_suppressor"));


        // Suppressor

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SUPPRESSOR_ITEM)
                .input(TRAVEL_STONE)
                .input(SOUL_SAND)
                .criterion(FabricRecipeProvider.hasItem(TRAVEL_STONE), FabricRecipeProvider.conditionsFromItem(TRAVEL_STONE))
                .criterion(FabricRecipeProvider.hasItem(SOUL_SAND), FabricRecipeProvider.conditionsFromItem(SOUL_SAND))
                .offerTo(exporter);

        // Soul Grabber

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SOUL_GRABBER)
                .input(IRON_HOE)
                .input(SOUL_SAND)
                .input(GOLD_INGOT)
                .criterion(FabricRecipeProvider.hasItem(SOUL_SAND), FabricRecipeProvider.conditionsFromItem(SOUL_SAND))
                .offerTo(exporter);

        // Repelling Fruit

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, REPELLING_FRUIT)
                .input(BEETROOT)
                .input(SPIDER_EYE)
                .criterion(FabricRecipeProvider.hasItem(BEETROOT), FabricRecipeProvider.conditionsFromItem(BEETROOT))
                .criterion(FabricRecipeProvider.hasItem(SPIDER_EYE),FabricRecipeProvider.conditionsFromItem(SPIDER_EYE))
                .offerTo(exporter);
    }
}

