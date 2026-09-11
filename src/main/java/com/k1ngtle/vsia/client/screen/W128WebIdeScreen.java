package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.client.web.W128CodeEditor;
import com.k1ngtle.vsia.client.web.W129IdeButton;
import com.k1ngtle.vsia.client.web.W129IdeEditBox;
import com.k1ngtle.vsia.client.web.W129IdeTheme;
import com.k1ngtle.vsia.client.web.W130CommandRegistry;
import com.k1ngtle.vsia.client.web.W130WorkspaceTree;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W128IdeRequestPacket;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import com.k1ngtle.vsia.network.web.W129IdeEventPacket;
import com.k1ngtle.vsia.signality.internet.web.W128CodeFormatter;
import com.k1ngtle.vsia.signality.internet.web.W128IdeAction;
import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;
import com.k1ngtle.vsia.signality.internet.web.W129ComputeEngine;
import com.k1ngtle.vsia.signality.internet.web.W130DebugSnapshot;
import com.k1ngtle.vsia.signality.internet.web.W130ScmSnapshot;
import com.k1ngtle.vsia.signality.internet.web.W130Workspace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private static final int PANEL_SCROLL_STEP = 3;

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
    private EditBox commitMessageField;
    private Button paletteRunButton;

    private W130DebugSnapshot debugSnapshot = W130DebugSnapshot.idle();
    private W130ScmSnapshot scmSnapshot = W130ScmSnapshot.empty();
    private List<W130CommandRegistry.Command> paletteMatches = List.of();
    private int paletteSelection;

    private final Set<String> expandedFolders = new HashSet<>(Set.of(
            "/src", "/tools", "/native", "/java", "/dotnet", "/asm", "/.vsia"
    ));

    private final List<String> openTabs = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();
    private final List<String> terminalLines = new ArrayList<>();
    private final List<String> problemLines = new ArrayList<>();
    private final List<ProblemTarget> problemTargets = new ArrayList<>();
    private final List<String> outputLines = new ArrayList<>();

    private SideMode sideMode = SideMode.EXPLORER;
    private BottomMode bottomMode = BottomMode.TERMINAL;

    private boolean bottomPanelVisible = true;
    private boolean paletteVisible;
    private boolean prettyGenerated = true;

    private int filePage;
    private int pendingGoLine = -1;
    private int terminalScrollOffset;
    private int problemScrollOffset;
    private int outputScrollOffset;
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
        int editorTop = TOP + 16;
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
        applyDebugDecorations();
        addRenderableWidget(editor);

        addToolbar(toolbarY);

        if (bottomPanelVisible) {
            addBottomPanel(bottomPanelTop);
        }

        goLineField = new W129IdeEditBox(
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
                W129IdeButton.themedBuilder(
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

        int paletteWidth =
                Math.min(
                        620,
                        width - 60
                );

        int paletteX =
                Math.max(
                        ACTIVITY_WIDTH + 12,
                        (width - paletteWidth) / 2
                );

        int paletteY =
                Math.max(
                        62,
                        height / 2 - 135
                );

        paletteField = new W129IdeEditBox(
                font,
                paletteX + 16,
                paletteY + 38,
                Math.max(
                        170,
                        paletteWidth - 144
                ),
                22,
                Component.literal("Command Palette")
        );

        paletteField.setMaxLength(128);
        paletteField.setHint(
                Component.literal(
                        "Type a command or search..."
                )
        );
        paletteField.setResponder(value -> {
            paletteMatches = W130CommandRegistry.search(value, 6);
            paletteSelection = 0;
        });
        paletteMatches = W130CommandRegistry.search("", 6);
        paletteField.visible =
                paletteVisible;
        addRenderableWidget(paletteField);

        paletteRunButton = W129IdeButton.themedBuilder(
                        Component.literal("Run Command"),
                        button -> executePalette()
                )
                .bounds(
                        paletteX + paletteWidth - 118,
                        paletteY + 38,
                        102,
                        22
                )
                .build();

        paletteRunButton.visible =
                paletteVisible;
        addRenderableWidget(
                paletteRunButton
        );

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
        Button button = W129IdeButton.themedBuilder(
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

        ((W129IdeButton) button).setSelected(
                sideMode == target
        );
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
        pathField = new W129IdeEditBox(
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
                W129IdeButton.themedBuilder(
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
                W129IdeButton.themedBuilder(
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
        List<W130WorkspaceTree.Row> rows = W130WorkspaceTree.rows(
                files.stream().map(W128IdeSnapshotPacket.FileEntry::path).toList(),
                expandedFolders
        );

        int start = filePage * FILE_ROWS;
        int y = TOP + 24;

        for (int i = 0; i < FILE_ROWS; i++) {
            int index = start + i;
            if (index >= rows.size()) {
                break;
            }

            W130WorkspaceTree.Row row = rows.get(index);
            String indent = "  ".repeat(Math.min(6, row.depth()));

            if (row.folder()) {
                String label = indent
                        + (expandedFolders.contains(row.path()) ? "v " : "> ")
                        + trimLabel(row.label(), Math.max(12, 27 - row.depth() * 2));

                addRenderableWidget(
                        W129IdeButton.themedBuilder(
                                        Component.literal(label),
                                        ignored -> {
                                            if (!expandedFolders.remove(row.path())) {
                                                expandedFolders.add(row.path());
                                            }
                                            filePage = 0;
                                            reinitPreservingEditor();
                                        }
                                )
                                .bounds(sidebarX + 8, y, SIDEBAR_WIDTH - 16, 18)
                                .build()
                );
            } else {
                W128IdeSnapshotPacket.FileEntry entry = fileEntry(row.path());
                String marker = entry != null && entry.generated()
                        ? "G "
                        : (managedOutput(row.path()) ? "M " : "  ");

                Button fileButton = W129IdeButton.themedBuilder(
                                Component.literal(
                                        indent + marker
                                                + trimLabel(row.label(), Math.max(12, 29 - row.depth() * 2))
                                ),
                                ignored -> openFile(row.path())
                        )
                        .bounds(sidebarX + 8, y, SIDEBAR_WIDTH - 16, 18)
                        .build();

                ((W129IdeButton) fileButton).setSelected(
                        row.path().equals(activePath)
                );
                addRenderableWidget(fileButton);
            }

            y += 20;
        }

        int pages = Math.max(1, (rows.size() + FILE_ROWS - 1) / FILE_ROWS);
        int pageY = TOP + 24 + FILE_ROWS * 20 + 2;

        Button previous = W129IdeButton.themedBuilder(
                        Component.literal("<"),
                        ignored -> {
                            if (filePage > 0) {
                                filePage--;
                                reinitPreservingEditor();
                            }
                        }
                )
                .bounds(sidebarX + 8, pageY, 30, 18)
                .build();
        previous.active = filePage > 0;
        addRenderableWidget(previous);

        Button next = W129IdeButton.themedBuilder(
                        Component.literal(">"),
                        ignored -> {
                            if (filePage + 1 < pages) {
                                filePage++;
                                reinitPreservingEditor();
                            }
                        }
                )
                .bounds(sidebarX + SIDEBAR_WIDTH - 38, pageY, 30, 18)
                .build();
        next.active = filePage + 1 < pages;
        addRenderableWidget(next);
    }

    private W128IdeSnapshotPacket.FileEntry fileEntry(String path) {
        for (W128IdeSnapshotPacket.FileEntry entry : files) {
            if (entry.path().equals(path)) {
                return entry;
            }
        }
        return null;
    }

    private void addSearch(int sidebarX) {
        searchField = new W129IdeEditBox(
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
                W129IdeButton.themedBuilder(
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
                    W129IdeButton.themedBuilder(
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
        boolean paused = debugSnapshot.paused();
        boolean hasSession = hasDebugSession();

        y = runPanelButton(sidebarX, y, "Start Debugging (F5)", this::startDebugging, debuggableActiveFile());
        y = runPanelButton(sidebarX, y, "Run Without Debugging", this::runCurrent, true);
        y = runPanelButton(sidebarX, y, "Continue", this::continueDebugging, paused);
        y = runPanelButton(sidebarX, y, "Step Over (F10)", () -> stepDebugging("debug next"), paused);
        y = runPanelButton(sidebarX, y, "Step Into (F11)", () -> stepDebugging("debug step"), paused);
        y = runPanelButton(sidebarX, y, "Step Out (Shift+F11)", () -> stepDebugging("debug out"), paused);
        y = runPanelButton(sidebarX, y, "Restart", this::restartDebugging, hasSession);
        y = runPanelButton(sidebarX, y, "Stop (Shift+F5)", this::stopDebugging, hasSession);
        y = runPanelButton(sidebarX, y, "Run Build Task", () -> requestW130("task run build"), true);
        runPanelButton(sidebarX, y, "W1.30 Self Test", () -> requestW130("selftest"), true);
    }

    private int runPanelButton(
            int sidebarX,
            int y,
            String label,
            Runnable action,
            boolean enabled
    ) {
        Button button = W129IdeButton.themedBuilder(
                        Component.literal(label),
                        ignored -> action.run()
                )
                .bounds(sidebarX + 8, y, SIDEBAR_WIDTH - 16, 20)
                .build();
        button.active = enabled;
        addRenderableWidget(button);
        return y + 22;
    }

    private void addSourceControl(int sidebarX) {
        int y = TOP;
        y = runPanelButton(sidebarX, y, "Refresh", () -> requestW130("scm status"), true);
        y = runPanelButton(sidebarX, y, "Stage All", () -> requestW130("scm stage *"), !scmSnapshot.changes().isEmpty());
        y = runPanelButton(sidebarX, y, "Unstage All", () -> requestW130("scm unstage *"), scmSnapshot.stagedCount() > 0);
        y = runPanelButton(sidebarX, y, "Diff Active", () -> requestW130("scm diff " + activePath), true);
        y = runPanelButton(sidebarX, y, "Revert Active", () -> requestW130("scm revert " + activePath), true);

        commitMessageField = new W129IdeEditBox(
                font,
                sidebarX + 8,
                y,
                SIDEBAR_WIDTH - 78,
                20,
                Component.literal("Commit message")
        );
        commitMessageField.setMaxLength(120);
        commitMessageField.setHint(Component.literal("Commit message"));
        addRenderableWidget(commitMessageField);

        Button commit = W129IdeButton.themedBuilder(
                        Component.literal("Commit"),
                        ignored -> {
                            String message = commitMessageField == null ? "" : commitMessageField.getValue();
                            requestW130("scm commit " + message);
                        }
                )
                .bounds(sidebarX + SIDEBAR_WIDTH - 66, y, 58, 20)
                .build();
        commit.active = scmSnapshot.stagedCount() > 0;
        addRenderableWidget(commit);
        y += 24;

        int shown = 0;
        for (W130ScmSnapshot.Change change : scmSnapshot.changes()) {
            if (shown >= 9) {
                break;
            }
            String label = (change.staged() ? "S " : "  ")
                    + change.state() + " " + trimLabel(change.path(), 27);
            addRenderableWidget(
                    W129IdeButton.themedBuilder(
                                    Component.literal(label),
                                    ignored -> requestW130(
                                            (change.staged() ? "scm unstage " : "scm stage ") + change.path()
                                    )
                            )
                            .bounds(sidebarX + 8, y, SIDEBAR_WIDTH - 16, 18)
                            .build()
            );
            y += 20;
            shown++;
        }
    }

    private void addExtensions(int sidebarX) {
        addRenderableWidget(
                W129IdeButton.themedBuilder(
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
        int y = TOP;
        y = runPanelButton(
                sidebarX,
                y,
                bottomPanelVisible ? "Hide Bottom Panel" : "Show Bottom Panel",
                () -> {
                    bottomPanelVisible = !bottomPanelVisible;
                    reinitPreservingEditor();
                },
                true
        );

        y = runPanelButton(
                sidebarX,
                y,
                prettyGenerated ? "Generated: Pretty" : "Generated: Raw",
                () -> {
                    if (dirty()) {
                        setLocalStatus("Save or Reload before changing generated-file display mode.", false);
                        return;
                    }
                    prettyGenerated = !prettyGenerated;
                    reinitPreservingEditor();
                },
                true
        );

        y = runPanelButton(sidebarX, y, "Open settings.json", () -> openFile(W130Workspace.SETTINGS_PATH), true);
        y = runPanelButton(sidebarX, y, "Open launch.json", () -> openFile(W130Workspace.LAUNCH_PATH), true);
        y = runPanelButton(sidebarX, y, "Open tasks.json", () -> openFile(W130Workspace.TASKS_PATH), true);
        runPanelButton(sidebarX, y, "Workspace Status", () -> requestW130("workspace status"), true);
    }

    private void addEditorTabs(int editorX) {
        int x = editorX;
        int available = Math.max(120, width - editorX - 170);
        int shown = 0;

        for (String path : openTabs) {
            if (shown >= MAX_OPEN_TABS) {
                break;
            }

            int tabWidth = Math.min(150, Math.max(86, font.width(baseName(path)) + 36));
            if (x + tabWidth > editorX + available) {
                break;
            }

            int labelWidth = tabWidth - 18;
            Button tab = W129IdeButton.themedBuilder(
                            Component.literal(
                                    baseName(path)
                                            + (path.equals(activePath) && dirty() ? " *" : "")
                            ),
                            ignored -> openFile(path)
                    )
                    .bounds(x, 29, labelWidth, 20)
                    .build();
            ((W129IdeButton) tab).setSelected(
                    path.equals(activePath)
            );
            addRenderableWidget(tab);

            addRenderableWidget(
                    W129IdeButton.themedBuilder(
                                    Component.literal("x"),
                                    ignored -> closeTab(path)
                            )
                            .bounds(x + labelWidth + 1, 29, 17, 20)
                            .build()
            );

            x += tabWidth + 2;
            shown++;
        }
    }

    private void closeTab(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (path.equals(activePath)) {
            closeActiveTab();
            return;
        }
        openTabs.remove(path);
        reinitPreservingEditor();
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
                W129IdeButton.themedBuilder(
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
                W129IdeButton.themedBuilder(
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
            terminalField = new W129IdeEditBox(
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
                    W129IdeButton.themedBuilder(
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
        Button button = W129IdeButton.themedBuilder(
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

        ((W129IdeButton) button).setSelected(
                bottomMode == target
        );

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
        renderW129Chrome(graphics);

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
                27,
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

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        if (paletteVisible) {
            graphics.pose().pushPose();
            graphics.pose().translate(
                    0.0F,
                    0.0F,
                    1000.0F
            );

            renderCommandPaletteOverlay(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );

            graphics.pose().popPose();
        }
    }

    private void renderW129Chrome(
            GuiGraphics graphics
    ) {
        int editorLeft =
                ACTIVITY_WIDTH
                        + SIDEBAR_WIDTH;

        int bottomTop =
                bottomPanelVisible
                        ? height
                        - STATUS_HEIGHT
                        - BOTTOM_PANEL_HEIGHT
                        : height - STATUS_HEIGHT;

        int toolbarTop =
                bottomTop
                        - TOOLBAR_HEIGHT;

        graphics.fill(
                0,
                0,
                width,
                2,
                W129IdeTheme.ACCENT_DARK
        );

        graphics.fill(
                6,
                5,
                21,
                20,
                W129IdeTheme.ACCENT_DARK
        );

        graphics.fill(
                8,
                7,
                19,
                18,
                W129IdeTheme.SUCCESS
        );

        graphics.fill(
                10,
                9,
                17,
                16,
                0xFF3B7B45
        );

        graphics.fill(
                editorLeft,
                26,
                editorLeft + 1,
                height - STATUS_HEIGHT,
                W129IdeTheme.BORDER
        );

        graphics.fill(
                editorLeft,
                50,
                width,
                51,
                W129IdeTheme.BORDER_SOFT
        );

        graphics.fill(
                editorLeft,
                toolbarTop,
                width,
                toolbarTop + 1,
                W129IdeTheme.BORDER
        );

        if (bottomPanelVisible) {
            graphics.fill(
                    editorLeft,
                    bottomTop,
                    width,
                    bottomTop + 1,
                    W129IdeTheme.ACCENT_DARK
            );
        }

        graphics.fill(
                0,
                height - STATUS_HEIGHT,
                width,
                height - STATUS_HEIGHT + 1,
                W129IdeTheme.BORDER
        );
    }

    private void renderCommandPaletteOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int paletteWidth = Math.min(620, width - 60);
        int paletteHeight = 210;
        int paletteX = Math.max(ACTIVITY_WIDTH + 12, (width - paletteWidth) / 2);
        int paletteY = Math.max(62, height / 2 - 135);

        graphics.fill(0, 26, width, height - STATUS_HEIGHT, 0xFF070D13);
        graphics.fill(
                paletteX - 2,
                paletteY - 2,
                paletteX + paletteWidth + 2,
                paletteY + paletteHeight + 2,
                W129IdeTheme.ACCENT
        );
        graphics.fill(
                paletteX,
                paletteY,
                paletteX + paletteWidth,
                paletteY + paletteHeight,
                W129IdeTheme.PANEL
        );
        graphics.fill(
                paletteX,
                paletteY,
                paletteX + paletteWidth,
                paletteY + 28,
                W129IdeTheme.HEADER_ALT
        );

        graphics.drawString(font, ">_  Run Command", paletteX + 14, paletteY + 10, W129IdeTheme.ACCENT, false);
        String shortcut = "Ctrl+Shift+P";
        graphics.drawString(
                font,
                shortcut,
                paletteX + paletteWidth - font.width(shortcut) - 14,
                paletteY + 10,
                W129IdeTheme.TEXT_DIM,
                false
        );

        String query = paletteField == null ? "" : paletteField.getValue();
        paletteMatches = W130CommandRegistry.search(query, 6);
        if (paletteMatches.isEmpty()) {
            paletteSelection = 0;
        } else {
            paletteSelection = Math.max(0, Math.min(paletteSelection, paletteMatches.size() - 1));
        }

        graphics.drawString(font, "COMMANDS", paletteX + 16, paletteY + 72, W129IdeTheme.TEXT_DIM, false);

        int rowY = paletteY + 88;
        for (int i = 0; i < paletteMatches.size(); i++) {
            W130CommandRegistry.Command command = paletteMatches.get(i);
            drawPaletteSuggestion(
                    graphics,
                    paletteX,
                    rowY,
                    paletteWidth,
                    command.label(),
                    command.description(),
                    i == paletteSelection
            );
            rowY += 18;
        }

        if (paletteMatches.isEmpty()) {
            graphics.drawString(
                    font,
                    "No matching commands. Enter a terminal-style command directly.",
                    paletteX + 20,
                    paletteY + 92,
                    W129IdeTheme.TEXT_DIM,
                    false
            );
        }

        if (paletteField != null) {
            paletteField.render(graphics, mouseX, mouseY, partialTick);
        }
        if (paletteRunButton != null) {
            paletteRunButton.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawPaletteSuggestion(
            GuiGraphics graphics,
            int paletteX,
            int y,
            int paletteWidth,
            String command,
            String description,
            boolean selected
    ) {
        if (selected) {
            graphics.fill(
                    paletteX + 12,
                    y - 3,
                    paletteX + paletteWidth - 12,
                    y + 13,
                    W129IdeTheme.SELECTION
            );
        }

        graphics.drawString(
                font,
                "> " + command,
                paletteX + 20,
                y,
                selected
                        ? 0xFFFFFFFF
                        : W129IdeTheme.TEXT,
                false
        );

        if (paletteWidth >= 430) {
            graphics.drawString(
                    font,
                    description,
                    paletteX
                            + paletteWidth
                            - font.width(description)
                            - 20,
                    y,
                    W129IdeTheme.TEXT_DIM,
                    false
            );
        }
    }

    private void renderSidePanelText(
            GuiGraphics graphics
    ) {
        int x = ACTIVITY_WIDTH + 8;

        graphics.drawString(font, sideMode.title(), x, 33, 0xC7D0DB, false);

        switch (sideMode) {
            case RUN -> {
                int y = TOP + 230;
                graphics.drawString(font, "DEBUG SESSION", x, y, 0x8FA9BD, false);
                y += 15;
                graphics.drawString(
                        font,
                        debugSnapshot.state() + (debugSnapshot.reason().isBlank() ? "" : " | " + debugSnapshot.reason()),
                        x,
                        y,
                        debugSnapshot.paused() ? 0xFFD166 : (debugSnapshot.active() ? 0x62E38A : 0x74879A),
                        false
                );
                y += 15;
                if (!debugSnapshot.path().isBlank()) {
                    graphics.drawString(font, trimLabel(debugSnapshot.path(), 30), x, y, 0xE7EDF5, false);
                    y += 15;
                }
                if (debugSnapshot.line() > 0) {
                    graphics.drawString(font, "Line " + debugSnapshot.line(), x, y, 0x6FD7FF, false);
                    y += 15;
                }

                int shown = 0;
                for (var entry : debugSnapshot.variables().entrySet()) {
                    if (shown++ >= 5 || y > height - 75) {
                        break;
                    }
                    graphics.drawString(
                            font,
                            trimLabel(entry.getKey() + " = " + entry.getValue(), 32),
                            x,
                            y,
                            0xA7B4C3,
                            false
                    );
                    y += 13;
                }
            }

            case SOURCE_CONTROL -> {
                int y = Math.min(height - 80, TOP + 330);
                graphics.drawString(font, "HEAD #" + scmSnapshot.head(), x, y, 0x8FA9BD, false);
                y += 14;
                graphics.drawString(
                        font,
                        scmSnapshot.changes().size() + " change(s), " + scmSnapshot.stagedCount() + " staged",
                        x,
                        y,
                        scmSnapshot.changes().isEmpty() ? 0x62E38A : 0xFFD166,
                        false
                );
            }

            case EXTENSIONS -> {
                int y = TOP + 34;
                String[] entries = {
                        "HTML/CSS      Web",
                        "JavaScript    Web",
                        "React JSX     Web",
                        "Assembly      VSIA VM + Debug",
                        "Python        Sandbox + Debug",
                        "C             Sandbox + Debug",
                        "C++           Sandbox + Debug",
                        "C#            Sandbox + Debug",
                        "Java          Sandbox + Debug",
                        "JSON/MD       Editor",
                        "W1.30 Tasks   Built-in",
                        "W1.30 SCM     Built-in"
                };
                for (String entry : entries) {
                    graphics.drawString(font, entry, x, y, 0xA7B4C3, false);
                    y += 15;
                }
            }

            case SETTINGS -> {
                int y = TOP + 142;
                graphics.drawString(font, "W1.30 Workspace", x, y, 0xE7EDF5, false);
                graphics.drawString(font, "Debugger: ON", x, y + 18, 0x62E38A, false);
                graphics.drawString(font, "Tasks: ON", x, y + 34, 0x62E38A, false);
                graphics.drawString(font, "Source Control: ON", x, y + 50, 0x62E38A, false);
                graphics.drawString(font, "Host compiler exec: OFF", x, y + 66, 0x62E38A, false);
            }

            case SEARCH -> {
                if (searchResults.isEmpty()) {
                    graphics.drawString(font, "Ctrl+Shift+F", x, TOP + 26, 0x65788B, false);
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

        int maxOffset =
                Math.max(
                        0,
                        lines.size()
                                - MAX_PANEL_LINES
                );

        int scrollOffset =
                Math.min(
                        panelScrollOffset(bottomMode),
                        maxOffset
                );

        setPanelScrollOffset(bottomMode, scrollOffset);

        int end =
                Math.max(
                        0,
                        lines.size() - scrollOffset
                );

        int start =
                Math.max(
                        0,
                        end - MAX_PANEL_LINES
                );

        if (maxOffset > 0) {
            String scrollState =
                    scrollOffset == 0
                            ? "BOTTOM"
                            : "SCROLL +" + scrollOffset;

            graphics.drawString(
                    font,
                    scrollState,
                    Math.max(left + 250, width - font.width(scrollState) - 42),
                    top + 8,
                    0x65788B,
                    false
            );
        }

        for (int i = start;
             i < end;
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

    private List<String> panelLines(BottomMode mode) {
        return switch (mode) {
            case TERMINAL -> terminalLines;
            case PROBLEMS -> problemLines;
            case OUTPUT -> outputLines;
        };
    }

    private int panelScrollOffset(BottomMode mode) {
        return switch (mode) {
            case TERMINAL -> terminalScrollOffset;
            case PROBLEMS -> problemScrollOffset;
            case OUTPUT -> outputScrollOffset;
        };
    }

    private void setPanelScrollOffset(BottomMode mode, int value) {
        int clamped = Math.max(0, value);
        switch (mode) {
            case TERMINAL -> terminalScrollOffset = clamped;
            case PROBLEMS -> problemScrollOffset = clamped;
            case OUTPUT -> outputScrollOffset = clamped;
        }
    }

    private void scrollBottomPanel(BottomMode mode, int deltaLines) {
        List<String> lines = panelLines(mode);
        int maxOffset = Math.max(0, lines.size() - MAX_PANEL_LINES);
        int next = panelScrollOffset(mode) + deltaLines;
        setPanelScrollOffset(mode, Math.max(0, Math.min(maxOffset, next)));
    }

    private void scrollPanelToTop(BottomMode mode) {
        List<String> lines = panelLines(mode);
        setPanelScrollOffset(mode, Math.max(0, lines.size() - MAX_PANEL_LINES));
    }

    private void scrollPanelToBottom(BottomMode mode) {
        setPanelScrollOffset(mode, 0);
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
        if (paletteVisible) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                if (!paletteMatches.isEmpty()) {
                    paletteSelection = Math.max(0, paletteSelection - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                if (!paletteMatches.isEmpty()) {
                    paletteSelection = Math.min(paletteMatches.size() - 1, paletteSelection + 1);
                }
                return true;
            }

            if (
                    keyCode == GLFW.GLFW_KEY_ESCAPE
                            || (
                            keyCode == GLFW.GLFW_KEY_P
                                    && Screen.hasControlDown()
                                    && Screen.hasShiftDown()
                    )
            ) {
                hidePalette();
                return true;
            }

            if (
                    paletteField != null
                            && (
                            keyCode == GLFW.GLFW_KEY_ENTER
                                    || keyCode
                                    == GLFW.GLFW_KEY_KP_ENTER
                    )
            ) {
                executePalette();
                return true;
            }

            return super.keyPressed(
                    keyCode,
                    scanCode,
                    modifiers
            );
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

        if (terminalField != null
                && terminalField.isFocused()
                && keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollBottomPanel(bottomMode, MAX_PANEL_LINES - 1);
            return true;
        }

        if (terminalField != null
                && terminalField.isFocused()
                && keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollBottomPanel(bottomMode, -(MAX_PANEL_LINES - 1));
            return true;
        }

        if (terminalField != null
                && terminalField.isFocused()
                && Screen.hasControlDown()
                && keyCode == GLFW.GLFW_KEY_HOME) {
            scrollPanelToTop(bottomMode);
            return true;
        }

        if (terminalField != null
                && terminalField.isFocused()
                && Screen.hasControlDown()
                && keyCode == GLFW.GLFW_KEY_END) {
            scrollPanelToBottom(bottomMode);
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

        if (keyCode == GLFW.GLFW_KEY_F5 && Screen.hasShiftDown()) {
            stopDebugging();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5 && Screen.hasControlDown()) {
            runCurrent();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5) {
            if (debugSnapshot.paused()) {
                continueDebugging();
            } else if (debugSnapshot.active()) {
                setLocalStatus(
                        "The debugger is already running.",
                        false
                );
            } else {
                startDebugging();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F10) {
            stepDebugging("debug next");
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F11) {
            stepDebugging(
                    Screen.hasShiftDown()
                            ? "debug out"
                            : "debug step"
            );
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
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (paletteVisible) {
            if (
                    paletteField != null
                            && paletteField.visible
                            && paletteField.mouseClicked(
                            mouseX,
                            mouseY,
                            button
                    )
            ) {
                setInitialFocus(paletteField);
                return true;
            }

            if (
                    paletteRunButton != null
                            && paletteRunButton.visible
                            && paletteRunButton.mouseClicked(
                            mouseX,
                            mouseY,
                            button
                    )
            ) {
                return true;
            }

            int suggestion = paletteSuggestionAt(mouseX, mouseY);
            if (suggestion >= 0 && suggestion < paletteMatches.size()) {
                paletteSelection = suggestion;
                executePaletteSelection();
                return true;
            }

            if (paletteField != null) {
                setInitialFocus(paletteField);
            }

            return true;
        }

        if (editor != null && editor.isInGutter(mouseX, mouseY) && button == 0) {
            int line = editor.lineAtMouse(mouseX, mouseY);
            if (line > 0) {
                requestW130("break " + activePath + ":" + line);
                return true;
            }
        }

        if (bottomPanelVisible && bottomMode == BottomMode.PROBLEMS && button == 0
                && navigateProblemAt(mouseX, mouseY)) {
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
        if (paletteVisible) {
            return true;
        }

        if (bottomPanelVisible) {
            int left = ACTIVITY_WIDTH + SIDEBAR_WIDTH;
            int top = height - STATUS_HEIGHT - BOTTOM_PANEL_HEIGHT;
            int bottom = height - STATUS_HEIGHT;

            if (mouseX >= left
                    && mouseX < width
                    && mouseY >= top
                    && mouseY < bottom) {
                if (delta > 0.0D) {
                    scrollBottomPanel(bottomMode, PANEL_SCROLL_STEP);
                } else if (delta < 0.0D) {
                    scrollBottomPanel(bottomMode, -PANEL_SCROLL_STEP);
                }
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
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
                terminalScrollOffset = 0;
            }

            case "TERMINAL_CLEAR" -> {
                terminalLines.clear();
                terminalScrollOffset = 0;
            }

            case "PROBLEMS" -> {
                problemLines.clear();
                problemTargets.clear();
                problemScrollOffset = 0;

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
                            try {
                                problemTargets.add(new ProblemTarget(
                                        activePath,
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[2])
                                ));
                            } catch (NumberFormatException ignored) {
                                problemTargets.add(new ProblemTarget("", 0, 0));
                            }
                        } else {
                            problemLines.add(raw);
                            problemTargets.add(new ProblemTarget("", 0, 0));
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

            case "W130_DEBUG" -> {
                debugSnapshot = W130DebugSnapshot.fromJson(packet.payload());
                if (debugSnapshot.paused()
                        && debugSnapshot.path().equals(activePath)
                        && debugSnapshot.line() > 0) {
                    pendingGoLine = debugSnapshot.line();
                }
            }

            case "W130_SCM" -> {
                scmSnapshot = W130ScmSnapshot.fromJson(packet.payload());
            }

            case "W130_PROBLEMS" -> {
                problemLines.clear();
                problemTargets.clear();
                problemScrollOffset = 0;

                if (packet.payload().isBlank()) {
                    problemLines.add("No problems detected.");
                    problemTargets.add(new ProblemTarget("", 0, 0));
                } else {
                    for (String raw : packet.payload().split("\\n")) {
                        String[] parts = raw.split("\\|", 5);
                        if (parts.length == 5) {
                            try {
                                int line = Integer.parseInt(parts[2]);
                                int column = Integer.parseInt(parts[3]);
                                problemLines.add(
                                        parts[0] + " " + parts[1] + ":" + line + ":" + column + " " + parts[4]
                                );
                                problemTargets.add(new ProblemTarget(parts[1], line, column));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                if (!packet.success()) {
                    bottomMode = BottomMode.PROBLEMS;
                    bottomPanelVisible = true;
                }
            }

            case "OUTPUT" -> {
                appendPanelBlock(
                        outputLines,
                        packet.title(),
                        packet.payload()
                );
                outputScrollOffset = 0;
            }

            default -> {
                appendPanelBlock(
                        outputLines,
                        packet.title(),
                        packet.payload()
                );
                outputScrollOffset = 0;
            }
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

    private void applyDebugDecorations() {
        if (editor == null) {
            return;
        }
        List<Integer> breakpoints = debugSnapshot.breakpoints().getOrDefault(activePath, List.of());
        editor.setBreakpointLines(Set.copyOf(breakpoints));
        editor.setExecutionLine(
                debugSnapshot.paused() && activePath.equals(debugSnapshot.path())
                        ? debugSnapshot.line()
                        : 0
        );
    }

    private boolean navigateProblemAt(double mouseX, double mouseY) {
        int top = height - STATUS_HEIGHT - BOTTOM_PANEL_HEIGHT;
        int left = ACTIVITY_WIDTH + SIDEBAR_WIDTH;
        if (mouseX < left || mouseX >= width || mouseY < top + 26 || mouseY >= height - STATUS_HEIGHT) {
            return false;
        }

        int row = (int) ((mouseY - (top + 28)) / 12.0D);
        if (row < 0) {
            return false;
        }

        int maxOffset = Math.max(0, problemLines.size() - MAX_PANEL_LINES);
        int offset = Math.min(problemScrollOffset, maxOffset);
        int end = Math.max(0, problemLines.size() - offset);
        int start = Math.max(0, end - MAX_PANEL_LINES);
        int index = start + row;
        if (index < 0 || index >= problemTargets.size()) {
            return false;
        }

        ProblemTarget target = problemTargets.get(index);
        if (target.path().isBlank() || target.line() <= 0) {
            return false;
        }

        pendingGoLine = target.line();
        if (target.path().equals(activePath)) {
            if (editor != null) {
                editor.goToLine(target.line());
            }
        } else {
            openFile(target.path());
        }
        return true;
    }

    private int paletteSuggestionAt(double mouseX, double mouseY) {
        int paletteWidth = Math.min(620, width - 60);
        int paletteX = Math.max(ACTIVITY_WIDTH + 12, (width - paletteWidth) / 2);
        int paletteY = Math.max(62, height / 2 - 135);
        if (mouseX < paletteX + 12 || mouseX >= paletteX + paletteWidth - 12
                || mouseY < paletteY + 85 || mouseY >= paletteY + 85 + 18 * 6) {
            return -1;
        }
        return (int) ((mouseY - (paletteY + 85)) / 18.0D);
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

        if (target == SideMode.SOURCE_CONTROL) {
            requestW130("scm status");
        } else if (target == SideMode.RUN) {
            requestW130("debug status");
        }
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

    private boolean hasDebugSession() {
        if (debugSnapshot == null
                || "IDLE".equalsIgnoreCase(
                debugSnapshot.state()
        )) {
            return false;
        }

        return !"TERMINATED".equalsIgnoreCase(
                debugSnapshot.state()
        )
                || !"stopped".equalsIgnoreCase(
                debugSnapshot.reason()
        );
    }

    private boolean debuggableActiveFile() {
        if (activePath == null || activePath.isBlank()) {
            return false;
        }

        W128IdeLanguage language = W128IdeLanguage.detect(activePath);

        return W129ComputeEngine.executable(activePath)
                || language == W128IdeLanguage.JAVASCRIPT
                || language == W128IdeLanguage.JSX;
    }

    private void startDebugging() {
        if (dirty()) {
            setLocalStatus(
                    "Save the file before starting the debugger.",
                    false
            );
            return;
        }

        if (!debuggableActiveFile()) {
            setLocalStatus(
                    "The active file has no W1.30 debug runtime.",
                    false
            );
            return;
        }

        requestW130("debug start");
    }

    private void continueDebugging() {
        if (!debugSnapshot.paused()) {
            setLocalStatus(
                    hasDebugSession()
                            ? "The debugger is not currently paused."
                            : "Start Debugging (F5) first.",
                    false
            );
            return;
        }

        requestW130("debug continue");
    }

    private void stepDebugging(String command) {
        if (!debugSnapshot.paused()) {
            setLocalStatus(
                    hasDebugSession()
                            ? "Step commands require a paused debug session."
                            : "Start Debugging (F5) first.",
                    false
            );
            return;
        }

        requestW130(command);
    }

    private void restartDebugging() {
        if (!hasDebugSession()) {
            setLocalStatus(
                    "There is no debug session to restart.",
                    false
            );
            return;
        }

        requestW130("debug restart");
    }

    private void stopDebugging() {
        if (!hasDebugSession()) {
            setLocalStatus(
                    "There is no debug session to stop.",
                    false
            );
            return;
        }

        requestW130("debug stop");
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
            W128IdeLanguage language = W128IdeLanguage.detect(activePath);

            if (language.webRuntime()) {
                setLocalStatus(
                        "Web runtime selected: Run performs a project Build. Publish when ready.",
                        true
                );
                build();
                return;
            }

            setLocalStatus(
                    "This file is editor-only and has no W1.29 execution runtime.",
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

        terminalScrollOffset = 0;
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

        String raw = paletteField.getValue().trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        boolean directWithArgument = lower.startsWith("search ")
                || lower.startsWith("new file ")
                || lower.startsWith("rename ")
                || lower.startsWith("go to line ")
                || lower.startsWith("run ")
                || lower.startsWith("check ")
                || lower.startsWith("validate ")
                || lower.startsWith("w130 ");

        if (!directWithArgument && !paletteMatches.isEmpty()) {
            executePaletteSelection();
            return;
        }

        hidePalette();
        if (raw.startsWith(">")) {
            raw = raw.substring(1).trim();
        }
        if (!raw.isEmpty()) {
            executePaletteCommand(raw);
        }
    }

    private void executePaletteSelection() {
        if (paletteMatches.isEmpty()) {
            return;
        }
        int index = Math.max(0, Math.min(paletteSelection, paletteMatches.size() - 1));
        String command = paletteMatches.get(index).command();
        hidePalette();
        executePaletteCommand(command);
    }

    private void executePaletteCommand(String rawCommand) {
        String command = rawCommand.trim().replaceAll("\\s+", " ");
        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.startsWith("w130 ")) {
            requestW130(command.substring(5).trim());
            return;
        }

        if (lower.startsWith("run ")) {
            sendTerminal(command);
            return;
        }

        if (lower.startsWith("check ") || lower.startsWith("validate ")) {
            String argument = command.substring(command.indexOf(' ') + 1).trim();
            sendTerminal("check " + argument);
            return;
        }

        if (lower.startsWith("search ")) {
            String query = command.substring(command.indexOf(' ') + 1).trim();
            sideMode = SideMode.SEARCH;
            reinitPreservingEditor();
            if (searchField != null) {
                searchField.setValue(query);
                setInitialFocus(searchField);
                searchWorkspace();
            }
            return;
        }

        if (lower.startsWith("new file ")) {
            request(W128IdeAction.CREATE, command.substring("new file ".length()).trim(), "");
            return;
        }

        if (lower.startsWith("rename ")) {
            request(W128IdeAction.RENAME, activePath, command.substring("rename ".length()).trim());
            return;
        }

        if (lower.startsWith("go to line ")) {
            String rawLine = command.substring("go to line ".length()).trim();
            try {
                if (editor != null) {
                    editor.goToLine(Integer.parseInt(rawLine));
                }
            } catch (NumberFormatException exception) {
                setLocalStatus("Invalid line number: " + rawLine, false);
            }
            return;
        }

        switch (lower) {
            case "save", "file: save", "file save" -> save();
            case "run", "run current file", "debug: run current file" -> runCurrent();
            case "check", "validate", "check current file", "validate current file" -> validateCurrent();
            case "build", "build project", "tasks: build project" -> build();
            case "publish", "publish project" -> publish();
            case "unpublish", "unpublish project" -> unpublish();
            case "reload", "refresh", "developer: reload" -> refresh();
            case "format", "format document", "editor: format document" -> formatOrToggle();

            case "explorer", "view: explorer" -> {
                sideMode = SideMode.EXPLORER;
                reinitPreservingEditor();
            }

            case "search", "view: search" -> {
                sideMode = SideMode.SEARCH;
                reinitPreservingEditor();
                if (searchField != null) {
                    setInitialFocus(searchField);
                }
            }

            case "run and debug", "view: run and debug" -> {
                sideMode = SideMode.RUN;
                reinitPreservingEditor();
            }

            case "source control", "view: source control" -> {
                sideMode = SideMode.SOURCE_CONTROL;
                reinitPreservingEditor();
            }

            case "extensions", "view: extensions" -> {
                sideMode = SideMode.EXTENSIONS;
                reinitPreservingEditor();
            }

            case "settings", "preferences: settings" -> {
                sideMode = SideMode.SETTINGS;
                reinitPreservingEditor();
            }

            case "terminal", "view: terminal", "terminal: focus terminal" -> {
                bottomPanelVisible = true;
                bottomMode = BottomMode.TERMINAL;
                terminalScrollOffset = 0;
                reinitPreservingEditor();
                if (terminalField != null) {
                    setInitialFocus(terminalField);
                }
            }

            case "problems", "view: problems" -> {
                bottomPanelVisible = true;
                bottomMode = BottomMode.PROBLEMS;
                reinitPreservingEditor();
            }

            case "output", "view: output" -> {
                bottomPanelVisible = true;
                bottomMode = BottomMode.OUTPUT;
                reinitPreservingEditor();
            }

            case "toggle panel", "view: toggle panel" -> {
                bottomPanelVisible = !bottomPanelVisible;
                reinitPreservingEditor();
            }

            case "clear terminal", "terminal: clear" -> sendTerminal("clear");
            case "status", "project status" -> sendTerminal("status");
            case "selftest", "w1.29 self test", "w1.29 selftest" -> sendTerminal("selftest");
            case "close editor", "close active editor", "file: close editor" -> closeActiveTab();

            case "help", "command palette help" -> {
                bottomPanelVisible = true;
                bottomMode = BottomMode.OUTPUT;
                appendPanelBlock(
                        outputLines,
                        "Command Palette",
                        "save | run | check | build | publish | reload | format | explorer | search | terminal | problems | output | w130 debug start|continue|pause|next|step|out|restart|stop | w130 task run <label> | w130 scm status|stage|unstage|commit|diff|log|revert | w130 selftest | new file <path> | rename <path> | go to line <n> | close editor"
                );
                outputScrollOffset = 0;
                reinitPreservingEditor();
            }

            default -> setLocalStatus(
                    "Unknown Command Palette entry: " + rawCommand + " | use 'help' for commands",
                    false
            );
        }
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

    private void requestW130(String command) {
        request(
                W128IdeAction.W130,
                activePath,
                command == null ? "" : command
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
        applyDebugDecorations();
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

    private record ProblemTarget(
            String path,
            int line,
            int column
    ) {
    }
}
