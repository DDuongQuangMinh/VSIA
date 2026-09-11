package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class CellularQosScheduler {

    public List<ResourceBlockAllocation> schedule(
            Collection<UeContext> contexts,
            Function<UUID, PduSession> sessionLookup,
            int totalResourceBlocks
    ) {
        if (contexts == null
                || contexts.isEmpty()
                || totalResourceBlocks <= 0) {
            return List.of();
        }

        List<UeContext> active = contexts.stream()
                .filter(context -> context != null)
                .sorted(
                        Comparator.comparing(
                                context -> context.ueId().toString()
                        )
                )
                .toList();

        if (active.isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> scores =
                new LinkedHashMap<>();

        double scoreSum = 0.0;

        for (UeContext context : active) {
            PduSession session = sessionLookup == null
                    ? null
                    : sessionLookup.apply(context.ueId());

            int fiveQi = session == null
                    ? 9
                    : session.fiveQi();

            CellularQosClass qos =
                    CellularQosCatalog.forFiveQi(fiveQi);

            double instantaneousRate =
                    spectralEfficiencyFromCqi(
                            context.cqi()
                    );

            double proportionalFair =
                    instantaneousRate
                            / Math.max(
                            1.0,
                            context.averageDeliveredBits()
                    );

            double demand =
                    Math.max(
                            1.0,
                            context.requestedResourceBlocks()
                    );

            double score =
                    Math.max(
                            1.0e-9,
                            proportionalFair
                                    * qos.schedulerWeight()
                                    * Math.sqrt(demand)
                    );

            scores.put(
                    context.ueId(),
                    score
            );

            scoreSum += score;
        }

        Map<UUID, Integer> assigned =
                new LinkedHashMap<>();

        int remaining =
                totalResourceBlocks;

        for (UeContext context : active) {
            if (remaining <= 0) {
                break;
            }

            int allocation =
                    Math.max(
                            1,
                            (int) Math.floor(
                                    totalResourceBlocks
                                            * scores.get(
                                            context.ueId()
                                    )
                                            / scoreSum
                            )
                    );

            allocation =
                    Math.min(
                            allocation,
                            Math.min(
                                    context.requestedResourceBlocks(),
                                    remaining
                            )
                    );

            assigned.put(
                    context.ueId(),
                    allocation
            );

            remaining -= allocation;
        }

        while (remaining > 0) {
            UeContext best = active.stream()
                    .filter(context ->
                            assigned.getOrDefault(
                                    context.ueId(),
                                    0
                            )
                                    < context.requestedResourceBlocks())
                    .max(
                            Comparator.comparingDouble(
                                    context ->
                                            scores.get(
                                                    context.ueId()
                                            )
                                                    / (
                                                    1.0
                                                            + assigned.getOrDefault(
                                                            context.ueId(),
                                                            0
                                                    )
                                            )
                            )
                    )
                    .orElse(null);

            if (best == null) {
                break;
            }

            assigned.merge(
                    best.ueId(),
                    1,
                    Integer::sum
            );

            remaining--;
        }

        List<ResourceBlockAllocation> output =
                new ArrayList<>();

        int start = 0;

        for (UeContext context : active) {
            int count =
                    assigned.getOrDefault(
                            context.ueId(),
                            0
                    );

            if (count <= 0) {
                continue;
            }

            output.add(
                    new ResourceBlockAllocation(
                            context.ueId(),
                            start,
                            count,
                            context.cqi()
                    )
            );

            start += count;
        }

        return List.copyOf(output);
    }

    public static double spectralEfficiencyFromCqi(int cqi) {
        double[] table = {
                0.1523,
                0.2344,
                0.3770,
                0.6016,
                0.8770,
                1.1758,
                1.4766,
                1.9141,
                2.4063,
                2.7305,
                3.3223,
                3.9023,
                4.5234,
                5.1152,
                5.5547
        };

        int index =
                Math.max(
                        1,
                        Math.min(
                                15,
                                cqi
                        )
                )
                        - 1;

        return table[index];
    }
}
