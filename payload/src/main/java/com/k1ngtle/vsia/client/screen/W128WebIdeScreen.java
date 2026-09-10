package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W128IdeRequestPacket;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import com.k1ngtle.vsia.signality.internet.web.W128IdeAction;
import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class W128WebIdeScreen extends Screen {
    private static final int MAX_FILE_CHARACTERS = 262_144;
    private static final int EXPLORER_WIDTH = 196;
    private static final int TOP = 46;
    private static final int BOTTOM = 54;
    private static final int FILE_ROWS = 12;

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

    private MultiLineEditBox editor;
    private EditBox pathField;

    private int filePage;
    private boolean loadingValue;

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
        int editorWidth = Math.max(120, width - editorX - 10);
        int editorHeight = Math.max(80, height - TOP - BOTTOM);

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

        editor = new MultiLineEditBox(
                font,
                editorX,
                editorY,
                editorWidth,
                editorHeight,
                Component.literal("Paste or type code here"),
                Component.literal("VSIA Web IDE Editor")
        );
        editor.setCharacterLimit(MAX_FILE_CHARACTERS);

        loadingValue = true;
        editor.setValue(serverContent);
        loadingValue = false;

        editor.active = !activeGenerated;
        addRenderableWidget(editor);

        addFileButtons();

        int toolbarY = height - 45;
        int x = 10;

        x = button(x, toolbarY, 58, "Save", this::save);
        x = button(x, toolbarY, 58, "Build", this::build);
        x = button(
                x,
                toolbarY,
                68,
                published ? "Unpublish" : "Publish",
                published ? this::unpublish : this::publish
        );
        x = button(x, toolbarY, 70, "Reload", this::refresh);
        x = button(x, toolbarY, 86, "Book -> File", this::importBook);
        x = button(x, toolbarY, 82, "Paste Full", this::pasteFull);
        x = button(x, toolbarY, 72, "Copy All", this::copyAll);
        button(x, toolbarY, 64, "Delete", this::deleteFile);
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

            String marker = entry.generated() ? "G " : "  ";
            String label = marker + trimLabel(entry.path(), 27);

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
                .bounds(
                        10,
                        pageY,
                        24,
                        18
                )
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
                .bounds(
                        EXPLORER_WIDTH - 34,
                        pageY,
                        24,
                        18
                )
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

        graphics.fill(
                0,
                0,
                width,
                36,
                0xEE11161D
        );

        graphics.fill(
                0,
                36,
                EXPLORER_WIDTH,
                height - BOTTOM + 18,
                0xEE151B23
        );

        graphics.fill(
                EXPLORER_WIDTH,
                36,
                EXPLORER_WIDTH + 1,
                height - BOTTOM + 18,
                0xFF3B4654
        );

        graphics.drawString(
                font,
                "VS:IA WEB IDE",
                10,
                9,
                0x6FD7FF,
                false
        );

        String state = published ? "PUBLISHED" : "DRAFT";
        int stateColor = published ? 0x62E38A : 0xFFD166;

        graphics.drawString(
                font,
                host
                        + "  |  "
                        + mode
                        + "  |  "
                        + state
                        + "  |  revision "
                        + buildRevision,
                104,
                9,
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
                23,
                0xA7B4C3,
                false
        );

        graphics.drawString(
                font,
                "EXPLORER",
                10,
                38,
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

        graphics.drawString(
                font,
                activePath.isBlank()
                        ? "No file selected"
                        : activePath,
                editorX,
                38,
                activeGenerated
                        ? 0xFFD166
                        : 0xE8F3FF,
                false
        );

        String right =
                language.displayName()
                        + " | "
                        + runtime
                        + (
                        activeGenerated
                                ? " | GENERATED READ-ONLY"
                                : ""
                );

        graphics.drawString(
                font,
                right,
                Math.max(
                        editorX,
                        width - font.width(right) - 10
                ),
                38,
                activeGenerated
                        ? 0xFFD166
                        : 0x8FA9BD,
                false
        );

        int statusY = height - 20;

        graphics.drawString(
                font,
                status
                        + (
                        dirty()
                                ? " | UNSAVED"
                                : ""
                ),
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

        String shortcut =
                "Ctrl+S Save | Ctrl+V paste works in editor";

        graphics.drawString(
                font,
                shortcut,
                Math.max(
                        10,
                        width - font.width(shortcut) - 10
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

    public void acceptSnapshot(W128IdeSnapshotPacket packet) {
        String localValue =
                editor == null
                        ? null
                        : editor.getValue();

        String previousPath = activePath;

        boolean preserveLocal =
                !packet.success()
                        && localValue != null
                        && previousPath.equals(packet.activePath());

        applySnapshot(packet);

        int pages = Math.max(
                1,
                (files.size() + FILE_ROWS - 1) / FILE_ROWS
        );
        filePage = Math.min(filePage, pages - 1);

        init();

        if (preserveLocal && editor != null) {
            editor.setValue(localValue);
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

    private void openFile(String path) {
        if (dirty()) {
            setLocalStatus(
                    "Save the current file before opening another file.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.OPEN,
                path,
                ""
        );
    }

    private void save() {
        if (editor == null) {
            return;
        }

        if (activeGenerated) {
            setLocalStatus(
                    "Generated files are read-only. Edit source and rebuild.",
                    false
            );
            return;
        }

        if (activePath.isBlank()) {
            setLocalStatus(
                    "Create or select a file first.",
                    false
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
                    "Save the current file before creating another file.",
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

    private void deleteFile() {
        if (activePath.isBlank()) {
            setLocalStatus(
                    "Select a file first.",
                    false
            );
            return;
        }

        if (activeGenerated) {
            setLocalStatus(
                    "Generated files are removed/replaced by the build system.",
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

    private void build() {
        if (dirty()) {
            setLocalStatus(
                    "Save before building so the server receives your latest code.",
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
                    "Save and build before publishing.",
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
                    "Unsaved changes exist. Save them before reload.",
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

    private void importBook() {
        if (activeGenerated) {
            setLocalStatus(
                    "Generated files are read-only.",
                    false
            );
            return;
        }

        if (activePath.isBlank()) {
            setLocalStatus(
                    "Select or create a destination file first.",
                    false
            );
            return;
        }

        request(
                W128IdeAction.IMPORT_BOOK,
                activePath,
                ""
        );
    }

    private void pasteFull() {
        if (editor == null || activeGenerated) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String clipboard =
                minecraft.keyboardHandler.getClipboard();

        if (clipboard == null) {
            clipboard = "";
        }

        if (clipboard.length() > MAX_FILE_CHARACTERS) {
            clipboard = clipboard.substring(
                    0,
                    MAX_FILE_CHARACTERS
            );

            setLocalStatus(
                    "Clipboard truncated to the W1.28 per-file limit.",
                    false
            );
        } else {
            setLocalStatus(
                    "Clipboard imported into the editor. Press Save.",
                    true
            );
        }

        editor.setValue(clipboard);
    }

    private void copyAll() {
        if (editor == null) {
            return;
        }

        Minecraft.getInstance()
                .keyboardHandler
                .setClipboard(editor.getValue());

        setLocalStatus(
                "Current file copied to system clipboard.",
                true
        );
    }

    private boolean dirty() {
        return !loadingValue
                && editor != null
                && !editor.getValue().equals(serverContent);
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
                        value.length() - max + 3
                );
    }
}
