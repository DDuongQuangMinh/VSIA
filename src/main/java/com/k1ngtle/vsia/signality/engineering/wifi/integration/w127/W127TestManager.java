package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w126.W126Snapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w126.W126TestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class W127TestManager {
    private static W127InternetStressSession internetSession;
    private static boolean internetSuitePassed;
    private static boolean wifiRegressionPassed;
    private static boolean watchingWifiRegression;
    private static String wifiRegressionDetail =
            "W1.26.4 regression not run in this W1.27 session.";

    private W127TestManager() {
    }

    public static synchronized boolean startInternet(
            ServerLevel level,
            BlockPos resolver,
            BlockPos root,
            BlockPos tld,
            BlockPos primary,
            BlockPos secondary,
            String zone,
            String hostname
    ) {
        if (internetSession != null
                && !internetSession.finished()) {
            return false;
        }

        if (watchingWifiRegression) {
            return false;
        }

        internetSession =
                new W127InternetStressSession(
                        level,
                        resolver,
                        root,
                        tld,
                        primary,
                        secondary,
                        zone,
                        hostname
                );

        internetSuitePassed = false;

        return true;
    }

    public static synchronized boolean startWifiRegression(
            ServerLevel level,
            BlockPos sta1,
            BlockPos sta2,
            BlockPos ap1,
            BlockPos ap2,
            BlockPos networkSwitch,
            BlockPos server
    ) {
        if (internetSession != null
                && !internetSession.finished()) {
            return false;
        }

        if (watchingWifiRegression) {
            return false;
        }

        W126TestManager.reset();

        boolean started =
                W126TestManager.start(
                        level,
                        sta1,
                        sta2,
                        ap1,
                        ap2,
                        networkSwitch,
                        server
                );

        if (!started) {
            return false;
        }

        watchingWifiRegression = true;
        wifiRegressionPassed = false;
        wifiRegressionDetail =
                "W1.26.4 regression RUNNING";

        return true;
    }

    public static synchronized W127Snapshot snapshot() {
        if (internetSession != null) {
            return internetSession.snapshot(
                    internetSuitePassed,
                    wifiRegressionPassed,
                    wifiRegressionDetail
            );
        }

        return new W127Snapshot(
                W127Stage.SETUP,
                W127Failure.NONE,
                false,
                false,
                "No W1.27 Internet stress suite active.",
                0L,
                "",
                "",
                "",
                W127FaultController.snapshot(),
                internetSuitePassed,
                wifiRegressionPassed,
                wifiRegressionDetail
        );
    }

    public static synchronized boolean internetSuitePassed() {
        return internetSuitePassed;
    }

    public static synchronized boolean wifiRegressionPassed() {
        return wifiRegressionPassed;
    }

    public static synchronized String wifiRegressionDetail() {
        return wifiRegressionDetail;
    }

    public static synchronized boolean closurePassed() {
        return internetSuitePassed
                && wifiRegressionPassed
                && W127FaultController.snapshot().activeRules() == 0
                && W127FaultController.snapshot().delayedQueued() == 0;
    }

    public static synchronized void resetCurrent() {
        internetSession = null;
        watchingWifiRegression = false;
        W126TestManager.reset();
        W127FaultController.clearAll();
    }

    public static synchronized void resetAll() {
        resetCurrent();
        internetSuitePassed = false;
        wifiRegressionPassed = false;
        wifiRegressionDetail =
                "W1.26.4 regression not run in this W1.27 session.";
        W127FaultController.resetMetrics();
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        W127InternetStressSession internet;

        synchronized (W127TestManager.class) {
            internet = internetSession;
        }

        if (internet != null
                && !internet.finished()) {
            internet.tick();

            if (internet.finished()) {
                synchronized (W127TestManager.class) {
                    internetSuitePassed =
                            internet.passed();
                }
            }
        }

        boolean watch;

        synchronized (W127TestManager.class) {
            watch = watchingWifiRegression;
        }

        if (!watch) {
            return;
        }

        W126Snapshot snapshot =
                W126TestManager.snapshot();

        if (snapshot == null) {
            synchronized (W127TestManager.class) {
                watchingWifiRegression = false;
                wifiRegressionPassed = false;
                wifiRegressionDetail =
                        "W1.26.4 regression disappeared before completion.";
            }
            return;
        }

        if (!snapshot.finished()) {
            synchronized (W127TestManager.class) {
                wifiRegressionDetail =
                        snapshot.stage()
                                + " | "
                                + snapshot.detail();
            }
            return;
        }

        synchronized (W127TestManager.class) {
            watchingWifiRegression = false;
            wifiRegressionPassed =
                    snapshot.passed();

            wifiRegressionDetail =
                    (snapshot.passed()
                            ? "PASS | "
                            : "FAIL | ")
                            + snapshot.stage()
                            + " | "
                            + snapshot.detail();
        }
    }
}
