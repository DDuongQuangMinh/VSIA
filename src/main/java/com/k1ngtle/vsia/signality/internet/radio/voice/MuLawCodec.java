package com.k1ngtle.vsia.signality.internet.radio.voice;

public final class MuLawCodec {
    private static final int BIAS = 0x84;
    private static final int CLIP = 32635;

    private MuLawCodec() {
    }

    public static byte encode(short pcm) {
        int sample = pcm;
        int sign = (sample >> 8) & 0x80;

        if (sign != 0) {
            sample = -sample;
        }

        if (sample > CLIP) {
            sample = CLIP;
        }

        sample += BIAS;

        int exponent = 7;

        for (int mask = 0x4000;
             exponent > 0 && (sample & mask) == 0;
             exponent--, mask >>= 1) {
        }

        int mantissa =
                (sample >> (exponent + 3))
                        & 0x0F;

        return (byte) ~(
                sign
                        | (exponent << 4)
                        | mantissa
        );
    }

    public static short decode(byte muLaw) {
        int value =
                (~muLaw)
                        & 0xFF;

        int sign =
                value
                        & 0x80;

        int exponent =
                (value >> 4)
                        & 0x07;

        int mantissa =
                value
                        & 0x0F;

        int sample =
                ((mantissa << 3)
                        + BIAS)
                        << exponent;

        sample -= BIAS;

        if (sign != 0) {
            sample = -sample;
        }

        return (short) Math.max(
                Short.MIN_VALUE,
                Math.min(
                        Short.MAX_VALUE,
                        sample
                )
        );
    }

    public static byte[] decodePcm16Le(
            byte[] muLaw,
            int repeat
    ) {
        if (muLaw == null
                || muLaw.length == 0) {
            return new byte[0];
        }

        int safeRepeat =
                Math.max(
                        1,
                        repeat
                );

        byte[] pcm =
                new byte[
                        muLaw.length
                                * safeRepeat
                                * 2
                ];

        int cursor = 0;

        for (byte encoded
                : muLaw) {
            short sample =
                    decode(
                            encoded
                    );

            for (int r = 0;
                 r < safeRepeat;
                 r++) {
                pcm[cursor++] =
                        (byte) (
                                sample
                                        & 0xFF
                        );

                pcm[cursor++] =
                        (byte) (
                                (
                                        sample
                                                >>> 8
                                )
                                        & 0xFF
                        );
            }
        }

        return pcm;
    }
}
