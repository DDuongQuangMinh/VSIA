package com.k1ngtle.vsia.signality.engineering.radio;

import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MeshRoutingController {
    private static final long ROUTE_LIFETIME_NANOS =
            30_000_000_000L;

    private final MeshRouteTable routes =
            new MeshRouteTable();

    private final Set<String> seenRequests =
            new HashSet<>();

    private int ownSequenceNumber;
    private int nextRequestId;

    public MeshRouteTable routes() {
        return routes;
    }

    public CompoundTag createRouteRequest(
            UUID ownId,
            UUID destination
    ) {
        CompoundTag tag =
                new CompoundTag();

        tag.putString(
                "radio_message_type",
                RadioMessageType.ROUTE_REQUEST.name()
        );

        tag.putUUID(
                "origin",
                ownId
        );

        tag.putUUID(
                "destination",
                destination
        );

        tag.putInt(
                "origin_sequence",
                ++ownSequenceNumber
        );

        tag.putInt(
                "request_id",
                nextRequestId++
        );

        tag.putInt(
                "hop_count",
                0
        );

        return tag;
    }

    public boolean markAndCheckNewRequest(
            UUID origin,
            int requestId
    ) {
        return seenRequests.add(
                origin.toString()
                        + ":"
                        + requestId
        );
    }

    public void learnReverseRoute(
            UUID origin,
            UUID previousHop,
            int hopCount,
            int originSequence
    ) {
        routes.learn(
                origin,
                previousHop,
                hopCount,
                originSequence,
                ROUTE_LIFETIME_NANOS
        );
    }

    public void learnForwardRoute(
            UUID destination,
            UUID nextHop,
            int hopCount,
            int destinationSequence
    ) {
        routes.learn(
                destination,
                nextHop,
                hopCount,
                destinationSequence,
                ROUTE_LIFETIME_NANOS
        );
    }

    public int ownSequenceNumber() {
        return ownSequenceNumber;
    }
}
