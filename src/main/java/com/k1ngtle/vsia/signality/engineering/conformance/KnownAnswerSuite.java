package com.k1ngtle.vsia.signality.engineering.conformance;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32;

public final class KnownAnswerSuite {

    private KnownAnswerSuite() {
    }

    public static List<KnownAnswerResult> runAll() {
        List<KnownAnswerResult> results =
                new ArrayList<>();

        results.add(
                crc32Vector()
        );

        results.add(
                sha256Vector()
        );

        results.add(
                aesGcmEmptyVector()
        );

        return List.copyOf(
                results
        );
    }

    private static KnownAnswerResult crc32Vector() {
        byte[] input =
                "123456789"
                        .getBytes(
                                StandardCharsets.US_ASCII
                        );

        CRC32 crc =
                new CRC32();

        crc.update(
                input
        );

        String actual =
                String.format(
                        "%08X",
                        crc.getValue()
                );

        String expected =
                "CBF43926";

        return new KnownAnswerResult(
                "crc32-123456789",
                expected.equals(
                        actual
                ),
                expected,
                actual,
                "Standard CRC-32 check value. This validates the CRC primitive, not 802.11 FCS byte ordering."
        );
    }

    private static KnownAnswerResult sha256Vector() {
        try {
            byte[] digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            )
                            .digest(
                                    "abc".getBytes(
                                            StandardCharsets.US_ASCII
                                    )
                            );

            String actual =
                    HexFormat.of()
                            .formatHex(
                                    digest
                            );

            String expected =
                    "ba7816bf8f01cfea414140de5dae2223"
                            + "b00361a396177a9cb410ff61f20015ad";

            return new KnownAnswerResult(
                    "sha256-abc",
                    expected.equals(
                            actual
                    ),
                    expected,
                    actual,
                    "SHA-256 known-answer test."
            );
        } catch (Exception exception) {
            return new KnownAnswerResult(
                    "sha256-abc",
                    false,
                    "known digest",
                    exception.toString(),
                    "SHA-256 unavailable."
            );
        }
    }

    private static KnownAnswerResult aesGcmEmptyVector() {
        try {
            byte[] key =
                    new byte[16];

            byte[] iv =
                    new byte[12];

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

            byte[] output =
                    cipher.doFinal(
                            new byte[0]
                    );

            String actual =
                    HexFormat.of()
                            .formatHex(
                                    output
                            );

            String expected =
                    "58e2fccefa7e3061367f1d57a4e7455a";

            return new KnownAnswerResult(
                    "aes-gcm-zero-empty",
                    expected.equals(
                            actual
                    ),
                    expected,
                    actual,
                    "Deterministic AES-GCM fixture used to validate the Java crypto primitive."
            );
        } catch (Exception exception) {
            return new KnownAnswerResult(
                    "aes-gcm-zero-empty",
                    false,
                    "58e2fccefa7e3061367f1d57a4e7455a",
                    exception.toString(),
                    "AES-GCM unavailable."
            );
        }
    }
}
