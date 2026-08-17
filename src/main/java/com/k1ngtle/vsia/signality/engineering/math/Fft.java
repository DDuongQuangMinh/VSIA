package com.k1ngtle.vsia.signality.engineering.math;

public final class Fft {
    private Fft() {
    }

    public static Complex[] fft(Complex[] input) {
        int n = input.length;

        if (n == 0 || Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException("FFT length must be a non-zero power of two");
        }

        Complex[] output = input.clone();

        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;

            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }

            j ^= bit;

            if (i < j) {
                Complex temp = output[i];
                output[i] = output[j];
                output[j] = temp;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            Complex wLen = new Complex(Math.cos(angle), Math.sin(angle));

            for (int i = 0; i < n; i += len) {
                Complex w = new Complex(1.0, 0.0);

                for (int j = 0; j < len / 2; j++) {
                    Complex even = output[i + j];
                    Complex odd = output[i + j + len / 2].multiply(w);

                    output[i + j] = even.add(odd);
                    output[i + j + len / 2] = even.subtract(odd);

                    w = w.multiply(wLen);
                }
            }
        }

        return output;
    }

    public static Complex[] ifft(Complex[] input) {
        Complex[] conjugated = new Complex[input.length];

        for (int i = 0; i < input.length; i++) {
            conjugated[i] = new Complex(input[i].re(), -input[i].im());
        }

        Complex[] transformed = fft(conjugated);
        Complex[] output = new Complex[input.length];

        for (int i = 0; i < transformed.length; i++) {
            output[i] = new Complex(
                    transformed[i].re() / input.length,
                    -transformed[i].im() / input.length
            );
        }

        return output;
    }
}
