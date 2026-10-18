package com.happysadduck.quantumegg.client;

import com.happysadduck.quantumegg.blocks.quantum_box_entity_t;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class quantum_box_entity_renderer_t implements BlockEntityRenderer<quantum_box_entity_t> {
    private final ItemRenderer item_renderer;

    public quantum_box_entity_renderer_t(BlockEntityRendererFactory.Context ctx) {
        this.item_renderer = ctx.getItemRenderer();
    }

    @Override
    public void render(quantum_box_entity_t entity, float tick_delta, MatrixStack matrices,
            VertexConsumerProvider vertex_consumers, int light, int overlay) {
        ItemStack stack = entity.getStack(0);
        if (stack.isEmpty())
            return;

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(0.5f, 0.5f, 0.5f);
        float time = entity.getWorld().getTime() + tick_delta;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 2.0f));
        float bobbing = (float) Math.sin(time * 0.1f) * 0.05f;
        matrices.translate(0, bobbing, 0);

        item_renderer.renderItem(stack, ModelTransformationMode.FIXED, light, overlay, matrices, vertex_consumers,
                entity.getWorld(), 0);
        matrices.pop();
    }
}
