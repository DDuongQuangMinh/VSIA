package com.k1ngtle.vsia.signality.engineering.wifi.security.protocol;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class WifiSecurityProtocolRegistry {
    private static final Map<String, WifiSecurityProtocol> BY_ALIAS =
            new LinkedHashMap<>();

    static {
        register(
                new OpenWifiSecurityProtocol(),
                "open",
                "none",
                "signality:none"
        );

        register(
                new PskWifiSecurityProtocol(
                        "signality:wpa",
                        "WPA-PSK compatibility"
                ),
                "wpa"
        );

        register(
                new PskWifiSecurityProtocol(
                        "signality:wpa2",
                        "WPA2-PSK compatibility"
                ),
                "wpa2",
                "wpa2-psk"
        );

        register(
                new PskWifiSecurityProtocol(
                        "signality:wpa3",
                        "WPA3 compatibility"
                ),
                "wpa3",
                "wpa3-psk",
                "sae"
        );
    }

    private WifiSecurityProtocolRegistry() {
    }

    public static synchronized void register(
            WifiSecurityProtocol protocol,
            String... aliases
    ) {
        if (protocol == null
                || protocol.id() == null
                || protocol.id().isBlank()) {
            throw new IllegalArgumentException(
                    "Security protocol and id are required"
            );
        }

        BY_ALIAS.put(
                normalize(
                        protocol.id()
                ),
                protocol
        );

        if (aliases != null) {
            for (String alias
                    : aliases) {
                if (alias != null
                        && !alias.isBlank()) {
                    BY_ALIAS.put(
                            normalize(
                                    alias
                            ),
                            protocol
                    );
                }
            }
        }
    }

    public static synchronized WifiSecurityProtocol resolve(
            String idOrAlias
    ) {
        String key =
                normalize(
                        idOrAlias == null
                                || idOrAlias.isBlank()
                                ? "signality:open"
                                : idOrAlias
                );

        WifiSecurityProtocol protocol =
                BY_ALIAS.get(
                        key
                );

        if (protocol == null) {
            throw new IllegalArgumentException(
                    "Unsupported Wi-Fi security protocol: "
                            + idOrAlias
            );
        }

        return protocol;
    }

    public static synchronized boolean supports(
            String idOrAlias
    ) {
        try {
            resolve(
                    idOrAlias
            );
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static synchronized String canonicalId(
            String idOrAlias
    ) {
        return resolve(
                idOrAlias
        ).id();
    }

    public static synchronized Collection<WifiSecurityProtocol> protocols() {
        return BY_ALIAS.values()
                .stream()
                .distinct()
                .toList();
    }

    private static String normalize(
            String value
    ) {
        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}
