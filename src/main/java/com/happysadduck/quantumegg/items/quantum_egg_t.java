package com.happysadduck.quantumegg.items;

import com.happysadduck.quantumegg.blocks.quantum_box_entity_t;
import com.happysadduck.quantumegg.quantum.quantum_global_state_t;
import com.happysadduck.quantumegg.registry.mod_data_components_t;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class quantum_egg_t extends Item {
    public quantum_egg_t(Settings settings) {
        super(settings);
    }

    public static double get_bx(ItemStack stack) {
        return stack.getOrDefault(mod_data_components_t.BX, 0.0);
    }

    public static double get_by(ItemStack stack) {
        return stack.getOrDefault(mod_data_components_t.BY, 0.0);
    }

    public static double get_bz(ItemStack stack) {
        return stack.getOrDefault(mod_data_components_t.BZ, 1.0);
    }

    public static int get_uuid(ItemStack stack) {
        return stack.getOrDefault(mod_data_components_t.UUID, -1);
    }

    public static void set_state(ItemStack stack, double bx, double by, double bz) {
        stack.set(mod_data_components_t.BX, bx);
        stack.set(mod_data_components_t.BY, by);
        stack.set(mod_data_components_t.BZ, bz);
    }

    public static boolean is_measured(ItemStack stack) {
        return stack.getOrDefault(mod_data_components_t.MEASURED, 1) == 1 ? true : false;
    }

    public static void set_measured(ItemStack stack, int v) {
        stack.set(mod_data_components_t.MEASURED, v);
    }

    public static void set_uuid(ItemStack stack, int new_uuid) {
        stack.set(mod_data_components_t.UUID, new_uuid);
    }

    public static double[] get_bloch(ItemStack stack) {
        double[] bloch = { get_bx(stack), get_by(stack), get_bz(stack) };
        return bloch;
    }

    public static void rotate_state(World world, BlockPos pos, quantum_box_entity_t box, ItemStack bs) {
        MinecraftServer server = world.getServer();
        quantum_global_state_t gs = quantum_global_state_t.get_server_state(server);

        int uuid = get_uuid(bs);

        if (uuid == -1) {
            uuid = gs.get_new_uuid();
            set_uuid(bs, uuid);
        }

        int up = world.getEmittedRedstonePower(pos.up(), Direction.DOWN);
        int down = world.getEmittedRedstonePower(pos.down(), Direction.UP);
        int north = world.getEmittedRedstonePower(pos.north(), Direction.SOUTH);
        int south = world.getEmittedRedstonePower(pos.south(), Direction.NORTH);
        int east = world.getEmittedRedstonePower(pos.east(), Direction.WEST);
        int west = world.getEmittedRedstonePower(pos.west(), Direction.EAST);
        int y_rotation = up > down ? up : down;
        int x_rotation = east > west ? east : west;
        int z_rotation = south > north ? south : north;

        double[] bloch = { get_bx(bs), get_by(bs), get_bz(bs) };
        if (x_rotation != 0)
            gs.rotate(uuid, bloch, x_rotation * Math.PI / 8.0, 0.0, 0.0);
        else if (z_rotation != 0)
            gs.rotate(uuid, bloch, 0.0, 0.0, z_rotation * Math.PI / 8.0);
        else if (y_rotation != 0)
            gs.rotate(uuid, bloch, 0.0, y_rotation * Math.PI / 8.0, 0.0);
        else
            return;

        set_measured(bs, 0);
        set_state(bs, bloch[0], bloch[1], bloch[2]);
        box.markDirty();
    }
}
