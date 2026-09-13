package com.k1ngtle.vsia.signality.internet.radio.portable;

import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PortableRadioService {
    private static final Map<UUID, PortableRadioEndpoint> ENDPOINTS =
            new HashMap<>();

    private PortableRadioService() {
    }

    public static void tick(
            ServerPlayer player
    ) {
        InteractionHand radioHand =
                radioHand(player);

        PortableRadioEndpoint existing =
                ENDPOINTS.get(
                        player.getUUID()
                );

        if (radioHand == null) {
            if (existing != null) {
                existing.unregister();
                ENDPOINTS.remove(
                        player.getUUID()
                );
            }

            return;
        }

        UUID currentRadioId =
                PortableRadioState.id(
                        player.getItemInHand(
                                radioHand
                        )
                );

        if (existing == null
                || existing.hand()
                != radioHand
                || !existing.valid()
                || !existing.id()
                .equals(
                        currentRadioId
                )) {

            if (existing != null) {
                existing.unregister();
            }

            PortableRadioEndpoint endpoint =
                    new PortableRadioEndpoint(
                            player,
                            radioHand
                    );

            SignalBus.registerReceiver(
                    endpoint
            );

            SignalBus.registerTransmitter(
                    endpoint
            );

            ENDPOINTS.put(
                    player.getUUID(),
                    endpoint
            );

            return;
        }

        existing.refresh();
    }

    public static void remove(
            ServerPlayer player
    ) {
        PortableRadioEndpoint endpoint =
                ENDPOINTS.remove(
                        player.getUUID()
                );

        if (endpoint != null) {
            endpoint.unregister();
        }
    }

    public static void clear() {
        for (PortableRadioEndpoint endpoint
                : java.util.List.copyOf(
                ENDPOINTS.values()
        )) {
            endpoint.unregister();
        }

        ENDPOINTS.clear();
    }

    public static List<PortableRadioEndpoint> endpointsInLevel(
            ServerLevel level
    ) {
        if (level == null) {
            return List.of();
        }

        List<PortableRadioEndpoint> result =
                new ArrayList<>();

        for (PortableRadioEndpoint endpoint
                : ENDPOINTS.values()) {
            if (endpoint != null
                    && endpoint.valid()
                    && endpoint.level() == level) {
                result.add(
                        endpoint
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public static PortableRadioEndpoint endpoint(
            ServerPlayer player,
            InteractionHand hand
    ) {
        tick(player);

        PortableRadioEndpoint endpoint =
                ENDPOINTS.get(
                        player.getUUID()
                );

        if (endpoint == null
                || endpoint.hand()
                != hand) {
            return null;
        }

        return endpoint;
    }

    private static InteractionHand radioHand(
            ServerPlayer player
    ) {
        ItemStack main =
                player.getMainHandItem();

        if (main.getItem()
                instanceof TemporaryRadioItem) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack off =
                player.getOffhandItem();

        if (off.getItem()
                instanceof TemporaryRadioItem) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }
}
