package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class RadioAudioSettingsScreen
        extends Screen {

    private static final int PANEL =
            0xFF111821;

    private static final int PANEL_2 =
            0xFF17232E;

    private static final int PANEL_3 =
            0xFF1D2D3A;

    private static final int BORDER =
            0xFF2B4050;

    private static final int TEXT =
            0xFFE8F1F7;

    private static final int MUTED =
            0xFF8EA2B0;

    private static final int CYAN =
            0xFF57D7FF;

    private static final int GREEN =
            0xFF6BE89A;

    private static final int AMBER =
            0xFFFFC95C;

    private static final int RED =
            0xFFFF6B6B;

    private static final int ROW_HEIGHT =
            28;

    private final List<RadioAudioDevices.DeviceOption>
            outputOptions =
            RadioAudioDevices.outputOptions();

    private final List<RadioAudioDevices.DeviceOption>
            inputOptions =
            RadioAudioDevices.inputOptions();

    private String pendingOutput =
            RadioAudioSettings.outputDevice();

    private String pendingInput =
            RadioAudioSettings.inputDevice();

    private double pendingVolume =
            RadioAudioSettings.outputVolume();

    private int outputScroll;
    private int inputScroll;

    private String status =
            "Select audio devices, then Apply or Test Tone.";

    private int statusColor =
            MUTED;

    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    private Rect outputList;
    private Rect inputList;

    private Rect volumeDown;
    private Rect volumeUp;

    private Rect applyButton;
    private Rect testButton;
    private Rect resetButton;
    private Rect doneButton;

    public RadioAudioSettingsScreen() {
        super(
                Component.literal(
                        "VS:IA Radio Audio"
                )
        );
    }

    @Override
    protected void init() {
        panelWidth =
                Math.min(
                        680,
                        width - 28
                );

        panelHeight =
                Math.min(
                        390,
                        height - 28
                );

        left =
                (width - panelWidth)
                        / 2;

        top =
                (height - panelHeight)
                        / 2;

        int margin =
                18;

        int gap =
                14;

        int columnWidth =
                (
                        panelWidth
                                - margin * 2
                                - gap
                )
                        / 2;

        int listY =
                top + 88;

        int listHeight =
                Math.max(
                        112,
                        panelHeight - 194
                );

        outputList =
                new Rect(
                        left + margin,
                        listY,
                        columnWidth,
                        listHeight
                );

        inputList =
                new Rect(
                        outputList.x()
                                + columnWidth
                                + gap,
                        listY,
                        columnWidth,
                        listHeight
                );

        int volumeY =
                top + panelHeight - 88;

        volumeDown =
                new Rect(
                        left + 170,
                        volumeY,
                        28,
                        22
                );

        volumeUp =
                new Rect(
                        left + 264,
                        volumeY,
                        28,
                        22
                );

        int buttonY =
                top + panelHeight - 48;

        int buttonGap =
                8;

        int buttonWidth =
                (
                        panelWidth
                                - margin * 2
                                - buttonGap * 3
                )
                        / 4;

        applyButton =
                new Rect(
                        left + margin,
                        buttonY,
                        buttonWidth,
                        26
                );

        testButton =
                new Rect(
                        applyButton.x()
                                + buttonWidth
                                + buttonGap,
                        buttonY,
                        buttonWidth,
                        26
                );

        resetButton =
                new Rect(
                        testButton.x()
                                + buttonWidth
                                + buttonGap,
                        buttonY,
                        buttonWidth,
                        26
                );

        doneButton =
                new Rect(
                        resetButton.x()
                                + buttonWidth
                                + buttonGap,
                        buttonY,
                        buttonWidth,
                        26
                );

        outputScroll =
                clampScroll(
                        outputScroll,
                        outputOptions,
                        outputList
                );

        inputScroll =
                clampScroll(
                        inputScroll,
                        inputOptions,
                        inputList
                );
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

        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + panelHeight,
                PANEL
        );

        outline(
                graphics,
                left,
                top,
                panelWidth,
                panelHeight,
                BORDER
        );

        graphics.drawString(
                font,
                "VS:IA RADIO AUDIO",
                left + 18,
                top + 16,
                TEXT,
                false
        );

        graphics.drawString(
                font,
                "Press ] again or Esc to close",
                left + panelWidth
                        - 18
                        - font.width(
                        "Press ] again or Esc to close"
                ),
                top + 16,
                MUTED,
                false
        );

        RadioVoiceClient voiceClient =
                RadioVoiceClient.get();

        String playbackError =
                voiceClient.playbackError();

        String activeOutput =
                voiceClient.playbackDeviceDescription();

        boolean hasPlaybackError =
                playbackError != null
                        && !playbackError.isBlank();

        String activeLine =
                hasPlaybackError
                        ? "Playback error: "
                        + playbackError
                        : activeOutput == null
                        || activeOutput.isBlank()
                        ? "Active output: not opened yet"
                        : "Active output: "
                        + activeOutput;

        graphics.drawString(
                font,
                ellipsize(
                        activeLine,
                        panelWidth - 36
                ),
                left + 18,
                top + 40,
                hasPlaybackError
                        ? RED
                        : activeOutput == null
                        || activeOutput.isBlank()
                        ? MUTED
                        : GREEN,
                false
        );

        graphics.drawString(
                font,
                ellipsize(
                        status,
                        panelWidth - 36
                ),
                left + 18,
                top + 57,
                statusColor,
                false
        );

        drawDeviceColumn(
                graphics,
                outputList,
                "OUTPUT DEVICE",
                outputOptions,
                pendingOutput,
                outputScroll,
                mouseX,
                mouseY
        );

        drawDeviceColumn(
                graphics,
                inputList,
                "MICROPHONE",
                inputOptions,
                pendingInput,
                inputScroll,
                mouseX,
                mouseY
        );

        int volumeY =
                top + panelHeight - 88;

        graphics.drawString(
                font,
                "RADIO VOLUME",
                left + 18,
                volumeY + 7,
                MUTED,
                false
        );

        drawButton(
                graphics,
                volumeDown,
                "-",
                volumeDown.contains(
                        mouseX,
                        mouseY
                ),
                false
        );

        String volumeText =
                String.format(
                        Locale.ROOT,
                        "%d%%",
                        Math.round(
                                pendingVolume
                                        * 100.0
                        )
                );

        graphics.drawCenteredString(
                font,
                volumeText,
                left + 230,
                volumeY + 7,
                TEXT
        );

        drawButton(
                graphics,
                volumeUp,
                "+",
                volumeUp.contains(
                        mouseX,
                        mouseY
                ),
                false
        );

        graphics.drawString(
                font,
                "20 ms radio frames · 8 kHz G.711 μ-law · jitter buffer",
                left + 316,
                volumeY + 7,
                MUTED,
                false
        );

        drawButton(
                graphics,
                applyButton,
                "APPLY",
                applyButton.contains(
                        mouseX,
                        mouseY
                ),
                false
        );

        drawButton(
                graphics,
                testButton,
                "TEST TONE",
                testButton.contains(
                        mouseX,
                        mouseY
                ),
                false
        );

        drawButton(
                graphics,
                resetButton,
                "RESET",
                resetButton.contains(
                        mouseX,
                        mouseY
                ),
                false
        );

        drawButton(
                graphics,
                doneButton,
                "APPLY & CLOSE",
                doneButton.contains(
                        mouseX,
                        mouseY
                ),
                true
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void drawDeviceColumn(
            GuiGraphics graphics,
            Rect rect,
            String title,
            List<RadioAudioDevices.DeviceOption> options,
            String selected,
            int scroll,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(
                font,
                title,
                rect.x(),
                rect.y() - 16,
                CYAN,
                false
        );

        graphics.fill(
                rect.x(),
                rect.y(),
                rect.right(),
                rect.bottom(),
                PANEL_2
        );

        outline(
                graphics,
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height(),
                BORDER
        );

        int visible =
                visibleRows(
                        rect
                );

        for (int row = 0;
             row < visible;
             row++) {
            int index =
                    scroll + row;

            if (index >= options.size()) {
                break;
            }

            RadioAudioDevices.DeviceOption option =
                    options.get(
                            index
                    );

            int rowY =
                    rect.y()
                            + row
                            * ROW_HEIGHT;

            Rect rowRect =
                    new Rect(
                            rect.x() + 2,
                            rowY + 2,
                            rect.width() - 4,
                            ROW_HEIGHT - 4
                    );

            boolean selectedRow =
                    option.id()
                            .equals(
                                    selected
                            );

            boolean hover =
                    rowRect.contains(
                            mouseX,
                            mouseY
                    );

            int background =
                    selectedRow
                            ? 0xFF1B4A42
                            : hover
                            ? PANEL_3
                            : PANEL_2;

            graphics.fill(
                    rowRect.x(),
                    rowRect.y(),
                    rowRect.right(),
                    rowRect.bottom(),
                    background
            );

            if (selectedRow) {
                graphics.fill(
                        rowRect.x(),
                        rowRect.y(),
                        rowRect.x() + 3,
                        rowRect.bottom(),
                        GREEN
                );
            }

            graphics.drawString(
                    font,
                    ellipsize(
                            option.label(),
                            rowRect.width() - 16
                    ),
                    rowRect.x() + 8,
                    rowRect.y() + 5,
                    selectedRow
                            ? GREEN
                            : TEXT,
                    false
            );

            if (!option.detail()
                    .isBlank()
                    && rowRect.height() >= 24) {
                graphics.drawString(
                        font,
                        ellipsize(
                                option.detail(),
                                rowRect.width() - 16
                        ),
                        rowRect.x() + 8,
                        rowRect.y() + 15,
                        MUTED,
                        false
                );
            }
        }

        if (options.size() > visible) {
            int barX =
                    rect.right() - 4;

            graphics.fill(
                    barX,
                    rect.y() + 2,
                    barX + 2,
                    rect.bottom() - 2,
                    0xFF253541
            );

            int track =
                    rect.height() - 4;

            int thumb =
                    Math.max(
                            16,
                            track
                                    * visible
                                    / options.size()
                    );

            int maxScroll =
                    Math.max(
                            1,
                            options.size()
                                    - visible
                    );

            int thumbY =
                    rect.y() + 2
                            + (
                            track - thumb
                    )
                            * scroll
                            / maxScroll;

            graphics.fill(
                    barX,
                    thumbY,
                    barX + 2,
                    thumbY + thumb,
                    CYAN
            );
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        int outputIndex =
                clickedIndex(
                        outputList,
                        outputScroll,
                        outputOptions,
                        mouseX,
                        mouseY
                );

        if (outputIndex >= 0) {
            pendingOutput =
                    outputOptions.get(
                            outputIndex
                    )
                            .id();

            status =
                    "Output selected: "
                            + outputOptions.get(
                            outputIndex
                    )
                            .label();

            statusColor =
                    CYAN;

            return true;
        }

        int inputIndex =
                clickedIndex(
                        inputList,
                        inputScroll,
                        inputOptions,
                        mouseX,
                        mouseY
                );

        if (inputIndex >= 0) {
            pendingInput =
                    inputOptions.get(
                            inputIndex
                    )
                            .id();

            status =
                    "Microphone selected: "
                            + inputOptions.get(
                            inputIndex
                    )
                            .label();

            statusColor =
                    CYAN;

            return true;
        }

        if (volumeDown.contains(
                mouseX,
                mouseY
        )) {
            pendingVolume =
                    clampVolume(
                            pendingVolume
                                    - 0.10
                    );

            return true;
        }

        if (volumeUp.contains(
                mouseX,
                mouseY
        )) {
            pendingVolume =
                    clampVolume(
                            pendingVolume
                                    + 0.10
                    );

            return true;
        }

        if (applyButton.contains(
                mouseX,
                mouseY
        )) {
            applySettings();

            return true;
        }

        if (testButton.contains(
                mouseX,
                mouseY
        )) {
            applySettings();

            RadioVoiceClient.get()
                    .playLocalTestTone();

            status =
                    "700 Hz radio test tone queued.";

            statusColor =
                    GREEN;

            return true;
        }

        if (resetButton.contains(
                mouseX,
                mouseY
        )) {
            RadioAudioSettings.reset();

            pendingOutput =
                    RadioAudioSettings.outputDevice();

            pendingInput =
                    RadioAudioSettings.inputDevice();

            pendingVolume =
                    RadioAudioSettings.outputVolume();

            RadioVoiceClient.get()
                    .reloadAudioSettings();

            status =
                    "Radio audio reset to defaults.";

            statusColor =
                    AMBER;

            return true;
        }

        if (doneButton.contains(
                mouseX,
                mouseY
        )) {
            applySettings();
            onClose();

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (outputList.contains(
                mouseX,
                mouseY
        )) {
            outputScroll =
                    clampScroll(
                            outputScroll
                                    + (
                                    delta > 0.0
                                            ? -1
                                            : 1
                            ),
                            outputOptions,
                            outputList
                    );

            return true;
        }

        if (inputList.contains(
                mouseX,
                mouseY
        )) {
            inputScroll =
                    clampScroll(
                            inputScroll
                                    + (
                                    delta > 0.0
                                            ? -1
                                            : 1
                            ),
                            inputOptions,
                            inputList
                    );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode
                == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            onClose();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applySettings() {
        RadioAudioSettings.setOutputDevice(
                pendingOutput
        );

        RadioAudioSettings.setInputDevice(
                pendingInput
        );

        RadioAudioSettings.setOutputVolume(
                pendingVolume
        );

        RadioAudioSettings.save();

        RadioVoiceClient.get()
                .reloadAudioSettings();

        status =
                "Audio settings applied.";

        statusColor =
                GREEN;
    }

    private int clickedIndex(
            Rect rect,
            int scroll,
            List<RadioAudioDevices.DeviceOption> options,
            double mouseX,
            double mouseY
    ) {
        if (!rect.contains(
                mouseX,
                mouseY
        )) {
            return -1;
        }

        int row =
                (
                        (int) mouseY
                                - rect.y()
                )
                        / ROW_HEIGHT;

        int index =
                scroll + row;

        if (index < 0
                || index >= options.size()) {
            return -1;
        }

        return index;
    }

    private int clampScroll(
            int value,
            List<?> options,
            Rect rect
    ) {
        int max =
                Math.max(
                        0,
                        options.size()
                                - visibleRows(
                                rect
                        )
                );

        return Math.max(
                0,
                Math.min(
                        max,
                        value
                )
        );
    }

    private int visibleRows(
            Rect rect
    ) {
        return Math.max(
                1,
                rect.height()
                        / ROW_HEIGHT
        );
    }

    private double clampVolume(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        2.0,
                        Math.round(
                                value
                                        * 10.0
                        )
                                / 10.0
                )
        );
    }

    private void drawButton(
            GuiGraphics graphics,
            Rect rect,
            String label,
            boolean hover,
            boolean primary
    ) {
        int background =
                primary
                        ? hover
                        ? 0xFF207A67
                        : 0xFF195E51
                        : hover
                        ? PANEL_3
                        : PANEL_2;

        graphics.fill(
                rect.x(),
                rect.y(),
                rect.right(),
                rect.bottom(),
                background
        );

        outline(
                graphics,
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height(),
                primary
                        ? GREEN
                        : BORDER
        );

        graphics.drawCenteredString(
                font,
                label,
                rect.x()
                        + rect.width()
                        / 2,
                rect.y() + 9,
                primary
                        ? 0xFFFFFFFF
                        : TEXT
        );
    }

    private String ellipsize(
            String value,
            int maxWidth
    ) {
        if (value == null) {
            return "";
        }

        if (font.width(
                value
        ) <= maxWidth) {
            return value;
        }

        String suffix =
                "...";

        int suffixWidth =
                font.width(
                        suffix
                );

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < value.length();
             i++) {
            char next =
                    value.charAt(
                            i
                    );

            if (font.width(
                    builder.toString()
                            + next
            )
                    + suffixWidth
                    > maxWidth) {
                break;
            }

            builder.append(
                    next
            );
        }

        return builder
                + suffix;
    }

    private static void outline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.fill(
                x,
                y,
                x + width,
                y + 1,
                color
        );

        graphics.fill(
                x,
                y + height - 1,
                x + width,
                y + height,
                color
        );

        graphics.fill(
                x,
                y,
                x + 1,
                y + height,
                color
        );

        graphics.fill(
                x + width - 1,
                y,
                x + width,
                y + height,
                color
        );
    }

    private record Rect(
            int x,
            int y,
            int width,
            int height
    ) {
        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(
                double mouseX,
                double mouseY
        ) {
            return mouseX >= x
                    && mouseX < right()
                    && mouseY >= y
                    && mouseY < bottom();
        }
    }
}
