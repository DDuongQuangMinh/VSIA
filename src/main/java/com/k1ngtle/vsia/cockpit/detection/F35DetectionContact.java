package com.k1ngtle.vsia.cockpit.detection;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record F35DetectionContact(
        UUID contactId,
        F35DetectionType type,
        String label,
        Vec3 position,
        Vec3 velocity
) {
    public F35DetectionContact {
        label =
                label == null
                        ? type.name()
                        : label;
    }

    public double speedMps() {
        return velocity.length();
    }

    public String shortId() {
        String raw =
                contactId.toString()
                        .replace(
                                "-",
                                ""
                        );

        return raw.substring(
                0,
                Math.min(
                        4,
                        raw.length()
                )
        ).toUpperCase();
    }
}
