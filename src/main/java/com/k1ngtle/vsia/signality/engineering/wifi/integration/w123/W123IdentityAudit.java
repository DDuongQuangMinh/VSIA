package com.k1ngtle.vsia.signality.engineering.wifi.integration.w123;

import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmScheduler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveScheduler;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class W123IdentityAudit {
    public record Entry(
            char label,
            UUID deviceId,
            String macAddress,
            BlockPos storagePosition,
            Vec3 worldPosition,
            boolean directUuidResolution,
            boolean probeIdentityMatches,
            boolean tcpSchedulerOwned,
            boolean vmSchedulerOwned,
            boolean detailedPropagation
    ) {
    }

    public record Result(
            boolean passed,
            String detail,
            List<Entry> entries
    ) {
    }

    private W123IdentityAudit() {
    }

    public static Result audit(
            ServerLevel level,
            List<BlockPos> requested
    ) {
        if (level == null
                || requested == null
                || requested.size() != 4) {
            return new Result(
                    false,
                    "W1.23.3 identity audit requires exactly four requested positions",
                    List.of()
            );
        }

        Set<UUID> excluded =
                new HashSet<>();

        List<Entry> entries =
                new ArrayList<>();

        for (int index = 0;
             index < requested.size();
             index++) {
            BlockPos position =
                    requested.get(
                            index
                    );

            NetworkDeviceBlockEntity device =
                    WifiEngineeringDeviceIdentityResolver.resolveNearWorld(
                            level,
                            position,
                            excluded
                    );

            char label =
                    (char) ('A' + index);

            if (device == null) {
                return new Result(
                        false,
                        "Device "
                                + label
                                + " @ "
                                + position.toShortString()
                                + " did not resolve to a unique live Wi-Fi device",
                        List.copyOf(
                                entries
                        )
                );
            }

            if (!excluded.add(
                    device.id()
            )) {
                return new Result(
                        false,
                        "Device "
                                + label
                                + " resolved to a duplicate UUID "
                                + device.id(),
                        List.copyOf(
                                entries
                        )
                );
            }

            NetworkDeviceBlockEntity direct =
                    WifiEngineeringDeviceIdentityResolver.resolve(
                            level,
                            device.id()
                    );

            WifiEngineeringSnapshot probe =
                    WifiEngineeringProbe.capture(
                            device
                    );

            Vec3 world =
                    device.positionWorld();

            boolean finiteWorld =
                    world != null
                            && Double.isFinite(
                            world.x
                    )
                            && Double.isFinite(
                            world.y
                    )
                            && Double.isFinite(
                            world.z
                    );

            Entry entry =
                    new Entry(
                            label,
                            device.id(),
                            device.wifiMacAddress(),
                            device.getBlockPos()
                            .immutable(),
                            world,
                            direct == device,
                            device.id()
                            .equals(
                                    probe.deviceId()
                            ),
                            TcpLiveScheduler.isRegisteredTo(
                                    device.id(),
                                    device
                            ),
                            ProtocolVmScheduler.isRegisteredTo(
                                    device.id(),
                                    device
                            ),
                            device.usesDetailedPropagationModel()
                    );

            entries.add(
                    entry
            );

            if (!finiteWorld
                    || !entry.directUuidResolution()
                    || !entry.probeIdentityMatches()
                    || !entry.tcpSchedulerOwned()
                    || !entry.vmSchedulerOwned()
                    || !entry.detailedPropagation()) {
                return new Result(
                        false,
                        "Device "
                                + label
                                + " failed W1.23 identity/lifecycle/RF invariants"
                                + " | finiteWorld="
                                + finiteWorld
                                + " directUuid="
                                + entry.directUuidResolution()
                                + " probeId="
                                + entry.probeIdentityMatches()
                                + " tcpOwner="
                                + entry.tcpSchedulerOwned()
                                + " vmOwner="
                                + entry.vmSchedulerOwned()
                                + " detailedRF="
                                + entry.detailedPropagation(),
                        List.copyOf(
                                entries
                        )
                );
            }
        }

        return new Result(
                true,
                "Four unique persistent UUID targets passed direct resolution, probe identity, scheduler ownership and detailed-RF checks",
                List.copyOf(
                        entries
                )
        );
    }
}
