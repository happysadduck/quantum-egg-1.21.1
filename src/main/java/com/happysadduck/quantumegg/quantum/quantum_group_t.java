package com.happysadduck.quantumegg.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class quantum_group_t {
    public final List<Integer> quuid;
    public int qcnt;
    public quantum_state_t state;
    public final Map<Integer, Integer> uuid_to_pos_map;

    public quantum_group_t(List<Integer> quuid, quantum_state_t state) {
        this.quuid = new ArrayList<>(quuid);
        this.qcnt = this.quuid.size();
        this.state = state;
        this.uuid_to_pos_map = new HashMap<>();
        for (int i = 0; i < qcnt; i++) {
            uuid_to_pos_map.put(this.quuid.get(i), i);
        }
    }

    public int uuid_to_pos(int uuid) {
        return uuid_to_pos_map.get(uuid);
    }

    public void rotate(int pos, double ax, double ay, double az) {
        complex_t[][] u = quantum_global_state_t.matmul(quantum_global_state_t.rz(az),
                quantum_global_state_t.matmul(quantum_global_state_t.ry(ay), quantum_global_state_t.rx(ax)));
        state.apply_single_gate(pos, u);
        state.normalize();
    }

    public void cnot(int pc, int pt) {
        state.apply_cnot(pc, pt);
    }

    public static quantum_group_t create_union_group(quantum_group_t g1, quantum_group_t g2) {
        quantum_state_t combined = g1.state.tensor(g2.state);
        List<Integer> new_uuids = new ArrayList<>(g1.quuid);
        new_uuids.addAll(g2.quuid);
        return new quantum_group_t(new_uuids, combined);
    }

    public double[] measure(int pos, Random rng) {
        int result = state.measure_at(pos, rng);
        quuid.remove(pos);
        qcnt--;
        uuid_to_pos_map.clear();
        for (int i = 0; i < qcnt; i++)
            uuid_to_pos_map.put(quuid.get(i), i);
        return result == 0 ? new double[] { 0, 0, 1 } : new double[] { 0, 0, -1 };
    }

    public NbtCompound to_nbt(WrapperLookup reg) {
        NbtCompound nbt = new NbtCompound();

        NbtList quuid_list = new NbtList();
        for (int id : quuid)
            quuid_list.add(NbtInt.of(id));
        nbt.put("quuid", quuid_list);
        nbt.putInt("qcnt", qcnt);
        nbt.put("state", state.to_nbt(reg));
        NbtCompound pos_map_nbt = new NbtCompound();
        uuid_to_pos_map.forEach((uuid, pos) -> pos_map_nbt.putInt(Integer.toString(uuid), pos));
        nbt.put("uuid_to_pos", pos_map_nbt);

        return nbt;
    }

    public static quantum_group_t from_nbt(NbtCompound nbt, WrapperLookup reg) {
        NbtList quuid_list = nbt.getList("quuid", NbtElement.INT_TYPE);
        List<Integer> quuid = new ArrayList<>();
        for (NbtElement el : quuid_list)
            quuid.add(((NbtInt) el).intValue());

        NbtCompound state_nbt = nbt.getCompound("state");
        quantum_state_t state = quantum_state_t.from_nbt(state_nbt, reg);

        quantum_group_t g = new quantum_group_t(quuid, state);

        return g;
    }
}
