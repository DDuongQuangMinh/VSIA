package com.k1ngtle.vsia.signality.internet.satellite.client;

import com.k1ngtle.vsia.phone.client.PhoneText;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiAction;
import com.k1ngtle.vsia.signality.internet.satellite.gui.SatelliteGuiSnapshot;
import com.k1ngtle.vsia.signality.internet.satellite.network.C2SSatelliteGuiActionPacket;
import com.k1ngtle.vsia.signality.internet.satellite.network.C2SSatelliteGuiRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SatelliteTerminalScreen
        extends Screen {

    private static final int WIDTH =
            390;

    private static final int HEIGHT =
            270;

    private static final int BG =
            0xFF09111B;

    private static final int PANEL =
            0xFF111E2D;

    private static final int PANEL_2 =
            0xFF18283A;

    private static final int DISPLAY =
            0xFF081A28;

    private static final int TEXT =
            0xFFE8F4FF;

    private static final int MUTED =
            0xFF8EA3B7;

    private static final int CYAN =
            0xFF5EDCFF;

    private static final int GREEN =
            0xFF69E6A6;

    private static final int AMBER =
            0xFFFFC857;

    private static final int RED =
            0xFFFF6B6B;

    private final BlockPos pos;

    private final List<ButtonArea> buttons =
            new ArrayList<>();

    private SatelliteGuiSnapshot snapshot;

    private int refreshTicks;

    private int left;
    private int top;

    public SatelliteTerminalScreen(
            BlockPos pos
    ) {
        super(
                Component.literal(
                        "VSIA Satellite Terminal"
                )
        );

        this.pos =
                pos.immutable();
    }

    @Override
    protected void init() {
        super.init();

        left =
                (width - WIDTH)
                        / 2;

        top =
                (height - HEIGHT)
                        / 2;

        buildButtons();

        FieldDeviceNetwork.sendToServer(
                new C2SSatelliteGuiRequestPacket(
                        pos
                )
        );
    }

    public void acceptSnapshot(
            SatelliteGuiSnapshot snapshot
    ) {
        if (snapshot == null
                || !snapshot.pos()
                .equals(pos)) {
            return;
        }

        this.snapshot =
                snapshot;
    }

    @Override
    public void tick() {
        super.tick();

        refreshTicks++;

        if (refreshTicks >= 20) {
            refreshTicks = 0;

            FieldDeviceNetwork.sendToServer(
                    new C2SSatelliteGuiRequestPacket(
                            pos
                    )
            );
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(
                graphics
        );

        rounded(
                graphics,
                left,
                top,
                WIDTH,
                HEIGHT,
                12,
                BG
        );

        rounded(
                graphics,
                left + 8,
                top + 8,
                WIDTH - 16,
                HEIGHT - 16,
                9,
                PANEL
        );

        PhoneText.draw(
                graphics,
                font,
                "VS:IA SATELLITE TERMINAL",
                left + 18,
                top + 16,
                TEXT
        );

        PhoneText.draw(
                graphics,
                font,
                "LEO CONSTELLATION",
                left + WIDTH - 118,
                top + 16,
                MUTED
        );

        renderLinkPanel(
                graphics
        );

        for (ButtonArea button
                : buttons) {
            boolean hover =
                    button.contains(
                            mouseX,
                            mouseY
                    );

            rounded(
                    graphics,
                    button.x(),
                    button.y(),
                    button.width(),
                    button.height(),
                    6,
                    hover
                            ? 0xFF29425A
                            : PANEL_2
            );

            String label =
                    button.action()
                            == SatelliteGuiAction.GATEWAY_TOGGLE
                            && snapshot != null
                            ? (
                            snapshot.internetGatewayEnabled()
                                    ? "GATEWAY ON"
                                    : "GATEWAY OFF"
                    )
                            : button.label();

            PhoneText.drawCentered(
                    graphics,
                    font,
                    label,
                    button.x()
                            + button.width()
                            / 2,
                    button.y()
                            + 8,
                    button.action()
                            == SatelliteGuiAction.GATEWAY_TOGGLE
                            && snapshot != null
                            && snapshot.internetGatewayEnabled()
                            ? GREEN
                            : TEXT
            );
        }

        PhoneText.draw(
                graphics,
                font,
                snapshot == null
                        ? "Waiting for terminal..."
                        : snapshot.status(),
                left + 18,
                top + HEIGHT - 20,
                snapshot != null
                        && !snapshot.valid()
                        ? RED
                        : MUTED
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderLinkPanel(
            GuiGraphics graphics
    ) {
        int x =
                left + 18;

        int y =
                top + 38;

        int width =
                WIDTH - 36;

        int height =
                132;

        rounded(
                graphics,
                x,
                y,
                width,
                height,
                8,
                DISPLAY
        );

        if (snapshot == null) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "SYNCING ORBIT DATA...",
                    left + WIDTH / 2,
                    y + 57,
                    CYAN
            );

            return;
        }

        if (!snapshot.valid()) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "TERMINAL UNAVAILABLE",
                    left + WIDTH / 2,
                    y + 42,
                    RED
            );

            return;
        }

        PhoneText.draw(
                graphics,
                font,
                "BAND "
                        + snapshot.band(),
                x + 10,
                y + 9,
                CYAN
        );

        PhoneText.draw(
                graphics,
                font,
                snapshot.internetGatewayEnabled()
                        ? "INTERNET GW: ON"
                        : "INTERNET GW: OFF",
                x + width - 105,
                y + 9,
                snapshot.internetGatewayEnabled()
                        ? GREEN
                        : MUTED
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "UP %.3f GHz   DOWN %.3f GHz",
                        snapshot.uplinkHz()
                                / 1.0E9,
                        snapshot.downlinkHz()
                                / 1.0E9
                ),
                x + 10,
                y + 25,
                TEXT
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "BW %.1f MHz   MASK %.1f deg",
                        snapshot.bandwidthHz()
                                / 1.0E6,
                        snapshot.minimumElevationDeg()
                ),
                x + 10,
                y + 41,
                MUTED
        );

        SatelliteLinkAssessment link =
                snapshot.assessment();

        if (!link.visible()) {
            PhoneText.draw(
                    graphics,
                    font,
                    "NO COMMON SATELLITE ABOVE MASK",
                    x + 10,
                    y + 67,
                    AMBER
            );

            PhoneText.draw(
                    graphics,
                    font,
                    "Constellation: 144 LEO satellites / 550 km / 53 deg",
                    x + 10,
                    y + 85,
                    MUTED
            );

            return;
        }

        PhoneText.draw(
                graphics,
                font,
                "SAT "
                        + link.satelliteName(),
                x + 10,
                y + 61,
                GREEN
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "ELEV %.1f / %.1f deg   RANGE %.0f / %.0f km",
                        link.sourceElevationDeg(),
                        link.targetElevationDeg(),
                        link.sourceSlantRangeMeters()
                                / 1000.0,
                        link.targetSlantRangeMeters()
                                / 1000.0
                ),
                x + 10,
                y + 77,
                TEXT
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "SNR %.1f / %.1f dB   DELAY %.2f ms   P %.3f",
                        link.uplinkSnrDb(),
                        link.downlinkSnrDb(),
                        link.propagationDelayMs(),
                        link.packetSuccessProbability()
                ),
                x + 10,
                y + 93,
                TEXT
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "DOPPLER %.0f / %.0f Hz",
                        link.uplinkDopplerHz(),
                        link.downlinkDopplerHz()
                ),
                x + 10,
                y + 109,
                MUTED
        );

        if (!snapshot.lastPacket()
                .isBlank()) {
            PhoneText.draw(
                    graphics,
                    font,
                    "RX: "
                            + trim(
                            snapshot.lastPacket(),
                            46
                    ),
                    x + 168,
                    y + 109,
                    CYAN
            );
        }
    }

    private void buildButtons() {
        buttons.clear();

        int y1 =
                top + 184;

        int y2 =
                top + 216;

        add(
                left + 18,
                y1,
                96,
                24,
                "CYCLE BAND",
                SatelliteGuiAction.BAND_CYCLE
        );

        add(
                left + 120,
                y1,
                72,
                24,
                "MASK -",
                SatelliteGuiAction.ELEVATION_DOWN
        );

        add(
                left + 198,
                y1,
                72,
                24,
                "MASK +",
                SatelliteGuiAction.ELEVATION_UP
        );

        add(
                left + 276,
                y1,
                96,
                24,
                "REFRESH",
                SatelliteGuiAction.REFRESH
        );

        add(
                left + 18,
                y2,
                110,
                24,
                "LINK TEST",
                SatelliteGuiAction.LINK_TEST
        );

        add(
                left + 134,
                y2,
                110,
                24,
                "TEST PACKET",
                SatelliteGuiAction.PACKET_TEST
        );

        add(
                left + 250,
                y2,
                122,
                24,
                "GATEWAY OFF",
                SatelliteGuiAction.GATEWAY_TOGGLE
        );
    }

    private void add(
            int x,
            int y,
            int width,
            int height,
            String label,
            SatelliteGuiAction action
    ) {
        buttons.add(
                new ButtonArea(
                        x,
                        y,
                        width,
                        height,
                        label,
                        action
                )
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            for (ButtonArea area
                    : buttons) {
                if (area.contains(
                        mouseX,
                        mouseY
                )) {
                    FieldDeviceNetwork
                            .sendToServer(
                                    new C2SSatelliteGuiActionPacket(
                                            pos,
                                            area.action()
                                    )
                            );

                    return true;
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String trim(
            String value,
            int max
    ) {
        if (value == null
                || value.length()
                <= max) {
            return value == null
                    ? ""
                    : value;
        }

        return value.substring(
                0,
                max - 3
        ) + "...";
    }

    private static void rounded(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        int r =
                Math.max(
                        0,
                        Math.min(
                                radius,
                                Math.min(
                                        width / 2,
                                        height / 2
                                )
                        )
                );

        if (r == 0) {
            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    color
            );

            return;
        }

        graphics.fill(
                x + r,
                y,
                x + width - r,
                y + height,
                color
        );

        graphics.fill(
                x,
                y + r,
                x + width,
                y + height - r,
                color
        );

        for (int i = 0;
             i < r;
             i++) {
            int dy =
                    r - i;

            int inset =
                    (int) Math.ceil(
                            r - Math.sqrt(
                                    Math.max(
                                            0,
                                            r * r
                                                    - dy
                                                    * dy
                                    )
                            )
                    );

            graphics.fill(
                    x + inset,
                    y + i,
                    x + width - inset,
                    y + i + 1,
                    color
            );

            graphics.fill(
                    x + inset,
                    y + height - i - 1,
                    x + width - inset,
                    y + height - i,
                    color
            );
        }
    }

    private record ButtonArea(
            int x,
            int y,
            int width,
            int height,
            String label,
            SatelliteGuiAction action
    ) {
        private boolean contains(
                double mouseX,
                double mouseY
        ) {
            return mouseX >= x
                    && mouseX < x + width
                    && mouseY >= y
                    && mouseY < y + height;
        }
    }
}
