package com.k1ngtle.vsia.signality.engineering.wifi.dns;

public enum DnsType {
    A(1),
    CNAME(5),
    PTR(12),
    MX(15),
    AAAA(28);

    private final int code;

    DnsType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DnsType fromCode(int code) {
        for (DnsType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported DNS type " + code
        );
    }

    public static DnsType byName(String name) {
        return valueOf(
                name == null || name.isBlank()
                        ? "A"
                        : name.toUpperCase(java.util.Locale.ROOT)
        );
    }
}
