package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.List;
import java.util.Map;

public final class CellularProtocolConceptCatalog {
    private static final Map<CellularGeneration, ProtocolConcept> CONCEPTS =
            Map.of(
                    CellularGeneration.G1_ANALOG,
                    new ProtocolConcept(
                            "FDMA analog cellular",
                            "Forward control channel / reverse control channel",
                            "Registration + page + dedicated analog voice channel",
                            "Circuit-switched voice",
                            List.of(
                                    "scan control channels",
                                    "camp on strongest system",
                                    "registration",
                                    "page/origination",
                                    "assign FDMA voice channel"
                            )
                    ),
                    CellularGeneration.G2_GSM,
                    new ProtocolConcept(
                            "GSM",
                            "FCCH/SCH/BCCH + RACH/AGCH/SDCCH",
                            "GSM authentication + location update",
                            "Circuit voice + SMS + GPRS-style packet concepts",
                            List.of(
                                    "frequency correction",
                                    "synchronization",
                                    "read BCCH",
                                    "RACH access",
                                    "SDCCH signalling",
                                    "authentication/location update",
                                    "assign TCH or packet resources"
                            )
                    ),
                    CellularGeneration.G3_UMTS,
                    new ProtocolConcept(
                            "UMTS / W-CDMA",
                            "P-SCH/S-SCH/CPICH + PRACH",
                            "RRC connection + AKA + packet-domain registration",
                            "Circuit voice + packet data",
                            List.of(
                                    "cell search",
                                    "scrambling-code acquisition",
                                    "PRACH access",
                                    "RRC connection",
                                    "AKA",
                                    "PDP-context-style packet session"
                            )
                    ),
                    CellularGeneration.G4_LTE,
                    new ProtocolConcept(
                            "LTE / E-UTRA",
                            "PSS/SSS/PBCH + PRACH",
                            "RRC + EPS AKA + attach",
                            "All-IP packet service / EPS bearers",
                            List.of(
                                    "PSS/SSS cell search",
                                    "read MIB/SIB",
                                    "PRACH",
                                    "RRC connection",
                                    "EPS AKA/security",
                                    "default EPS bearer",
                                    "measurement reports/handover"
                            )
                    ),
                    CellularGeneration.G5_NR,
                    new ProtocolConcept(
                            "5G NR",
                            "SSB/PBCH + PRACH",
                            "RRC + 5G-AKA + registration",
                            "PDU sessions with 5QI QoS",
                            List.of(
                                    "SSB beam/cell search",
                                    "read system information",
                                    "PRACH",
                                    "RRC setup",
                                    "5G-AKA/security",
                                    "registration",
                                    "PDU session",
                                    "measurement reports/handover"
                            )
                    )
            );

    private CellularProtocolConceptCatalog() {
    }

    public static ProtocolConcept forGeneration(
            CellularGeneration generation
    ) {
        return CONCEPTS.get(
                generation == null
                        ? CellularGeneration.G5_NR
                        : generation
        );
    }

    public record ProtocolConcept(
            String radioAccess,
            String synchronizationAndAccess,
            String mobilityManagement,
            String serviceModel,
            List<String> simplifiedAttachSequence
    ) {
        public ProtocolConcept {
            radioAccess = radioAccess == null ? "" : radioAccess;
            synchronizationAndAccess = synchronizationAndAccess == null
                    ? ""
                    : synchronizationAndAccess;
            mobilityManagement = mobilityManagement == null
                    ? ""
                    : mobilityManagement;
            serviceModel = serviceModel == null
                    ? ""
                    : serviceModel;
            simplifiedAttachSequence = simplifiedAttachSequence == null
                    ? List.of()
                    : List.copyOf(simplifiedAttachSequence);
        }
    }
}
