package com.k1ngtle.vsia.signality.engineering.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RfTransmissionRegistry {
    private static final Map<UUID, ActiveRfTransmission> TRANSMISSIONS =
            new LinkedHashMap<>();

    private RfTransmissionRegistry() {
    }

    public static synchronized void register(
            ActiveRfTransmission transmission
    ) {
        cleanup(
                transmission.startTick()
        );

        TRANSMISSIONS.put(
                transmission.transmissionId(),
                transmission
        );
    }

    public static synchronized ActiveRfTransmission get(
            UUID transmissionId,
            long currentTick
    ) {
        cleanup(
                currentTick
        );

        return TRANSMISSIONS.get(
                transmissionId
        );
    }

    public static synchronized Collection<ActiveRfTransmission> activeInDimension(
            String dimensionId,
            long currentTick
    ) {
        cleanup(
                currentTick
        );

        List<ActiveRfTransmission> result =
                new ArrayList<>();

        for (ActiveRfTransmission transmission
                : TRANSMISSIONS.values()) {
            if (transmission.dimensionId()
                    .equals(
                            dimensionId
                    )
                    && transmission.activeAt(
                    currentTick
            )) {
                result.add(
                        transmission
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public static synchronized void clear() {
        TRANSMISSIONS.clear();
    }

    private static void cleanup(
            long currentTick
    ) {
        Iterator<ActiveRfTransmission> iterator =
                TRANSMISSIONS
                        .values()
                        .iterator();

        while (iterator.hasNext()) {
            ActiveRfTransmission transmission =
                    iterator.next();

            if (transmission.endTick()
                    < currentTick - 1L) {
                iterator.remove();
            }
        }
    }
}
