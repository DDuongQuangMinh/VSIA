package com.k1ngtle.vsia.cockpit.detection;

public record F35DetectionFilter(
        boolean mobs,
        boolean players,
        boolean ships
) {
    public static final F35DetectionFilter ALL =
            new F35DetectionFilter(
                    true,
                    true,
                    true
            );

    public static final F35DetectionFilter NONE =
            new F35DetectionFilter(
                    false,
                    false,
                    false
            );

    public boolean accepts(
            F35DetectionType type
    ) {
        return switch (type) {
            case MOB -> mobs;
            case PLAYER -> players;
            case SHIP -> ships;
        };
    }

    public String summary() {
        StringBuilder builder =
                new StringBuilder();

        if (mobs) {
            builder.append("MOB");
        }

        if (players) {
            if (!builder.isEmpty()) {
                builder.append('+');
            }
            builder.append("PLY");
        }

        if (ships) {
            if (!builder.isEmpty()) {
                builder.append('+');
            }
            builder.append("SHIP");
        }

        return builder.isEmpty()
                ? "NONE"
                : builder.toString();
    }
}
