package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WifiOfdmaScheduler {
    private WifiOfdmaScheduler() {
    }

    public static List<WifiOfdmaAllocation> allocate(
            WifiChannelWidth width,
            List<WifiOfdmaUserDemand> demands
    ) {
        if (demands == null
                || demands.isEmpty()) {
            return List.of();
        }

        WifiResourceUnit full =
                WifiPhyRateCalculator.fullBandRu(
                        width
                );

        List<WifiOfdmaUserDemand> ordered =
                new ArrayList<>(
                        demands
                );

        ordered.sort(
                Comparator
                        .comparingDouble(
                                WifiOfdmaUserDemand::weight
                        )
                        .reversed()
                        .thenComparing(
                                WifiOfdmaUserDemand::stationId
                        )
        );

        int users =
                ordered.size();

        WifiResourceUnit ru =
                chooseRu(
                        full.tones(),
                        users
                );

        int maxAllocations =
                Math.max(
                        1,
                        full.tones()
                                / ru.tones()
                );

        int count =
                Math.min(
                        users,
                        maxAllocations
                );

        List<WifiOfdmaAllocation> result =
                new ArrayList<>(
                        count
                );

        int cursor =
                0;

        for (int i = 0;
             i < count;
             i++) {
            WifiOfdmaUserDemand demand =
                    ordered.get(
                            i
                    );

            result.add(
                    new WifiOfdmaAllocation(
                            demand.stationId(),
                            ru,
                            cursor,
                            ru.tones(),
                            demand.weight()
                    )
            );

            cursor +=
                    ru.tones();
        }

        return List.copyOf(
                result
        );
    }

    private static WifiResourceUnit chooseRu(
            int fullTones,
            int users
    ) {
        int target =
                Math.max(
                        26,
                        fullTones
                                / Math.max(
                                1,
                                users
                        )
                );

        WifiResourceUnit best =
                WifiResourceUnit.RU_26;

        for (WifiResourceUnit candidate
                : WifiResourceUnit.values()) {
            if (candidate.tones()
                    <= target
                    && candidate.tones()
                    <= fullTones) {
                best =
                        candidate;
            }
        }

        return best;
    }
}
