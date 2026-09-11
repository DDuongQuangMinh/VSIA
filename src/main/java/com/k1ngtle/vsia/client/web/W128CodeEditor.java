package com.k1ngtle.vsia.client.web;

import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class W128CodeEditor extends AbstractWidget {
    private static final int GUTTER_WIDTH = 48;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 12;
    private static final int MAX_HISTORY = 100;

    private final Font font;
    private final int characterLimit;

    private String value = "";
    private List<Line> lines = List.of(new Line(0, 0, ""));

    private int cursor;
    private int anchor;
    private int firstVisibleLine;
    private int horizontalScrollPixels;
    private int preferredColumn = -1;

    private boolean editable = true;
    private boolean dragging;
    private W128IdeLanguage language = W128IdeLanguage.TEXT;
    private Set<Integer> breakpointLines = Set.of();
    private int executionLine;

    private final Deque<State> undo = new ArrayDeque<>();
    private final Deque<State> redo = new ArrayDeque<>();

    public W128CodeEditor(
            Font font,
            int x,
            int y,
            int width,
            int height,
            int characterLimit
    ) {
        super(x, y, width, height, Component.literal("VS:IA code editor"));
        this.font = font;
        this.characterLimit = Math.max(1, characterLimit);
        rebuildLines();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String text) {
        value = truncate(normalize(text));
        cursor = Math.min(cursor, value.length());
        anchor = cursor;
        firstVisibleLine = 0;
        horizontalScrollPixels = 0;
        preferredColumn = -1;
        undo.clear();
        redo.clear();
        rebuildLines();
    }

    public void replaceAll(String text) {
        if (!editable) {
            return;
        }

        pushUndo();
        value = truncate(normalize(text));
        cursor = value.length();
        anchor = cursor;
        preferredColumn = -1;
        redo.clear();
        rebuildLines();
        ensureCursorVisible();
    }

    public void setEditable(boolean value) {
        editable = value;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setLanguage(W128IdeLanguage language) {
        this.language = language == null
                ? W128IdeLanguage.TEXT
                : language;
    }

    public W128IdeLanguage language() {
        return language;
    }

    public int cursorLine() {
        return lineIndexFor(cursor) + 1;
    }

    public int cursorColumn() {
        Line line = lines.get(lineIndexFor(cursor));
        return Math.max(0, cursor - line.start()) + 1;
    }

    public int lineCount() {
        return lines.size();
    }

    public void setBreakpointLines(Set<Integer> lines) {
        if (lines == null || lines.isEmpty()) {
            breakpointLines = Set.of();
            return;
        }
        TreeSet<Integer> valid = new TreeSet<>();
        for (Integer line : lines) {
            if (line != null && line > 0) {
                valid.add(line);
            }
        }
        breakpointLines = Set.copyOf(valid);
    }

    public void setExecutionLine(int oneBasedLine) {
        executionLine = Math.max(0, oneBasedLine);
        if (executionLine > 0) {
            int index = clamp(executionLine - 1, 0, lines.size() - 1);
            if (index < firstVisibleLine || index >= firstVisibleLine + visibleLineCount()) {
                firstVisibleLine = clamp(index - Math.max(1, visibleLineCount() / 2), 0, maxFirstVisibleLine());
            }
        }
    }

    public boolean isInGutter(double mouseX, double mouseY) {
        return mouseX >= getX()
                && mouseX < getX() + GUTTER_WIDTH
                && mouseY >= getY()
                && mouseY < getY() + height;
    }

    public int lineAtMouse(double mouseX, double mouseY) {
        if (!isInGutter(mouseX, mouseY)) {
            return -1;
        }
        int row = (int) Math.floor((mouseY - getY() - PADDING) / LINE_HEIGHT);
        int index = firstVisibleLine + Math.max(0, row);
        return index >= 0 && index < lines.size() ? index + 1 : -1;
    }

    public void goToLine(int oneBasedLine) {
        int lineIndex = clamp(oneBasedLine - 1, 0, lines.size() - 1);
        Line line = lines.get(lineIndex);
        cursor = line.start();
        anchor = cursor;
        preferredColumn = -1;
        ensureCursorVisible();
        setFocused(true);
    }

    public boolean hasSelection() {
        return cursor != anchor;
    }

    public void selectAll() {
        anchor = 0;
        cursor = value.length();
        preferredColumn = -1;
        ensureCursorVisible();
    }

    public String selectedText() {
        if (!hasSelection()) {
            return "";
        }

        int start = Math.min(cursor, anchor);
        int end = Math.max(cursor, anchor);
        return value.substring(start, end);
    }

    public void copySelectionOrAll() {
        String copy = hasSelection() ? selectedText() : value;
        Minecraft.getInstance().keyboardHandler.setClipboard(copy);
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;

        graphics.fill(left, top, right, bottom, 0xFF090C10);
        graphics.fill(left, top, left + GUTTER_WIDTH, bottom, 0xFF111821);
        graphics.fill(
                left + GUTTER_WIDTH - 1,
                top,
                left + GUTTER_WIDTH,
                bottom,
                0xFF34414F
        );

        int currentLine = lineIndexFor(cursor);
        int visibleLines = visibleLineCount();

        graphics.enableScissor(left, top, right, bottom);

        for (int row = 0; row < visibleLines; row++) {
            int lineIndex = firstVisibleLine + row;

            if (lineIndex >= lines.size()) {
                break;
            }

            Line line = lines.get(lineIndex);
            int y = top + PADDING + row * LINE_HEIGHT;

            if (lineIndex + 1 == executionLine) {
                graphics.fill(
                        left + GUTTER_WIDTH,
                        y - 1,
                        right,
                        y + LINE_HEIGHT - 1,
                        0xFF163246
                );
            } else if (lineIndex == currentLine) {
                graphics.fill(
                        left + GUTTER_WIDTH,
                        y - 1,
                        right,
                        y + LINE_HEIGHT - 1,
                        0xFF101722
                );
            }

            if (breakpointLines.contains(lineIndex + 1)) {
                graphics.fill(
                        left + 6,
                        y + 2,
                        left + 12,
                        y + 8,
                        0xFFFF5C5C
                );
            }

            if (lineIndex + 1 == executionLine) {
                graphics.fill(
                        left + 15,
                        y + 3,
                        left + 20,
                        y + 8,
                        0xFFFFD166
                );
            }

            String number = Integer.toString(lineIndex + 1);

            graphics.drawString(
                    font,
                    number,
                    left + GUTTER_WIDTH - 6 - font.width(number),
                    y,
                    lineIndex == currentLine ? 0xFFD166 : 0x65788B,
                    false
            );

            drawSelectionForLine(graphics, lineIndex, line, y);

            drawHighlightedLine(
                    graphics,
                    line.text(),
                    textStartX() - horizontalScrollPixels,
                    y
            );
        }

        drawCaret(graphics);
        graphics.disableScissor();

        int border = isFocused() ? 0xFF5BB8FF : 0xFF3B4654;
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
    }

    private void drawHighlightedLine(
            GuiGraphics graphics,
            String text,
            int x,
            int y
    ) {
        int drawX = x;

        for (W129SyntaxHighlighter.Span span
                : W129SyntaxHighlighter.highlight(
                language,
                text
        )) {
            graphics.drawString(
                    font,
                    span.text(),
                    drawX,
                    y,
                    editable
                            ? span.color()
                            : 0xC3CBD5,
                    false
            );

            drawX += font.width(
                    span.text()
            );
        }
    }

    private void drawSelectionForLine(
            GuiGraphics graphics,
            int lineIndex,
            Line line,
            int y
    ) {
        if (!hasSelection()) {
            return;
        }

        int selectionStart = Math.min(cursor, anchor);
        int selectionEnd = Math.max(cursor, anchor);

        if (selectionEnd < line.start() || selectionStart > line.end()) {
            return;
        }

        int start = Math.max(selectionStart, line.start());
        int end = Math.min(selectionEnd, line.end());

        int startColumn = Math.max(0, start - line.start());
        int endColumn = Math.max(startColumn, end - line.start());

        int x1 = textStartX()
                - horizontalScrollPixels
                + font.width(line.text().substring(
                0,
                Math.min(startColumn, line.text().length())
        ));

        int x2 = textStartX()
                - horizontalScrollPixels
                + font.width(line.text().substring(
                0,
                Math.min(endColumn, line.text().length())
        ));

        if (selectionEnd > line.end() && lineIndex < lines.size() - 1) {
            x2 += 4;
        }

        graphics.fill(
                x1,
                y - 1,
                Math.max(x1 + 1, x2),
                y + LINE_HEIGHT - 1,
                0xAA315D8A
        );
    }

    private void drawCaret(GuiGraphics graphics) {
        if (!isFocused()) {
            return;
        }

        if ((System.currentTimeMillis() / 500L) % 2L != 0L) {
            return;
        }

        int lineIndex = lineIndexFor(cursor);

        if (lineIndex < firstVisibleLine
                || lineIndex >= firstVisibleLine + visibleLineCount()) {
            return;
        }

        Line line = lines.get(lineIndex);
        int column = Math.min(
                Math.max(0, cursor - line.start()),
                line.text().length()
        );

        int x = textStartX()
                - horizontalScrollPixels
                + font.width(line.text().substring(0, column));

        int y = getY()
                + PADDING
                + (lineIndex - firstVisibleLine) * LINE_HEIGHT;

        graphics.fill(
                x,
                y - 1,
                x + 1,
                y + LINE_HEIGHT - 1,
                editable ? 0xFFF2F6FA : 0xFFFFD166
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !inside(mouseX, mouseY)) {
            return false;
        }

        setFocused(true);
        dragging = true;

        int index = indexAt(mouseX, mouseY);

        if (Screen.hasShiftDown()) {
            cursor = index;
        } else {
            cursor = index;
            anchor = index;
        }

        preferredColumn = -1;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button != 0 || !dragging) {
            return false;
        }

        autoScrollForDrag(mouseY);
        cursor = indexAt(mouseX, mouseY);
        preferredColumn = -1;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inside(mouseX, mouseY)) {
            return false;
        }

        if (Screen.hasShiftDown()) {
            horizontalScrollPixels = Math.max(
                    0,
                    horizontalScrollPixels - (int) Math.round(delta * 24.0)
            );
        } else {
            int direction = delta > 0 ? -3 : 3;
            firstVisibleLine = clamp(
                    firstVisibleLine + direction,
                    0,
                    maxFirstVisibleLine()
            );
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean control = Screen.hasControlDown();
        boolean shift = Screen.hasShiftDown();

        if (control) {
            if (keyCode == GLFW.GLFW_KEY_A) {
                selectAll();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_C) {
                copySelectionOrAll();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_X) {
                if (hasSelection()) {
                    Minecraft.getInstance()
                            .keyboardHandler
                            .setClipboard(selectedText());

                    if (editable) {
                        deleteSelection();
                    }
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_V) {
                if (editable) {
                    insertText(
                            Minecraft.getInstance()
                                    .keyboardHandler
                                    .getClipboard()
                    );
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_Z) {
                if (editable) {
                    undo();
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_Y) {
                if (editable) {
                    redo();
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveWord(-1, shift);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveWord(1, shift);
                return true;
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                moveHorizontal(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveHorizontal(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveVertical(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveVertical(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                moveLineBoundary(false, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                moveLineBoundary(true, shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                moveVertical(-visibleLineCount(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                moveVertical(visibleLineCount(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (editable) {
                    backspace();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (editable) {
                    deleteForward();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (editable) {
                    insertNewlineWithIndent();
                }
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                if (editable) {
                    insertText("    ");
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!editable || !SharedConstants.isAllowedChatCharacter(codePoint)) {
            return false;
        }

        insertText(Character.toString(codePoint));
        return true;
    }

    private void insertNewlineWithIndent() {
        Line line = lines.get(
                lineIndexFor(cursor)
        );

        int column = Math.min(
                Math.max(0, cursor - line.start()),
                line.text().length()
        );

        String before =
                line.text().substring(
                        0,
                        column
                );

        int spaces = 0;

        while (spaces < before.length()
                && before.charAt(spaces) == ' ') {
            spaces++;
        }

        String trimmed =
                before.trim();

        if (trimmed.endsWith("{")
                || (
                language == W128IdeLanguage.PYTHON
                        && trimmed.endsWith(":")
        )) {
            spaces += 4;
        }

        insertText(
                "\n"
                        + " ".repeat(
                        Math.max(0, spaces)
                )
        );
    }

    private void insertText(String text) {
        if (!editable || text == null || text.isEmpty()) {
            return;
        }

        String insert = normalize(text);
        int start = Math.min(cursor, anchor);
        int end = Math.max(cursor, anchor);

        int available = characterLimit - (value.length() - (end - start));

        if (available <= 0) {
            return;
        }

        if (insert.length() > available) {
            insert = insert.substring(0, available);
        }

        pushUndo();

        value = value.substring(0, start)
                + insert
                + value.substring(end);

        cursor = start + insert.length();
        anchor = cursor;
        preferredColumn = -1;
        redo.clear();
        rebuildLines();
        ensureCursorVisible();
    }

    private void backspace() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }

        if (cursor <= 0) {
            return;
        }

        pushUndo();

        value = value.substring(0, cursor - 1)
                + value.substring(cursor);

        cursor--;
        anchor = cursor;
        preferredColumn = -1;
        redo.clear();
        rebuildLines();
        ensureCursorVisible();
    }

    private void deleteForward() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }

        if (cursor >= value.length()) {
            return;
        }

        pushUndo();

        value = value.substring(0, cursor)
                + value.substring(cursor + 1);

        anchor = cursor;
        preferredColumn = -1;
        redo.clear();
        rebuildLines();
        ensureCursorVisible();
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }

        int start = Math.min(cursor, anchor);
        int end = Math.max(cursor, anchor);

        pushUndo();

        value = value.substring(0, start)
                + value.substring(end);

        cursor = start;
        anchor = cursor;
        preferredColumn = -1;
        redo.clear();
        rebuildLines();
        ensureCursorVisible();
    }

    private void moveHorizontal(int delta, boolean selecting) {
        moveCursorTo(
                clamp(cursor + delta, 0, value.length()),
                selecting
        );
        preferredColumn = -1;
    }

    private void moveVertical(int deltaLines, boolean selecting) {
        int currentLine = lineIndexFor(cursor);
        Line current = lines.get(currentLine);
        int currentColumn = cursor - current.start();

        if (preferredColumn < 0) {
            preferredColumn = currentColumn;
        }

        int targetLine = clamp(
                currentLine + deltaLines,
                0,
                lines.size() - 1
        );

        Line target = lines.get(targetLine);

        moveCursorTo(
                target.start()
                        + Math.min(preferredColumn, target.text().length()),
                selecting
        );
    }

    private void moveLineBoundary(boolean end, boolean selecting) {
        Line line = lines.get(lineIndexFor(cursor));
        moveCursorTo(end ? line.end() : line.start(), selecting);
        preferredColumn = -1;
    }

    private void moveWord(int direction, boolean selecting) {
        int target = cursor;

        if (direction < 0) {
            while (target > 0
                    && Character.isWhitespace(value.charAt(target - 1))) {
                target--;
            }
            while (target > 0 && word(value.charAt(target - 1))) {
                target--;
            }
        } else {
            while (target < value.length()
                    && Character.isWhitespace(value.charAt(target))) {
                target++;
            }
            while (target < value.length() && word(value.charAt(target))) {
                target++;
            }
        }

        moveCursorTo(target, selecting);
        preferredColumn = -1;
    }

    private static boolean word(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private void moveCursorTo(int target, boolean selecting) {
        cursor = clamp(target, 0, value.length());

        if (!selecting) {
            anchor = cursor;
        }

        ensureCursorVisible();
    }

    private int indexAt(double mouseX, double mouseY) {
        int row = (int) Math.floor(
                (mouseY - getY() - PADDING) / LINE_HEIGHT
        );

        int lineIndex = clamp(
                firstVisibleLine + row,
                0,
                lines.size() - 1
        );

        Line line = lines.get(lineIndex);

        double targetPixels = mouseX
                - textStartX()
                + horizontalScrollPixels;

        if (targetPixels <= 0) {
            return line.start();
        }

        int widthSoFar = 0;

        for (int i = 0; i < line.text().length(); i++) {
            int charWidth = font.width(
                    line.text().substring(i, i + 1)
            );

            if (targetPixels < widthSoFar + charWidth / 2.0) {
                return line.start() + i;
            }

            widthSoFar += charWidth;
        }

        return line.end();
    }

    private void ensureCursorVisible() {
        int lineIndex = lineIndexFor(cursor);
        int visible = visibleLineCount();

        if (lineIndex < firstVisibleLine) {
            firstVisibleLine = lineIndex;
        } else if (lineIndex >= firstVisibleLine + visible) {
            firstVisibleLine = lineIndex - visible + 1;
        }

        firstVisibleLine = clamp(
                firstVisibleLine,
                0,
                maxFirstVisibleLine()
        );

        Line line = lines.get(lineIndex);
        int column = Math.min(
                Math.max(0, cursor - line.start()),
                line.text().length()
        );

        int caretPixels = font.width(line.text().substring(0, column));
        int available = Math.max(
                20,
                width - GUTTER_WIDTH - PADDING * 2
        );

        if (caretPixels - horizontalScrollPixels > available - 8) {
            horizontalScrollPixels = Math.max(
                    0,
                    caretPixels - available + 8
            );
        } else if (caretPixels < horizontalScrollPixels) {
            horizontalScrollPixels = Math.max(0, caretPixels - 8);
        }
    }

    private void autoScrollForDrag(double mouseY) {
        if (mouseY < getY() + PADDING) {
            firstVisibleLine = Math.max(0, firstVisibleLine - 1);
        } else if (mouseY > getY() + height - PADDING) {
            firstVisibleLine = Math.min(
                    maxFirstVisibleLine(),
                    firstVisibleLine + 1
            );
        }
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= getX()
                && mouseX < getX() + width
                && mouseY >= getY()
                && mouseY < getY() + height;
    }

    private int textStartX() {
        return getX() + GUTTER_WIDTH + PADDING;
    }

    private int visibleLineCount() {
        return Math.max(
                1,
                (height - PADDING * 2) / LINE_HEIGHT
        );
    }

    private int maxFirstVisibleLine() {
        return Math.max(0, lines.size() - visibleLineCount());
    }

    private int lineIndexFor(int index) {
        int target = clamp(index, 0, value.length());
        int low = 0;
        int high = lines.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Line line = lines.get(mid);

            if (target < line.start()) {
                high = mid - 1;
            } else if (target > line.end() && mid < lines.size() - 1) {
                low = mid + 1;
            } else {
                return mid;
            }
        }

        return clamp(low, 0, lines.size() - 1);
    }

    private void rebuildLines() {
        List<Line> rebuilt = new ArrayList<>();
        int start = 0;

        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == '\n') {
                rebuilt.add(new Line(
                        start,
                        i,
                        value.substring(start, i)
                ));
                start = i + 1;
            }
        }

        if (rebuilt.isEmpty()) {
            rebuilt.add(new Line(0, 0, ""));
        }

        lines = List.copyOf(rebuilt);
        cursor = clamp(cursor, 0, value.length());
        anchor = clamp(anchor, 0, value.length());
        firstVisibleLine = clamp(
                firstVisibleLine,
                0,
                maxFirstVisibleLine()
        );
    }

    private void pushUndo() {
        undo.push(new State(value, cursor, anchor));

        while (undo.size() > MAX_HISTORY) {
            undo.removeLast();
        }
    }

    private void undo() {
        if (undo.isEmpty()) {
            return;
        }

        redo.push(new State(value, cursor, anchor));
        restore(undo.pop());
    }

    private void redo() {
        if (redo.isEmpty()) {
            return;
        }

        undo.push(new State(value, cursor, anchor));
        restore(redo.pop());
    }

    private void restore(State state) {
        value = state.value();
        cursor = state.cursor();
        anchor = state.anchor();
        preferredColumn = -1;
        rebuildLines();
        ensureCursorVisible();
    }

    private String truncate(String text) {
        if (text.length() <= characterLimit) {
            return text;
        }
        return text.substring(0, characterLimit);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private record Line(int start, int end, String text) {
    }

    private record State(String value, int cursor, int anchor) {
    }
}
