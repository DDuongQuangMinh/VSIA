package com.k1ngtle.vsia.cockpit;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;

public final class CcTerminalBuffer {
    private final int width;
    private final int height;

    private final char[][] characters;
    private final byte[][] foreground;
    private final byte[][] background;

    private int cursorX;
    private int cursorY;

    private CcTerminalColor textColor =
            CcTerminalColor.WHITE;

    private CcTerminalColor backgroundColor =
            CcTerminalColor.BLACK;

    public CcTerminalBuffer(
            int width,
            int height
    ) {
        this.width =
                Math.max(
                        1,
                        width
                );

        this.height =
                Math.max(
                        1,
                        height
                );

        characters =
                new char[this.height][this.width];

        foreground =
                new byte[this.height][this.width];

        background =
                new byte[this.height][this.width];

        clear();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int cursorX() {
        return cursorX + 1;
    }

    public int cursorY() {
        return cursorY + 1;
    }

    public void setCursorPos(
            int x,
            int y
    ) {
        cursorX =
                clamp(
                        x - 1,
                        0,
                        width - 1
                );

        cursorY =
                clamp(
                        y - 1,
                        0,
                        height - 1
                );
    }

    public void setTextColor(
            CcTerminalColor color
    ) {
        if (color != null) {
            textColor =
                    color;
        }
    }

    public void setBackgroundColor(
            CcTerminalColor color
    ) {
        if (color != null) {
            backgroundColor =
                    color;
        }
    }

    public CcTerminalColor textColor() {
        return textColor;
    }

    public CcTerminalColor backgroundColor() {
        return backgroundColor;
    }

    public void clear() {
        byte fg =
                colorIndex(
                        textColor
                );

        byte bg =
                colorIndex(
                        backgroundColor
                );

        for (int row = 0;
             row < height;
             row++) {
            Arrays.fill(
                    characters[row],
                    ' '
            );

            Arrays.fill(
                    foreground[row],
                    fg
            );

            Arrays.fill(
                    background[row],
                    bg
            );
        }

        cursorX = 0;
        cursorY = 0;
    }

    public void clearLine() {
        byte fg =
                colorIndex(
                        textColor
                );

        byte bg =
                colorIndex(
                        backgroundColor
                );

        Arrays.fill(
                characters[cursorY],
                ' '
        );

        Arrays.fill(
                foreground[cursorY],
                fg
        );

        Arrays.fill(
                background[cursorY],
                bg
        );

        cursorX = 0;
    }

    public void write(
            String text
    ) {
        if (text == null
                || text.isEmpty()) {
            return;
        }

        byte fg =
                colorIndex(
                        textColor
                );

        byte bg =
                colorIndex(
                        backgroundColor
                );

        for (int i = 0;
             i < text.length();
             i++) {
            char value =
                    text.charAt(
                            i
                    );

            if (value == '\n') {
                cursorX = 0;
                cursorY++;

                if (cursorY >= height) {
                    scroll(
                            1
                    );

                    cursorY =
                            height - 1;
                }

                continue;
            }

            if (cursorX >= width) {
                cursorX = 0;
                cursorY++;

                if (cursorY >= height) {
                    scroll(
                            1
                    );

                    cursorY =
                            height - 1;
                }
            }

            characters[cursorY][cursorX] =
                    value;

            foreground[cursorY][cursorX] =
                    fg;

            background[cursorY][cursorX] =
                    bg;

            cursorX++;
        }
    }

    public void blit(
            String text,
            String foregroundCodes,
            String backgroundCodes
    ) {
        if (text == null) {
            return;
        }

        for (int i = 0;
             i < text.length();
             i++) {
            if (cursorX >= width) {
                break;
            }

            char value =
                    text.charAt(
                            i
                    );

            CcTerminalColor fg =
                    i < safeLength(
                            foregroundCodes
                    )
                            ? CcTerminalColor.fromBlit(
                            foregroundCodes.charAt(
                                    i
                            )
                    )
                            : textColor;

            CcTerminalColor bg =
                    i < safeLength(
                            backgroundCodes
                    )
                            ? CcTerminalColor.fromBlit(
                            backgroundCodes.charAt(
                                    i
                            )
                    )
                            : backgroundColor;

            characters[cursorY][cursorX] =
                    value;

            foreground[cursorY][cursorX] =
                    colorIndex(
                            fg
                    );

            background[cursorY][cursorX] =
                    colorIndex(
                            bg
                    );

            cursorX++;
        }
    }

    public void scroll(
            int lines
    ) {
        if (lines == 0) {
            return;
        }

        int amount =
                Math.min(
                        height,
                        Math.abs(
                                lines
                        )
                );

        if (lines > 0) {
            for (int row = 0;
                 row < height - amount;
                 row++) {
                copyRow(
                        row + amount,
                        row
                );
            }

            for (int row = height - amount;
                 row < height;
                 row++) {
                blankRow(
                        row
                );
            }
        } else {
            for (int row = height - 1;
                 row >= amount;
                 row--) {
                copyRow(
                        row - amount,
                        row
                );
            }

            for (int row = 0;
                 row < amount;
                 row++) {
                blankRow(
                        row
                );
            }
        }
    }

    public char characterAt(
            int x,
            int y
    ) {
        return characters[y][x];
    }

    public CcTerminalColor foregroundAt(
            int x,
            int y
    ) {
        return CcTerminalColor.values()[
                Byte.toUnsignedInt(
                        foreground[y][x]
                )
                        % CcTerminalColor.values().length
                ];
    }

    public CcTerminalColor backgroundAt(
            int x,
            int y
    ) {
        return CcTerminalColor.values()[
                Byte.toUnsignedInt(
                        background[y][x]
                )
                        % CcTerminalColor.values().length
                ];
    }

    public String line(
            int row
    ) {
        return new String(
                characters[row]
        );
    }

    public void save(
            CompoundTag tag
    ) {
        tag.putInt(
                "TerminalWidth",
                width
        );

        tag.putInt(
                "TerminalHeight",
                height
        );

        tag.putInt(
                "TerminalCursorX",
                cursorX
        );

        tag.putInt(
                "TerminalCursorY",
                cursorY
        );

        tag.putByte(
                "TerminalTextColor",
                colorIndex(
                        textColor
                )
        );

        tag.putByte(
                "TerminalBackgroundColor",
                colorIndex(
                        backgroundColor
                )
        );

        for (int row = 0;
             row < height;
             row++) {
            tag.putString(
                    "TerminalLine"
                            + row,
                    new String(
                            characters[row]
                    )
            );

            tag.putByteArray(
                    "TerminalFg"
                            + row,
                    foreground[row]
            );

            tag.putByteArray(
                    "TerminalBg"
                            + row,
                    background[row]
            );
        }
    }

    public void load(
            CompoundTag tag
    ) {
        cursorX =
                clamp(
                        tag.getInt(
                                "TerminalCursorX"
                        ),
                        0,
                        width - 1
                );

        cursorY =
                clamp(
                        tag.getInt(
                                "TerminalCursorY"
                        ),
                        0,
                        height - 1
                );

        textColor =
                colorFromIndex(
                        tag.getByte(
                                "TerminalTextColor"
                        )
                );

        backgroundColor =
                colorFromIndex(
                        tag.getByte(
                                "TerminalBackgroundColor"
                        )
                );

        for (int row = 0;
             row < height;
             row++) {
            String line =
                    tag.getString(
                            "TerminalLine"
                                    + row
                    );

            Arrays.fill(
                    characters[row],
                    ' '
            );

            int copy =
                    Math.min(
                            width,
                            line.length()
                    );

            line.getChars(
                    0,
                    copy,
                    characters[row],
                    0
            );

            byte[] fg =
                    tag.getByteArray(
                            "TerminalFg"
                                    + row
                    );

            byte[] bg =
                    tag.getByteArray(
                            "TerminalBg"
                                    + row
                    );

            if (fg.length
                    == width) {
                System.arraycopy(
                        fg,
                        0,
                        foreground[row],
                        0,
                        width
                );
            }

            if (bg.length
                    == width) {
                System.arraycopy(
                        bg,
                        0,
                        background[row],
                        0,
                        width
                );
            }
        }
    }

    private void copyRow(
            int source,
            int destination
    ) {
        System.arraycopy(
                characters[source],
                0,
                characters[destination],
                0,
                width
        );

        System.arraycopy(
                foreground[source],
                0,
                foreground[destination],
                0,
                width
        );

        System.arraycopy(
                background[source],
                0,
                background[destination],
                0,
                width
        );
    }

    private void blankRow(
            int row
    ) {
        Arrays.fill(
                characters[row],
                ' '
        );

        Arrays.fill(
                foreground[row],
                colorIndex(
                        textColor
                )
        );

        Arrays.fill(
                background[row],
                colorIndex(
                        backgroundColor
                )
        );
    }

    private static int safeLength(
            String value
    ) {
        return value == null
                ? 0
                : value.length();
    }

    private static byte colorIndex(
            CcTerminalColor color
    ) {
        return (byte) color.ordinal();
    }

    private static CcTerminalColor colorFromIndex(
            byte value
    ) {
        return CcTerminalColor.values()[
                Byte.toUnsignedInt(
                        value
                )
                        % CcTerminalColor.values().length
                ];
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}
