package com.happysadduck.quantumegg.quantum;

public class complex_t {
    public final double re, im;

    public complex_t(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static final complex_t ZERO = new complex_t(0, 0);
    public static final complex_t ONE = new complex_t(1, 0);

    public complex_t add(complex_t o) {
        return new complex_t((re + o.re), im + o.im);
    }

    public complex_t sub(complex_t o) {
        return new complex_t(re - o.re, im - o.im);
    }

    public complex_t mul(complex_t o) {
        return new complex_t(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public complex_t mul(double s) {
        return new complex_t(re * s, im * s);
    }

    public complex_t conj() {
        return new complex_t(re, -im);
    }

    public double abs2() {
        return re * re + im * im;
    }

    public double abs() {
        return Math.sqrt(abs2());
    }
}