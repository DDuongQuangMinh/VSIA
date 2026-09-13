package com.k1ngtle.vsia.signality.internet.radio.satellite;

import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioService;
import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import com.k1ngtle.vsia.signality.internet.satellite.internet.SatelliteBackhaulService;
import com.k1ngtle.vsia.signality.internet.satellite.internet.SatelliteInternetPath;
import net.minecraft.nbt.CompoundTag;

import java.util.Random;

public final class SatelliteRadioRelayService {
    private SatelliteRadioRelayService() {
    }

    public static void relayLongHaul(
            PortableRadioEndpoint source,
            CompoundTag radioMessage,
            double frequencyHz
    ) {
        if (source == null
                || radioMessage == null
                || !source.valid()) {
            return;
        }

        String type =
                radioMessage.getString(
                        "radio_message_type"
                );

        if (!"VOICE".equalsIgnoreCase(
                type
        )
                && !"PACKET".equalsIgnoreCase(
                type
        )) {
            return;
        }

        for (PortableRadioEndpoint destination
                : PortableRadioService.endpointsInLevel(
                source.level()
        )) {
            if (destination == null
                    || destination == source
                    || !destination.valid()) {
                continue;
            }

            double distanceBlocks =
                    source.positionWorld()
                            .distanceTo(
                                    destination.positionWorld()
                            );

            if (distanceBlocks
                    < LongHaulRoutePolicy
                    .SATELLITE_REQUIRED_DISTANCE_BLOCKS) {
                continue;
            }

            SatelliteBackhaulService.PathResult result =
                    SatelliteBackhaulService.resolve(
                            source.level(),
                            source.positionWorld(),
                            destination.positionWorld()
                    );

            if (!result.success()
                    || !result.satelliteRequired()
                    || result.path() == null) {
                continue;
            }

            SatelliteInternetPath path =
                    result.path();

            double probability =
                    Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    path.link()
                                            .packetSuccessProbability()
                            )
                    );

            if (!survives(
                    source,
                    destination,
                    radioMessage,
                    probability
            )) {
                continue;
            }

            destination.receiveSatelliteRelay(
                    radioMessage.copy(),
                    frequencyHz,
                    source.id(),
                    path.link()
            );
        }
    }

    private static boolean survives(
            PortableRadioEndpoint source,
            PortableRadioEndpoint destination,
            CompoundTag radioMessage,
            double probability
    ) {
        if (probability >= 0.999999) {
            return true;
        }

        long sequence =
                radioMessage.contains(
                        "voice_stream_seq"
                )
                        ? radioMessage.getInt(
                        "voice_stream_seq"
                )
                        : radioMessage.getInt(
                        "voice_sequence"
                );

        long seed =
                source.id()
                        .getMostSignificantBits()
                        ^ Long.rotateLeft(
                        destination.id()
                                .getLeastSignificantBits(),
                        17
                )
                        ^ Long.rotateLeft(
                        sequence,
                        7
                )
                        ^ source.level()
                        .getGameTime();

        return new Random(
                seed
        )
                .nextDouble()
                <= probability;
    }
}
