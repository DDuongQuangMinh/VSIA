package com.k1ngtle.vsia.signality.engineering.wifi;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class EdcaController {
    private final Random random;
    private final Map<WifiAccessCategory, CsmaCaController> controllers =
            new EnumMap<>(WifiAccessCategory.class);

    public EdcaController(Random random) {
        this.random = random;

        for (WifiAccessCategory category
                : WifiAccessCategory.values()) {
            controllers.put(
                    category,
                    new CsmaCaController(
                            category.cwMin(),
                            category.cwMax(),
                            random
                    )
            );
        }
    }

    public int acquireLogicalMedium(
            WifiAccessCategory category
    ) {
        CsmaCaController controller =
                controllers.get(category);

        int aifsSlots =
                category.aifsn();

        int backoffSlots =
                controller.consumeBackoffForSimplifiedExecution();

        return aifsSlots + backoffSlots;
    }

    public void onSuccess(
            WifiAccessCategory category
    ) {
        controllers
                .get(category)
                .onSuccess(random);
    }

    public void onFailure(
            WifiAccessCategory category
    ) {
        controllers
                .get(category)
                .onFailure(random);
    }

    public int contentionWindow(
            WifiAccessCategory category
    ) {
        return controllers
                .get(category)
                .contentionWindow();
    }
}
