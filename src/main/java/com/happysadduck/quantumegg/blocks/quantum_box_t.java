package com.happysadduck.quantumegg.blocks;

import java.util.ArrayList;
import java.util.List;

import com.happysadduck.quantumegg.items.quantum_egg_t;
import com.happysadduck.quantumegg.quantum.quantum_global_state_t;
import com.happysadduck.quantumegg.registry.mod_items_t;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class quantum_box_t extends BlockWithEntity {

    public quantum_box_t(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState adjacent_block_state, Direction direction) {
        if (adjacent_block_state.isOf(this)) {
            return true;
        }
        return super.isSideInvisible(state, adjacent_block_state, direction);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new quantum_box_entity_t(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(quantum_box_t::new);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState new_state, boolean moved) {
        if (!state.isOf(new_state.getBlock())) {
            BlockEntity block_entity = world.getBlockEntity(pos);
            if (block_entity instanceof quantum_box_entity_t box) {
                ItemScatterer.spawn(world, pos, box);
            }
            super.onStateReplaced(state, world, pos, new_state, moved);
        }
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof quantum_box_entity_t box)) {
            return ActionResult.PASS;
        }
        ItemStack held_stack = player.getMainHandStack();
        if (box.isEmpty() && !held_stack.isEmpty()) {
            box.setStack(0, held_stack);
            held_stack.decrement(1);

            if (held_stack.isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            }
            return ActionResult.SUCCESS;
        }
        if (!box.isEmpty() && held_stack.isEmpty()) {
            ItemStack to_get = box.getStack(0).copy();
            box.clear();
            player.setStackInHand(Hand.MAIN_HAND, to_get);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private boolean is_powered(World world, BlockPos pos) {
        return world.getEmittedRedstonePower(pos.up(), Direction.DOWN) > 0
                || world.getEmittedRedstonePower(pos.down(), Direction.UP) > 0
                || world.getEmittedRedstonePower(pos.north(), Direction.SOUTH) > 0
                || world.getEmittedRedstonePower(pos.south(), Direction.NORTH) > 0
                || world.getEmittedRedstonePower(pos.east(), Direction.WEST) > 0
                || world.getEmittedRedstonePower(pos.west(), Direction.EAST) > 0;
    }

    private void do_quantum_entanglement(World world, BlockPos pos) {
        List<BlockPos> dirs = List.of(
                pos.east(),
                pos.south(),
                pos.west(),
                pos.north(),
                pos.up(),
                pos.down());

        List<quantum_box_entity_t> candidates = new ArrayList<>();
        for (BlockPos neighbor_pos : dirs) {
            BlockEntity be = world.getBlockEntity(neighbor_pos);
            if (!(be instanceof quantum_box_entity_t box))
                continue;
            ItemStack stack = box.getStack(0);
            if (stack.getItem() instanceof quantum_egg_t)
                candidates.add(box);
        }

        if (candidates.size() < 2)
            return;

        MinecraftServer server = world.getServer();
        quantum_global_state_t gs = quantum_global_state_t.get_server_state(server);

        quantum_box_entity_t control_box = candidates.get(0);
        quantum_box_entity_t target_box = candidates.get(1);
        ItemStack control_stack = control_box.getStack(0);
        ItemStack target_stack = target_box.getStack(0);

        int cuuid = quantum_egg_t.get_uuid(control_stack);
        if (cuuid == -1) {
            cuuid = gs.get_new_uuid();
            quantum_egg_t.set_uuid(control_stack, cuuid);
        }
        int tuuid = quantum_egg_t.get_uuid(target_stack);
        if (tuuid == -1) {
            tuuid = gs.get_new_uuid();
            quantum_egg_t.set_uuid(target_stack, tuuid);
        }

        double[] cbloch = quantum_egg_t.get_bloch(control_stack);
        double[] tbloch = quantum_egg_t.get_bloch(target_stack);

        boolean ok = gs.cnot(cuuid, cbloch, tuuid, tbloch);
        if (!ok) {
            ServerWorld server_world = (ServerWorld) world;
            server_world.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    3,
                    0.2, 0.2, 0.2,
                    0.0);
        }

        quantum_egg_t.set_state(control_stack, cbloch[0], cbloch[1], cbloch[2]);
        quantum_egg_t.set_state(target_stack, tbloch[0], tbloch[1], tbloch[2]);
        quantum_egg_t.set_measured(control_stack, 0);
        quantum_egg_t.set_measured(target_stack, 0);
        control_box.markDirty();
        target_box.markDirty();
    }

    private void handle_rising_edge(World world, BlockPos pos, quantum_box_entity_t box) {
        ItemStack bs = box.getStack(0);
        Item item = bs.getItem();

        if (item instanceof quantum_egg_t) {
            quantum_egg_t.rotate_state(world, pos, box, bs);
            box.markDirty();
        } else if (bs.isOf(Items.EGG))
            box.setStack(0, new ItemStack(mod_items_t.QUANTUM_EGG, 1));
        else if (bs.isOf(Items.TNT)) {
            MinecraftServer server = world.getServer();
            quantum_global_state_t gs = quantum_global_state_t.get_server_state(server);
            box.clear();
            gs.clear_all();
            ServerWorld server_world = (ServerWorld) world;
            server_world.spawnParticles(
                    ParticleTypes.FIREWORK,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    15,
                    0.5, 0.5, 0.5,
                    0.0);
        } else
            do_quantum_entanglement(world, pos);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block source_block, BlockPos source_pos,
            boolean notify) {
        if (world.isClient)
            return;
        if (!(world.getBlockEntity(pos) instanceof quantum_box_entity_t box))
            return;
        boolean powered = is_powered(world, pos);
        if (!powered) {
            if (box.get_triggered()) {
                box.set_triggered(false);
            }
            return;
        }
        if (box.get_triggered())
            return;
        box.set_triggered(true);
        handle_rising_edge(world, pos, box);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.isClient())
            return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof quantum_box_entity_t box))
            return 0;
        ItemStack stack = box.getStack(0);
        if (!(stack.getItem() instanceof quantum_egg_t))
            return 0;

        if (quantum_egg_t.is_measured(stack))
            return quantum_egg_t.get_bz(stack) == -1.0 ? 15 : 0;

        int uuid = quantum_egg_t.get_uuid(stack);
        MinecraftServer server = world.getServer();
        quantum_global_state_t gs = quantum_global_state_t.get_server_state(server);
        if (uuid == -1) {
            uuid = gs.get_new_uuid();
            quantum_egg_t.set_uuid(stack, uuid);
        }

        double[] bloch = quantum_egg_t.get_bloch(stack);

        gs.measure(uuid, bloch);
        quantum_egg_t.set_state(stack, bloch[0], bloch[1], bloch[2]);
        quantum_egg_t.set_measured(stack, 1);
        box.markDirty();

        return (bloch[2] == -1.0) ? 15 : 0;
    }
}
