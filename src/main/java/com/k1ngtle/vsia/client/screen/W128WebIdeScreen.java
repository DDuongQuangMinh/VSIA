package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.client.web.W128CodeEditor;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W128IdeRequestPacket;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import com.k1ngtle.vsia.network.web.W129IdeEventPacket;
import com.k1ngtle.vsia.signality.internet.web.W128CodeFormatter;
import com.k1ngtle.vsia.signality.internet.web.W128IdeAction;
import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;
import com.k1ngtle.vsia.signality.internet.web.W129ComputeEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class W128WebIdeScreen extends Screen {
    private static final int MAX_FILE_CHARACTERS = 262_144;
    private static final int ACTIVITY_WIDTH = 38;
    private static final int SIDEBAR_WIDTH = 255;
    private static final int TOP = 54;
    private static final int STATUS_HEIGHT = 22;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int BOTTOM_PANEL_HEIGHT = 170;
    private static final int FILE_ROWS = 14;
    private static final int MAX_OPEN_TABS = 8;
    private static final int MAX_PANEL_LINES = 11;

    private String host;
    private String mode;
    private String boundServerIp;
    private boolean published;
    private long buildRevision;

    private String activePath;
    private String serverContent;
    private boolean activeGenerated;
    private List<W128IdeSnapshotPacket.FileEntry> files;

    private String status;
    private boolean statusSuccess;

    private W128CodeEditor editor;
    private EditBox pathField;
    private EditBox goLineField;
    private EditBox searchField;
    private EditBox terminalField;
    private EditBox paletteField;
    private Button paletteRunButton;

    private final List<String> openTabs = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();
    private final List<String> terminalLines = new ArrayList<>();
    private final List<String> problemLines = new ArrayList<>();
    private final List<String> outputLines = new ArrayList<>();

    private SideMode sideMode = SideMode.EXPLORER;
    private BottomMode bottomMode = BottomMode.TERMINAL;

    private boolean bottomPanelVisible = true;
    private boolean paletteVisible;
    private boolean prettyGenerated = true;

    private int filePage;
    private int pendingGoLine = -1;
    private String editorBaseline = "";

    public W128WebIdeScreen(W128IdeSnapshotPacket packet) {
        super(Component.literal("VS:IA Development Platform"));
        host = packet.host();
        applySnapshot(packet);
        rememberTab(activePath);
    }

    public String host() {
        return host;
    }

    @Override
    protected void init() {
        clearWidgets();

        int sidebarX = ACTIVITY_WIDTH;
        int editorX = ACTIVITY_WIDTH + SIDEBAR_WIDTH + 8;
        int editorTop = TOP;
        int bottomPanelTop = bottomPanelVisible
                ? height - STATUS_HEIGHT - BOTTOM_PANEL_HEIGHT
                : height - STATUS_HEIGHT;

        int toolbarY = bottomPanelTop - TOOLBAR_HEIGHT;
        int editorHeight = Math.max(
                90,
                toolbarY - editorTop - 2
        );

        addActivityBar();
        addSidePanel(sidebarX);

        addEditorTabs(editorX);

        editor = new W128CodeEditor(
                font,
                editorX,
                editorTop,
                Math.max(180, width - editorX - 8),
                editorHeight,
                MAX_FILE_CHARACTERS
        );

        editorBaseline = displayValue();
        editor.setValue(editorBaseline);
        editor.setEditable(true);
        editor.setLanguage(
                W128IdeLanguage.detect(activePath)
        );
        addRenderableWidget(editor);

        addToolbar(toolbarY);

        if (bottomPanelVisible) {
            addBottomPanel(bottomPanelTop);
        }

        goLineField = new EditBox(
                font,
                Math.max(editorX + 20, width - 154),
                29,
                60,
                18,
                Component.literal("Line")
        );
        goLineField.setMaxLength(7);
        goLineField.setFilter(
                value ->
                        value.isEmpty()
                                || value.matches("\\d+")
        );
        addRenderableWidget(goLineField);

        addRenderableWidget(
                Button.builder(
                                Component.literal("Go line"),
                                button -> goToLine()
                        )
                        .bounds(
                                Math.max(editorX + 84, width - 90),
                                29,
                                80,
                                18
                        )
                        .build()
        );

        paletteField = new EditBox(
                font,
                Math.max(80, width / 2 - 230),
                58,
                390,
                20,
                Component.literal("Command Palette")
        );
        paletteField.setMaxLength(128);
        paletteField.setHint(
                Component.literal(
                        "Type command: run, save, build, search, terminal..."
                )
        );
        paletteField.visible = paletteVisible;
        addRenderableWidget(paletteField);

        paletteRunButton = Button.builder(
                        Component.literal("Run Command"),
                        button -> executePalette()
                )
                .bounds(
                        Math.max(474, width / 2 + 164),
                        58,
                        110,
                        20
                )
                .build();
        paletteRunButton.visible = paletteVisible;
        addRenderableWidget(paletteRunButton);

        if (paletteVisible) {
            setInitialFocus(paletteField);
        }

        if (pendingGoLine > 0
                && editor != null) {
            editor.goToLine(pendingGoLine);
            pendingGoLine = -1;
        }
    }

    private void addActivityBar() {
        int y = 54;

        y = activityButton(
                y,
                "EX",
                "Explorer",
                SideMode.EXPLORER
        );
        y = activityButton(
                y,
                "SE",
                "Search",
                SideMode.SEARCH
        );
        y = activityButton(
                y,
                "RUN",
                "Run and Debug",
                SideMode.RUN
        );
        y = activityButton(
                y,
                "SC",
                "Source Control",
                SideMode.SOURCE_CONTROL
        );
        y = activityButton(
                y,
                "EXT",
                "Extensions",
                SideMode.EXTENSIONS
        );
        activityButton(
                y,
                "CFG",
                "Settings",
                SideMode.SETTINGS
        );
    }

    private int activityButton(
            int y,
            String label,
            String tooltip,
            SideMode target
    ) {
        Button button = Button.builder(
                        Component.literal(label),
                        ignored -> switchSide(target)
                )
                .bounds(
                        3,
                        y,
                        ACTIVITY_WIDTH - 6,
                        26
                )
                .build();

        button.setTooltip(
                net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal(tooltip)
                )
        );

        button.active = sideMode != target;
        addRenderableWidget(button);

        return y + 30;
    }

    private void addSidePanel(int sidebarX) {
        switch (sideMode) {
            case EXPLORER ->
                    addExplorer(sidebarX);
            case SEARCH ->
                    addSearch(sidebarX);
            case RUN ->
                    addRunPanel(sidebarX);
            case SOURCE_CONTROL ->
                    addSourceControl(sidebarX);
            case EXTENSIONS ->
                    addExtensions(sidebarX);
            case SETTINGS ->
                    addSettings(sidebarX);
        }
    }

    private void addExplorer(int sidebarX) {
        pathField = new EditBox(
                font,
                sidebarX + 8,
                TOP,
                SIDEBAR_WIDTH - 104,
                18,
                Component.literal("Path")
        );
        pathField.setMaxLength(512);
        pathField.setValue(
                activePath.isBlank()
                        ? "/new-file.txt"
                        : activePath
        );
        addRenderableWidget(pathField);

        addRenderableWidget(
                Button.builder(
                                Component.literal("New"),
                                ignored -> createFile()
                        )
                        .bounds(
                                sidebarX + SIDEBAR_WIDTH - 90,
                                TOP,
                                38,
                                18
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Ren"),
                                ignored -> renameFile()
                        )
                        .bounds(
                                sidebarX + SIDEBAR_WIDTH - 48,
                                TOP,
                                40,
                                18
                        )
                        .build()
        );

        addFileButtons(sidebarX);
    }

    private void addFileButtons(int sidebarX) {
        int start = filePage * FILE_ROWS;
        int y = TOP + 24;

        for (int i = 0; i < FILE_ROWS; i++) {
            int index = start + i;

            if (index >= files.size()) {
                break;
            }

            W128IdeSnapshotPacket.FileEntry entry =
                    files.get(index);

            String marker = entry.generated()
                    ? "G "
                    : (
                    managedOutput(entry.path())
                            ? "M "
                            : "  "
            );

            Button fileButton = Button.builder(
                            Component.literal(
                                    marker
                                            + trimLabel(
                                            entry.path(),
                                            33
                                    )
                            ),
                            ignored -> openFile(
                                    entry.path()
                            )
                    )
                    .bounds(
                            sidebarX + 8,
                            y,
                            SIDEBAR_WIDTH - 16,
                            18
                    )
                    .build();

            if (entry.path().equals(activePath)) {
                fileButton.active = false;
            }

            addRenderableWidget(fileButton);
            y += 20;
        }

        int pages = Math.max(
                1,
                (files.size() + FILE_ROWS - 1)
                        / FILE_ROWS
        );

        int pageY =
                TOP
                        + 24
                        + FILE_ROWS * 20
                        + 2;

        Button previous = Button.builder(
                        Component.literal("<"),
                        ignored -> {
                            if (filePage > 0) {
                                filePage--;
                                reinitPreservingEditor();
                            }
                        }
                )
                .bounds(
                        sidebarX + 8,
                        pageY,
                        30,
                        18
                )
                .build();

        previous.active = filePage > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(
                        Component.literal(">"),
                        ignored -> {
                            if (filePage + 1 < pages) {
                                filePage++;
                                reinitPreservingEditor();
                            }
                        }
                )
                .bounds(
                        sidebarX + SIDEBAR_WIDTH - 38,
                        pageY,
                        30,
                        18
                )
                .build();

        next.active =
                filePage + 1 < pages;

        addRenderableWidget(next);
    }

    private void addSearch(int sidebarX) {
        searchField = new EditBox(
                font,
                sidebarX + 8,
                TOP,
                SIDEBAR_WIDTH - 64,
                18,
                Component.literal("Search workspace")
        );
        searchField.setMaxLength(128);
        addRenderableWidget(searchField);

        addRenderableWidget(
                Button.builder(
                                Component.literal("Go"),
                                ignored -> searchWorkspace()
                        )
                        .bounds(
                                sidebarX
                                        + SIDEBAR_WIDTH
                                        - 52,
                                TOP,
                                44,
                                18
                        )
                        .build()
        );

        int y = TOP + 24;
        int count = 0;

        for (SearchResult result : searchResults) {
            if (count >= 16) {
                break;
            }

            String label =
                    trimLabel(
                            result.path(),
                            20
                    )
                            + ":"
                            + result.line();

            addRenderableWidget(
                    Button.builder(
                                    Component.literal(label),
                                    ignored -> {
                                        pendingGoLine =
                                                result.line();
                                        openFile(
                                                result.path()
                                        );
                                    }
                            )
                            .bounds(
                                    sidebarX + 8,
                                    y,
                                    SIDEBAR_WIDTH - 16,
                                    18
                            )
                            .build()
            );

            y += 20;
            count++;
        }
    }

    private void addRunPanel(int sidebarX) {
        int y = TOP;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Run Current"),
                                ignored -> runCurrent()
                        )
                        .bounds(
                                sidebarX + 8,
                                y,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );

        y += 24;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Check / Validate"),
                                ignored -> validateCurrent()
                        )
                        .bounds(
                                sidebarX + 8,
                                y,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );

        y += 24;

        addRenderableWidget(
                Button.builder(
                                Component.literal("W1.29 Self Test"),
                                ignored -> sendTerminal(
                                        "selftest"
                                )
                        )
                        .bounds(
                                sidebarX + 8,
                                y,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );

        y += 24;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Open Terminal"),
                                ignored -> {
                                    bottomMode =
                                            BottomMode.TERMINAL;
                                    bottomPanelVisible =
                                            true;
                                    reinitPreservingEditor();
                                }
                        )
                        .bounds(
                                sidebarX + 8,
                                y,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );
    }

    private void addSourceControl(int sidebarX) {
        addRenderableWidget(
                Button.builder(
                                Component.literal("Build Project"),
                                ignored -> build()
                        )
                        .bounds(
                                sidebarX + 8,
                                TOP,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        published
                                                ? "Unpublish"
                                                : "Publish"
                                ),
                                ignored -> {
                                    if (published) {
                                        unpublish();
                                    } else {
                                        publish();
                                    }
                                }
                        )
                        .bounds(
                                sidebarX + 8,
                                TOP + 24,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );
    }

    private void addExtensions(int sidebarX) {
        addRenderableWidget(
                Button.builder(
                                Component.literal("Refresh Capabilities"),
                                ignored -> setLocalStatus(
                                        "Built-in W1.29 language capabilities refreshed.",
                                        true
                                )
                        )
                        .bounds(
                                sidebarX + 8,
                                TOP,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );
    }

    private void addSettings(int sidebarX) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        bottomPanelVisible
                                                ? "Hide Bottom Panel"
                                                : "Show Bottom Panel"
                                ),
                                ignored -> {
                                    bottomPanelVisible =
                                            !bottomPanelVisible;
                                    reinitPreservingEditor();
                                }
                        )
                        .bounds(
                                sidebarX + 8,
                                TOP,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        prettyGenerated
                                                ? "Generated: Pretty"
                                                : "Generated: Raw"
                                ),
                                ignored -> {
                                    if (dirty()) {
                                        setLocalStatus(
                                                "Save or Reload before changing generated-file display mode.",
                                                false
                                        );
                                        return;
                                    }

                                    prettyGenerated =
                                            !prettyGenerated;
                                    reinitPreservingEditor();
                                }
                        )
                        .bounds(
                                sidebarX + 8,
                                TOP + 24,
                                SIDEBAR_WIDTH - 16,
                                20
                        )
                        .build()
        );
    }

    private void addEditorTabs(int editorX) {
        int x = editorX;
        int available =
                Math.max(
                        120,
                        width
                                - editorX
                                - 170
                );

        int shown = 0;

        for (String path : openTabs) {
            if (shown >= MAX_OPEN_TABS) {
                break;
            }

            int tabWidth =
                    Math.min(
                            150,
                            Math.max(
                                    74,
                                    font.width(
                                            baseName(path)
                                    ) + 20
                            )
                    );

            if (x + tabWidth > editorX + available) {
                break;
            }

            Button tab = Button.builder(
                            Component.literal(
                                    baseName(path)
                                            + (
                                            path.equals(activePath)
                                                    && dirty()
                                                    ? " *"
                                                    : ""
                                    )
                            ),
                            ignored -> openFile(path)
                    )
                    .bounds(
                            x,
                            29,
                            tabWidth,
                            20
                    )
                    .build();

            tab.active =
                    !path.equals(activePath);

            addRenderableWidget(tab);

            x += tabWidth + 2;
            shown++;
        }
    }

    private void addToolbar(int toolbarY) {
        int x = ACTIVITY_WIDTH + SIDEBAR_WIDTH + 8;

        x = toolbarButton(
                x,
                toolbarY,
                54,
                "Save",
                this::save
        );

        x = toolbarButton(
                x,
                toolbarY,
                54,
                "Run",
                this::runCurrent
        );

        x = toolbarButton(
                x,
                toolbarY,
                58,
                "Check",
                this::validateCurrent
        );

        x = toolbarButton(
                x,
                toolbarY,
                58,
                "Build",
                this::build
        );

        x = toolbarButton(
                x,
                toolbarY,
                70,
                published
                        ? "Unpublish"
                        : "Publish",
                published
                        ? this::unpublish
                        : this::publish
        );

        x = toolbarButton(
                x,
                toolbarY,
                58,
                "Reload",
                this::refresh
        );

        x = toolbarButton(
                x,
                toolbarY,
                58,
                activeGenerated
                        ? (
                        prettyGenerated
                                ? "Raw"
                                : "Pretty"
                )
                        : "Format",
                this::formatOrToggle
        );

        if (managedOutput(activePath)) {
            x = toolbarButton(
                    x,
                    toolbarY,
                    74,
                    "Regenerate",
                    this::regenerate
            );
        }

        toolbarButton(
                x,
                toolbarY,
                54,
                "Delete",
                this::deleteFile
        );
    }

    private int toolbarButton(
            int x,
            int y,
            int buttonWidth,
            String label,
            Runnable action
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                ignored -> action.run()
                        )
                        .bounds(
                                x,
                                y + 4,
                                buttonWidth,
                                20
                        )
                        .build()
        );

        return x + buttonWidth + 3;
    }

    private void addBottomPanel(int panelTop) {
        int x =
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH
                        + 8;

        int y = panelTop + 4;

        addRenderableWidget(
                bottomTabButton(
                        x,
                        y,
                        72,
                        "Terminal",
                        BottomMode.TERMINAL
                )
        );
        x += 75;

        addRenderableWidget(
                bottomTabButton(
                        x,
                        y,
                        72,
                        "Problems",
                        BottomMode.PROBLEMS
                )
        );
        x += 75;

        addRenderableWidget(
                bottomTabButton(
                        x,
                        y,
                        62,
                        "Output",
                        BottomMode.OUTPUT
                )
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("X"),
                                ignored -> {
                                    bottomPanelVisible =
                                            false;
                                    reinitPreservingEditor();
                                }
                        )
                        .bounds(
                                width - 31,
                                y,
                                22,
                                18
                        )
                        .build()
        );

        if (bottomMode == BottomMode.TERMINAL) {
            terminalField = new EditBox(
                    font,
                    ACTIVITY_WIDTH
                            + SIDEBAR_WIDTH
                            + 16,
                    height
                            - STATUS_HEIGHT
                            - 25,
                    Math.max(
                            120,
                            width
                                    - ACTIVITY_WIDTH
                                    - SIDEBAR_WIDTH
                                    - 88
                    ),
                    18,
                    Component.literal("Terminal command")
            );
            terminalField.setMaxLength(512);
            terminalField.setHint(
                    Component.literal(
                            "help | run | check | build | publish | selftest"
                    )
            );
            addRenderableWidget(terminalField);

            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Enter"),
                                    ignored -> executeTerminalField()
                            )
                            .bounds(
                                    width - 67,
                                    height
                                            - STATUS_HEIGHT
                                            - 25,
                                    58,
                                    18
                            )
                            .build()
            );
        }
    }

    private Button bottomTabButton(
            int x,
            int y,
            int buttonWidth,
            String label,
            BottomMode target
    ) {
        Button button = Button.builder(
                        Component.literal(label),
                        ignored -> {
                            bottomMode = target;
                            reinitPreservingEditor();
                        }
                )
                .bounds(
                        x,
                        y,
                        buttonWidth,
                        18
                )
                .build();

        button.active =
                bottomMode != target;

        return button;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.fill(
                0,
                0,
                width,
                26,
                0xF011161D
        );

        graphics.fill(
                0,
                26,
                ACTIVITY_WIDTH,
                height - STATUS_HEIGHT,
                0xF0181D25
        );

        graphics.fill(
                ACTIVITY_WIDTH,
                26,
                ACTIVITY_WIDTH + SIDEBAR_WIDTH,
                height - STATUS_HEIGHT,
                0xEE151B23
        );

        graphics.fill(
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH,
                26,
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH
                        + 1,
                height - STATUS_HEIGHT,
                0xFF3B4654
        );

        graphics.fill(
                0,
                height - STATUS_HEIGHT,
                width,
                height,
                0xF010141A
        );

        graphics.drawString(
                font,
                "VS:IA CODE",
                8,
                8,
                0x6FD7FF,
                false
        );

        String projectState =
                host
                        + " | "
                        + mode
                        + " | "
                        + (
                        published
                                ? "PUBLISHED"
                                : "DRAFT"
                )
                        + " | revision "
                        + buildRevision;

        graphics.drawString(
                font,
                projectState,
                104,
                8,
                published
                        ? 0x62E38A
                        : 0xFFD166,
                false
        );

        renderSidePanelText(graphics);
        renderEditorHeader(graphics);

        if (bottomPanelVisible) {
            renderBottomPanel(graphics);
        }

        renderStatusBar(graphics);

        if (paletteVisible) {
            int left =
                    Math.max(
                            70,
                            width / 2 - 240
                    );

            graphics.fill(
                    left - 6,
                    52,
                    Math.min(
                            width - 8,
                            left + 520
                    ),
                    86,
                    0xF01B222C
            );

            graphics.drawString(
                    font,
                    "COMMAND PALETTE",
                    left,
                    54,
                    0xC7D0DB,
                    false
            );
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderSidePanelText(
            GuiGraphics graphics
    ) {
        int x = ACTIVITY_WIDTH + 8;

        graphics.drawString(
                font,
                sideMode.title(),
                x,
                33,
                0xC7D0DB,
                false
        );

        switch (sideMode) {
            case RUN -> {
                W128IdeLanguage language =
                        W128IdeLanguage.detect(
                                activePath
                        );

                graphics.drawString(
                        font,
                        "Current:",
                        x,
                        TOP + 110,
                        0x74879A,
                        false
                );

                graphics.drawString(
                        font,
                        trimLabel(
                                activePath,
                                28
                        ),
                        x,
                        TOP + 124,
                        0xE7EDF5,
                        false
                );

                graphics.drawString(
                        font,
                        W129ComputeEngine.runtimeName(
                                activePath
                        ),
                        x,
                        TOP + 140,
                        W129ComputeEngine.executable(
                                activePath
                        )
                                ? 0x62E38A
                                : 0xFFD166,
                        false
                );

                graphics.drawString(
                        font,
                        W129ComputeEngine.executable(
                                activePath
                        )
                                ? "sandbox executable"
                                : (
                                language.webRuntime()
                                        ? "web runtime"
                                        : "editor-only"
                        ),
                        x,
                        TOP + 154,
                        0x8FA9BD,
                        false
                );
            }

            case SOURCE_CONTROL -> {
                int y = TOP + 58;

                graphics.drawString(
                        font,
                        "WORKING TREE",
                        x,
                        y,
                        0x8FA9BD,
                        false
                );

                y += 16;

                for (W128IdeSnapshotPacket.FileEntry entry : files) {
                    if (y > height - 70) {
                        break;
                    }

                    if (
                            entry.generated()
                                    || managedOutput(
                                    entry.path()
                            )
                    ) {
                        String state =
                                entry.generated()
                                        ? "G"
                                        : "M";

                        graphics.drawString(
                                font,
                                state
                                        + " "
                                        + trimLabel(
                                        entry.path(),
                                        27
                                ),
                                x,
                                y,
                                entry.generated()
                                        ? 0xFFD166
                                        : 0x62E38A,
                                false
                        );

                        y += 14;
                    }
                }

                graphics.drawString(
                        font,
                        "G=generated M=manual override",
                        x,
                        Math.min(
                                height - 84,
                                y + 8
                        ),
                        0x65788B,
                        false
                );
            }

            case EXTENSIONS -> {
                int y = TOP + 34;

                String[] entries = {
                        "HTML/CSS      Web",
                        "JavaScript    Web",
                        "React JSX     Web",
                        "Assembly      VSIA VM",
                        "Python        Sandbox subset",
                        "C             Sandbox subset",
                        "C++           Sandbox subset",
                        "C#            Sandbox subset",
                        "Java          Sandbox subset",
                        "JSON/MD       Editor"
                };

                for (String entry : entries) {
                    graphics.drawString(
                            font,
                            entry,
                            x,
                            y,
                            0xA7B4C3,
                            false
                    );
                    y += 15;
                }
            }

            case SETTINGS -> {
                graphics.drawString(
                        font,
                        "W1.29 Workbench",
                        x,
                        TOP + 58,
                        0xE7EDF5,
                        false
                );

                graphics.drawString(
                        font,
                        "Syntax highlight: ON",
                        x,
                        TOP + 76,
                        0x62E38A,
                        false
                );

                graphics.drawString(
                        font,
                        "Auto indent: ON",
                        x,
                        TOP + 92,
                        0x62E38A,
                        false
                );

                graphics.drawString(
                        font,
                        "VM isolation: ON",
                        x,
                        TOP + 108,
                        0x62E38A,
                        false
                );

                graphics.drawString(
                        font,
                        "Host compiler exec: OFF",
                        x,
                        TOP + 124,
                        0x62E38A,
                        false
                );
            }

            case SEARCH -> {
                if (searchResults.isEmpty()) {
                    graphics.drawString(
                            font,
                            "Ctrl+Shift+F",
                            x,
                            TOP + 26,
                            0x65788B,
                            false
                    );
                }
            }

            case EXPLORER -> {
            }
        }
    }

    private void renderEditorHeader(
            GuiGraphics graphics
    ) {
        int editorX =
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH
                        + 8;

        W128IdeLanguage language =
                W128IdeLanguage.detect(
                        activePath
                );

        String runtime =
                W129ComputeEngine.executable(
                        activePath
                )
                        ? "VSIA compute"
                        : (
                        language.webRuntime()
                                ? "web-runtime"
                                : "editor"
                );

        String management =
                activeGenerated
                        ? " | GENERATED"
                        : (
                        managedOutput(activePath)
                                ? " | MANUAL OVERRIDE"
                                : ""
                );

        String right =
                language.displayName()
                        + " | "
                        + runtime
                        + management;

        graphics.drawString(
                font,
                trimLabel(
                        activePath,
                        60
                ),
                editorX,
                51,
                activeGenerated
                        ? 0xFFD166
                        : 0xE8F3FF,
                false
        );

        graphics.drawString(
                font,
                right,
                Math.max(
                        editorX + 150,
                        width
                                - font.width(right)
                                - 12
                ),
                51,
                activeGenerated
                        ? 0xFFD166
                        : 0x8FA9BD,
                false
        );
    }

    private void renderBottomPanel(
            GuiGraphics graphics
    ) {
        int top =
                height
                        - STATUS_HEIGHT
                        - BOTTOM_PANEL_HEIGHT;

        int left =
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH;

        graphics.fill(
                left,
                top,
                width,
                height - STATUS_HEIGHT,
                0xF00C1016
        );

        graphics.fill(
                left,
                top,
                width,
                top + 1,
                0xFF3B4654
        );

        List<String> lines =
                switch (bottomMode) {
                    case TERMINAL ->
                            terminalLines;
                    case PROBLEMS ->
                            problemLines;
                    case OUTPUT ->
                            outputLines;
                };

        int y = top + 28;

        int start =
                Math.max(
                        0,
                        lines.size()
                                - MAX_PANEL_LINES
                );

        for (int i = start;
             i < lines.size();
             i++) {
            String line =
                    lines.get(i);

            graphics.drawString(
                    font,
                    trimLabel(
                            line,
                            150
                    ),
                    left + 16,
                    y,
                    colorForPanelLine(
                            bottomMode,
                            line
                    ),
                    false
            );

            y += 12;
        }
    }

    private void renderStatusBar(
            GuiGraphics graphics
    ) {
        int y =
                height
                        - STATUS_HEIGHT
                        + 6;

        String cursor =
                editor == null
                        ? ""
                        : "Ln "
                        + editor.cursorLine()
                        + ", Col "
                        + editor.cursorColumn();

        graphics.drawString(
                font,
                status
                        + (
                        dirty()
                                ? " | UNSAVED"
                                : ""
                ),
                8,
                y,
                statusSuccess
                        ? (
                        dirty()
                                ? 0xFFD166
                                : 0x62E38A
                )
                        : 0xFF6B6B,
                false
        );

        String right =
                cursor
                        + " | "
                        + W128IdeLanguage.detect(
                        activePath
                ).displayName()
                        + " | Ctrl+Shift+P Commands | Ctrl+` Terminal";

        graphics.drawString(
                font,
                right,
                Math.max(
                        180,
                        width
                                - font.width(right)
                                - 8
                ),
                y,
                0x74879A,
                false
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (paletteVisible
                && paletteField != null
                && paletteField.isFocused()
                && (
                keyCode == GLFW.GLFW_KEY_ENTER
                        || keyCode
                        == GLFW.GLFW_KEY_KP_ENTER
        )) {
            executePalette();
            return true;
        }

        if (terminalField != null
                && terminalField.isFocused()
                && (
                keyCode == GLFW.GLFW_KEY_ENTER
                        || keyCode
                        == GLFW.GLFW_KEY_KP_ENTER
        )) {
            executeTerminalField();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_P
                        && Screen.hasControlDown()
                        && Screen.hasShiftDown()
        ) {
            togglePalette();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_F
                        && Screen.hasControlDown()
                        && Screen.hasShiftDown()
        ) {
            sideMode = SideMode.SEARCH;
            reinitPreservingEditor();

            if (searchField != null) {
                setInitialFocus(
                        searchField
                );
            }

            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT
                        && Screen.hasControlDown()
        ) {
            bottomPanelVisible =
                    !bottomPanelVisible;

            bottomMode =
                    BottomMode.TERMINAL;

            reinitPreservingEditor();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_S
                        && Screen.hasControlDown()
        ) {
            save();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5) {
            refresh();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_ENTER
                        && Screen.hasControlDown()
        ) {
            runCurrent();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_W
                        && Screen.hasControlDown()
        ) {
            closeActiveTab();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_F
                        && Screen.hasControlDown()
                        && Screen.hasAltDown()
        ) {
            formatOrToggle();
            return true;
        }

        if (
                keyCode == GLFW.GLFW_KEY_ESCAPE
                        && paletteVisible
        ) {
            hidePalette();
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

    public void acceptSnapshot(
            W128IdeSnapshotPacket packet
    ) {
        String localValue =
                editor == null
                        ? null
                        : editor.getValue();

        String previousPath =
                activePath;

        boolean preserveLocal =
                !packet.success()
                        && localValue != null
                        && previousPath.equals(
                        packet.activePath()
                );

        applySnapshot(packet);
        rememberTab(activePath);

        int pages =
                Math.max(
                        1,
                        (
                                files.size()
                                        + FILE_ROWS
                                        - 1
                        )
                                / FILE_ROWS
                );

        filePage =
                Math.min(
                        filePage,
                        pages - 1
                );

        init();

        if (preserveLocal
                && editor != null) {
            editor.setEditable(true);
            editor.replaceAll(localValue);
        }
    }

    public void acceptW129Event(
            W129IdeEventPacket packet
    ) {
        String kind =
                packet.kind()
                        .toUpperCase(
                                Locale.ROOT
                        );

        switch (kind) {
            case "TERMINAL" -> {
                appendPanelBlock(
                        terminalLines,
                        packet.title(),
                        packet.payload()
                );

                bottomMode =
                        BottomMode.TERMINAL;

                bottomPanelVisible =
                        true;
            }

            case "TERMINAL_CLEAR" ->
                    terminalLines.clear();

            case "PROBLEMS" -> {
                problemLines.clear();

                if (packet.payload().isBlank()) {
                    problemLines.add(
                            "No problems detected."
                    );
                } else {
                    for (String raw
                            : packet.payload()
                            .split("\n")) {
                        String[] parts =
                                raw.split(
                                        "\\|",
                                        4
                                );

                        if (parts.length == 4) {
                            problemLines.add(
                                    parts[0]
                                            + " "
                                            + activePath
                                            + ":"
                                            + parts[1]
                                            + ":"
                                            + parts[2]
                                            + " "
                                            + parts[3]
                            );
                        } else {
                            problemLines.add(raw);
                        }
                    }
                }

                if (!packet.success()) {
                    bottomMode =
                            BottomMode.PROBLEMS;
                    bottomPanelVisible =
                            true;
                }
            }

            case "SEARCH" -> {
                searchResults.clear();

                if (!packet.payload().isBlank()) {
                    for (String raw
                            : packet.payload()
                            .split("\n")) {
                        String[] parts =
                                raw.split(
                                        "\\|",
                                        3
                                );

                        if (parts.length == 3) {
                            try {
                                searchResults.add(
                                        new SearchResult(
                                                parts[0],
                                                Integer.parseInt(
                                                        parts[1]
                                                ),
                                                parts[2]
                                        )
                                );
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                sideMode =
                        SideMode.SEARCH;
            }

            case "OUTPUT" ->
                    appendPanelBlock(
                            outputLines,
                            packet.title(),
                            packet.payload()
                    );

            default ->
                    appendPanelBlock(
                            outputLines,
                            packet.title(),
                            packet.payload()
                    );
        }

        reinitPreservingEditor();
    }

    private void applySnapshot(
            W128IdeSnapshotPacket packet
    ) {
        host = packet.host();
        mode = packet.mode();
        boundServerIp =
                packet.boundServerIp();
        published =
                packet.published();
        buildRevision =
                packet.buildRevision();
        activePath =
                packet.activePath();
        serverContent =
                packet.activeContent();
        activeGenerated =
                packet.activeGenerated();
        files = packet.files();
        status = packet.message();
        statusSuccess =
                packet.success();
    }

    private String displayValue() {
        if (
                activeGenerated
                        && prettyGenerated
                        && W128CodeFormatter.supports(
                        activePath
                )
        ) {
            return W128CodeFormatter.format(
                    activePath,
                    serverContent
            );
        }

        return serverContent == null
                ? ""
                : serverContent;
    }

    private void switchSide(
            SideMode target
    ) {
        sideMode = target;
        reinitPreservingEditor();
    }

    private void openFile(String path) {
        if (path == null
                || path.isBlank()
                || path.equals(activePath)) {
            return;
        }

        if (dirty()) {
            setLocalStatus(
                    "Save or Reload the current file before switching.",
                    false
            );
            return;
        }

        rememberTab(path);

        request(
                W128IdeAction.OPEN,
                path,
                ""
        );
    }

    private void rememberTab(String path) {
        if (path == null
                || path.isBlank()) {
            return;
        }

        openTabs.remove(path);
        openTabs.add(path);

        while (openTabs.size()
                > MAX_OPEN_TABS) {
            openTabs.remove(0);
        }
    }

    private void closeActiveTab() {
        if (dirty()) {
            setLocalStatus(
                    "Save or Reload before closing the tab.",
                    false
            );
            return;
        }

        openTabs.remove(activePath);

        if (!openTabs.isEmpty()) {
            openFile(
                    openTabs.get(
                            openTabs.size() - 1
                    )
            );
        } else {
            setLocalStatus(
                    "Tab closed. Select another file from Explorer.",
                    true
            );
            reinitPreservingEditor();
        }
    }

    private void save() {
        if (editor == null
                || activePath.isBlank()) {
            return;
        }

        if (!dirty()) {
            setLocalStatus(
                    "No changes to save.",
                    true
            );
            return;
        }

        request(
                W128IdeAction.SAVE,
                activePath,
                editor.getValue()
        );
    }

    private void createFile() {
        if (dirty()) {
            setLocalStatus(
                    "Save or Reload before creating another file.",
                    false
            );
            return;
        }

        String value =
                pathField == null
                        ? ""
                        : pathField.getValue();

        request(
                W128IdeAction.CREATE,
                value,
                ""
        );
    }

    private void renameFile() {
        if (dirty()) {
            setLocalStatus(
                    "Save or Reload before renaming.",
                    false
            );
            return;
        }

        if (pathField == null
                || pathField.getValue().isBlank()) {
            setLocalStatus(
                    "Enter the new path in the path box.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.RENAME,
                activePath,
                pathField.getValue()
        );
    }

    private void deleteFile() {
        if (dirty()) {
            setLocalStatus(
                    "Save or Reload before deleting.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.DELETE,
                activePath,
                ""
        );
    }

    private void runCurrent() {
        if (dirty()) {
            setLocalStatus(
                    "Save the file before running it.",
                    false
            );
            return;
        }

        if (!W129ComputeEngine.executable(
                activePath
        )) {
            setLocalStatus(
                    "This file uses the web/editor runtime. Use Build + Publish for web execution.",
                    false
            );
            return;
        }

        bottomMode =
                BottomMode.TERMINAL;
        bottomPanelVisible =
                true;

        request(
                W128IdeAction.RUN,
                activePath,
                ""
        );
    }

    private void validateCurrent() {
        if (dirty()) {
            setLocalStatus(
                    "Save the file before validation.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.VALIDATE,
                activePath,
                ""
        );
    }

    private void searchWorkspace() {
        String query =
                searchField == null
                        ? ""
                        : searchField.getValue();

        request(
                W128IdeAction.SEARCH,
                activePath,
                query
        );
    }

    private void build() {
        if (dirty()) {
            setLocalStatus(
                    "Save before building.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.BUILD,
                activePath,
                ""
        );
    }

    private void publish() {
        if (dirty()) {
            setLocalStatus(
                    "Save before publishing.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.PUBLISH,
                activePath,
                ""
        );
    }

    private void unpublish() {
        request(
                W128IdeAction.UNPUBLISH,
                activePath,
                ""
        );
    }

    private void refresh() {
        if (dirty()) {
            setLocalStatus(
                    "Unsaved changes exist. Save or discard them first.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.REFRESH,
                activePath,
                ""
        );
    }

    private void regenerate() {
        if (dirty()) {
            setLocalStatus(
                    "Save or Reload before regenerating.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.REGENERATE,
                activePath,
                ""
        );
    }

    private void formatOrToggle() {
        if (editor == null
                || !W128CodeFormatter.supports(
                activePath
        )) {
            setLocalStatus(
                    "No formatter is registered for this file type.",
                    false
            );
            return;
        }

        if (activeGenerated) {
            if (dirty()) {
                setLocalStatus(
                        "Save or Reload before switching Pretty/Raw.",
                        false
                );
                return;
            }

            prettyGenerated =
                    !prettyGenerated;
            reinitPreservingEditor();

            setLocalStatus(
                    prettyGenerated
                            ? "Pretty view enabled."
                            : "Raw generated view enabled.",
                    true
            );

            return;
        }

        String formatted =
                W128CodeFormatter.format(
                        activePath,
                        editor.getValue()
                );

        if (formatted.equals(
                editor.getValue()
        )) {
            setLocalStatus(
                    "Formatting produced no changes.",
                    true
            );
            return;
        }

        editor.replaceAll(formatted);

        setLocalStatus(
                "Formatted locally. Review and Save.",
                true
        );
    }

    private void goToLine() {
        if (editor == null
                || goLineField == null
                || goLineField.getValue().isBlank()) {
            return;
        }

        try {
            editor.goToLine(
                    Integer.parseInt(
                            goLineField.getValue()
                    )
            );
        } catch (NumberFormatException ignored) {
        }
    }

    private void executeTerminalField() {
        if (terminalField == null) {
            return;
        }

        String command =
                terminalField.getValue()
                        .trim();

        terminalField.setValue("");

        if (command.isEmpty()) {
            return;
        }

        appendPanelBlock(
                terminalLines,
                "",
                "> " + command
        );

        sendTerminal(command);
    }

    private void sendTerminal(
            String command
    ) {
        bottomMode =
                BottomMode.TERMINAL;
        bottomPanelVisible =
                true;

        request(
                W128IdeAction.TERMINAL,
                activePath,
                command
        );
    }

    private void togglePalette() {
        paletteVisible =
                !paletteVisible;

        if (paletteField != null) {
            paletteField.visible =
                    paletteVisible;

            if (paletteRunButton != null) {
                paletteRunButton.visible =
                        paletteVisible;
            }

            if (paletteVisible) {
                paletteField.setValue("");
                setInitialFocus(
                        paletteField
                );
            } else if (editor != null) {
                setInitialFocus(editor);
            }
        }
    }

    private void hidePalette() {
        paletteVisible = false;

        if (paletteField != null) {
            paletteField.visible = false;
        }

        if (paletteRunButton != null) {
            paletteRunButton.visible = false;
        }

        if (editor != null) {
            setInitialFocus(editor);
        }
    }

    private void executePalette() {
        if (paletteField == null) {
            return;
        }

        String command =
                paletteField.getValue()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (command.startsWith(">")) {
            command =
                    command.substring(1)
                            .trim();
        }

        switch (command) {
            case "save",
                 "file: save" ->
                    save();

            case "run",
                 "run current file" ->
                    runCurrent();

            case "check",
                 "validate",
                 "check current file" ->
                    validateCurrent();

            case "build",
                 "build project" ->
                    build();

            case "publish" ->
                    publish();

            case "reload",
                 "refresh" ->
                    refresh();

            case "format",
                 "format document" ->
                    formatOrToggle();

            case "explorer" -> {
                sideMode =
                        SideMode.EXPLORER;
                reinitPreservingEditor();
            }

            case "search" -> {
                sideMode =
                        SideMode.SEARCH;
                reinitPreservingEditor();
            }

            case "terminal" -> {
                bottomPanelVisible =
                        true;
                bottomMode =
                        BottomMode.TERMINAL;
                reinitPreservingEditor();
            }

            case "problems" -> {
                bottomPanelVisible =
                        true;
                bottomMode =
                        BottomMode.PROBLEMS;
                reinitPreservingEditor();
            }

            case "output" -> {
                bottomPanelVisible =
                        true;
                bottomMode =
                        BottomMode.OUTPUT;
                reinitPreservingEditor();
            }

            case "selftest",
                 "w1.29 self test" ->
                    sendTerminal(
                            "selftest"
                    );

            default ->
                    setLocalStatus(
                            "Unknown command palette entry: "
                                    + command,
                            false
                    );
        }

        hidePalette();
    }

    private void request(
            W128IdeAction action,
            String path,
            String content
    ) {
        VsiaNetwork.sendToServer(
                new W128IdeRequestPacket(
                        action,
                        host,
                        path,
                        content
                )
        );

        setLocalStatus(
                action.name()
                        + " requested...",
                true
        );
    }

    private void reinitPreservingEditor() {
        String local =
                editor == null
                        ? null
                        : editor.getValue();

        boolean preserve =
                editor != null
                        && !local.equals(
                        editorBaseline
                );

        String baseline =
                editorBaseline;

        init();

        if (preserve
                && editor != null) {
            editor.replaceAll(local);
            editorBaseline =
                    baseline;
        }
    }

    private boolean dirty() {
        return editor != null
                && !editor.getValue()
                .equals(editorBaseline);
    }

    private void setLocalStatus(
            String value,
            boolean success
    ) {
        status =
                value == null
                        ? ""
                        : value;
        statusSuccess =
                success;
    }

    private boolean managedOutput(
            String path
    ) {
        if (!"REACT".equalsIgnoreCase(mode)
                || path == null) {
            return false;
        }

        return "/app.js".equals(path)
                || "/index.html".equals(path)
                || "/react-runtime.js".equals(path);
    }

    private static void appendPanelBlock(
            List<String> target,
            String title,
            String payload
    ) {
        if (title != null
                && !title.isBlank()) {
            target.add(
                    "[" + title + "]"
            );
        }

        if (payload != null
                && !payload.isBlank()) {
            for (String line : payload.split(
                    "\n",
                    -1
            )) {
                target.add(line);
            }
        }

        while (target.size() > 300) {
            target.remove(0);
        }
    }

    private static int colorForPanelLine(
            BottomMode mode,
            String line
    ) {
        if (mode == BottomMode.PROBLEMS) {
            if (line.startsWith("ERROR")) {
                return 0xFF6B6B;
            }

            if (line.startsWith("WARNING")) {
                return 0xFFD166;
            }
        }

        if (line.startsWith("[PASS]")) {
            return 0x62E38A;
        }

        if (line.startsWith("[FAIL]")) {
            return 0xFF6B6B;
        }

        if (line.startsWith(">")) {
            return 0x6FD7FF;
        }

        return 0xC7D0DB;
    }

    private static String baseName(
            String path
    ) {
        if (path == null
                || path.isBlank()) {
            return "(none)";
        }

        int slash =
                path.lastIndexOf('/');

        return slash >= 0
                ? path.substring(
                slash + 1
        )
                : path;
    }

    private static String trimLabel(
            String value,
            int max
    ) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return "..."
                + value.substring(
                value.length()
                        - max
                        + 3
        );
    }

    private enum SideMode {
        EXPLORER("EXPLORER"),
        SEARCH("SEARCH"),
        RUN("RUN AND DEBUG"),
        SOURCE_CONTROL("SOURCE CONTROL"),
        EXTENSIONS("EXTENSIONS"),
        SETTINGS("SETTINGS");

        private final String title;

        SideMode(String title) {
            this.title = title;
        }

        private String title() {
            return title;
        }
    }

    private enum BottomMode {
        TERMINAL,
        PROBLEMS,
        OUTPUT
    }

    private record SearchResult(
            String path,
            int line,
            String preview
    ) {
    }
}
