package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.client.web.W128CodeEditor;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W128IdeRequestPacket;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import com.k1ngtle.vsia.signality.internet.web.W128CodeFormatter;
import com.k1ngtle.vsia.signality.internet.web.W128IdeAction;
import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class W128WebIdeScreen extends Screen {
    private static final int MAX_FILE_CHARACTERS = 262_144;
    private static final int EXPLORER_WIDTH = 230;
    private static final int TOP = 52;
    private static final int BOTTOM = 60;
    private static final int FILE_ROWS = 13;

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

    private int filePage;
    private boolean prettyGenerated = true;
    private String editorBaseline = "";

    public W128WebIdeScreen(W128IdeSnapshotPacket packet) {
        super(Component.literal("VS:IA Web IDE"));
        host = packet.host();
        applySnapshot(packet);
    }

    public String host() {
        return host;
    }

    @Override
    protected void init() {
        clearWidgets();

        int editorX = EXPLORER_WIDTH + 12;
        int editorY = TOP;
        int editorWidth = Math.max(160, width - editorX - 10);
        int editorHeight = Math.max(90, height - TOP - BOTTOM);

        pathField = new EditBox(
                font,
                10,
                TOP,
                EXPLORER_WIDTH - 72,
                18,
                Component.literal("File path")
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
                                button -> createFile()
                        )
                        .bounds(
                                EXPLORER_WIDTH - 58,
                                TOP,
                                48,
                                18
                        )
                        .build()
        );

        editor = new W128CodeEditor(
                font,
                editorX,
                editorY,
                editorWidth,
                editorHeight,
                MAX_FILE_CHARACTERS
        );

        editorBaseline = displayValue();
        editor.setValue(editorBaseline);
        editor.setEditable(true);
        addRenderableWidget(editor);

        addFileButtons();

        int toolbarY = height - 49;
        int x = 10;

        x = button(x, toolbarY, 56, "Save", this::save);
        x = button(x, toolbarY, 56, "Build", this::build);
        x = button(
                x,
                toolbarY,
                76,
                published ? "Unpublish" : "Publish",
                published ? this::unpublish : this::publish
        );
        x = button(x, toolbarY, 62, "Reload", this::refresh);
        x = button(x, toolbarY, 82, "Book -> File", this::importBook);
        x = button(x, toolbarY, 78, "Paste Full", this::pasteFull);
        x = button(x, toolbarY, 70, "Copy All", this::copyAll);
        x = button(
                x,
                toolbarY,
                74,
                activeGenerated
                        ? (prettyGenerated ? "Raw View" : "Pretty")
                        : "Format",
                this::formatOrToggle
        );

        if (managedOutput(activePath)) {
            x = button(
                    x,
                    toolbarY,
                    84,
                    "Regenerate",
                    this::regenerate
            );
        }

        button(x, toolbarY, 62, "Delete", this::deleteFile);

        goLineField = new EditBox(
                font,
                Math.max(EXPLORER_WIDTH + 12, width - 150),
                29,
                58,
                18,
                Component.literal("Line")
        );
        goLineField.setMaxLength(7);
        goLineField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        addRenderableWidget(goLineField);

        addRenderableWidget(
                Button.builder(
                                Component.literal("Go line"),
                                button -> goToLine()
                        )
                        .bounds(
                                Math.max(EXPLORER_WIDTH + 74, width - 88),
                                29,
                                78,
                                18
                        )
                        .build()
        );
    }

    private int button(
            int x,
            int y,
            int buttonWidth,
            String label,
            Runnable action
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> action.run()
                        )
                        .bounds(
                                x,
                                y,
                                buttonWidth,
                                20
                        )
                        .build()
        );
        return x + buttonWidth + 4;
    }

    private void addFileButtons() {
        int start = filePage * FILE_ROWS;
        int y = TOP + 24;

        for (int i = 0; i < FILE_ROWS; i++) {
            int index = start + i;

            if (index >= files.size()) {
                break;
            }

            W128IdeSnapshotPacket.FileEntry entry = files.get(index);
            String marker = entry.generated()
                    ? "G "
                    : (managedOutput(entry.path()) ? "M " : "  ");
            String label = marker + trimLabel(entry.path(), 31);

            Button fileButton = Button.builder(
                            Component.literal(label),
                            button -> openFile(entry.path())
                    )
                    .bounds(
                            10,
                            y,
                            EXPLORER_WIDTH - 20,
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
                (files.size() + FILE_ROWS - 1) / FILE_ROWS
        );

        int pageY = TOP + 24 + FILE_ROWS * 20 + 2;

        Button previous = Button.builder(
                        Component.literal("<"),
                        button -> {
                            if (filePage > 0) {
                                filePage--;
                                init();
                            }
                        }
                )
                .bounds(10, pageY, 28, 18)
                .build();

        previous.active = filePage > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(
                        Component.literal(">"),
                        button -> {
                            if (filePage + 1 < pages) {
                                filePage++;
                                init();
                            }
                        }
                )
                .bounds(EXPLORER_WIDTH - 38, pageY, 28, 18)
                .build();

        next.active = filePage + 1 < pages;
        addRenderableWidget(next);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.fill(0, 0, width, 26, 0xEE11161D);
        graphics.fill(0, 26, EXPLORER_WIDTH, height - BOTTOM + 20, 0xEE151B23);
        graphics.fill(
                EXPLORER_WIDTH,
                26,
                EXPLORER_WIDTH + 1,
                height - BOTTOM + 20,
                0xFF3B4654
        );

        graphics.drawString(
                font,
                "VS:IA WEB IDE",
                10,
                8,
                0x6FD7FF,
                false
        );

        String state = published ? "PUBLISHED" : "DRAFT";
        int stateColor = published ? 0x62E38A : 0xFFD166;

        graphics.drawString(
                font,
                host
                        + " | "
                        + mode
                        + " | "
                        + state
                        + " | revision "
                        + buildRevision,
                112,
                8,
                stateColor,
                false
        );

        graphics.drawString(
                font,
                "Server Rack: "
                        + (
                        boundServerIp.isBlank()
                                ? "UNBOUND"
                                : boundServerIp
                ),
                10,
                29,
                0xA7B4C3,
                false
        );

        graphics.drawString(
                font,
                "EXPLORER",
                10,
                41,
                0xC7D0DB,
                false
        );

        int editorX = EXPLORER_WIDTH + 12;

        W128IdeLanguage language =
                W128IdeLanguage.detect(activePath);

        String runtime =
                language.webRuntime()
                        ? "web-runtime"
                        : "source-only";

        String fileTitle =
                activePath.isBlank()
                        ? "No file selected"
                        : activePath;

        graphics.drawString(
                font,
                fileTitle,
                editorX,
                29,
                activeGenerated
                        ? 0xFFD166
                        : 0xE8F3FF,
                false
        );

        String management =
                activeGenerated
                        ? " | GENERATED | EDITABLE | SAVE = MANUAL OVERRIDE"
                        : (
                        managedOutput(activePath)
                                ? " | MANUAL OVERRIDE"
                                : ""
                );

        String view =
                activeGenerated
                        && W128CodeFormatter.supports(activePath)
                        ? (
                        prettyGenerated
                                ? " | PRETTY VIEW"
                                : " | RAW VIEW"
                )
                        : "";

        String right =
                language.displayName()
                        + " | "
                        + runtime
                        + management
                        + view;

        graphics.drawString(
                font,
                right,
                Math.max(
                        editorX,
                        width - font.width(right) - 160
                ),
                29,
                activeGenerated ? 0xFFD166 : 0x8FA9BD,
                false
        );

        int statusY = height - 21;

        String cursorStatus =
                editor == null
                        ? ""
                        : "Ln "
                        + editor.cursorLine()
                        + ", Col "
                        + editor.cursorColumn()
                        + " | "
                        + editor.lineCount()
                        + " lines";

        String dirtyText =
                dirty()
                        ? " | UNSAVED"
                        : "";

        graphics.drawString(
                font,
                status + dirtyText,
                10,
                statusY,
                statusSuccess
                        ? (
                        dirty()
                                ? 0xFFD166
                                : 0x62E38A
                )
                        : 0xFF6B6B,
                false
        );

        String help =
                cursorStatus
                        + " | Click line to place caret"
                        + " | Shift+wheel horizontal"
                        + " | Ctrl+S Save";

        graphics.drawString(
                font,
                help,
                Math.max(
                        10,
                        width - font.width(help) - 10
                ),
                statusY,
                0x74879A,
                false
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == GLFW.GLFW_KEY_S
                && Screen.hasControlDown()) {
            save();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5) {
            refresh();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F
                && Screen.hasControlDown()
                && Screen.hasShiftDown()) {
            formatOrToggle();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void acceptSnapshot(W128IdeSnapshotPacket packet) {
        String localValue = editor == null ? null : editor.getValue();
        String previousPath = activePath;

        boolean preserveLocal =
                !packet.success()
                        && localValue != null
                        && previousPath.equals(packet.activePath());

        applySnapshot(packet);
        prettyGenerated = true;

        int pages = Math.max(
                1,
                (files.size() + FILE_ROWS - 1) / FILE_ROWS
        );
        filePage = Math.min(filePage, pages - 1);

        init();

        if (preserveLocal && editor != null) {
            editor.setEditable(true);
            editor.replaceAll(localValue);
        }
    }

    private void applySnapshot(W128IdeSnapshotPacket packet) {
        host = packet.host();
        mode = packet.mode();
        boundServerIp = packet.boundServerIp();
        published = packet.published();
        buildRevision = packet.buildRevision();
        activePath = packet.activePath();
        serverContent = packet.activeContent();
        activeGenerated = packet.activeGenerated();
        files = packet.files();
        status = packet.message();
        statusSuccess = packet.success();
    }

    private String displayValue() {
        if (activeGenerated
                && prettyGenerated
                && W128CodeFormatter.supports(activePath)) {
            return W128CodeFormatter.format(
                    activePath,
                    serverContent
            );
        }

        return serverContent == null ? "" : serverContent;
    }

    private void openFile(String path) {
        if (dirty()) {
            setLocalStatus(
                    "Save the current source file before opening another file.",
                    false
            );
            return;
        }

        request(W128IdeAction.OPEN, path, "");
    }

    private void save() {
        if (editor == null) {
            return;
        }

        if (activePath.isBlank()) {
            setLocalStatus(
                    "Create or select a file first.",
                    false
            );
            return;
        }

        if (!dirty()) {
            setLocalStatus("No source changes to save.", true);
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
                    "Save the current source file before creating another file.",
                    false
            );
            return;
        }

        String value =
                pathField == null
                        ? ""
                        : pathField.getValue();

        request(W128IdeAction.CREATE, value, "");
    }

    private void deleteFile() {
        if (activePath.isBlank()) {
            setLocalStatus("Select a file first.", false);
            return;
        }

        request(W128IdeAction.DELETE, activePath, "");
    }

    private void build() {
        if (dirty()) {
            setLocalStatus(
                    "Save the current source file before building.",
                    false
            );
            return;
        }

        request(W128IdeAction.BUILD, activePath, "");
    }

    private void publish() {
        if (dirty()) {
            setLocalStatus(
                    "Save and build before publishing.",
                    false
            );
            return;
        }

        request(W128IdeAction.PUBLISH, activePath, "");
    }

    private void unpublish() {
        request(W128IdeAction.UNPUBLISH, activePath, "");
    }

    private void refresh() {
        if (dirty()) {
            setLocalStatus(
                    "Unsaved changes exist. Save them before reload.",
                    false
            );
            return;
        }

        request(W128IdeAction.REFRESH, activePath, "");
    }

    private void importBook() {
        if (activePath.isBlank()) {
            setLocalStatus(
                    "Select or create a destination file first.",
                    false
            );
            return;
        }

        request(W128IdeAction.IMPORT_BOOK, activePath, "");
    }

    private void pasteFull() {
        if (editor == null) {
            return;
        }

        String clipboard =
                Minecraft.getInstance()
                        .keyboardHandler
                        .getClipboard();

        if (clipboard == null) {
            clipboard = "";
        }

        if (clipboard.length() > MAX_FILE_CHARACTERS) {
            clipboard = clipboard.substring(0, MAX_FILE_CHARACTERS);
            setLocalStatus(
                    "Clipboard truncated to the W1.28 per-file limit.",
                    false
            );
        } else {
            setLocalStatus(
                    "Clipboard replaced the source buffer. Press Save.",
                    true
            );
        }

        editor.replaceAll(clipboard);
    }

    private void copyAll() {
        if (editor == null) {
            return;
        }

        editor.copySelectionOrAll();

        setLocalStatus(
                editor.hasSelection()
                        ? "Selection copied to system clipboard."
                        : "Current file copied to system clipboard.",
                true
        );
    }

    private void formatOrToggle() {
        if (editor == null) {
            return;
        }

        if (!W128CodeFormatter.supports(activePath)) {
            setLocalStatus(
                    "No formatter is registered for this file type.",
                    false
            );
            return;
        }

        if (activeGenerated) {
            if (dirty()) {
                setLocalStatus(
                        "Save or Reload your edits before switching Pretty/Raw view.",
                        false
                );
                return;
            }

            prettyGenerated = !prettyGenerated;
            editorBaseline = displayValue();
            editor.setValue(editorBaseline);
            editor.setEditable(true);

            setLocalStatus(
                    prettyGenerated
                            ? "Pretty view active. Edit + Save creates a manual override."
                            : "Raw view active. Edit + Save creates a manual override.",
                    true
            );
            return;
        }

        String formatted =
                W128CodeFormatter.format(
                        activePath,
                        editor.getValue()
                );

        if (formatted.equals(editor.getValue())) {
            setLocalStatus("Formatting produced no changes.", true);
            return;
        }

        editor.replaceAll(formatted);
        setLocalStatus(
                "Source formatted locally. Review it, then Save.",
                true
        );
    }

    private void goToLine() {
        if (editor == null || goLineField == null) {
            return;
        }

        String value = goLineField.getValue();

        if (value.isBlank()) {
            setLocalStatus("Enter a line number.", false);
            return;
        }

        try {
            int line = Integer.parseInt(value);
            editor.goToLine(line);
            setLocalStatus(
                    "Moved caret to line "
                            + editor.cursorLine()
                            + ".",
                    true
            );
        } catch (NumberFormatException exception) {
            setLocalStatus("Invalid line number.", false);
        }
    }

    private boolean dirty() {
        return editor != null
                && !editor.getValue().equals(editorBaseline);
    }

    private void regenerate() {
        if (!managedOutput(activePath)) {
            setLocalStatus(
                    "This file is not a React-managed build output.",
                    false
            );
            return;
        }

        if (dirty()) {
            setLocalStatus(
                    "Save your edit or Reload before regenerating this file.",
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

    private boolean managedOutput(String path) {
        if (!"REACT".equalsIgnoreCase(mode)
                || path == null) {
            return false;
        }

        return "/app.js".equals(path)
                || "/index.html".equals(path)
                || "/react-runtime.js".equals(path);
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
                action.name() + " requested...",
                true
        );
    }

    private void setLocalStatus(
            String value,
            boolean success
    ) {
        status = value;
        statusSuccess = success;
    }

    private static String trimLabel(String value, int max) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return "..."
                + value.substring(
                        value.length() - max + 3
                );
    }
}
