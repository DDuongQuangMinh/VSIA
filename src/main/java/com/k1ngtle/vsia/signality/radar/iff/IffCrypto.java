package com.k1ngtle.vsia.signality.radar.iff;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class IffCrypto {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private IffCrypto() {
    }

    public static byte[] response(
            String key,
            long challengeNonce,
            int modeSAddress,
            int squawkCode
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));

            ByteBuffer buffer = ByteBuffer.allocate(
                    Long.BYTES + Integer.BYTES + Integer.BYTES
            );
            buffer.putLong(challengeNonce);
            buffer.putInt(modeSAddress);
            buffer.putInt(squawkCode);
            return mac.doFinal(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate IFF challenge response",
                    exception
            );
        }
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
