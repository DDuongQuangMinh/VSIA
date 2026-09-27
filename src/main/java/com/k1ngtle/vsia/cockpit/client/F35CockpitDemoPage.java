package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.CcTerminalBuffer;
import com.k1ngtle.vsia.cockpit.CcTerminalColor;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class F35CockpitDemoPage {
    private static final Map<F35CockpitSeatBlockEntity, Long> LAST_UPDATE =
            new WeakHashMap<>();

    private F35CockpitDemoPage() {
    }

    public static void update(
            F35CockpitSeatBlockEntity cockpit
    ) {
        if (!cockpit.demoMode()
                || cockpit.getLevel() == null) {
            return;
        }

        long gameTime =
                cockpit.getLevel()
                        .getGameTime();

        Long previous =
                LAST_UPDATE.get(
                        cockpit
                );

        if (previous != null
                && gameTime - previous
                < 4L) {
            return;
        }

        LAST_UPDATE.put(
                cockpit,
                gameTime
        );

        LocalPlayer player =
                Minecraft.getInstance()
                        .player;

        if (player == null) {
            return;
        }

        CcTerminalBuffer terminal =
                cockpit.terminal();

        terminal.setBackgroundColor(
                CcTerminalColor.BLACK
        );

        terminal.setTextColor(
                CcTerminalColor.WHITE
        );

        terminal.clear();

        terminal.setCursorPos(
                1,
                1
        );

        terminal.setTextColor(
                CcTerminalColor.CYAN
        );

        terminal.write(
                fit(
                        "VSIA F-35 // DISPLAY",
                        terminal.width()
                )
        );

        terminal.setCursorPos(
                1,
                2
        );

        terminal.setTextColor(
                CcTerminalColor.LIGHT_BLUE
        );

        terminal.write(
                fit(
                        "MONITOR_CENTER ONLINE",
                        terminal.width()
                )
        );

        double speedMps =
                player.getDeltaMovement()
                        .length()
                        * 20.0;

        double heading =
                (
                        player.getYRot()
                                % 360.0
                                + 360.0
                )
                        % 360.0;

        terminal.setTextColor(
                CcTerminalColor.WHITE
        );

        line(
                terminal,
                4,
                String.format(
                        Locale.ROOT,
                        "SPD  %6.1f m/s",
                        speedMps
                )
        );

        line(
                terminal,
                5,
                String.format(
                        Locale.ROOT,
                        "ALT  %7.1f m",
                        player.getY()
                )
        );

        line(
                terminal,
                6,
                String.format(
                        Locale.ROOT,
                        "HDG  %03.0f deg",
                        heading
                )
        );

        line(
                terminal,
                7,
                String.format(
                        Locale.ROOT,
                        "POS  %.0f %.0f %.0f",
                        player.getX(),
                        player.getY(),
                        player.getZ()
                )
        );

        terminal.setCursorPos(
                1,
                9
        );

        terminal.setTextColor(
                CcTerminalColor.LIME
        );

        terminal.write(
                fit(
                        "CC TERMINAL API READY",
                        terminal.width()
                )
        );

        terminal.setCursorPos(
                1,
                10
        );

        terminal.setTextColor(
                CcTerminalColor.GRAY
        );

        terminal.write(
                fit(
                        "RMB: DEMO / API MODE",
                        terminal.width()
                )
        );
    }

    private static void line(
            CcTerminalBuffer terminal,
            int y,
            String text
    ) {
        terminal.setCursorPos(
                1,
                y
        );

        terminal.write(
                fit(
                        text,
                        terminal.width()
                )
        );
    }

    private static String fit(
            String text,
            int width
    ) {
        if (text.length()
                <= width) {
            return text;
        }

        return text.substring(
                0,
                width
        );
    }
}
