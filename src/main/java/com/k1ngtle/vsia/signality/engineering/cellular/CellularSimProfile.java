package com.k1ngtle.vsia.signality.engineering.cellular;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public record CellularSimProfile(
        String iccid,
        String supi,
        String plmn,
        byte[] subscriberKey,
        String defaultDnn,
        int defaultFiveQi
) {
    public CellularSimProfile {
        iccid = normalizeDigits(iccid);
        supi = normalizeDigits(supi);
        plmn = normalizeDigits(plmn);
        subscriberKey = subscriberKey == null
                ? new byte[0]
                : subscriberKey.clone();
        defaultDnn = defaultDnn == null || defaultDnn.isBlank()
                ? "internet"
                : defaultDnn.trim();
        defaultFiveQi = Math.max(1, defaultFiveQi);

        if (plmn.length() < 5 || plmn.length() > 6) {
            throw new IllegalArgumentException(
                    "PLMN must contain 5 or 6 digits"
            );
        }

        if (supi.length() < plmn.length() + 1) {
            throw new IllegalArgumentException(
                    "SUPI must include PLMN + subscriber digits"
            );
        }

        if (subscriberKey.length < 16) {
            throw new IllegalArgumentException(
                    "subscriber key must be at least 16 bytes"
            );
        }
    }

    @Override
    public byte[] subscriberKey() {
        return subscriberKey.clone();
    }

    public static CellularSimProfile fromSecret(
            String plmn,
            String subscriberDigits,
            String secret
    ) {
        String normalizedPlmn =
                normalizeDigits(plmn);

        String subscriber =
                normalizeDigits(subscriberDigits);

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] key =
                    digest.digest(
                            (
                                    normalizedPlmn
                                            + ":"
                                            + subscriber
                                            + ":"
                                            + (
                                            secret == null
                                                    ? ""
                                                    : secret
                                    )
                            )
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            String iccid =
                    "89"
                            + normalizedPlmn
                            + decimalDigest(
                            subscriber
                                    + ":"
                                    + (
                                    secret == null
                                            ? ""
                                            : secret
                            ),
                            Math.max(
                                    1,
                                    20
                                            - 2
                                            - normalizedPlmn.length()
                            )
                    );

            if (iccid.length() > 20) {
                iccid =
                        iccid.substring(
                                0,
                                20
                        );
            }

            return new CellularSimProfile(
                    iccid,
                    normalizedPlmn + subscriber,
                    normalizedPlmn,
                    key,
                    "internet",
                    9
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to derive deterministic SIM",
                    exception
            );
        }
    }

    public static CellularSimProfile random(
            String plmn,
            String subscriberDigits
    ) {
        String normalizedPlmn =
                normalizeDigits(plmn);

        String subscriber =
                normalizeDigits(subscriberDigits);

        SecureRandom random =
                new SecureRandom();

        byte[] key =
                new byte[32];

        random.nextBytes(key);

        String entropy =
                Long.toUnsignedString(
                        random.nextLong()
                );

        String iccid =
                "89"
                        + normalizedPlmn
                        + decimalDigest(
                        subscriber + entropy,
                        Math.max(
                                1,
                                20 - 2 - normalizedPlmn.length()
                        )
                );

        if (iccid.length() > 20) {
            iccid =
                    iccid.substring(
                            0,
                            20
                    );
        }

        return new CellularSimProfile(
                iccid,
                normalizedPlmn + subscriber,
                normalizedPlmn,
                key,
                "internet",
                9
        );
    }

    public String fingerprint() {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            digest.update(
                    supi.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            digest.update(
                    subscriberKey
            );

            return HexFormat.of()
                    .formatHex(
                            digest.digest()
                    )
                    .substring(
                            0,
                            16
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fingerprint SIM profile",
                    exception
            );
        }
    }

    private static String normalizeDigits(String value) {
        return value == null
                ? ""
                : value.replaceAll(
                        "\\D",
                        ""
                );
    }

    private static String decimalDigest(
            String seed,
            int length
    ) {
        StringBuilder out =
                new StringBuilder();

        try {
            byte[] digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    ).digest(
                            seed.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            int index = 0;

            while (out.length() < length) {
                int value =
                        digest[
                                index
                                        % digest.length
                                ]
                                & 0xFF;

                out.append(
                        value % 10
                );

                index++;
            }

            return out.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    exception
            );
        }
    }
}
