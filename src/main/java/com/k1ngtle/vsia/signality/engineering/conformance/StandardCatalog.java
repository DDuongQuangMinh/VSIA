package com.k1ngtle.vsia.signality.engineering.conformance;

import java.util.List;

public final class StandardCatalog {

    public static final StandardReference IEEE_802_11_2024 =
            new StandardReference(
                    "ieee-802.11-2024",
                    StandardsOrganization.IEEE,
                    "IEEE Std 802.11",
                    "2024",
                    "WLAN MAC and PHY base standard",
                    "IEEE Std 802.11-2024"
            );

    public static final StandardReference IEEE_802_11BE_2024 =
            new StandardReference(
                    "ieee-802.11be-2024",
                    StandardsOrganization.IEEE,
                    "IEEE Std 802.11be",
                    "2024",
                    "Extremely High Throughput amendment",
                    "IEEE Std 802.11be-2024"
            );

    public static final StandardReference GPP_38_211 =
            new StandardReference(
                    "3gpp-ts-38.211-j20",
                    StandardsOrganization.THREE_GPP,
                    "3GPP TS 38.211",
                    "j20 archive artifact",
                    "NR physical channels and modulation",
                    "38211-j20.zip"
            );

    public static final StandardReference GPP_38_331 =
            new StandardReference(
                    "3gpp-ts-38.331-j10",
                    StandardsOrganization.THREE_GPP,
                    "3GPP TS 38.331",
                    "j10 archive artifact",
                    "NR Radio Resource Control",
                    "38331-j10.zip"
            );

    public static final StandardReference GPP_38_323 =
            new StandardReference(
                    "3gpp-ts-38.323-j10",
                    StandardsOrganization.THREE_GPP,
                    "3GPP TS 38.323",
                    "j10 archive artifact",
                    "NR PDCP",
                    "38323-j10.zip"
            );

    public static final StandardReference GPP_24_501 =
            new StandardReference(
                    "3gpp-ts-24.501-j50",
                    StandardsOrganization.THREE_GPP,
                    "3GPP TS 24.501",
                    "j50 archive artifact",
                    "5GS NAS protocol",
                    "24501-j50.zip"
            );

    public static final StandardReference NIST_GCM =
            new StandardReference(
                    "nist-aes-gcm-kat",
                    StandardsOrganization.NIST,
                    "AES-GCM known-answer fixture",
                    "Phase 10 fixture",
                    "Cryptographic deterministic test",
                    "zero-key/zero-IV empty-plaintext GCM vector"
            );

    private static final List<StandardReference> REFERENCES =
            List.of(
                    IEEE_802_11_2024,
                    IEEE_802_11BE_2024,
                    GPP_38_211,
                    GPP_38_331,
                    GPP_38_323,
                    GPP_24_501,
                    NIST_GCM
            );

    private StandardCatalog() {
    }

    public static List<StandardReference> all() {
        return REFERENCES;
    }
}
