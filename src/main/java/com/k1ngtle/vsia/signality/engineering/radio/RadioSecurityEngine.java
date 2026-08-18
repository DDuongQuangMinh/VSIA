package com.k1ngtle.vsia.signality.engineering.radio;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public final class RadioSecurityEngine {
    private static final SecureRandom RANDOM =
            new SecureRandom();

    private RadioSecurityEngine() {
    }

    public static byte[] protect(
            byte[] key,
            byte[] plaintext
    ) {
        if (key == null || key.length == 0) {
            return plaintext == null
                    ? new byte[0]
                    : plaintext.clone();
        }

        try {
            byte[] nonce =
                    new byte[12];

            RANDOM.nextBytes(
                    nonce
            );

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(
                            normalizeKey(key),
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            nonce
                    )
            );

            byte[] ciphertext =
                    cipher.doFinal(
                            plaintext == null
                                    ? new byte[0]
                                    : plaintext
                    );

            byte[] result =
                    new byte[
                            nonce.length
                                    + ciphertext.length
                            ];

            System.arraycopy(
                    nonce,
                    0,
                    result,
                    0,
                    nonce.length
            );

            System.arraycopy(
                    ciphertext,
                    0,
                    result,
                    nonce.length,
                    ciphertext.length
            );

            return result;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to protect radio payload",
                    exception
            );
        }
    }

    public static byte[] unprotect(
            byte[] key,
            byte[] protectedBytes
    ) {
        if (key == null || key.length == 0) {
            return protectedBytes == null
                    ? new byte[0]
                    : protectedBytes.clone();
        }

        if (protectedBytes == null
                || protectedBytes.length < 13) {
            throw new IllegalArgumentException(
                    "Protected radio payload is too short"
            );
        }

        try {
            byte[] nonce =
                    Arrays.copyOfRange(
                            protectedBytes,
                            0,
                            12
                    );

            byte[] ciphertext =
                    Arrays.copyOfRange(
                            protectedBytes,
                            12,
                            protectedBytes.length
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(
                            normalizeKey(key),
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            nonce
                    )
            );

            return cipher.doFinal(
                    ciphertext
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to authenticate/decrypt radio payload",
                    exception
            );
        }
    }

    private static byte[] normalizeKey(
            byte[] key
    ) {
        if (key.length == 16
                || key.length == 24
                || key.length == 32) {
            return key.clone();
        }

        return Arrays.copyOf(
                key,
                16
        );
    }
}
