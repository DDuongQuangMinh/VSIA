package com.k1ngtle.vsia.signality.engineering.wifi.integration.w123;

import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmScheduler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveScheduler;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

public final class W123ShipTrackingStore {
    public record Mark(
            UUID deviceId,
            BlockPos storagePosition,
            Vec3 initialWorldPosition
    ) {
    }

    public record VerifyResult(
            boolean passed,
            String detail,
            UUID deviceId,
            BlockPos storagePosition,
            Vec3 initialWorldPosition,
            Vec3 currentWorldPosition,
            double worldDisplacementBlocks
    ) {
    }

    private static Mark mark;

    private W123ShipTrackingStore() {
    }

    public static synchronized Mark mark(
            ServerLevel level,
            BlockPos requestedWorldPosition
    ) {
        NetworkDeviceBlockEntity device =
                WifiEngineeringDeviceIdentityResolver.resolveNearWorld(
                        level,
                        requestedWorldPosition,
                        Set.of()
                );

        if (device == null) {
            return null;
        }

        Vec3 world =
                device.positionWorld();

        if (!finite(
                world
        )) {
            return null;
        }

        mark =
                new Mark(
                        device.id(),
                        device.getBlockPos()
                        .immutable(),
                        world
                );

        return mark;
    }

    public static synchronized VerifyResult verify(
            ServerLevel level,
            BlockPos requestedNewWorldPosition
    ) {
        if (mark == null) {
            return new VerifyResult(
                    false,
                    "No W1.23 ship identity mark exists. Run shipmark first.",
                    null,
                    null,
                    null,
                    null,
                    0.0D
            );
        }

        NetworkDeviceBlockEntity direct =
                WifiEngineeringDeviceIdentityResolver.resolve(
                        level,
                        mark.deviceId()
                );

        NetworkDeviceBlockEntity byNewWorld =
                WifiEngineeringDeviceIdentityResolver.resolveNearWorld(
                        level,
                        requestedNewWorldPosition,
                        Set.of()
                );

        if (direct == null
                || byNewWorld == null) {
            return new VerifyResult(
                    false,
                    "Persistent UUID or new world-position resolution failed after movement",
                    mark.deviceId(),
                    mark.storagePosition(),
                    mark.initialWorldPosition(),
                    direct == null
                            ? null
                            : direct.positionWorld(),
                    0.0D
            );
        }

        Vec3 current =
                direct.positionWorld();

        if (!finite(
                current
        )) {
            return new VerifyResult(
                    false,
                    "Resolved device has a non-finite world position",
                    mark.deviceId(),
                    direct.getBlockPos()
                    .immutable(),
                    mark.initialWorldPosition(),
                    current,
                    0.0D
            );
        }

        double displacement =
                current.distanceTo(
                        mark.initialWorldPosition()
                );

        boolean sameUuid =
                mark.deviceId()
                        .equals(
                                direct.id()
                        )
                        && mark.deviceId()
                        .equals(
                                byNewWorld.id()
                        );

        boolean schedulersOwned =
                TcpLiveScheduler.isRegisteredTo(
                        direct.id(),
                        direct
                )
                        && ProtocolVmScheduler.isRegisteredTo(
                        direct.id(),
                        direct
                );

        boolean moved =
                displacement >= 0.25D;

        boolean passed =
                sameUuid
                        && schedulersOwned
                        && moved
                        && direct.usesDetailedPropagationModel();

        String detail =
                passed
                        ? "Persistent UUID followed the same Wi-Fi device through world-space movement; schedulers remain owned by the live entity"
                        : "Ship tracking invariant failed"
                        + " | sameUuid="
                        + sameUuid
                        + " schedulersOwned="
                        + schedulersOwned
                        + " moved="
                        + moved
                        + " displacement="
                        + String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        displacement
                )
                        + " detailedRF="
                        + direct.usesDetailedPropagationModel();

        return new VerifyResult(
                passed,
                detail,
                direct.id(),
                direct.getBlockPos()
                .immutable(),
                mark.initialWorldPosition(),
                current,
                displacement
        );
    }

    public static synchronized Mark currentMark() {
        return mark;
    }

    public static synchronized void clear() {
        mark =
                null;
    }

    private static boolean finite(
            Vec3 value
    ) {
        return value != null
                && Double.isFinite(
                value.x
        )
                && Double.isFinite(
                value.y
        )
                && Double.isFinite(
                value.z
        );
    }
}
