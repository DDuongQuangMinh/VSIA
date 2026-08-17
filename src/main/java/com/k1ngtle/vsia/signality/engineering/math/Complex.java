package com.k1ngtle.vsia.signality.engineering.math;

public record Complex(double re, double im) {
    public static final Complex ZERO = new Complex(0.0, 0.0);

    public Complex add(Complex other) {
        return new Complex(re + other.re, im + other.im);
    }

    public Complex subtract(Complex other) {
        return new Complex(re - other.re, im - other.im);
    }

    public Complex multiply(Complex other) {
        return new Complex(
                re * other.re - im * other.im,
                re * other.im + im * other.re
        );
    }

    public Complex scale(double scalar) {
        return new Complex(re * scalar, im * scalar);
    }

    public double magnitudeSquared() {
        return re * re + im * im;
    }
}
