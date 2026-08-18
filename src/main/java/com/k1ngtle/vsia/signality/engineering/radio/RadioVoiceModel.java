package com.k1ngtle.vsia.signality.engineering.radio;

import java.util.Random;

public final class RadioVoiceModel {
    private RadioVoiceModel() {
    }

    public static byte[] degrade(
            byte[] encodedAudio,
            RadioEmission emission,
            RadioLinkQuality quality,
            Random random
    ) {
        if (encodedAudio == null) {
            return new byte[0];
        }

        if (!quality.squelchOpen()) {
            return new byte[0];
        }

        if (emission == RadioEmission.DIGITAL
                && quality.intelligibility() < 0.35) {
            return new byte[0];
        }

        byte[] result =
                encodedAudio.clone();

        double factor =
                switch (emission) {
                    case AM -> 0.12;
                    case FM -> 0.07;
                    case DIGITAL -> 0.015;
                };

        double corruptionProbability =
                Math.min(
                        0.30,
                        quality.staticLevel()
                                * factor
                );

        for (int i = 0;
             i < result.length;
             i++) {
            if (random.nextDouble()
                    < corruptionProbability) {
                int bit =
                        1 << random.nextInt(8);

                result[i] =
                        (byte) (
                                result[i] ^ bit
                        );
            }
        }

        return result;
    }
}
