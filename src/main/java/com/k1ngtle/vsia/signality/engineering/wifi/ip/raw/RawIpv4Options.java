package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIpv4Options {
    public static final int MAX_OPTIONS_BYTES = 40;

    private RawIpv4Options() {
    }

    public static Parsed parse(byte[] rawOptions) {
        byte[] options =
                rawOptions == null
                        ? new byte[0]
                        : rawOptions.clone();

        if (options.length > MAX_OPTIONS_BYTES) {
            throw new IllegalArgumentException(
                    "IPv4 options exceed 40 bytes"
            );
        }

        if ((options.length & 3) != 0) {
            throw new IllegalArgumentException(
                    "IPv4 options must make the header 32-bit aligned"
            );
        }

        List<Option> parsed =
                new ArrayList<>();

        int cursor = 0;
        boolean eolSeen = false;

        while (cursor < options.length) {
            int type =
                    options[cursor]
                            & 0xFF;

            if (eolSeen) {
                if (type != 0) {
                    throw new IllegalArgumentException(
                            "Non-zero data after IPv4 End of Option List"
                    );
                }

                cursor++;
                continue;
            }

            if (type == 0) {
                parsed.add(
                        new Option(
                                0,
                                false,
                                0,
                                0,
                                new byte[] {0}
                        )
                );

                eolSeen = true;
                cursor++;
                continue;
            }

            if (type == 1) {
                parsed.add(
                        new Option(
                                1,
                                false,
                                0,
                                1,
                                new byte[] {1}
                        )
                );

                cursor++;
                continue;
            }

            if (cursor + 1 >= options.length) {
                throw new IllegalArgumentException(
                        "Truncated IPv4 option length"
                );
            }

            int length =
                    options[cursor + 1]
                            & 0xFF;

            if (length < 2) {
                throw new IllegalArgumentException(
                        "IPv4 option length must be >= 2"
                );
            }

            if (cursor + length > options.length) {
                throw new IllegalArgumentException(
                        "IPv4 option exceeds header"
                );
            }

            byte[] encoded =
                    Arrays.copyOfRange(
                            options,
                            cursor,
                            cursor + length
                    );

            parsed.add(
                    new Option(
                            type,
                            (type & 0x80) != 0,
                            (type >>> 5) & 0x03,
                            type & 0x1F,
                            encoded
                    )
            );

            cursor += length;
        }

        return new Parsed(
                options,
                List.copyOf(parsed)
        );
    }

    public static byte[] copiedForNonInitialFragment(
            byte[] rawOptions
    ) {
        Parsed parsed =
                parse(rawOptions);

        List<Byte> bytes =
                new ArrayList<>();

        for (Option option
                : parsed.options()) {
            if (option.type() == 0) {
                break;
            }

            if (option.type() == 1) {
                continue;
            }

            if (!option.copyToFragments()) {
                continue;
            }

            for (byte value
                    : option.encoded()) {
                bytes.add(value);
            }
        }

        return padToWord(
                toArray(bytes)
        );
    }

    public static void validateForFragmentOffset(
            byte[] rawOptions,
            int fragmentOffsetUnits
    ) {
        Parsed parsed =
                parse(rawOptions);

        if (fragmentOffsetUnits == 0) {
            return;
        }

        for (Option option
                : parsed.options()) {
            if (option.type() == 0
                    || option.type() == 1) {
                continue;
            }

            if (!option.copyToFragments()) {
                throw new IllegalArgumentException(
                        "Non-copied IPv4 option present on non-initial fragment"
                );
            }
        }
    }

    public static byte[] padToWord(
            byte[] raw
    ) {
        byte[] source =
                raw == null
                        ? new byte[0]
                        : raw.clone();

        if (source.length > MAX_OPTIONS_BYTES) {
            throw new IllegalArgumentException(
                    "IPv4 options exceed 40 bytes"
            );
        }

        int paddedLength =
                (source.length + 3)
                        & ~3;

        if (paddedLength > MAX_OPTIONS_BYTES) {
            throw new IllegalArgumentException(
                    "IPv4 options padding exceeds 40 bytes"
            );
        }

        return Arrays.copyOf(
                source,
                paddedLength
        );
    }

    private static byte[] toArray(
            List<Byte> values
    ) {
        byte[] out =
                new byte[values.size()];

        for (int i = 0;
             i < values.size();
             i++) {
            out[i] =
                    values.get(i);
        }

        return out;
    }

    public record Parsed(
            byte[] raw,
            List<Option> options
    ) {
        public Parsed {
            raw =
                    raw == null
                            ? new byte[0]
                            : raw.clone();

            options =
                    options == null
                            ? List.of()
                            : List.copyOf(options);
        }

        @Override
        public byte[] raw() {
            return raw.clone();
        }
    }

    public record Option(
            int type,
            boolean copyToFragments,
            int optionClass,
            int number,
            byte[] encoded
    ) {
        public Option {
            encoded =
                    encoded == null
                            ? new byte[0]
                            : encoded.clone();
        }

        @Override
        public byte[] encoded() {
            return encoded.clone();
        }
    }
}
