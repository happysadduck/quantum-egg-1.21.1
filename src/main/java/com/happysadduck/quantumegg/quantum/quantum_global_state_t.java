package com.happysadduck.quantumegg.quantum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public class quantum_global_state_t extends PersistentState {
    private final Map<Integer, quantum_group_t> uuid_to_group = new HashMap<>();
    private final int max_size;
    private final Random rng = new Random();
    private int next_uuid;

    private static class part_t {
        final List<Integer> uuids;
        final quantum_state_t state;

        part_t(List<Integer> u, quantum_state_t s) {
            uuids = u;
            state = s;
        }
    }

    public quantum_global_state_t() {
        this.max_size = 8;
        this.next_uuid = 0;
    }

    private static int[] to_int_array(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            arr[i] = list.get(i);
        return arr;
    }

    private List<part_t> split(List<Integer> uuids, quantum_state_t state) {
        int n = uuids.size();
        List<part_t> result = new ArrayList<>();
        if (n <= 1) {
            result.add(new part_t(new ArrayList<>(uuids), state));
            return result;
        }

        int other_cnt = n - 1;
        int total_mask = 1 << other_cnt;
        for (int mask = 0; mask < total_mask; mask++) {
            List<Integer> idxa = new ArrayList<>();
            idxa.add(0);
            for (int j = 0; j < other_cnt; j++) {
                if ((mask & (1 << j)) != 0)
                    idxa.add(j + 1);
            }
            List<Integer> idxb = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (!idxa.contains(i))
                    idxb.add(i);
            }
            if (idxb.isEmpty())
                continue;

            int[] a = to_int_array(idxa);
            int[] b = to_int_array(idxb);
            quantum_state_t[] sep = state.try_seperate(a, b);
            if (sep != null) {
                List<Integer> uuidsa = new ArrayList<>();
                for (int i : idxa)
                    uuidsa.add(uuids.get(i));
                List<Integer> uuidsb = new ArrayList<>();
                for (int i : idxb)
                    uuidsb.add(uuids.get(i));
                result.addAll(split(uuidsa, sep[0]));
                result.addAll(split(uuidsb, sep[1]));
                return result;
            }
        }

        result.add(new part_t(new ArrayList<>(uuids), state));
        return result;
    }

    private void register_group(quantum_group_t g) {
        for (int uuid : g.quuid)
            uuid_to_group.put(uuid, g);
    }

    private static quantum_state_t bloch_to_state(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        x /= len;
        y /= len;
        z /= len;
        z = Math.max(-1.0, Math.min(1.0, z));
        double theta = Math.acos(z);
        double phi = Math.atan2(y, x);
        complex_t a = new complex_t(Math.cos(theta / 2), 0);
        complex_t b = new complex_t(
                Math.cos(phi) * Math.sin(theta / 2),
                Math.sin(phi) * Math.sin(theta / 2));
        return new quantum_state_t(1, new complex_t[] { a, b });
    }

    private static double[] state_to_bloch(quantum_state_t s) {
        complex_t a = s.get(0), b = s.get(1);
        double x = 2 * a.conj().mul(b).re;
        double y = 2 * a.conj().mul(b).im;
        double z = a.abs2() - b.abs2();
        return new double[] { x, y, z };
    }

    private void measure_isolated(double[] bloch) {
        double p0 = (1 + bloch[2]) / 2;
        double[] nb = rng.nextDouble() < p0 ? new double[] { 0, 0, 1 } : new double[] { 0, 0, -1 };
        System.arraycopy(nb, 0, bloch, 0, 3);
    }

    static complex_t[][] rx(double ax) {
        double c = Math.cos(ax / 2), s = Math.sin(ax / 2);
        return new complex_t[][] {
                { new complex_t(c, 0), new complex_t(0, -s) },
                { new complex_t(0, -s), new complex_t(c, 0) }
        };
    }

    static complex_t[][] ry(double ay) {
        double c = Math.cos(ay / 2), s = Math.sin(ay / 2);
        return new complex_t[][] {
                { new complex_t(c, 0), new complex_t(-s, 0) },
                { new complex_t(s, 0), new complex_t(c, 0) }
        };
    }

    static complex_t[][] rz(double az) {
        double c = Math.cos(az / 2), s = Math.sin(az / 2);
        return new complex_t[][] {
                { new complex_t(c, -s), complex_t.ZERO },
                { complex_t.ZERO, new complex_t(c, s) }
        };
    }

    static complex_t[][] matmul(complex_t[][] a, complex_t[][] b) {
        int n = a.length, m = b[0].length, k = b.length;
        complex_t[][] c = new complex_t[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                complex_t s = complex_t.ZERO;
                for (int l = 0; l < k; l++)
                    s = s.add(a[i][l].mul(b[l][j]));
                c[i][j] = s;
            }
        }
        return c;
    }

    private double[] rotate_isolated(int uuid, double[] bloch, double ax, double ay, double az) {
        quantum_state_t s = bloch_to_state(bloch[0], bloch[1], bloch[2]);
        complex_t[][] u = matmul(rz(az), matmul(ry(ay), rx(ax)));
        s.apply_single_gate(0, u);
        s.normalize();
        double[] nb = state_to_bloch(s);

        return nb;
    }

    public int get_new_uuid() {
        int allocated = next_uuid;
        next_uuid += 1;
        markDirty();
        return allocated;
    }

    public boolean organize_group(quantum_group_t group) {
        Map<Integer, quantum_group_t> saved = new HashMap<>();
        for (int uuid : group.quuid) {
            quantum_group_t old = uuid_to_group.get(uuid);
            if (old != null)
                saved.put(uuid, old);
            uuid_to_group.remove(uuid);
        }

        List<part_t> parts = split(group.quuid, group.state);

        for (part_t p : parts) {
            if (p.uuids.size() > max_size) {
                uuid_to_group.putAll(saved);
                return false;
            }
        }

        for (part_t p : parts) {
            quantum_group_t ng = new quantum_group_t(p.uuids, p.state);
            register_group(ng);
        }
        return true;
    }

    public void rotate(int uuid, double[] bloch, double ax, double ay, double az) {
        quantum_group_t g = uuid_to_group.get(uuid);
        if (g == null) {
            System.arraycopy(rotate_isolated(uuid, bloch, ax, ay, az), 0, bloch, 0, 3);
            return;
        }
        if (g.qcnt == 1) {
            uuid_to_group.remove(uuid);
            g.rotate(0, ax, ay, az);
            markDirty();
            System.arraycopy(state_to_bloch(g.state), 0, bloch, 0, 3);
            return;
        }
        int pos = g.uuid_to_pos(uuid);
        g.rotate(pos, ax, ay, az);
        System.arraycopy(new double[] { 0, 0, 1 }, 0, bloch, 0, 3);
        markDirty();
    }

    public boolean cnot(int cuuid, double[] cbloch, int tuuid, double[] tbloch) {
        if (cuuid == tuuid)
            return false;
        if (!uuid_to_group.containsKey(cuuid)) {
            quantum_group_t g = new quantum_group_t(Collections.singletonList(cuuid),
                    bloch_to_state(cbloch[0], cbloch[1], cbloch[2]));
            register_group(g);
        }
        if (!uuid_to_group.containsKey(tuuid)) {
            quantum_group_t g = new quantum_group_t(Collections.singletonList(tuuid),
                    bloch_to_state(tbloch[0], tbloch[1], tbloch[2]));
            register_group(g);
        }

        quantum_group_t gc = uuid_to_group.get(cuuid);
        quantum_group_t gt = uuid_to_group.get(tuuid);

        if (gc == gt) {
            int pc = gc.uuid_to_pos(cuuid);
            int pt = gc.uuid_to_pos(tuuid);
            gc.cnot(pc, pt);
            organize_group(gc);
            markDirty();
            return true;
        }
        quantum_group_t ug = quantum_group_t.create_union_group(gc, gt);
        int pc = gc.uuid_to_pos(cuuid);
        int pt = gt.uuid_to_pos(tuuid) + gc.qcnt;
        ug.cnot(pc, pt);

        boolean ok = organize_group(ug);
        markDirty();

        return ok;
    }

    public void measure(int uuid, double[] bloch) {
        quantum_group_t g = uuid_to_group.get(uuid);

        if (g == null) {
            measure_isolated(bloch);
            return;
        }
        uuid_to_group.remove(uuid);
        if (g.qcnt == 1) {
            System.arraycopy(state_to_bloch(g.state), 0, bloch, 0, 3);
            measure_isolated(bloch);
            markDirty();
            return;
        }
        int pos = g.uuid_to_pos(uuid);
        System.arraycopy(g.measure(pos, rng), 0, bloch, 0, 3);
        organize_group(g);
        markDirty();
    }

    public void clear_all() {
        uuid_to_group.clear();
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup reg) {
        NbtCompound group_map_nbt = new NbtCompound();
        for (Map.Entry<Integer, quantum_group_t> entry : uuid_to_group.entrySet()) {
            int qubit_uuid = entry.getKey();
            quantum_group_t group = entry.getValue();
            group_map_nbt.put(String.valueOf(qubit_uuid), group.to_nbt(reg));
        }
        nbt.put("uuid_to_group", group_map_nbt);
        nbt.putInt("next_uuid", next_uuid);
        return nbt;
    }

    public static quantum_global_state_t from_nbt(NbtCompound nbt, WrapperLookup reg) {
        quantum_global_state_t state = new quantum_global_state_t();
        NbtCompound group_map_nbt = nbt.getCompound("uuid_to_group");
        for (String key_str : group_map_nbt.getKeys()) {
            int qubit_uuid = Integer.parseInt(key_str);
            NbtCompound group_nbt = group_map_nbt.getCompound(key_str);
            quantum_group_t group = quantum_group_t.from_nbt(group_nbt, reg);
            state.uuid_to_group.put(qubit_uuid, group);
        }
        state.next_uuid = nbt.getInt("next_uuid");
        return state;
    }

    private static final PersistentState.Type<quantum_global_state_t> TYPE = new PersistentState.Type<>(
            quantum_global_state_t::new,
            quantum_global_state_t::from_nbt,
            null);

    public static quantum_global_state_t get_server_state(MinecraftServer server) {
        var world = server.getWorld(World.OVERWORLD);
        var manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, "quantum_global_state");
    }
}
