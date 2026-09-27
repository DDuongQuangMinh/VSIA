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
                && gameTime - previous < 4L) {
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

        writeLine(
                terminal,
                1,
                CcTerminalColor.CYAN,
                "VSIA F-35 / MONITOR"
        );

        writeLine(
                terminal,
                2,
                CcTerminalColor.LIME,
                "MONITOR_CENTER ONLINE"
        );

        writeLine(
                terminal,
                3,
                CcTerminalColor.WHITE,
                String.format(
                        Locale.ROOT,
                        "SPD %5.1f  ALT %5.0f",
                        speedMps,
                        player.getY()
                )
        );

        writeLine(
                terminal,
                4,
                CcTerminalColor.WHITE,
                String.format(
                        Locale.ROOT,
                        "HDG %03.0f",
                        heading
                )
        );

        writeLine(
                terminal,
                5,
                CcTerminalColor.LIGHT_BLUE,
                String.format(
                        Locale.ROOT,
                        "X %.0f  Z %.0f",
                        player.getX(),
                        player.getZ()
                )
        );

        writeLine(
                terminal,
                6,
                CcTerminalColor.GRAY,
                "RMB DEMO / API MODE"
        );
    }

    private static void writeLine(
            CcTerminalBuffer terminal,
            int row,
            CcTerminalColor color,
            String text
    ) {
        terminal.setCursorPos(
                1,
                row
        );

        terminal.setTextColor(
                color
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
        if (text.length() <= width) {
            return text;
        }

        return text.substring(
                0,
                width
        );
    }
}
