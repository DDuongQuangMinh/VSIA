package com.k1ngtle.vsia.signality.engineering.cellular.nas;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public final class FiveGAkaEngine {
    private static final SecureRandom RANDOM =
            new SecureRandom();

    private FiveGAkaEngine() {
    }

    public static byte[] randomChallenge() {
        byte[] challenge = new byte[16];
        RANDOM.nextBytes(challenge);
        return challenge;
    }

    public static byte[] calculateResponse(
            byte[] subscriberKey,
            String supi,
            byte[] challenge
    ) {
        return Arrays.copyOf(
                hmac(
                        subscriberKey,
                        concat(
                                "RES*|".getBytes(StandardCharsets.UTF_8),
                                supi.getBytes(StandardCharsets.UTF_8),
                                challenge
                        )
                ),
                16
        );
    }

    public static byte[] deriveAnchorKey(
            byte[] subscriberKey,
            String supi,
            byte[] challenge
    ) {
        return hmac(
                subscriberKey,
                concat(
                        "KAMF|".getBytes(StandardCharsets.UTF_8),
                        supi.getBytes(StandardCharsets.UTF_8),
                        challenge
                )
        );
    }

    public static byte[] deriveKey(
            byte[] anchorKey,
            String label
    ) {
        return hmac(
                anchorKey,
                label.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] hmac(
            byte[] key,
            byte[] data
    ) {
        try {
            Mac mac =
                    Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            key,
                            "HmacSHA256"
                    )
            );

            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate 5G-AKA simulation HMAC",
                    exception
            );
        }
    }

    private static byte[] concat(byte[]... values) {
        int length = 0;

        for (byte[] value : values) {
            length += value.length;
        }

        byte[] result = new byte[length];
        int cursor = 0;

        for (byte[] value : values) {
            System.arraycopy(
                    value,
                    0,
                    result,
                    cursor,
                    value.length
            );
            cursor += value.length;
        }

        return result;
    }
}
