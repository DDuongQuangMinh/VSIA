package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class ProportionalFairScheduler {
    public List<ResourceBlockAllocation> schedule(
            Collection<UeContext> contexts,
            int totalResourceBlocks
    ) {
        if (totalResourceBlocks <= 0 || contexts.isEmpty()) {
            return List.of();
        }

        List<UeContext> ordered = new ArrayList<>(contexts);
        ordered.sort(Comparator.comparingDouble(this::priority).reversed());

        List<ResourceBlockAllocation> result = new ArrayList<>();
        int cursor = 0;
        int remaining = totalResourceBlocks;

        for (UeContext context : ordered) {
            if (remaining <= 0) {
                break;
            }

            int count = Math.min(
                    context.requestedResourceBlocks(),
                    remaining
            );

            result.add(new ResourceBlockAllocation(
                    context.ueId(),
                    cursor,
                    count,
                    context.cqi()
            ));

            cursor += count;
            remaining -= count;
        }

        return result;
    }

    private double priority(UeContext context) {
        return Math.max(1.0, context.cqi())
                / context.averageDeliveredBits();
    }
}
