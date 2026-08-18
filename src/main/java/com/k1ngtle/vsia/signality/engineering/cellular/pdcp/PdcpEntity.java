package com.k1ngtle.vsia.signality.engineering.cellular.pdcp;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

public final class PdcpEntity {
    private int nextSequenceNumber;

    public PdcpPdu protect(
            byte[] plaintext,
            PdcpSecurityContext security
    ) {
        int sn = nextSequenceNumber++ & 0x3FFFF;

        byte[] iv = nonce(sn);

        try {
            Cipher cipher =
                    Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(
                            normalizeAesKey(security.cipherKey()),
                            "AES"
                    ),
                    new GCMParameterSpec(128, iv)
            );

            byte[] ciphertext =
                    cipher.doFinal(
                            plaintext == null
                                    ? new byte[0]
                                    : plaintext
                    );

            byte[] integrity =
                    integrityTag(
                            security.integrityKey(),
                            sn,
                            ciphertext
                    );

            return new PdcpPdu(
                    sn,
                    ciphertext,
                    integrity
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to protect PDCP PDU",
                    exception
            );
        }
    }

    public byte[] unprotect(
            PdcpPdu pdu,
            PdcpSecurityContext security
    ) {
        byte[] expected =
                integrityTag(
                        security.integrityKey(),
                        pdu.sequenceNumber(),
                        pdu.protectedPayload()
                );

        if (!MessageDigest.isEqual(
                expected,
                pdu.integrityTag()
        )) {
            throw new IllegalArgumentException(
                    "PDCP integrity verification failed"
            );
        }

        try {
            Cipher cipher =
                    Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(
                            normalizeAesKey(security.cipherKey()),
                            "AES"
                    ),
                    new GCMParameterSpec(
                            128,
                            nonce(pdu.sequenceNumber())
                    )
            );

            return cipher.doFinal(
                    pdu.protectedPayload()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to decipher PDCP PDU",
                    exception
            );
        }
    }

    private static byte[] integrityTag(
            byte[] key,
            int sequenceNumber,
            byte[] payload
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            key.length == 0
                                    ? new byte[32]
                                    : key,
                            "HmacSHA256"
                    )
            );

            mac.update(
                    ByteBuffer.allocate(4)
                            .putInt(sequenceNumber)
                            .array()
            );

            mac.update(payload);

            return Arrays.copyOf(
                    mac.doFinal(),
                    16
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate PDCP integrity tag",
                    exception
            );
        }
    }

    private static byte[] nonce(int sequenceNumber) {
        return ByteBuffer
                .allocate(12)
                .putInt(0)
                .putLong(
                        Integer.toUnsignedLong(
                                sequenceNumber
                        )
                )
                .array();
    }

    private static byte[] normalizeAesKey(byte[] key) {
        if (key.length == 16
                || key.length == 24
                || key.length == 32) {
            return key;
        }

        return Arrays.copyOf(
                key,
                16
        );
    }
}
