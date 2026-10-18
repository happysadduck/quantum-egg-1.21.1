package com.happysadduck.quantumegg.client;

import com.happysadduck.quantumegg.registry.mod_blocks_t;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class quantum_egg_client_t implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), mod_blocks_t.QUANTUM_BOX);
        BlockEntityRendererFactories.register(mod_blocks_t.QUANTUM_BOX_ENTITY, quantum_box_entity_renderer_t::new);
    }
}
