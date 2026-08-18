package com.k1ngtle.vsia.signality.engineering.wifi;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public final class WifiSecurityEngine {
    private static final SecureRandom RANDOM =
            new SecureRandom();

    private WifiSecurityEngine() {
    }

    public static byte[] derivePmk(
            String passphrase,
            String ssid
    ) {
        try {
            PBEKeySpec spec =
                    new PBEKeySpec(
                            passphrase.toCharArray(),
                            ssid.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            4096,
                            256
                    );

            return SecretKeyFactory
                    .getInstance(
                            "PBKDF2WithHmacSHA1"
                    )
                    .generateSecret(
                            spec
                    )
                    .getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to derive Wi-Fi PMK",
                    exception
            );
        }
    }

    public static byte[] randomNonce() {
        byte[] nonce =
                new byte[32];

        RANDOM.nextBytes(
                nonce
        );

        return nonce;
    }

    public static byte[] derivePtk(
            byte[] pmk,
            String apMac,
            String stationMac,
            byte[] anonce,
            byte[] snonce
    ) {
        byte[] macA =
                normalizedMacBytes(
                        apMac
                );

        byte[] macB =
                normalizedMacBytes(
                        stationMac
                );

        byte[] firstMac =
                compareUnsigned(
                        macA,
                        macB
                ) <= 0
                        ? macA
                        : macB;

        byte[] secondMac =
                firstMac == macA
                        ? macB
                        : macA;

        byte[] firstNonce =
                compareUnsigned(
                        anonce,
                        snonce
                ) <= 0
                        ? anonce
                        : snonce;

        byte[] secondNonce =
                firstNonce == anonce
                        ? snonce
                        : anonce;

        ByteBuffer context =
                ByteBuffer.allocate(
                        firstMac.length
                                + secondMac.length
                                + firstNonce.length
                                + secondNonce.length
                );

        context.put(firstMac);
        context.put(secondMac);
        context.put(firstNonce);
        context.put(secondNonce);

        return hmacSha256(
                pmk,
                context.array()
        );
    }

    public static byte[] mic(
            byte[] ptk,
            byte[] message
    ) {
        return Arrays.copyOf(
                hmacSha256(
                        ptk,
                        message
                ),
                16
        );
    }

    public static boolean verifyMic(
            byte[] ptk,
            byte[] message,
            byte[] expectedMic
    ) {
        return constantTimeEquals(
                mic(
                        ptk,
                        message
                ),
                expectedMic
        );
    }

    public static byte[] protect(
            byte[] ptk,
            byte[] plaintext
    ) {
        try {
            byte[] key =
                    Arrays.copyOf(
                            ptk,
                            16
                    );

            byte[] iv =
                    new byte[12];

            RANDOM.nextBytes(
                    iv
            );

            Cipher cipher =
                    Cipher.getInstance(
                            "AES/GCM/NoPadding"
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(
                            key,
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(
                            plaintext
                    );

            ByteBuffer result =
                    ByteBuffer.allocate(
                            iv.length
                                    + encrypted.length
                    );

            result.put(iv);
            result.put(encrypted);

            return result.array();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to protect Wi-Fi payload",
                    exception
            );
        }
    }

    public static byte[] unprotect(
            byte[] ptk,
            byte[] protectedBytes
    ) {
        try {
            if (protectedBytes.length < 13) {
                throw new IllegalArgumentException(
                        "Protected Wi-Fi payload is too short"
                );
            }

            byte[] key =
                    Arrays.copyOf(
                            ptk,
                            16
                    );

            byte[] iv =
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
                            key,
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            iv
                    )
            );

            return cipher.doFinal(
                    ciphertext
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to decrypt/authenticate Wi-Fi payload",
                    exception
            );
        }
    }

    private static byte[] hmacSha256(
            byte[] key,
            byte[] data
    ) {
        try {
            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            mac.init(
                    new SecretKeySpec(
                            key,
                            "HmacSHA256"
                    )
            );

            return mac.doFinal(
                    data
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate HMAC",
                    exception
            );
        }
    }

    private static byte[] normalizedMacBytes(
            String mac
    ) {
        String value =
                mac.replace(":", "")
                        .replace("-", "");

        if (value.length() != 12) {
            throw new IllegalArgumentException(
                    "Invalid MAC address "
                            + mac
            );
        }

        byte[] result =
                new byte[6];

        for (int i = 0;
             i < 6;
             i++) {
            result[i] =
                    (byte) Integer.parseInt(
                            value.substring(
                                    i * 2,
                                    i * 2 + 2
                            ),
                            16
                    );
        }

        return result;
    }

    private static int compareUnsigned(
            byte[] a,
            byte[] b
    ) {
        int length =
                Math.min(
                        a.length,
                        b.length
                );

        for (int i = 0;
             i < length;
             i++) {
            int av =
                    a[i] & 0xFF;

            int bv =
                    b[i] & 0xFF;

            if (av != bv) {
                return Integer.compare(
                        av,
                        bv
                );
            }
        }

        return Integer.compare(
                a.length,
                b.length
        );
    }

    private static boolean constantTimeEquals(
            byte[] a,
            byte[] b
    ) {
        if (a == null || b == null) {
            return false;
        }

        int diff =
                a.length ^ b.length;

        int max =
                Math.max(
                        a.length,
                        b.length
                );

        for (int i = 0;
             i < max;
             i++) {
            int av =
                    i < a.length
                            ? a[i] & 0xFF
                            : 0;

            int bv =
                    i < b.length
                            ? b[i] & 0xFF
                            : 0;

            diff |= av ^ bv;
        }

        return diff == 0;
    }
}
