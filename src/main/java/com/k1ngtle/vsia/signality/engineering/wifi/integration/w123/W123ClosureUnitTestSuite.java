package com.k1ngtle.vsia.signality.engineering.wifi.integration.w123;

import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringOpenPacket;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmController;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmHost;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmScheduler;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiContentionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class W123ClosureUnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private W123ClosureUnitTestSuite() {
    }

    public static List<Result> runAll() {
        List<Result> results =
                new ArrayList<>();

        results.add(
                uniqueMultiAnalyzerIdsAccepted()
        );

        results.add(
                duplicateMultiAnalyzerIdsRejected()
        );

        results.add(
                tcpSchedulerOwnerSafety()
        );

        results.add(
                protocolVmSchedulerOwnerSafety()
        );

        results.add(
                contentionSnapshotContract()
        );

        return List.copyOf(
                results
        );
    }

    private static Result uniqueMultiAnalyzerIdsAccepted() {
        try {
            new WifiMultiEngineeringOpenPacket(
                    List.of(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID()
                    )
            );

            return new Result(
                    "w123-multi-uuid-unique",
                    true,
                    "Four unique persistent device UUIDs are accepted"
            );
        } catch (Exception exception) {
            return new Result(
                    "w123-multi-uuid-unique",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private static Result duplicateMultiAnalyzerIdsRejected() {
        UUID duplicate =
                UUID.randomUUID();

        try {
            new WifiMultiEngineeringOpenPacket(
                    List.of(
                            duplicate,
                            UUID.randomUUID(),
                            duplicate,
                            UUID.randomUUID()
                    )
            );

            return new Result(
                    "w123-multi-uuid-duplicate-reject",
                    false,
                    "Duplicate persistent UUID targets were unexpectedly accepted"
            );
        } catch (IllegalArgumentException expected) {
            return new Result(
                    "w123-multi-uuid-duplicate-reject",
                    true,
                    "Duplicate persistent UUID targets are rejected"
            );
        }
    }

    private static Result tcpSchedulerOwnerSafety() {
        UUID id =
                UUID.randomUUID();

        Object ownerA =
                new Object();

        Object ownerB =
                new Object();

        AtomicInteger ticks =
                new AtomicInteger();

        try {
            TcpLiveScheduler.register(
                    id,
                    ownerA,
                    ticks::incrementAndGet
            );

            boolean registered =
                    TcpLiveScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            TcpLiveScheduler.unregister(
                    id,
                    ownerB
            );

            boolean survivedWrongOwner =
                    TcpLiveScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            TcpLiveScheduler.unregister(
                    id,
                    ownerA
            );

            boolean removedByOwner =
                    !TcpLiveScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            boolean passed =
                    registered
                            && survivedWrongOwner
                            && removedByOwner;

            return new Result(
                    "w123-tcp-scheduler-owner-safety",
                    passed,
                    passed
                            ? "Wrong-owner unregister cannot remove a live TCP scheduler entry"
                            : "TCP scheduler owner-safety invariant failed"
            );
        } finally {
            TcpLiveScheduler.unregister(
                    id
            );
        }
    }

    private static Result protocolVmSchedulerOwnerSafety() {
        UUID id =
                UUID.randomUUID();

        Object ownerA =
                new Object();

        Object ownerB =
                new Object();

        ProtocolVmHost host =
                new ProtocolVmHost() {
                    @Override
                    public void sendFrame(
                            byte[] frame
                    ) {
                    }

                    @Override
                    public void deliverToHost(
                            byte[] payload
                    ) {
                    }

                    @Override
                    public long currentTick() {
                        return 0L;
                    }
                };

        ProtocolVmController controller =
                new ProtocolVmController(
                        host
                );

        try {
            ProtocolVmScheduler.register(
                    id,
                    ownerA,
                    controller
            );

            boolean registered =
                    ProtocolVmScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            ProtocolVmScheduler.unregister(
                    id,
                    ownerB
            );

            boolean survivedWrongOwner =
                    ProtocolVmScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            ProtocolVmScheduler.unregister(
                    id,
                    ownerA
            );

            boolean removedByOwner =
                    !ProtocolVmScheduler.isRegisteredTo(
                            id,
                            ownerA
                    );

            boolean passed =
                    registered
                            && survivedWrongOwner
                            && removedByOwner;

            return new Result(
                    "w123-vm-scheduler-owner-safety",
                    passed,
                    passed
                            ? "Wrong-owner unregister cannot remove a live Protocol VM scheduler entry"
                            : "Protocol VM scheduler owner-safety invariant failed"
            );
        } finally {
            ProtocolVmScheduler.unregister(
                    id
            );
        }
    }

    private static Result contentionSnapshotContract() {
        WifiContentionSnapshot snapshot =
                new WifiContentionSnapshot(
                        0,
                        64,
                        64,
                        64L,
                        64L,
                        60L,
                        4L,
                        32L,
                        8L,
                        3,
                        7,
                        15,
                        15
                );

        boolean passed =
                snapshot.queueDepth()
                        <= snapshot.queueCapacity()
                        && snapshot.queuePeak()
                        <= snapshot.queueCapacity()
                        && snapshot.compact()
                        .startsWith(
                                "W1.23 q="
                        );

        return new Result(
                "w123-contention-snapshot-contract",
                passed,
                passed
                        ? "W1.23 contention snapshot exposes bounded queue and contention counters"
                        : "Contention snapshot contract failed"
        );
    }
}
