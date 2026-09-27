package com.k1ngtle.vsia.cockpit;

public enum CcTerminalColor {
    WHITE('0', 0xFFF0F0F0),
    ORANGE('1', 0xFFF2B233),
    MAGENTA('2', 0xFFE57FD8),
    LIGHT_BLUE('3', 0xFF99B2F2),
    YELLOW('4', 0xFFDEDE6C),
    LIME('5', 0xFF7FCC19),
    PINK('6', 0xFFF2B2CC),
    GRAY('7', 0xFF4C4C4C),
    LIGHT_GRAY('8', 0xFF999999),
    CYAN('9', 0xFF4C99B2),
    PURPLE('a', 0xFFB266E5),
    BLUE('b', 0xFF3366CC),
    BROWN('c', 0xFF7F664C),
    GREEN('d', 0xFF57A64E),
    RED('e', 0xFFCC4C4C),
    BLACK('f', 0xFF111111);

    private final char blitCode;
    private final int argb;

    CcTerminalColor(
            char blitCode,
            int argb
    ) {
        this.blitCode =
                blitCode;

        this.argb =
                argb;
    }

    public char blitCode() {
        return blitCode;
    }

    public int argb() {
        return argb;
    }

    public static CcTerminalColor fromBlit(
            char value
    ) {
        char lower =
                Character.toLowerCase(
                        value
                );

        for (CcTerminalColor color :
                values()) {
            if (color.blitCode
                    == lower) {
                return color;
            }
        }

        return WHITE;
    }
}
