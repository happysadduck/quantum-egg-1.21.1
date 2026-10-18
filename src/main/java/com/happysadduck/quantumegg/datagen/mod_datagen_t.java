package com.happysadduck.quantumegg.datagen;

import java.util.concurrent.CompletableFuture;

import com.happysadduck.quantumegg.registry.mod_blocks_t;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class mod_datagen_t implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabric_data_generator) {
        FabricDataGenerator.Pack pack = fabric_data_generator.createPack();
        pack.addProvider(block_loot_provider_t::new);
    }

    private static class block_loot_provider_t extends FabricBlockLootTableProvider {
        protected block_loot_provider_t(FabricDataOutput data_output,
                CompletableFuture<WrapperLookup> registry_lookup) {
            super(data_output, registry_lookup);
        }

        @Override
        public void generate() {
            addDrop(mod_blocks_t.QUANTUM_BOX);
        }
    }
}
