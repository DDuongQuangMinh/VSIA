package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkApi;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkTrack;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class F35RadarSyncEvents {
    private static final String DISPLAY_NETWORK =
            "default";

    private F35RadarSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player
                instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount
                % 5
                != 0) {
            return;
        }

        List<RadarNetworkTrack> tracks =
                RadarNetworkApi.tracks(
                        player.serverLevel(),
                        DISPLAY_NETWORK
                );

        VsiaNetwork.sendToPlayer(
                player,
                new S2CF35RadarSnapshotPacket(
                        DISPLAY_NETWORK,
                        player.serverLevel()
                                .getGameTime(),
                        tracks
                )
        );
    }
}
