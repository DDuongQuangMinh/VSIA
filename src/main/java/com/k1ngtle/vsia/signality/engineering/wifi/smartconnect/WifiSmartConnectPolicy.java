package com.k1ngtle.vsia.signality.engineering.wifi.smartconnect;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class WifiSmartConnectPolicy {
    public static final WifiSmartConnectPolicy DEFAULT =
            new WifiSmartConnectPolicy(
                    18.0D,
                    6.0D,
                    3.0D
            );

    private final double fiveGhzMinimumSnrDb;
    private final double fiveGhzPreferenceBonusDb;
    private final double stickinessDb;

    public WifiSmartConnectPolicy(
            double fiveGhzMinimumSnrDb,
            double fiveGhzPreferenceBonusDb,
            double stickinessDb
    ) {
        this.fiveGhzMinimumSnrDb =
                fiveGhzMinimumSnrDb;

        this.fiveGhzPreferenceBonusDb =
                fiveGhzPreferenceBonusDb;

        this.stickinessDb =
                Math.max(
                        0.0D,
                        stickinessDb
                );
    }

    public WifiSmartConnectDecision select(
            String ssid,
            String currentBssid,
            Collection<WifiNetworkRecord> networks,
            ToDoubleFunction<WifiNetworkRecord> snrProvider
    ) {
        if (ssid == null
                || ssid.isBlank()
                || networks == null
                || networks.isEmpty()
                || snrProvider == null) {
            return WifiSmartConnectDecision.unavailable(
                    "No Smart Connect candidates"
            );
        }

        Candidate best =
                networks.stream()
                        .filter(
                                value ->
                                        value != null
                                                && ssid.equals(
                                                value.ssid()
                                        )
                                                && WifiBandUtil.mb1DualBandEligible(
                                                value.frequencyHz()
                                        )
                        )
                        .map(
                                value ->
                                        candidate(
                                                value,
                                                snrProvider.applyAsDouble(
                                                        value
                                                )
                                        )
                        )
                        .filter(
                                value ->
                                        Double.isFinite(
                                                value.snrDb()
                                        )
                        )
                        .max(
                                Comparator.comparingDouble(
                                        Candidate::score
                                )
                        )
                        .orElse(
                                null
                        );

        if (best == null) {
            return WifiSmartConnectDecision.unavailable(
                    "No finite-SNR 2.4/5 GHz candidate for "
                            + ssid
            );
        }

        Candidate current =
                networks.stream()
                        .filter(
                                value ->
                                        value != null
                                                && currentBssid != null
                                                && !currentBssid.isBlank()
                                                && normalize(
                                                value.bssid()
                                        ).equals(
                                                normalize(
                                                        currentBssid
                                                )
                                        )
                                                && ssid.equals(
                                                value.ssid()
                                        )
                                                && WifiBandUtil.mb1DualBandEligible(
                                                value.frequencyHz()
                                        )
                        )
                        .map(
                                value ->
                                        candidate(
                                                value,
                                                snrProvider.applyAsDouble(
                                                        value
                                                )
                                        )
                        )
                        .filter(
                                value ->
                                        Double.isFinite(
                                                value.snrDb()
                                        )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (current != null
                && current.network() != best.network()
                && best.score()
                < current.score()
                + stickinessDb) {
            return decision(
                    current,
                    "Keep current BSSID: challenger improvement "
                            + format(
                            best.score()
                            - current.score()
                    )
                            + " dB is below Smart Connect stickiness "
                            + format(
                            stickinessDb
                    )
                            + " dB"
            );
        }

        String reason =
                best.band() == WifiBand.FIVE_GHZ
                        ? "5 GHz selected with healthy SNR and Smart Connect band bonus"
                        : "2.4 GHz selected because its effective link score is stronger/more robust";

        return decision(
                best,
                reason
        );
    }

    public double fiveGhzMinimumSnrDb() {
        return fiveGhzMinimumSnrDb;
    }

    public double fiveGhzPreferenceBonusDb() {
        return fiveGhzPreferenceBonusDb;
    }

    public double stickinessDb() {
        return stickinessDb;
    }

    private Candidate candidate(
            WifiNetworkRecord network,
            double snrDb
    ) {
        WifiBand band =
                WifiBandUtil.bandForFrequency(
                        network.frequencyHz()
                );

        double score =
                snrDb;

        if (band == WifiBand.FIVE_GHZ) {
            if (snrDb >= fiveGhzMinimumSnrDb) {
                score +=
                        fiveGhzPreferenceBonusDb;
            } else {
                score -=
                        fiveGhzPreferenceBonusDb;
            }
        }

        return new Candidate(
                network,
                band,
                snrDb,
                score
        );
    }

    private WifiSmartConnectDecision decision(
            Candidate candidate,
            String reason
    ) {
        return new WifiSmartConnectDecision(
                true,
                candidate.network(),
                candidate.band(),
                candidate.snrDb(),
                candidate.score(),
                reason
        );
    }

    private String normalize(
            String bssid
    ) {
        if (bssid == null) {
            return "";
        }

        return bssid.replace(
                ":",
                ""
        ).replace(
                "-",
                ""
        ).toLowerCase(
                Locale.ROOT
        );
    }

    private String format(
            double value
    ) {
        return String.format(
                Locale.ROOT,
                "%.1f",
                value
        );
    }

    private record Candidate(
            WifiNetworkRecord network,
            WifiBand band,
            double snrDb,
            double score
    ) {
    }
}
