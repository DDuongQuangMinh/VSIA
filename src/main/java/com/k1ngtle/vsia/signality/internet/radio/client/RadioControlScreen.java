package com.k1ngtle.vsia.signality.internet.radio.client;

import com.k1ngtle.vsia.phone.client.PhoneText;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiAction;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiSnapshot;
import com.k1ngtle.vsia.signality.internet.radio.gui.RadioGuiTarget;
import com.k1ngtle.vsia.signality.internet.radio.voice.client.RadioVoiceClient;
import com.k1ngtle.vsia.signality.internet.radio.network.C2SRadioGuiActionPacket;
import com.k1ngtle.vsia.signality.internet.radio.network.C2SRadioGuiRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RadioControlScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 326;

    private static final int BG = 0xFF0E1110;
    private static final int PANEL = 0xFF171D19;
    private static final int PANEL_2 = 0xFF202921;
    private static final int DISPLAY = 0xFF0E2A18;
    private static final int GREEN = 0xFF61E786;
    private static final int TEXT = 0xFFE6ECE7;
    private static final int MUTED = 0xFF93A097;
    private static final int BLUE = 0xFF4B9CFF;
    private static final int RED = 0xFFFF6961;
    private static final int AMBER = 0xFFFFC857;

    private final RadioGuiTarget target;
    private final List<HitButton> buttons =
            new ArrayList<>();

    private RadioGuiSnapshot snapshot;

    private int refreshTicks;

    private int left;
    private int top;

    public RadioControlScreen(
            RadioGuiTarget target
    ) {
        super(
                Component.literal(
                        "VSIA Radio"
                )
        );

        this.target = target;
    }

    @Override
    protected void init() {
        super.init();

        left =
                (width - PANEL_WIDTH)
                        / 2;

        top =
                (height - PANEL_HEIGHT)
                        / 2;

        rebuildButtons();

        FieldDeviceNetwork.sendToServer(
                new C2SRadioGuiRequestPacket(
                        target
                )
        );
    }

    public void acceptSnapshot(
            RadioGuiSnapshot snapshot
    ) {
        if (snapshot == null
                || !sameTarget(
                snapshot.target(),
                target
        )) {
            return;
        }

        this.snapshot = snapshot;
        rebuildButtons();
    }

    private static boolean sameTarget(
            RadioGuiTarget a,
            RadioGuiTarget b
    ) {
        if (a == null
                || b == null
                || a.kind()
                != b.kind()) {
            return false;
        }

        if (a.isBlock()) {
            return a.blockPos()
                    .equals(
                            b.blockPos()
                    );
        }

        return true;
    }

    @Override
    public void tick() {
        super.tick();

        refreshTicks++;

        if (refreshTicks >= 20) {
            refreshTicks = 0;

            FieldDeviceNetwork.sendToServer(
                    new C2SRadioGuiRequestPacket(
                            target
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

        fillRounded(
                graphics,
                left,
                top,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                10,
                BG
        );

        fillRounded(
                graphics,
                left + 8,
                top + 8,
                PANEL_WIDTH - 16,
                PANEL_HEIGHT - 16,
                8,
                PANEL
        );

        PhoneText.draw(
                graphics,
                font,
                "VS:IA FIELD RADIO",
                left + 18,
                top + 16,
                TEXT
        );

        PhoneText.draw(
                graphics,
                font,
                target.isBlock()
                        ? "PLACED UNIT"
                        : "PORTABLE UNIT",
                left + PANEL_WIDTH - 93,
                top + 16,
                MUTED
        );

        renderDisplay(
                graphics
        );

        for (HitButton button
                : buttons) {
            renderButton(
                    graphics,
                    button,
                    button.contains(
                            mouseX,
                            mouseY
                    )
            );
        }

        if (target.isHeld()) {
            String ptt =
                    "LIVE PTT: Hold "
                            + RadioVoiceClient
                            .get()
                            .pttKeyName()
                            + " | 20 ms G.711 u-law / jitter buffer";

            PhoneText.draw(
                    graphics,
                    font,
                    ptt,
                    left + 18,
                    top + PANEL_HEIGHT - 35,
                    GREEN
            );
        }

        PhoneText.draw(
                graphics,
                font,
                snapshot == null
                        ? "Waiting for radio..."
                        : snapshot.status(),
                left + 18,
                top + PANEL_HEIGHT - 21,
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

    private void renderDisplay(
            GuiGraphics graphics
    ) {
        int x =
                left + 18;

        int y =
                top + 38;

        int w =
                PANEL_WIDTH - 36;

        int h =
                86;

        fillRounded(
                graphics,
                x,
                y,
                w,
                h,
                6,
                DISPLAY
        );

        if (snapshot == null) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "SYNCING...",
                    left + PANEL_WIDTH / 2,
                    y + 36,
                    GREEN
            );

            return;
        }

        if (!snapshot.valid()) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "RADIO UNAVAILABLE",
                    left + PANEL_WIDTH / 2,
                    y + 24,
                    RED
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    snapshot.status(),
                    left + PANEL_WIDTH / 2,
                    y + 44,
                    MUTED
            );

            return;
        }

        PhoneText.draw(
                graphics,
                font,
                snapshot.band()
                        + "  "
                        + snapshot.emission(),
                x + 10,
                y + 9,
                GREEN
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "%.4f MHz",
                        snapshot.frequencyHz()
                                / 1_000_000.0
                ),
                x + 10,
                y + 26,
                0xFFD8FFDF
        );

        PhoneText.draw(
                graphics,
                font,
                String.format(
                        Locale.ROOT,
                        "SQL %.1f dB  BW %.1f kHz",
                        snapshot.squelchDb(),
                        snapshot.bandwidthHz()
                                / 1000.0
                ),
                x + 10,
                y + 43,
                MUTED
        );

        String link =
                snapshot.measured()
                        ? String.format(
                        Locale.ROOT,
                        "RX %.1f dBm   SNR %.1f dB",
                        snapshot.receivedPowerDbm(),
                        snapshot.snrDb()
                )
                        : "RX NO SIGNAL / NOT MEASURED";

        PhoneText.draw(
                graphics,
                font,
                link,
                x + 10,
                y + 60,
                snapshot.measured()
                        ? GREEN
                        : MUTED
        );

        String flags =
                (snapshot.meshEnabled()
                        ? "MESH "
                        : "")
                        + (
                        snapshot.fhssEnabled()
                                ? "FHSS "
                                : ""
                )
                        + (
                        snapshot.squelchOpen()
                                ? "SQL:OPEN"
                                : "SQL:CLOSED"
                );

        PhoneText.draw(
                graphics,
                font,
                flags,
                x + w - 118,
                y + 9,
                snapshot.squelchOpen()
                        ? GREEN
                        : AMBER
        );

        if (target.isHeld()) {
            String comsec =
                    snapshot.comsecEnabled()
                            ? "SECURE TEK-"
                            + String.format(
                                    Locale.ROOT,
                                    "%02d",
                                    snapshot.comsecKeySlot()
                            )
                            : "CLEAR";

            PhoneText.draw(
                    graphics,
                    font,
                    comsec,
                    x + w - 118,
                    y + 26,
                    snapshot.comsecEnabled()
                            ? BLUE
                            : MUTED
            );
        }

        if (!snapshot.lastVoice()
                .isBlank()) {
            PhoneText.draw(
                    graphics,
                    font,
                    "VOICE: "
                            + trim(
                            snapshot.lastVoice(),
                            36
                    ),
                    x + 10,
                    y + 74,
                    BLUE
            );
        }
    }

    private void rebuildButtons() {
        buttons.clear();

        int y1 =
                top + 136;

        int y2 =
                top + 170;

        int y3 =
                top + 204;

        int y4 =
                top + 238;

        add(
                left + 18,
                y1,
                56,
                24,
                "HF",
                RadioGuiAction.BAND_HF
        );

        add(
                left + 80,
                y1,
                56,
                24,
                "VHF",
                RadioGuiAction.BAND_VHF
        );

        add(
                left + 142,
                y1,
                56,
                24,
                "UHF",
                RadioGuiAction.BAND_UHF
        );

        add(
                left + 208,
                y1,
                40,
                24,
                "-F",
                RadioGuiAction.TUNE_DOWN
        );

        add(
                left + 254,
                y1,
                40,
                24,
                "+F",
                RadioGuiAction.TUNE_UP
        );

        add(
                left + 300,
                y1,
                42,
                24,
                "MODE",
                RadioGuiAction.EMISSION_CYCLE
        );

        add(
                left + 18,
                y2,
                72,
                24,
                "SQL -",
                RadioGuiAction.SQUELCH_DOWN
        );

        add(
                left + 96,
                y2,
                72,
                24,
                "SQL +",
                RadioGuiAction.SQUELCH_UP
        );

        add(
                left + 174,
                y2,
                72,
                24,
                "MESH",
                RadioGuiAction.MESH_TOGGLE
        );

        add(
                left + 252,
                y2,
                90,
                24,
                "FHSS",
                RadioGuiAction.FHSS_TOGGLE
        );

        add(
                left + 18,
                y3,
                102,
                24,
                target.isHeld()
                        ? "COMSEC"
                        : "COMSEC N/A",
                RadioGuiAction.COMSEC_TOGGLE
        );

        add(
                left + 126,
                y3,
                96,
                24,
                snapshot != null
                        && target.isHeld()
                        ? "KEY "
                        + snapshot.comsecKeySlot()
                        : "KEY -",
                RadioGuiAction.COMSEC_SLOT_NEXT
        );

        add(
                left + 228,
                y3,
                114,
                24,
                "REFRESH",
                RadioGuiAction.REFRESH
        );

        add(
                left + 18,
                y4,
                156,
                24,
                "PTT TEST",
                RadioGuiAction.PTT_TEST
        );

        add(
                left + 180,
                y4,
                162,
                24,
                "PACKET TEST",
                RadioGuiAction.PACKET_TEST
        );
    }

    private void add(
            int x,
            int y,
            int width,
            int height,
            String label,
            RadioGuiAction action
    ) {
        buttons.add(
                new HitButton(
                        x,
                        y,
                        width,
                        height,
                        label,
                        action
                )
        );
    }

    private void renderButton(
            GuiGraphics graphics,
            HitButton button,
            boolean hovered
    ) {
        boolean active =
                switch (button.action()) {
                    case MESH_TOGGLE ->
                            snapshot != null
                                    && snapshot.meshEnabled();

                    case FHSS_TOGGLE ->
                            snapshot != null
                                    && snapshot.fhssEnabled();

                    case COMSEC_TOGGLE,
                         COMSEC_SLOT_NEXT ->
                            target.isHeld()
                                    && snapshot != null
                                    && snapshot.comsecEnabled();

                    case BAND_HF ->
                            snapshot != null
                                    && "HF".equals(
                                    snapshot.band()
                            );

                    case BAND_VHF ->
                            snapshot != null
                                    && "VHF".equals(
                                    snapshot.band()
                            );

                    case BAND_UHF ->
                            snapshot != null
                                    && "UHF".equals(
                                    snapshot.band()
                            );

                    default ->
                            false;
                };

        int color =
                active
                        ? 0xFF245F36
                        : hovered
                        ? 0xFF34443A
                        : PANEL_2;

        fillRounded(
                graphics,
                button.x(),
                button.y(),
                button.width(),
                button.height(),
                5,
                color
        );

        PhoneText.drawCentered(
                graphics,
                font,
                button.label(),
                button.x()
                        + button.width()
                        / 2,
                button.y()
                        + 8,
                active
                        ? GREEN
                        : TEXT
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            for (HitButton hit
                    : buttons) {
                if (hit.contains(
                        mouseX,
                        mouseY
                )) {
                    FieldDeviceNetwork
                            .sendToServer(
                                    new C2SRadioGuiActionPacket(
                                            target,
                                            hit.action()
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
            int maximum
    ) {
        if (value == null) {
            return "";
        }

        if (value.length()
                <= maximum) {
            return value;
        }

        return value.substring(
                0,
                Math.max(
                        0,
                        maximum - 3
                )
        ) + "...";
    }

    private static void fillRounded(
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

    private record HitButton(
            int x,
            int y,
            int width,
            int height,
            String label,
            RadioGuiAction action
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
