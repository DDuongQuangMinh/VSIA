package com.k1ngtle.vsia.signality.engineering.wifi;

import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

import java.util.List;

public final class WifiMcsTable {
    private static final List<WifiMcs> TABLE = List.of(
            new WifiMcs(0, Modulation.BPSK,    1.0 / 2.0, -5.0),
            new WifiMcs(1, Modulation.QPSK,    1.0 / 2.0,  0.0),
            new WifiMcs(2, Modulation.QPSK,    3.0 / 4.0,  3.0),
            new WifiMcs(3, Modulation.QAM16,   1.0 / 2.0,  6.0),
            new WifiMcs(4, Modulation.QAM16,   3.0 / 4.0,  9.0),
            new WifiMcs(5, Modulation.QAM64,   2.0 / 3.0, 12.0),
            new WifiMcs(6, Modulation.QAM64,   3.0 / 4.0, 15.0),
            new WifiMcs(7, Modulation.QAM64,   5.0 / 6.0, 18.0),
            new WifiMcs(8, Modulation.QAM256,  3.0 / 4.0, 21.0),
            new WifiMcs(9, Modulation.QAM256,  5.0 / 6.0, 24.0),
            new WifiMcs(10, Modulation.QAM1024, 3.0 / 4.0, 27.0),
            new WifiMcs(11, Modulation.QAM1024, 5.0 / 6.0, 30.0),
            new WifiMcs(12, Modulation.QAM4096, 3.0 / 4.0, 34.0),
            new WifiMcs(13, Modulation.QAM4096, 5.0 / 6.0, 37.0)
    );

    private WifiMcsTable() {
    }

    public static WifiMcs byIndex(int index) {
        if (index < 0 || index >= TABLE.size()) {
            return TABLE.get(0);
        }

        return TABLE.get(index);
    }

    public static WifiMcs select(String protocol, double snrDb) {
        int maximum = maximumIndex(protocol);

        WifiMcs selected = TABLE.get(0);

        for (WifiMcs candidate : TABLE) {
            if (candidate.index() > maximum) {
                break;
            }

            if (snrDb >= candidate.minimumSnrDb()) {
                selected = candidate;
            }
        }

        return selected;
    }

    public static int maximumIndex(String protocol) {
        String normalized =
                protocol == null
                        ? ""
                        : protocol.toLowerCase();

        if (normalized.contains("80211be")) {
            return 13;
        }

        if (normalized.contains("80211ax")) {
            return 11;
        }

        if (normalized.contains("80211ac")) {
            return 9;
        }

        if (normalized.contains("80211n")) {
            return 7;
        }

        return 7;
    }
}
