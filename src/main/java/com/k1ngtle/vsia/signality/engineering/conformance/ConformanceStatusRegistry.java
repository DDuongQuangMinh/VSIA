package com.k1ngtle.vsia.signality.engineering.conformance;

import java.util.List;

public final class ConformanceStatusRegistry {

    private static final List<ComponentConformanceStatus> STATUS =
            List.of(
                    new ComponentConformanceStatus(
                            "RF equations",
                            ImplementationLevel.CONFORMANCE_PREP,
                            "internal-engineering",
                            "Deterministic equation tests exist; not a protocol conformance claim."
                    ),
                    new ComponentConformanceStatus(
                            "Wi-Fi MAC",
                            ImplementationLevel.SIMULATION,
                            "ieee-802.11-2024",
                            "Management/data framing and timers remain simplified."
                    ),
                    new ComponentConformanceStatus(
                            "Wi-Fi EHT model",
                            ImplementationLevel.SIMULATION,
                            "ieee-802.11be-2024",
                            "MCS/SNR mapping is gameplay calibration, not normative receiver sensitivity."
                    ),
                    new ComponentConformanceStatus(
                            "NR PHY/RAN",
                            ImplementationLevel.SIMULATION,
                            "3gpp-ts-38.211-j20",
                            "RAN procedures use protocol concepts without exact PHY bit mapping."
                    ),
                    new ComponentConformanceStatus(
                            "NR RRC",
                            ImplementationLevel.SIMULATION,
                            "3gpp-ts-38.331-j10",
                            "State flow is simplified and does not use ASN.1 wire encoding."
                    ),
                    new ComponentConformanceStatus(
                            "NR PDCP",
                            ImplementationLevel.SIMULATION,
                            "3gpp-ts-38.323-j10",
                            "Uses simulation security/framing rather than exact TS 38.323 formats."
                    ),
                    new ComponentConformanceStatus(
                            "5GS NAS",
                            ImplementationLevel.SIMULATION,
                            "3gpp-ts-24.501-j50",
                            "NAS messages are conceptual NBT structures, not TS 24.501 byte encoding."
                    ),
                    new ComponentConformanceStatus(
                            "Radio stack",
                            ImplementationLevel.SIMULATION,
                            "internal-radio",
                            "AM/FM/digital, repeater and mesh behavior are engineering abstractions."
                    ),
                    new ComponentConformanceStatus(
                            "Protocol VM",
                            ImplementationLevel.CONFORMANCE_PREP,
                            "internal-vm-v1",
                            "Deterministic bounded execution supports reproducible protocol test vectors."
                    )
            );

    private ConformanceStatusRegistry() {
    }

    public static List<ComponentConformanceStatus> all() {
        return STATUS;
    }
}
