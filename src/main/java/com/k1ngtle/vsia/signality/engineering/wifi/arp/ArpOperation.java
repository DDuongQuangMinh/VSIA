package com.k1ngtle.vsia.signality.engineering.wifi.arp;

public enum ArpOperation {
    REQUEST(
            1
    ),
    REPLY(
            2
    );

    private final int code;

    ArpOperation(
            int code
    ) {
        this.code =
                code;
    }

    public int code() {
        return code;
    }

    public static ArpOperation fromCode(
            int code
    ) {
        return switch (code) {
            case 1 -> REQUEST;
            case 2 -> REPLY;
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported ARP operation "
                                    + code
                    );
        };
    }
}
