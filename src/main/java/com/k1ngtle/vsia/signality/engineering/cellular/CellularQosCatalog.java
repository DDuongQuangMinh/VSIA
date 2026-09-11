package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.Map;

public final class CellularQosCatalog {
    private static final Map<Integer, CellularQosClass> CLASSES = Map.of(
            1, new CellularQosClass(
                    1,
                    "Conversational voice",
                    20,
                    100,
                    1.0e-2,
                    2.5
            ),
            2, new CellularQosClass(
                    2,
                    "Conversational video",
                    40,
                    150,
                    1.0e-3,
                    2.0
            ),
            5, new CellularQosClass(
                    5,
                    "IMS signalling",
                    10,
                    100,
                    1.0e-6,
                    3.0
            ),
            7, new CellularQosClass(
                    7,
                    "Interactive video",
                    70,
                    100,
                    1.0e-3,
                    1.6
            ),
            8, new CellularQosClass(
                    8,
                    "TCP interactive",
                    80,
                    300,
                    1.0e-6,
                    1.3
            ),
            9, new CellularQosClass(
                    9,
                    "Best effort data",
                    90,
                    300,
                    1.0e-6,
                    1.0
            )
    );

    private CellularQosCatalog() {
    }

    public static CellularQosClass forFiveQi(int fiveQi) {
        return CLASSES.getOrDefault(
                fiveQi,
                CLASSES.get(9)
        );
    }
}
