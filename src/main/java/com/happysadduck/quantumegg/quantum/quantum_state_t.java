package com.happysadduck.quantumegg.quantum;

import java.util.Random;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class quantum_state_t {
    public int n;
    public complex_t[] amp;

    public quantum_state_t(int n) {
        this.n = n;
        this.amp = new complex_t[1 << n];
        for (int i = 0; i < amp.length; i++)
            amp[i] = complex_t.ZERO;
        amp[0] = complex_t.ONE;
    }

    public quantum_state_t(int n, complex_t[] amp) {
        this.n = n;
        this.amp = amp.clone();
    }

    public complex_t get(int i) {
        return amp[i];
    }

    public void set(int i, complex_t c) {
        amp[i] = c;
    }

    public double norm() {
        double s = 0;
        for (complex_t c : amp)
            s += c.abs2();
        return Math.sqrt(s);
    }

    public void normalize() {
        double nrm = norm();
        if (nrm < 1e-15)
            return;
        for (int i = 0; i < amp.length; i++)
            amp[i] = amp[i].mul(1.0 / nrm);
    }

    public void apply_single_gate(int pos, complex_t[][] U) {
        int mask = 1 << pos;
        complex_t[] out = amp.clone();
        for (int i = 0; i < amp.length; i++) {
            if ((i & mask) == 0) {
                int i0 = i, i1 = i | mask;
                out[i0] = U[0][0].mul(amp[i0]).add(U[0][1].mul(amp[i1]));
                out[i1] = U[1][0].mul(amp[i0]).add(U[1][1].mul(amp[i1]));
            }
        }
        amp = out;
    }

    public void apply_cnot(int pc, int pt) {
        int mc = 1 << pc, mt = 1 << pt;
        complex_t[] out = amp.clone();
        for (int i = 0; i < amp.length; i++) {
            if ((i & mc) != 0)
                out[i] = amp[i ^ mt];
        }
        amp = out;
    }

    public int measure_at(int pos, Random rng) {
        int mask = 1 << pos;
        double p0 = 0;
        for (int i = 0; i < amp.length; i++)
            if ((i & mask) == 0)
                p0 += amp[i].abs2();
        int result = rng.nextDouble() < p0 ? 0 : 1;

        int new_n = n - 1;
        complex_t[] new_amp = new complex_t[1 << new_n];
        for (int i = 0; i < amp.length; i++) {
            if (((i >> pos) & 1) == result) {
                int low = i & (mask - 1);
                int high = i >> (pos + 1);
                new_amp[low | (high << pos)] = amp[i];
            }
        }
        this.n = new_n;
        this.amp = new_amp;
        normalize();
        return result;
    }

    public quantum_state_t tensor(quantum_state_t other) {
        int new_n = n + other.n;
        complex_t[] new_amp = new complex_t[1 << new_n];
        java.util.Arrays.fill(new_amp, complex_t.ZERO);
        for (int i = 0; i < (1 << n); i++) {
            complex_t a = amp[i];
            if (a.abs2() < 1e-30)
                continue;
            for (int j = 0; j < (1 << other.n); j++)
                new_amp[i | (j << n)] = a.mul(other.amp[j]);
        }
        return new quantum_state_t(new_n, new_amp);
    }

    public quantum_state_t[] try_seperate(int[] posa, int[] posb) {
        int na = posa.length, nb = posb.length;
        int dima = 1 << na, dimb = 1 << nb;

        complex_t[][] m = new complex_t[dima][dimb];
        for (int i = 0; i < (1 << n); i++) {
            int ia = 0, ib = 0;
            for (int p = 0; p < na; p++)
                if (((i >> posa[p]) & 1) != 0)
                    ia |= (1 << p);
            for (int p = 0; p < nb; p++)
                if (((i >> posb[p]) & 1) != 0)
                    ib |= (1 << p);
            m[ia][ib] = amp[i];
        }

        int pi = 0, pj = 0;
        double best = 0;
        for (int i = 0; i < dima; i++) {
            for (int j = 0; j < dimb; j++) {
                double a = m[i][j].abs2();
                if (a > best) {
                    best = a;
                    pi = i;
                    pj = j;
                }
            }
        }
        complex_t pivot = m[pi][pj];

        complex_t[] u = new complex_t[dima];
        complex_t[] v = new complex_t[dimb];
        double invp_2 = 1.0 / pivot.abs2();
        for (int i = 0; i < dima; i++)
            u[i] = m[i][pj];
        for (int j = 0; j < dimb; j++)
            v[j] = m[pi][j].mul(pivot.conj()).mul(invp_2);

        double tol = 1e-8;
        for (int i = 0; i < dima; i++) {
            for (int j = 0; j < dimb; j++) {
                double err = u[i].mul(v[j]).sub(m[i][j]).abs();
                if (err > tol * (1 + m[i][j].abs()))
                    return null;
            }
        }

        double nu = 0, nv = 0;
        for (complex_t c : u)
            nu += c.abs2();
        for (complex_t c : v)
            nv += c.abs2();
        nu = Math.sqrt(nu);
        nv = Math.sqrt(nv);
        for (int i = 0; i < dima; i++)
            u[i] = u[i].mul(1.0 / nu);
        for (int j = 0; j < dimb; j++)
            v[j] = v[j].mul(1.0 / nv);

        return new quantum_state_t[] {
                new quantum_state_t(na, u),
                new quantum_state_t(nb, v)
        };
    }

    public NbtCompound to_nbt(WrapperLookup reg) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("n", n);
        NbtList amp_list = new NbtList();
        for (complex_t c : amp) {
            NbtCompound complex_nbt = new NbtCompound();
            complex_nbt.putDouble("re", c.re);
            complex_nbt.putDouble("im", c.im);
            amp_list.add(complex_nbt);
        }
        nbt.put("amp", amp_list);
        return nbt;
    }

    public static quantum_state_t from_nbt(NbtCompound nbt, WrapperLookup reg) {
        int n = nbt.getInt("n");
        NbtList amp_list = nbt.getList("amp", NbtElement.COMPOUND_TYPE);
        int size = 1 << n;
        complex_t[] amp = new complex_t[size];
        for (int i = 0; i < size; i++) {
            NbtCompound cnbt = amp_list.getCompound(i);
            double re = cnbt.getDouble("re");
            double im = cnbt.getDouble("im");
            amp[i] = new complex_t(re, im);
        }
        return new quantum_state_t(n, amp);
    }
}
